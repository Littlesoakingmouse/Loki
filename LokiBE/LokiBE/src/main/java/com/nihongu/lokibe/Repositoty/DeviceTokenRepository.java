package com.nihongu.lokibe.Repositoty;

import com.nihongu.lokibe.Entity.DeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, Long> {
    Optional<DeviceTokenEntity> findByToken(String token);

    List<DeviceTokenEntity> findByUserIdIn(Collection<Long> userIds);

    @Modifying
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.token IN :tokens")
    void deleteAllByTokenIn(@Param("tokens") Collection<String> tokens);

    @Modifying
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.userId = :userId AND d.token = :token")
    void deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);
}
