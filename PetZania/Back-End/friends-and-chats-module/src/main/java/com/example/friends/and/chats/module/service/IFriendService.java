package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.model.entity.FriendRequest;
import com.example.friends.and.chats.module.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface IFriendService {
    void sendFriendRequest();

    void acceptFriendRequest();

    void declineFriendRequest();

    void cancelFriendRequest();

    void getPendingRequests();

    void removeFriend();

    void getFriends();

    void areFriends();

    void follow();

    void unfollow();

    void getFollowers();

    void getFollowing();

    void block();

    void unblock();

    void isBlocked();

    void getBlockedUsers();

}
