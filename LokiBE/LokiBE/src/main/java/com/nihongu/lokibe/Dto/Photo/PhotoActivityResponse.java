package com.nihongu.lokibe.Dto.Photo;

import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Một người nhận của ảnh mình gửi: đã xem lúc nào, thả emoji gì (null = chưa)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoActivityResponse {
    private UserProfileResponse user;
    private LocalDateTime seenAt;
    private String emoji;
}
