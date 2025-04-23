package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.pet.PetNotFound;
import com.example.registrationmodule.exception.rateLimiting.TooManyPetRequests;
import com.example.registrationmodule.model.dto.UpdatePetDTO;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.repository.PetRepository;
import com.example.registrationmodule.service.IPetService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class PetService implements IPetService {

    private final PetRepository petRepository;

    @Override
    @RateLimiter(name = "savePetRateLimiter", fallbackMethod = "savePetFallback")
    public Pet savePet(Pet pet) {
        return petRepository.save(pet);
    }

    @Override
    public Optional<Pet> getPetById(UUID petId) {
        return petRepository.findById(petId);
    }

    @Override
    public List<Pet> getPetsByUserId(UUID userId) {
        return petRepository.findByUser_UserId(userId);
    }

    @Override
    public boolean existsById(UUID petId) {
        return petRepository.existsById(petId);
    }

    @Override
    @RateLimiter(name = "updatePetRateLimiter", fallbackMethod = "updatePetFallback")
    public Pet partialUpdatePet(UUID petId, UpdatePetDTO petDto) {
        return petRepository.findById(petId).map(existingPet -> {
            Optional.ofNullable(petDto.getName()).ifPresent(existingPet::setName);
            Optional.ofNullable(petDto.getDescription()).ifPresent(existingPet::setDescription);
            Optional.ofNullable(petDto.getGender()).ifPresent(existingPet::setGender);
            Optional.ofNullable(petDto.getDateOfBirth()).ifPresent(existingPet::setDateOfBirth);
            Optional.ofNullable(petDto.getBreed()).ifPresent(existingPet::setBreed);
            Optional.ofNullable(petDto.getSpecies()).ifPresent(existingPet::setSpecies);
            Optional.ofNullable(petDto.getMyVaccinesURLs()).ifPresent(existingPet::setMyVaccinesURLs);
            Optional.ofNullable(petDto.getMyPicturesURLs()).ifPresent(existingPet::setMyPicturesURLs);

            return petRepository.save(existingPet);
        }).orElseThrow(() -> new PetNotFound("Pet does not exist"));
    }

    @Override
    @RateLimiter(name = "deletePetRateLimiter", fallbackMethod = "deletePetFallback")
    public void deleteById(UUID petId) {
        petRepository.deleteById(petId);
    }

    // Fallback methods
    public Pet savePetFallback(Pet pet, RequestNotPermitted t) {
        throw new TooManyPetRequests("Rate limit exceeded for saving pets. Please try again later.");
    }

    public Pet updatePetFallback(UUID petId, UpdatePetDTO petDto, RequestNotPermitted t) {
        throw new TooManyPetRequests("Rate limit exceeded for updating pets. Please try again later.");
    }

    public void deletePetFallback(UUID petId, RequestNotPermitted t) {
        throw new TooManyPetRequests("Rate limit exceeded for deleting pets. Please try again later.");
    }
}