package com.nihongu.lokibe.Repositoty;

import com.nihongu.lokibe.Entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    // Lấy lịch sử tin nhắn của một cuộc trò chuyện (Phân trang và sắp xếp mới nhất lên đầu)
    // Spring Boot sẽ tự động tận dụng Index (conversation_id, created_at) mà ta đã đánh ở Entity
    Optional<MessageEntity> findBySenderIdAndClientId(Long senderId, String clientId);

    List<MessageEntity> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    // Đếm số lượng tin nhắn chưa đọc trong 1 cuộc trò chuyện cụ thể đối với user hiện tại
    // (Tin nhắn chưa đọc là tin nhắn do người kia gửi và isRead = false)
    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.conversationId = :conversationId AND m.senderId != :userId AND m.isRead = false")
    long countUnreadMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    // Đánh dấu toàn bộ tin nhắn người kia gửi trong phòng chat là "Đã đọc"
    // Thường được gọi khi user bấm vào mở khung chat
    @Modifying
    @Query("UPDATE MessageEntity m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP WHERE m.conversationId = :conversationId AND m.senderId != :userId AND m.isRead = false")
    void markAllAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
