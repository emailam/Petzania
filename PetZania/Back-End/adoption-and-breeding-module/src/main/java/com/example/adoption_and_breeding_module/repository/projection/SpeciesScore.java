package com.example.adoption_and_breeding_module.repository.projection;

import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;

public interface SpeciesScore {
    PetSpecies getSpecies();
    Long getScore();
}
