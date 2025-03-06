package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.PetDto;
import com.example.registrationmodule.model.dto.UpdatePetDto;
import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.dto.UserProfileDto;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IDtoConversionService;
import com.example.registrationmodule.service.IPetService;
import com.example.registrationmodule.service.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("api")
public class PetController {

    private final IUserService userService;
    private final IPetService petService;
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
        Optional<User> user = userService.getUserById(userId);
        return user.map(userEntity -> {
            UserProfileDto userProfileDto = dtoConversionService.mapToUserProfileDto(userEntity);
            return new ResponseEntity<>(userProfileDto, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping(path = "/user/{id}")
    public ResponseEntity<UserProfileDto> partialUpdateUserProfileById(@PathVariable("id") UUID userId,
            @RequestBody UpdateUserProfileDto updateUserProfileDto) {

        if(!userService.existsById(userId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        User user = dtoConversionService.mapToUser(updateUserProfileDto);
        User updatedUser = userService.partialUpdateUserById(userId, user);
        return new ResponseEntity<>(
                dtoConversionService.mapToUserProfileDto(updatedUser),
                HttpStatus.OK
        );
    }

    @PostMapping(path = "/pet")
    public ResponseEntity<PetDto> createPet(@RequestBody PetDto petDto) {
        UUID userId = petDto.getUserId();

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID must not be null");
        }

        if (!userService.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        // Ensure the client cannot manually set petId
        petDto.setPetId(null);

        Pet pet = dtoConversionService.mapToPet(petDto);
        Pet createdPet = petService.savePet(pet);
        return new ResponseEntity<>(
                dtoConversionService.mapToPetDto(createdPet),
                HttpStatus.CREATED
        );
    }

    @GetMapping(path = "/pet/{id}")
    public ResponseEntity<PetDto> getPetById(@PathVariable(name = "id") UUID petId) {
        Optional<Pet> pet = petService.getPetById(petId);
        return pet.map(petEntity -> {
            PetDto petDto = dtoConversionService.mapToPetDto(petEntity);
            return new ResponseEntity<>(petDto, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping(path = "/user/{id}/pets")
    public List<PetDto> getAllPetsByUserId(@PathVariable(name = "id") UUID userId) {
        if (!userService.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Pet> pets = petService.getPetsByUserId(userId);
        return pets.stream()
                .map(dtoConversionService::mapToPetDto)
                .collect(Collectors.toList());
    }

    @PatchMapping(path = "/pet/{id}")
    public ResponseEntity<PetDto> updatePetById(@PathVariable("id") UUID petId, @RequestBody UpdatePetDto updatePetDto) {

        if (!petService.existsById(petId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Pet updatedPet = petService.partialUpdatePet(petId, updatePetDto);

        return new ResponseEntity<>(
                dtoConversionService.mapToPetDto(updatedPet),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/pet/{id}")
    public ResponseEntity<Void> deletePetById(@PathVariable UUID petId) {
        if (!petService.existsById(petId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        petService.deleteById(petId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}


