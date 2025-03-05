package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.UserProfileDTO;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.IProfileService;
import com.example.registrationmodule.service.IUserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

//@AllArgsConstructor
@RestController
@RequestMapping("api/user/profile")
public class PetController {

    private final IUserService userService;
    private final IProfileService profileService;
    private final IDTOConversionService dtoConversionService;

    public PetController(IUserService userService,
                         IProfileService profileService,
                         IDTOConversionService dtoConversionService) {
        this.userService = userService;
        this.profileService = profileService;
        this.dtoConversionService = dtoConversionService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<UserProfileDTO> getUserProfileById(@PathVariable("id") UUID userId) {
        Optional<User> user = userService.getUserById(userId);
        return user.map(userEntity -> {
            UserProfileDTO userProfileDto = dtoConversionService.mapToUserProfileDto(userEntity);
            return new ResponseEntity<>(userProfileDto, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

//    public ResponseEntity<UserProfileDto> updateUserProfileById(UUID userId,
//                                         UpdateUserProfileDto updateUserProfileDto);
//
//    public PetDto createPet(PetDto pet);
//
//    public ResponseEntity<PetDto> getPetById(UUID petId);
//
//    public List<PetDto> getAllPetsByUserId(UUID userId);
//
//    public ResponseEntity<PetDto> updatePetById(UUID petId, PetDto petDto);
//
//    public ResponseEntity deletePetById(UUID petId);
}


