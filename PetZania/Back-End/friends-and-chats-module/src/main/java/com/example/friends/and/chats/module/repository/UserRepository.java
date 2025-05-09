package com.example.friendsAndChatsModule.repository;

import com.example.friendsAndChatsModule.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}