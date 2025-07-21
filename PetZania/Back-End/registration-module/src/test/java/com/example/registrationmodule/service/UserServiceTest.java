package com.example.registrationmodule.service;

import com.example.registrationmodule.exception.authenticationAndVerificattion.*;
import com.example.registrationmodule.exception.rateLimiting.*;
import com.example.registrationmodule.exception.user.*;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.repository.BlockRepository;
import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.service.impl.JWTService;
import com.example.registrationmodule.service.impl.RefreshTokenService;
import com.example.registrationmodule.service.impl.UserPublisher;
import com.example.registrationmodule.service.impl.UserService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Mock private IDTOConversionService converter;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private BlockRepository blockRepository;
    @Mock private IEmailService emailService;
    @Mock private JWTService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserPublisher userPublisher;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_success() {
        RegisterUserDTO dto = new RegisterUserDTO();
        dto.setUsername("user");
        dto.setEmail("user@test.com");
        dto.setPassword("pass");
        User user = new User(); user.setUsername("user"); user.setEmail("user@test.com"); user.setPassword("pass");
        when(converter.mapToUser(dto)).thenReturn(user);
        when(userRepository.findByUsernameIgnoreCase("user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);
        doNothing().when(userPublisher).sendUserRegisteredMessage(any());
        doNothing().when(emailService).sendEmail(any());
        when(converter.mapToUserProfileDto(user)).thenReturn(new UserProfileDTO());
        UserProfileDTO result = userService.registerUser(dto);
        assertNotNull(result);
    }

    @Test
    void registerUser_usernameExists_throws() {
        RegisterUserDTO dto = new RegisterUserDTO();
        dto.setUsername("user");
        User user = new User(); user.setUsername("user");
        when(converter.mapToUser(dto)).thenReturn(user);
        when(userRepository.findByUsernameIgnoreCase("user")).thenReturn(Optional.of(user));
        assertThrows(UsernameAlreadyExists.class, () -> userService.registerUser(dto));
    }

    @Test
    void registerUser_emailExists_throws() {
        RegisterUserDTO dto = new RegisterUserDTO();
        dto.setUsername("user");
        dto.setEmail("user@test.com");
        User user = new User(); user.setUsername("user"); user.setEmail("user@test.com");
        when(converter.mapToUser(dto)).thenReturn(user);
        when(userRepository.findByUsernameIgnoreCase("user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(EmailAlreadyExists.class, () -> userService.registerUser(dto));
    }

    @Test
    void registerUser_emailSendFails_doesNotThrow() {
        RegisterUserDTO dto = new RegisterUserDTO();
        dto.setUsername("user");
        dto.setEmail("user@test.com");
        dto.setPassword("pass");
        User user = new User(); user.setUsername("user"); user.setEmail("user@test.com"); user.setPassword("pass");
        when(converter.mapToUser(dto)).thenReturn(user);
        when(userRepository.findByUsernameIgnoreCase("user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);
        doNothing().when(userPublisher).sendUserRegisteredMessage(any());
        doThrow(new RuntimeException("fail")).when(emailService).sendEmail(any());
        when(converter.mapToUserProfileDto(user)).thenReturn(new UserProfileDTO());
        assertDoesNotThrow(() -> userService.registerUser(dto));
    }

    @Test
    void login_success() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("user@test.com");
        dto.setPassword("pass");
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(true); user.setBlocked(false); user.setLoginTimes(0); user.setUserId(UUID.randomUUID());
        when(userRepository.findByEmailIgnoreCase(dto.getEmail())).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh");
        when(userRepository.save(any())).thenReturn(user);
        ResponseLoginDTO result = userService.login(dto);
        assertEquals("Successful login", result.getMessage());
        assertEquals("access", result.getTokenDTO().getAccessToken());
        assertEquals("refresh", result.getTokenDTO().getRefreshToken());
        assertEquals(1, result.getLoginTimes());
        assertEquals(user.getUserId(), result.getUserId());
    }

    @Test
    void login_wrongEmail_throws() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("notfound@test.com");
        when(userRepository.findByEmailIgnoreCase(dto.getEmail())).thenReturn(Optional.empty());
        assertThrows(InvalidUserCredentials.class, () -> userService.login(dto));
    }

    @Test
    void login_wrongPassword_throws() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("user@test.com");
        dto.setPassword("wrong");
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(true); user.setBlocked(false);
        when(userRepository.findByEmailIgnoreCase(dto.getEmail())).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(false);
        assertThrows(InvalidUserCredentials.class, () -> userService.login(dto));
    }

    @Test
    void login_notVerified_throws() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("user@test.com");
        dto.setPassword("pass");
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(false); user.setBlocked(false);
        when(userRepository.findByEmailIgnoreCase(dto.getEmail())).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        assertThrows(UserNotVerified.class, () -> userService.login(dto));
    }

    @Test
    void login_blocked_throws() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("user@test.com");
        dto.setPassword("pass");
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(true); user.setBlocked(true);
        when(userRepository.findByEmailIgnoreCase(dto.getEmail())).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        assertThrows(UserIsBlocked.class, () -> userService.login(dto));
    }

    @Test
    void refreshToken_success() {
        String refreshToken = "refresh";
        User user = new User(); user.setEmail("user@test.com"); user.setBlocked(false);
        when(refreshTokenService.isTokenRevoked(refreshToken)).thenReturn(false);
        when(jwtService.extractEmail(refreshToken)).thenReturn("user@test.com");
        when(jwtService.extractRole(refreshToken)).thenReturn("ROLE_USER");
        when(jwtService.isTokenExpired(refreshToken)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("newAccess");
        TokenDTO result = userService.refreshToken(refreshToken);
        assertEquals("newAccess", result.getAccessToken());
        assertEquals(refreshToken, result.getRefreshToken());
    }

    @Test
    void refreshToken_null_throws() {
        assertThrows(RefreshTokenNotValid.class, () -> userService.refreshToken(null));
    }

    @Test
    void refreshToken_revoked_throws() {
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(true);
        assertThrows(RefreshTokenNotValid.class, () -> userService.refreshToken("refresh"));
    }

    @Test
    void refreshToken_expired_throws() {
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(false);
        when(jwtService.extractEmail("refresh")).thenReturn("user@test.com");
        when(jwtService.extractRole("refresh")).thenReturn("ROLE_USER");
        when(jwtService.isTokenExpired("refresh")).thenReturn(true);
        assertThrows(RefreshTokenNotValid.class, () -> userService.refreshToken("refresh"));
    }

    @Test
    void refreshToken_blocked_throws() {
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(false);
        when(jwtService.extractEmail("refresh")).thenReturn("user@test.com");
        when(jwtService.extractRole("refresh")).thenReturn("ROLE_USER");
        when(jwtService.isTokenExpired("refresh")).thenReturn(false);
        User user = new User(); user.setBlocked(true);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(UserIsBlocked.class, () -> userService.refreshToken("refresh"));
    }

    @Test
    void sendVerificationCode_success() {
        User user = new User(); user.setEmail("user@test.com"); user.setUsername("user"); user.setVerified(false); user.setBlocked(false);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(emailService).sendEmail(any());
        when(userRepository.save(any())).thenReturn(user);
        assertDoesNotThrow(() -> userService.sendVerificationCode("user@test.com"));
    }

    @Test
    void sendVerificationCode_alreadyVerified_throws() {
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(true);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(UserAlreadyVerified.class, () -> userService.sendVerificationCode("user@test.com"));
    }

    @Test
    void sendVerificationCode_blocked_throws() {
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(false); user.setBlocked(true);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(UserIsBlocked.class, () -> userService.sendVerificationCode("user@test.com"));
    }

    @Test
    void verifyCode_success() {
        OTPValidationDTO dto = new OTPValidationDTO();
        dto.setEmail("user@test.com");
        dto.setOtp("780023");
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(false); user.setVerificationCode("780023"); user.setExpirationTime(new Timestamp(System.currentTimeMillis() + 10000));
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        assertDoesNotThrow(() -> userService.verifyCode(dto));
    }

    @Test
    void verifyCode_alreadyVerified_throws() {
        OTPValidationDTO dto = new OTPValidationDTO();
        dto.setEmail("user@test.com");
        dto.setOtp("780023");
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(true);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(UserAlreadyVerified.class, () -> userService.verifyCode(dto));
    }

    @Test
    void verifyCode_wrongOtp_throws() {
        OTPValidationDTO dto = new OTPValidationDTO();
        dto.setEmail("user@test.com");
        dto.setOtp("000000");
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(false); user.setVerificationCode("780023");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(InvalidOTPCode.class, () -> userService.verifyCode(dto));
    }

    @Test
    void verifyCode_expiredOtp_throws() {
        OTPValidationDTO dto = new OTPValidationDTO();
        dto.setEmail("user@test.com");
        dto.setOtp("780023");
        User user = new User(); user.setEmail("user@test.com"); user.setVerified(false); user.setVerificationCode("780023"); user.setExpirationTime(new Timestamp(System.currentTimeMillis() - 10000));
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(ExpiredOTP.class, () -> userService.verifyCode(dto));
    }

    @Test
    void sendResetPasswordOTP_success() {
        EmailDTO dto = new EmailDTO();
        dto.setEmail("user@test.com");
        User user = new User(); user.setEmail("user@test.com"); user.setUsername("user"); user.setBlocked(false);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(emailService).sendEmail(any());
        when(userRepository.save(any())).thenReturn(user);
        assertDoesNotThrow(() -> userService.sendResetPasswordOTP(dto));
    }

    @Test
    void sendResetPasswordOTP_blocked_throws() {
        EmailDTO dto = new EmailDTO();
        dto.setEmail("user@test.com");
        User user = new User(); user.setEmail("user@test.com"); user.setBlocked(true);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(UserIsBlocked.class, () -> userService.sendResetPasswordOTP(dto));
    }

    @Test
    void verifyResetOTP_success() {
        User user = new User(); user.setEmail("user@test.com"); user.setResetCode("607234"); user.setResetCodeExpirationTime(new Timestamp(System.currentTimeMillis() + 10000));
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertDoesNotThrow(() -> userService.verifyResetOTP("user@test.com", "607234"));
    }

    @Test
    void verifyResetOTP_wrongOtp_throws() {
        User user = new User(); user.setEmail("user@test.com"); user.setResetCode("607234");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(InvalidOTPCode.class, () -> userService.verifyResetOTP("user@test.com", "000000"));
    }

    @Test
    void verifyResetOTP_expired_throws() {
        User user = new User(); user.setEmail("user@test.com"); user.setResetCode("607234"); user.setResetCodeExpirationTime(new Timestamp(System.currentTimeMillis() - 10000));
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(ExpiredOTP.class, () -> userService.verifyResetOTP("user@test.com", "607234"));
    }

    @Test
    void resetPassword_success() {
        User user = new User(); user.setEmail("user@test.com"); user.setResetCode("607234");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        assertDoesNotThrow(() -> userService.resetPassword("user@test.com", "607234", "newPass123!"));
    }

    @Test
    void resetPassword_wrongOtp_throws() {
        User user = new User(); user.setEmail("user@test.com"); user.setResetCode("607234");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(InvalidOTPCode.class, () -> userService.resetPassword("user@test.com", "000000", "newPass123!"));
    }

    @Test
    void blockUser_success() {
        BlockUserDTO dto = new BlockUserDTO();
        dto.setEmail("user@test.com");
        User user = new User(); user.setEmail("user@test.com"); user.setBlocked(false);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(emailService).sendEmail(any());
        assertDoesNotThrow(() -> userService.blockUser(dto));
        assertTrue(user.isBlocked());
    }

    @Test
    void blockUser_alreadyBlocked_throws() {
        BlockUserDTO dto = new BlockUserDTO();
        dto.setEmail("user@test.com");
        User user = new User(); user.setEmail("user@test.com"); user.setBlocked(true);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(UserAlreadyBlocked.class, () -> userService.blockUser(dto));
    }

    @Test
    void unblockUser_success() {
        BlockUserDTO dto = new BlockUserDTO();
        dto.setEmail("user@test.com");
        User user = new User(); user.setEmail("user@test.com"); user.setBlocked(true);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertDoesNotThrow(() -> userService.unblockUser(dto));
        assertFalse(user.isBlocked());
    }

    @Test
    void unblockUser_alreadyUnblocked_throws() {
        BlockUserDTO dto = new BlockUserDTO();
        dto.setEmail("user@test.com");
        User user = new User(); user.setEmail("user@test.com"); user.setBlocked(false);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertThrows(UserAlreadyUnblocked.class, () -> userService.unblockUser(dto));
    }

    @Test
    void changePassword_success() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setEmail("user@test.com");
        dto.setNewPassword("NewPass123!");
        User user = new User(); user.setEmail("user@test.com");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        assertDoesNotThrow(() -> userService.changePassword(dto));
    }

    @Test
    void changePassword_userNotFound_throws() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setEmail("notfound@test.com");
        when(userRepository.findByEmailIgnoreCase("notfound@test.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFound.class, () -> userService.changePassword(dto));
    }

    @Test
    void deleteUser_success() {
        EmailDTO dto = new EmailDTO();
        dto.setEmail("user@test.com");
        User user = new User(); user.setEmail("user@test.com");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(emailService).sendEmail(any());
        doNothing().when(userPublisher).sendUserDeletedMessage(any());
        doNothing().when(userRepository).deleteByEmail("user@test.com");
        assertDoesNotThrow(() -> userService.deleteUser(dto));
    }

    @Test
    void deleteUser_notFound_throws() {
        EmailDTO dto = new EmailDTO();
        dto.setEmail("notfound@test.com");
        when(userRepository.findByEmailIgnoreCase("notfound@test.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFound.class, () -> userService.deleteUser(dto));
    }

    @Test
    void logout_success() {
        LogoutDTO dto = new LogoutDTO("user@test.com", "refresh");
        User user = new User(); user.setEmail("user@test.com");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(false);
        when(refreshTokenService.saveToken("refresh")).thenReturn(true);
        when(userRepository.save(any())).thenReturn(user);
        assertDoesNotThrow(() -> userService.logout(dto));
    }

    @Test
    void logout_alreadyLoggedOut_throws() {
        LogoutDTO dto = new LogoutDTO("user@test.com", "refresh");
        User user = new User(); user.setEmail("user@test.com");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(true);
        assertThrows(UserAlreadyLoggedOut.class, () -> userService.logout(dto));
    }

    @Test
    void updateUserById_success() {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileDto dto = new UpdateUserProfileDto();
        dto.setName("New Name");
        dto.setBio("New Bio");
        dto.setProfilePictureURL("http://pic.url");
        dto.setPhoneNumber("+1234567890");
        User user = new User(); user.setUserId(userId);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(converter.mapToUserProfileDto(any())).thenReturn(new UserProfileDTO());
        assertNotNull(userService.updateUserById(userId, dto));
    }

    @Test
    void updateUserById_notFound_throws() {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileDto dto = new UpdateUserProfileDto();
        when(userRepository.existsById(userId)).thenReturn(false);
        assertThrows(UserNotFound.class, () -> userService.updateUserById(userId, dto));
    }

    @Test
    void getUserById_success() {
        UUID requesterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User(); user.setUserId(userId);
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(requesterId, userId)).thenReturn(false);
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(userId, requesterId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(converter.mapToUserProfileDto(user)).thenReturn(new UserProfileDTO());
        assertNotNull(userService.getUserById(requesterId, userId));
    }

    @Test
    void getUserById_blocked_throws() {
        UUID requesterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(requesterId, userId)).thenReturn(true);
        assertThrows(UserAccessDenied.class, () -> userService.getUserById(requesterId, userId));
    }

    @Test
    void getUsers_success() {
        UUID requesterId = UUID.randomUUID();
        List<User> users = List.of(new User());
        when(userRepository.findAllExcludingBlocked(eq(requesterId), any())).thenReturn(new PageImpl<>(users));
        when(converter.mapToUserProfileDto(any())).thenReturn(new UserProfileDTO());
        assertNotNull(userService.getUsers(requesterId, 0, 10, "username", "asc"));
    }

    @Test
    void getUsersByPrefixUsername_success() {
        UUID requesterId = UUID.randomUUID();
        List<User> users = List.of(new User());
        when(userRepository.findByUsernameStartingWithIgnoreCaseExcludingBlocked(eq("pre"), eq(requesterId), any())).thenReturn(new PageImpl<>(users));
        when(converter.mapToUserProfileDto(any())).thenReturn(new UserProfileDTO());
        assertNotNull(userService.getUsersByPrefixUsername(requesterId, 0, 10, "username", "asc", "pre"));
    }

    @Test
    void getProfilePictureURLByUserId_success() {
        UUID requesterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User(); user.setUserId(userId); user.setProfilePictureURL("http://pic.url");
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(requesterId, userId)).thenReturn(false);
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(userId, requesterId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        ProfilePictureDTO expected = new ProfilePictureDTO("http://pic.url");
        assertEquals(expected.getProfilePictureURL(), userService.getProfilePictureURLByUserId(requesterId, userId).getProfilePictureURL());
    }

    @Test
    void getProfilePictureURLByUserId_blocked_throws() {
        UUID requesterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(requesterId, userId)).thenReturn(true);
        assertThrows(UserAccessDenied.class, () -> userService.getProfilePictureURLByUserId(requesterId, userId));
    }

    @Test
    void deleteAll_success() {
        User user = new User(); user.setUserId(UUID.randomUUID()); user.setUsername("user"); user.setEmail("user@test.com");
        when(userRepository.findAll()).thenReturn(List.of(user));
        doNothing().when(userRepository).deleteAll();
        doNothing().when(userPublisher).sendUserDeletedMessage(any());
        assertDoesNotThrow(() -> userService.deleteAll());
    }

    @Test
    void userExistsById_true() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        assertTrue(userService.userExistsById(userId));
    }

    @Test
    void userExistsById_false() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertFalse(userService.userExistsById(userId));
    }

    // Fallbacks for rate limiters
    @Test
    void registerFallback_throws() {
        assertThrows(TooManyRegistrationRequests.class, () -> userService.registerFallback(new RegisterUserDTO(), mock(RequestNotPermitted.class)));
    }
    @Test
    void loginFallback_throws() {
        assertThrows(TooManyLoginRequests.class, () -> userService.loginFallback(new LoginUserDTO(), mock(RequestNotPermitted.class)));
    }
    @Test
    void refreshFallback_throws() {
        assertThrows(TooManyRefreshRequests.class, () -> userService.refreshFallback("refresh", mock(RequestNotPermitted.class)));
    }
    @Test
    void logoutFallback_throws() {
        assertThrows(TooManyLogoutRequests.class, () -> userService.logoutFallback(new LogoutDTO("user@test.com", "refresh"), mock(RequestNotPermitted.class)));
    }
    @Test
    void otpFallback_throws() {
        assertThrows(TooManyOtpRequests.class, () -> userService.otpFallback(new OTPValidationDTO(), mock(RequestNotPermitted.class)));
    }
    @Test
    void sendOtpFallback_throws() {
        assertThrows(TooManyOtpRequests.class, () -> userService.sendOtpFallback("user@test.com", mock(RequestNotPermitted.class)));
    }
} 