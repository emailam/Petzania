package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;

import java.time.LocalDate;

public interface IDTOConversionService {

    UserProfileDTO mapToUserProfileDto(User user);

    User mapToUser(UpdateUserProfileDto updateUserProfileDto);
    AdminDTO mapToAdminDTO(Admin admin);

    Admin mapToAdmin(AdminDTO adminDTO);
    PetDTO mapToPetDto(Pet pet);

    Pet mapToPet(PetDTO petDto);

    User mapToUser(RegisterUserDTO registerUserDTO);

    String getPetAge(LocalDate dateOfBirth);
}
