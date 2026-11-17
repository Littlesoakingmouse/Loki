package com.nihongu.lokibe.Entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "photo_reactions",
        indexes = {
                // Đánh index cho photo_id vì hệ thống sẽ gọi query lấy danh sách reaction theo photo_id rất thường xuyên
                @Index(name = "idx_photo_reaction", columnList = "photo_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE photo_reactions SET is_deleted = true WHERE id = ?")
public class PhotoReactionEntity extends CommonEntity{
    @Column(name = "photo_id", nullable = false)
    private Long photoId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String emoji;
}
