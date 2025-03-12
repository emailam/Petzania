package com.example.registrationmodule.model.entity;

import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
@Table(name = "pets")
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
    private PetSpecies species; // cat or dog,..

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
