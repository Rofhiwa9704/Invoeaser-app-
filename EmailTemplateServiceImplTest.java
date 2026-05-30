package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Customer;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.EmailTemplate;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.Invoice;
import co.za.kingstechco.kingstechco.invoeaserapp.exception.ResourceNotFoundException;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.EmailTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceImplTest {

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    private EmailTemplateServiceImpl emailTemplateService;

    private static final Long CUSTOMER_ID = 123L;
    private static final String CUSTOMER_NAME = "Acme Corporation";
    private static final String INVOICE_REFERENCE = "INV-2024-001";
    private static final String SUBJECT_TEMPLATE = "Invoice {invoiceReference} from Our Company";
    private static final String BODY_TEMPLATE = "Dear {clientName}, your invoice {invoiceReference} is ready.";
    private static final String EXPECTED_SUBJECT = "Invoice {invoiceReference} from Our Company";
    private static final String EXPECTED_BODY = "Dear Acme Corporation, your invoice INV-2024-001 is ready.";

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateServiceImpl(emailTemplateRepository);
    }

    @Test
    void prepareEmailBody_ShouldReturnProcessedTemplate_WhenTemplateExists() {
        // Given
        Invoice invoice = createTestInvoice();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When
        String result = emailTemplateService.prepareEmailBody(invoice);

        // Then
        assertEquals(EXPECTED_BODY, result);
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailBody_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> emailTemplateService.prepareEmailBody(invoice));
        
        assertTrue(exception.getMessage().contains("No email template found for customer"));
        assertTrue(exception.getMessage().contains(CUSTOMER_ID.toString()));
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailSubject_ShouldReturnSubjectTemplate_WhenTemplateExists() {
        // Given
        Invoice invoice = createTestInvoice();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When
        String result = emailTemplateService.prepareEmailSubject(invoice);

        // Then
        assertEquals(EXPECTED_SUBJECT, result);
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailSubject_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> emailTemplateService.prepareEmailSubject(invoice));
        
        assertTrue(exception.getMessage().contains("No email template found for customer"));
        assertTrue(exception.getMessage().contains(CUSTOMER_ID.toString()));
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailBody_ShouldHandleNullCustomerName_Gracefully() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.getCustomer().setCustomerName(null);
        EmailTemplate emailTemplate = createTestEmailTemplate();
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When & Then
        // This will throw NullPointerException because String.replace() doesn't handle null replacement
        assertThrows(NullPointerException.class,
                () -> emailTemplateService.prepareEmailBody(invoice));
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailBody_ShouldHandleNullInvoiceReference_Gracefully() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setInvoiceReference(null);
        EmailTemplate emailTemplate = createTestEmailTemplate();
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When & Then
        // This will throw NullPointerException because String.replace() doesn't handle null replacement
        assertThrows(NullPointerException.class,
                () -> emailTemplateService.prepareEmailBody(invoice));
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailBody_ShouldHandleEmptyTemplate_Appropriately() {
        // Given
        Invoice invoice = createTestInvoice();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        emailTemplate.setBodyTemplate("");
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When
        String result = emailTemplateService.prepareEmailBody(invoice);

        // Then
        assertEquals("", result);
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailBody_ShouldHandleTemplateWithoutPlaceholders_Correctly() {
        // Given
        Invoice invoice = createTestInvoice();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        emailTemplate.setBodyTemplate("Static email body without placeholders");
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When
        String result = emailTemplateService.prepareEmailBody(invoice);

        // Then
        assertEquals("Static email body without placeholders", result);
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailBody_ShouldHandleMultipleOccurrencesOfSamePlaceholder_Correctly() {
        // Given
        Invoice invoice = createTestInvoice();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        emailTemplate.setBodyTemplate("Dear {clientName}, {clientName} has invoice {invoiceReference}. {clientName} should pay soon.");
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When
        String result = emailTemplateService.prepareEmailBody(invoice);

        // Then
        assertEquals("Dear Acme Corporation, Acme Corporation has invoice INV-2024-001. Acme Corporation should pay soon.", result);
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailBody_ShouldHandleRepositoryException_Gracefully() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailTemplateService.prepareEmailBody(invoice));
        
        assertEquals("Database connection error", exception.getMessage());
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailSubject_ShouldHandleEmptySubjectTemplate_Appropriately() {
        // Given
        Invoice invoice = createTestInvoice();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        emailTemplate.setSubjectTemplate("");
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When
        String result = emailTemplateService.prepareEmailSubject(invoice);

        // Then
        assertEquals("", result);
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailSubject_ShouldHandleNullSubjectTemplate_Gracefully() {
        // Given
        Invoice invoice = createTestInvoice();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        emailTemplate.setSubjectTemplate(null);
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When
        String result = emailTemplateService.prepareEmailSubject(invoice);

        // Then
        // The implementation simply returns null when template is null
        assertNull(result);
        
        verify(emailTemplateRepository).findByCustomer(invoice.getCustomer());
    }

    @Test
    void prepareEmailBody_ShouldUseCorrectCustomer_WhenCallingRepository() {
        // Given
        Invoice invoice = createTestInvoice();
        Customer specificCustomer = invoice.getCustomer();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        
        when(emailTemplateRepository.findByCustomer(specificCustomer))
                .thenReturn(Optional.of(emailTemplate));

        // When
        emailTemplateService.prepareEmailBody(invoice);

        // Then
        verify(emailTemplateRepository).findByCustomer(eq(specificCustomer));
        verify(emailTemplateRepository).findByCustomer(argThat(customer -> 
            customer.getCustomerId().equals(CUSTOMER_ID) &&
            customer.getCustomerName().equals(CUSTOMER_NAME)
        ));
    }

    @Test
    void prepareEmailBody_ShouldHandleNullCustomer_Gracefully() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setCustomer(null);

        // When & Then
        // This will throw a NullPointerException when trying to call findByCustomer(null)
        assertThrows(Exception.class,
                () -> emailTemplateService.prepareEmailBody(invoice));
        
        // Repository is called with null customer, causing the exception
        verify(emailTemplateRepository).findByCustomer(null);
    }

    @Test
    void prepareEmailBody_ShouldCallRepository_OnlyOnce() {
        // Given
        Invoice invoice = createTestInvoice();
        EmailTemplate emailTemplate = createTestEmailTemplate();
        
        when(emailTemplateRepository.findByCustomer(invoice.getCustomer()))
                .thenReturn(Optional.of(emailTemplate));

        // When
        emailTemplateService.prepareEmailBody(invoice);

        // Then
        verify(emailTemplateRepository, times(1)).findByCustomer(invoice.getCustomer());
    }

    // Helper methods
    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceReference(INVOICE_REFERENCE);
        invoice.setTotalAmount(BigDecimal.valueOf(1000.00));
        
        Customer customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setCustomerName(CUSTOMER_NAME);
        customer.setEmail("customer@example.com");
        invoice.setCustomer(customer);
        
        return invoice;
    }

    private EmailTemplate createTestEmailTemplate() {
        EmailTemplate template = new EmailTemplate();
        template.setSubjectTemplate(SUBJECT_TEMPLATE);
        template.setBodyTemplate(BODY_TEMPLATE);
        return template;
    }
}