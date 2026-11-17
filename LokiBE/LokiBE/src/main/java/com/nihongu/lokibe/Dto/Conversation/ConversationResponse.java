package com.nihongu.lokibe.Dto.Conversation;

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
public class ConversationResponse {
    private Long id;
    private UserProfileResponse otherUser;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private Long lastMessageSenderId;
    private long unreadCount;
}
