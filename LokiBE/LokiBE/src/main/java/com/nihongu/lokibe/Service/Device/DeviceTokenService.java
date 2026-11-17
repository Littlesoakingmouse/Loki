package com.nihongu.lokibe.Service.Device;

import com.nihongu.lokibe.Entity.DeviceTokenEntity;
import com.nihongu.lokibe.Repositoty.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/** Các thiết bị nhận push của mỗi user (đăng nhập nhiều máy cùng lúc). */
@Service
@RequiredArgsConstructor
public class DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;

    // Gọi khi đăng nhập / app báo token mới. Token đang thuộc user khác (cùng máy, đổi tài khoản) thì chuyển sang user này
    @Transactional
    public void register(Long userId, String token) {
        if (userId == null || !StringUtils.hasText(token)) return;
        DeviceTokenEntity device = deviceTokenRepository.findByToken(token)
                .orElseGet(() -> DeviceTokenEntity.builder().token(token).build());
        device.setUserId(userId);
        deviceTokenRepository.save(device);
    }

    // Đăng xuất 1 máy: chỉ xoá token của máy đó; không biết token thì xoá hết (an toàn hơn để lọt push)
    @Transactional
    public void remove(Long userId, String token) {
        if (StringUtils.hasText(token)) deviceTokenRepository.deleteByUserIdAndToken(userId, token);
        else deviceTokenRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public void removeAll(Long userId) {
        deviceTokenRepository.deleteAllByUserId(userId);
    }

    // FCM báo token không còn hợp lệ (gỡ app, xoá dữ liệu)
    @Transactional
    public void removeTokens(Collection<String> tokens) {
        if (!tokens.isEmpty()) deviceTokenRepository.deleteAllByTokenIn(tokens);
    }

    @Transactional(readOnly = true)
    public List<String> tokensOf(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return deviceTokenRepository.findByUserIdIn(userIds).stream().map(DeviceTokenEntity::getToken).distinct().toList();
    }
}
