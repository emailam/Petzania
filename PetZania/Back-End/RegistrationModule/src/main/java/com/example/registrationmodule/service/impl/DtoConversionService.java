package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.dto.PetDto;
import com.example.registrationmodule.model.dto.UserProfileDto;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IDtoConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DtoConversionService implements IDtoConversionService {

    @Override
    public UserProfileDto mapToUserProfileDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserProfileDto(
                user.getUsername(),
                user.getName(),
                user.getBio(),
                user.getProfilePictureURL(),
                user.getPhoneNumber(),
                user.getUserRoles(),
                user.getMyPets().stream()
                        .map(this::mapToPetDto)
                        .collect(Collectors.toList()),
                user.getFriends().size(),
                user.getFollowers().size(),
                user.getFollowing().size(),
                user.getStoreProfileId(),
                user.getVetProfileId()
        );
    }

    private PetDto mapToPetDto(Pet pet) {
        return new PetDto(
                pet.getPetId(),
                pet.getName(),
                pet.getDescription(),
                pet.getGender(),
                pet.getAge(),
                pet.getBreed(),
                pet.getSpecies(),
                pet.getMyVaccinesURLs(),
                pet.getMyPicturesURLs()
        );
    }

}
