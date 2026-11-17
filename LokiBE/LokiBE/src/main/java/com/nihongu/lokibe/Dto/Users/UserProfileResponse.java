package com.nihongu.lokibe.Dto.Users;

import com.nihongu.lokibe.Enum.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private AuthProvider authProvider;
    private LocalDateTime createdAt;
}
