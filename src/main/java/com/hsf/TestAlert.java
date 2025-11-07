package com.hsf;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Test class để kiểm tra Alert hiển thị đúng không
 * Chạy: mvn compile exec:java -Dexec.mainClass="com.hsf.TestAlert"
 */
public class TestAlert extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("License bị thu hồi");
            alert.setHeaderText("License của bạn đã bị thu hồi!");
            alert.setContentText("License đã bị vô hiệu hóa bởi quản trị viên");
            alert.showAndWait();
            
            System.out.println("Alert closed");
            System.exit(0);
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
