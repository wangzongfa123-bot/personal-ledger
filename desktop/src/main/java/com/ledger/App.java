package com.ledger;

import com.ledger.ui.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController();
        Scene scene = new Scene(controller.getRoot(), 980, 640);

        var css = getClass().getResource("/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("个人记账本 · 桌面版");
        stage.setScene(scene);
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.show();

        controller.bootstrap();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
