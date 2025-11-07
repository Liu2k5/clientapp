# Hướng dẫn Test WebSocket License Revocation

## 1. Chạy ứng dụng với logging
```powershell
mvn javafx:run
```

## 2. Kiểm tra console output khi kích hoạt
Sau khi nhập license và click "Kích hoạt", bạn sẽ thấy:
```
Kích hoạt thành công
Heartbeat sent
=== Starting WebSocket connection ===
WebSocket client created
MainController set in WebSocket client
WebSocket connected successfully
```

## 3. Format message từ server
Server cần gửi WebSocket message theo format JSON:
```json
{
  "type": "LOCK",
  "reason": "License đã bị vô hiệu hóa bởi quản trị viên"
}
```

## 4. Khi nhận được message LOCK, console sẽ hiển thị:
```
Received message: {"type":"LOCK","reason":"..."}
=== handleLicenseLock called ===
Reason: License đã bị vô hiệu hóa bởi quản trị viên
MainController is null? false
Heartbeat stopped
Calling mainController.showLockAlert()
=== showLockAlert called in MainController ===
Reason: License đã bị vô hiệu hóa bởi quản trị viên
Running on JavaFX thread
Status label updated
About to show alert
```

## 5. Kết quả mong đợi
- Label "Trạng thái" chuyển màu đỏ: "Trạng thái: Bị khóa"
- Hiển thị Alert dialog với tiêu đề "License bị thu hồi"
- Sau khi đóng alert, ứng dụng tự động thoát

## 6. Troubleshooting

### Nếu không thấy "WebSocket connected successfully"
- Kiểm tra server WebSocket có chạy ở `ws://localhost:8080/ws/license` không
- Kiểm tra firewall/port 8080

### Nếu không thấy "Received message"
- Server chưa gửi message
- Kiểm tra connection bị đóng (xem "WebSocket closed" message)

### Nếu thấy "MainController is null? true"
- Bug trong code, MainController chưa được set
- Cần debug thêm trong Controller.java

### Nếu không hiện Alert
- Kiểm tra có lỗi exception không
- Có thể JavaFX thread bị block

## 7. Test thủ công từ server
Từ server Spring Boot, gửi message đến client:
```java
// Trong WebSocket handler của server
SimpMessagingTemplate messagingTemplate;

public void revokeClientLicense(String licenseKey, String deviceId) {
    Map<String, String> message = new HashMap<>();
    message.put("type", "LOCK");
    message.put("reason", "License đã bị vô hiệu hóa bởi quản trị viên");
    
    messagingTemplate.convertAndSendToUser(
        licenseKey + "-" + deviceId, 
        "/queue/license", 
        message
    );
}
```
