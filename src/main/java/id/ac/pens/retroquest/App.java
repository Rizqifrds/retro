package id.ac.pens.retroquest;

import id.ac.pens.retroquest.dao.Database;
import id.ac.pens.retroquest.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class App extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    start(stage, new Database());
  }

  void start(Stage stage, Database database) throws Exception {
    var loader = new FXMLLoader(App.class.getResource("/ui/main.fxml"));
    var scene = new Scene(loader.load(), 1280, 820);
    scene.getStylesheets().add(App.class.getResource("/ui/retroquest.css").toExternalForm());
    stage.setScene(scene);
    stage.setTitle("RetroQuest · Academic Adventure");
    stage.setMinWidth(1040);
    stage.setMinHeight(700);
    MainController controller = loader.getController();
    controller.start(stage, database);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
