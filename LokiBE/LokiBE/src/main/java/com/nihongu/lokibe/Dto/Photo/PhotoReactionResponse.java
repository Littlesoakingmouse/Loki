package com.nihongu.lokibe.Dto.Photo;

import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoReactionResponse {
    private Long id;
    private Long photoId;
    private UserProfileResponse user;
    private String emoji;
    private LocalDateTime createdAt;
}
