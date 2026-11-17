package com.nihongu.lokibe.Repositoty;

import com.nihongu.lokibe.Entity.PhotoReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository
public interface PhotoReactionRepository extends JpaRepository<PhotoReactionEntity, Long> {

    List<PhotoReactionEntity> findByPhotoId(Long photoId);

    // Lấy reaction của nhiều ảnh trong 1 query (dùng cho Feed, tránh N+1)
    List<PhotoReactionEntity> findByPhotoIdIn(Collection<Long> photoIds);


    Optional<PhotoReactionEntity> findByPhotoIdAndUserId(Long photoId, Long userId);

    long countByPhotoId(Long photoId);
}
