package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.dto.PetDto;
import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.dto.UserProfileDto;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IDtoConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
                user.getUserId(),
                user.getUsername(),
                user.getName(),
                user.getBio(),
                user.getProfilePictureURL(),
                user.getPhoneNumber(),
                user.getUserRoles() != null ? user.getUserRoles() : new ArrayList<>(),
                user.getMyPets() != null ?
                        user.getMyPets().stream().map(this::mapToPetDto).collect(Collectors.toList())
                        : new ArrayList<>(),
                user.getFriends() != null ? user.getFriends().size() : 0,
                user.getFollowers() != null ? user.getFollowers().size() : 0,
                user.getFollowing() != null ? user.getFollowing().size() : 0,
                user.getStoreProfileId(),
                user.getVetProfileId()
        );
    }


    @Override
    public User mapToUser(UpdateUserProfileDto updateUserProfileDto) {
        User user = new User();
        user.setName(updateUserProfileDto.getName());
        user.setBio(updateUserProfileDto.getBio());
        user.setProfilePictureURL(updateUserProfileDto.getProfilePictureURL());
        user.setPhoneNumber(updateUserProfileDto.getPhoneNumber());
        return user;
    }


    public PetDto mapToPetDto(Pet pet) {
        if (pet == null) {
            return null;
        }
        return new PetDto(
                pet.getPetId(),
                pet.getName(),
                pet.getDescription(),
                pet.getGender(),
                pet.getAge(),
                pet.getBreed(),
                pet.getSpecies(),
                pet.getMyVaccinesURLs() != null ? pet.getMyVaccinesURLs() : new ArrayList<>(),
                pet.getMyPicturesURLs() != null ? pet.getMyPicturesURLs() : new ArrayList<>()
        );
    }

}
