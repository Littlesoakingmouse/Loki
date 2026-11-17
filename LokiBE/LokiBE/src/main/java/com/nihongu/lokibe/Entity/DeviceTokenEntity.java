package com.nihongu.lokibe.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Một thiết bị đang đăng nhập (FCM token) của user. Một user có thể có nhiều thiết bị.
 * Token là duy nhất: đăng nhập tài khoản khác trên cùng máy sẽ chuyển token sang tài khoản đó.
 * Không xoá mềm: token cũ không còn giá trị gì nên xoá hẳn.
 */
@Entity
@Table(name = "device_tokens", indexes = @Index(name = "idx_device_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
