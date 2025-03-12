package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;

public interface IDTOConversionService {

    UserProfileDTO mapToUserProfileDto(User user);

    User mapToUser(UpdateUserProfileDto updateUserProfileDto);

    Admin mapToAdmin(UpdateAdminDto updateAdminDto);

    AdminDto mapToAdminDto(Admin admin);


    PetDTO mapToPetDto(Pet pet);

    Pet mapToPet(PetDTO petDto);

    User mapToUser(RegisterUserDTO registerUserDTO);
}
