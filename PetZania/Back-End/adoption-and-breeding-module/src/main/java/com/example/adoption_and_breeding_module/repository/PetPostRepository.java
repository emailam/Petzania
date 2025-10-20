package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PetPostRepository extends JpaRepository<PetPost, UUID>, JpaSpecificationExecutor<PetPost> {
    Page<PetPost> findAllByPostType(PetPostType postType, Pageable pageable);
    long countByReactedUsersUserId(UUID userId);

    interface SpeciesCount {
        PetSpecies getSpecies();
        long      getCnt();
    }

    interface TypeCount {
        PetPostType getPostType();
        long        getCnt();
    }

    /**
     * For a given userId, count how many posts they've reacted to, grouped by species.
     */
    @Query("""
      SELECT p.pet.species   AS species,
             COUNT(r)        AS cnt
        FROM PetPost p
        JOIN p.reactedUsers r
       WHERE r.userId       = :userId
       GROUP BY p.pet.species
    """)
    List<SpeciesCount> countReactsBySpecies(@Param("userId") UUID userId);

    /**
     * For a given userId, count how many posts they've reacted to, grouped by postType.
     */
    @Query("""
      SELECT p.postType      AS postType,
             COUNT(r)        AS cnt
        FROM PetPost p
        JOIN p.reactedUsers r
       WHERE r.userId       = :userId
       GROUP BY p.postType
    """)
    List<TypeCount> countReactsByPostType(@Param("userId") UUID userId);
}
