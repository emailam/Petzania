package com.example.registrationmodule.controller;

import com.example.registrationmodule.exception.pet.PetNotFound;
import com.example.registrationmodule.exception.user.UserAccessDenied;
import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.dto.PetDTO;
import com.example.registrationmodule.model.dto.UpdatePetDTO;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.UserPrincipal;
import com.example.registrationmodule.service.IPetService;
import com.example.registrationmodule.service.IUserService;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.util.SecurityUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("api")
public class PetController {

    private final IUserService userService;
    private final IPetService petService;
    private final IDTOConversionService dtoConversionService;

    @PostMapping(path = "/pet")
    public ResponseEntity<PetDTO> createPet(@RequestBody PetDTO petDto){

        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

//        if (userId == null) {
//            throw new UserIdNull("User ID must not be null");
//        }
//
//        if (!userService.userExistsById(userId)) {
//            throw new UserNotFound("User not found with ID: " + userId);
//        }

        // Ensure the client cannot manually set petId
        petDto.setPetId(null);

        petDto.setUserId(userId);

        Pet pet = dtoConversionService.mapToPet(petDto);
        Pet createdPet = petService.savePet(pet);
        return new ResponseEntity<>(
                dtoConversionService.mapToPetDto(createdPet),
                HttpStatus.CREATED
        );
    }

    @GetMapping(path = "/pet/{petId}")
    public ResponseEntity<PetDTO> getPetById(@PathVariable(name = "petId") UUID petId) {

        Pet pet = petService.getPetById(petId)
                .orElseThrow(() -> new PetNotFound("Pet not found with ID: " + petId));
        return new ResponseEntity<>(dtoConversionService.mapToPetDto(pet), HttpStatus.OK);
    }

    @GetMapping(path = "/user/{userId}/pets")
    public ResponseEntity<List<PetDTO>> getAllPetsByUserId(@PathVariable(name = "userId") UUID userId) {
        if (!userService.userExistsById(userId)) {
            throw new UserNotFound("User not found with ID: " + userId);
        }

        List<Pet> pets = petService.getPetsByUserId(userId);
        return new ResponseEntity<>(
                pets.stream()
                .map(dtoConversionService::mapToPetDto)
                .collect(Collectors.toList()),
                HttpStatus.OK);
    }

    @PatchMapping(path = "/pet/{petId}")
    public ResponseEntity<PetDTO> updatePetById(@PathVariable("petId") UUID petId,
                                                @RequestBody UpdatePetDTO updatePetDto) {

        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        if (!petService.getPetById(petId)
                .orElseThrow(() -> new PetNotFound("Pet not found with ID: " + petId))
                .getUser()
                .getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only update your pets");
        }

        Pet updatedPet = petService.partialUpdatePet(petId, updatePetDto);

        return new ResponseEntity<>(
                dtoConversionService.mapToPetDto(updatedPet),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/pet/{id}")
    public ResponseEntity<Void> deletePetById(@PathVariable(name = "id") UUID petId) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser();
        UUID userId = userPrincipal.getUserId();

        if (!petService.getPetById(petId)
                .orElseThrow(() -> new PetNotFound("Pet not found with ID: " + petId))
                .getUser()
                .getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only delete your pets");
        }

        petService.deleteById(petId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}


