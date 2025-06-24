package com.example.registrationmodule.repository;

import com.example.registrationmodule.model.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {
    boolean existsByBlocker_UserIdAndBlocked_UserId(UUID blockerId, UUID blockedId);
    void deleteByBlocker_UserIdAndBlocked_UserId(UUID blockerId, UUID blockedId);
}

