package com.hsf;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
// cleaned unused imports
// import java.nio.file.*;
// import java.util.concurrent.*;


// import java.time.*;

public class LicenseClient extends Application {
    private static final String SERVER_URL = "http://localhost:8080/api/license";
    private static final String LICENSE_KEY = "LICENSE-001";
    private static final String PRODUCT_NAME = "Product 1";

    public static String getDeviceId() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
        byte[] mac = ni.getHardwareAddress();
        StringBuilder sb = new StringBuilder();
        for (byte b : mac) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    public static void activate() throws Exception {
        String deviceId = getDeviceId();
        String encodedLicenseKey = URLEncoder.encode(LICENSE_KEY, StandardCharsets.UTF_8);
        String encodedDeviceId = URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
        String encodedProductName = URLEncoder.encode(PRODUCT_NAME, StandardCharsets.UTF_8);
        String url = SERVER_URL + "/activate?licenseKey=" + encodedLicenseKey + "&deviceId=" + encodedDeviceId + "&productName=" + encodedProductName;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void deactivate() throws Exception {
        String deviceId = getDeviceId();
        String encodedLicenseKey = URLEncoder.encode(LICENSE_KEY, StandardCharsets.UTF_8);
        String encodedDeviceId = URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
        String url = SERVER_URL + "/deactivate?licenseKey=" + encodedLicenseKey + "&deviceId=" + encodedDeviceId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void heartbeat() throws Exception {
        String deviceId = getDeviceId();
        String encodedLicenseKey = URLEncoder.encode(LICENSE_KEY, StandardCharsets.UTF_8);
        String encodedDeviceId = URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
        String url = SERVER_URL + "/heartbeat?licenseKey=" + encodedLicenseKey + "&deviceId=" + encodedDeviceId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Thread heartbeatThread = new Thread(() -> {
        try {
            while (true) {
                heartbeat();
                Thread.sleep(5000); // 5 seconds
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    @Override
    public void start(Stage primaryStage) {
        TextField tfLicense = new TextField();
        tfLicense.setPromptText("License Key");
        Button btnActivate = new Button("Activate");
        btnActivate.setOnAction(e -> {
            try {
                activate();
                System.out.println("Activated");
            } catch (Exception ex) {
                System.out.println("Activation error: " + ex.getMessage());
            }
        });

        heartbeatThread.start();

        VBox vbox = new VBox(10, tfLicense, btnActivate);
        Scene scene = new Scene(vbox, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("License Client");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

