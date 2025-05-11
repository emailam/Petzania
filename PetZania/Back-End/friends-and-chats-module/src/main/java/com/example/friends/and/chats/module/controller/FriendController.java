package com.example.friends.and.chats.module.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/friends")
public class FriendController {
    // friend
    @PostMapping("/send-request/{receiverId}")
    public ResponseEntity<?> sendFriendRequest(@PathVariable UUID receiverId) {
        return new ResponseEntity<>(null);
    }

    @PostMapping("/accept-request/{requestId}")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable UUID requestId) {
        return new ResponseEntity<>(null);
    }

    @PostMapping("/decline-request/{requestId}")
    public ResponseEntity<?> declineFriendRequest(@PathVariable UUID requestId) {
        return new ResponseEntity<>(null);
    }

    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable UUID friendId) {
        return new ResponseEntity<>(null);
    }

    @GetMapping("/getFriends/{userId}")
    public ResponseEntity<?> getFriends(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    @GetMapping("/getNumberOfFriends/{userId}")
    public ResponseEntity<?> getNumberOfFriends(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    @GetMapping("/suggestions/{userId}")
    public ResponseEntity<?> getSuggestedFriends(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    // follow
    @PostMapping("/follow/{userId}")
    public ResponseEntity<?> followUser(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    @PostMapping("/unfollow/{userId}")
    public ResponseEntity<?> unfollowUser(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    @GetMapping("/getFollowing")
    public ResponseEntity<?> getFollowing() {
        return new ResponseEntity<>(null);
    }

    @GetMapping("/getFollowers")
    public ResponseEntity<?> getFollowers() {
        return new ResponseEntity<>(null);
    }

    @GetMapping("/getNumberOfFollowing/{userId}")
    public ResponseEntity<?> getNumberOfFollowing(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    @GetMapping("/getNumberOfFollowers/{userId}")
    public ResponseEntity<?> getNumberOfFollowers(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    // block
    @PostMapping("/block/{userId}")
    public ResponseEntity<?> blockUser(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    @PostMapping("/unblock/{userId}")
    public ResponseEntity<?> unblockUser(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    @PostMapping("/report/{userId}")
    public ResponseEntity<?> reportUser(@PathVariable UUID userId) {
        return new ResponseEntity<>(null);
    }

    @PostMapping("/getBlockedUsers")
    public ResponseEntity<?> getBlockedUsers() {
        return new ResponseEntity<>(null);
    }
}
