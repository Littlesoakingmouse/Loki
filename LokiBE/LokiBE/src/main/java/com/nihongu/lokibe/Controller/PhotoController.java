package com.nihongu.lokibe.Controller;

import com.nihongu.lokibe.Constant.AppConstants;
import com.nihongu.lokibe.Dto.Photo.AddReactionRequest;
import com.nihongu.lokibe.Dto.Photo.PhotoActivityResponse;
import com.nihongu.lokibe.Dto.Photo.PhotoReactionResponse;
import com.nihongu.lokibe.Dto.Photo.PhotoResponse;
import com.nihongu.lokibe.Dto.Photo.SendPhotoRequest;
import com.nihongu.lokibe.Service.Photo.PhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    // Gửi ảnh (client upload ảnh lên storage trước, gửi imageUrl lên đây)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoResponse sendPhoto(@Valid @RequestBody SendPhotoRequest request) {
        return photoService.sendPhoto(request);
    }

    // Ảnh bạn bè gửi cho mình
    @GetMapping("/feed")
    public List<PhotoResponse> getFeed(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return photoService.getFeed(page, size);
    }

    // Ảnh mới nhất cho Widget
    @GetMapping("/widget")
    public PhotoResponse getLatestForWidget() {
        return photoService.getLatestPhotoForWidget();
    }

    // Ảnh mình đã gửi
    @GetMapping("/sent")
    public List<PhotoResponse> getSentPhotos(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return photoService.getSentPhotos(page, size);
    }

    // Lịch sử kiểu Locket (ảnh gửi + nhận), lọc theo bạn bằng ?friendId=
    @GetMapping("/history")
    public List<PhotoResponse> getHistory(
            @RequestParam(required = false) Long friendId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return photoService.getHistory(friendId, page, size);
    }

    @PutMapping("/{photoId}/seen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsSeen(@PathVariable Long photoId) {
        photoService.markAsSeen(photoId);
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable Long photoId) {
        photoService.deletePhoto(photoId);
    }

    // Thả / đổi emoji
    @PostMapping("/reactions")
    public PhotoReactionResponse react(@Valid @RequestBody AddReactionRequest request) {
        return photoService.react(request);
    }

    @DeleteMapping("/{photoId}/reactions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReaction(@PathVariable Long photoId) {
        photoService.removeReaction(photoId);
    }

    // Chỉ người gửi: ai đã xem, thả emoji gì
    @GetMapping("/{photoId}/activity")
    public List<PhotoActivityResponse> getActivity(@PathVariable Long photoId) {
        return photoService.getActivity(photoId);
    }
}
