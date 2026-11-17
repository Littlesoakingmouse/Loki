package com.nihongu.lokibe.Repositoty;

import com.nihongu.lokibe.Entity.FriendshipsEntity;
import com.nihongu.lokibe.Enum.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<FriendshipsEntity, Long> {
    // Kiểm tra mối quan hệ 2 chiều giữa 2 user (User A gửi cho B hoặc B gửi cho A)
    @Query("SELECT f FROM FriendshipsEntity f WHERE " +
            "(f.requesterId = :userA AND f.addresseeId = :userB) OR " +
            "(f.requesterId = :userB AND f.addresseeId = :userA)")
    Optional<FriendshipsEntity> findFriendshipBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    // Lấy danh sách các lời mời kết bạn đang chờ xử lý (PENDING) gửi tới user này
    // Người mình đã chặn (người chặn luôn là requester)
    List<FriendshipsEntity> findByRequesterIdAndStatus(Long requesterId, Status status);

    List<FriendshipsEntity> findByAddresseeIdAndStatus(Long addresseeId, Status status);

    // Lấy danh sách ID bạn bè đã đồng ý (ACCEPTED) của một user
    @Query("SELECT CASE WHEN f.requesterId = :userId THEN f.addresseeId ELSE f.requesterId END " +
            "FROM FriendshipsEntity f " +
            "WHERE (f.requesterId = :userId OR f.addresseeId = :userId) " +
            "AND f.status = 'ACCEPTED'")
    List<Long> findAllFriendIdsByUserId(@Param("userId") Long userId);

    // Kiểm tra 2 user có phải là bạn bè hay không
    @Query("SELECT COUNT(f) > 0 FROM FriendshipsEntity f WHERE " +
            "((f.requesterId = :userA AND f.addresseeId = :userB) OR (f.requesterId = :userB AND f.addresseeId = :userA)) " +
            "AND f.status = 'ACCEPTED'")
    boolean isFriend(@Param("userA") Long userA, @Param("userB") Long userB);

    // Xoá mềm toàn bộ quan hệ của 1 user (dùng khi xoá tài khoản)
    @Modifying
    @Query("UPDATE FriendshipsEntity f SET f.isDeleted = true WHERE f.requesterId = :userId OR f.addresseeId = :userId")
    void softDeleteAllByUserId(@Param("userId") Long userId);
}
