package com.nihongu.lokibe.Dto.Auth;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginRequest {
    @NotBlank(message = "idToken không được để trống")
    private String idToken;

    private String fcmToken;
}
