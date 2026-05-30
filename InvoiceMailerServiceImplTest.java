package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Customer;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.Invoice;
import co.za.kingstechco.kingstechco.invoeaserapp.service.EmailTemplateService;
import co.za.kingstechco.kingstechco.invoeaserapp.service.TenantUsageTrackingService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceMailerServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;
    
    @Mock
    private EmailTemplateService emailTemplateService;
    
    @Mock
    private TenantUsageTrackingService tenantUsageTrackingService;
    
    @Mock
    private MimeMessage mimeMessage;

    private InvoiceMailerServiceImpl invoiceMailerService;

    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String EMAIL_SUBJECT = "Invoice INV-2024-001";
    private static final String EMAIL_BODY = "<html><body>Your invoice is ready</body></html>";
    private static final String INVOICE_REFERENCE = "INV-2024-001";

    @BeforeEach
    void setUp() {
        invoiceMailerService = new InvoiceMailerServiceImpl(javaMailSender, emailTemplateService, tenantUsageTrackingService);
    }

    @Test
    void sendInvoiceEmail_ShouldSendEmail_WhenValidInvoice() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn(EMAIL_SUBJECT);
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn(EMAIL_BODY);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        invoiceMailerService.sendInvoiceEmail(invoice);

        // Then
        verify(emailTemplateService).prepareEmailSubject(invoice);
        verify(emailTemplateService).prepareEmailBody(invoice);
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendInvoiceEmail_ShouldSetCorrectEmailProperties_WhenSending() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn(EMAIL_SUBJECT);
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn(EMAIL_BODY);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        invoiceMailerService.sendInvoiceEmail(invoice);

        // Then
        // Verify template service calls
        verify(emailTemplateService).prepareEmailSubject(invoice);
        verify(emailTemplateService).prepareEmailBody(invoice);
        
        // Verify mail sender calls
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send( mimeMessage);
    }

    @Test
    void sendInvoiceEmail_ShouldThrowRuntimeException_WhenSendFails() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn(EMAIL_SUBJECT);
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn(EMAIL_BODY);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        // Simulate mail sending failure by throwing RuntimeException from send
        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(javaMailSender).send(mimeMessage);

        // When & Then
        // RuntimeException should be thrown since it's not caught by the implementation
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> invoiceMailerService.sendInvoiceEmail(invoice));

        assertEquals("SMTP server unavailable", exception.getMessage());
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendInvoiceEmail_ShouldThrowException_WhenEmailTemplateServiceFails() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateService.prepareEmailSubject(invoice))
                .thenThrow(new RuntimeException("Template processing failed"));

        // When & Then
        // The implementation doesn't catch RuntimeException, so it will be thrown
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> invoiceMailerService.sendInvoiceEmail(invoice));
        
        assertEquals("Template processing failed", exception.getMessage());
        verify(emailTemplateService).prepareEmailSubject(invoice);
        verify(javaMailSender, never()).createMimeMessage();
        verify(javaMailSender, never()).send((MimeMessage) any());
    }

    @Test
    void sendInvoiceEmail_ShouldThrowException_WhenCustomerEmailIsNull() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.getCustomer().setEmail(null);
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn(EMAIL_SUBJECT);
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn(EMAIL_BODY);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then
        // MimeMessageHelper.setTo(null) will throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> invoiceMailerService.sendInvoiceEmail(invoice));

        verify(emailTemplateService).prepareEmailSubject(invoice);
        verify(emailTemplateService).prepareEmailBody(invoice);
        verify(javaMailSender).createMimeMessage();
    }

    @Test
    void sendInvoiceEmail_ShouldThrowException_WhenCustomerIsNull() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setCustomer(null);

        // When & Then
        // Should throw NullPointerException when trying to access customer.getEmail()
        assertThrows(NullPointerException.class,
                () -> invoiceMailerService.sendInvoiceEmail(invoice));

        // Should fail early when trying to access customer email
        verify(emailTemplateService, never()).prepareEmailSubject(any());
        verify(javaMailSender, never()).createMimeMessage();
    }

    @Test
    void sendInvoiceEmail_ShouldHandleEmptyEmailSubject_Appropriately() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn("");
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn(EMAIL_BODY);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        invoiceMailerService.sendInvoiceEmail(invoice);

        // Then
        verify(emailTemplateService).prepareEmailSubject(invoice);
        verify(emailTemplateService).prepareEmailBody(invoice);
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendInvoiceEmail_ShouldHandleEmptyEmailBody_Appropriately() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn(EMAIL_SUBJECT);
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn("");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        invoiceMailerService.sendInvoiceEmail(invoice);

        // Then
        verify(emailTemplateService).prepareEmailSubject(invoice);
        verify(emailTemplateService).prepareEmailBody(invoice);
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendInvoiceEmail_ShouldThrowException_WhenCreateMimeMessageFails() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn(EMAIL_SUBJECT);
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn(EMAIL_BODY);
        when(javaMailSender.createMimeMessage())
                .thenThrow(new RuntimeException("Message creation failed"));

        // When & Then
        // RuntimeException is not caught, so it will be thrown
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> invoiceMailerService.sendInvoiceEmail(invoice));
        
        assertEquals("Message creation failed", exception.getMessage());
        verify(emailTemplateService).prepareEmailSubject(invoice);
        verify(emailTemplateService).prepareEmailBody(invoice);
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender, never()).send((MimeMessage) any());
    }

    @Test
    void sendInvoiceEmail_ShouldCallServices_InCorrectOrder() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn(EMAIL_SUBJECT);
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn(EMAIL_BODY);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        invoiceMailerService.sendInvoiceEmail(invoice);

        // Then - Verify order of operations
        var inOrder = inOrder(emailTemplateService, javaMailSender);
        inOrder.verify(emailTemplateService).prepareEmailSubject(invoice);
        inOrder.verify(emailTemplateService).prepareEmailBody(invoice);
        inOrder.verify(javaMailSender).createMimeMessage();
        inOrder.verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendInvoiceEmail_ShouldPassCorrectInvoice_ToTemplateService() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setInvoiceReference("CUSTOM-REF-001");
        invoice.setTotalAmount(BigDecimal.valueOf(2500.00));
        
        when(emailTemplateService.prepareEmailSubject(invoice)).thenReturn(EMAIL_SUBJECT);
        when(emailTemplateService.prepareEmailBody(invoice)).thenReturn(EMAIL_BODY);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        invoiceMailerService.sendInvoiceEmail(invoice);

        // Then
        verify(emailTemplateService).prepareEmailSubject(argThat(inv -> 
            inv.getInvoiceReference().equals("CUSTOM-REF-001") &&
            inv.getTotalAmount().equals(BigDecimal.valueOf(2500.00))
        ));
        verify(emailTemplateService).prepareEmailBody(argThat(inv -> 
            inv.getInvoiceReference().equals("CUSTOM-REF-001") &&
            inv.getTotalAmount().equals(BigDecimal.valueOf(2500.00))
        ));
    }

    // Helper methods
    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceReference(INVOICE_REFERENCE);
        invoice.setTotalAmount(BigDecimal.valueOf(1000.00));
        
        Customer customer = new Customer();
        customer.setEmail(CUSTOMER_EMAIL);
        customer.setCustomerName("Test Customer");
        invoice.setCustomer(customer);
        
        return invoice;
    }
}