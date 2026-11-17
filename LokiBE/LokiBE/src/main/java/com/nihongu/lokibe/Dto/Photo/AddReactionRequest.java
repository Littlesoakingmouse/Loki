package com.nihongu.lokibe.Dto.Photo;

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
public class AddReactionRequest {
    @NotNull(message = "ID bức ảnh không được để trống")
    private Long photoId;

    @NotBlank(message = "Emoji không được để trống")
    @Size(max = 10, message = "Emoji không hợp lệ")
    private String emoji;
}
