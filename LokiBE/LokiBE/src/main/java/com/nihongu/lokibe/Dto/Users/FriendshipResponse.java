package com.nihongu.lokibe.Dto.Users;

import com.nihongu.lokibe.Enum.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendshipResponse {
    private Long id;
    // Sender
    private UserProfileResponse requester;
    // Receiver
    private UserProfileResponse addressee;
    private Status status;
    private LocalDateTime createdAt;
}
