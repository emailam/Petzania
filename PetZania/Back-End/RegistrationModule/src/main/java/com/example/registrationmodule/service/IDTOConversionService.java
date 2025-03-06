package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.PetDTO;
import com.example.registrationmodule.model.dto.RegisterUserDTO;
import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.dto.UserProfileDTO;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;

public interface IDTOConversionService {

    UserProfileDTO mapToUserProfileDto(User user);

    User mapToUser(UpdateUserProfileDto updateUserProfileDto);

    PetDTO mapToPetDto(Pet pet);

    Pet mapToPet(PetDTO petDto);

    User convertToUser(RegisterUserDTO registerUserDTO);
}
