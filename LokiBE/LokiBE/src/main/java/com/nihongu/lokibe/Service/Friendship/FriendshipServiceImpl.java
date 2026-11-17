package com.nihongu.lokibe.Service.Friendship;

import com.nihongu.lokibe.Constant.AppConstants;
import com.nihongu.lokibe.Dto.Users.FriendshipResponse;
import com.nihongu.lokibe.Dto.Users.RespondFriendRequest;
import com.nihongu.lokibe.Dto.Users.SendFriendRequest;
import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import com.nihongu.lokibe.Entity.FriendshipsEntity;
import com.nihongu.lokibe.Entity.UserEntity;
import com.nihongu.lokibe.Enum.Status;
import com.nihongu.lokibe.Exception.BadRequestException;
import com.nihongu.lokibe.Exception.ResourceNotFoundException;
import com.nihongu.lokibe.Mapper.UserMapper;
import com.nihongu.lokibe.Repositoty.FriendshipRepository;
import com.nihongu.lokibe.Repositoty.UserRepository;
import com.nihongu.lokibe.Service.Notification.NotificationService;
import com.nihongu.lokibe.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FriendshipResponse sendFriendRequest(SendFriendRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long addresseeId = request.getAddresseeId();

        if (currentUserId.equals(addresseeId)) {
            throw new BadRequestException("Không thể tự kết bạn với chính mình.");
        }
        if (!userRepository.existsById(addresseeId)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng.");
        }

        // Đã có quan hệ (dù ai gửi trước) -> xử lý theo trạng thái hiện tại
        FriendshipsEntity existing = friendshipRepository.findFriendshipBetween(currentUserId, addresseeId).orElse(null);
        if (existing != null) {
            switch (existing.getStatus()) {
                case ACCEPTED -> throw new BadRequestException("Hai bạn đã là bạn bè.");
                case BLOCKED -> throw new BadRequestException("Không thể gửi lời mời tới người dùng này.");
                case PENDING -> {
                    // Người kia đã gửi lời mời cho mình trước -> tự động chấp nhận
                    if (existing.getAddresseeId().equals(currentUserId)) {
                        checkFriendLimit(currentUserId, addresseeId);
                        existing.setStatus(Status.ACCEPTED);
                        return toResponse(existing);
                    }
                    throw new BadRequestException("Bạn đã gửi lời mời trước đó.");
                }
            }
        }

        checkFriendLimit(currentUserId);

        FriendshipsEntity friendship = friendshipRepository.save(FriendshipsEntity.builder()
                .requesterId(currentUserId)
                .addresseeId(addresseeId)
                .status(Status.PENDING)
                .build());

        FriendshipResponse response = toResponse(friendship);
        String requesterName = response.getRequester() != null ? response.getRequester().getDisplayName() : "Ai đó";
        notificationService.sendToUser(
                addresseeId,
                "Lời mời kết bạn mới",
                requesterName + " đã gửi lời mời kết bạn cho bạn",
                Map.of("type", "FRIEND_REQUEST", "friendshipId", String.valueOf(friendship.getId()))
        );
        return response;
    }

    @Override
    @Transactional
    public FriendshipResponse respondFriendRequest(RespondFriendRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        FriendshipsEntity friendship = findFriendship(request.getFriendshipId());

        // Chỉ người NHẬN mới được phản hồi lời mời đang PENDING
        if (!friendship.getAddresseeId().equals(currentUserId)) {
            throw new BadRequestException("Bạn không có quyền phản hồi lời mời này.");
        }
        if (friendship.getStatus() != Status.PENDING) {
            throw new BadRequestException("Lời mời này đã được xử lý.");
        }
        if (request.getStatus() == Status.PENDING) {
            throw new BadRequestException("Trạng thái phản hồi không hợp lệ.");
        }

        if (request.getStatus() == Status.ACCEPTED) {
            checkFriendLimit(currentUserId, friendship.getRequesterId());
        } else if (request.getStatus() == Status.BLOCKED) {
            // Quy ước: người chặn luôn là requester
            friendship.setAddresseeId(friendship.getRequesterId());
            friendship.setRequesterId(currentUserId);
        }
        friendship.setStatus(request.getStatus());
        return toResponse(friendship);
    }

    @Override
    @Transactional
    public void deleteFriendship(Long friendshipId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        FriendshipsEntity friendship = findFriendship(friendshipId);

        boolean isParticipant = friendship.getRequesterId().equals(currentUserId)
                || friendship.getAddresseeId().equals(currentUserId);
        if (!isParticipant) {
            throw new BadRequestException("Bạn không có quyền thao tác với mối quan hệ này.");
        }
        // Không cho người bị chặn tự gỡ chặn (người chặn là requester)
        if (friendship.getStatus() == Status.BLOCKED && !friendship.getRequesterId().equals(currentUserId)) {
            throw new BadRequestException("Bạn không có quyền thao tác với mối quan hệ này.");
        }

        friendshipRepository.delete(friendship); // soft delete nhờ @SQLDelete
    }

    @Override
    @Transactional
    public void unfriend(Long friendId) {
        FriendshipsEntity friendship = friendshipRepository
                .findFriendshipBetween(SecurityUtils.getCurrentUserId(), friendId)
                .filter(f -> f.getStatus() == Status.ACCEPTED)
                .orElseThrow(() -> new ResourceNotFoundException("Hai bạn chưa là bạn bè."));
        friendshipRepository.delete(friendship);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getPendingRequests() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<FriendshipsEntity> pending = friendshipRepository.findByAddresseeIdAndStatus(currentUserId, Status.PENDING);

        // Load toàn bộ user liên quan trong 1 query
        Map<Long, UserEntity> users = loadUsers(pending.stream()
                .flatMap(f -> Stream.of(f.getRequesterId(), f.getAddresseeId()))
                .toList());

        return pending.stream().map(f -> toResponse(f, users)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getFriends() {
        List<Long> friendIds = friendshipRepository.findAllFriendIdsByUserId(SecurityUtils.getCurrentUserId());
        return userRepository.findAllById(friendIds).stream()
                .map(UserMapper::toProfile)
                .toList();
    }

    @Override
    @Transactional
    public void block(Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BadRequestException("Không thể tự chặn chính mình.");
        }
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng.");
        }
        FriendshipsEntity relation = friendshipRepository.findFriendshipBetween(currentUserId, userId)
                .orElseGet(() -> FriendshipsEntity.builder().build());
        if (relation.getStatus() == Status.BLOCKED && !currentUserId.equals(relation.getRequesterId())) {
            // Người kia đã chặn mình trước: giữ nguyên, họ mới gỡ được
            return;
        }
        relation.setRequesterId(currentUserId);
        relation.setAddresseeId(userId);
        relation.setStatus(Status.BLOCKED);
        friendshipRepository.save(relation);
    }

    @Override
    @Transactional
    public void unblock(Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        FriendshipsEntity relation = friendshipRepository.findFriendshipBetween(currentUserId, userId)
                .filter(f -> f.getStatus() == Status.BLOCKED && f.getRequesterId().equals(currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa chặn người này."));
        friendshipRepository.delete(relation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getBlocked() {
        List<Long> ids = friendshipRepository.findByRequesterIdAndStatus(SecurityUtils.getCurrentUserId(), Status.BLOCKED)
                .stream().map(FriendshipsEntity::getAddresseeId).toList();
        return userRepository.findAllById(ids).stream().map(UserMapper::toProfile).toList();
    }

    // ===================== Helpers =====================

    // Giống Locket: tối đa MAX_FRIENDS bạn; kiểm tra cả hai phía khi kết bạn
    private void checkFriendLimit(Long... userIds) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        for (Long userId : userIds) {
            if (friendshipRepository.findAllFriendIdsByUserId(userId).size() >= AppConstants.MAX_FRIENDS) {
                throw new BadRequestException(userId.equals(currentUserId)
                        ? "Bạn đã đủ " + AppConstants.MAX_FRIENDS + " bạn bè."
                        : "Người này đã đủ " + AppConstants.MAX_FRIENDS + " bạn bè.");
            }
        }
    }

    private FriendshipsEntity findFriendship(Long id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời kết bạn."));
    }

    private Map<Long, UserEntity> loadUsers(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (a, b) -> a));
    }

    private FriendshipResponse toResponse(FriendshipsEntity f) {
        return toResponse(f, loadUsers(List.of(f.getRequesterId(), f.getAddresseeId())));
    }

    private FriendshipResponse toResponse(FriendshipsEntity f, Map<Long, UserEntity> users) {
        return FriendshipResponse.builder()
                .id(f.getId())
                .requester(UserMapper.toProfile(users.get(f.getRequesterId())))
                .addressee(UserMapper.toProfile(users.get(f.getAddresseeId())))
                .status(f.getStatus())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
