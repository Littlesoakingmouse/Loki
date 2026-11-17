package com.nihongu.lokibe.Entity;

import com.nihongu.lokibe.Enum.MessageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "messages",
        // Gửi lại cùng clientId (mất mạng giữa chừng) -> không tạo tin trùng
        uniqueConstraints = @UniqueConstraint(name = "uk_message_sender_client", columnNames = {"sender_id", "client_id"}),
        indexes = {
                @Index(name = "idx_conversation_created", columnList = "conversation_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE messages SET is_deleted = true WHERE id = ?")
public class MessageEntity extends CommonEntity{
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(name = "photo_id")
    private Long photoId;

    // UUID do app tạo cho mỗi tin (null với app cũ)
    @Column(name = "client_id", length = 64)
    private String clientId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

}
