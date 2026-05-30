package co.za.kingstechco.kingstechco.invoeaserapp.integration;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.LoginRequestDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
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
public class EmailSendAuditApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getEmailAudits_shouldReturn403_whenNotAdmin() throws Exception {
        // Register and authenticate user (without admin role)
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("emailaudit.user");
        register.setEmail("emailaudit.user@example.com");
        register.setPassword("EmailAuditP@ss123!");
        register.setFirstName("EmailAudit");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("emailaudit.user");
        login.setPassword("EmailAuditP@ss123!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits",
                HttpMethod.GET,
                getRequest,
                String.class,
                1L
        );

        System.out.println("Get email audits response status: " + response.getStatusCode());
        System.out.println("Get email audits response body: " + response.getBody());
        // Should get 403 FORBIDDEN due to lack of admin role
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getEmailAuditsWithFilters_shouldReturn403_whenNotAdmin() throws Exception {
        // Register and authenticate user (without admin role)
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("emailaudit.filter.user");
        register.setEmail("emailaudit.filter@example.com");
        register.setPassword("EmailAuditFilterP@ss!");
        register.setFirstName("EmailAuditFilter");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("emailaudit.filter.user");
        login.setPassword("EmailAuditFilterP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits?status=SENT&from=2024-01-01&to=2024-12-31",
                HttpMethod.GET,
                getRequest,
                String.class,
                1L
        );

        System.out.println("Get email audits with filters response: " + response.getStatusCode());
        // Should get 403 FORBIDDEN due to lack of admin role
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void exportEmailAuditsAsCsv_shouldReturn403_whenNotAdmin() throws Exception {
        // Register and authenticate user (without admin role)
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("emailaudit.export.user");
        register.setEmail("emailaudit.export@example.com");
        register.setPassword("EmailAuditExportP@ss!");
        register.setFirstName("EmailAuditExport");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("emailaudit.export.user");
        login.setPassword("EmailAuditExportP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits/export",
                HttpMethod.GET,
                getRequest,
                String.class,
                1L
        );

        System.out.println("Export email audits response: " + response.getStatusCode());
        // Should get 403 FORBIDDEN due to lack of admin role
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void retryFailedEmails_shouldReturn403_whenNotAdmin() throws Exception {
        // Register and authenticate user (without admin role)
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("emailaudit.retry.user");
        register.setEmail("emailaudit.retry@example.com");
        register.setPassword("EmailAuditRetryP@ss!");
        register.setFirstName("EmailAuditRetry");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("emailaudit.retry.user");
        login.setPassword("EmailAuditRetryP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> putRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits/retry",
                HttpMethod.PUT,
                putRequest,
                String.class,
                1L
        );

        System.out.println("Retry failed emails response: " + response.getStatusCode());
        // Should get 403 FORBIDDEN due to lack of admin role
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getAuditSummary_shouldReturn403_whenNotAdmin() throws Exception {
        // Register and authenticate user (without admin role)
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("emailaudit.summary.user");
        register.setEmail("emailaudit.summary@example.com");
        register.setPassword("EmailAuditSummaryP@ss!");
        register.setFirstName("EmailAuditSummary");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("emailaudit.summary.user");
        login.setPassword("EmailAuditSummaryP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits/summary",
                HttpMethod.GET,
                getRequest,
                String.class,
                1L
        );

        System.out.println("Get audit summary response: " + response.getStatusCode());
        // Should get 403 FORBIDDEN due to lack of admin role
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void emailAuditEndpoints_shouldAllReturnConsistentForbidden() throws Exception {
        // Register and authenticate user (without admin role)
        UserRegistrationDto register = new UserRegistrationDto();
        register.setUsername("emailaudit.comprehensive.user");
        register.setEmail("emailaudit.comprehensive@example.com");
        register.setPassword("EmailAuditComprehensiveP@ss!");
        register.setFirstName("EmailAuditComprehensive");
        register.setLastName("User");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/v1/auth/register", new HttpEntity<>(register, headers), String.class);

        LoginRequestDto login = new LoginRequestDto();
        login.setUsername("emailaudit.comprehensive.user");
        login.setPassword("EmailAuditComprehensiveP@ss!");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", new HttpEntity<>(login, headers), String.class);
        String accessToken = new ObjectMapper().readTree(loginResponse.getBody()).get("access_token").asText();

        headers.setBearerAuth(accessToken);

        // Test all endpoints - they should all return 403 FORBIDDEN
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        Long tenantId = 1L;

        // Test 1: Get audits
        ResponseEntity<String> getAuditsResponse = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits",
                HttpMethod.GET,
                getRequest,
                String.class,
                tenantId
        );
        assertThat(getAuditsResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Test 2: Export CSV
        ResponseEntity<String> exportResponse = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits/export",
                HttpMethod.GET,
                getRequest,
                String.class,
                tenantId
        );
        assertThat(exportResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Test 3: Retry failed emails
        ResponseEntity<String> retryResponse = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits/retry",
                HttpMethod.PUT,
                getRequest,
                String.class,
                tenantId
        );
        assertThat(retryResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Test 4: Get summary
        ResponseEntity<String> summaryResponse = restTemplate.exchange(
                "/api/v1/{tenantId}/email-audits/summary",
                HttpMethod.GET,
                getRequest,
                String.class,
                tenantId
        );
        assertThat(summaryResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        System.out.println("All email audit endpoints returned 403 FORBIDDEN as expected for non-admin user");
    }
}