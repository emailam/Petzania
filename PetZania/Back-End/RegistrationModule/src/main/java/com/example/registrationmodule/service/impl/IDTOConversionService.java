package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.dto.UserProfileDto;
import com.example.registrationmodule.model.entity.User;

public interface IDTOConversionService {

    public UserProfileDto mapToUserProfileDto(User user);
}
