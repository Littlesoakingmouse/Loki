package com.nihongu.lokibe.Constant;

public class AppConstants {
    // Ngăn không cho khởi tạo object từ class này
    private AppConstants() {}

    // Pagination mặc định
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "15";
    public static final int MAX_PAGE_SIZE = 50;

    // Giống Locket: một vòng bạn thân nhỏ
    public static final int MAX_FRIENDS = 20;

    // Security
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    // Push Notification (FCM)
    public static final String NOTI_TITLE_NEW_PHOTO = "Bạn có ảnh mới!";
    public static final String NOTI_TITLE_NEW_MESSAGE = "Tin nhắn mới";

    // Kích thước tối đa cho file upload
    public static final long MAX_FILE_SIZE_MB = 5 * 1024 * 1024;
}
