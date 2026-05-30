package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.User;
import co.za.kingstechco.kingstechco.invoeaserapp.exception.ResourceNotFoundException;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.RoleRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String RAW_PASSWORD = "SecurePassword123!";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPasswordHash";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void registerUser_ShouldCreateAndSaveUser_WhenValidDto() {
        // Given
        UserRegistrationDto registrationDto = createUserRegistrationDto();
        User savedUser = createExpectedUser();
        savedUser.setUserId(USER_ID);
        
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.registerUser(registrationDto);

        // Then
        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(USERNAME, result.getUsername());
        assertEquals(EMAIL, result.getEmail());
        assertEquals(FIRST_NAME, result.getFirstName());
        assertEquals(LAST_NAME, result.getLastName());
        assertEquals(ENCODED_PASSWORD, result.getPasswordHash());
        
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository).save(argThat(user -> 
            user.getUsername().equals(USERNAME) &&
            user.getEmail().equals(EMAIL) &&
            user.getFirstName().equals(FIRST_NAME) &&
            user.getLastName().equals(LAST_NAME) &&
            user.getPasswordHash().equals(ENCODED_PASSWORD)
        ));
    }

    @Test
    void registerUser_ShouldEncodePassword_BeforeSaving() {
        // Given
        UserRegistrationDto registrationDto = createUserRegistrationDto();
        User savedUser = createExpectedUser();
        
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        userService.registerUser(registrationDto);

        // Then
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository).save(argThat(user -> 
            user.getPasswordHash().equals(ENCODED_PASSWORD)
        ));
    }

    @Test
    void registerUser_ShouldHandleRepositoryException_Gracefully() {
        // Given
        UserRegistrationDto registrationDto = createUserRegistrationDto();
        
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.registerUser(registrationDto));
        
        assertEquals("Database error", exception.getMessage());
        
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldHandlePasswordEncodingException_Gracefully() {
        // Given
        UserRegistrationDto registrationDto = createUserRegistrationDto();
        
        when(passwordEncoder.encode(RAW_PASSWORD)).thenThrow(new RuntimeException("Password encoding failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.registerUser(registrationDto));
        
        assertEquals("Password encoding failed", exception.getMessage());
        
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_ShouldSetAllUserFields_FromDto() {
        // Given
        UserRegistrationDto registrationDto = createUserRegistrationDto();
        registrationDto.setFirstName("Jane");
        registrationDto.setLastName("Smith");
        registrationDto.setEmail("jane.smith@example.com");
        registrationDto.setUsername("janesmith");
        
        User savedUser = new User();
        savedUser.setUserId(USER_ID);
        
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        userService.registerUser(registrationDto);

        // Then
        verify(userRepository).save(argThat(user -> 
            user.getUsername().equals("janesmith") &&
            user.getEmail().equals("jane.smith@example.com") &&
            user.getFirstName().equals("Jane") &&
            user.getLastName().equals("Smith") &&
            user.getPasswordHash().equals(ENCODED_PASSWORD)
        ));
    }

    @Test
    void registerUser_ShouldHandleNullValues_InDto() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername(USERNAME);
        registrationDto.setPassword(RAW_PASSWORD);
        // firstName, lastName, email are null
        
        User savedUser = new User();
        savedUser.setUserId(USER_ID);
        
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        userService.registerUser(registrationDto);

        // Then
        verify(userRepository).save(argThat(user -> 
            user.getUsername().equals(USERNAME) &&
            user.getEmail() == null &&
            user.getFirstName() == null &&
            user.getLastName() == null &&
            user.getPasswordHash().equals(ENCODED_PASSWORD)
        ));
    }

    @Test
    void registerUser_ShouldCallServices_InCorrectOrder() {
        // Given
        UserRegistrationDto registrationDto = createUserRegistrationDto();
        User savedUser = createExpectedUser();
        
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        userService.registerUser(registrationDto);

        // Then - Verify order of operations
        var inOrder = inOrder(passwordEncoder, userRepository);
        inOrder.verify(passwordEncoder).encode(RAW_PASSWORD);
        inOrder.verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserByUsername_ShouldReturnUser_WhenUserExists() {
        // Given
        User expectedUser = createExpectedUser();
        expectedUser.setUserId(USER_ID);
        
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(expectedUser));

        // When
        User result = userService.getUserByUsername(USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(expectedUser, result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(USERNAME, result.getUsername());
        assertEquals(EMAIL, result.getEmail());
        
        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void getUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUsername(USERNAME));
        
        assertEquals("User not found with username: " + USERNAME, exception.getMessage());
        
        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void getUserByUsername_ShouldHandleNullUsername_Gracefully() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUsername(null));
        
        assertTrue(exception.getMessage().contains("User not found with username: null"));
        
        verify(userRepository).findByUsername(null);
    }

    @Test
    void getUserByUsername_ShouldHandleEmptyUsername_Gracefully() {
        // Given
        String emptyUsername = "";
        when(userRepository.findByUsername(emptyUsername)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUsername(emptyUsername));
        
        assertTrue(exception.getMessage().contains("User not found with username: "));
        
        verify(userRepository).findByUsername(emptyUsername);
    }

    @Test
    void getUserByUsername_ShouldHandleRepositoryException_Gracefully() {
        // Given
        when(userRepository.findByUsername(USERNAME)).thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getUserByUsername(USERNAME));
        
        assertEquals("Database connection error", exception.getMessage());
        
        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void getUserByUsername_ShouldHandleDifferentUsernames_Correctly() {
        // Given
        String differentUsername = "anotheruser";
        User differentUser = new User();
        differentUser.setUserId(2L);
        differentUser.setUsername(differentUsername);
        
        when(userRepository.findByUsername(differentUsername)).thenReturn(Optional.of(differentUser));

        // When
        User result = userService.getUserByUsername(differentUsername);

        // Then
        assertNotNull(result);
        assertEquals(differentUsername, result.getUsername());
        assertEquals(2L, result.getUserId());
        
        verify(userRepository).findByUsername(differentUsername);
    }

    @Test
    void registerUser_ShouldCreateNewUserInstance_ForEachCall() {
        // Given
        UserRegistrationDto registrationDto1 = createUserRegistrationDto();
        UserRegistrationDto registrationDto2 = createUserRegistrationDto();
        registrationDto2.setUsername("user2");
        
        User savedUser1 = new User();
        savedUser1.setUserId(1L);
        User savedUser2 = new User();
        savedUser2.setUserId(2L);
        
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(savedUser1, savedUser2);

        // When
        User result1 = userService.registerUser(registrationDto1);
        User result2 = userService.registerUser(registrationDto2);

        // Then
        assertNotSame(result1, result2);
        verify(userRepository, times(2)).save(any(User.class));
        verify(passwordEncoder, times(2)).encode(RAW_PASSWORD);
    }

    @Test
    void constructor_ShouldInitializeCorrectly_WithDependencies() {
        // When
        UserServiceImpl service = new UserServiceImpl(userRepository, roleRepository, passwordEncoder);

        // Then
        assertNotNull(service);
        // Verify that the service can be used
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
                () -> service.getUserByUsername(USERNAME));
        verify(userRepository).findByUsername(USERNAME);
    }

    // Helper methods
    private UserRegistrationDto createUserRegistrationDto() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername(USERNAME);
        dto.setEmail(EMAIL);
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setPassword(RAW_PASSWORD);
        return dto;
    }

    private User createExpectedUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setPasswordHash(ENCODED_PASSWORD);
        return user;
    }
}