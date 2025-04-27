package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.*;

import java.util.List;

import java.time.LocalDate;

public interface IDTOConversionService {
    UserProfileDTO mapToUserProfileDto(User user);
    User mapToUser(UpdateUserProfileDto updateUserProfileDto);
    Admin mapToAdmin(UpdateAdminDto updateAdminDto);
    AdminDto mapToAdminDto(Admin admin);
    PetDTO mapToPetDto(Pet pet);
    Pet mapToPet(PetDTO petDto);
    PostResponseDTO postToDto(Post post);
    MediaResponseDTO mediaToDto(Media media);
    Media convertToMedia(MediaDTO mediaDTO);
    User mapToUser(RegisterUserDTO registerUserDTO);
    String getPetAge(LocalDate dateOfBirth);
}
