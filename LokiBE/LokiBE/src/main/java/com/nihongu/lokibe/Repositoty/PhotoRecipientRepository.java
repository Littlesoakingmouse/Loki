package com.nihongu.lokibe.Repositoty;

import com.nihongu.lokibe.Entity.PhotorecipientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRecipientRepository extends JpaRepository<PhotorecipientEntity, Long> {
    // Lấy danh sách ảnh nhận được mới nhất để hiển thị lên Feed/Widget (có phân trang)
    List<PhotorecipientEntity> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    // Lấy bức ảnh mới nhất chưa xem/mới gửi tới người dùng (dùng riêng cho Widget)
    Optional<PhotorecipientEntity> findFirstByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    // Tìm bản ghi cụ thể của photo và recipient
    // Mọi người nhận của 1 ảnh (danh sách "đã xem" cho người gửi)
    List<PhotorecipientEntity> findByPhotoId(Long photoId);

    Optional<PhotorecipientEntity> findByPhotoIdAndRecipientId(Long photoId, Long recipientId);

    // Lấy seenAt của mình cho cả 1 trang ảnh
    List<PhotorecipientEntity> findByRecipientIdAndPhotoIdIn(Long recipientId, Collection<Long> photoIds);

    // Cập nhật thời gian đã xem (seenAt) khi người dùng mở ảnh
    @Modifying
    @Query("UPDATE PhotorecipientEntity p SET p.seenAt = :seenAt WHERE p.photoId = :photoId AND p.recipientId = :recipientId")
    void markAsSeen(@Param("photoId") Long photoId, @Param("recipientId") Long recipientId, @Param("seenAt") LocalDateTime seenAt);
}
