package id.ac.pens.retroquest;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.pens.retroquest.dao.Database;
import id.ac.pens.retroquest.service.AuthService;
import id.ac.pens.retroquest.service.DemoData;
import id.ac.pens.retroquest.service.QuestService;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Integration test of our JavaFX scene graph; opt-in because a desktop is required. */
@EnabledIfSystemProperty(named = "retroquest.guiTest", matches = "true")
class GuiSmokeTest {
  private Stage stage;
  private Parent root;

  private <T> T fx(Callable<T> action) throws Exception {
    var task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(20, TimeUnit.SECONDS);
  }

  private Button find(Parent parent, String text) {
    for (Node child : parent.getChildrenUnmodifiable()) {
      if (child instanceof Button b && b.getText().equals(text)) return b;
      if (child instanceof Parent p) {
        Button result = find(p, text);
        if (result != null) return result;
      }
    }
    return null;
  }

  private void click(String text) throws Exception {
    fx(
        () -> {
          Button button = find(root, text);
          assertNotNull(button, "Button missing: " + text);
          button.fire();
          return null;
        });
    idle();
  }

  private void idle() throws Exception {
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    while (System.nanoTime() < end) {
      Thread.sleep(100);
      if (fx(() -> !root.isDisabled())) {
        Thread.sleep(150);
        if (fx(() -> !root.isDisabled())) return;
      }
    }
    fail("JavaFX background task did not finish");
  }

  private void snapshot(String name) throws Exception {
    snapshotScene(stage.getScene(), name);
  }

  private void snapshotScene(Scene scene, String name) throws Exception {
    fx(
        () -> {
          scene.getRoot().applyCss();
          scene.getRoot().layout();
          var image = scene.snapshot(null);
          var png =
              new BufferedImage(
                  (int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
          for (int y = 0; y < png.getHeight(); y++)
            for (int x = 0; x < png.getWidth(); x++)
              png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
          Path folder = Path.of("docs/screenshots");
          Files.createDirectories(folder);
          ImageIO.write(png, "png", folder.resolve(name + ".png").toFile());
          return null;
        });
  }

  private void modal(String buttonText, String screenshot) throws Exception {
    Platform.runLater(
        () -> {
          Button button = find(root, buttonText);
          assertNotNull(button);
          button.fire();
        });
    javafx.stage.Window dialog = null;
    for (int i = 0; i < 100 && dialog == null; i++) {
      Thread.sleep(100);
      dialog =
          fx(
              () ->
                  javafx.stage.Window.getWindows().stream()
                      .filter(w -> w != stage && w.isShowing())
                      .findFirst()
                      .orElse(null));
    }
    assertNotNull(dialog, "Dialog did not open");
    Thread.sleep(300);
    snapshotScene(dialog.getScene(), screenshot);
    var target = dialog;
    fx(
        () -> {
          target.hide();
          return null;
        });
    idle();
  }

  private boolean hasLabel(Parent parent, String text) {
    for (Node child : parent.getChildrenUnmodifiable()) {
      if (child instanceof Label l && l.getText().equals(text)) return true;
      if (child instanceof Parent p && hasLabel(p, text)) return true;
    }
    return false;
  }

  private void pdfOriginalFlow() throws Exception {
    Platform.runLater(() -> find(root, "Tinjau berkas & nilai →").fire());
    javafx.stage.Window review = null;
    for (int i = 0; i < 100 && review == null; i++) {
      Thread.sleep(100);
      review =
          fx(
              () ->
                  javafx.stage.Window.getWindows().stream()
                      .filter(w -> w != stage && w.isShowing())
                      .findFirst()
                      .orElse(null));
    }
    assertNotNull(review);
    var dialog = review;
    fx(
        () -> {
          find(dialog.getScene().getRoot(), "Buka berkas asli").fire();
          return null;
        });
    var viewer =
        fx(
            () ->
                javafx.stage.Window.getWindows().stream()
                    .filter(w -> w != stage && w != dialog && w.isShowing())
                    .findFirst()
                    .orElse(null));
    assertNotNull(viewer, "PDF harus dibuka dalam viewer aplikasi, tanpa browser");
    assertEquals(
        dialog, ((Stage) viewer).getOwner(), "Viewer harus dapat digunakan dari dialog penilaian");
    for (int i = 0;
        i < 100 && !fx(() -> hasLabel(viewer.getScene().getRoot(), "Halaman 1 / 2"));
        i++) Thread.sleep(100);
    assertTrue(fx(() -> hasLabel(viewer.getScene().getRoot(), "Halaman 1 / 2")));
    snapshotScene(viewer.getScene(), "13-pdf-original");
    fx(
        () -> {
          find(viewer.getScene().getRoot(), "Berikutnya →").fire();
          return null;
        });
    for (int i = 0;
        i < 100 && !fx(() -> hasLabel(viewer.getScene().getRoot(), "Halaman 2 / 2"));
        i++) Thread.sleep(100);
    assertTrue(fx(() -> hasLabel(viewer.getScene().getRoot(), "Halaman 2 / 2")));
    fx(
        () -> {
          viewer.hide();
          dialog.hide();
          return null;
        });
    idle();
  }

  @Test
  void studentAndLecturerScreensRender() throws Exception {
    var ready = new CountDownLatch(1);
    Platform.startup(
        () -> {
          Platform.setImplicitExit(false);
          ready.countDown();
        });
    assertTrue(ready.await(10, TimeUnit.SECONDS));
    Database db =
        new Database(
            "jdbc:h2:mem:gui-"
                + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    db.initialize();
    DemoData.seed(db);
    Path sample = Files.createTempFile("retroquest-preview-test-", ".pdf");
    try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument()) {
      for (int i = 1; i <= 2; i++) {
        var page = new org.apache.pdfbox.pdmodel.PDPage();
        pdf.addPage(page);
        try (var text = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
          text.beginText();
          text.setFont(
              new org.apache.pdfbox.pdmodel.font.PDType1Font(
                  org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),
              20);
          text.newLineAtOffset(60, 700);
          text.showText("RetroQuest - original PDF page " + i);
          text.endText();
        }
      }
      pdf.save(sample.toFile());
    }
    try {
      var student = new AuthService(db).login("mahasiswa@retroquest.demo", DemoData.PASSWORD);
      var quests = new QuestService(db);
      long room = quests.classes(student).getFirst().id("id");
      quests.submit(student, quests.quests(student, room).getLast().id("id"), sample);
    } finally {
      Files.deleteIfExists(sample);
    }
    try {
      fx(
          () -> {
            stage = new Stage();
            new App().start(stage, db);
            assertNotNull(stage.getScene(), "Startup harus memasang scene pada jendela");
            assertTrue(stage.isShowing(), "Startup harus menampilkan jendela");
            root = stage.getScene().getRoot();
            return null;
          });
      idle();
      snapshot("01-login");
      click("Demo mahasiswa");
      snapshot("02-dashboard-mahasiswa");
      String[][] screens = {
        {"Quest Board", "03-quest-board"},
        {"Materi", "04-materi"},
        {"Riwayat Tugas", "05-riwayat"},
        {"Leaderboard", "06-leaderboard"},
        {"Profil", "07-profil"}
      };
      for (String[] screen : screens) {
        click(screen[0]);
        snapshot(screen[1]);
      }
      click("Keluar");
      click("Demo dosen");
      snapshot("08-dashboard-dosen");
      click("Verification Center");
      snapshot("09-verification-center");
      assertTrue(fx(() -> find(root, "Tinjau berkas & nilai →") != null));
      modal("Tinjau berkas & nilai →", "10-review-dialog");
      pdfOriginalFlow();
      click("Quest Board");
      modal("Tinjau draf →", "11-quest-editor");
      click("Materi");
      modal("+ Unggah / tulis materi", "12-module-editor");
    } finally {
      fx(
          () -> {
            stage.fireEvent(
                new javafx.stage.WindowEvent(stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
            stage.close();
            return null;
          });
      Platform.exit();
      db.read(
          c -> {
            for (var row :
                id.ac.pens.retroquest.dao.JdbcDao.list(c, "SELECT file_path FROM submissions"))
              Files.deleteIfExists(Path.of(row.text("file_path")));
            return null;
          });
    }
  }
}
