package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Invoice;
import co.za.kingstechco.kingstechco.invoeaserapp.pdf.renderer.PdfRenderer;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.InvoiceRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.service.TenantUsageTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    
    @Mock
    private PdfRenderer pdfRenderer;
    
    @Mock
    private TenantUsageTrackingService tenantUsageTrackingService;

    private InvoicePdfServiceImpl invoicePdfService;

    private static final Long INVOICE_ID = 123L;
    private static final byte[] MOCK_PDF_BYTES = "mock-pdf-content".getBytes();

    @BeforeEach
    void setUp() {
        invoicePdfService = new InvoicePdfServiceImpl(invoiceRepository, pdfRenderer, tenantUsageTrackingService);
    }

    @Test
    void generatePdf_ShouldReturnByteArrayResource_WhenInvoiceExists() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(pdfRenderer.renderInvoiceToPdf(invoice)).thenReturn(MOCK_PDF_BYTES);

        // When
        ByteArrayResource result = invoicePdfService.generatePdf(INVOICE_ID);

        // Then
        assertNotNull(result);
        assertArrayEquals(MOCK_PDF_BYTES, result.getByteArray());
        assertEquals(MOCK_PDF_BYTES.length, result.contentLength());
        
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(pdfRenderer).renderInvoiceToPdf(invoice);
    }

    @Test
    void generatePdf_ShouldThrowException_WhenInvoiceNotFound() {
        // Given
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> invoicePdfService.generatePdf(INVOICE_ID));
        
        assertEquals("Invoice not found: " + INVOICE_ID, exception.getMessage());
        
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(pdfRenderer, never()).renderInvoiceToPdf(any());
    }

    @Test
    void generatePdf_ShouldHandlePdfRenderingExceptions_Gracefully() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(pdfRenderer.renderInvoiceToPdf(invoice))
                .thenThrow(new RuntimeException("PDF rendering failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> invoicePdfService.generatePdf(INVOICE_ID));
        
        assertEquals("PDF rendering failed", exception.getMessage());
        
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(pdfRenderer).renderInvoiceToPdf(invoice);
    }

    @Test
    void generatePdf_ShouldHandleEmptyPdfBytes_Appropriately() {
        // Given
        Invoice invoice = createTestInvoice();
        byte[] emptyPdfBytes = new byte[0];
        
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(pdfRenderer.renderInvoiceToPdf(invoice)).thenReturn(emptyPdfBytes);

        // When
        ByteArrayResource result = invoicePdfService.generatePdf(INVOICE_ID);

        // Then
        assertNotNull(result);
        assertArrayEquals(emptyPdfBytes, result.getByteArray());
        assertEquals(0, result.contentLength());
        
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(pdfRenderer).renderInvoiceToPdf(invoice);
    }

    @Test
    void generatePdf_ShouldHandleNullInvoiceId_Gracefully() {
        // Given
        Long nullInvoiceId = null;

        // When & Then
        Exception exception = assertThrows(Exception.class,
                () -> invoicePdfService.generatePdf(nullInvoiceId));
        
        // The actual exception type may vary based on repository implementation
        assertNotNull(exception);
        
        // Should still call repository, but it may handle null differently
        verify(invoiceRepository).findById(nullInvoiceId);
        verify(pdfRenderer, never()).renderInvoiceToPdf(any());
    }

    @Test
    void generatePdf_ShouldHandleLargePdfBytes_Correctly() {
        // Given
        Invoice invoice = createTestInvoice();
        byte[] largePdfBytes = new byte[1024 * 1024]; // 1MB mock PDF
        
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(pdfRenderer.renderInvoiceToPdf(invoice)).thenReturn(largePdfBytes);

        // When
        ByteArrayResource result = invoicePdfService.generatePdf(INVOICE_ID);

        // Then
        assertNotNull(result);
        assertEquals(largePdfBytes.length, result.contentLength());
        assertSame(largePdfBytes, result.getByteArray());
        
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(pdfRenderer).renderInvoiceToPdf(invoice);
    }

    @Test
    void generatePdf_ShouldPassCorrectInvoice_ToPdfRenderer() {
        // Given
        Invoice invoice = createTestInvoice();
        invoice.setInvoiceReference("INV-2024-001");
        invoice.setTotalAmount(BigDecimal.valueOf(1500.00));
        
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(pdfRenderer.renderInvoiceToPdf(invoice)).thenReturn(MOCK_PDF_BYTES);

        // When
        invoicePdfService.generatePdf(INVOICE_ID);

        // Then
        verify(pdfRenderer).renderInvoiceToPdf(argThat(inv -> 
            inv.getInvoiceReference().equals("INV-2024-001") &&
            inv.getTotalAmount().equals(BigDecimal.valueOf(1500.00))
        ));
    }

    @Test
    void generatePdf_ShouldHandleRepositoryExceptions_Appropriately() {
        // Given
        when(invoiceRepository.findById(INVOICE_ID))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> invoicePdfService.generatePdf(INVOICE_ID));
        
        assertEquals("Database connection error", exception.getMessage());
        
        verify(invoiceRepository).findById(INVOICE_ID);
        verify(pdfRenderer, never()).renderInvoiceToPdf(any());
    }

    @Test
    void generatePdf_ShouldCallDependencies_InCorrectOrder() {
        // Given
        Invoice invoice = createTestInvoice();
        
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(pdfRenderer.renderInvoiceToPdf(invoice)).thenReturn(MOCK_PDF_BYTES);

        // When
        invoicePdfService.generatePdf(INVOICE_ID);

        // Then - Verify order of operations
        var inOrder = inOrder(invoiceRepository, pdfRenderer);
        inOrder.verify(invoiceRepository).findById(INVOICE_ID);
        inOrder.verify(pdfRenderer).renderInvoiceToPdf(invoice);
    }

    // Helper methods
    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(INVOICE_ID);
        invoice.setInvoiceReference("INV-TEST-001");
        invoice.setTotalAmount(BigDecimal.valueOf(1000.00));
        invoice.setCreatedAt(LocalDateTime.now());
        return invoice;
    }
}