package co.za.kingstechco.kingstechco.invoeaserapp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    private static final String TEST_SECRET_KEY = "testSecretKey123";
    private static final String TEST_PLAIN_TEXT = "Hello, World!";

    @BeforeEach
    void setUp() {
        encryptionUtil = new EncryptionUtil();
        ReflectionTestUtils.setField(encryptionUtil, "secretKeyString", TEST_SECRET_KEY);
    }

    @Test
    void encrypt_ShouldReturnEncryptedString_WhenGivenValidPlainText() {
        // When
        String encrypted = encryptionUtil.encrypt(TEST_PLAIN_TEXT);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(TEST_PLAIN_TEXT, encrypted);
        assertFalse(encrypted.isEmpty());
        // Encrypted string should be Base64 encoded
        assertTrue(encrypted.matches("^[A-Za-z0-9+/]*={0,2}$"));
    }

    @Test
    void decrypt_ShouldReturnOriginalPlainText_WhenGivenValidEncryptedText() {
        // Given
        String encrypted = encryptionUtil.encrypt(TEST_PLAIN_TEXT);

        // When
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertEquals(TEST_PLAIN_TEXT, decrypted);
    }

    @Test
    void encryptDecrypt_ShouldBeSymmetric_ForVariousInputs() {
        // Given
        String[] testInputs = {
            "Simple text",
            "Text with numbers 123456",
            "Special chars: !@#$%^&*()",
            "Unicode: 你好世界",
            "Empty string test: ",
            "Long text: " + "a".repeat(1000)
        };

        for (String input : testInputs) {
            // When
            String encrypted = encryptionUtil.encrypt(input);
            String decrypted = encryptionUtil.decrypt(encrypted);

            // Then
            assertEquals(input, decrypted, "Failed for input: " + input);
            assertNotEquals(input, encrypted, "Encryption should change the input: " + input);
        }
    }

    @Test
    void encrypt_ShouldReturnNull_WhenInputIsNull() {
        // When
        String encrypted = encryptionUtil.encrypt(null);

        // Then
        assertNull(encrypted);
    }

    @Test
    void encrypt_ShouldReturnEmptyString_WhenInputIsEmpty() {
        // When
        String encrypted = encryptionUtil.encrypt("");

        // Then
        assertEquals("", encrypted);
    }

    @Test
    void decrypt_ShouldReturnNull_WhenInputIsNull() {
        // When
        String decrypted = encryptionUtil.decrypt(null);

        // Then
        assertNull(decrypted);
    }

    @Test
    void decrypt_ShouldReturnEmptyString_WhenInputIsEmpty() {
        // When
        String decrypted = encryptionUtil.decrypt("");

        // Then
        assertEquals("", decrypted);
    }

    @Test
    void encrypt_ShouldProduceDifferentResults_ForDifferentInputs() {
        // Given
        String input1 = "First input";
        String input2 = "Second input";

        // When
        String encrypted1 = encryptionUtil.encrypt(input1);
        String encrypted2 = encryptionUtil.encrypt(input2);

        // Then
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void encrypt_ShouldProduceSameResult_ForSameInput() {
        // Note: AES in ECB mode will produce same output for same input
        // Given
        String input = "Consistent input";

        // When
        String encrypted1 = encryptionUtil.encrypt(input);
        String encrypted2 = encryptionUtil.encrypt(input);

        // Then
        assertEquals(encrypted1, encrypted2);
    }

    @Test
    void encryptionUtil_ShouldHandleDifferentKeyLengths() {
        // Test with key shorter than 16 characters
        ReflectionTestUtils.setField(encryptionUtil, "secretKeyString", "short");
        String encrypted1 = encryptionUtil.encrypt(TEST_PLAIN_TEXT);
        String decrypted1 = encryptionUtil.decrypt(encrypted1);
        assertEquals(TEST_PLAIN_TEXT, decrypted1);

        // Test with key exactly 16 characters
        ReflectionTestUtils.setField(encryptionUtil, "secretKeyString", "exactly16charkey");
        String encrypted2 = encryptionUtil.encrypt(TEST_PLAIN_TEXT);
        String decrypted2 = encryptionUtil.decrypt(encrypted2);
        assertEquals(TEST_PLAIN_TEXT, decrypted2);

        // Test with key longer than 16 characters
        ReflectionTestUtils.setField(encryptionUtil, "secretKeyString", "thisIsAVeryLongKeyThatExceeds16Characters");
        String encrypted3 = encryptionUtil.encrypt(TEST_PLAIN_TEXT);
        String decrypted3 = encryptionUtil.decrypt(encrypted3);
        assertEquals(TEST_PLAIN_TEXT, decrypted3);

        // All encrypted values should be different due to different keys
        assertNotEquals(encrypted1, encrypted2);
        assertNotEquals(encrypted2, encrypted3);
        assertNotEquals(encrypted1, encrypted3);
    }

    @Test
    void decrypt_ShouldThrowException_WhenGivenInvalidEncryptedText() {
        // Given
        String invalidEncryptedText = "This is not a valid encrypted string";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> encryptionUtil.decrypt(invalidEncryptedText));
        
        assertTrue(exception.getMessage().contains("Error decrypting data"));
    }

    @Test
    void decrypt_ShouldThrowException_WhenGivenMalformedBase64() {
        // Given
        String malformedBase64 = "Invalid@Base64!String";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> encryptionUtil.decrypt(malformedBase64));
        
        assertTrue(exception.getMessage().contains("Error decrypting data"));
    }

    @Test
    void encryptSensitiveData_ShouldNotContainOriginalData() {
        // Given
        String sensitiveEmail = "user@example.com";
        String sensitivePassword = "mySecretPassword123";
        String sensitiveCardNumber = "4111-1111-1111-1111";

        // When
        String encryptedEmail = encryptionUtil.encrypt(sensitiveEmail);
        String encryptedPassword = encryptionUtil.encrypt(sensitivePassword);
        String encryptedCardNumber = encryptionUtil.encrypt(sensitiveCardNumber);

        // Then
        assertFalse(encryptedEmail.contains("@example.com"));
        assertFalse(encryptedEmail.contains("user"));
        assertFalse(encryptedPassword.contains("mySecret"));
        assertFalse(encryptedPassword.contains("Password"));
        assertFalse(encryptedCardNumber.contains("4111"));
        assertFalse(encryptedCardNumber.contains("-"));
    }

    @Test
    void encrypt_ShouldHandleUnicodeCharacters() {
        // Given
        String unicodeText = "测试文本 🎉 emoji and special chars: àáâãäå";

        // When
        String encrypted = encryptionUtil.encrypt(unicodeText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertEquals(unicodeText, decrypted);
        assertNotEquals(unicodeText, encrypted);
    }

    @Test
    void encrypt_ShouldHandleLargeText() {
        // Given
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeText.append("Large text content ").append(i).append(" ");
        }
        String input = largeText.toString();

        // When
        String encrypted = encryptionUtil.encrypt(input);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertEquals(input, decrypted);
        assertFalse(encrypted.isEmpty());
    }

    @Test
    void encrypt_ShouldHandleWhitespaceAndSpecialCharacters() {
        // Given
        String specialText = "   \n\t\r  Special whitespace and newlines  \n\t\r   ";

        // When
        String encrypted = encryptionUtil.encrypt(specialText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertEquals(specialText, decrypted);
    }

    @Test
    void encryption_ShouldBeSecure_NotReversibleWithoutKey() {
        // Given
        String sensitiveData = "top-secret-information";
        String encrypted = encryptionUtil.encrypt(sensitiveData);

        // Create new instance with different key
        EncryptionUtil differentKeyUtil = new EncryptionUtil();
        ReflectionTestUtils.setField(differentKeyUtil, "secretKeyString", "differentKey123");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> differentKeyUtil.decrypt(encrypted));
        
        assertTrue(exception.getMessage().contains("Error decrypting data"));
    }

    @Test
    void getValidKey_ShouldHandleEdgeCases() {
        // Test edge cases by using reflection to test the private method indirectly
        
        // Test with exactly 16 character key
        ReflectionTestUtils.setField(encryptionUtil, "secretKeyString", "exactly16charkey");
        String result1 = encryptionUtil.encrypt("test");
        assertNotNull(result1);

        // Test with empty key (should be padded)
        ReflectionTestUtils.setField(encryptionUtil, "secretKeyString", "");
        String result2 = encryptionUtil.encrypt("test");
        assertNotNull(result2);

        // Test with single character key (should be padded)
        ReflectionTestUtils.setField(encryptionUtil, "secretKeyString", "a");
        String result3 = encryptionUtil.encrypt("test");
        assertNotNull(result3);
    }
}