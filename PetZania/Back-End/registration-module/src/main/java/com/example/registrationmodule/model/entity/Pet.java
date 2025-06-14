package com.example.registrationmodule.model.entity;

import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(
        name = "pets",
        indexes = {
                @Index(name = "idx_pet_user", columnList = "user_id"),
                @Index(name = "idx_pet_species", columnList = "species"),
                @Index(name = "idx_pet_breed", columnList = "breed"),
                @Index(name = "idx_pet_gender", columnList = "gender")
        }
)
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "breed", nullable = false)
    private String breed;

    @Enumerated(EnumType.STRING)
    @Column(name = "species", nullable = false)
    private PetSpecies species;

    @ElementCollection
    @CollectionTable(name = "pets_vaccines_urls", joinColumns = @JoinColumn(name = "pet_id"))
    @Column(name = "vaccine_url")
    private List<String> myVaccinesURLs;

    @ElementCollection
    @CollectionTable(name = "pets_pictures_urls", joinColumns = @JoinColumn(name = "pet_id"))
    @Column(name = "picture_url")
    private List<String> myPicturesURLs;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

