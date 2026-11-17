package com.nihongu.lokibe.Service.Friendship;

import com.nihongu.lokibe.Dto.Users.FriendshipResponse;
import com.nihongu.lokibe.Dto.Users.RespondFriendRequest;
import com.nihongu.lokibe.Dto.Users.SendFriendRequest;
import com.nihongu.lokibe.Dto.Users.UserProfileResponse;

import java.util.List;

public interface FriendshipService {

    FriendshipResponse sendFriendRequest(SendFriendRequest request);

    // Người nhận chấp nhận (ACCEPTED) hoặc chặn (BLOCKED) lời mời
    FriendshipResponse respondFriendRequest(RespondFriendRequest request);

    // Dùng chung cho: từ chối lời mời / huỷ lời mời đã gửi / huỷ kết bạn
    void deleteFriendship(Long friendshipId);

    // Huỷ kết bạn theo ID người bạn (app có sẵn ID này từ danh sách bạn bè)
    void unfriend(Long friendId);

    // Lời mời đang chờ mình xử lý
    List<FriendshipResponse> getPendingRequests();

    List<UserProfileResponse> getFriends();

    // Chặn: huỷ kết bạn/lời mời, người kia không tìm thấy mình, không gửi lời mời hay tin nhắn được nữa
    void block(Long userId);

    // Chỉ người chặn mới gỡ được
    void unblock(Long userId);

    // Những người mình đã chặn
    List<UserProfileResponse> getBlocked();
}
