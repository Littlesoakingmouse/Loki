package com.nihongu.lokibe.Dto.Conversation;

import com.nihongu.lokibe.Enum.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    @NotNull(message = "ID người nhận không được để trống")
    private Long recipientId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;

    @Builder.Default
    private MessageType type = MessageType.TEXT;

    // Truyền photoId nếu đây là tin nhắn reply trực tiếp 1 bức ảnh Locket
    private Long photoId;

    // UUID của tin này; gửi lại cùng clientId sẽ nhận lại tin cũ thay vì tạo tin mới
    @Size(max = 64)
    private String clientId;
}
