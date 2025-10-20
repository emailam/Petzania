package com.example.registrationmodule.service;

import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.*;
import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;
import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.service.impl.DTOConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DTOConversionServiceTest {

    private UserRepository userRepository;
    private DTOConversionService conversionService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        conversionService = new DTOConversionService(userRepository);
    }

    @Test
    void mapToUserProfileDto_MapsCorrectly() {
        UUID userId = UUID.randomUUID();

        User author = new User();
        author.setUserId(userId);

        Pet pet = new Pet();
        pet.setPetId(UUID.randomUUID());
        pet.setName("Fluffy");
        pet.setDateOfBirth(LocalDate.now().minusYears(2));
        pet.setUser(author);
        List<Pet> pets = new ArrayList<>();
        pets.add(pet);

        User user = new User();
        user.setUserId(userId);
        user.setUsername("jane");
        user.setEmail("JANE@EXAMPLE.COM");
        user.setName("Jane Doe");
        user.setLoginTimes(10);
        user.setBio("About me");
        user.setProfilePictureURL("http://pic");
        user.setPhoneNumber("12345");
        user.setMyPets(pets);
        user.setBlocked(false);
        user.setOnline(true);

        UserProfileDTO dto = conversionService.mapToUserProfileDto(user);
        assertEquals(userId, dto.getUserId());
        assertEquals("jane", dto.getUsername());
        assertEquals("jane@example.com", dto.getEmail());
        assertEquals(1, dto.getMyPets().size());
        assertEquals("Fluffy", dto.getMyPets().get(0).getName());
    }

    @Test
    void mapToUserProfileDto_ReturnsNullIfUserIsNull() {
        assertNull(conversionService.mapToUserProfileDto(null));
    }

    @Test
    void mapToUser_ConvertsUpdateUserProfileDto() {
        UpdateUserProfileDto dto = new UpdateUserProfileDto();
        dto.setName("Bob");
        dto.setBio("Bio");
        dto.setProfilePictureURL("url");
        dto.setPhoneNumber("555");

        User user = conversionService.mapToUser(dto);

        assertEquals("Bob", user.getName());
        assertEquals("Bio", user.getBio());
        assertEquals("url", user.getProfilePictureURL());
        assertEquals("555", user.getPhoneNumber());
    }

    @Test
    void mapToAdmin_ConvertsAdminDto() {
        AdminDTO dto = new AdminDTO(null, "adm", "pw", AdminRole.ADMIN);
        Admin admin = conversionService.mapToAdmin(dto);

        assertEquals("adm", admin.getUsername());
        assertEquals("pw", admin.getPassword());
        assertEquals(AdminRole.ADMIN, admin.getRole());
    }

    @Test
    void mapToAdminDTO_ReturnsNullIfAdminNull() {
        assertNull(conversionService.mapToAdminDTO(null));
    }

    @Test
    void mapToAdminDTO_MapsAllFields() {
        UUID adminId = UUID.randomUUID();
        Admin admin = new Admin();
        admin.setAdminId(adminId);
        admin.setUsername("a");
        admin.setPassword("pw");

        admin.setRole(AdminRole.ADMIN);

        AdminDTO dto = conversionService.mapToAdminDTO(admin);

        assertEquals(adminId, dto.getAdminId());
        assertEquals("a", dto.getUsername());
        assertEquals("pw", dto.getPassword());
        assertEquals(AdminRole.ADMIN, dto.getAdminRole());
    }

    @Test
    void mapToPetDto_ReturnsNullIfPetNull() {
        assertNull(conversionService.mapToPetDto(null));
    }

    @Test
    void mapToPetDto_MapsAllFields() {
        UUID petId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);

        Pet pet = new Pet();
        pet.setPetId(petId);
        pet.setName("Rex");
        pet.setDescription("desc");
        pet.setGender(Gender.MALE);
        pet.setDateOfBirth(LocalDate.now().minusYears(3));
        pet.setBreed("breed");
        pet.setSpecies(PetSpecies.CAT);
        pet.setMyVaccinesURLs(Collections.singletonList("img1"));
        pet.setMyPicturesURLs(Collections.singletonList("img2"));
        pet.setUser(user);

        PetDTO dto = conversionService.mapToPetDto(pet);
        assertEquals("Rex", dto.getName());
        assertNotNull(dto.getAge());
        assertEquals(userId, dto.getUserId());
    }

    @Test
    void mapToPet_ThrowsUserNotFound() {
        PetDTO dto = new PetDTO();
        dto.setUserId(UUID.randomUUID());
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> conversionService.mapToPet(dto));
    }

    @Test
    void mapToPet_Success() {
        UUID userId = UUID.randomUUID();
        PetDTO dto = new PetDTO();
        dto.setName("Ruffus");
        dto.setUserId(userId);

        User user = new User();
        user.setUserId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Pet pet = conversionService.mapToPet(dto);
        assertEquals("Ruffus", pet.getName());
        assertEquals(userId, pet.getUser().getUserId());
    }

    @Test
    void mapToUser_FromRegisterUserDTOWorks() {
        RegisterUserDTO reg = new RegisterUserDTO();
        reg.setUsername("asmith");
        reg.setEmail("AS@i.com");
        reg.setPassword("pass");
        User user = conversionService.mapToUser(reg);

        assertEquals("asmith", user.getUsername());
        assertEquals("as@i.com", user.getEmail());
        assertEquals("pass", user.getPassword());
    }

    @Test
    void mapToMedia_Works() {
        MediaDTO dto = new MediaDTO();
        dto.setKey("abc");
        dto.setFormat("jpg");
        dto.setType("pic");
        dto.setUploadedAt(null);

        Media media = conversionService.mapToMedia(dto);
        assertEquals("abc", media.getKey());
        assertEquals("jpg", media.getFormat());
        assertEquals("pic", media.getType());
    }

    @Test
    void mediaToDto_Works() {
        UUID mediaId = UUID.randomUUID();
        Media media = new Media();
        media.setMediaId(mediaId);
        media.setKey("z");
        media.setFormat("png");
        media.setType("note");
        media.setUploadedAt(null);

        MediaResponseDTO dto = conversionService.mediaToDto(media);
        assertEquals(mediaId, dto.getMediaId());
        assertEquals("z", dto.getKey());
        assertEquals("png", dto.getFormat());
        assertEquals("note", dto.getType());
    }

    @Test
    void getPetAge_ReturnsCorrectAge() {
        LocalDate dob = LocalDate.now().minusYears(2).minusMonths(3);
        String age = conversionService.getPetAge(dob);
        assertTrue(age.contains("2 years") && age.contains("3 months"));
    }

    @Test
    void getPetAge_LessThanOneYear() {
        LocalDate dob = LocalDate.now().minusMonths(4);
        String age = conversionService.getPetAge(dob);
        assertEquals("4 months", age);
    }
}