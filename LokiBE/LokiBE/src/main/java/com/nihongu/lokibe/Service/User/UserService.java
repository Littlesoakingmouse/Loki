package com.nihongu.lokibe.Service.User;

import com.nihongu.lokibe.Dto.Auth.AuthResponse;
import com.nihongu.lokibe.Dto.Auth.GoogleLoginRequest;
import com.nihongu.lokibe.Dto.Auth.LoginRequest;
import com.nihongu.lokibe.Dto.Auth.RegisterRequest;
import com.nihongu.lokibe.Dto.Users.ChangePasswordRequest;
import com.nihongu.lokibe.Dto.Users.UpdateProfileRequest;
import com.nihongu.lokibe.Dto.Users.UserProfileResponse;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    // Đăng nhập bằng email + mật khẩu
    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    UserProfileResponse getCurrentUserProfile();

    // Cập nhật tên, avatar, fcmToken (field nào null thì bỏ qua)
    UserProfileResponse updateProfile(UpdateProfileRequest request);

    // Tìm user theo email để gửi lời mời kết bạn
    UserProfileResponse findByEmail(String email);

    UserProfileResponse findById(Long userId);

    // Cấp token mới khi token cũ còn hạn (app gọi mỗi lần mở) -> người dùng không bị đăng xuất
    AuthResponse refreshToken();

    // Gỡ máy này (fcmToken) khỏi danh sách nhận push; null = gỡ mọi máy
    void logout(String fcmToken);

    // Chỉ tài khoản đăng ký bằng email + mật khẩu
    void changePassword(ChangePasswordRequest request);

    // Xoá tài khoản (bắt buộc theo chính sách Google Play)
    void deleteAccount();
}
