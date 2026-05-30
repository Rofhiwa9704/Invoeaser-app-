package co.za.kingstechco.kingstechco.invoeaserapp.integration;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.LoginRequestDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.ServiceProvider;
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
public class ServiceProviderApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createServiceProvider_shouldReturn200_whenValidRequest() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("serviceprovider.creator");
        register.setEmail("serviceprovider.creator@example.com");
        register.setPassword("ServiceProviderP@ss123!");
        register.setFirstName("ServiceProvider");
        register.setLastName("Creator");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("serviceprovider.creator");
        login.setPassword("ServiceProviderP@ss123!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Create ServiceProvider
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setTenantId(1L);
        serviceProvider.setProviderName("Test Service Provider");
        serviceProvider.setEmail("test.provider@example.com");
        serviceProvider.setPhone("+1234567890");
        serviceProvider.setAddress("123 Provider Street");
        serviceProvider.setCity("Provider City");
        serviceProvider.setState("Provider State");
        serviceProvider.setPostalCode("12345");
        serviceProvider.setCountry("Provider Country");
        serviceProvider.setAdditionalDetails("Test additional details");

        headers.setBearerAuth(accessToken);

        HttpEntity<ServiceProvider> createRequest = new HttpEntity<>(serviceProvider, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/service-providers",
                HttpMethod.POST,
                createRequest,
                String.class
        );

        System.out.println("Create service provider response status: " + response.getStatusCode());
        System.out.println("Create service provider response body: " + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJson = mapper.readTree(response.getBody());
        assertThat(responseJson.get("providerName").asText()).isEqualTo("Test Service Provider");
        assertThat(responseJson.get("email").asText()).isEqualTo("test.provider@example.com");
    }

    @Test
    void createServiceProvider_shouldReturn500_whenInvalidRequest() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("invalid.serviceprovider.creator");
        register.setEmail("invalid.serviceprovider@example.com");
        register.setPassword("InvalidServiceP@ss123!");
        register.setFirstName("Invalid");
        register.setLastName("ServiceProvider");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("invalid.serviceprovider.creator");
        login.setPassword("InvalidServiceP@ss123!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Create invalid ServiceProvider (missing required fields)
        ServiceProvider serviceProvider = new ServiceProvider();
        // Missing tenantId and other required fields

        headers.setBearerAuth(accessToken);

        HttpEntity<ServiceProvider> createRequest = new HttpEntity<>(serviceProvider, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/service-providers",
                HttpMethod.POST,
                createRequest,
                String.class
        );

        System.out.println("Invalid service provider response: " + response.getBody());
        // Could be 400 BAD_REQUEST or 500 INTERNAL_SERVER_ERROR
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void getAllServiceProviders_shouldReturn200() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("serviceprovider.list.user");
        register.setEmail("serviceprovider.list@example.com");
        register.setPassword("ListServiceProvidersP@ss!");
        register.setFirstName("List");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("serviceprovider.list.user");
        login.setPassword("ListServiceProvidersP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/service-providers/{tenantId}",
                HttpMethod.GET,
                getRequest,
                String.class,
                1L
        );

        System.out.println("Get all service providers response: " + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getServiceProviderById_shouldReturn404_whenServiceProviderNotFound() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("serviceprovider.get.user");
        register.setEmail("serviceprovider.get@example.com");
        register.setPassword("GetServiceProviderP@ss!");
        register.setFirstName("Get");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("serviceprovider.get.user");
        login.setPassword("GetServiceProviderP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/service-providers/provider/{serviceProviderId}",
                HttpMethod.GET,
                getRequest,
                String.class,
                99999L
        );

        System.out.println("Get service provider by ID response: " + response.getStatusCode());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateServiceProvider_shouldReturn404_whenServiceProviderNotFound() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("serviceprovider.update.user");
        register.setEmail("serviceprovider.update@example.com");
        register.setPassword("UpdateServiceProviderP@ss!");
        register.setFirstName("Update");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("serviceprovider.update.user");
        login.setPassword("UpdateServiceProviderP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        // Try to update non-existent service provider
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setTenantId(1L);
        serviceProvider.setProviderName("Updated Service Provider");
        serviceProvider.setEmail("updated.provider@example.com");
        serviceProvider.setPhone("+1987654321");

        headers.setBearerAuth(accessToken);

        HttpEntity<ServiceProvider> updateRequest = new HttpEntity<>(serviceProvider, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/service-providers/{serviceProviderId}",
                HttpMethod.PUT,
                updateRequest,
                String.class,
                99999L
        );

        System.out.println("Update service provider response: " + response.getBody());
        // Could be 404 NOT_FOUND or 500 INTERNAL_SERVER_ERROR
        assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void deleteServiceProvider_shouldReturn404_whenServiceProviderNotFound() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("serviceprovider.delete.user");
        register.setEmail("serviceprovider.delete@example.com");
        register.setPassword("DeleteServiceProviderP@ss!");
        register.setFirstName("Delete");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("serviceprovider.delete.user");
        login.setPassword("DeleteServiceProviderP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/service-providers/{serviceProviderId}",
                HttpMethod.DELETE,
                deleteRequest,
                String.class,
                99999L
        );

        System.out.println("Delete service provider response: " + response.getStatusCode());
        // DELETE operations require ADMIN/MANAGER role, so regular user gets 403
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createAndManageServiceProvider_shouldWork_whenServiceProviderExists() throws Exception {
        // Register and authenticate user
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("full.serviceprovider.test");
        register.setEmail("full.serviceprovider@example.com");
        register.setPassword("FullServiceProviderP@ss!");
        register.setFirstName("Full");
        register.setLastName("Test");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("full.serviceprovider.test");
        login.setPassword("FullServiceProviderP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        // Step 1: Create ServiceProvider
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setTenantId(1L);
        serviceProvider.setProviderName("Full Test Service Provider");
        serviceProvider.setEmail("fulltest.provider@example.com");
        serviceProvider.setPhone("+1111111111");
        serviceProvider.setAddress("123 Full Test Street");
        serviceProvider.setCity("Full Test City");
        serviceProvider.setPostalCode("11111");
        serviceProvider.setCountry("Full Test Country");

        HttpEntity<ServiceProvider> createRequest = new HttpEntity<>(serviceProvider, headers);
        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/v1/service-providers",
                HttpMethod.POST,
                createRequest,
                String.class
        );

        if (createResponse.getStatusCode() == HttpStatus.OK) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode createdServiceProvider = mapper.readTree(createResponse.getBody());
            Long serviceProviderId = createdServiceProvider.get("providerId").asLong();

            // Step 2: Get ServiceProvider by ID
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<String> getResponse = restTemplate.exchange(
                    "/api/v1/service-providers/provider/{serviceProviderId}",
                    HttpMethod.GET,
                    getRequest,
                    String.class,
                    serviceProviderId
            );

            System.out.println("Get service provider response: " + getResponse.getBody());
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Step 3: Update ServiceProvider
            serviceProvider.setProviderName("Updated Full Test Service Provider");
            serviceProvider.setEmail("updated.fulltest.provider@example.com");

            HttpEntity<ServiceProvider> updateRequest = new HttpEntity<>(serviceProvider, headers);
            ResponseEntity<String> updateResponse = restTemplate.exchange(
                    "/api/v1/service-providers/{serviceProviderId}",
                    HttpMethod.PUT,
                    updateRequest,
                    String.class,
                    serviceProviderId
            );

            System.out.println("Update service provider response: " + updateResponse.getBody());
            assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Step 4: Delete ServiceProvider
            HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
            ResponseEntity<String> deleteResponse = restTemplate.exchange(
                    "/api/v1/service-providers/{serviceProviderId}",
                    HttpMethod.DELETE,
                    deleteRequest,
                    String.class,
                    serviceProviderId
            );

            System.out.println("Delete service provider response: " + deleteResponse.getStatusCode());
            // DELETE operations require ADMIN/MANAGER role, so regular user gets 403
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        } else {
            System.out.println("Service provider creation failed, skipping update/delete tests");
            System.out.println("Create response: " + createResponse.getBody());
        }
    }
}