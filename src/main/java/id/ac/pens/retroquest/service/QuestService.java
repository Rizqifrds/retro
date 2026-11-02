package id.ac.pens.retroquest.service;

import static id.ac.pens.retroquest.dao.JdbcDao.*;
import static id.ac.pens.retroquest.service.AiService.JSON;

import id.ac.pens.retroquest.dao.*;
import id.ac.pens.retroquest.dao.JdbcDao.Row;
import id.ac.pens.retroquest.model.*;
import java.nio.file.*;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class QuestService {
  private final Database db;
  private final FilesService files = new FilesService();
  private final List<Runnable> observers = new CopyOnWriteArrayList<>();

  public QuestService(Database db) {
    this.db = db;
  }

  public void observe(Runnable listener) {
    observers.add(listener);
  }

  private void changed() {
    observers.forEach(
        r -> {
          try {
            r.run();
          } catch (RuntimeException ignored) {
            /* UI listener cannot roll back a committed transaction. */
          }
        });
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }

  private void role(Connection c, User u, String role) throws Exception {
    one(c, "SELECT id FROM users WHERE id=? AND role=?", u.id(), role);
  }

  private Row access(Connection c, User u, long classId) throws Exception {
    Row room = one(c, "SELECT * FROM classes WHERE id=?", classId);
    if (room.id("lecturer_id") != u.id())
      one(c, "SELECT id FROM class_members WHERE class_id=? AND student_id=?", classId, u.id());
    return room;
  }

  private Row owner(Connection c, User u, long classId) throws Exception {
    role(c, u, "LECTURER");
    return one(c, "SELECT * FROM classes WHERE id=? AND lecturer_id=?", classId, u.id());
  }

  private Row quest(Connection c, long id) throws Exception {
    return one(
        c,
        "SELECT q.*, m.class_id, m.content_json, m.title AS module_title FROM quests q JOIN"
            + " learning_modules m ON m.id=q.module_id WHERE q.id=?",
        id);
  }

  public List<Row> classes(User u) {
    return db.read(
        c ->
            list(
                c,
                "SELECT DISTINCT c.* FROM classes c LEFT JOIN class_members cm ON cm.class_id=c.id"
                    + " WHERE c.lecturer_id=? OR cm.student_id=? ORDER BY c.id",
                u.id(),
                u.id()));
  }

  public long createClass(User u, String name) {
    require(
        name != null && !name.isBlank() && name.length() <= 160,
        "Nama kelas wajib diisi, maksimal 160 karakter.");
    long id =
        db.transaction(
            c -> {
              role(c, u, "LECTURER");
              return insert(
                  c,
                  "INSERT INTO classes(lecturer_id,name,join_code) VALUES(?,?,?)",
                  u.id(),
                  name.strip(),
                  UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
            });
    changed();
    return id;
  }

  public void join(User u, String code, String archetype) {
    require(
        Set.of("Code Warrior", "Byte Mage", "Logic Rogue").contains(archetype),
        "Arketipe tidak valid.");
    db.transaction(
        c -> {
          role(c, u, "STUDENT");
          var room =
              one(
                  c,
                  "SELECT * FROM classes WHERE join_code=?",
                  code.strip().toUpperCase(Locale.ROOT));
          require(
              list(
                      c,
                      "SELECT id FROM class_members WHERE class_id=? AND student_id=?",
                      room.id("id"),
                      u.id())
                  .isEmpty(),
              "Anda sudah menjadi anggota kelas ini.");
          insert(
              c,
              "INSERT INTO class_members(class_id,student_id,archetype) VALUES(?,?,?)",
              room.id("id"),
              u.id(),
              archetype);
          return null;
        });
    changed();
  }

  public void avatar(User u, long classId, String archetype) {
    require(
        Set.of("Code Warrior", "Byte Mage", "Logic Rogue").contains(archetype),
        "Arketipe tidak valid.");
    db.transaction(
        c -> {
          role(c, u, "STUDENT");
          one(c, "SELECT id FROM class_members WHERE class_id=? AND student_id=?", classId, u.id());
          update(
              c,
              "UPDATE class_members SET archetype=? WHERE class_id=? AND student_id=?",
              archetype,
              classId,
              u.id());
          return null;
        });
    changed();
  }

  public List<Row> quests(User u, long classId) {
    return db.read(
        c -> {
          var room = access(c, u, classId);
          return list(
              c,
              "SELECT q.*,m.title AS module_title FROM quests q JOIN learning_modules m ON"
                  + " m.id=q.module_id WHERE m.class_id=?"
                  + (room.id("lecturer_id") == u.id() ? "" : " AND q.status='PUBLISHED'")
                  + " ORDER BY q.deadline,q.id",
              classId);
        });
  }

  public Row detail(User u, long questId) {
    return db.read(
        c -> {
          var q = quest(c, questId);
          var room = access(c, u, q.id("class_id"));
          require(
              q.text("status").equals("PUBLISHED") || room.id("lecturer_id") == u.id(),
              "Quest belum dipublikasikan.");
          return q;
        });
  }

  public List<Row> modules(User u, long classId) {
    return db.read(
        c -> {
          owner(c, u, classId);
          return list(
              c, "SELECT * FROM learning_modules WHERE class_id=? ORDER BY id DESC", classId);
        });
  }

  public long saveModule(
      User u, long classId, String title, String text, List<AiService.Card> cards, Path source) {
    require(
        title != null && !title.isBlank() && title.length() <= 200,
        "Judul modul wajib diisi, maksimal 200 karakter.");
    require(
        cards != null
            && !cards.isEmpty()
            && cards.stream()
                .allMatch(
                    card ->
                        card.title() != null
                            && !card.title().isBlank()
                            && card.body() != null
                            && !card.body().isBlank()),
        "Kartu belajar harus lengkap.");
    return db.transaction(
        c -> {
          owner(c, u, classId);
          Path saved = source == null ? null : files.store(source, Set.of("pdf", "docx"));
          try {
            return insert(
                c,
                "INSERT INTO"
                    + " learning_modules(class_id,title,file_path,extracted_text,content_json,status)"
                    + " VALUES(?,?,?,?,?,'DRAFT')",
                classId,
                title.strip(),
                saved == null ? null : saved.toString(),
                text,
                JSON.writeValueAsString(cards));
          } catch (Exception e) {
            if (saved != null) Files.deleteIfExists(saved);
            throw e;
          }
        });
  }

  public void updateModule(
      User u, long moduleId, String title, String text, List<AiService.Card> cards) {
    require(
        title != null && !title.isBlank() && title.length() <= 200,
        "Judul modul wajib diisi, maksimal 200 karakter.");
    require(
        cards != null
            && !cards.isEmpty()
            && cards.stream()
                .allMatch(
                    card ->
                        card.title() != null
                            && !card.title().isBlank()
                            && card.body() != null
                            && !card.body().isBlank()),
        "Kartu belajar harus lengkap.");
    db.transaction(
        c -> {
          var module = one(c, "SELECT * FROM learning_modules WHERE id=? FOR UPDATE", moduleId);
          owner(c, u, module.id("class_id"));
          require(
              list(c, "SELECT id FROM quests WHERE module_id=? AND status='PUBLISHED'", moduleId)
                  .isEmpty(),
              "Materi yang terhubung ke quest terbit tidak dapat diubah. Buat modul baru untuk"
                  + " perubahan.");
          update(
              c,
              "UPDATE learning_modules SET title=?,extracted_text=?,content_json=? WHERE id=?",
              title.strip(),
              text,
              JSON.writeValueAsString(cards),
              moduleId);
          return null;
        });
    changed();
  }

  public long saveQuest(
      User u,
      long questId,
      long moduleId,
      String title,
      String instructions,
      LocalDateTime deadline,
      String allowed,
      int exp,
      Rubric rubric,
      boolean publish) {
    require(
        title != null && !title.isBlank() && title.length() <= 200,
        "Judul quest wajib diisi, maksimal 200 karakter.");
    require(instructions != null && !instructions.isBlank(), "Instruksi harus diisi.");
    require(deadline != null, "Tenggat wajib diisi.");
    require(exp > 0 && exp <= 10000, "Reward EXP harus 1-10.000.");
    Objects.requireNonNull(rubric, "Rubrik wajib diisi.");
    Set<String> types = parseTypes(allowed);
    require(
        !types.isEmpty() && DocumentService.TYPES.containsAll(types),
        "Format berkas tidak didukung.");
    if (publish)
      require(deadline.isAfter(LocalDateTime.now()), "Tenggat publikasi harus di masa depan.");
    long id =
        db.transaction(
            c -> {
              var module = one(c, "SELECT * FROM learning_modules WHERE id=? FOR UPDATE", moduleId);
              owner(c, u, module.id("class_id"));
              if (publish)
                require(
                    !module.text("content_json").isBlank(), "Modul harus memuat kartu belajar.");
              String status = publish ? "PUBLISHED" : "DRAFT";
              String formats = String.join(",", new TreeSet<>(types));
              if (questId == 0) {
                long result =
                    insert(
                        c,
                        "INSERT INTO"
                            + " quests(module_id,title,description,deadline,allowed_types,exp_reward,rubric_json,status)"
                            + " VALUES(?,?,?,?,?,?,?,?)",
                        moduleId,
                        title.strip(),
                        instructions,
                        deadline,
                        formats,
                        exp,
                        JSON.writeValueAsString(rubric),
                        status);
                if (publish)
                  update(c, "UPDATE learning_modules SET status='PUBLISHED' WHERE id=?", moduleId);
                return result;
              }
              var old = one(c, "SELECT * FROM quests WHERE id=? FOR UPDATE", questId);
              require(
                  old.id("module_id") == moduleId && old.text("status").equals("DRAFT"),
                  "Quest yang sudah dipublikasikan tidak dapat diubah.");
              update(
                  c,
                  "UPDATE quests SET"
                      + " title=?,description=?,deadline=?,allowed_types=?,exp_reward=?,rubric_json=?,status=?"
                      + " WHERE id=?",
                  title.strip(),
                  instructions,
                  deadline,
                  formats,
                  exp,
                  JSON.writeValueAsString(rubric),
                  status,
                  questId);
              if (publish)
                update(c, "UPDATE learning_modules SET status='PUBLISHED' WHERE id=?", moduleId);
              return questId;
            });
    changed();
    return id;
  }

  public static Set<String> parseTypes(String types) {
    Set<String> result = new HashSet<>();
    if (types != null)
      for (String t : types.toLowerCase(Locale.ROOT).split(","))
        if (!t.isBlank()) result.add(t.strip().replaceFirst("^\\.", ""));
    return result;
  }

  public long submit(User u, long questId, Path source) {
    long id =
        db.transaction(
            c -> {
              role(c, u, "STUDENT");
              var q = quest(c, questId);
              one(
                  c,
                  "SELECT * FROM class_members WHERE class_id=? AND student_id=? FOR UPDATE",
                  q.id("class_id"),
                  u.id());
              require(q.text("status").equals("PUBLISHED"), "Quest belum dipublikasikan.");
              require(
                  !LocalDateTime.now().isAfter(q.time("deadline")),
                  "Tenggat pengumpulan telah berakhir.");
              var previous =
                  list(
                      c,
                      "SELECT * FROM submissions WHERE quest_id=? AND student_id=? ORDER BY version"
                          + " DESC",
                      questId,
                      u.id());
              require(
                  previous.isEmpty()
                      || previous.getFirst().text("status").equals("REVISION_REQUIRED"),
                  "Pengumpulan baru hanya tersedia jika belum mengirim atau dosen meminta revisi.");
              int version = previous.isEmpty() ? 1 : previous.getFirst().number("version") + 1;
              Path stored = files.store(source, parseTypes(q.text("allowed_types")));
              try {
                return insert(
                    c,
                    "INSERT INTO"
                        + " submissions(quest_id,student_id,version,file_path,submitted_at,status)"
                        + " VALUES(?,?,?,?,?,'REVIEW_PENDING')",
                    questId,
                    u.id(),
                    version,
                    stored.toString(),
                    LocalDateTime.now());
              } catch (Exception e) {
                Files.deleteIfExists(stored);
                throw e;
              }
            });
    changed();
    return id;
  }

  public List<Row> submissions(User u, long classId) {
    return db.read(
        c -> {
          var room = access(c, u, classId);
          return list(
              c,
              "SELECT s.*,q.title AS quest_title,q.exp_reward,u.name AS student_name FROM"
                  + " submissions s JOIN quests q ON q.id=s.quest_id JOIN learning_modules m ON"
                  + " m.id=q.module_id JOIN users u ON u.id=s.student_id WHERE m.class_id=?"
                  + (room.id("lecturer_id") == u.id() ? "" : " AND s.student_id=" + u.id())
                  + " ORDER BY s.submitted_at DESC,s.version DESC",
              classId);
        });
  }

  private Row submissionAccess(Connection c, User u, long id) throws Exception {
    var row =
        one(
            c,
            "SELECT s.*,q.description,q.rubric_json,m.class_id,c.lecturer_id FROM submissions s"
                + " JOIN quests q ON q.id=s.quest_id JOIN learning_modules m ON m.id=q.module_id"
                + " JOIN classes c ON c.id=m.class_id WHERE s.id=?",
            id);
    require(
        row.id("lecturer_id") == u.id() || row.id("student_id") == u.id(),
        "Tidak berhak mengakses pengumpulan ini.");
    return row;
  }

  public Path submissionFile(User u, long id) {
    return db.read(c -> Path.of(submissionAccess(c, u, id).text("file_path")));
  }

  public List<Row> evaluations(User u, long submissionId) {
    return db.read(
        c -> {
          submissionAccess(c, u, submissionId);
          return list(
              c,
              "SELECT * FROM ai_evaluations WHERE submission_id=? ORDER BY id DESC",
              submissionId);
        });
  }

  public String evaluate(User u, long submissionId) {
    var s =
        db.read(
            c -> {
              var row = submissionAccess(c, u, submissionId);
              require(
                  row.text("status").equals("REVIEW_PENDING"),
                  "Evaluasi hanya untuk tugas menunggu tinjauan.");
              return row;
            });
    String extracted = "";
    AiService.Evaluation evaluation = null;
    String error = null;
    try {
      extracted = new DocumentService().extract(Path.of(s.text("file_path")));
      evaluation =
          new AiService().evaluate(extracted, s.text("description"), s.text("rubric_json"));
    } catch (Exception e) {
      error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
    final String text = extracted, failure = error;
    final var result = evaluation;
    db.transaction(
        c -> {
          submissionAccess(c, u, submissionId);
          insert(
              c,
              "INSERT INTO"
                  + " ai_evaluations(submission_id,status,extracted_text,score_draft,feedback,error_message,created_at)"
                  + " VALUES(?,?,?,?,?,?,?)",
              submissionId,
              result == null ? "FAILED" : "SUCCESS",
              text,
              result == null ? null : result.score(),
              result == null ? null : result.feedback(),
              failure,
              LocalDateTime.now());
          return null;
        });
    changed();
    return result == null
        ? "Evaluasi otomatis tidak tersedia: " + error
        : "Rekomendasi AI tersimpan. Menunggu keputusan dosen.";
  }

  public void review(User u, long submissionId, Integer score, String feedback, boolean revision) {
    require(feedback != null && !feedback.isBlank(), "Catatan dosen wajib diisi.");
    require(revision || score != null && score >= 0 && score <= 100, "Nilai akhir harus 0-100.");
    db.transaction(
        c -> {
          var s = submissionAccess(c, u, submissionId);
          owner(c, u, s.id("class_id"));
          var member =
              one(
                  c,
                  "SELECT * FROM class_members WHERE class_id=? AND student_id=? FOR UPDATE",
                  s.id("class_id"),
                  s.id("student_id"));
          s = one(c, "SELECT * FROM submissions WHERE id=? FOR UPDATE", submissionId);
          var latest =
              one(
                  c,
                  "SELECT MAX(version) AS latest FROM submissions WHERE quest_id=? AND"
                      + " student_id=?",
                  s.id("quest_id"),
                  s.id("student_id"));
          require(
              s.number("version") == latest.number("latest"),
              "Hanya versi terbaru yang dapat ditinjau.");
          if (s.text("status").equals("APPROVED") && !revision)
            return null; // Idempotent: persetujuan ulang tidak mengubah nilai atau EXP.
          require(s.text("status").equals("REVIEW_PENDING"), "Pengumpulan sudah ditinjau.");
          update(
              c,
              "UPDATE submissions SET"
                  + " status=?,final_score=?,final_feedback=?,reviewed_by=?,reviewed_at=? WHERE"
                  + " id=?",
              revision ? "REVISION_REQUIRED" : "APPROVED",
              revision ? null : score,
              feedback.strip(),
              u.id(),
              LocalDateTime.now(),
              submissionId);
          if (!revision
              && list(
                      c,
                      "SELECT id FROM exp_transactions WHERE member_id=? AND quest_id=?",
                      member.id("id"),
                      s.id("quest_id"))
                  .isEmpty()) {
            int reward = quest(c, s.id("quest_id")).number("exp_reward");
            insert(
                c,
                "INSERT INTO exp_transactions(member_id,quest_id,exp_amount,awarded_at)"
                    + " VALUES(?,?,?,?)",
                member.id("id"),
                s.id("quest_id"),
                reward,
                LocalDateTime.now());
            update(
                c,
                "UPDATE class_members SET exp_total=exp_total+? WHERE id=?",
                reward,
                member.id("id"));
            int exp = member.number("exp_total") + reward;
            award(c, member.id("id"), "FIRST_QUEST");
            if (exp >= 300) award(c, member.id("id"), "ADEPT");
            if (exp >= 700) award(c, member.id("id"), "ELITE");
            if (exp >= 1000) award(c, member.id("id"), "LEGENDARY");
            if (one(
                        c,
                        "SELECT COUNT(*) AS total FROM exp_transactions WHERE member_id=?",
                        member.id("id"))
                    .number("total")
                >= 5) award(c, member.id("id"), "QUEST_MASTER");
          }
          return null;
        });
    changed();
  }

  private void award(Connection c, long member, String badge) throws Exception {
    if (list(c, "SELECT id FROM achievements WHERE member_id=? AND badge_code=?", member, badge)
        .isEmpty())
      insert(
          c,
          "INSERT INTO achievements(member_id,badge_code,earned_at) VALUES(?,?,?)",
          member,
          badge,
          LocalDateTime.now());
  }

  public List<Row> leaderboard(User u, long classId) {
    return db.read(
        c -> {
          access(c, u, classId);
          return list(
              c,
              "SELECT cm.*,u.name,u.email,(SELECT COUNT(*) FROM exp_transactions e WHERE"
                  + " e.member_id=cm.id) AS completed,(SELECT s.status FROM submissions s JOIN"
                  + " quests q ON q.id=s.quest_id JOIN learning_modules m ON m.id=q.module_id WHERE"
                  + " s.student_id=cm.student_id AND m.class_id=cm.class_id ORDER BY s.submitted_at"
                  + " DESC,s.version DESC LIMIT 1) AS latest_status FROM class_members cm JOIN"
                  + " users u ON u.id=cm.student_id WHERE cm.class_id=? ORDER BY cm.exp_total"
                  + " DESC,u.name,cm.id",
              classId);
        });
  }

  public List<Row> badges(User u, long classId) {
    return db.read(
        c -> {
          access(c, u, classId);
          return list(
              c,
              "SELECT a.* FROM achievements a JOIN class_members cm ON cm.id=a.member_id WHERE"
                  + " cm.class_id=? AND cm.student_id=? ORDER BY a.id",
              classId,
              u.id());
        });
  }
}
