package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTokenService redisTokenService;

    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    private static final String REVOKED_KEY = "revoked_token:" + TEST_TOKEN;
    private static final String BLACKLIST_KEY = "blacklisted_token:" + TEST_TOKEN;
    private static final long EXPIRATION_TIME = 3600L; // 1 hour in seconds

    @BeforeEach
    void setUp() {
        redisTokenService = new RedisTokenService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void revokeToken_ShouldStoreRevokedToken_WithExpirationTime() {
        // When
        redisTokenService.revokeToken(TEST_TOKEN, EXPIRATION_TIME);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(REVOKED_KEY, "revoked", EXPIRATION_TIME, TimeUnit.SECONDS);
    }

    @Test
    void revokeToken_ShouldHandleShortExpirationTime_Correctly() {
        // Given
        long shortExpiration = 60L; // 1 minute

        // When
        redisTokenService.revokeToken(TEST_TOKEN, shortExpiration);

        // Then
        verify(valueOperations).set(REVOKED_KEY, "revoked", shortExpiration, TimeUnit.SECONDS);
    }

    @Test
    void revokeToken_ShouldHandleLongExpirationTime_Correctly() {
        // Given
        long longExpiration = 86400L; // 24 hours

        // When
        redisTokenService.revokeToken(TEST_TOKEN, longExpiration);

        // Then
        verify(valueOperations).set(REVOKED_KEY, "revoked", longExpiration, TimeUnit.SECONDS);
    }

    @Test
    void isTokenRevoked_ShouldReturnTrue_WhenTokenIsRevoked() {
        // Given
        when(valueOperations.get(REVOKED_KEY)).thenReturn("revoked");

        // When
        boolean result = redisTokenService.isTokenRevoked(TEST_TOKEN);

        // Then
        assertTrue(result);
        verify(valueOperations).get(REVOKED_KEY);
    }

    @Test
    void isTokenRevoked_ShouldReturnFalse_WhenTokenIsNotRevoked() {
        // Given
        when(valueOperations.get(REVOKED_KEY)).thenReturn(null);

        // When
        boolean result = redisTokenService.isTokenRevoked(TEST_TOKEN);

        // Then
        assertFalse(result);
        verify(valueOperations).get(REVOKED_KEY);
    }

    @Test
    void isTokenRevoked_ShouldReturnFalse_WhenTokenHasDifferentValue() {
        // Given
        when(valueOperations.get(REVOKED_KEY)).thenReturn("active");

        // When
        boolean result = redisTokenService.isTokenRevoked(TEST_TOKEN);

        // Then
        assertFalse(result);
        verify(valueOperations).get(REVOKED_KEY);
    }

    @Test
    void blacklistToken_ShouldStoreBlacklistedToken_WithDefaultExpiration() {
        // When
        redisTokenService.blacklistToken(TEST_TOKEN);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(BLACKLIST_KEY, "blacklisted", 24, TimeUnit.HOURS);
    }

    @Test
    void isTokenBlacklisted_ShouldReturnTrue_WhenTokenIsBlacklisted() {
        // Given
        when(valueOperations.get(BLACKLIST_KEY)).thenReturn("blacklisted");

        // When
        boolean result = redisTokenService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertTrue(result);
        verify(valueOperations).get(BLACKLIST_KEY);
    }

    @Test
    void isTokenBlacklisted_ShouldReturnFalse_WhenTokenIsNotBlacklisted() {
        // Given
        when(valueOperations.get(BLACKLIST_KEY)).thenReturn(null);

        // When
        boolean result = redisTokenService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertFalse(result);
        verify(valueOperations).get(BLACKLIST_KEY);
    }

    @Test
    void isTokenBlacklisted_ShouldReturnFalse_WhenTokenHasDifferentValue() {
        // Given
        when(valueOperations.get(BLACKLIST_KEY)).thenReturn("active");

        // When
        boolean result = redisTokenService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertFalse(result);
        verify(valueOperations).get(BLACKLIST_KEY);
    }

    @Test
    void revokeToken_ShouldHandleNullToken_Gracefully() {
        // Given
        String nullToken = null;
        String expectedKey = "revoked_token:" + nullToken;

        // When
        redisTokenService.revokeToken(nullToken, EXPIRATION_TIME);

        // Then
        verify(valueOperations).set(expectedKey, "revoked", EXPIRATION_TIME, TimeUnit.SECONDS);
    }

    @Test
    void isTokenRevoked_ShouldHandleNullToken_Gracefully() {
        // Given
        String nullToken = null;
        String expectedKey = "revoked_token:" + nullToken;
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        boolean result = redisTokenService.isTokenRevoked(nullToken);

        // Then
        assertFalse(result);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    void blacklistToken_ShouldHandleNullToken_Gracefully() {
        // Given
        String nullToken = null;
        String expectedKey = "blacklisted_token:" + nullToken;

        // When
        redisTokenService.blacklistToken(nullToken);

        // Then
        verify(valueOperations).set(expectedKey, "blacklisted", 24, TimeUnit.HOURS);
    }

    @Test
    void isTokenBlacklisted_ShouldHandleNullToken_Gracefully() {
        // Given
        String nullToken = null;
        String expectedKey = "blacklisted_token:" + nullToken;
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        boolean result = redisTokenService.isTokenBlacklisted(nullToken);

        // Then
        assertFalse(result);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    void revokeToken_ShouldHandleEmptyToken_Correctly() {
        // Given
        String emptyToken = "";
        String expectedKey = "revoked_token:";

        // When
        redisTokenService.revokeToken(emptyToken, EXPIRATION_TIME);

        // Then
        verify(valueOperations).set(expectedKey, "revoked", EXPIRATION_TIME, TimeUnit.SECONDS);
    }

    @Test
    void isTokenRevoked_ShouldHandleEmptyToken_Correctly() {
        // Given
        String emptyToken = "";
        String expectedKey = "revoked_token:";
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        boolean result = redisTokenService.isTokenRevoked(emptyToken);

        // Then
        assertFalse(result);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    void revokeToken_ShouldHandleZeroExpirationTime_Correctly() {
        // Given
        long zeroExpiration = 0L;

        // When
        redisTokenService.revokeToken(TEST_TOKEN, zeroExpiration);

        // Then
        verify(valueOperations).set(REVOKED_KEY, "revoked", zeroExpiration, TimeUnit.SECONDS);
    }

    @Test
    void revokeToken_ShouldHandleNegativeExpirationTime_Correctly() {
        // Given
        long negativeExpiration = -100L;

        // When
        redisTokenService.revokeToken(TEST_TOKEN, negativeExpiration);

        // Then
        verify(valueOperations).set(REVOKED_KEY, "revoked", negativeExpiration, TimeUnit.SECONDS);
    }

    @Test
    void isTokenRevoked_ShouldHandleRedisException_Gracefully() {
        // Given
        when(valueOperations.get(REVOKED_KEY)).thenThrow(new RuntimeException("Redis connection error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> redisTokenService.isTokenRevoked(TEST_TOKEN));
        
        assertEquals("Redis connection error", exception.getMessage());
        verify(valueOperations).get(REVOKED_KEY);
    }

    @Test
    void isTokenBlacklisted_ShouldHandleRedisException_Gracefully() {
        // Given
        when(valueOperations.get(BLACKLIST_KEY)).thenThrow(new RuntimeException("Redis connection error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> redisTokenService.isTokenBlacklisted(TEST_TOKEN));
        
        assertEquals("Redis connection error", exception.getMessage());
        verify(valueOperations).get(BLACKLIST_KEY);
    }

    @Test
    void revokeToken_ShouldHandleRedisException_Gracefully() {
        // Given
        doThrow(new RuntimeException("Redis connection error"))
                .when(valueOperations).set(any(), any(), anyLong(), any(TimeUnit.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> redisTokenService.revokeToken(TEST_TOKEN, EXPIRATION_TIME));
        
        assertEquals("Redis connection error", exception.getMessage());
    }

    @Test
    void blacklistToken_ShouldHandleRedisException_Gracefully() {
        // Given
        doThrow(new RuntimeException("Redis connection error"))
                .when(valueOperations).set(any(), any(), anyLong(), any(TimeUnit.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> redisTokenService.blacklistToken(TEST_TOKEN));
        
        assertEquals("Redis connection error", exception.getMessage());
    }

    @Test
    void revokeToken_ShouldCreateCorrectRedisKey_ForComplexToken() {
        // Given
        String complexToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String expectedKey = "revoked_token:" + complexToken;

        // When
        redisTokenService.revokeToken(complexToken, EXPIRATION_TIME);

        // Then
        verify(valueOperations).set(expectedKey, "revoked", EXPIRATION_TIME, TimeUnit.SECONDS);
    }

    @Test
    void blacklistToken_ShouldCreateCorrectRedisKey_ForComplexToken() {
        // Given
        String complexToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String expectedKey = "blacklisted_token:" + complexToken;

        // When
        redisTokenService.blacklistToken(complexToken);

        // Then
        verify(valueOperations).set(expectedKey, "blacklisted", 24, TimeUnit.HOURS);
    }

    @Test
    void isTokenRevoked_ShouldHandleEmptyStringFromRedis_Correctly() {
        // Given
        when(valueOperations.get(REVOKED_KEY)).thenReturn("");

        // When
        boolean result = redisTokenService.isTokenRevoked(TEST_TOKEN);

        // Then
        assertFalse(result); // Empty string is not "revoked"
        verify(valueOperations).get(REVOKED_KEY);
    }

    @Test
    void isTokenBlacklisted_ShouldHandleEmptyStringFromRedis_Correctly() {
        // Given
        when(valueOperations.get(BLACKLIST_KEY)).thenReturn("");

        // When
        boolean result = redisTokenService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertFalse(result); // Empty string is not "blacklisted"
        verify(valueOperations).get(BLACKLIST_KEY);
    }

    @Test
    void constructor_ShouldInitializeCorrectly_WithRedisTemplate() {
        // When
        RedisTokenService service = new RedisTokenService(redisTemplate);

        // Then
        assertNotNull(service);
        // Verify that the service can be used
        service.blacklistToken(TEST_TOKEN);
        verify(redisTemplate).opsForValue();
    }
}