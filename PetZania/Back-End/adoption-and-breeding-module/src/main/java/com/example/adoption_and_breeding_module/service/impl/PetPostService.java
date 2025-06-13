package com.example.adoption_and_breeding_module.service.impl;

import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.dto.CreatePetPostDTO;
import com.example.adoption_and_breeding_module.model.dto.PetPostDTO;
import com.example.adoption_and_breeding_module.model.entity.PetPost;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.repository.PetPostRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.service.IDTOConversionService;
import com.example.adoption_and_breeding_module.service.IPetPostService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class PetPostService implements IPetPostService {

    private final UserRepository userRepository;
    private final PetPostRepository petPostRepository;
    private final IDTOConversionService dtoConversionService;

    @Override
    public PetPostDTO createPetPost(CreatePetPostDTO dto, UUID ownerId) {
//        User owner = userRepository.findById(ownerId)
//                .orElseThrow(() -> new UserNotFound("User not found"));
        // dummy
        User owner = User.builder()
                .userId(UUID.randomUUID())
                .username("dummy_user")
                .email("dummy@example.com")
                .profilePictureURL("https://example.com/default-avatar.png")
                .build();
        owner = userRepository.save(owner);

        PetPost post = PetPost.builder()
                .pet(dtoConversionService.mapToPet(dto.getPetDTO()))
                .owner(owner)
                .description(dto.getDescription())
                .postType(dto.getPostType())
                .build();

        post = petPostRepository.save(post);
        return dtoConversionService.mapToPetPostDTO(post);
    }
}
