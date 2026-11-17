package com.nihongu.lokibe.Service.Photo;

import com.nihongu.lokibe.Dto.Photo.AddReactionRequest;
import com.nihongu.lokibe.Dto.Photo.PhotoActivityResponse;
import com.nihongu.lokibe.Dto.Photo.PhotoReactionResponse;
import com.nihongu.lokibe.Dto.Photo.PhotoResponse;
import com.nihongu.lokibe.Dto.Photo.SendPhotoRequest;

import java.util.List;

public interface PhotoService {

    // Xử lý khi user bấm chụp và gửi ảnh cho bạn bè
    PhotoResponse sendPhoto(SendPhotoRequest request);

    // Lấy danh sách ảnh trên màn hình chính (Phân trang)
    List<PhotoResponse> getFeed(int page, int size);

    // Lấy đúng 1 ảnh mới nhất để hiển thị lên Widget ngoài màn hình Home
    PhotoResponse getLatestPhotoForWidget();

    // Lịch sử ảnh do chính mình gửi đi
    List<PhotoResponse> getSentPhotos(int page, int size);

    // Lịch sử: ảnh mình gửi + ảnh nhận được; friendId = lọc theo 1 người (chính mình = chỉ ảnh mình gửi)
    List<PhotoResponse> getHistory(Long friendId, int page, int size);

    // Đánh dấu đã xem ảnh (người nhận)
    void markAsSeen(Long photoId);

    // Chỉ người gửi mới được xoá ảnh
    void deletePhoto(Long photoId);

    // Thả / đổi emoji (mỗi user 1 reaction cho 1 ảnh)
    PhotoReactionResponse react(AddReactionRequest request);

    void removeReaction(Long photoId);

    // Người gửi xem ai đã xem / thả emoji ảnh của mình
    List<PhotoActivityResponse> getActivity(Long photoId);
}
