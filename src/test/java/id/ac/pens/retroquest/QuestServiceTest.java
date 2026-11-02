package id.ac.pens.retroquest;

import static id.ac.pens.retroquest.dao.JdbcDao.*;
import static org.junit.jupiter.api.Assertions.*;

import id.ac.pens.retroquest.dao.*;
import id.ac.pens.retroquest.model.*;
import id.ac.pens.retroquest.service.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class QuestServiceTest {
  Database db;
  AuthService auth;
  QuestService service;
  User teacher, student, outsider, otherTeacher;
  long room, module, quest;
  @TempDir Path temp;
  Path file;

  @BeforeEach
  void setup() throws Exception {
    db =
        new Database(
            "jdbc:h2:mem:"
                + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    db.initialize();
    auth = new AuthService(db);
    service = new QuestService(db);
    teacher = auth.register("Dosen", "dosen@test.id", "Password123!", true);
    student = auth.register("Mahasiswa", "student@test.id", "Password123!", false);
    outsider = auth.register("Luar kelas", "outsider@test.id", "Password123!", false);
    otherTeacher = auth.register("Dosen lain", "other@test.id", "Password123!", true);
    room = service.createClass(teacher, "PBO");
    String code = service.classes(teacher).getFirst().text("join_code");
    service.join(student, code, "Code Warrior");
    module =
        service.saveModule(
            teacher,
            room,
            "Enkapsulasi",
            "Materi",
            List.of(new AiService.Card("Class", "Objek dan atribut private")),
            null);
    quest = makeQuest(module, 350, true);
    file = temp.resolve("Tugas.java");
    Files.writeString(file, "class Tugas { private int value; }");
  }

  long makeQuest(long moduleId, int reward, boolean published) {
    return service.saveQuest(
        teacher,
        0,
        moduleId,
        "Latihan",
        "Terapkan enkapsulasi",
        LocalDateTime.now().plusDays(7),
        "java,pdf",
        reward,
        Rubric.standard(),
        published);
  }

  int exp() {
    return service.leaderboard(student, room).getFirst().number("exp_total");
  }

  @AfterEach
  void cleanupUploads() {
    if (db != null)
      db.read(
          c -> {
            for (var row : list(c, "SELECT file_path FROM submissions"))
              Files.deleteIfExists(Path.of(row.text("file_path")));
            return null;
          });
  }

  @Test
  void registerLoginAndPasswordSecurity() {
    assertInstanceOf(Student.class, auth.login(" STUDENT@test.id ", "Password123!"));
    String hash =
        db.read(
            c ->
                one(c, "SELECT password_hash FROM users WHERE id=?", student.id())
                    .text("password_hash"));
    assertNotEquals("Password123!", hash);
    assertTrue(Passwords.verify("Password123!", hash));
    assertFalse(Passwords.verify("wrong", hash));
    assertThrows(IllegalArgumentException.class, () -> auth.login("student@test.id", "wrong"));
    assertThrows(
        IllegalArgumentException.class,
        () -> auth.register("X", "STUDENT@test.id", "Password123!", false));
  }

  @Test
  void registrationValidation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> auth.register("", "x@test.id", "Password123!", false));
    assertThrows(
        IllegalArgumentException.class, () -> auth.register("X", "invalid", "Password123!", false));
    assertThrows(
        IllegalArgumentException.class, () -> auth.register("X", "x@test.id", "short", false));
  }

  @Test
  void enforceRoleAndClassOwnership() {
    assertThrows(IllegalArgumentException.class, () -> service.createClass(student, "Illegal"));
    assertThrows(IllegalArgumentException.class, () -> service.quests(outsider, room));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.saveModule(
                otherTeacher, room, "X", "", List.of(new AiService.Card("X", "Y")), null));
    assertThrows(IllegalArgumentException.class, () -> service.leaderboard(otherTeacher, room));
    assertThrows(IllegalArgumentException.class, () -> service.submit(outsider, quest, file));
  }

  @Test
  void duplicateMembershipRejected() {
    String code = service.classes(teacher).getFirst().text("join_code");
    assertThrows(IllegalArgumentException.class, () -> service.join(student, code, "Code Warrior"));
    assertThrows(IllegalArgumentException.class, () -> service.join(outsider, code, "Unknown"));
  }

  @Test
  void draftsHiddenFromStudent() {
    long draft = makeQuest(module, 100, false);
    assertEquals(1, service.quests(student, room).size());
    assertEquals(2, service.quests(teacher, room).size());
    assertThrows(IllegalArgumentException.class, () -> service.detail(student, draft));
    assertThrows(IllegalArgumentException.class, () -> service.submit(student, draft, file));
  }

  @Test
  void publishDraftAndFreeze() {
    long draft = makeQuest(module, 100, false);
    service.saveQuest(
        teacher,
        draft,
        module,
        "Reviewed",
        "Complete",
        LocalDateTime.now().plusDays(2),
        "java",
        100,
        Rubric.standard(),
        true);
    assertEquals(2, service.quests(student, room).size());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.saveQuest(
                teacher,
                draft,
                module,
                "Changed",
                "Changed",
                LocalDateTime.now().plusDays(3),
                "java",
                500,
                Rubric.standard(),
                true));
  }

  @Test
  void invalidRubricAndFormats() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Rubric(List.of(new Rubric.Criterion("A", 90, "Guide"))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Rubric(
                List.of(
                    new Rubric.Criterion("A", -1, "Guide"),
                    new Rubric.Criterion("B", 101, "Guide"))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.saveQuest(
                teacher,
                0,
                module,
                "X",
                "X",
                LocalDateTime.now().plusDays(1),
                "exe",
                100,
                Rubric.standard(),
                true));
  }

  @Test
  void rejectInvalidAndEmptyFiles() throws Exception {
    Path empty = temp.resolve("empty.java");
    Files.createFile(empty);
    Path exe = temp.resolve("bad.exe");
    Files.writeString(exe, "bad");
    assertThrows(IllegalArgumentException.class, () -> service.submit(student, quest, empty));
    assertThrows(IllegalArgumentException.class, () -> service.submit(student, quest, exe));
    assertEquals(0, service.submissions(student, room).size());
  }

  @Test
  void rejectExpiredQuest() {
    db.transaction(
        c -> {
          update(
              c,
              "UPDATE quests SET deadline=? WHERE id=?",
              LocalDateTime.now().minusMinutes(1),
              quest);
          return null;
        });
    assertThrows(IllegalArgumentException.class, () -> service.submit(student, quest, file));
  }

  @Test
  void rejectOversizedSubmission() throws Exception {
    Path large = temp.resolve("large.java");
    try (var random = new java.io.RandomAccessFile(large.toFile(), "rw")) {
      random.setLength(FilesService.MAX_BYTES + 1);
    }
    assertThrows(IllegalArgumentException.class, () -> service.submit(student, quest, large));
    assertTrue(service.submissions(student, room).isEmpty());
  }

  @Test
  void submissionCopiesFileAndWaitsForTeacher() throws Exception {
    long sub = service.submit(student, quest, file);
    Path saved = service.submissionFile(student, sub);
    assertNotEquals(file, saved);
    assertEquals(Files.readString(file), Files.readString(saved));
    assertEquals("REVIEW_PENDING", service.submissions(student, room).getFirst().text("status"));
    assertEquals(0, exp());
    assertThrows(IllegalArgumentException.class, () -> service.submit(student, quest, file));
  }

  @Test
  void finalApprovalAwardsExactlyOnce() {
    long sub = service.submit(student, quest, file);
    service.review(teacher, sub, 92, "Bagus", false);
    assertEquals(350, exp());
    assertEquals(4, new Progress(exp()).level());
    assertEquals(2, new Progress(exp()).tier());
    service.review(teacher, sub, 92, "Ulang", false);
    assertEquals(350, exp());
    assertEquals(2, service.badges(student, room).size());
    assertEquals(92, service.submissions(student, room).getFirst().number("final_score"));
  }

  @Test
  void unauthorizedReviewAndBadScore() {
    long sub = service.submit(student, quest, file);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.review(student, sub, 100, "Self grading", false));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.review(otherTeacher, sub, 100, "Not owner", false));
    assertThrows(
        IllegalArgumentException.class, () -> service.review(teacher, sub, 101, "Invalid", false));
    assertThrows(IllegalArgumentException.class, () -> service.review(teacher, sub, 90, "", false));
    assertEquals(0, exp());
  }

  @Test
  void revisionHistoryAndLatestVersion() {
    long first = service.submit(student, quest, file);
    service.review(teacher, first, null, "Perbaiki validasi", true);
    assertEquals(0, exp());
    long second = service.submit(student, quest, file);
    assertEquals(2, service.submissions(student, room).size());
    assertThrows(
        IllegalArgumentException.class, () -> service.review(teacher, first, 95, "Old", false));
    service.review(teacher, second, 95, "Revisi baik", false);
    assertEquals(350, exp());
    assertThrows(IllegalArgumentException.class, () -> service.submit(student, quest, file));
    assertEquals("REVISION_REQUIRED", service.submissions(student, room).get(1).text("status"));
  }

  @Test
  void databaseRollbackKeepsApprovalAndExpAtomic() {
    long sub = service.submit(student, quest, file);
    db.transaction(
        c -> {
          update(
              c,
              "ALTER TABLE exp_transactions ADD CONSTRAINT force_failure CHECK (exp_amount < 300)");
          return null;
        });
    assertThrows(
        IllegalStateException.class, () -> service.review(teacher, sub, 90, "Valid", false));
    assertEquals(0, exp());
    assertEquals("REVIEW_PENDING", service.submissions(student, room).getFirst().text("status"));
    assertEquals(
        0,
        db.read(c -> one(c, "SELECT COUNT(*) AS n FROM exp_transactions").number("n")).intValue());
  }

  @Test
  void concurrentApprovalsDoNotDoubleReward() throws Exception {
    long sub = service.submit(student, quest, file);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var tasks =
          List.<Callable<Void>>of(
              () -> {
                service.review(teacher, sub, 90, "A", false);
                return null;
              },
              () -> {
                service.review(teacher, sub, 90, "B", false);
                return null;
              });
      for (var result : pool.invokeAll(tasks)) result.get();
    }
    assertEquals(350, exp());
    assertEquals(
        1,
        db.read(c -> one(c, "SELECT COUNT(*) AS n FROM exp_transactions").number("n")).intValue());
  }

  @Test
  void progressIsPerClass() {
    long sub = service.submit(student, quest, file);
    service.review(teacher, sub, 90, "OK", false);
    long second = service.createClass(teacher, "Other guild");
    String code =
        service.classes(teacher).stream()
            .filter(r -> r.id("id") == second)
            .findFirst()
            .orElseThrow()
            .text("join_code");
    service.join(student, code, "Logic Rogue");
    assertEquals(0, service.leaderboard(student, second).getFirst().number("exp_total"));
    assertTrue(service.badges(student, second).isEmpty());
    assertEquals(350, exp());
  }

  @Test
  void privateSubmissionsNotVisibleToOthers() {
    long sub = service.submit(student, quest, file);
    String code = service.classes(teacher).getFirst().text("join_code");
    service.join(outsider, code, "Logic Rogue");
    assertTrue(service.submissions(outsider, room).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> service.submissionFile(outsider, sub));
    assertThrows(IllegalArgumentException.class, () -> service.evaluations(outsider, sub));
  }

  @Test
  void aiFailureKeepsManualReviewAvailable() {
    long sub = service.submit(student, quest, file);
    String message = service.evaluate(student, sub);
    assertTrue(message.contains("tidak tersedia"));
    assertEquals("FAILED", service.evaluations(student, sub).getFirst().text("status"));
    assertEquals("REVIEW_PENDING", service.submissions(student, room).getFirst().text("status"));
    service.review(teacher, sub, 80, "Manual review", false);
    assertEquals(350, exp());
  }

  @Test
  void avatarSelectionPersists() {
    service.avatar(student, room, "Logic Rogue");
    assertEquals("Logic Rogue", service.leaderboard(student, room).getFirst().text("archetype"));
    assertThrows(IllegalArgumentException.class, () -> service.avatar(student, room, "Hacker"));
  }

  @Test
  void onlyUnpublishedModuleCanBeEdited() {
    long draftModule =
        service.saveModule(
            teacher, room, "Draft", "Source", List.of(new AiService.Card("A", "B")), null);
    service.updateModule(
        teacher, draftModule, "Revised", "Text", List.of(new AiService.Card("New", "Body")));
    assertEquals("Revised", service.modules(teacher, room).getFirst().text("title"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.updateModule(
                teacher, module, "Changed", "Text", List.of(new AiService.Card("A", "B"))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.updateModule(
                otherTeacher,
                draftModule,
                "Changed",
                "Text",
                List.of(new AiService.Card("A", "B"))));
  }

  @Test
  void observerFiresOnlyAfterSuccess() {
    int[] events = {0};
    service.observe(() -> events[0]++);
    long sub = service.submit(student, quest, file);
    assertEquals(1, events[0]);
    assertThrows(
        IllegalArgumentException.class, () -> service.review(teacher, sub, 999, "No", false));
    assertEquals(1, events[0]);
    service.review(teacher, sub, 90, "OK", false);
    assertEquals(2, events[0]);
  }
}
