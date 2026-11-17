package com.nihongu.lokibe.Service.Conversation;

import com.nihongu.lokibe.Constant.AppConstants;
import com.nihongu.lokibe.Dto.Conversation.ConversationResponse;
import com.nihongu.lokibe.Dto.Conversation.MessageResponse;
import com.nihongu.lokibe.Dto.Conversation.SendMessageRequest;
import com.nihongu.lokibe.Dto.Photo.PhotoResponse;
import com.nihongu.lokibe.Entity.ConversationEntity;
import com.nihongu.lokibe.Entity.MessageEntity;
import com.nihongu.lokibe.Entity.PhotoEntity;
import com.nihongu.lokibe.Entity.UserEntity;
import com.nihongu.lokibe.Enum.MessageType;
import com.nihongu.lokibe.Exception.BadRequestException;
import com.nihongu.lokibe.Exception.ResourceNotFoundException;
import com.nihongu.lokibe.Mapper.UserMapper;
import com.nihongu.lokibe.Repositoty.*;
import com.nihongu.lokibe.Service.Notification.NotificationService;
import com.nihongu.lokibe.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final FriendshipRepository friendshipRepository;
    private final PhotoRepository photoRepository;
    private final PhotoRecipientRepository photoRecipientRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        Long senderId = SecurityUtils.getCurrentUserId();
        Long recipientId = request.getRecipientId();

        if (senderId.equals(recipientId)) {
            throw new BadRequestException("Không thể nhắn tin cho chính mình.");
        }
        // App gửi lại (mất mạng sau khi server đã lưu): trả lại tin cũ, không tạo tin và push trùng
        if (request.getClientId() != null && !request.getClientId().isBlank()) {
            MessageEntity existing = messageRepository.findBySenderIdAndClientId(senderId, request.getClientId()).orElse(null);
            if (existing != null) {
                UserEntity sender = userRepository.findById(senderId).orElse(null);
                PhotoEntity photo = existing.getPhotoId() != null ? photoRepository.findById(existing.getPhotoId()).orElse(null) : null;
                PhotoResponse photoResponse = photo != null
                        ? toPhotoResponse(photo, userRepository.findById(photo.getSenderId()).orElse(null)) : null;
                return toMessageResponse(existing, sender, photoResponse);
            }
        }

        if (!friendshipRepository.isFriend(senderId, recipientId)) {
            throw new BadRequestException("Chỉ có thể nhắn tin với bạn bè.");
        }

        // Reply ảnh: ảnh phải nằm giữa 2 người (1 người gửi, người kia nhận)
        PhotoEntity photo = null;
        MessageType type = request.getType() != null ? request.getType() : MessageType.TEXT;
        if (request.getPhotoId() != null) {
            photo = photoRepository.findById(request.getPhotoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh."));
            checkPhotoBelongsToPair(photo, senderId, recipientId);
            type = MessageType.PHOTO_REPLY;
        }

        ConversationEntity conversation = getOrCreateConversation(senderId, recipientId);

        MessageEntity message = messageRepository.save(MessageEntity.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .content(request.getContent().trim())
                .type(type)
                .photoId(photo != null ? photo.getId() : null)
                .clientId(request.getClientId())
                .build());

        // Cập nhật thông tin tin nhắn cuối để hiển thị ở Inbox (managed entity -> tự flush)
        conversation.setLastMessageContent(message.getContent());
        conversation.setLastMessageAt(message.getCreatedAt());
        conversation.setLastMessageSenderId(senderId);

        UserEntity sender = userRepository.findById(senderId).orElse(null);
        String senderName = sender != null ? sender.getDisplayName() : AppConstants.NOTI_TITLE_NEW_MESSAGE;
        notificationService.sendToUser(
                recipientId,
                senderName,
                message.getContent(),
                Map.of(
                        "type", "NEW_MESSAGE",
                        "conversationId", String.valueOf(conversation.getId()),
                        "senderId", String.valueOf(senderId)
                )
        );
        return toMessageResponse(message, sender, photo != null ? toPhotoResponse(photo, sender) : null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<ConversationEntity> conversations = conversationRepository
                .findAllByUserIdOrderByLastMessageAtDesc(currentUserId, pageOf(page, size));

        // Load "người kia" của mọi conversation trong 1 query
        List<Long> otherIds = conversations.stream().map(c -> otherUserId(c, currentUserId)).toList();
        Map<Long, UserEntity> users = mapById(userRepository.findAllById(otherIds), UserEntity::getId);

        return conversations.stream().map(c -> ConversationResponse.builder()
                .id(c.getId())
                .otherUser(UserMapper.toProfile(users.get(otherUserId(c, currentUserId))))
                .lastMessageContent(c.getLastMessageContent())
                .lastMessageAt(c.getLastMessageAt())
                .lastMessageSenderId(c.getLastMessageSenderId())
                .unreadCount(messageRepository.countUnreadMessages(c.getId(), currentUserId))
                .build()
        ).toList();
    }

     @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConversationEntity conversation = findConversationOfUser(conversationId, currentUserId);

        List<MessageEntity> messages = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageOf(page, size));

        // Conversation chỉ có 2 người -> load cả 2 một lần
        Map<Long, UserEntity> users = mapById(
                userRepository.findAllById(List.of(conversation.getUser1Id(), conversation.getUser2Id())),
                UserEntity::getId);

        // Load các ảnh được reply trong trang này
        List<Long> photoIds = messages.stream().map(MessageEntity::getPhotoId).filter(Objects::nonNull).toList();
        Map<Long, PhotoEntity> photos = mapById(photoRepository.findAllById(photoIds), PhotoEntity::getId);

        return messages.stream().map(m -> {
            PhotoEntity photo = m.getPhotoId() != null ? photos.get(m.getPhotoId()) : null;
            PhotoResponse photoResponse = photo != null ? toPhotoResponse(photo, users.get(photo.getSenderId())) : null;
            return toMessageResponse(m, users.get(m.getSenderId()), photoResponse);
        }).toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long conversationId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        findConversationOfUser(conversationId, currentUserId);
        messageRepository.markAllAsRead(conversationId, currentUserId);
    }

    // ===================== Helpers =====================

    // Quy ước luôn lưu user1Id < user2Id -> mỗi cặp user chỉ có đúng 1 conversation
    private ConversationEntity getOrCreateConversation(Long userA, Long userB) {
        Long user1 = Math.min(userA, userB);
        Long user2 = Math.max(userA, userB);
        return conversationRepository.findByUser1IdAndUser2Id(user1, user2)
                .orElseGet(() -> conversationRepository.save(ConversationEntity.builder()
                        .user1Id(user1)
                        .user2Id(user2)
                        .build()));
    }

    private ConversationEntity findConversationOfUser(Long conversationId, Long userId) {
        ConversationEntity c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện."));
        if (!c.getUser1Id().equals(userId) && !c.getUser2Id().equals(userId)) {
            // Trả 404 thay vì 403 để không lộ sự tồn tại của conversation
            throw new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện.");
        }
        return c;
    }

    private void checkPhotoBelongsToPair(PhotoEntity photo, Long userA, Long userB) {
        boolean valid = (photo.getSenderId().equals(userB)
                && photoRecipientRepository.findByPhotoIdAndRecipientId(photo.getId(), userA).isPresent())
                || (photo.getSenderId().equals(userA)
                && photoRecipientRepository.findByPhotoIdAndRecipientId(photo.getId(), userB).isPresent());
        if (!valid) {
            throw new BadRequestException("Ảnh không thuộc cuộc trò chuyện này.");
        }
    }

    private Long otherUserId(ConversationEntity c, Long currentUserId) {
        return c.getUser1Id().equals(currentUserId) ? c.getUser2Id() : c.getUser1Id();
    }

    private Pageable pageOf(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, AppConstants.MAX_PAGE_SIZE));
    }

    private MessageResponse toMessageResponse(MessageEntity m, UserEntity sender, PhotoResponse photo) {
        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .sender(UserMapper.toProfile(sender))
                .content(m.getContent())
                .type(m.getType())
                .photo(photo)
                .isRead(m.isRead())
                .readAt(m.getReadAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    // Bản rút gọn của ảnh để hiển thị trong khung chat (không cần reactions)
    private PhotoResponse toPhotoResponse(PhotoEntity photo, UserEntity sender) {
        return PhotoResponse.builder()
                .id(photo.getId())
                .sender(UserMapper.toProfile(sender))
                .imageUrl(photo.getImageUrl())
                .caption(photo.getCaption())
                .createdAt(photo.getCreatedAt())
                .build();
    }

    private static <T> Map<Long, T> mapById(List<T> items, Function<T, Long> idGetter) {
        return items.stream().collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a));
    }
}
