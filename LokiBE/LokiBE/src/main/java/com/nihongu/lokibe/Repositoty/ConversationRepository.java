package com.nihongu.lokibe.Repositoty;

import com.nihongu.lokibe.Entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;


@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    // Tìm cuộc trò chuyện giữa 2 user.
    // Nhờ quy tắc lưu user1Id < user2Id, ta có thể tìm chính xác và rất nhanh
    Optional<ConversationEntity> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    // Lấy danh sách các cuộc trò chuyện (Inbox) của 1 user, sắp xếp theo thời gian tin nhắn mới nhất
    @Query("SELECT c FROM ConversationEntity c WHERE c.user1Id = :userId OR c.user2Id = :userId ORDER BY c.lastMessageAt DESC")
    List<ConversationEntity> findAllByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId, Pageable pageable);
}
