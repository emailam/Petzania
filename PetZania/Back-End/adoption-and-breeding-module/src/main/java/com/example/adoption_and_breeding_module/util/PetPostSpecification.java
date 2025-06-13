package com.example.adoption_and_breeding_module.util;

import com.example.adoption_and_breeding_module.model.dto.PetPostFilterDTO;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;

import java.util.Date;
import java.util.List;

public class PetPostSpecification {

    public static Specification<PetPost> withFilters(PetPostFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("pet").get("category"), filter.getCategory()));
            }
            if (filter.getBreed() != null && !filter.getBreed().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("pet").get("breed")),
                        "%" + filter.getBreed().toLowerCase() + "%"));
            }
            if (filter.getGender() != null) {
                predicates.add(cb.equal(root.get("pet").get("gender"), filter.getGender()));
            }

            Expression<Integer> ageInMonths = cb.sum(
                    cb.prod(
                            cb.function("date_part", Integer.class, cb.literal("year"),
                                    cb.function("age", Date.class, cb.currentDate(), root.get("pet").get("dateOfBirth"))
                            ), 12
                    ),
                    cb.function("date_part", Integer.class, cb.literal("month"),
                            cb.function("age", Date.class, cb.currentDate(), root.get("pet").get("dateOfBirth"))
                    )
            );

            if (filter.getMinAge() != null) {
                predicates.add(cb.greaterThanOrEqualTo(ageInMonths, filter.getMinAge()));
            }
            if (filter.getMaxAge() != null) {
                predicates.add(cb.lessThanOrEqualTo(ageInMonths, filter.getMaxAge()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
