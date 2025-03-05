package com.example.registrationmodule.service;

import com.example.registrationmodule.model.dto.UpdatePetDto;
import com.example.registrationmodule.model.entity.Pet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPetService {

    Pet savePet(Pet pet);

    Optional<Pet> getPetById(UUID petId);

    List<Pet> getPetsByUserId(UUID userId);

    boolean existsById(UUID petId);

    Pet partialUpdatePet(UUID petId, UpdatePetDto petDto);

    void deleteById(UUID petId);
}
