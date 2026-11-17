package com.nihongu.lokibe.Dto.Report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    @NotNull(message = "Người bị báo cáo không được để trống")
    private Long userId;

    // Ảnh cụ thể bị báo cáo (nếu có)
    private Long photoId;

    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;
}
