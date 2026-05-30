package co.za.kingstechco.kingstechco.invoeaserapp.util;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.LoginRequestDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.*;
import co.za.kingstechco.kingstechco.invoeaserapp.enums.InvoiceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for API testing that provides common functionality
 * for creating test data, authentication, and HTTP operations.
 */
public class ApiTestUtils {
    
    private final TestRestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String testRunId;
    
    public ApiTestUtils(TestRestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.testRunId = UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Creates a test user registration DTO with unique data.
     */
    public UserRegistrationDto createTestUserRegistration() {
        UserRegistrationDto registration = new UserRegistrationDto();
        registration.setUsername(createUniqueEmail("testuser"));
        registration.setPassword("TestPassword123!");
        registration.setEmail(createUniqueEmail("testuser"));
        registration.setFirstName("Test");
        registration.setLastName("User");
        return registration;
    }
    
    /**
     * Creates a test login request DTO.
     */
    public LoginRequestDto createTestLoginRequest(String username, String password) {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        return loginRequest;
    }
    
    /**
     * Registers a test user and returns the access token.
     */
    public String registerAndLoginTestUser() {
        UserRegistrationDto registration = createTestUserRegistration();
        
        // Register user
        post("/api/v1/auth/register", registration, User.class);
        
        // Login and get token
        LoginRequestDto loginRequest = createTestLoginRequest(registration.getUsername(), registration.getPassword());
        ResponseEntity<Map> loginResponse = post("/api/v1/auth/login", loginRequest, Map.class);
        
        if (loginResponse.getBody() != null) {
            return (String) loginResponse.getBody().get("access_token");
        }
        
        throw new RuntimeException("Failed to obtain access token");
    }
    
    /**
     * Creates a test customer with unique data.
     */
    public Customer createTestCustomer(Long tenantId) {
        Customer customer = new Customer();
        customer.setCustomerName(createUniqueName("Test Customer"));
        customer.setEmail(createUniqueEmail("customer"));
        customer.setPhone("+1234567890");
        customer.setTenantId(tenantId);
        return customer;
    }
    
    /**
     * Creates a test recipient with unique data.
     */
    public Recipient createTestRecipient(Long tenantId) {
        Recipient recipient = new Recipient();
        recipient.setCompanyName(createUniqueName("Test Recipient"));
        recipient.setContactName(createUniqueName("Test Contact"));
        recipient.setEmail(createUniqueEmail("recipient"));
        recipient.setPhone("+1234567890");
        recipient.setTenantId(tenantId);
        return recipient;
    }
    
    /**
     * Creates a test service provider with unique data.
     */
    public ServiceProvider createTestServiceProvider(Long tenantId) {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setProviderName(createUniqueName("Test Service Provider"));
        serviceProvider.setEmail(createUniqueEmail("provider"));
        serviceProvider.setPhone("+1234567890");
        serviceProvider.setTenantId(tenantId);
        return serviceProvider;
    }
    
    /**
     * Creates a test invoice with unique data.
     */
    public Invoice createTestInvoice(Long tenantId, Long customerId, Long recipientId, ServiceProvider serviceProvider) {
        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setCustomerId(customerId);
        invoice.setRecipientId(recipientId);
        invoice.setInvoiceReference(createUniqueReference("INV-TEST"));
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTotalAmount(BigDecimal.valueOf(1000.00));
        invoice.setServiceProvider(serviceProvider);
        invoice.setInvoiceItems(new HashSet<>());
        invoice.setVersion(0L);
        invoice.setDeleted(false);
        return invoice;
    }
    
    /**
     * Creates a test invoice item with unique data.
     */
    public InvoiceItem createTestInvoiceItem(Long tenantId, Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setTenantId(tenantId);
        item.setHoursWorked(10);
        item.setHourlyRate(BigDecimal.valueOf(100.00));
        item.setLineTotal(BigDecimal.valueOf(1000.00));
        return item;
    }
    
    /**
     * Creates a unique email for testing.
     */
    public String createUniqueEmail(String prefix) {
        return prefix + "-" + testRunId + "@test.com";
    }
    
    /**
     * Creates a unique name for testing.
     */
    public String createUniqueName(String baseName) {
        return baseName + " " + testRunId;
    }
    
    /**
     * Creates a unique reference for testing.
     */
    public String createUniqueReference(String prefix) {
        return prefix + "-" + testRunId;
    }
    
    /**
     * Gets the current test run ID.
     */
    public String getTestRunId() {
        return testRunId;
    }
    
    /**
     * Converts an object to JSON string.
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
    
    /**
     * Converts JSON string to object.
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }
    
    /**
     * Creates HTTP headers with JSON content type.
     */
    public HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return headers;
    }
    
    /**
     * Creates HTTP headers with authentication token.
     */
    public HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = createJsonHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }
    
    /**
     * Creates HTTP headers with tenant ID.
     */
    public HttpHeaders createTenantHeaders(Long tenantId) {
        HttpHeaders headers = createJsonHeaders();
        headers.set("X-Tenant-ID", String.valueOf(tenantId));
        return headers;
    }
    
    /**
     * Creates HTTP headers with both authentication and tenant ID.
     */
    public HttpHeaders createAuthTenantHeaders(String token, Long tenantId) {
        HttpHeaders headers = createAuthHeaders(token);
        headers.set("X-Tenant-ID", String.valueOf(tenantId));
        return headers;
    }
    
    /**
     * Creates HTTP entity with JSON body.
     */
    public <T> HttpEntity<T> createJsonEntity(T body) {
        return new HttpEntity<>(body, createJsonHeaders());
    }
    
    /**
     * Creates HTTP entity with JSON body and auth headers.
     */
    public <T> HttpEntity<T> createAuthEntity(T body, String token) {
        return new HttpEntity<>(body, createAuthHeaders(token));
    }
    
    /**
     * Creates HTTP entity with JSON body and tenant headers.
     */
    public <T> HttpEntity<T> createTenantEntity(T body, Long tenantId) {
        return new HttpEntity<>(body, createTenantHeaders(tenantId));
    }
    
    /**
     * Creates HTTP entity with JSON body, auth, and tenant headers.
     */
    public <T> HttpEntity<T> createAuthTenantEntity(T body, String token, Long tenantId) {
        return new HttpEntity<>(body, createAuthTenantHeaders(token, tenantId));
    }
    
    /**
     * Makes a GET request to the specified endpoint.
     */
    public <T> ResponseEntity<T> get(String endpoint, Class<T> responseType) {
        return restTemplate.getForEntity(baseUrl + endpoint, responseType);
    }
    
    /**
     * Makes a GET request with authentication.
     */
    public <T> ResponseEntity<T> get(String endpoint, Class<T> responseType, String token) {
        return restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(token)),
                responseType
        );
    }
    
    /**
     * Makes a GET request with tenant headers.
     */
    public <T> ResponseEntity<T> get(String endpoint, Class<T> responseType, Long tenantId) {
        return restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.GET,
                new HttpEntity<>(createTenantHeaders(tenantId)),
                responseType
        );
    }
    
    /**
     * Makes a POST request to the specified endpoint.
     */
    public <T, R> ResponseEntity<R> post(String endpoint, T body, Class<R> responseType) {
        return restTemplate.postForEntity(baseUrl + endpoint, createJsonEntity(body), responseType);
    }
    
    /**
     * Makes a POST request with authentication.
     */
    public <T, R> ResponseEntity<R> post(String endpoint, T body, Class<R> responseType, String token) {
        return restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.POST,
                createAuthEntity(body, token),
                responseType
        );
    }
    
    /**
     * Makes a POST request with tenant headers.
     */
    public <T, R> ResponseEntity<R> post(String endpoint, T body, Class<R> responseType, Long tenantId) {
        return restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.POST,
                createTenantEntity(body, tenantId),
                responseType
        );
    }
    
    /**
     * Makes a PUT request to the specified endpoint.
     */
    public <T, R> ResponseEntity<R> put(String endpoint, T body, Class<R> responseType) {
        return restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.PUT,
                createJsonEntity(body),
                responseType
        );
    }
    
    /**
     * Makes a PUT request with authentication.
     */
    public <T, R> ResponseEntity<R> put(String endpoint, T body, Class<R> responseType, String token) {
        return restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.PUT,
                createAuthEntity(body, token),
                responseType
        );
    }
    
    /**
     * Makes a DELETE request to the specified endpoint.
     */
    public <T> ResponseEntity<T> delete(String endpoint, Class<T> responseType) {
        return restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.DELETE,
                new HttpEntity<>(createJsonHeaders()),
                responseType
        );
    }
    
    /**
     * Makes a DELETE request with authentication.
     */
    public <T> ResponseEntity<T> delete(String endpoint, Class<T> responseType, String token) {
        return restTemplate.exchange(
                baseUrl + endpoint,
                HttpMethod.DELETE,
                new HttpEntity<>(createAuthHeaders(token)),
                responseType
        );
    }
    
    /**
     * Creates a test data builder for fluent API.
     */
    public TestDataBuilder testData() {
        return new TestDataBuilder(this);
    }
    
    /**
     * Builder class for creating test data with fluent API.
     */
    public static class TestDataBuilder {
        private final ApiTestUtils utils;
        
        public TestDataBuilder(ApiTestUtils utils) {
            this.utils = utils;
        }
        
        public Customer customer(Long tenantId) {
            return utils.createTestCustomer(tenantId);
        }
        
        public Recipient recipient(Long tenantId) {
            return utils.createTestRecipient(tenantId);
        }
        
        public ServiceProvider serviceProvider(Long tenantId) {
            return utils.createTestServiceProvider(tenantId);
        }
        
        public Invoice invoice(Long tenantId, Long customerId, Long recipientId, ServiceProvider serviceProvider) {
            return utils.createTestInvoice(tenantId, customerId, recipientId, serviceProvider);
        }
        
        public InvoiceItem invoiceItem(Long tenantId, Invoice invoice) {
            return utils.createTestInvoiceItem(tenantId, invoice);
        }
        
        public UserRegistrationDto userRegistration() {
            return utils.createTestUserRegistration();
        }
        
        public LoginRequestDto loginRequest(String username, String password) {
            return utils.createTestLoginRequest(username, password);
        }
    }
}