package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PetPostRepository extends JpaRepository<PetPost, UUID>, JpaSpecificationExecutor<PetPost> {
    Page<PetPost> findAllByPostType(PetPostType postType, Pageable pageable);
    long countByReactedUsersUserId(UUID userId);

    long countByReactedUsersUserIdAndPetSpecies(UUID userId, PetSpecies species);

    long countByReactedUsersUserIdAndPostType(UUID userId, PetPostType postType);

}
