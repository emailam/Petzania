package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.Block;
import com.example.adoption_and_breeding_module.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {
    boolean existsByBlocker_UserIdAndBlocked_UserId(UUID blockerId, UUID blockedId);

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);
    List<Block> findByBlocker(User blocker);
    Page<Block> findBlocksByBlocker(User blocker, Pageable pageable);

    void deleteByBlocker_UserIdAndBlocked_UserId(UUID blockerId, UUID blockedId);
    int countByBlocker(User blocker);
}
