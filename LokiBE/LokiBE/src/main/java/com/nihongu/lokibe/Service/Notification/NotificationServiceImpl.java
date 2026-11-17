package com.nihongu.lokibe.Service.Notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;

import com.nihongu.lokibe.Service.Device.DeviceTokenService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final DeviceTokenService deviceTokenService;
    private final ResourceLoader resourceLoader;

    @Value("${firebase.config-path:classpath:firebase-service-account.json}")
    private String firebaseConfigPath;

    private boolean firebaseEnabled = false;

    @PostConstruct
    public void initFirebase() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                firebaseEnabled = true;
                return;
            }
            Resource resource = resourceLoader.getResource(firebaseConfigPath);
            if (!resource.exists()) {
                log.warn("Chưa tìm thấy file cấu hình Firebase tại '{}'. Tính năng Push Notification tạm thời tắt.", firebaseConfigPath);
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(is))
                        .build();
                FirebaseApp.initializeApp(options);
                firebaseEnabled = true;
                log.info("Khởi tạo Firebase Cloud Messaging (FCM) thành công.");
            }
        } catch (Exception e) {
            log.warn("Không thể khởi tạo Firebase ({}). Push Notification sẽ được bỏ qua.", e.getMessage());
        }
    }

    @Async
    @Override
    public void sendToUser(Long recipientId, String title, String body, Map<String, String> data) {
        if (recipientId == null) return;
        send(List.of(recipientId), title, body, data);
    }

    @Async
    @Override
    public void sendToUsers(Collection<Long> recipientIds, String title, String body, Map<String, String> data) {
        send(recipientIds, title, body, data);
    }

    // Gửi tới mọi thiết bị của mọi người nhận (1 người có thể đăng nhập nhiều máy)
    private void send(Collection<Long> recipientIds, String title, String body, Map<String, String> data) {
        if (!firebaseEnabled || recipientIds == null || recipientIds.isEmpty()) return;

        List<String> tokens = deviceTokenService.tokensOf(recipientIds);
        if (tokens.isEmpty()) return;

        try {
            // Data-only: app luôn nhận onMessageReceived (kể cả chạy nền) để tự hiện thông báo + cập nhật Widget.
            // Priority HIGH đánh thức máy đang tắt màn hình để Widget cập nhật ngay
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .putAllData(withTitleAndBody(title, body, data))
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            removeDeadTokens(tokens, FirebaseMessaging.getInstance().sendEachForMulticast(message));
        } catch (Exception e) {
            log.warn("Gửi FCM thất bại: {}", e.getMessage());
        }
    }

    // App đã bị gỡ hoặc token hết hạn: xoá để lần sau không gửi vào token chết
    private void removeDeadTokens(List<String> tokens, BatchResponse result) {
        List<String> dead = new ArrayList<>();
        List<SendResponse> responses = result.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful() || r.getException() == null) continue;
            MessagingErrorCode code = r.getException().getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                dead.add(tokens.get(i));
            }
        }
        if (!dead.isEmpty()) {
            deviceTokenService.removeTokens(dead);
            log.info("Đã xoá {} FCM token không còn hợp lệ", dead.size());
        }
    }

    private static Map<String, String> withTitleAndBody(String title, String body, Map<String, String> data) {
        Map<String, String> all = new HashMap<>();
        if (data != null) all.putAll(data);
        if (title != null) all.put("title", title);
        if (body != null) all.put("body", body);
        return all;
    }
}
