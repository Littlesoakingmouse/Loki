package com.nihongu.lokibe.Repositoty;

import com.nihongu.lokibe.Entity.PhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<PhotoEntity, Long> {
    // Ảnh đã lưu với clientId này (app gửi lại sau khi mất mạng)
    Optional<PhotoEntity> findBySenderIdAndClientId(Long senderId, String clientId);

    // Lấy lịch sử các ảnh do chính user này đã gửi đi (có phân trang)
    List<PhotoEntity> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);

    // Lịch sử kiểu Locket: ảnh mình gửi + ảnh bạn bè gửi cho mình, mới nhất trước.
    // Bỏ ảnh của người mình đã chặn hoặc đã chặn mình (lọc trong query để mỗi trang vẫn đủ số ảnh)
    @Query("""
            SELECT p FROM PhotoEntity p
            WHERE (p.senderId = :userId
               OR p.id IN (SELECT r.photoId FROM PhotorecipientEntity r WHERE r.recipientId = :userId))
              AND NOT EXISTS (SELECT f FROM FriendshipsEntity f WHERE f.status = 'BLOCKED'
                   AND ((f.requesterId = :userId AND f.addresseeId = p.senderId)
                     OR (f.addresseeId = :userId AND f.requesterId = p.senderId)))
            ORDER BY p.createdAt DESC
            """)
    List<PhotoEntity> findHistory(@Param("userId") Long userId, Pageable pageable);

    // Chỉ ảnh của 1 người bạn đã gửi cho mình (bộ lọc theo bạn)
    @Query("""
            SELECT p FROM PhotoEntity p
            WHERE p.senderId = :friendId
              AND p.id IN (SELECT r.photoId FROM PhotorecipientEntity r WHERE r.recipientId = :userId)
            ORDER BY p.createdAt DESC
            """)
    List<PhotoEntity> findHistoryFromFriend(@Param("userId") Long userId, @Param("friendId") Long friendId, Pageable pageable);
}
