package com.example.registrationmodule.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

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
    private UUID petId;
    private String name;
    private String description;
    private String gender;
    private int age;
    private String breed;
    private String species; // cat or dog,..

    @ElementCollection
    @CollectionTable(name = "pets_vaccines_urls", joinColumns = @JoinColumn(name = "pet_id"))
    @Column(name = "url")
    private List<String> myVaccinesURLs;

    @ElementCollection
    @CollectionTable(name = "pets_pictures_urls", joinColumns = @JoinColumn(name = "pet_id"))
    @Column(name = "url")
    private List<String> myPicturesURLs;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
