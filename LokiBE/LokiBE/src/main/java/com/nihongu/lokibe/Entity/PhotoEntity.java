package com.nihongu.lokibe.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
// (sender_id, client_id) duy nhất: app gửi lại cùng clientId khi mất mạng giữa chừng -> không tạo ảnh trùng
@Table(name = "photos", uniqueConstraints = @UniqueConstraint(name = "uk_photo_sender_client", columnNames = {"sender_id", "client_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE photos SET is_deleted = true WHERE id = ?")
public class PhotoEntity extends CommonEntity{
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;


    @Column(columnDefinition = "TEXT")
    private String caption;

    // UUID do app tạo cho mỗi lần gửi (null với app cũ)
    @Column(name = "client_id", length = 64)
    private String clientId;
}
