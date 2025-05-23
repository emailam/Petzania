package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.model.dto.FriendRequestDTO;
import com.example.friends.and.chats.module.model.dto.FriendshipDTO;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.service.IFriendService;
import com.example.friends.and.chats.module.util.SecurityUtils;
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

    @PostMapping("/send-request/{receiverId}")
    public ResponseEntity<FriendRequestDTO> sendFriendRequest(@PathVariable UUID receiverId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.sendFriendRequest(userPrincipal.getUserId(), receiverId));
    }

    @PostMapping("/accept-request/{requestId}")
    public ResponseEntity<FriendshipDTO> acceptFriendRequest(@PathVariable UUID requestId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(friendService.acceptFriendRequest(requestId));
    }

    @PutMapping("/decline-request/{requestId}")
    public ResponseEntity<String> declineFriendRequest(@PathVariable UUID requestId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.declineFriendRequest(requestId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Request was removed successfully!");
    }

    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<String> removeFriend(@PathVariable UUID friendId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        friendService.removeFriend(userPrincipal.getUserId(), friendId);
        return new ResponseEntity<>(null);
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
//    @PostMapping("/follow/{userId}")
//    public ResponseEntity<?> followUser(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
//
//    @PostMapping("/unfollow/{userId}")
//    public ResponseEntity<?> unfollowUser(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
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
//    @PostMapping("/block/{userId}")
//    public ResponseEntity<?> blockUser(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
//
//    @PostMapping("/unblock/{userId}")
//    public ResponseEntity<?> unblockUser(@PathVariable UUID userId) {
//        return new ResponseEntity<>(null);
//    }
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
