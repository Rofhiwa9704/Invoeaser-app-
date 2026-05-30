package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.InvoiceDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.*;
import co.za.kingstechco.kingstechco.invoeaserapp.enums.InvoiceStatus;
import co.za.kingstechco.kingstechco.invoeaserapp.exception.ResourceNotFoundException;
import co.za.kingstechco.kingstechco.invoeaserapp.mapper.dtoMapper.InvoiceMapper;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.*;
import co.za.kingstechco.kingstechco.invoeaserapp.service.InvoiceMailerService;
import co.za.kingstechco.kingstechco.invoeaserapp.service.TenantUsageTrackingService;
import co.za.kingstechco.kingstechco.invoeaserapp.pdf.renderer.InvoiceTemplateRenderer;
import co.za.kingstechco.kingstechco.invoeaserapp.mail.InvoiceEmailPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceMailerService invoiceMailerService;
    
    @Mock
    private InvoiceTemplateRenderer invoiceTemplateRenderer;
    
    @Mock
    private InvoiceRepository invoiceRepository;
    
    @Mock
    private InvoiceEmailPublisher invoiceEmailPublisher;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private InvoiceDescriptionRepository invoiceDescriptionRepository;
    
    @Mock
    private InvoiceItemDescriptionRepository invoiceItemDescriptionRepository;
    
    @Mock
    private ServiceProviderRepository serviceProviderRepository;
    
    @Mock
    private EmailTemplateServiceImpl emailTemplateService;
    
    @Mock
    private InvoiceMapper invoiceMapper;
    
    @Mock
    private RecipientRepository recipientRepository;
    
    @Mock
    private TenantUsageTrackingService tenantUsageTrackingService;

    private InvoiceServiceImpl invoiceService;

    private static final Long TENANT_ID = 1L;
    private static final Long INVOICE_ID = 100L;
    private static final Long CUSTOMER_ID = 200L;
    private static final Long SERVICE_PROVIDER_ID = 300L;
    private static final Long RECIPIENT_ID = 400L;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceServiceImpl(
                invoiceMailerService,
                invoiceTemplateRenderer,
                invoiceRepository,
                invoiceEmailPublisher,
                customerRepository,
                invoiceDescriptionRepository,
                invoiceItemDescriptionRepository,
                serviceProviderRepository,
                emailTemplateService,
                invoiceMapper,
                recipientRepository,
                tenantUsageTrackingService
        );
    }

    @Test
    void createInvoiceFromDto_ShouldCreateInvoice_WhenValidDto() {
        // Given
        InvoiceDto dto = createTestInvoiceDto();
        Customer customer = createTestCustomer();
        ServiceProvider serviceProvider = createTestServiceProvider();
        Recipient recipient = createTestRecipient();
        Invoice expectedInvoice = createTestInvoice();

        when(customerRepository.findByTenantIdAndCustomerId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(customer));
        when(serviceProviderRepository.findByTenantIdAndProviderId(TENANT_ID, SERVICE_PROVIDER_ID))
                .thenReturn(Optional.of(serviceProvider));
        when(recipientRepository.findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID))
                .thenReturn(Optional.of(recipient));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // When
        Invoice result = invoiceService.createInvoiceFromDto(TENANT_ID, dto);

        // Then
        assertNotNull(result);
        verify(customerRepository).findByTenantIdAndCustomerId(TENANT_ID, CUSTOMER_ID);
        verify(serviceProviderRepository).findByTenantIdAndProviderId(TENANT_ID, SERVICE_PROVIDER_ID);
        verify(recipientRepository).findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createInvoiceFromDto_ShouldThrowException_WhenCustomerNotFound() {
        // Given
        InvoiceDto dto = createTestInvoiceDto();
        when(customerRepository.findByTenantIdAndCustomerId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.createInvoiceFromDto(TENANT_ID, dto));
        
        assertEquals("Customer not found", exception.getMessage());
        verify(customerRepository).findByTenantIdAndCustomerId(TENANT_ID, CUSTOMER_ID);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceFromDto_ShouldThrowException_WhenServiceProviderNotFound() {
        // Given
        InvoiceDto dto = createTestInvoiceDto();
        Customer customer = createTestCustomer();
        
        when(customerRepository.findByTenantIdAndCustomerId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(customer));
        when(serviceProviderRepository.findByTenantIdAndProviderId(TENANT_ID, SERVICE_PROVIDER_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.createInvoiceFromDto(TENANT_ID, dto));
        
        assertEquals("Service provider not found", exception.getMessage());
        verify(serviceProviderRepository).findByTenantIdAndProviderId(TENANT_ID, SERVICE_PROVIDER_ID);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceFromDto_ShouldCreateInvoice_WhenRecipientIsNull() {
        // Given
        InvoiceDto dto = createTestInvoiceDto();
        dto.setRecipientId(null);
        Customer customer = createTestCustomer();
        ServiceProvider serviceProvider = createTestServiceProvider();
        Invoice expectedInvoice = createTestInvoice();

        when(customerRepository.findByTenantIdAndCustomerId(TENANT_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(customer));
        when(serviceProviderRepository.findByTenantIdAndProviderId(TENANT_ID, SERVICE_PROVIDER_ID))
                .thenReturn(Optional.of(serviceProvider));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // When
        Invoice result = invoiceService.createInvoiceFromDto(TENANT_ID, dto);

        // Then
        assertNotNull(result);
        verify(recipientRepository, never()).findByTenantIdAndRecipientId(any(), any());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void getAllInvoices_ShouldReturnAllInvoicesForTenant() {
        // Given
        List<Invoice> expectedInvoices = Arrays.asList(
                createTestInvoice(),
                createTestInvoice()
        );
        when(invoiceRepository.findAllByTenantId(TENANT_ID)).thenReturn(expectedInvoices);

        // When
        List<Invoice> result = invoiceService.getAllInvoices(TENANT_ID);

        // Then
        assertEquals(expectedInvoices.size(), result.size());
        assertEquals(expectedInvoices, result);
        verify(invoiceRepository).findAllByTenantId(TENANT_ID);
    }

    @Test
    void getInvoiceById_ShouldReturnInvoice_WhenExists() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setTenantId(TENANT_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        // When
        Optional<Invoice> result = invoiceService.getInvoiceById(TENANT_ID, INVOICE_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(invoice, result.get());
        verify(invoiceRepository).findById(INVOICE_ID);
    }

    @Test
    void getInvoiceById_ShouldReturnEmpty_WhenInvoiceNotFound() {
        // Given
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.empty());

        // When
        Optional<Invoice> result = invoiceService.getInvoiceById(TENANT_ID, INVOICE_ID);

        // Then
        assertFalse(result.isPresent());
        verify(invoiceRepository).findById(INVOICE_ID);
    }

    @Test
    void getInvoiceById_ShouldReturnEmpty_WhenTenantIdDoesNotMatch() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setTenantId(999L); // Different tenant ID
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        // When
        Optional<Invoice> result = invoiceService.getInvoiceById(TENANT_ID, INVOICE_ID);

        // Then
        assertFalse(result.isPresent());
        verify(invoiceRepository).findById(INVOICE_ID);
    }

    @Test
    void deleteInvoice_ShouldReturnTrue_WhenInvoiceExists() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setTenantId(TENANT_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        // When
        boolean result = invoiceService.deleteInvoice(TENANT_ID, INVOICE_ID);

        // Then
        assertTrue(result);
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void deleteInvoice_ShouldReturnFalse_WhenInvoiceNotFound() {
        // Given
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.empty());

        // When
        boolean result = invoiceService.deleteInvoice(TENANT_ID, INVOICE_ID);

        // Then
        assertFalse(result);
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void deleteInvoice_ShouldReturnFalse_WhenTenantIdDoesNotMatch() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setTenantId(999L); // Different tenant ID
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        // When
        boolean result = invoiceService.deleteInvoice(TENANT_ID, INVOICE_ID);

        // Then
        assertFalse(result);
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void sendInvoice_ShouldSendImmediately_WhenSendImmediatelyIsTrue() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setTenantId(TENANT_ID);
        invoice.setCustomer(createTestCustomer()); // Add customer to avoid NPE
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        // When
        invoiceService.sendInvoice(TENANT_ID, INVOICE_ID, true, null);

        // Then
        verify(invoiceRepository, times(1)).findById(INVOICE_ID);
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());
    }

    @Test
    void sendInvoice_ShouldScheduleInvoice_WhenSendImmediatelyIsFalseAndScheduledTimeProvided() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setTenantId(TENANT_ID);
        LocalDateTime scheduledTime = LocalDateTime.of(2024, 1, 15, 10, 0);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        invoiceService.sendInvoice(TENANT_ID, INVOICE_ID, false, scheduledTime);

        // Then
        verify(invoiceRepository, times(2)).findById(INVOICE_ID);
        verify(invoiceRepository).save(invoice);
        // Note: The actual scheduling logic would need to be mocked if it involves external dependencies
    }

    @Test
    void sendInvoice_ShouldThrowException_WhenInvoiceNotFound() {
        // Given
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.sendInvoice(TENANT_ID, INVOICE_ID, true, null));
        
        assertTrue(exception.getMessage().contains("Invoice not found"));
        verify(invoiceRepository).findById(INVOICE_ID);
    }

    @Test
    void sendInvoice_ShouldThrowException_WhenScheduledTimeIsNullAndSendImmediatelyIsFalse() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setTenantId(TENANT_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> invoiceService.sendInvoice(TENANT_ID, INVOICE_ID, false, null));
        
        assertEquals("Scheduled time is required when sendImmediately is false", exception.getMessage());
    }

    // Helper methods to create test objects
    private InvoiceDto createTestInvoiceDto() {
        InvoiceDto dto = new InvoiceDto();
        dto.setCustomerId(CUSTOMER_ID);
        dto.setServiceProviderId(SERVICE_PROVIDER_ID);
        dto.setRecipientId(RECIPIENT_ID);
        dto.setCustomPrefix("ABC");
        dto.setDueDate(LocalDate.now().plusDays(30));
        return dto;
    }

    private Customer createTestCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setTenantId(TENANT_ID);
        customer.setEmail("test@example.com"); // Add email to avoid NPE
        customer.setCustomerName("Test Customer");
        customer.setPhone("1234567890");
        return customer;
    }

    private ServiceProvider createTestServiceProvider() {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setProviderId(SERVICE_PROVIDER_ID);
        serviceProvider.setTenantId(TENANT_ID);
        serviceProvider.setProviderName("Test Service Provider");
        return serviceProvider;
    }

    private Recipient createTestRecipient() {
        Recipient recipient = new Recipient();
        recipient.setRecipientId(RECIPIENT_ID);
        recipient.setTenantId(TENANT_ID);
        recipient.setEmail("test@example.com");
        return recipient;
    }

    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(INVOICE_ID);
        invoice.setTenantId(TENANT_ID);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTotalAmount(BigDecimal.valueOf(1000.00));
        invoice.setInvoiceReference("ABC123");
        return invoice;
    }
}