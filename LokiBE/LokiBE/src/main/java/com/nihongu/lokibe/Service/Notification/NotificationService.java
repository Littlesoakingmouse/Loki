package com.nihongu.lokibe.Service.Notification;

import java.util.Collection;
import java.util.Map;

public interface NotificationService {

    // Gửi push notification tới 1 user
    void sendToUser(Long recipientId, String title, String body, Map<String, String> data);

    // Gửi push notification tới danh sách nhiều user (VD: gửi ảnh Locket cho nhiều bạn bè cùng lúc)
    void sendToUsers(Collection<Long> recipientIds, String title, String body, Map<String, String> data);
}
