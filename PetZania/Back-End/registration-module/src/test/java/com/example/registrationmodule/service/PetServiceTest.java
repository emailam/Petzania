package com.example.registrationmodule.service;

import com.example.registrationmodule.exception.pet.PetNotFound;
import com.example.registrationmodule.exception.rateLimiting.TooManyPetRequests;
import com.example.registrationmodule.model.dto.UpdatePetDTO;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;
import com.example.registrationmodule.repository.PetRepository;
import com.example.registrationmodule.service.impl.PetService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PetServiceTest {
    @Mock private PetRepository petRepository;
    @InjectMocks private PetService petService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void savePet_success() {
        Pet pet = buildPet();
        when(petRepository.save(pet)).thenReturn(pet);
        assertEquals(pet, petService.savePet(pet));
    }

    @Test
    void savePetFallback_throws() {
        assertThrows(TooManyPetRequests.class, () -> petService.savePetFallback(buildPet(), mock(RequestNotPermitted.class)));
    }

    @Test
    void getPetById_found() {
        Pet pet = buildPet();
        UUID id = pet.getPetId();
        when(petRepository.findById(id)).thenReturn(Optional.of(pet));
        assertEquals(Optional.of(pet), petService.getPetById(id));
    }

    @Test
    void getPetById_notFound() {
        UUID id = UUID.randomUUID();
        when(petRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), petService.getPetById(id));
    }

    @Test
    void getPetsByUserId_returnsList() {
        UUID userId = UUID.randomUUID();
        List<Pet> pets = List.of(buildPet());
        when(petRepository.findByUser_UserId(userId)).thenReturn(pets);
        assertEquals(pets, petService.getPetsByUserId(userId));
    }

    @Test
    void existsById_true() {
        UUID id = UUID.randomUUID();
        when(petRepository.existsById(id)).thenReturn(true);
        assertTrue(petService.existsById(id));
    }

    @Test
    void existsById_false() {
        UUID id = UUID.randomUUID();
        when(petRepository.existsById(id)).thenReturn(false);
        assertFalse(petService.existsById(id));
    }

    @Test
    void partialUpdatePet_success() {
        Pet pet = buildPet();
        UUID id = pet.getPetId();
        UpdatePetDTO dto = new UpdatePetDTO();
        dto.setName("NewName");
        when(petRepository.findById(id)).thenReturn(Optional.of(pet));
        when(petRepository.save(any())).thenReturn(pet);
        Pet updated = petService.partialUpdatePet(id, dto);
        assertEquals("NewName", updated.getName());
    }

    @Test
    void partialUpdatePet_notFound_throws() {
        UUID id = UUID.randomUUID();
        UpdatePetDTO dto = new UpdatePetDTO();
        when(petRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(PetNotFound.class, () -> petService.partialUpdatePet(id, dto));
    }

    @Test
    void updatePetFallback_throws() {
        assertThrows(TooManyPetRequests.class, () -> petService.updatePetFallback(UUID.randomUUID(), new UpdatePetDTO(), mock(RequestNotPermitted.class)));
    }

    @Test
    void deleteById_success() {
        UUID id = UUID.randomUUID();
        doNothing().when(petRepository).deleteById(id);
        assertDoesNotThrow(() -> petService.deleteById(id));
    }

    @Test
    void deletePetFallback_throws() {
        assertThrows(TooManyPetRequests.class, () -> petService.deletePetFallback(UUID.randomUUID(), mock(RequestNotPermitted.class)));
    }

    private Pet buildPet() {
        return Pet.builder()
                .petId(UUID.randomUUID())
                .name("Fluffy")
                .description("A cute pet")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2020, 1, 1))
                .breed("Labrador")
                .species(PetSpecies.DOG)
                .myVaccinesURLs(List.of("url1"))
                .myPicturesURLs(List.of("pic1"))
                .user(new User())
                .build();
    }
} 