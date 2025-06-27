package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import com.example.adoption_and_breeding_module.repository.PetPostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedScorer {

    @Value("${feed.weights.recency:0.4}")
    private double wRecency;

    @Value("${feed.weights.totalReacts:0.3}")
    private double wTotalReacts;

    @Value("${feed.weights.petCategoryAffinity:0.25}")
    private double wPetCategoryAffinity;

    @Value("${feed.weights.postCategoryAffinity:0.2}")
    private double wPostCategoryAffinity;

    @Value("${feed.freshness-window-hours:96}")
    private long freshnessWindowHours;

    private final Clock clock = Clock.systemUTC();
    private final PetPostRepository petPostRepository;

    public FeedScorer(PetPostRepository petPostRepository) {
        this.petPostRepository = petPostRepository;
    }

    public void scoreAndSort(List<PetPost> posts, UUID userId) {
        Instant now = Instant.now(clock);

        long maxReacts = posts.stream()
                .mapToLong(PetPost::getReacts)
                .max()
                .orElse(1L);

        long userTotalReacts = petPostRepository.countByReactedUsersUserId(userId);

        Map<PetSpecies, Long> reactsBySpecies = Arrays.stream(PetSpecies.values())
                .collect(Collectors.toMap(
                        sp -> sp,
                        sp -> petPostRepository.countByReactedUsersUserIdAndPetSpecies(userId, sp)
                ));
        Map<PetPostType, Long> reactsByPostType = Arrays.stream(PetPostType.values())
                .collect(Collectors.toMap(
                        pt -> pt,
                        pt -> petPostRepository.countByReactedUsersUserIdAndPostType(userId, pt)
                ));

        posts.forEach(p -> {
            double recency = recencyScore(p.getCreatedAt(), now);
            double totalReacts = totalReactsScore(p.getReacts(), maxReacts);
            double petAffinity = affinityScore(
                    reactsBySpecies.getOrDefault(p.getPet().getSpecies(), 0L),
                    userTotalReacts
            );
            double postAffinity = affinityScore(
                    reactsByPostType.getOrDefault(p.getPostType(), 0L),
                    userTotalReacts
            );

            double score = wRecency * recency
                    + wTotalReacts * totalReacts
                    + wPetCategoryAffinity * petAffinity
                    + wPostCategoryAffinity * postAffinity;

            p.setScore(score);
        });

        posts.sort(Comparator.comparingDouble(PetPost::getScore).reversed());
    }

    private double recencyScore(Instant createdAt, Instant now) {
        Duration age = Duration.between(createdAt, now);
        double frac = age.toHours() / (double) freshnessWindowHours;
        return Math.max(0.0, 1.0 - frac);
    }

    private double totalReactsScore(long reacts, long maxReacts) {
        return reacts / (double) (1 + maxReacts);
    }

    private double affinityScore(long categoryReacts, long totalReacts) {
        return totalReacts > 0 ? categoryReacts / (double) totalReacts : 0.0;
    }
}
