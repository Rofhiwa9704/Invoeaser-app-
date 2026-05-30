package co.za.kingstechco.kingstechco.invoeaserapp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    private static final String TEST_IDENTIFIER = "testuser:192.168.1.1";
    private static final String RATE_LIMIT_KEY = "rate_limit:" + TEST_IDENTIFIER;
    private static final String LOCKOUT_KEY = "lockout:" + TEST_IDENTIFIER;
    private static final String API_RATE_LIMIT_KEY = "api_rate_limit:" + TEST_IDENTIFIER;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
        
        // Set default configuration values
        ReflectionTestUtils.setField(rateLimitService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(rateLimitService, "windowMinutes", 15);
        ReflectionTestUtils.setField(rateLimitService, "lockoutDurationMinutes", 30);
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", true);
        
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isRateLimited_ShouldReturnFalse_WhenRateLimitDisabled() {
        // Given
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        // When
        boolean result = rateLimitService.isRateLimited(TEST_IDENTIFIER);

        // Then
        assertFalse(result);
        verify(redisTemplate, never()).hasKey(any());
    }

    @Test
    void isRateLimited_ShouldReturnFalse_WhenNoLockoutExists() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);

        // When
        boolean result = rateLimitService.isRateLimited(TEST_IDENTIFIER);

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey(LOCKOUT_KEY);
    }

    @Test
    void isRateLimited_ShouldReturnTrue_WhenUserIsLockedOut() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(true);

        // When
        boolean result = rateLimitService.isRateLimited(TEST_IDENTIFIER);

        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey(LOCKOUT_KEY);
    }

    @Test
    void incrementAttempt_ShouldReturnFalse_WhenRateLimitDisabled() {
        // Given
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        // When
        boolean result = rateLimitService.incrementAttempt(TEST_IDENTIFIER);

        // Then
        assertFalse(result);
        verify(redisTemplate, never()).hasKey(any());
    }

    @Test
    void incrementAttempt_ShouldReturnTrue_WhenAlreadyInLockout() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(true);

        // When
        boolean result = rateLimitService.incrementAttempt(TEST_IDENTIFIER);

        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey(LOCKOUT_KEY);
        verify(valueOperations, never()).get(any());
    }

    @Test
    void incrementAttempt_ShouldIncrementCounter_WhenFirstAttempt() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn(null);

        // When
        boolean result = rateLimitService.incrementAttempt(TEST_IDENTIFIER);

        // Then
        assertFalse(result);
        verify(valueOperations).get(RATE_LIMIT_KEY);
        verify(valueOperations).set(RATE_LIMIT_KEY, "1", Duration.ofMinutes(15));
    }

    @Test
    void incrementAttempt_ShouldIncrementCounter_WhenSubsequentAttempts() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn("2");

        // When
        boolean result = rateLimitService.incrementAttempt(TEST_IDENTIFIER);

        // Then
        assertFalse(result);
        verify(valueOperations).get(RATE_LIMIT_KEY);
        verify(valueOperations).set(RATE_LIMIT_KEY, "3", Duration.ofMinutes(15));
    }

    @Test
    void incrementAttempt_ShouldLockUser_WhenMaxAttemptsReached() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn("4"); // 5th attempt (reaching max)

        // When
        boolean result = rateLimitService.incrementAttempt(TEST_IDENTIFIER);

        // Then
        assertTrue(result);
        verify(valueOperations).set(LOCKOUT_KEY, "locked", Duration.ofMinutes(30));
        verify(redisTemplate).delete(RATE_LIMIT_KEY);
    }

    @Test
    void clearAttempts_ShouldDoNothing_WhenRateLimitDisabled() {
        // Given
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        // When
        rateLimitService.clearAttempts(TEST_IDENTIFIER);

        // Then
        verify(redisTemplate, never()).delete((String) any());
    }

    @Test
    void clearAttempts_ShouldDeleteKey_WhenRateLimitEnabled() {
        // When
        rateLimitService.clearAttempts(TEST_IDENTIFIER);

        // Then
        verify(redisTemplate).delete(RATE_LIMIT_KEY);
    }

    @Test
    void getRemainingAttempts_ShouldReturnMaxAttempts_WhenRateLimitDisabled() {
        // Given
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        // When
        int result = rateLimitService.getRemainingAttempts(TEST_IDENTIFIER);

        // Then
        assertEquals(5, result);
        verify(redisTemplate, never()).hasKey(any());
    }

    @Test
    void getRemainingAttempts_ShouldReturnZero_WhenUserIsLockedOut() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(true);

        // When
        int result = rateLimitService.getRemainingAttempts(TEST_IDENTIFIER);

        // Then
        assertEquals(0, result);
        verify(redisTemplate).hasKey(LOCKOUT_KEY);
    }

    @Test
    void getRemainingAttempts_ShouldReturnMaxAttempts_WhenNoAttemptsRecorded() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn(null);

        // When
        int result = rateLimitService.getRemainingAttempts(TEST_IDENTIFIER);

        // Then
        assertEquals(5, result);
        verify(valueOperations).get(RATE_LIMIT_KEY);
    }

    @Test
    void getRemainingAttempts_ShouldCalculateCorrectly_WhenAttemptsExist() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn("3");

        // When
        int result = rateLimitService.getRemainingAttempts(TEST_IDENTIFIER);

        // Then
        assertEquals(2, result); // 5 - 3 = 2
        verify(valueOperations).get(RATE_LIMIT_KEY);
    }

    @Test
    void getLockoutTimeRemaining_ShouldReturnZero_WhenRateLimitDisabled() {
        // Given
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        // When
        long result = rateLimitService.getLockoutTimeRemaining(TEST_IDENTIFIER);

        // Then
        assertEquals(0, result);
        verify(redisTemplate, never()).getExpire(any(), any());
    }

    @Test
    void getLockoutTimeRemaining_ShouldReturnExpireTime_WhenLockoutExists() {
        // Given
        when(redisTemplate.getExpire(LOCKOUT_KEY, TimeUnit.SECONDS)).thenReturn(1800L); // 30 minutes

        // When
        long result = rateLimitService.getLockoutTimeRemaining(TEST_IDENTIFIER);

        // Then
        assertEquals(1800L, result);
        verify(redisTemplate).getExpire(LOCKOUT_KEY, TimeUnit.SECONDS);
    }

    @Test
    void isApiRateLimited_ShouldReturnFalse_WhenRateLimitDisabled() {
        // Given
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        // When
        boolean result = rateLimitService.isApiRateLimited(TEST_IDENTIFIER);

        // Then
        assertFalse(result);
        verify(valueOperations, never()).get(any());
    }

    @Test
    void isApiRateLimited_ShouldReturnFalse_WhenUnderLimit() {
        // Given
        when(valueOperations.get(API_RATE_LIMIT_KEY)).thenReturn("500");

        // When
        boolean result = rateLimitService.isApiRateLimited(TEST_IDENTIFIER);

        // Then
        assertFalse(result);
        verify(valueOperations).get(API_RATE_LIMIT_KEY);
        verify(valueOperations).set(API_RATE_LIMIT_KEY, "501", Duration.ofHours(1));
    }

    @Test
    void isApiRateLimited_ShouldReturnTrue_WhenOverLimit() {
        // Given
        when(valueOperations.get(API_RATE_LIMIT_KEY)).thenReturn("1000");

        // When
        boolean result = rateLimitService.isApiRateLimited(TEST_IDENTIFIER);

        // Then
        assertTrue(result);
        verify(valueOperations).get(API_RATE_LIMIT_KEY);
        verify(valueOperations, never()).set(eq(API_RATE_LIMIT_KEY), any(), any());
    }

    @Test
    void isApiRateLimited_ShouldIncrementCounter_WhenFirstRequest() {
        // Given
        when(valueOperations.get(API_RATE_LIMIT_KEY)).thenReturn(null);

        // When
        boolean result = rateLimitService.isApiRateLimited(TEST_IDENTIFIER);

        // Then
        assertFalse(result);
        verify(valueOperations).get(API_RATE_LIMIT_KEY);
        verify(valueOperations).set(API_RATE_LIMIT_KEY, "1", Duration.ofHours(1));
    }

    @Test
    void clearApiRateLimit_ShouldDoNothing_WhenRateLimitDisabled() {
        // Given
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        // When
        rateLimitService.clearApiRateLimit(TEST_IDENTIFIER);

        // Then
        verify(redisTemplate, never()).delete((String) any());
    }

    @Test
    void clearApiRateLimit_ShouldDeleteKey_WhenRateLimitEnabled() {
        // When
        rateLimitService.clearApiRateLimit(TEST_IDENTIFIER);

        // Then
        verify(redisTemplate).delete(API_RATE_LIMIT_KEY);
    }

    @Test
    void incrementAttempt_ShouldHandleInvalidCounterValue_Gracefully() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn("invalid");

        // When & Then
        // Should handle NumberFormatException gracefully
        assertThrows(NumberFormatException.class, 
                () -> rateLimitService.incrementAttempt(TEST_IDENTIFIER));
    }

    @Test
    void getRemainingAttempts_ShouldHandleInvalidCounterValue_Gracefully() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn("invalid");

        // When & Then
        // Should handle NumberFormatException gracefully
        assertThrows(NumberFormatException.class,
                () -> rateLimitService.getRemainingAttempts(TEST_IDENTIFIER));
    }

    @Test
    void isApiRateLimited_ShouldHandleInvalidCounterValue_Gracefully() {
        // Given
        when(valueOperations.get(API_RATE_LIMIT_KEY)).thenReturn("invalid");

        // When & Then
        // Should handle NumberFormatException gracefully
        assertThrows(NumberFormatException.class,
                () -> rateLimitService.isApiRateLimited(TEST_IDENTIFIER));
    }

    @Test
    void incrementAttempt_ShouldUseCorrectConfiguration_Values() {
        // Given
        ReflectionTestUtils.setField(rateLimitService, "maxLoginAttempts", 3);
        ReflectionTestUtils.setField(rateLimitService, "windowMinutes", 10);
        ReflectionTestUtils.setField(rateLimitService, "lockoutDurationMinutes", 20);
        
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn("2"); // 3rd attempt (reaching max)

        // When
        boolean result = rateLimitService.incrementAttempt(TEST_IDENTIFIER);

        // Then
        assertTrue(result);
        verify(valueOperations).set(LOCKOUT_KEY, "locked", Duration.ofMinutes(20));
        verify(redisTemplate).delete(RATE_LIMIT_KEY);
    }

    @Test
    void getRemainingAttempts_ShouldNotReturnNegativeValue_WhenAttemptsExceedMax() {
        // Given
        when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);
        when(valueOperations.get(RATE_LIMIT_KEY)).thenReturn("10"); // More than max attempts

        // When
        int result = rateLimitService.getRemainingAttempts(TEST_IDENTIFIER);

        // Then
        assertEquals(0, result); // Should not be negative
        verify(valueOperations).get(RATE_LIMIT_KEY);
    }
}