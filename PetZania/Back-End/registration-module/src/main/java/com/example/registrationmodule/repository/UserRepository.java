package com.example.registrationmodule.repository;

import com.example.registrationmodule.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE " +
            "NOT EXISTS (" +
            "    SELECT b FROM Block b WHERE " +
            "    (b.blocker.userId = :currentUserId AND b.blocked.userId = u.userId) " +
            "    OR (b.blocker.userId = u.userId AND b.blocked.userId = :currentUserId)" +
            ")")
    Page<User> findAllExcludingBlocked(@Param("currentUserId") UUID currentUserId, Pageable pageable);


    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT(:prefix, '%')) " +
            "AND NOT EXISTS (" +
            "    SELECT b FROM Block b WHERE " +
            "    (b.blocker.userId = :currentUserId AND b.blocked.userId = u.userId) " +
            "    OR (b.blocker.userId = u.userId AND b.blocked.userId = :currentUserId)" +
            ")")
    Page<User> findByUsernameStartingWithIgnoreCaseExcludingBlocked(
            @Param("prefix") String prefix,
            @Param("currentUserId") UUID currentUserId,
            Pageable pageable
    );

    void deleteByEmail(String email);
}