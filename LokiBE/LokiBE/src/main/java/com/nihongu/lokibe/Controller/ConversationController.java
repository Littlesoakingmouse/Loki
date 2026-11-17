package com.nihongu.lokibe.Controller;

import com.nihongu.lokibe.Constant.AppConstants;
import com.nihongu.lokibe.Dto.Conversation.ConversationResponse;
import com.nihongu.lokibe.Dto.Conversation.MessageResponse;
import com.nihongu.lokibe.Dto.Conversation.SendMessageRequest;
import com.nihongu.lokibe.Service.Conversation.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    // Inbox
    @GetMapping
    public List<ConversationResponse> getConversations(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return conversationService.getConversations(page, size);
    }

    // Gửi tin nhắn theo recipientId (conversation tự tạo nếu chưa có)
    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return conversationService.sendMessage(request);
    }


    @GetMapping("/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return conversationService.getMessages(conversationId, page, size);
    }

    @PutMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long conversationId) {
        conversationService.markAsRead(conversationId);
    }
}
