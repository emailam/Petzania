package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.*;

import java.util.List;

public interface IDTOConversionService {

    UserProfileDTO mapToUserProfileDto(User user);

    User mapToUser(UpdateUserProfileDto updateUserProfileDto);

    Admin mapToAdmin(UpdateAdminDto updateAdminDto);

    AdminDto mapToAdminDto(Admin admin);


    PetDTO mapToPetDto(Pet pet);

    Pet mapToPet(PetDTO petDto);

    User convertToUser(RegisterUserDTO registerUserDTO);

    PostResponseDTO postToDto(Post post);
    MediaResponseDTO mediaToDto(Media media);
    Media convertToMedia(MediaDTO mediaDTO);
}
