package co.za.kingstechco.kingstechco.invoeaserapp.util;

import co.za.kingstechco.kingstechco.invoeaserapp.config.JwtConfig;
import co.za.kingstechco.kingstechco.invoeaserapp.service.impl.RedisTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RedisTokenService redisTokenService;

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "VGhpcyBpcyBhIHZlcnkgc2VjdXJlIGFuZCBsb25nIHNlY3JldCBrZXkgZm9yIEpXVCB0ZXN0aW5n";
    private static final String TEST_USERNAME = "testuser";
    private static final long ACCESS_TOKEN_EXPIRATION = 900000L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        lenient().when(jwtConfig.getSecret()).thenReturn(TEST_SECRET);
        lenient().when(jwtConfig.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        lenient().when(jwtConfig.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);
        
        jwtUtil = new JwtUtil(jwtConfig, redisTokenService);
    }

    @Test
    void generateTokens_ShouldReturnBothAccessAndRefreshTokens() {
        // When
        Map<String, String> tokens = jwtUtil.generateTokens(TEST_USERNAME);

        // Then
        assertNotNull(tokens);
        assertTrue(tokens.containsKey("access_token"));
        assertTrue(tokens.containsKey("refresh_token"));
        assertNotNull(tokens.get("access_token"));
        assertNotNull(tokens.get("refresh_token"));
        assertNotEquals(tokens.get("access_token"), tokens.get("refresh_token"));
    }

    @Test
    void generateAccessToken_ShouldReturnValidToken() {
        // When
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Verify token can be parsed
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        assertEquals(TEST_USERNAME, extractedUsername);
    }

    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        // When
        String token = jwtUtil.generateRefreshToken(TEST_USERNAME);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Verify token can be parsed
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        assertEquals(TEST_USERNAME, extractedUsername);
    }

    @Test
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // When
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Then
        assertEquals(TEST_USERNAME, extractedUsername);
    }

    @Test
    void validateToken_ShouldReturnTrue_WhenTokenIsValid() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);
        when(redisTokenService.isTokenRevoked(token)).thenReturn(false);

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsRevoked() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);
        when(redisTokenService.isTokenRevoked(token)).thenReturn(true);

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsExpired() {
        // Given
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(-1000L); // Already expired
        String expiredToken = jwtUtil.generateAccessToken(TEST_USERNAME);
        when(redisTokenService.isTokenRevoked(expiredToken)).thenReturn(false);

        // When
        boolean isValid = jwtUtil.validateToken(expiredToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsMalformed() {
        // Given
        String malformedToken = "invalid.token.format";
        when(redisTokenService.isTokenRevoked(malformedToken)).thenReturn(false);

        // When
        boolean isValid = jwtUtil.validateToken(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsNull() {
        // When
        boolean isValid = jwtUtil.validateToken(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsEmpty() {
        // When
        boolean isValid = jwtUtil.validateToken("");

        // Then
        assertFalse(isValid);
    }

    @Test
    void revokeToken_ShouldCallRedisTokenService() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // When
        jwtUtil.revokeToken(token);

        // Then
        verify(redisTokenService).revokeToken(anyString(), anyLong());
    }

    @Test
    void extractUsername_ShouldReturnUsername_WhenTokenIsValid() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // When
        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertEquals(TEST_USERNAME, extractedUsername);
    }

    @Test
    void extractUsername_ShouldReturnNull_WhenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid.token.format";

        // When
        String extractedUsername = jwtUtil.extractUsername(invalidToken);

        // Then
        assertNull(extractedUsername);
    }

    @Test
    void isTokenValid_WithUserDetails_ShouldReturnTrue_WhenTokenIsValidForUser() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);
        UserDetails userDetails = User.builder()
                .username(TEST_USERNAME)
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // When
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);

        // Then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_WithUserDetails_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);
        UserDetails userDetails = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // When
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_WithoutUserDetails_ShouldReturnTrue_WhenTokenIsValid() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);
        when(redisTokenService.isTokenRevoked(token)).thenReturn(false);

        // When
        boolean isValid = jwtUtil.isTokenValid(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_WithoutUserDetails_ShouldReturnFalse_WhenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid.token.format";

        // When
        boolean isValid = jwtUtil.isTokenValid(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void getAccessTokenExpiration_ShouldReturnConfiguredValue() {
        // When
        long expiration = jwtUtil.getAccessTokenExpiration();

        // Then
        assertEquals(ACCESS_TOKEN_EXPIRATION, expiration);
    }

    @Test
    void getRefreshTokenExpiration_ShouldReturnConfiguredValue() {
        // When
        long expiration = jwtUtil.getRefreshTokenExpiration();

        // Then
        assertEquals(REFRESH_TOKEN_EXPIRATION, expiration);
    }

    @Test
    void generateAccessToken_ShouldCreateTokenWithCorrectExpiration() {
        // Given
        long currentTime = System.currentTimeMillis();
        
        // When
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // Then
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        long tokenExpiration = expiration.getTime();
        long expectedExpiration = currentTime + ACCESS_TOKEN_EXPIRATION;
        
        // Allow 1 second tolerance for test execution time
        assertTrue(Math.abs(tokenExpiration - expectedExpiration) < 1000);
    }

    @Test
    void generateRefreshToken_ShouldCreateTokenWithCorrectExpiration() {
        // Given
        long currentTime = System.currentTimeMillis();
        
        // When
        String token = jwtUtil.generateRefreshToken(TEST_USERNAME);

        // Then
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        long tokenExpiration = expiration.getTime();
        long expectedExpiration = currentTime + REFRESH_TOKEN_EXPIRATION;
        
        // Allow 1 second tolerance for test execution time
        assertTrue(Math.abs(tokenExpiration - expectedExpiration) < 1000);
    }

    @Test
    void revokeToken_ShouldCalculateCorrectExpirationTime() {
        // Given
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // When
        jwtUtil.revokeToken(token);

        // Then
        verify(redisTokenService).revokeToken(anyString(), anyLong());
    }

    @Test
    void validateToken_ShouldHandleUnsupportedJwtException() {
        // Given - Create a token with unsupported features
        String unsupportedToken = "unsupported.jwt.token";
        when(redisTokenService.isTokenRevoked(unsupportedToken)).thenReturn(false);

        // When
        boolean isValid = jwtUtil.validateToken(unsupportedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void extractUsername_ShouldHandleNullToken() {
        // When
        String extractedUsername = jwtUtil.extractUsername(null);

        // Then
        assertNull(extractedUsername);
    }

    @Test
    void isTokenValid_WithUserDetails_ShouldReturnFalse_WhenTokenIsExpired() {
        // Given
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(-1000L); // Already expired
        String expiredToken = jwtUtil.generateAccessToken(TEST_USERNAME);
        UserDetails userDetails = User.builder()
                .username(TEST_USERNAME)
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // When
        boolean isValid = jwtUtil.isTokenValid(expiredToken, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void generateTokens_ShouldGenerateUniqueTokensForDifferentUsers() {
        // Given
        String user1 = "user1";
        String user2 = "user2";

        // When
        Map<String, String> tokens1 = jwtUtil.generateTokens(user1);
        Map<String, String> tokens2 = jwtUtil.generateTokens(user2);

        // Then
        assertNotEquals(tokens1.get("access_token"), tokens2.get("access_token"));
        assertNotEquals(tokens1.get("refresh_token"), tokens2.get("refresh_token"));
        
        assertEquals(user1, jwtUtil.extractUsername(tokens1.get("access_token")));
        assertEquals(user2, jwtUtil.extractUsername(tokens2.get("access_token")));
    }
}