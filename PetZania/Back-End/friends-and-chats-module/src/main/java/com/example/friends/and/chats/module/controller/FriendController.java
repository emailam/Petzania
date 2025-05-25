package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.model.dto.BlockDTO;
import com.example.friends.and.chats.module.model.dto.FollowDTO;
import com.example.friends.and.chats.module.model.dto.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.FriendshipDTO;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.service.IFriendService;
import com.example.friends.and.chats.module.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
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
//
//    @GetMapping("/getFriends/{userId}")
//    public ResponseEntity<?> getFriends(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
//
//    @GetMapping("/getNumberOfFriends/{userId}")
//    public ResponseEntity<?> getNumberOfFriends(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
//
//    // follow
//
//
//    @GetMapping("/getFollowing")
//    public ResponseEntity<?> getFollowing() {
//        return new ResponseEntity<>(null);
//    }
//
//    @GetMapping("/getFollowers")
//    public ResponseEntity<?> getFollowers() {
//        return new ResponseEntity<>(null);
//    }
//
//    @GetMapping("/getNumberOfFollowing/{userId}")
//    public ResponseEntity<?> getNumberOfFollowing(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
//
//    @GetMapping("/getNumberOfFollowers/{userId}")
//    public ResponseEntity<?> getNumberOfFollowers(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
//
//    // block
//
//    @PostMapping("/report/{userId}")
//    public ResponseEntity<?> reportUser(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
//
//    @PostMapping("/getBlockedUsers")
//    public ResponseEntity<?> getBlockedUsers() {
//        return new ResponseEntity<>(null);
//    }
}
