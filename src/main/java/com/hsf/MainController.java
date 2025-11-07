package com.hsf;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MainController {
    
    @FXML
    private Label licenseLabel;
    
    @FXML
    private Label deviceLabel;
    
    @FXML
    private Label statusLabel;
    
    private String license;
    private String deviceId;
    private Controller parentController;
    
    public void initialize(String license, String deviceId, Controller parentController) {
        this.license = license;
        this.deviceId = deviceId;
        this.parentController = parentController;
        
        licenseLabel.setText("License: " + license);
        deviceLabel.setText("Device ID: " + deviceId);
    }
    
    @FXML
    public void deactivate() {
        try {
            if (parentController != null) {
                parentController.deactivate();
            }
            
            // Navigate back to login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) licenseLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Kích hoạt License");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void showLockAlert(String reason) {
        System.out.println("=== showLockAlert called in MainController ===");
        System.out.println("Reason: " + reason);
        
        Platform.runLater(() -> {
            System.out.println("Running on JavaFX thread");
            statusLabel.setText("Trạng thái: Bị khóa");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: red;");
            System.out.println("Status label updated");
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("License bị thu hồi");
            alert.setHeaderText("License của bạn đã bị thu hồi!");
            alert.setContentText(reason);
            System.out.println("About to show alert");
            alert.showAndWait();
            
            System.out.println("Alert closed, exiting app");
            System.exit(0);
        });
    }
}
