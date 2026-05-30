package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Customer;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerServiceImpl customerService;

    private static final Long TENANT_ID = 1L;
    private static final Long CUSTOMER_ID = 100L;
    private static final String CUSTOMER_NAME = "Test Customer";
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String CUSTOMER_PHONE = "1234567890";

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository);
    }

    @Test
    void createCustomer_ShouldReturnSavedCustomer_WhenValidCustomer() {
        // Given
        Customer customer = createTestCustomer();
        Customer savedCustomer = createTestCustomer();
        savedCustomer.setCustomerId(CUSTOMER_ID);
        
        when(customerRepository.save(customer)).thenReturn(savedCustomer);

        // When
        Customer result = customerService.createCustomer(customer);

        // Then
        assertNotNull(result);
        assertEquals(CUSTOMER_ID, result.getCustomerId());
        assertEquals(CUSTOMER_NAME, result.getCustomerName());
        assertEquals(CUSTOMER_EMAIL, result.getEmail());
        assertEquals(CUSTOMER_PHONE, result.getPhone());
        
        verify(customerRepository).save(customer);
    }

    @Test
    void createCustomer_ShouldHandleRepositoryExceptions_Gracefully() {
        // Given
        Customer customer = createTestCustomer();
        when(customerRepository.save(customer)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> customerService.createCustomer(customer));
        
        assertEquals("Database error", exception.getMessage());
        verify(customerRepository).save(customer);
    }

    @Test
    void getAllCustomers_ShouldReturnCustomerList_WhenCustomersExist() {
        // Given
        List<Customer> expectedCustomers = Arrays.asList(
                createTestCustomer(),
                createTestCustomerWithId()
        );
        
        when(customerRepository.findAllByTenantId(TENANT_ID)).thenReturn(expectedCustomers);

        // When
        List<Customer> result = customerService.getAllCustomers(TENANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedCustomers, result);
        
        verify(customerRepository).findAllByTenantId(TENANT_ID);
    }

    @Test
    void getAllCustomers_ShouldReturnEmptyList_WhenNoCustomersExist() {
        // Given
        when(customerRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        // When
        List<Customer> result = customerService.getAllCustomers(TENANT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(customerRepository).findAllByTenantId(TENANT_ID);
    }

    @Test
    void getCustomerById_ShouldReturnCustomer_WhenCustomerExists() {
        // Given
        Customer expectedCustomer = createTestCustomer();
        expectedCustomer.setCustomerId(CUSTOMER_ID);
        
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(expectedCustomer));

        // When
        Optional<Customer> result = customerService.getCustomerById(CUSTOMER_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedCustomer, result.get());
        assertEquals(CUSTOMER_ID, result.get().getCustomerId());
        
        verify(customerRepository).findById(CUSTOMER_ID);
    }

    @Test
    void getCustomerById_ShouldReturnEmpty_WhenCustomerNotFound() {
        // Given
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        // When
        Optional<Customer> result = customerService.getCustomerById(CUSTOMER_ID);

        // Then
        assertFalse(result.isPresent());
        
        verify(customerRepository).findById(CUSTOMER_ID);
    }

    @Test
    void updateCustomer_ShouldReturnUpdatedCustomer_WhenCustomerExists() {
        // Given
        Customer existingCustomer = createTestCustomer();
        existingCustomer.setCustomerId(CUSTOMER_ID);
        
        Customer updatedCustomerData = new Customer();
        updatedCustomerData.setCustomerName("Updated Customer");
        updatedCustomerData.setEmail("updated@example.com");
        updatedCustomerData.setPhone("9999999999");
        
        Customer expectedUpdatedCustomer = new Customer();
        expectedUpdatedCustomer.setCustomerId(CUSTOMER_ID);
        expectedUpdatedCustomer.setCustomerName("Updated Customer");
        expectedUpdatedCustomer.setEmail("updated@example.com");
        expectedUpdatedCustomer.setPhone("9999999999");
        
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(expectedUpdatedCustomer);

        // When
        Customer result = customerService.updateCustomer(CUSTOMER_ID, updatedCustomerData);

        // Then
        assertNotNull(result);
        assertEquals("Updated Customer", result.getCustomerName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("9999999999", result.getPhone());
        
        verify(customerRepository).findById(CUSTOMER_ID);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void updateCustomer_ShouldThrowException_WhenCustomerNotFound() {
        // Given
        Customer updatedCustomerData = new Customer();
        updatedCustomerData.setCustomerName("Updated Customer");
        
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> customerService.updateCustomer(CUSTOMER_ID, updatedCustomerData));
        
        assertEquals("Customer not found with id " + CUSTOMER_ID, exception.getMessage());
        
        verify(customerRepository).findById(CUSTOMER_ID);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomer_ShouldUpdateOnlyProvidedFields_WhenPartialUpdate() {
        // Given
        Customer existingCustomer = createTestCustomer();
        existingCustomer.setCustomerId(CUSTOMER_ID);
        
        Customer partialUpdate = new Customer();
        partialUpdate.setCustomerName("New Name Only");
        partialUpdate.setEmail(CUSTOMER_EMAIL); // Keep same email
        partialUpdate.setPhone(CUSTOMER_PHONE); // Keep same phone
        
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Customer result = customerService.updateCustomer(CUSTOMER_ID, partialUpdate);

        // Then
        assertEquals("New Name Only", result.getCustomerName());
        assertEquals(CUSTOMER_EMAIL, result.getEmail());
        assertEquals(CUSTOMER_PHONE, result.getPhone());
        
        verify(customerRepository).save(existingCustomer);
    }

    @Test
    void deleteCustomer_ShouldReturnTrue_WhenCustomerExists() {
        // Given
        when(customerRepository.existsById(CUSTOMER_ID)).thenReturn(true);

        // When
        boolean result = customerService.deleteCustomer(CUSTOMER_ID);

        // Then
        assertTrue(result);
        
        verify(customerRepository).existsById(CUSTOMER_ID);
        verify(customerRepository).deleteById(CUSTOMER_ID);
    }

    @Test
    void deleteCustomer_ShouldReturnFalse_WhenCustomerNotFound() {
        // Given
        when(customerRepository.existsById(CUSTOMER_ID)).thenReturn(false);

        // When
        boolean result = customerService.deleteCustomer(CUSTOMER_ID);

        // Then
        assertFalse(result);
        
        verify(customerRepository).existsById(CUSTOMER_ID);
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void deleteCustomer_ShouldHandleRepositoryExceptions_WhenDeletionFails() {
        // Given
        when(customerRepository.existsById(CUSTOMER_ID)).thenReturn(true);
        doThrow(new RuntimeException("Database constraint violation"))
                .when(customerRepository).deleteById(CUSTOMER_ID);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> customerService.deleteCustomer(CUSTOMER_ID));
        
        assertEquals("Database constraint violation", exception.getMessage());
        
        verify(customerRepository).existsById(CUSTOMER_ID);
        verify(customerRepository).deleteById(CUSTOMER_ID);
    }

    @Test
    void createCustomer_ShouldPreserveTenantId_WhenCustomerHasTenantId() {
        // Given
        Customer customer = createTestCustomer();
        customer.setTenantId(TENANT_ID);
        
        Customer savedCustomer = createTestCustomer();
        savedCustomer.setCustomerId(CUSTOMER_ID);
        savedCustomer.setTenantId(TENANT_ID);
        
        when(customerRepository.save(customer)).thenReturn(savedCustomer);

        // When
        Customer result = customerService.createCustomer(customer);

        // Then
        assertEquals(TENANT_ID, result.getTenantId());
        verify(customerRepository).save(customer);
    }

    @Test
    void getAllCustomers_ShouldFilterByTenantId_WhenMultipleTenantsExist() {
        // Given
        List<Customer> tenantCustomers = Arrays.asList(
                createTestCustomerWithTenant(1L),
                createTestCustomerWithTenant(2L)
        );
        
        when(customerRepository.findAllByTenantId(TENANT_ID)).thenReturn(tenantCustomers);

        // When
        List<Customer> result = customerService.getAllCustomers(TENANT_ID);

        // Then
        assertEquals(2, result.size());
        result.forEach(customer -> assertEquals(TENANT_ID, customer.getTenantId()));
        
        verify(customerRepository).findAllByTenantId(TENANT_ID);
    }

    // Helper methods
    private Customer createTestCustomer() {
        Customer customer = new Customer();
        customer.setCustomerName(CUSTOMER_NAME);
        customer.setEmail(CUSTOMER_EMAIL);
        customer.setPhone(CUSTOMER_PHONE);
        return customer;
    }

    private Customer createTestCustomerWithId() {
        Customer customer = new Customer();
        customer.setCustomerId(2L);
        customer.setCustomerName("Another Customer");
        customer.setEmail("another@example.com");
        customer.setPhone("0987654321");
        return customer;
    }

    private Customer createTestCustomerWithTenant(Long customerId) {
        Customer customer = createTestCustomer();
        customer.setCustomerId(customerId);
        customer.setTenantId(CustomerServiceImplTest.TENANT_ID);
        return customer;
    }
}