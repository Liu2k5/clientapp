# Fix WebSocket 302 Redirect Error

## Vấn đề
```
WebSocket closed: Invalid status code received: 302 Status line: HTTP/1.1 302
```

Lỗi này có nghĩa server trả về HTTP 302 (redirect) thay vì upgrade connection lên WebSocket.

## Nguyên nhân
1. **Spring Security** đang chặn WebSocket endpoint
2. WebSocket endpoint URL không đúng
3. Server chưa cấu hình CORS cho WebSocket

## Giải pháp cho Server (Spring Boot)

### 1. Kiểm tra WebSocket configuration
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/license")
                .setAllowedOrigins("*")  // Cho phép tất cả origins
                .withSockJS();  // Fallback nếu WebSocket không khả dụng
    }
}
```

### 2. Cấu hình Spring Security để cho phép WebSocket
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll()  // Cho phép WebSocket
                .requestMatchers("/api/license/**").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/ws/**")  // Disable CSRF cho WebSocket
            );
        return http.build();
    }
}
```

### 3. Controller để gửi message LOCK
```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @PostMapping("/revoke-license")
    public void revokeLicense(@RequestParam String licenseKey, 
                              @RequestParam String deviceId,
                              @RequestParam String reason) {
        
        // Gửi message LOCK đến client qua WebSocket
        Map<String, String> message = new HashMap<>();
        message.put("type", "LOCK");
        message.put("reason", reason);
        
        // Gửi đến topic hoặc queue specific cho client
        messagingTemplate.convertAndSend(
            "/topic/license/" + licenseKey + "/" + deviceId, 
            message
        );
        
        System.out.println("Sent LOCK message to: " + licenseKey + "/" + deviceId);
    }
}
```

### 4. WebSocket handler (nếu dùng raw WebSocket thay vì STOMP)
```java
@Component
public class LicenseWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String licenseKey = extractLicenseKey(session);
        String deviceId = extractDeviceId(session);
        String key = licenseKey + "-" + deviceId;
        
        sessions.put(key, session);
        System.out.println("WebSocket connected: " + key);
    }
    
    public void sendLockMessage(String licenseKey, String deviceId, String reason) {
        String key = licenseKey + "-" + deviceId;
        WebSocketSession session = sessions.get(key);
        
        if (session != null && session.isOpen()) {
            try {
                Map<String, String> message = new HashMap<>();
                message.put("type", "LOCK");
                message.put("reason", reason);
                
                String json = new ObjectMapper().writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
                System.out.println("Sent LOCK to: " + key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private LicenseWebSocketHandler handler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/license")
                .setAllowedOrigins("*");
    }
}
```

## Client Fix: Thử kết nối lại nếu bị 302

Client đã được update để:
1. Log chi tiết khi WebSocket bị đóng
2. Tự động reconnect nếu bị đóng bất thường
3. Hiển thị thông báo lỗi rõ ràng

## Test lại

### 1. Chạy server với logging
```bash
./mvnw spring-boot:run --debug
```

### 2. Kiểm tra endpoint WebSocket
```
http://localhost:8080/ws/license
```
Phải trả về upgrade request, không phải redirect 302

### 3. Chạy client
```powershell
mvn javafx:run
```

### 4. Xem log, phải thấy:
```
WebSocket connected successfully
```
Không được thấy "Invalid status code received: 302"

### 5. Test LOCK message từ server
```bash
curl -X POST http://localhost:8080/api/admin/revoke-license \
  -d "licenseKey=TEST123" \
  -d "deviceId=ABC456" \
  -d "reason=License expired"
```

## Expected Result
```
=== Starting WebSocket connection ===
WebSocket client created
MainController set in WebSocket client
WebSocket connected successfully
Heartbeat sent
...
Received message: {"type":"LOCK","reason":"License expired"}
=== handleLicenseLock called ===
=== showLockAlert called in MainController ===
[Alert hiển thị]
```
