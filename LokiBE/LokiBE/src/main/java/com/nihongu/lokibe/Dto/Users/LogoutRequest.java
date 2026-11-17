package com.nihongu.lokibe.Dto.Users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// fcmToken của máy đang đăng xuất; để trống = ngừng push tới mọi máy của tài khoản
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    private String fcmToken;
}
