package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.AuthTokenResponseDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.LoginRequestDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.User;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.redis.RefreshTokenRedisRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.security.PasswordValidationService;
import co.za.kingstechco.kingstechco.invoeaserapp.security.RateLimitService;
import co.za.kingstechco.kingstechco.invoeaserapp.service.UserService;
import co.za.kingstechco.kingstechco.invoeaserapp.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authManager;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private UserService userService;
    
    @Mock
    private RefreshTokenRedisRepository refreshTokenRepo;
    
    @Mock
    private RateLimitService rateLimitService;
    
    @Mock
    private PasswordValidationService passwordValidator;
    
    @Mock
    private RedisTokenService redisTokenService;
    
    @Mock
    private UserDetailsService userDetailsService;
    
    @Mock
    private BindingResult bindingResult;

    private AuthServiceImpl authService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_IP = "192.168.1.1";
    private static final String TEST_ACCESS_TOKEN = "access-token";
    private static final String TEST_REFRESH_TOKEN = "refresh-token";
    private static final long TOKEN_EXPIRATION = 900000L;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                authManager,
                jwtUtil,
                userService,
                refreshTokenRepo,
                rateLimitService,
                passwordValidator,
                redisTokenService,
                userDetailsService
        );
    }

    @Test
    void registerUser_ShouldReturnCreatedResponse_WhenValidRegistration() {
        // Given
        UserRegistrationDto dto = createUserRegistrationDto();
        User newUser = createTestUser();
        
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.registerUser(dto)).thenReturn(newUser);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(
                org.springframework.security.core.userdetails.User.builder()
                        .username(TEST_USERNAME)
                        .password("password")
                        .authorities("ROLE_USER")
                        .build());
        when(jwtUtil.generateAccessTokenWithRoles(eq(TEST_USERNAME), any())).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(TEST_USERNAME)).thenReturn(TEST_REFRESH_TOKEN);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(TOKEN_EXPIRATION);

        // When
        ResponseEntity<AuthTokenResponseDto> response = authService.registerUser(dto, bindingResult, TEST_IP);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TEST_ACCESS_TOKEN, response.getBody().getAccessToken());
        assertEquals(TEST_REFRESH_TOKEN, response.getBody().getRefreshToken());
        assertEquals("Bearer", response.getBody().getTokenType());
        assertEquals(TOKEN_EXPIRATION, response.getBody().getExpiresIn());
        
        verify(userService).registerUser(dto);
        verify(jwtUtil).generateAccessTokenWithRoles(eq(TEST_USERNAME), any());
        verify(jwtUtil).generateRefreshToken(TEST_USERNAME);
    }

    @Test
    void registerUser_ShouldThrowException_WhenValidationErrors() {
        // Given
        UserRegistrationDto dto = createUserRegistrationDto();
        when(bindingResult.hasErrors()).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.registerUser(dto, bindingResult, TEST_IP));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Validation failed", exception.getReason());
        
        verify(userService, never()).registerUser(any());
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void authenticate_ShouldReturnTokenResponse_WhenValidCredentials() {
        // Given
        LoginRequestDto dto = createLoginRequestDto();
        String identifier = TEST_USERNAME + ":" + TEST_IP;
        
        when(bindingResult.hasErrors()).thenReturn(false);
        when(rateLimitService.isRateLimited(identifier)).thenReturn(false);
        
        // Mock authentication manager
        org.springframework.security.core.userdetails.UserDetails userDetails = 
                org.springframework.security.core.userdetails.User.builder()
                .username(TEST_USERNAME)
                .password("password")
                .authorities("ROLE_USER")
                .build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        
        when(jwtUtil.generateAccessTokenWithRoles(eq(TEST_USERNAME), any())).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(TEST_USERNAME)).thenReturn(TEST_REFRESH_TOKEN);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(TOKEN_EXPIRATION);

        // When
        ResponseEntity<?> response = authService.authenticate(dto, bindingResult, TEST_IP);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        verify(rateLimitService).clearAttempts(identifier);
        verify(refreshTokenRepo).saveRefreshToken(TEST_USERNAME, TEST_REFRESH_TOKEN);
        verify(jwtUtil).generateAccessTokenWithRoles(eq(TEST_USERNAME), any());
        verify(jwtUtil).generateRefreshToken(TEST_USERNAME);
    }

    @Test
    void authenticate_ShouldReturnRateLimitError_WhenRateLimited() {
        // Given
        LoginRequestDto dto = createLoginRequestDto();
        String identifier = TEST_USERNAME + ":" + TEST_IP;
        
        when(bindingResult.hasErrors()).thenReturn(false);
        when(rateLimitService.isRateLimited(identifier)).thenReturn(true);
        when(rateLimitService.getLockoutTimeRemaining(identifier)).thenReturn(300L);

        // When
        ResponseEntity<?> response = authService.authenticate(dto, bindingResult, TEST_IP);

        // Then
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        
        verify(authManager, never()).authenticate(any());
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void authenticate_ShouldReturnUnauthorized_WhenBadCredentials() {
        // Given
        LoginRequestDto dto = createLoginRequestDto();
        String identifier = TEST_USERNAME + ":" + TEST_IP;
        
        when(bindingResult.hasErrors()).thenReturn(false);
        when(rateLimitService.isRateLimited(identifier)).thenReturn(false);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Authentication failed"));
        when(rateLimitService.incrementAttempt(identifier)).thenReturn(false);
        when(rateLimitService.getRemainingAttempts(identifier)).thenReturn(2);

        // When
        ResponseEntity<?> response = authService.authenticate(dto, bindingResult, TEST_IP);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        verify(rateLimitService).incrementAttempt(identifier);
        verify(rateLimitService).getRemainingAttempts(identifier);
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class)); // Called but throws exception
    }

    @Test
    void authenticate_ShouldLockAccount_WhenTooManyFailedAttempts() {
        // Given
        LoginRequestDto dto = createLoginRequestDto();
        String identifier = TEST_USERNAME + ":" + TEST_IP;
        
        when(bindingResult.hasErrors()).thenReturn(false);
        when(rateLimitService.isRateLimited(identifier)).thenReturn(false);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Authentication failed"));
        when(rateLimitService.incrementAttempt(identifier)).thenReturn(true); // Account locked

        // When
        ResponseEntity<?> response = authService.authenticate(dto, bindingResult, TEST_IP);

        // Then
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        
        verify(rateLimitService).incrementAttempt(identifier);
    }

    @Test
    void authenticate_ShouldReturnValidationErrors_WhenBindingResultHasErrors() {
        // Given
        LoginRequestDto dto = createLoginRequestDto();
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        ResponseEntity<?> response = authService.authenticate(dto, bindingResult, TEST_IP);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(authManager, never()).authenticate(any());
        verify(rateLimitService, never()).isRateLimited(any());
    }

    @Test
    void logout_ShouldReturnSuccess_WhenValidRefreshToken() {
        // Given
        when(jwtUtil.validateToken(TEST_REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_USERNAME);

        // When
        ResponseEntity<?> response = authService.logout(TEST_REFRESH_TOKEN, TEST_ACCESS_TOKEN);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertNotNull(responseBody);
        assertEquals("Logged out successfully", responseBody.get("message"));
        
        verify(refreshTokenRepo).removeRefreshToken(TEST_USERNAME);
        verify(redisTokenService).blacklistToken(TEST_ACCESS_TOKEN);
    }

    @Test
    void logout_ShouldReturnBadRequest_WhenInvalidRefreshToken() {
        // Given
        when(jwtUtil.validateToken(TEST_REFRESH_TOKEN)).thenReturn(false);

        // When
        ResponseEntity<?> response = authService.logout(TEST_REFRESH_TOKEN, TEST_ACCESS_TOKEN);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(refreshTokenRepo, never()).removeRefreshToken(any());
        verify(redisTokenService, never()).blacklistToken(any());
    }

    @Test
    void logout_ShouldReturnBadRequest_WhenRefreshTokenIsNull() {
        // When
        ResponseEntity<?> response = authService.logout(null, TEST_ACCESS_TOKEN);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(jwtUtil, never()).validateToken(any());
        verify(refreshTokenRepo, never()).removeRefreshToken(any());
    }

    @Test
    void logout_ShouldHandleAccessTokenNull_Gracefully() {
        // Given
        when(jwtUtil.validateToken(TEST_REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_USERNAME);

        // When
        ResponseEntity<?> response = authService.logout(TEST_REFRESH_TOKEN, null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        verify(refreshTokenRepo).removeRefreshToken(TEST_USERNAME);
        verify(redisTokenService, never()).blacklistToken(any());
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenValidRefreshToken() {
        // Given
        when(jwtUtil.validateToken(TEST_REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_USERNAME);
        when(refreshTokenRepo.findRefreshTokenByUsername(TEST_USERNAME)).thenReturn(TEST_REFRESH_TOKEN);
        when(jwtUtil.generateAccessToken(TEST_USERNAME)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(TEST_USERNAME)).thenReturn("new-refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(TOKEN_EXPIRATION);

        // When
        ResponseEntity<?> response = authService.refreshToken(TEST_REFRESH_TOKEN);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        verify(refreshTokenRepo).saveRefreshToken(TEST_USERNAME, "new-refresh-token");
        verify(jwtUtil).generateAccessToken(TEST_USERNAME);
        verify(jwtUtil).generateRefreshToken(TEST_USERNAME);
    }

    @Test
    void refreshToken_ShouldReturnBadRequest_WhenRefreshTokenIsNull() {
        // When
        ResponseEntity<?> response = authService.refreshToken(null);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(jwtUtil, never()).validateToken(any());
    }

    @Test
    void refreshToken_ShouldReturnBadRequest_WhenRefreshTokenIsBlank() {
        // When
        ResponseEntity<?> response = authService.refreshToken("   ");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(jwtUtil, never()).validateToken(any());
    }

    @Test
    void refreshToken_ShouldReturnUnauthorized_WhenInvalidRefreshToken() {
        // Given
        when(jwtUtil.validateToken(TEST_REFRESH_TOKEN)).thenReturn(false);

        // When
        ResponseEntity<?> response = authService.refreshToken(TEST_REFRESH_TOKEN);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        verify(jwtUtil, never()).getUsernameFromToken(any());
        verify(refreshTokenRepo, never()).findRefreshTokenByUsername(any());
    }

    @Test
    void refreshToken_ShouldReturnUnauthorized_WhenRefreshTokenMismatch() {
        // Given
        when(jwtUtil.validateToken(TEST_REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_USERNAME);
        when(refreshTokenRepo.findRefreshTokenByUsername(TEST_USERNAME)).thenReturn("different-token");

        // When
        ResponseEntity<?> response = authService.refreshToken(TEST_REFRESH_TOKEN);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        verify(jwtUtil, never()).generateAccessToken(any());
        verify(jwtUtil, never()).generateRefreshToken(any());
    }

    @Test
    void authenticate_ShouldHandleGeneralException_Gracefully() {
        // Given
        LoginRequestDto dto = createLoginRequestDto();
        String identifier = TEST_USERNAME + ":" + TEST_IP;
        
        when(bindingResult.hasErrors()).thenReturn(false);
        when(rateLimitService.isRateLimited(identifier)).thenReturn(false);
        when(authManager.authenticate(any())).thenThrow(new RuntimeException("Unexpected error"));

        // When
        ResponseEntity<?> response = authService.authenticate(dto, bindingResult, TEST_IP);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        verify(refreshTokenRepo, never()).saveRefreshToken(any(), any());
    }

    @Test
    void logout_ShouldReturnInternalError_WhenExceptionOccurs() {
        // Given
        when(jwtUtil.validateToken(TEST_REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(TEST_REFRESH_TOKEN)).thenThrow(new RuntimeException("Token parsing error"));

        // When
        ResponseEntity<?> response = authService.logout(TEST_REFRESH_TOKEN, TEST_ACCESS_TOKEN);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        verify(refreshTokenRepo, never()).removeRefreshToken(any());
    }

    // Helper methods
    private UserRegistrationDto createUserRegistrationDto() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername(TEST_USERNAME);
        dto.setPassword(TEST_PASSWORD);
        dto.setEmail(TEST_EMAIL);
        return dto;
    }

    private LoginRequestDto createLoginRequestDto() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername(TEST_USERNAME);
        dto.setPassword(TEST_PASSWORD);
        return dto;
    }

    private User createTestUser() {
        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        return user;
    }
}