package com.nihongu.lokibe.Mapper;

import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import com.nihongu.lokibe.Entity.UserEntity;

//  UserEntity -> DTO, dùng chung cho mọi Service
public final class UserMapper {

    private UserMapper() {}

    // Hồ sơ người khác (bạn bè, người gửi ảnh, link mời): không lộ email
    public static UserProfileResponse toProfile(UserEntity user) {
        if (user == null) return null;
        return UserProfileResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // Hồ sơ của chính mình (đăng nhập, /me): có email
    public static UserProfileResponse toSelf(UserEntity user) {
        if (user == null) return null;
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
