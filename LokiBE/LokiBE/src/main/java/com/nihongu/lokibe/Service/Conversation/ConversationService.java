package com.nihongu.lokibe.Service.Conversation;

import com.nihongu.lokibe.Dto.Conversation.ConversationResponse;
import com.nihongu.lokibe.Dto.Conversation.MessageResponse;
import com.nihongu.lokibe.Dto.Conversation.SendMessageRequest;

import java.util.List;

public interface ConversationService {

    // Gửi tin nhắn (tự tạo conversation nếu chưa có). Có photoId => reply ảnh Locket
    MessageResponse sendMessage(SendMessageRequest request);

    // Danh sách hộp thư (Inbox)
    List<ConversationResponse> getConversations(int page, int size);

    // Lịch sử tin nhắn, mới nhất lên đầu
    List<MessageResponse> getMessages(Long conversationId, int page, int size);

    // Đánh dấu đã đọc toàn bộ tin nhắn người kia gửi
    void markAsRead(Long conversationId);
}
