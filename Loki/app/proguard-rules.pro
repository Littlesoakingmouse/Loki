# TỔNG QUAN FILE: Keep rule cho R8 ở bản release.
#
# Thành phần chính:
# - Giữ các lớp @Serializable (DTO, route điều hướng) để kotlinx.serialization tìm được serializer.
# - Retrofit, OkHttp, Room, Hilt, Coil, Firebase đã kèm keep rule riêng trong thư viện.
#
# Lưu ý:
# - Mỗi lần đổi thư viện hoặc thêm lớp dùng reflection, chạy thử bản release (assembleRelease) trên máy thật.

# kotlinx.serialization: keep the generated serializers of our @Serializable classes
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers @kotlinx.serialization.Serializable class com.example.loki.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.loki.**$$serializer { *; }

# Retrofit reads the generic return types of suspend API methods
-keep,allowobfuscation interface com.example.loki.data.remote.api.**
