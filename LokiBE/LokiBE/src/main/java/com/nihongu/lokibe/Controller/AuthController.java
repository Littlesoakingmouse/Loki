package com.nihongu.lokibe.Controller;

import com.nihongu.lokibe.Dto.Auth.AuthResponse;
import com.nihongu.lokibe.Dto.Auth.GoogleLoginRequest;
import com.nihongu.lokibe.Dto.Auth.LoginRequest;
import com.nihongu.lokibe.Dto.Auth.RegisterRequest;
import com.nihongu.lokibe.Service.User.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// API public (không cần token): đăng ký / đăng nhập
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return userService.loginWithGoogle(request);
    }

    // Gửi kèm header Authorization (token còn hạn) -> nhận token mới. Token hết hạn -> 401, app về màn login
    @PostMapping("/refresh")
    public AuthResponse refresh() {
        return userService.refreshToken();
    }
}
