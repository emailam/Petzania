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

    @Value("${feed.weights.recency:400}")
    private long wRecency;

    @Value("${feed.weights.totalReacts:300}")
    private long wTotalReacts;

    @Value("${feed.weights.petCategoryAffinity:250}")
    private long wPetCategoryAffinity;

    @Value("${feed.weights.postCategoryAffinity:200}")
    private long wPostCategoryAffinity;

    @Value("${feed.weights.friendBoost:5000}")
    private long wFriendBoost;

    @Value("${feed.weights.followeeBoost:3000}")
    private long wFolloweeBoost;

    @Value("${feed.weights.distance:400}")
    private long wDistance;

    @Value("${feed.weights.speciesAffinity:250}")
    private long wSpeciesAffinity;

    @Value("${feed.weights.breedAffinity:150}")
    private long wBreedAffinity;

    @Value("${feed.weights.postTypeAffinity:200}")
    private long wPostTypeAffinity;

    @Value("${feed.weights.authorAffinity:100}")
    private long wAuthorAffinity;

    @Value("${feed.freshness-window-hours:96}")
    private long freshnessWindowHours;

    private static final long SIGNAL_SCALE = 1_000L;

    private final Clock clock = Clock.systemUTC();
    private final PetPostRepository petPostRepository;

    public FeedScorer(PetPostRepository petPostRepository) {
        this.petPostRepository = petPostRepository;
    }

    public void scoreAndSort(
            List<PetPost> posts,
            double userLat, double userLng,
            long userTotalReacts,
            Map<PetSpecies, Long> reactsBySpecies,
            Map<PetPostType, Long> reactsByPostType,
            List<UUID> friendIds,
            List<UUID> followeeIds,
            Map<PetSpecies, Long> interestSpecies,
            Map<String, Long> interestBreed,
            Map<PetPostType, Long> interestPostType,
            Map<UUID, Long> interestOwner
    ) {
        Instant now = Instant.now(clock);
        long maxReacts = posts.stream()
                .mapToLong(PetPost::getReacts)
                .max().orElse(1L);

        // find the maximum *absolute* interest score in each category
        long maxSpeciesScore = interestSpecies.values().stream()
                .map(Math::abs)
                .max(Long::compare)
                .orElse(1L);
        long maxBreedScore = interestBreed.values().stream()
                .map(Math::abs)
                .max(Long::compare)
                .orElse(1L);
        long maxPostTypeScore = interestPostType.values().stream()
                .map(Math::abs)
                .max(Long::compare)
                .orElse(1L);
        long maxOwnerScore = interestOwner.values().stream()
                .map(Math::abs)
                .max(Long::compare)
                .orElse(1L);


        for (PetPost p : posts) {
            long recSc = recencyScoreLong(p.getCreatedAt(), now);
            long reactSc = totalReactsScoreLong(p.getReacts(), maxReacts);

            // … existing affinity from reacts …
            long petAff = affinityScoreLong(
                    reactsBySpecies.getOrDefault(p.getPet().getSpecies(), 0L),
                    userTotalReacts
            );
            long typeAff = affinityScoreLong(
                    reactsByPostType.getOrDefault(p.getPostType(), 0L),
                    userTotalReacts
            );

            // —— signed interest scores ——
            long speciesScore = interestSpecies.getOrDefault(p.getPet().getSpecies(), 0L);
            long breedScore = interestBreed.getOrDefault(p.getPet().getBreed(), 0L);
            long postTypeScore = interestPostType.getOrDefault(p.getPostType(), 0L);
            long ownerScore = interestOwner.getOrDefault(p.getOwner().getUserId(), 0L);

            // map into [–SIGNAL_SCALE .. +SIGNAL_SCALE]
            long speciesBoost = affinityScoreLong(speciesScore, maxSpeciesScore);
            long breedBoost = affinityScoreLong(breedScore, maxBreedScore);
            long postTypeBoost = affinityScoreLong(postTypeScore, maxPostTypeScore);
            long ownerBoost = affinityScoreLong(ownerScore, maxOwnerScore);


            long socialBoost = (friendIds.contains(p.getOwner().getUserId())
                    ? wFriendBoost : 0)
                    + (followeeIds.contains(p.getOwner().getUserId())
                    ? wFolloweeBoost : 0);

            long distSc = distanceScoreLong(
                    userLat, userLng,
                    p.getLatitude(), p.getLongitude()
            );

            long score =
                    wRecency * recSc
                    + wTotalReacts * reactSc
                    + wPetCategoryAffinity * petAff
                    + wPostCategoryAffinity * typeAff
                    + wSpeciesAffinity * speciesBoost
                    + wBreedAffinity * breedBoost
                    + wPostTypeAffinity * postTypeBoost
                    + wAuthorAffinity * ownerBoost
                    + socialBoost
                    + wDistance * distSc;

            p.setScore(score);
        }

        posts.sort(Comparator.comparingLong(PetPost::getScore).reversed());
    }

    private long recencyScoreLong(Instant createdAt, Instant now) {
        long ageH = Duration.between(createdAt, now).toHours();
        long remH = Math.max(freshnessWindowHours - ageH, 0L);
        return (remH * SIGNAL_SCALE) / freshnessWindowHours;
    }

    private long totalReactsScoreLong(long reacts, long maxReacts) {
        return (reacts * SIGNAL_SCALE) / Math.max(maxReacts, 1L);
    }

    private long affinityScoreLong(long userReactsInCat, long userTotalReacts) {
        return userTotalReacts == 0
                ? 0L
                : (userReactsInCat * SIGNAL_SCALE) / userTotalReacts;
    }

    private long distanceScoreLong(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        double dKm = haversine(lat1, lon1, lat2, lon2);
        double raw = (1.0 / (1.0 + dKm)) * SIGNAL_SCALE;
        return (long) raw;
    }

    private double haversine(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        final double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
