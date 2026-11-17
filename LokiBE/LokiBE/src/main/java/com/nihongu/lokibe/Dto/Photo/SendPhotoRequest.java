package com.nihongu.lokibe.Dto.Photo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendPhotoRequest {
    @NotBlank(message = "Đường dẫn ảnh không được để trống")
    private String imageUrl;

    private String caption;

    @NotEmpty(message = "Danh sách người nhận không được để trống")
    private List<Long> recipientIds;

    // UUID của lần gửi này; gửi lại cùng clientId sẽ nhận lại ảnh cũ thay vì tạo ảnh mới
    @Size(max = 64)
    private String clientId;
}
