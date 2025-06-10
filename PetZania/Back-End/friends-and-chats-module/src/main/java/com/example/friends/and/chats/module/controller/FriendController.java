package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.model.dto.friend.BlockDTO;
import com.example.friends.and.chats.module.model.dto.friend.FollowDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendshipDTO;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.service.IFriendService;
import com.example.friends.and.chats.module.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final IFriendService friendService;

    @Tag(name = "post", description = "Send Friend Request")
    @PostMapping("/send-request/{receiverId}")
    public ResponseEntity<FriendRequestDTO> sendFriendRequest(@PathVariable UUID receiverId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.sendFriendRequest(userPrincipal.getUserId(), receiverId));
    }

    @Tag(name = "post", description = "Accept Friend Request")
    @PostMapping("/accept-request/{requestId}")
    public ResponseEntity<FriendshipDTO> acceptFriendRequest(@PathVariable UUID requestId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.acceptFriendRequest(requestId, userPrincipal.getUserId()));
    }

    @Tag(name = "put", description = "Decline Friend Request")
    @PutMapping("/decline-request/{requestId}")
    public ResponseEntity<String> declineFriendRequest(@PathVariable UUID requestId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.declineFriendRequest(requestId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Request was removed successfully!");
    }

    @Tag(name = "delete", description = "Remove Friend")
    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<String> removeFriend(@PathVariable UUID friendId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.removeFriend(userPrincipal.getUserId(), friendId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Friend was removed successfully!");
    }

    @Tag(name = "post", description = "Follow User")
    @PostMapping("/follow/{userId}")
    public ResponseEntity<FollowDTO> followUser(@PathVariable UUID userId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.followUser(userPrincipal.getUserId(), userId));
    }

    @Tag(name = "put", description = "Unfollow User")
    @PutMapping("/unfollow/{userId}")
    public ResponseEntity<String> unfollowUser(@PathVariable UUID userId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.unfollowUser(userPrincipal.getUserId(), userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Follow was removed successfully!");
    }

    @Tag(name = "post", description = "Block User")
    @PostMapping("/block/{userId}")
    public ResponseEntity<BlockDTO> blockUser(@PathVariable UUID userId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.blockUser(userPrincipal.getUserId(), userId));
    }

    @Tag(name = "put", description = "Unblock User")
    @PutMapping("/unblock/{userId}")
    public ResponseEntity<?> unblockUser(@PathVariable UUID userId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.unblockUser(userPrincipal.getUserId(), userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Block was removed successfully!");
    }

    @Tag(name = "get", description = "Get Friends")
    @GetMapping("/getFriends")
    public ResponseEntity<Page<FriendshipDTO>> getFriends(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        Page<FriendshipDTO> friendshipDTOS = friendService.getFriendships(userPrincipal.getUserId(), page, size, sortBy, direction);
        return friendshipDTOS.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(friendshipDTOS);

    }

    @Tag(name = "get", description = "Get Following")
    @GetMapping("/getFollowing")
    public ResponseEntity<Page<FollowDTO>> getFollowing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        Page<FollowDTO> followDTOS = friendService.getFollowing(userPrincipal.getUserId(), page, size, sortBy, direction);
        return followDTOS.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(followDTOS);
    }

    @Tag(name = "get", description = "Get Followers")
    @GetMapping("/getFollowers")
    public ResponseEntity<Page<FollowDTO>> getFollowers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        Page<FollowDTO> followDTOS = friendService.getFollowers(userPrincipal.getUserId(), page, size, sortBy, direction);
        return followDTOS.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(followDTOS);
    }


    @Tag(name = "get", description = "Get Blocked Users")
    @GetMapping("/getBlockedUsers")
    public ResponseEntity<Page<BlockDTO>> getBlockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        Page<BlockDTO> blockDTOS = friendService.getBlockedUsers(userPrincipal.getUserId(), page, size, sortBy, direction);
        return blockDTOS.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(blockDTOS);
    }

    @Tag(name = "get", description = "Get Number Of Following")
    @GetMapping("/getNumberOfFollowing")
    public ResponseEntity<Integer> getNumberOfFollowing() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        int count = friendService.getFollowingCount(userPrincipal.getUserId());
        return ResponseEntity.ok(count);
    }

    @Tag(name = "get", description = "Get Number Of Followers")
    @GetMapping("/getNumberOfFollowers")
    public ResponseEntity<Integer> getNumberOfFollowers() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        int count = friendService.getFollowersCount(userPrincipal.getUserId());
        return ResponseEntity.ok(count);
    }

    @Tag(name = "get", description = "Get Number Of Blocked Users")
    @GetMapping("/getNumberOfBlockedUsers")
    public ResponseEntity<Integer> getNumberOfBlockedUsers() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        int count = friendService.getBlockedUsersCount(userPrincipal.getUserId());
        return ResponseEntity.ok(count);
    }

    @Tag(name = "get", description = "Get Number Of Friends")
    @GetMapping("/getNumberOfFriends")
    public ResponseEntity<Integer> getNumberOfFriends() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(friendService.getNumberOfFriends(userPrincipal.getUserId()));
    }


}
