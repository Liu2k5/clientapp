package com.hsf;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Simple WebSocket test client để debug connection
 * Chạy: mvn compile exec:java -Dexec.mainClass="com.hsf.SimpleWebSocketTest"
 */
public class SimpleWebSocketTest {
    
    public static void main(String[] args) {
        try {
            String url = "ws://localhost:8080/ws/license?licenseKey=TEST&deviceId=ABC123";
            System.out.println("=== Testing WebSocket Connection ===");
            System.out.println("URL: " + url);
            System.out.println("Attempting to connect...\n");
            
            WebSocketClient client = new WebSocketClient(new URI(url)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ SUCCESS! WebSocket connected!");
                    System.out.println("Status: " + handshakedata.getHttpStatus());
                    System.out.println("Status Message: " + handshakedata.getHttpStatusMessage());
                    
                    // Đợi 2 giây rồi đóng
                    try {
                        Thread.sleep(2000);
                        this.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("📩 Received message: " + message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("\n🔴 Connection closed");
                    System.out.println("Code: " + code);
                    System.out.println("Reason: " + reason);
                    System.out.println("Remote closed: " + remote);
                    
                    if (code == 1000) {
                        System.out.println("✅ Normal closure");
                    } else {
                        System.out.println("❌ Abnormal closure");
                    }
                    System.exit(0);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("\n❌ ERROR occurred:");
                    System.err.println("Message: " + ex.getMessage());
                    System.err.println("Class: " + ex.getClass().getName());
                    ex.printStackTrace();
                }
            };
            
            // Kết nối (blocking)
            System.out.println("Connecting...");
            boolean connected = client.connectBlocking();
            
            if (!connected) {
                System.err.println("❌ Connection failed!");
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
