package com.nihongu.lokibe.Util;


import com.nihongu.lokibe.Exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class ImageUtils {
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public static void validateImage(MultipartFile file){
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File ảnh không được để trống.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Chỉ chấp nhận định dạng JPG, PNG hoặc WEBP.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Dung lượng ảnh không được vượt quá 5MB.");
        }
    }
}
