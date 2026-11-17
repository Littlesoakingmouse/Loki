package com.nihongu.lokibe.Controller;

import com.nihongu.lokibe.Dto.Users.FriendshipResponse;
import com.nihongu.lokibe.Dto.Users.RespondFriendRequest;
import com.nihongu.lokibe.Dto.Users.SendFriendRequest;
import com.nihongu.lokibe.Dto.Users.UserProfileResponse;
import com.nihongu.lokibe.Service.Friendship.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    // Danh sách bạn bè
    @GetMapping
    public List<UserProfileResponse> getFriends() {
        return friendshipService.getFriends();
    }

    // Gửi lời mời kết bạn
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendshipResponse sendRequest(@Valid @RequestBody SendFriendRequest request) {
        return friendshipService.sendFriendRequest(request);
    }

    // Lời mời đang chờ mình xử lý
    @GetMapping("/requests/pending")
    public List<FriendshipResponse> getPendingRequests() {
        return friendshipService.getPendingRequests();
    }

    // Chấp nhận (ACCEPTED) hoặc chặn (BLOCKED)
    @PostMapping("/requests/respond")
    public FriendshipResponse respond(@Valid @RequestBody RespondFriendRequest request) {
        return friendshipService.respondFriendRequest(request);
    }

    // Từ chối / huỷ lời mời / huỷ kết bạn
    @DeleteMapping("/{friendshipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long friendshipId) {
        friendshipService.deleteFriendship(friendshipId);
    }

    // Chặn một người (huỷ luôn kết bạn / lời mời)
    @PostMapping("/users/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable Long userId) {
        friendshipService.block(userId);
    }

    @DeleteMapping("/users/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable Long userId) {
        friendshipService.unblock(userId);
    }

    // Danh sách người mình đã chặn
    @GetMapping("/blocked")
    public List<UserProfileResponse> getBlocked() {
        return friendshipService.getBlocked();
    }

    // Huỷ kết bạn theo ID người bạn
    @DeleteMapping("/users/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfriend(@PathVariable Long friendId) {
        friendshipService.unfriend(friendId);
    }
}
