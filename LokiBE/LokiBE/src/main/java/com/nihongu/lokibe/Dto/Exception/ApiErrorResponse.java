package com.nihongu.lokibe.Dto.Exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các trường null khi parse ra JSON
public class ApiErrorResponse {
    private int status;                 // Mã HTTP (400, 404, 500...)
    private String error;               // Tên lỗi (Not Found, Bad Request...)
    private String message;             // Lời nhắn chi tiết cho User
    private LocalDateTime timestamp;    // Thời gian xảy ra lỗi

    // Lưu các lỗi validation của form (ví dụ: email sai định dạng, pass quá ngắn)
    private Map<String, String> validationErrors;
}