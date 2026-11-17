package com.nihongu.lokibe.Dto.Photo;

import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoResponse {
    private Long id;
    private UserProfileResponse sender;
    private String imageUrl;
    private String caption;
    private LocalDateTime createdAt;
    private LocalDateTime seenAt;
    private List<PhotoReactionResponse> reactions;
}
