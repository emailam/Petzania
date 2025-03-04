package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.UserProfileDto;
import com.example.registrationmodule.model.entity.User;

public interface IDtoConversionService {

    public UserProfileDto mapToUserProfileDto(User user);
}
