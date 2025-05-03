package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.*;

import java.util.List;

import java.time.LocalDate;

public interface IDTOConversionService {

    UserProfileDTO mapToUserProfileDto(User user);

    User mapToUser(UpdateUserProfileDto updateUserProfileDto);
    AdminDTO mapToAdminDTO(Admin admin);

    Admin mapToAdmin(AdminDTO adminDTO);
    PetDTO mapToPetDto(Pet pet);

    Pet mapToPet(PetDTO petDto);
    MediaResponseDTO mediaToDto(Media media);
    Media convertToMedia(MediaDTO mediaDTO);
    User mapToUser(RegisterUserDTO registerUserDTO);

    String getPetAge(LocalDate dateOfBirth);
}
