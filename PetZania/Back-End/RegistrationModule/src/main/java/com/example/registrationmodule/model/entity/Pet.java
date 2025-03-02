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

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "pet_vaccines",
            joinColumns = @JoinColumn(name = "pet_id"),
            inverseJoinColumns = @JoinColumn(name = "vaccine_url"))
    private List<Media> myVaccines;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "pet_pictures",
            joinColumns = @JoinColumn(name = "pet_id"),
            inverseJoinColumns = @JoinColumn(name = "picture_url"))
    private List<Media> myPictures;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
