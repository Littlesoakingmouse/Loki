package com.nihongu.lokibe.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Báo cáo vi phạm: một người dùng, có thể kèm một ảnh cụ thể. Admin xem trực tiếp trong DB. */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE reports SET is_deleted = true WHERE id = ?")
public class ReportEntity extends CommonEntity {
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reported_user_id", nullable = false)
    private Long reportedUserId;

    @Column(name = "photo_id")
    private Long photoId;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
