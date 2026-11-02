package id.ac.pens.retroquest.ui;

import id.ac.pens.retroquest.service.PdfPreviewService;
import java.nio.file.*;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;

/** An independent original-document window. PDF work stays off the FX thread. */
public final class PdfViewer {
  private final PdfPreviewService service = new PdfPreviewService();
  private final Stage stage = new Stage();
  private final ImageView image = new ImageView();
  private final Label position = new Label("Memuat PDF…");
  private final Label message = new Label();
  private final Button previous = new Button("← Sebelumnya");
  private final Button next = new Button("Berikutnya →");
  private final Button retry = new Button("Coba lagi");
  private final ProgressIndicator loading = new ProgressIndicator();
  private final ComboBox<String> zoom = new ComboBox<>();
  private Path file;
  private int page;
  private int count;
  private int generation;
  private Task<PdfPreviewService.Page> pending;

  public void open(Window owner, Path source) {
    file = source.toAbsolutePath().normalize();
    stage.initOwner(owner);
    stage.setTitle("PDF asli · RetroQuest");
    image.setPreserveRatio(true);
    image.setSmooth(true);
    zoom.getItems().addAll("75%", "100%", "125%", "150%", "200%");
    zoom.setValue("100%");
    var picture = new VBox(image);
    picture.setAlignment(Pos.TOP_CENTER);
    picture.setPadding(new Insets(18));
    var viewport = new ScrollPane(picture);
    viewport.setFitToWidth(true);
    viewport
        .viewportBoundsProperty()
        .addListener((o, oldValue, newValue) -> fit(newValue.getWidth()));
    zoom.setOnAction(e -> fit(viewport.getViewportBounds().getWidth()));
    previous.setOnAction(e -> render(page - 1));
    next.setOnAction(e -> render(page + 1));
    retry.setOnAction(e -> render(page));
    retry.setVisible(false);
    retry.managedProperty().bind(retry.visibleProperty());
    var save = new Button("Simpan salinan PDF");
    save.setOnAction(e -> saveCopy());
    var space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    var toolbar = new HBox(12, previous, position, next, space, zoom, save);
    toolbar.setAlignment(Pos.CENTER_LEFT);
    toolbar.setPadding(new Insets(16));
    message.setWrapText(true);
    var feedback = new VBox(12, loading, message, retry);
    feedback.setAlignment(Pos.CENTER);
    feedback.setMaxWidth(650);
    feedback.setMaxHeight(Region.USE_PREF_SIZE);
    var center = new StackPane(viewport, feedback);
    var layout = new BorderPane(center, toolbar, null, null, null);
    var scene = new Scene(layout, 1050, 780);
    scene.getStylesheets().add(PdfViewer.class.getResource("/ui/retroquest.css").toExternalForm());
    stage.setScene(scene);
    stage.setOnHidden(
        e -> {
          generation++;
          if (pending != null) pending.cancel();
          image.setImage(null);
        });
    stage.show();
    render(0);
  }

  private void fit(double width) {
    double scale = Integer.parseInt(zoom.getValue().replace("%", "")) / 100.0;
    image.setFitWidth(Math.max(100, width - 42) * scale);
  }

  private void render(int target) {
    if (count > 0 && (target < 0 || target >= count)) return;
    page = target;
    int token = ++generation;
    if (pending != null) pending.cancel();
    previous.setDisable(true);
    next.setDisable(true);
    loading.setVisible(true);
    loading.setManaged(true);
    retry.setVisible(false);
    message.setText("");
    position.setText("Memuat halaman " + (target + 1) + "…");
    pending =
        new Task<>() {
          @Override
          protected PdfPreviewService.Page call() throws Exception {
            return service.render(file, target);
          }
        };
    var task = pending;
    task.setOnSucceeded(
        e -> {
          if (token != generation || !stage.isShowing()) return;
          var result = task.getValue();
          var pixels = result.image();
          var bitmap = new WritableImage(pixels.getWidth(), pixels.getHeight());
          int[] argb =
              pixels.getRGB(
                  0, 0, pixels.getWidth(), pixels.getHeight(), null, 0, pixels.getWidth());
          bitmap
              .getPixelWriter()
              .setPixels(
                  0,
                  0,
                  pixels.getWidth(),
                  pixels.getHeight(),
                  PixelFormat.getIntArgbInstance(),
                  argb,
                  0,
                  pixels.getWidth());
          image.setImage(bitmap);
          count = result.count();
          position.setText("Halaman " + (page + 1) + " / " + count);
          previous.setDisable(page == 0);
          next.setDisable(page == count - 1);
          loading.setVisible(false);
          loading.setManaged(false);
        });
    task.setOnFailed(
        e -> {
          if (token != generation || !stage.isShowing()) return;
          loading.setVisible(false);
          loading.setManaged(false);
          image.setImage(null);
          position.setText("PDF belum dapat ditampilkan");
          message.setText("Tidak dapat membaca PDF: " + task.getException().getMessage());
          retry.setVisible(true);
        });
    var worker = new Thread(task, "retroquest-pdf-viewer");
    worker.setDaemon(true);
    worker.start();
  }

  private void saveCopy() {
    var chooser = new FileChooser();
    chooser.setTitle("Simpan salinan berkas PDF asli");
    chooser.setInitialFileName(file.getFileName().toString());
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
    var chosen = chooser.showSaveDialog(stage);
    if (chosen == null) return;
    try {
      Path destination = chosen.toPath().toAbsolutePath().normalize();
      if (Files.exists(destination) && Files.isSameFile(file, destination)) return;
      Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception ex) {
      var alert =
          new Alert(
              Alert.AlertType.ERROR,
              "Salinan PDF tidak dapat disimpan: " + ex.getMessage(),
              ButtonType.OK);
      alert.initOwner(stage);
      alert.showAndWait();
    }
  }
}
