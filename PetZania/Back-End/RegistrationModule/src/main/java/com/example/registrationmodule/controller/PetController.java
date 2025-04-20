package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.PetDTO;
import com.example.registrationmodule.model.dto.UpdatePetDTO;
import com.example.registrationmodule.model.dto.UpdateUserProfileDto;
import com.example.registrationmodule.model.dto.UserProfileDTO;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.service.IPetService;
import com.example.registrationmodule.service.IUserService;
import com.example.registrationmodule.service.IDTOConversionService;
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
    private final IDTOConversionService dtoConversionService;

    @PostMapping(path = "/pet")
    public ResponseEntity<PetDTO> createPet(@RequestBody PetDTO petDto) {
        UUID userId = petDto.getUserId();

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID must not be null");
        }

        if (!userService.userExistsById(userId)) {
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
    public ResponseEntity<PetDTO> getPetById(@PathVariable(name = "id") UUID petId) {
        Optional<Pet> pet = petService.getPetById(petId);
        return pet.map(petEntity -> {
            PetDTO petDto = dtoConversionService.mapToPetDto(petEntity);
            return new ResponseEntity<>(petDto, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping(path = "/user/{id}/pets")
    public List<PetDTO> getAllPetsByUserId(@PathVariable(name = "id") UUID userId) {
        if (!userService.userExistsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Pet> pets = petService.getPetsByUserId(userId);
        return pets.stream()
                .map(dtoConversionService::mapToPetDto)
                .collect(Collectors.toList());
    }

    @PatchMapping(path = "/pet/{id}")
    public ResponseEntity<PetDTO> updatePetById(@PathVariable("id") UUID petId, @RequestBody UpdatePetDTO updatePetDto) {

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
    public ResponseEntity<Void> deletePetById(@PathVariable(name = "id") UUID petId) {
        if (!petService.existsById(petId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        petService.deleteById(petId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}


