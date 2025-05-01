package com.example.registrationmodule.controller;

import com.example.registrationmodule.exception.pet.PetNotFound;
import com.example.registrationmodule.exception.user.UserIdNull;
import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.dto.PetDTO;
import com.example.registrationmodule.model.dto.UpdatePetDTO;
import com.example.registrationmodule.model.entity.Media;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.service.ICloudService;
import com.example.registrationmodule.service.IPetService;
import com.example.registrationmodule.service.IUserService;
import com.example.registrationmodule.service.IDTOConversionService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("api")
public class PetController {

    private final IUserService userService;
    private final ICloudService cloudService;
    private final IPetService petService;
    private final IDTOConversionService dtoConversionService;

    @PostMapping(path = "/pet")
    public ResponseEntity<PetDTO> createPet(@RequestBody PetDTO petDto) {
        UUID userId = petDto.getUserId();

        if (userId == null) {
            throw new UserIdNull("User ID must not be null");
        }

        if (!userService.userExistsById(userId)) {
            throw new UserNotFound("User not found with ID: " + userId);
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
        Pet pet = petService.getPetById(petId)
                .orElseThrow(() -> new PetNotFound("Pet not found with ID: " + petId));
        return new ResponseEntity<>(dtoConversionService.mapToPetDto(pet), HttpStatus.OK);
    }

    @GetMapping(path = "/user/{id}/pets")
    public ResponseEntity<List<PetDTO>> getAllPetsByUserId(@PathVariable(name = "id") UUID userId) {
        if (!userService.userExistsById(userId)) {
            throw new UserNotFound("User not found with ID: " + userId);
        }

        List<Pet> pets = petService.getPetsByUserId(userId);
        return new ResponseEntity<>(pets.stream()
                .map(dtoConversionService::mapToPetDto)
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @PatchMapping(path = "/pet/{id}")
    public ResponseEntity<PetDTO> updatePetById(@PathVariable("id") UUID petId, @RequestBody UpdatePetDTO updatePetDto) {

        if (!petService.existsById(petId)) {
            throw new PetNotFound("Pet not found with ID: " + petId);
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
            throw new PetNotFound("Pet not found with ID: " + petId);
        }

        petService.deleteById(petId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{id}/add-pet-picture")
    public ResponseEntity<PetDTO> addPetPicture(@PathVariable UUID petId,
                                                               @RequestParam("file") MultipartFile file) throws IOException {
        if (!petService.existsById(petId)) {
            throw new PetNotFound("Pet not found with ID: " + petId);
        }
        Media media = cloudService.uploadAndSaveMedia(file, true);
        String cdnUrl = cloudService.getMediaUrl(media.getMediaId());

        Pet updatedPet = petService.addPetPicture(petId,cdnUrl);

        return new ResponseEntity<>(
                dtoConversionService.mapToPetDto(updatedPet),
                HttpStatus.OK
        );
    }
}


