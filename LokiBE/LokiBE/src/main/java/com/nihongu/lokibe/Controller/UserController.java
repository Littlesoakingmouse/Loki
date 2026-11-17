package com.nihongu.lokibe.Controller;

import com.nihongu.lokibe.Dto.Users.ChangePasswordRequest;
import com.nihongu.lokibe.Dto.Users.LogoutRequest;
import com.nihongu.lokibe.Dto.Users.UpdateProfileRequest;
import jakarta.validation.Valid;
import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import com.nihongu.lokibe.Service.User.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileResponse getMyProfile() {
        return userService.getCurrentUserProfile();
    }

    @PutMapping("/me")
    public UserProfileResponse updateMyProfile(@RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    // GET /api/users/search?email=abc@gmail.com
    @GetMapping("/search")
    public UserProfileResponse searchByEmail(@RequestParam String email) {
        return userService.findByEmail(email);
    }

    // Mở link mời kết bạn (loki://add/{id})
    @GetMapping("/{userId}")
    public UserProfileResponse getById(@PathVariable Long userId) {
        return userService.findById(userId);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) LogoutRequest request) {
        userService.logout(request != null ? request.getFcmToken() : null);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount() {
        userService.deleteAccount();
    }
}
