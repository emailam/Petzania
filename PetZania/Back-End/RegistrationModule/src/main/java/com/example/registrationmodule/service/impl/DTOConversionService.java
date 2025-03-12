package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.dto.PetDTO;
import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.dto.UserProfileDTO;import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;

import com.example.registrationmodule.repo.UserRepository;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.impl.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DTOConversionService implements IDTOConversionService {

    private final UserRepository userRepository;

    @Override
    public UserProfileDTO mapToUserProfileDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserProfileDTO(
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

    @Override
    public Admin mapToAdmin(UpdateAdminDto updateAdminDto){
        Admin admin = new Admin();
        admin.setUsername(updateAdminDto.getUsername());
        //admin.setRole(updateAdminDto.getAdminRoles());
        return admin;
    }

    @Override
    public AdminDto mapToAdminDto(Admin admin){
        if (admin == null) {
            return null;
        }

        return new AdminDto(
                admin.getAdminId(),
                admin.getUsername(),
                admin.getRole()
        );
    }

    @Override
    public PetDTO mapToPetDto(Pet pet) {
    if (pet == null) {
                return null;
            }
        return new PetDTO(
                pet.getPetId(),
                pet.getName(),
                pet.getDescription(),
                pet.getGender(),
                pet.getAge(),
                pet.getBreed(),
                pet.getSpecies(),
                pet.getMyVaccinesURLs() != null ? pet.getMyVaccinesURLs() : new ArrayList<>(),
                pet.getMyPicturesURLs() != null ? pet.getMyPicturesURLs() : new ArrayList<>(),
                pet.getUser().getUserId()
        );
    }

    @Override
    public Pet mapToPet(PetDTO petDto) {
        Pet pet = new Pet();

        pet.setName(petDto.getName());
        pet.setDescription(petDto.getDescription());
        pet.setGender(petDto.getGender());
        pet.setAge(petDto.getAge());
        pet.setBreed(petDto.getBreed());
        pet.setSpecies(petDto.getSpecies());
        pet.setMyVaccinesURLs(petDto.getMyVaccinesURLs());
        pet.setMyPicturesURLs(petDto.getMyPicturesURLs());

        User user = userRepository.findById(petDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        pet.setUser(user);

        return pet;
    }

    @Override
    public User convertToUser(RegisterUserDTO registerUserDTO) {
        User user = new User();
        user.setUsername(registerUserDTO.getUsername());
        user.setEmail(registerUserDTO.getEmail());
        user.setPassword(registerUserDTO.getPassword());
        return user;
    }

}
