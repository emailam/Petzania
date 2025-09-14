package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, UUID> {
}
