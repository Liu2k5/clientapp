package com.hsf;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class Controller {
    private static final String SERVER_URL = "http://localhost:8080/api/license";
    private static final String PRODUCT_NAME = "Product 1";
    
    @FXML
    private TextField textField;

    private String license = "";
    private Thread heartbeatThread;  // Lưu reference toàn cục
    private volatile boolean isRunning = false;  // Flag để dừng thread an toàn

    public static String getDeviceId() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
        byte[] mac = ni.getHardwareAddress();
        StringBuilder sb = new StringBuilder();
        for (byte b : mac) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    @FXML
    public void activate(ActionEvent event) throws Exception {
        license = textField.getText();

        String deviceId = getDeviceId();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("licenseKey", license);
        requestBody.put("deviceId", deviceId);
        requestBody.put("productName", PRODUCT_NAME);
        Gson gson = new Gson();
        String jsonBody = gson.toJson(requestBody);
        try {
            String url = SERVER_URL + "/activate";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Kích hoạt thành công");

                // Start heartbeat thread chỉ nếu chưa chạy
                if (heartbeatThread == null || !heartbeatThread.isAlive()) {
                    isRunning = true;
                    heartbeatThread = new Thread(() -> {
                        try {
                            while (isRunning) {
                                heartbeat();
                                System.out.println("Heartbeat sent");
                                Thread.sleep(10000); // 10 seconds
                            }
                        } catch (InterruptedException e) {
                            System.out.println("Heartbeat thread interrupted");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    heartbeatThread.setDaemon(true);
                    heartbeatThread.start();
                }
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Kích hoạt thành công");
                alert.setContentText("Kích hoạt li-xăng thành công.");
                alert.showAndWait();

            } else {
                String errorMsg = getErrorMessageFromResponse(response.body());
                System.out.println("Kích hoạt thất bại: " + errorMsg);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Kích hoạt thất bại");
                alert.setContentText("Kích hoạt li-xăng thất bại: " + errorMsg);
                alert.showAndWait();
            }
        } catch (ConnectException e) {
            System.out.println("Không thể kết nối đến máy chủ: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi kết nối");
            alert.setContentText("Không thể kết nối đến máy chủ: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void deactivate() throws Exception {
        String deviceId = getDeviceId();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("licenseKey", license);
        requestBody.put("deviceId", deviceId);
        Gson gson = new Gson();
        String jsonBody = gson.toJson(requestBody);
        String url = SERVER_URL + "/deactivate";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            System.out.println("Hủy kích hoạt thành công");
            // Stop heartbeat thread
            isRunning = false;
            if (heartbeatThread != null && heartbeatThread.isAlive()) {
                heartbeatThread.interrupt();
            }
        } else {
            String errorMsg = getErrorMessageFromResponse(response.body());
            System.out.println("Hủy kích hoạt thất bại: " + errorMsg);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Hủy kích hoạt thất bại");
            alert.setContentText("Hủy kích hoạt li-xăng thất bại: " + errorMsg);
            alert.showAndWait();
        }
    }

    public void heartbeat() throws Exception {
        String deviceId = getDeviceId();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("licenseKey", license);
        requestBody.put("deviceId", deviceId);
        Gson gson = new Gson();
        String jsonBody = gson.toJson(requestBody);
        String url = SERVER_URL + "/heartbeat";  // Sửa: Thêm /api/license nếu cần
        
        System.out.println("Sending heartbeat to: " + url);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            String errorMsg = getErrorMessageFromResponse(response.body());
            System.out.println("Heartbeat thất bại: " + errorMsg);
        }
    }

    private String getErrorMessageFromResponse(String responseBody) {
        Gson gson = new Gson();
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json.has("message")) {
                return json.get("message").getAsString();
            }
        } catch (Exception ex) {
            // fallback
        }
        return responseBody;
    }
}