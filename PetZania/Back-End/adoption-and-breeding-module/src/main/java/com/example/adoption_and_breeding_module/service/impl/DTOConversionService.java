package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.model.dto.PetDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostDTO;
import com.example.adoption_and_breeding_module.model.entity.Pet;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.service.IDTOConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.ArrayList;
import java.time.Period;

@Service
@AllArgsConstructor
@Transactional
public class DTOConversionService implements IDTOConversionService {

    @Override
    public Pet mapToPet(PetDTO petDto) {
        Pet pet = new Pet();

        pet.setName(petDto.getName());
        pet.setDescription(petDto.getDescription());
        pet.setGender(petDto.getGender());
        pet.setDateOfBirth(petDto.getDateOfBirth());
        pet.setBreed(petDto.getBreed());
        pet.setSpecies(petDto.getSpecies());
        pet.setMyVaccinesURLs(petDto.getMyVaccinesURLs());
        pet.setMyPicturesURLs(petDto.getMyPicturesURLs());

        return pet;
    }

    public PetDTO mapToPetDTO(Pet pet) {
        if (pet == null) {
            return null;
        }
        return new PetDTO(
                pet.getPetId(),
                pet.getName(),
                pet.getDescription(),
                pet.getGender(),
                pet.getDateOfBirth(),
                getPetAge(pet.getDateOfBirth()), // Calculate age
                pet.getBreed(),
                pet.getSpecies(),
                pet.getMyVaccinesURLs() != null ? pet.getMyVaccinesURLs() : new ArrayList<>(),
                pet.getMyPicturesURLs() != null ? pet.getMyPicturesURLs() : new ArrayList<>()
        );
    }


    @Override
    public PetPostDTO mapToPetPostDTO(PetPost post) {
        return PetPostDTO.builder()
                .postId(post.getPostId())
                .ownerId(post.getOwner().getUserId())
                .petDTO(mapToPetDTO(post.getPet()))
                .description(post.getDescription())
                .postType(post.getPostType())
                .postStatus(post.getPostStatus())
                .reactions(post.getReactions())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public String getPetAge(LocalDate dateOfBirth) {
        Period period = Period.between(dateOfBirth, LocalDate.now());
        int years = period.getYears();
        int months = period.getMonths();

        if (years > 0) {
            return years + " years" + (months > 0 ? " " + months + " months" : "");
        } else {
            return months + " months";
        }
    }
}
