package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.entity.PetPostInterest;
import com.example.adoption_and_breeding_module.model.entity.PetPostInterestId;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.repository.projection.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetPostInterestRepository extends JpaRepository<PetPostInterest, PetPostInterestId> {

    @Query("""
      SELECT i.post.pet.species AS species,
             SUM(
               CASE
                 WHEN i.interestType = com.example.adoption_and_breeding_module.model.enumeration.InterestType.INTERESTED
                   THEN 1
                 WHEN i.interestType = com.example.adoption_and_breeding_module.model.enumeration.InterestType.NOT_INTERESTED
                   THEN -1
                 ELSE 0
               END
             ) AS score
        FROM PetPostInterest i
       WHERE i.user.userId = :uid
       GROUP BY i.post.pet.species
      """)
    List<SpeciesScore> scoreBySpecies(@Param("uid") UUID userId);

    @Query("""
      SELECT i.post.pet.breed AS breed,
             SUM(
               CASE
                 WHEN i.interestType = com.example.adoption_and_breeding_module.model.enumeration.InterestType.INTERESTED
                   THEN 1
                 WHEN i.interestType = com.example.adoption_and_breeding_module.model.enumeration.InterestType.NOT_INTERESTED
                   THEN -1
                 ELSE 0
               END
             ) AS score
        FROM PetPostInterest i
       WHERE i.user.userId = :uid
       GROUP BY i.post.pet.breed
      """)
    List<BreedScore> scoreByBreed(@Param("uid") UUID userId);

    @Query("""
      SELECT i.post.postType AS postType,
             SUM(
               CASE
                 WHEN i.interestType = com.example.adoption_and_breeding_module.model.enumeration.InterestType.INTERESTED
                   THEN 1
                 WHEN i.interestType = com.example.adoption_and_breeding_module.model.enumeration.InterestType.NOT_INTERESTED
                   THEN -1
                 ELSE 0
               END
             ) AS score
        FROM PetPostInterest i
       WHERE i.user.userId = :uid
       GROUP BY i.post.postType
      """)
    List<PostTypeScore> scoreByPostType(@Param("uid") UUID userId);

    @Query("""
      SELECT i.post.owner.userId AS ownerId,
             SUM(
               CASE
                 WHEN i.interestType = com.example.adoption_and_breeding_module.model.enumeration.InterestType.INTERESTED
                   THEN 1
                 WHEN i.interestType = com.example.adoption_and_breeding_module.model.enumeration.InterestType.NOT_INTERESTED
                   THEN -1
                 ELSE 0
               END
             ) AS score
        FROM PetPostInterest i
       WHERE i.user.userId = :uid
       GROUP BY i.post.owner.userId
      """)
    List<OwnerScore> scoreByOwner(@Param("uid") UUID userId);

    Optional<PetPostInterest> findById(PetPostInterestId id);
}

