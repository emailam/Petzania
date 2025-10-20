package com.example.adoption_and_breeding_module.util;

import com.example.adoption_and_breeding_module.model.dto.PetPostFilterDTO;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.enumeration.Gender;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PetPostSpecification {

    public static Specification<PetPost> withFilters(PetPostFilterDTO filter, long expirationDays) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getPetPostType() != PetPostType.ALL) {
                predicates.add(cb.equal(root.get("postType"), filter.getPetPostType()));
            }
            if (filter.getPetPostStatus() != PetPostStatus.ALL) {
                predicates.add(cb.equal(root.get("postStatus"), filter.getPetPostStatus()));
            }
            if (filter.getSpecies() != PetSpecies.ALL) {
                predicates.add(cb.equal(root.get("pet").get("species"), filter.getSpecies()));
            }
            if (!Objects.equals(filter.getBreed(), "ALL") && !filter.getBreed().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("pet").get("breed")),
                        "%" + filter.getBreed().toLowerCase() + "%"));
            }
            if (filter.getGender() != Gender.ALL) {
                predicates.add(cb.equal(root.get("pet").get("gender"), filter.getGender()));
            }

            LocalDate today = LocalDate.now();
            if (filter.getMinAge() != null) {
                LocalDate dobBefore = today.minusMonths(filter.getMinAge());
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("pet").get("dateOfBirth"), dobBefore));
            }
            if (filter.getMaxAge() != null) {
                LocalDate dobAfter = today.minusMonths(filter.getMaxAge());
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("pet").get("dateOfBirth"), dobAfter));
            }

            // Expiration filter: only include posts where the most recent of updatedAt or createdAt is within expirationDays
            Instant expirationThreshold = Instant.now().minus(expirationDays, ChronoUnit.DAYS);
            Expression<Instant> updatedAt = root.get("updatedAt");
            Expression<Instant> createdAt = root.get("createdAt");
            // If updatedAt is null, use createdAt; otherwise use updatedAt
            predicates.add(cb.greaterThanOrEqualTo(
                cb.coalesce(updatedAt, createdAt), expirationThreshold
            ));


            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
