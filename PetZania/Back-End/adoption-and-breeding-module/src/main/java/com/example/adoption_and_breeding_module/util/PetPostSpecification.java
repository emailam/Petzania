package com.example.adoption_and_breeding_module.util;

import com.example.adoption_and_breeding_module.model.dto.PetPostFilterDTO;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;

import java.util.Date;
import java.util.List;

public class PetPostSpecification {

    public static Specification<PetPost> withFilters(PetPostFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getPetPostType() != null) {
                predicates.add(cb.equal(root.get("postType"), filter.getPetPostType()));
            }
            if (filter.getSpecies() != null) {
                predicates.add(cb.equal(root.get("pet").get("species"), filter.getSpecies()));
            }
            if (filter.getBreed() != null && !filter.getBreed().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("pet").get("breed")),
                        "%" + filter.getBreed().toLowerCase() + "%"));
            }
            if (filter.getGender() != null) {
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


            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
