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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "photo_recipients",
        indexes = {

                @Index(name = "idx_recipient", columnList = "recipient_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE photo_recipients SET is_deleted = true WHERE id = ?")
public class PhotorecipientEntity extends CommonEntity{
    @Column(name = "photo_id", nullable = false)
    private Long photoId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

}
