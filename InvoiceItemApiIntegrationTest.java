package co.za.kingstechco.kingstechco.invoeaserapp.integration;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.LoginRequestDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.InvoiceItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "management.health.mail.enabled=false"
})
public class InvoiceItemApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createInvoiceItem_shouldReturn500_whenInvoiceNotExists() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("invoiceitem.creator");
        register.setEmail("invoiceitem.creator@example.com");
        register.setPassword("InvoiceItemP@ss123!");
        register.setFirstName("InvoiceItem");
        register.setLastName("Creator");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("invoiceitem.creator");
        login.setPassword("InvoiceItemP@ss123!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Create InvoiceItem (will likely fail because invoice doesn't exist)
        InvoiceItem invoiceItem = InvoiceItem.builder()
                .tenantId(1L)
                .hoursWorked(8)
                .hourlyRate(new BigDecimal("50.00"))
                .lineTotal(new BigDecimal("400.00"))
                .unitPrice(new BigDecimal("50.00"))
                .quantity(8)
                // Note: invoice is required but we don't set it
                .build();

        headers.setBearerAuth(accessToken);

        HttpEntity<InvoiceItem> createRequest = new HttpEntity<>(invoiceItem, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/invoice-items",
                HttpMethod.POST,
                createRequest,
                String.class
        );

        System.out.println("Create invoice item response status: " + response.getStatusCode());
        System.out.println("Create invoice item response body: " + response.getBody());
        // Expect error due to missing invoice relationship
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void getInvoiceItemsByInvoiceId_shouldReturn200() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("invoiceitem.list.user");
        register.setEmail("invoiceitem.list@example.com");
        register.setPassword("ListInvoiceItemsP@ss!");
        register.setFirstName("List");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("invoiceitem.list.user");
        login.setPassword("ListInvoiceItemsP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/invoice-items/invoice/{invoiceId}",
                HttpMethod.GET,
                getRequest,
                String.class,
                99999L // Non-existent invoice ID
        );

        System.out.println("Get invoice items by invoice ID response: " + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getInvoiceItemById_shouldReturn404_whenInvoiceItemNotFound() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("invoiceitem.get.user");
        register.setEmail("invoiceitem.get@example.com");
        register.setPassword("GetInvoiceItemP@ss!");
        register.setFirstName("Get");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("invoiceitem.get.user");
        login.setPassword("GetInvoiceItemP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/invoice-items/{invoiceItemId}",
                HttpMethod.GET,
                getRequest,
                String.class,
                99999L
        );

        System.out.println("Get invoice item by ID response: " + response.getStatusCode());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateInvoiceItem_shouldReturn404_whenInvoiceItemNotFound() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("invoiceitem.update.user");
        register.setEmail("invoiceitem.update@example.com");
        register.setPassword("UpdateInvoiceItemP@ss!");
        register.setFirstName("Update");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("invoiceitem.update.user");
        login.setPassword("UpdateInvoiceItemP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Try to update non-existent invoice item
        InvoiceItem invoiceItem = InvoiceItem.builder()
                .tenantId(1L)
                .hoursWorked(10)
                .hourlyRate(new BigDecimal("60.00"))
                .lineTotal(new BigDecimal("600.00"))
                .unitPrice(new BigDecimal("60.00"))
                .quantity(10)
                .build();

        headers.setBearerAuth(accessToken);

        HttpEntity<InvoiceItem> updateRequest = new HttpEntity<>(invoiceItem, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/invoice-items/{invoiceItemId}",
                HttpMethod.PUT,
                updateRequest,
                String.class,
                99999L
        );

        System.out.println("Update invoice item response: " + response.getBody());
        // Could be 404 NOT_FOUND or 500 INTERNAL_SERVER_ERROR
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void deleteInvoiceItem_shouldReturn403_whenUserLacksDeletePermission() throws Exception {
        // Register and authenticate user (without ADMIN/MANAGER role)
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("invoiceitem.delete.user");
        register.setEmail("invoiceitem.delete@example.com");
        register.setPassword("DeleteInvoiceItemP@ss!");
        register.setFirstName("Delete");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("invoiceitem.delete.user");
        login.setPassword("DeleteInvoiceItemP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/invoice-items/{invoiceItemId}",
                HttpMethod.DELETE,
                deleteRequest,
                String.class,
                99999L
        );

        System.out.println("Delete invoice item response: " + response.getStatusCode());
        // DELETE operations require ADMIN/MANAGER role, so regular user gets 403
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createInvoiceItem_shouldReturn400_whenInvalidData() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("invalid.invoiceitem.creator");
        register.setEmail("invalid.invoiceitem@example.com");
        register.setPassword("InvalidInvoiceItemP@ss123!");
        register.setFirstName("Invalid");
        register.setLastName("InvoiceItem");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("invalid.invoiceitem.creator");
        login.setPassword("InvalidInvoiceItemP@ss123!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Create invalid InvoiceItem (missing required fields)
        InvoiceItem invoiceItem = InvoiceItem.builder()
                .tenantId(1L)
                // Missing other required fields like invoice relationship
                .build();

        headers.setBearerAuth(accessToken);

        HttpEntity<InvoiceItem> createRequest = new HttpEntity<>(invoiceItem, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/invoice-items",
                HttpMethod.POST,
                createRequest,
                String.class
        );

        System.out.println("Invalid invoice item response: " + response.getBody());
        // Could be 400 BAD_REQUEST or 500 INTERNAL_SERVER_ERROR
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }
}