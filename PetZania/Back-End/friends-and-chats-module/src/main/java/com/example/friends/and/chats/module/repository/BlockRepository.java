package com.example.friends.and.chats.module.repository;

import com.example.friends.and.chats.module.model.entity.Block;
import com.example.friends.and.chats.module.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    List<Block> findByBlocker(User blocker);

    void deleteByBlockerAndBlocked(User blocker, User blocked);
    Page<Block> findBlocksByBlocker(User blocker, Pageable pageable);
    int countByBlocker(User blocker);
}
