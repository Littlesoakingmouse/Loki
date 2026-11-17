package com.nihongu.lokibe.Service.Photo;

import com.nihongu.lokibe.Constant.AppConstants;
import com.nihongu.lokibe.Dto.Photo.AddReactionRequest;
import com.nihongu.lokibe.Dto.Photo.PhotoActivityResponse;
import com.nihongu.lokibe.Dto.Photo.PhotoReactionResponse;
import com.nihongu.lokibe.Dto.Photo.PhotoResponse;
import com.nihongu.lokibe.Dto.Photo.SendPhotoRequest;
import com.nihongu.lokibe.Entity.PhotoEntity;
import com.nihongu.lokibe.Entity.PhotoReactionEntity;
import com.nihongu.lokibe.Entity.PhotorecipientEntity;
import com.nihongu.lokibe.Entity.UserEntity;
import com.nihongu.lokibe.Exception.BadRequestException;
import com.nihongu.lokibe.Exception.ResourceNotFoundException;
import com.nihongu.lokibe.Mapper.UserMapper;
import com.nihongu.lokibe.Repositoty.FriendshipRepository;
import com.nihongu.lokibe.Repositoty.PhotoReactionRepository;
import com.nihongu.lokibe.Repositoty.PhotoRecipientRepository;
import com.nihongu.lokibe.Repositoty.PhotoRepository;
import com.nihongu.lokibe.Repositoty.UserRepository;
import com.nihongu.lokibe.Service.Notification.NotificationService;
import com.nihongu.lokibe.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoServiceImpl implements PhotoService {

    private final PhotoRepository photoRepository;
    private final PhotoRecipientRepository photoRecipientRepository;
    private final PhotoReactionRepository photoReactionRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional // Đảm bảo nếu lỗi giữa chừng, toàn bộ DB sẽ rollback lại
    public PhotoResponse sendPhoto(SendPhotoRequest request) {
        Long senderId = SecurityUtils.getCurrentUserId();

        // App gửi lại (mất mạng sau khi server đã lưu): trả lại ảnh cũ, không tạo ảnh và push trùng
        if (StringUtils.hasText(request.getClientId())) {
            PhotoEntity existing = photoRepository.findBySenderIdAndClientId(senderId, request.getClientId()).orElse(null);
            if (existing != null) return buildResponses(List.of(existing), Map.of()).get(0);
        }

        // Loại bỏ ID trùng và chính mình
        Set<Long> recipientIds = new LinkedHashSet<>(request.getRecipientIds());
        recipientIds.remove(senderId);
        if (recipientIds.isEmpty()) {
            throw new BadRequestException("Danh sách người nhận không hợp lệ.");
        }

        // Chỉ được gửi cho bạn bè: so sánh với danh sách bạn bè (1 query thay vì N query)
        Set<Long> friendIds = new HashSet<>(friendshipRepository.findAllFriendIdsByUserId(senderId));
        if (!friendIds.containsAll(recipientIds)) {
            throw new BadRequestException("Có người nhận không nằm trong danh sách bạn bè.");
        }

        // 1. Lưu ảnh gốc
        PhotoEntity photo = photoRepository.save(PhotoEntity.builder()
                .senderId(senderId)
                .imageUrl(request.getImageUrl())
                .caption(request.getCaption())
                .clientId(request.getClientId())
                .build());

        // 2. Lưu mỗi người nhận 1 bản ghi
        photoRecipientRepository.saveAll(recipientIds.stream()
                .map(id -> PhotorecipientEntity.builder()
                        .photoId(photo.getId())
                        .recipientId(id)
                        .build())
                .toList());

        // 3. Push FCM tới recipientIds để đánh thức app và cập nhật Widget ngoài màn hình chính
        PhotoResponse response = buildResponses(List.of(photo), Map.of()).get(0);
        String senderName = response.getSender() != null ? response.getSender().getDisplayName() : "Bạn bè";
        notificationService.sendToUsers(
                recipientIds,
                AppConstants.NOTI_TITLE_NEW_PHOTO,
                senderName + " vừa gửi một ảnh mới!",
                Map.of(
                        "type", "NEW_PHOTO",
                        "photoId", String.valueOf(photo.getId()),
                        "imageUrl", photo.getImageUrl(),
                        "senderName", senderName
                )
        );
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoResponse> getFeed(int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<PhotorecipientEntity> received = photoRecipientRepository
                .findByRecipientIdOrderByCreatedAtDesc(currentUserId, pageOf(page, size));

        // Map photoId -> recipient để lấy seenAt; giữ đúng thứ tự mới nhất
        Map<Long, PhotorecipientEntity> recipientByPhoto = received.stream()
                .collect(Collectors.toMap(PhotorecipientEntity::getPhotoId, Function.identity(), (a, b) -> a));
        Map<Long, PhotoEntity> photos = mapById(photoRepository.findAllById(recipientByPhoto.keySet()), PhotoEntity::getId);

        // Ảnh đã bị người gửi xoá thì bỏ qua
        List<PhotoEntity> ordered = received.stream()
                .map(r -> photos.get(r.getPhotoId()))
                .filter(Objects::nonNull)
                .toList();

        return buildResponses(ordered, recipientByPhoto);
    }

    @Override
    @Transactional(readOnly = true)
    public PhotoResponse getLatestPhotoForWidget() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PhotorecipientEntity latest = photoRecipientRepository
                .findFirstByRecipientIdOrderByCreatedAtDesc(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có ảnh nào để hiển thị trên Widget."));

        PhotoEntity photo = findPhoto(latest.getPhotoId());
        return buildResponses(List.of(photo), Map.of(photo.getId(), latest)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoResponse> getSentPhotos(int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<PhotoEntity> photos = photoRepository.findBySenderIdOrderByCreatedAtDesc(currentUserId, pageOf(page, size));
        return buildResponses(photos, Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoResponse> getHistory(Long friendId, int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = pageOf(page, size);
        List<PhotoEntity> photos;
        if (friendId == null) {
            photos = photoRepository.findHistory(currentUserId, pageable);
        } else if (friendId.equals(currentUserId)) {
            photos = photoRepository.findBySenderIdOrderByCreatedAtDesc(currentUserId, pageable);
        } else {
            photos = photoRepository.findHistoryFromFriend(currentUserId, friendId, pageable);
        }
        if (photos.isEmpty()) return List.of();

        // seenAt chỉ có với ảnh mình nhận
        Map<Long, PhotorecipientEntity> recipientByPhoto = photoRecipientRepository
                .findByRecipientIdAndPhotoIdIn(currentUserId, photos.stream().map(PhotoEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(PhotorecipientEntity::getPhotoId, Function.identity(), (a, b) -> a));
        return buildResponses(photos, recipientByPhoto);
    }

    @Override
    @Transactional
    public void markAsSeen(Long photoId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PhotorecipientEntity recipient = photoRecipientRepository.findByPhotoIdAndRecipientId(photoId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh."));

        // Chỉ ghi lần xem đầu tiên
        if (recipient.getSeenAt() == null) {
            recipient.setSeenAt(LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public void deletePhoto(Long photoId) {
        PhotoEntity photo = findPhoto(photoId);
        if (!photo.getSenderId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BadRequestException("Bạn chỉ có thể xoá ảnh của chính mình.");
        }
        photoRepository.delete(photo); // soft delete -> tự biến mất khỏi Feed người nhận
    }

    @Override
    @Transactional
    public PhotoReactionResponse react(AddReactionRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PhotoEntity photo = findPhoto(request.getPhotoId());
        checkCanAccess(photo, currentUserId);

        // Đã reaction rồi thì đổi emoji, chưa thì tạo mới
        PhotoReactionEntity reaction = photoReactionRepository
                .findByPhotoIdAndUserId(photo.getId(), currentUserId)
                .orElseGet(() -> PhotoReactionEntity.builder()
                        .photoId(photo.getId())
                        .userId(currentUserId)
                        .build());
        reaction.setEmoji(request.getEmoji());
        reaction = photoReactionRepository.save(reaction);

        UserEntity user = userRepository.findById(currentUserId).orElse(null);
        if (!photo.getSenderId().equals(currentUserId) && user != null) {
            notificationService.sendToUser(
                    photo.getSenderId(),
                    "Cảm xúc mới",
                    user.getDisplayName() + " đã thả " + request.getEmoji() + " vào ảnh của bạn",
                    Map.of("type", "PHOTO_REACTION", "photoId", String.valueOf(photo.getId()))
            );
        }
        return toReactionResponse(reaction, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoActivityResponse> getActivity(Long photoId) {
        PhotoEntity photo = findPhoto(photoId);
        if (!photo.getSenderId().equals(SecurityUtils.getCurrentUserId())) {
            // Chỉ người gửi xem được ai đã xem ảnh của mình
            throw new BadRequestException("Bạn chỉ xem được hoạt động trên ảnh của chính mình.");
        }
        List<PhotorecipientEntity> recipients = photoRecipientRepository.findByPhotoId(photoId);
        Map<Long, String> emojiByUser = photoReactionRepository.findByPhotoId(photoId).stream()
                .collect(Collectors.toMap(PhotoReactionEntity::getUserId, PhotoReactionEntity::getEmoji, (a, b) -> a));
        Map<Long, UserEntity> users = mapById(
                userRepository.findAllById(recipients.stream().map(PhotorecipientEntity::getRecipientId).toList()),
                UserEntity::getId);
        // Người xem gần nhất trước, người chưa xem ở cuối
        return recipients.stream()
                .filter(r -> users.containsKey(r.getRecipientId())) // tài khoản đã xoá thì bỏ
                .sorted(java.util.Comparator.comparing(PhotorecipientEntity::getSeenAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(r -> PhotoActivityResponse.builder()
                        .user(UserMapper.toProfile(users.get(r.getRecipientId())))
                        .seenAt(r.getSeenAt())
                        .emoji(emojiByUser.get(r.getRecipientId()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void removeReaction(Long photoId) {
        photoReactionRepository.findByPhotoIdAndUserId(photoId, SecurityUtils.getCurrentUserId())
                .ifPresent(photoReactionRepository::delete);
    }

    // ===================== Helpers =====================

    private Pageable pageOf(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, AppConstants.MAX_PAGE_SIZE));
    }

    private PhotoEntity findPhoto(Long photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh."));
    }

    // Chỉ người gửi hoặc người nhận ảnh mới được tương tác
    private void checkCanAccess(PhotoEntity photo, Long userId) {
        boolean allowed = photo.getSenderId().equals(userId)
                || photoRecipientRepository.findByPhotoIdAndRecipientId(photo.getId(), userId).isPresent();
        if (!allowed) {
            throw new BadRequestException("Bạn không có quyền truy cập ảnh này.");
        }
    }

    /**
     * Map danh sách ảnh -> DTO, load sender + reactions + user của reaction theo batch
     * (cố định ~2 query cho cả trang thay vì N+1).
     */
    private List<PhotoResponse> buildResponses(List<PhotoEntity> photos, Map<Long, PhotorecipientEntity> recipientByPhoto) {
        if (photos.isEmpty()) return List.of();

        List<Long> photoIds = photos.stream().map(PhotoEntity::getId).toList();
        Map<Long, List<PhotoReactionEntity>> reactionsByPhoto = photoReactionRepository.findByPhotoIdIn(photoIds).stream()
                .collect(Collectors.groupingBy(PhotoReactionEntity::getPhotoId));

        Set<Long> userIds = new HashSet<>();
        photos.forEach(p -> userIds.add(p.getSenderId()));
        reactionsByPhoto.values().forEach(list -> list.forEach(r -> userIds.add(r.getUserId())));
        Map<Long, UserEntity> users = mapById(userRepository.findAllById(userIds), UserEntity::getId);

        return photos.stream().map(photo -> {
            PhotorecipientEntity recipient = recipientByPhoto.get(photo.getId());
            List<PhotoReactionResponse> reactions = reactionsByPhoto.getOrDefault(photo.getId(), List.of()).stream()
                    .map(r -> toReactionResponse(r, users.get(r.getUserId())))
                    .toList();

            return PhotoResponse.builder()
                    .id(photo.getId())
                    .sender(UserMapper.toProfile(users.get(photo.getSenderId())))
                    .imageUrl(photo.getImageUrl())
                    .caption(photo.getCaption())
                    .createdAt(photo.getCreatedAt())
                    .seenAt(recipient != null ? recipient.getSeenAt() : null)
                    .reactions(reactions)
                    .build();
        }).toList();
    }

    private PhotoReactionResponse toReactionResponse(PhotoReactionEntity reaction, UserEntity user) {
        return PhotoReactionResponse.builder()
                .id(reaction.getId())
                .photoId(reaction.getPhotoId())
                .user(UserMapper.toProfile(user))
                .emoji(reaction.getEmoji())
                .createdAt(reaction.getCreatedAt())
                .build();
    }

    private static <T> Map<Long, T> mapById(Collection<T> items, Function<T, Long> idGetter) {
        return items.stream().collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a));
    }
}
