package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PetPostRepository extends JpaRepository<PetPost, UUID> {
    Page<PetPost> findAllByPostType(PetPostType postType, Pageable pageable);
}
