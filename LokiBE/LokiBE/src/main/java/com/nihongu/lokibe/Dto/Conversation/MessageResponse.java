package com.nihongu.lokibe.Dto.Conversation;

import com.nihongu.lokibe.Dto.Photo.PhotoResponse;
import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import com.nihongu.lokibe.Enum.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private UserProfileResponse sender;
    private String content;
    private MessageType type;

    //return PHOTO if this is response for a Image
    private PhotoResponse photo;

    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
