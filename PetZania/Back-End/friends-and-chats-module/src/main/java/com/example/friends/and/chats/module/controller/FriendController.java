package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.model.dto.friend.BlockDTO;
import com.example.friends.and.chats.module.model.dto.friend.FollowDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendshipDTO;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.service.IFriendService;
import com.example.friends.and.chats.module.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Friend", description = "Operations related to friend requests, friendships, follows, and blocks")
public class FriendController {
    private final IFriendService friendService;

    @Operation(summary = "Get all received friend requests", description = "Returns a list of received friend requests for the current user")
    @GetMapping("/received-requests")
    public ResponseEntity<Page<FriendRequestDTO>> getReceivedFriendRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        Page<FriendRequestDTO> requests = friendService.getReceivedFriendRequests(userPrincipal.getUserId(), page, size, sortBy, direction);
        return requests.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(requests);
    }

    @Operation(summary = "Send friend request", description = "Send a friend request to another user by their ID")
    @PostMapping("/send-request/{receiverId}")
    public ResponseEntity<FriendRequestDTO> sendFriendRequest(@PathVariable UUID receiverId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.sendFriendRequest(userPrincipal.getUserId(), receiverId));
    }

    @Operation(summary = "Accept friend request", description = "Accept a pending friend request by request ID")
    @PostMapping("/accept-request/{requestId}")
    public ResponseEntity<FriendshipDTO> acceptFriendRequest(@PathVariable UUID requestId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.acceptFriendRequest(requestId, userPrincipal.getUserId()));
    }

    @Operation(summary = "Decline friend request", description = "Decline a pending friend request by request ID")
    @PutMapping("/cancel-request/{requestId}")
    public ResponseEntity<String> declineFriendRequest(@PathVariable UUID requestId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.cancelFriendRequest(requestId, userPrincipal.getUserId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Request was removed successfully!");
    }

    @Operation(summary = "Remove friend", description = "Remove an existing friend by friend ID")
    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<String> removeFriend(@PathVariable UUID friendId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.removeFriend(userPrincipal.getUserId(), friendId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Friend was removed successfully!");
    }

    @Operation(summary = "Follow user", description = "Follow another user by their ID")
    @PostMapping("/follow/{userId}")
    public ResponseEntity<FollowDTO> followUser(@PathVariable UUID userId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.followUser(userPrincipal.getUserId(), userId));
    }

    @Operation(summary = "Unfollow user", description = "Unfollow a user by their ID")
    @PutMapping("/unfollow/{userId}")
    public ResponseEntity<String> unfollowUser(@PathVariable UUID userId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.unfollowUser(userPrincipal.getUserId(), userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Follow was removed successfully!");
    }

    @Operation(summary = "Block user", description = "Block a user by their ID")
    @PostMapping("/block/{userId}")
    public ResponseEntity<BlockDTO> blockUser(@PathVariable UUID userId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.blockUser(userPrincipal.getUserId(), userId));
    }

    @Operation(summary = "Unblock user", description = "Unblock a previously blocked user by their ID")
    @PutMapping("/unblock/{userId}")
    public ResponseEntity<?> unblockUser(@PathVariable UUID userId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.unblockUser(userPrincipal.getUserId(), userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Block was removed successfully!");
    }

    @Operation(summary = "Get friends list", description = "Retrieve all friends for the current user")
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

    @Operation(summary = "Get following users", description = "Retrieve a paginated list of users the current user is following")
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

    @Operation(summary = "Get followers", description = "Retrieve a paginated list of users following the current user")
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


    @Operation(summary = "Get blocked users", description = "Retrieve a list of users blocked by the current user")
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

    @Operation(summary = "Get number of following users", description = "Returns the total number of users the current user is following")
    @GetMapping("/getNumberOfFollowing")
    public ResponseEntity<Integer> getNumberOfFollowing() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        int count = friendService.getFollowingCount(userPrincipal.getUserId());
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Get number of followers", description = "Returns the total number of users following the current user")
    @GetMapping("/getNumberOfFollowers")
    public ResponseEntity<Integer> getNumberOfFollowers() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        int count = friendService.getFollowersCount(userPrincipal.getUserId());
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Get number of blocked users", description = "Returns the total number of users blocked by the current user")
    @GetMapping("/getNumberOfBlockedUsers")
    public ResponseEntity<Integer> getNumberOfBlockedUsers() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        int count = friendService.getBlockedUsersCount(userPrincipal.getUserId());
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Get number of friends", description = "Returns the total number of friends the current user has")
    @GetMapping("/getNumberOfFriends")
    public ResponseEntity<Integer> getNumberOfFriends() {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(friendService.getNumberOfFriends(userPrincipal.getUserId()));
    }


}
