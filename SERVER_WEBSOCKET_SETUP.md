# Hướng dẫn cấu hình WebSocket Server cho License Management

## Vấn đề hiện tại
```
GET http://localhost:8080/ws/license → 200 OK (HTML login page)
```

Server đang redirect WebSocket endpoint về trang login thay vì xử lý WebSocket connection.

## Giải pháp: Cấu hình Spring Boot WebSocket

### Bước 1: Thêm Dependencies vào pom.xml

```xml
<dependencies>
    <!-- WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- JSON processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### Bước 2: Tạo WebSocket Handler

```java
package com.yourpackage.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LicenseWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Lấy licenseKey và deviceId từ query parameters
        String query = session.getUri().getQuery();
        String licenseKey = extractParam(query, "licenseKey");
        String deviceId = extractParam(query, "deviceId");
        
        if (licenseKey == null || deviceId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        
        String key = licenseKey + "-" + deviceId;
        sessions.put(key, session);
        
        System.out.println("WebSocket connected: " + key);
        System.out.println("Total active sessions: " + sessions.size());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String query = session.getUri().getQuery();
        String licenseKey = extractParam(query, "licenseKey");
        String deviceId = extractParam(query, "deviceId");
        String key = licenseKey + "-" + deviceId;
        
        sessions.remove(key);
        System.out.println("WebSocket disconnected: " + key);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Xử lý message từ client nếu cần
        System.out.println("Received from client: " + message.getPayload());
    }
    
    /**
     * Gửi message LOCK đến client để vô hiệu hóa license
     */
    public void sendLockMessage(String licenseKey, String deviceId, String reason) {
        String key = licenseKey + "-" + deviceId;
        WebSocketSession session = sessions.get(key);
        
        if (session != null && session.isOpen()) {
            try {
                Map<String, String> message = Map.of(
                    "type", "LOCK",
                    "reason", reason
                );
                
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
                
                System.out.println("Sent LOCK message to: " + key);
                System.out.println("Message: " + json);
            } catch (IOException e) {
                System.err.println("Failed to send LOCK message: " + e.getMessage());
            }
        } else {
            System.out.println("Session not found or closed: " + key);
        }
    }
    
    private String extractParam(String query, String paramName) {
        if (query == null) return null;
        
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        return null;
    }
    
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
```

### Bước 3: Cấu hình WebSocket Config

```java
package com.yourpackage.config;

import com.yourpackage.websocket.LicenseWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private LicenseWebSocketHandler licenseWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(licenseWebSocketHandler, "/ws/license")
                .setAllowedOrigins("*"); // Trong production nên chỉ định cụ thể
    }
}
```

### Bước 4: Cấu hình Spring Security (QUAN TRỌNG!)

```java
package com.yourpackage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll()  // ← CHO PHÉP WEBSOCKET
                .requestMatchers("/api/license/**").permitAll()  // ← API license
                .requestMatchers("/guest/**").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/ws/**")  // ← DISABLE CSRF CHO WEBSOCKET
                .ignoringRequestMatchers("/api/license/**")
            );
        
        return http.build();
    }
}
```

### Bước 5: Tạo REST API để admin vô hiệu hóa license

```java
package com.yourpackage.controller;

import com.yourpackage.websocket.LicenseWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private LicenseWebSocketHandler webSocketHandler;
    
    /**
     * API để admin vô hiệu hóa license từ xa
     * 
     * POST http://localhost:8080/api/admin/revoke-license
     * Body: {
     *   "licenseKey": "ABC123",
     *   "deviceId": "DEVICE456",
     *   "reason": "License đã hết hạn"
     * }
     */
    @PostMapping("/revoke-license")
    public ResponseEntity<?> revokeLicense(@RequestBody Map<String, String> request) {
        String licenseKey = request.get("licenseKey");
        String deviceId = request.get("deviceId");
        String reason = request.get("reason");
        
        if (licenseKey == null || deviceId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "licenseKey and deviceId are required"));
        }
        
        if (reason == null) {
            reason = "License đã bị vô hiệu hóa bởi quản trị viên";
        }
        
        // Gửi message LOCK đến client qua WebSocket
        webSocketHandler.sendLockMessage(licenseKey, deviceId, reason);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "LOCK message sent to client",
            "activeConnections", webSocketHandler.getActiveSessionCount()
        ));
    }
    
    @GetMapping("/active-connections")
    public ResponseEntity<?> getActiveConnections() {
        return ResponseEntity.ok(Map.of(
            "activeConnections", webSocketHandler.getActiveSessionCount()
        ));
    }
}
```

### Bước 6: Test WebSocket Endpoint

#### Sau khi khởi động server, kiểm tra endpoint:

```powershell
# Kiểm tra xem WebSocket endpoint có trả về upgrade request không
Invoke-WebRequest -Uri "http://localhost:8080/ws/license?licenseKey=TEST&deviceId=ABC123" -Method GET
```

**Kết quả mong đợi:** 
- Không phải trang login HTML
- Hoặc có thể 400/405 (method not allowed) - điều này OK vì WebSocket cần upgrade header

#### Test kết nối từ client:
```powershell
cd d:\hsfclientapp\clientapp
mvn javafx:run
```

**Console phải hiển thị:**
```
=== Starting WebSocket connection ===
WebSocket connecting to: ws://localhost:8080/ws/license?licenseKey=XXX&deviceId=YYY
WebSocket client created
MainController set in WebSocket client
WebSocket connected successfully  ← QUAN TRỌNG
```

#### Test gửi message LOCK:
```powershell
# Gửi POST request để revoke license
Invoke-WebRequest -Uri "http://localhost:8080/api/admin/revoke-license" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"licenseKey":"XXX","deviceId":"YYY","reason":"Test revoke"}'
```

**Client phải hiển thị Alert và thoát ứng dụng**

## Checklist

- [ ] Thêm `spring-boot-starter-websocket` dependency
- [ ] Tạo `LicenseWebSocketHandler` class
- [ ] Tạo `WebSocketConfig` với `@EnableWebSocket`
- [ ] Cấu hình Spring Security cho phép `/ws/**`
- [ ] Disable CSRF cho WebSocket endpoint
- [ ] Tạo API `/api/admin/revoke-license` để test
- [ ] Restart server
- [ ] Test với client: `mvn javafx:run`
- [ ] Kiểm tra log: "WebSocket connected successfully"
- [ ] Test revoke: Gọi API admin và xem Alert hiển thị

## Troubleshooting

### Vẫn thấy trang login
→ Spring Security config chưa đúng, cần `permitAll()` cho `/ws/**`

### Lỗi 403 Forbidden
→ CSRF chưa disable cho WebSocket, thêm `.ignoringRequestMatchers("/ws/**")`

### Lỗi 404 Not Found
→ WebSocket handler chưa được register, kiểm tra `WebSocketConfig`

### Client hiển thị "WebSocket closed: Invalid status code 302"
→ Server đang redirect, cần fix Spring Security config

### Gửi LOCK message nhưng client không nhận
→ Kiểm tra session có tồn tại không, xem log "Session not found or closed"
