package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.PetDto;
import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.dto.UserProfileDto;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;

public interface IDtoConversionService {

    UserProfileDto mapToUserProfileDto(User user);

    User mapToUser(UpdateUserProfileDto updateUserProfileDto);

    PetDto mapToPetDto(Pet pet);

    Pet mapToPet(PetDto petDto);
}
