package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.dto.UserProfileDto;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IDtoConversionService;
import com.example.registrationmodule.service.IPetService;
import com.example.registrationmodule.service.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("api/pet")
public class PetController {

    private final IUserService userService;
    private final IPetService profileService;
    private final IDtoConversionService dtoConversionService;

//    public UserProfileController(IUserService userService, IProfileService profileService, IDtoConversionService dtoConversionService) {
//        this.userService = userService;
//        this.profileService = profileService;
//        this.dtoConversionService = dtoConversionService;
//    }

    @PostMapping(path = "/user")
    public ResponseEntity<UserProfileDto> createUserProfileById(@RequestBody UpdateUserProfileDto userProfileDto) {
        User user = dtoConversionService.mapToUser(userProfileDto);
        User createdUser = userService.saveUser(user);
        return new ResponseEntity<>(
                dtoConversionService.mapToUserProfileDto(createdUser),
                HttpStatus.CREATED
        );
    }

    @GetMapping(path = "/user/{id}")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable("id") UUID userId) {
        Optional<User> user = userService.getUser(userId);
        return user.map(userEntity -> {
            UserProfileDto userProfileDto = dtoConversionService.mapToUserProfileDto(userEntity);
            return new ResponseEntity<>(userProfileDto, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(path = "/user/{id}")
    public ResponseEntity<UserProfileDto> updateUserProfileById(@PathVariable("id") UUID userId,
            @RequestBody UpdateUserProfileDto updateUserProfileDto) {

        if(!userService.exists(userId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        User user = dtoConversionService.mapToUser(updateUserProfileDto);
        User updatedUser = userService.updateUser(userId, user);
        return new ResponseEntity<>(
                dtoConversionService.mapToUserProfileDto(updatedUser),
                HttpStatus.OK
        );
    }



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


