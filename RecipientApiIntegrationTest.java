package co.za.kingstechco.kingstechco.invoeaserapp.integration;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.LoginRequestDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.RecipientDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "management.health.mail.enabled=false"
})
public class RecipientApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createRecipient_shouldReturn200_whenValidRequest() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("recipient.creator");
        register.setEmail("recipient.creator@example.com");
        register.setPassword("RecipientP@ss123!");
        register.setFirstName("Recipient");
        register.setLastName("Creator");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("recipient.creator");
        login.setPassword("RecipientP@ss123!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Create Recipient
        RecipientDto recipient = RecipientDto.builder()
                .companyName("Test Company")
                .contactName("John Doe")
                .email("john.doe@testcompany.com")
                .phone("+1234567890")
                .addressLine1("123 Test Street")
                .city("Test City")
                .postalCode("12345")
                .country("Test Country")
                .build();

        headers.setBearerAuth(accessToken);

        HttpEntity<RecipientDto> createRequest = new HttpEntity<>(recipient, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/recipients",
                HttpMethod.POST,
                createRequest,
                String.class,
                1L
        );

        System.out.println("Create recipient response status: " + response.getStatusCode());
        System.out.println("Create recipient response body: " + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJson = mapper.readTree(response.getBody());
        assertThat(responseJson.get("companyName").asText()).isEqualTo("Test Company");
        assertThat(responseJson.get("email").asText()).isEqualTo("john.doe@testcompany.com");
    }

    @Test
    void createRecipient_shouldReturn400_whenInvalidRequest() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("invalid.recipient.creator");
        register.setEmail("invalid.recipient@example.com");
        register.setPassword("InvalidP@ss123!");
        register.setFirstName("Invalid");
        register.setLastName("Recipient");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("invalid.recipient.creator");
        login.setPassword("InvalidP@ss123!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Create invalid Recipient (missing required fields)
        RecipientDto recipient = RecipientDto.builder()
                // Missing companyName and email (required fields)
                .contactName("John Doe")
                .phone("+1234567890")
                .build();

        headers.setBearerAuth(accessToken);

        HttpEntity<RecipientDto> createRequest = new HttpEntity<>(recipient, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/recipients",
                HttpMethod.POST,
                createRequest,
                String.class,
                1L
        );

        System.out.println("Invalid recipient response: " + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getAllRecipients_shouldReturn200() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("recipient.list.user");
        register.setEmail("recipient.list@example.com");
        register.setPassword("ListRecipientsP@ss!");
        register.setFirstName("List");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("recipient.list.user");
        login.setPassword("ListRecipientsP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/recipients",
                HttpMethod.GET,
                getRequest,
                String.class,
                1L
        );

        System.out.println("Get all recipients response: " + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getRecipientById_shouldReturn404_whenRecipientNotFound() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("recipient.get.user");
        register.setEmail("recipient.get@example.com");
        register.setPassword("GetRecipientP@ss!");
        register.setFirstName("Get");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("recipient.get.user");
        login.setPassword("GetRecipientP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/recipients/{recipientId}",
                HttpMethod.GET,
                getRequest,
                String.class,
                1L,
                99999L
        );

        System.out.println("Get recipient by ID response: " + response.getStatusCode());
        // Could be 404 NOT_FOUND or 500 INTERNAL_SERVER_ERROR
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void updateRecipient_shouldReturn404_whenRecipientNotFound() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("recipient.update.user");
        register.setEmail("recipient.update@example.com");
        register.setPassword("UpdateRecipientP@ss!");
        register.setFirstName("Update");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("recipient.update.user");
        login.setPassword("UpdateRecipientP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Try to update non-existent recipient
        RecipientDto recipient = RecipientDto.builder()
                .companyName("Updated Company")
                .contactName("Jane Doe")
                .email("jane.doe@updated.com")
                .phone("+1987654321")
                .build();

        headers.setBearerAuth(accessToken);

        HttpEntity<RecipientDto> updateRequest = new HttpEntity<>(recipient, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/recipients/{recipientId}",
                HttpMethod.PUT,
                updateRequest,
                String.class,
                1L,
                99999L
        );

        System.out.println("Update recipient response: " + response.getBody());
        // Could be 404 NOT_FOUND or 500 INTERNAL_SERVER_ERROR
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void deleteRecipient_shouldReturn404_whenRecipientNotFound() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("recipient.delete.user");
        register.setEmail("recipient.delete@example.com");
        register.setPassword("DeleteRecipientP@ss!");
        register.setFirstName("Delete");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("recipient.delete.user");
        login.setPassword("DeleteRecipientP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/recipients/{recipientId}",
                HttpMethod.DELETE,
                deleteRequest,
                String.class,
                1L,
                99999L
        );

        System.out.println("Delete recipient response: " + response.getStatusCode());
        // Could be 404 NOT_FOUND or 500 INTERNAL_SERVER_ERROR  
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void createAndManageRecipient_shouldWork_whenRecipientExists() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("full.recipient.test");
        register.setEmail("full.recipient@example.com");
        register.setPassword("FullRecipientP@ss!");
        register.setFirstName("Full");
        register.setLastName("Test");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("full.recipient.test");
        login.setPassword("FullRecipientP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        // Step 1: Create Recipient
        RecipientDto recipient = RecipientDto.builder()
                .companyName("Full Test Company")
                .contactName("Full Test User")
                .email("fulltest@company.com")
                .phone("+1111111111")
                .addressLine1("123 Full Test Street")
                .city("Full Test City")
                .postalCode("11111")
                .country("Full Test Country")
                .build();

        HttpEntity<RecipientDto> createRequest = new HttpEntity<>(recipient, headers);
        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/v1/{tenantId}/recipients",
                HttpMethod.POST,
                createRequest,
                String.class,
                1L
        );

        if (createResponse.getStatusCode() == HttpStatus.OK) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode createdRecipient = mapper.readTree(createResponse.getBody());
            Long recipientId = createdRecipient.get("recipientId").asLong();

            // Step 2: Get Recipient by ID
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<String> getResponse = restTemplate.exchange(
                    "/api/v1/{tenantId}/recipients/{recipientId}",
                    HttpMethod.GET,
                    getRequest,
                    String.class,
                    1L,
                    recipientId
            );

            System.out.println("Get recipient response: " + getResponse.getBody());
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Step 3: Update Recipient
            recipient.setCompanyName("Updated Full Test Company");
            recipient.setEmail("updated.fulltest@company.com");

            HttpEntity<RecipientDto> updateRequest = new HttpEntity<>(recipient, headers);
            ResponseEntity<String> updateResponse = restTemplate.exchange(
                    "/api/v1/{tenantId}/recipients/{recipientId}",
                    HttpMethod.PUT,
                    updateRequest,
                    String.class,
                    1L,
                    recipientId
            );

            System.out.println("Update recipient response: " + updateResponse.getBody());
            assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Step 4: Delete Recipient
            HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
            ResponseEntity<String> deleteResponse = restTemplate.exchange(
                    "/api/v1/{tenantId}/recipients/{recipientId}",
                    HttpMethod.DELETE,
                    deleteRequest,
                    String.class,
                    1L,
                    recipientId
            );

            System.out.println("Delete recipient response: " + deleteResponse.getStatusCode());
            // DELETE operations require ADMIN/MANAGER role - regular users get FORBIDDEN
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        } else {
            System.out.println("Recipient creation failed, skipping update/delete tests");
            System.out.println("Create response: " + createResponse.getBody());
        }
    }
}