package com.hsf;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class LicenseWebSocketClient extends WebSocketClient {
    
    private Controller controller;
    private MainController mainController;
    
    public LicenseWebSocketClient(String serverUrl, String licenseKey, String deviceId, Controller controller) throws Exception {
        super(new URI(serverUrl + "/ws/license?licenseKey=" + licenseKey + "&deviceId=" + deviceId));
        this.controller = controller;
        String fullUrl = serverUrl + "/ws/license?licenseKey=" + licenseKey + "&deviceId=" + deviceId;
        System.out.println("WebSocket connecting to: " + fullUrl);
        
        // Thêm headers để tránh redirect
        this.addHeader("Origin", "http://localhost");
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        System.out.println("MainController set in WebSocket client");
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

        System.out.println("WebSocket CONNECTION OPENED");
        System.out.println("HTTP Status: " + handshakedata.getHttpStatus());
        System.out.println("HTTP Status Message: " + handshakedata.getHttpStatusMessage());

    }

    @Override
    public void onMessage(String message) {

        System.out.println("WebSocket MESSAGE RECEIVED");
        System.out.println("Raw message: " + message);
        System.out.println("Message length: " + message.length());

        
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            System.out.println("Parsed JSON: " + json);
            
            if (json.has("type")) {
                String type = json.get("type").getAsString();
                System.out.println("Message type: " + type);
                
                if ("LOCK".equals(type)) {
                    String reason = json.has("reason") ? json.get("reason").getAsString() : "License has been revoked";
                    System.out.println("LOCK message detected! Reason: " + reason);
                    handleLicenseLock(reason);
                } else {
                    System.out.println("Unknown message type: " + type);
                }
            } else {
                System.out.println("Message has no 'type' field");
            }
        } catch (Exception e) {
            System.err.println("Error parsing message:");
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.err.println("=== WebSocket CLOSED ===");
        System.err.println("Code: " + code);
        System.err.println("Reason: " + reason);
        System.err.println("Remote closed: " + remote);
        
        // Nếu bị đóng bất thường, thử kết nối lại
        if (code != 1000 && !remote) { // 1000 = normal closure
            System.err.println("Abnormal closure detected, attempting reconnect...");
            try {
                Thread.sleep(2000);
                this.reconnect();
            } catch (Exception e) {
                System.err.println("Reconnect failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("=== WebSocket ERROR ===");
        System.err.println("WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }

    private void handleLicenseLock(String reason) {
        System.out.println("=== handleLicenseLock called ===");
        System.out.println("Reason: " + reason);
        System.out.println("MainController is null? " + (mainController == null));
        
        // Dừng heartbeat thread trước
        if (controller != null) {
            controller.stopHeartbeat();
            System.out.println("Heartbeat stopped");
        }
        
        // Hiển thị alert trên MainController nếu có, nếu không thì hiển thị trực tiếp
        if (mainController != null) {
            System.out.println("Calling mainController.showLockAlert()");
            mainController.showLockAlert(reason);
        } else {
            System.out.println("MainController is null, showing alert directly");
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("License bị thu hồi");
                alert.setHeaderText("License của bạn đã bị thu hồi!");
                alert.setContentText(reason);
                alert.showAndWait();
                
                System.exit(0);
            });
        }
    }
}
