package com.hsf;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class LicenseClient extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("view"), 640, 480);
        stage.setScene(scene);
        // Gắn sự kiện đóng cửa sổ để gọi deactivate
        stage.setOnCloseRequest(event -> {
            try {
                new Controller().deactivate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        stage.show();
    }
    
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(LicenseClient.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

