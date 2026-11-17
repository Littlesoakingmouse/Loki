package com.nihongu.lokibe.Service.User;

import com.nihongu.lokibe.Dto.Auth.AuthResponse;
import com.nihongu.lokibe.Dto.Auth.GoogleLoginRequest;
import com.nihongu.lokibe.Dto.Auth.LoginRequest;
import com.nihongu.lokibe.Dto.Auth.RegisterRequest;
import com.nihongu.lokibe.Dto.Users.ChangePasswordRequest;
import com.nihongu.lokibe.Dto.Users.UpdateProfileRequest;
import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import com.nihongu.lokibe.Entity.UserEntity;
import com.nihongu.lokibe.Enum.AuthProvider;
import com.nihongu.lokibe.Exception.BadRequestException;
import com.nihongu.lokibe.Exception.ResourceNotFoundException;
import com.nihongu.lokibe.Exception.UnauthorizedException;
import com.nihongu.lokibe.Mapper.UserMapper;
import com.nihongu.lokibe.Repositoty.FriendshipRepository;
import com.nihongu.lokibe.Repositoty.UserRepository;
import com.nihongu.lokibe.Enum.Status;
import com.nihongu.lokibe.Service.Device.DeviceTokenService;
import com.nihongu.lokibe.Util.JwtUtils;
import com.nihongu.lokibe.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // Endpoint chính thức của Google để verify idToken (không cần thêm thư viện)
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={token}";

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final DeviceTokenService deviceTokenService;
    private final RestClient restClient = RestClient.create();

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng.");
        }

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName().trim())
                .authProvider(AuthProvider.LOCAL)
                .build();
        user = userRepository.save(user);
        deviceTokenService.register(user.getId(), request.getFcmToken());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không đúng."));

        // Tài khoản Google không có mật khẩu -> không cho login kiểu LOCAL
        if (user.getAuthProvider() != AuthProvider.LOCAL
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không đúng.");
        }
        deviceTokenService.register(user.getId(), request.getFcmToken());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        Map<String, Object> payload = verifyGoogleToken(request.getIdToken());

        String email = normalizeEmail((String) payload.get("email"));
        String googleId = (String) payload.get("sub");

        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Lần đầu đăng nhập -> tạo tài khoản mới
            user = userRepository.save(UserEntity.builder()
                    .email(email)
                    .googleId(googleId)
                    .displayName((String) payload.get("name"))
                    .avatarUrl((String) payload.get("picture"))
                    .authProvider(AuthProvider.GOOGLE)
                    .build());
        } else {
            if (user.getAuthProvider() != AuthProvider.GOOGLE) {
                throw new BadRequestException("Email này đã được đăng ký bằng phương thức khác.");
            }
        }

        deviceTokenService.register(user.getId(), request.getFcmToken());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        return UserMapper.toSelf(getCurrentUser());
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        UserEntity user = getCurrentUser();

        if (StringUtils.hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        // App báo token push mới (Firebase đổi token, hoặc lúc mở app)
        deviceTokenService.register(user.getId(), request.getFcmToken());
        // Entity đang managed trong transaction -> tự động flush, không cần save()
        return UserMapper.toSelf(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> !hasBlockedMe(user.getId()))
                .map(UserMapper::toProfile)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email này."));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse findById(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !hasBlockedMe(user.getId()))
                .map(UserMapper::toProfile)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken() {
        return buildAuthResponse(getCurrentUser());
    }

    @Override
    @Transactional
    public void logout(String fcmToken) {
        // JWT là stateless -> app tự xoá token; server chỉ cần ngừng gửi push tới thiết bị này
        deviceTokenService.remove(SecurityUtils.getCurrentUserId(), fcmToken);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserEntity user = getCurrentUser();
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Tài khoản Google không có mật khẩu để đổi.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    }

    @Override
    @Transactional
    public void deleteAccount() {
        UserEntity user = getCurrentUser();

        // Ẩn danh hoá dữ liệu cá nhân + giải phóng email/googleId (cột unique) để có thể đăng ký lại
        user.setEmail("deleted_" + user.getId() + "_" + System.currentTimeMillis());
        user.setGoogleId(null);
        user.setPasswordHash(null);
        user.setAvatarUrl(null);
        user.setDisplayName("Người dùng Loki");
        userRepository.saveAndFlush(user);

        // Gỡ khỏi danh sách bạn bè của mọi người, rồi xoá mềm user
        friendshipRepository.softDeleteAllByUserId(user.getId());
        deviceTokenService.removeAll(user.getId());
        userRepository.delete(user);
    }

    // ===================== Helpers =====================

    private UserEntity getCurrentUser() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateToken(user.getId()))
                .user(UserMapper.toSelf(user))
                .build();
    }

    // Người đã chặn mình thì không tìm thấy được (cả tìm email lẫn link mời)
    private boolean hasBlockedMe(Long otherUserId) {
        Long me = SecurityUtils.getCurrentUserId();
        return friendshipRepository.findFriendshipBetween(me, otherUserId)
                .filter(f -> f.getStatus() == Status.BLOCKED && f.getRequesterId().equals(otherUserId))
                .isPresent();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("Email không được để trống.");
        }
        return email.trim().toLowerCase();
    }

    // Gọi Google verify idToken, đồng thời kiểm tra token được cấp cho đúng app của mình (aud)
    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleToken(String idToken) {
        Map<String, Object> payload;
        try {
            payload = restClient.get()
                    .uri(GOOGLE_TOKEN_INFO_URL, idToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.warn("Verify Google idToken thất bại: {}", e.getMessage());
            throw new UnauthorizedException("Google token không hợp lệ.");
        }

        if (payload == null
                || !googleClientId.equals(payload.get("aud"))
                || !"true".equals(String.valueOf(payload.get("email_verified")))) {
            throw new UnauthorizedException("Google token không hợp lệ.");
        }
        return payload;
    }
}
