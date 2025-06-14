package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.*;
import com.example.registrationmodule.model.dto.PetDTO;
import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.dto.UserProfileDTO;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;

import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.service.IDTOConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
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
                user.getEmail(),
                user.getName(),
                user.getLoginTimes(),
                user.getBio(),
                user.getProfilePictureURL(),
                user.getPhoneNumber(),
                user.getMyPets() != null ?
                        user.getMyPets().stream().map(this::mapToPetDto).collect(Collectors.toList())
                        : new ArrayList<>(),
                user.isBlocked(),
                user.isOnline()
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
    public Admin mapToAdmin(AdminDTO adminDTO) {
        Admin admin = new Admin();
        admin.setUsername(adminDTO.getUsername());
        admin.setPassword(adminDTO.getPassword());
        admin.setRole(adminDTO.getAdminRole());
        return admin;
    }

    @Override
    public AdminDTO mapToAdminDTO(Admin admin) {
        if (admin == null) {
            return null;
        }

        return new AdminDTO(
                admin.getAdminId(),
                admin.getUsername(),
                admin.getPassword(),
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
                pet.getDateOfBirth(),
                getPetAge(pet.getDateOfBirth()), // Calculate age
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
        pet.setDateOfBirth(petDto.getDateOfBirth());
        pet.setBreed(petDto.getBreed());
        pet.setSpecies(petDto.getSpecies());
        pet.setMyVaccinesURLs(petDto.getMyVaccinesURLs());
        pet.setMyPicturesURLs(petDto.getMyPicturesURLs());

        User user = userRepository.findById(petDto.getUserId())
                .orElseThrow(() -> new UserNotFound("User not found"));
        pet.setUser(user);

        return pet;
    }

    @Override
    public User mapToUser(RegisterUserDTO registerUserDTO) {
        User user = new User();
        user.setUsername(registerUserDTO.getUsername());
        user.setEmail(registerUserDTO.getEmail());
        user.setPassword(registerUserDTO.getPassword());
        return user;
    }

    @Override
    public Media mapToMedia(MediaDTO mediaDTO) {
        Media media = new Media();
        media.setKey(mediaDTO.getKey());
        media.setFormat(mediaDTO.getFormat());
        media.setType(mediaDTO.getType());
        media.setUploadedAt(mediaDTO.getUploadedAt());
        return media;
    }

    @Override
    public MediaResponseDTO mediaToDto(Media media) {
        return MediaResponseDTO.builder()
                .mediaId(media.getMediaId())
                .key(media.getKey())
                .type(media.getType())
                .format(media.getFormat())
                .uploadedAt(media.getUploadedAt())
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
