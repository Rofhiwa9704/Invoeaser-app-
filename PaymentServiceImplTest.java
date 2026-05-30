package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Payment;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentServiceImpl paymentService;

    private static final Long PAYMENT_ID = 1L;
    private static final BigDecimal PAYMENT_AMOUNT = BigDecimal.valueOf(1000.00);
    private static final LocalDateTime PAYMENT_DATE = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository);
    }

    @Test
    void createPayment_ShouldReturnSavedPayment_WhenValidPayment() {
        // Given
        Payment payment = createTestPayment();
        Payment savedPayment = createTestPayment();
        savedPayment.setPaymentId(PAYMENT_ID);
        
        when(paymentRepository.save(payment)).thenReturn(savedPayment);

        // When
        Payment result = paymentService.createPayment(payment);

        // Then
        assertNotNull(result);
        assertEquals(PAYMENT_ID, result.getPaymentId());
        assertEquals(PAYMENT_AMOUNT, result.getAmount());
        assertEquals(PAYMENT_DATE, result.getPaymentDate());
        
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPayment_ShouldHandleRepositoryExceptions_Gracefully() {
        // Given
        Payment payment = createTestPayment();
        when(paymentRepository.save(payment)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createPayment(payment));
        
        assertEquals("Database error", exception.getMessage());
        verify(paymentRepository).save(payment);
    }

    @Test
    void getAllPayments_ShouldReturnPaymentList_WhenPaymentsExist() {
        // Given
        List<Payment> expectedPayments = Arrays.asList(
                createTestPaymentWithId(1L, BigDecimal.valueOf(500.00)),
                createTestPaymentWithId(2L, BigDecimal.valueOf(750.00)),
                createTestPaymentWithId(3L, BigDecimal.valueOf(1200.00))
        );
        
        when(paymentRepository.findAll()).thenReturn(expectedPayments);

        // When
        List<Payment> result = paymentService.getAllPayments();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedPayments, result);
        
        // Verify amounts
        assertEquals(BigDecimal.valueOf(500.00), result.get(0).getAmount());
        assertEquals(BigDecimal.valueOf(750.00), result.get(1).getAmount());
        assertEquals(BigDecimal.valueOf(1200.00), result.get(2).getAmount());
        
        verify(paymentRepository).findAll();
    }

    @Test
    void getAllPayments_ShouldReturnEmptyList_WhenNoPaymentsExist() {
        // Given
        when(paymentRepository.findAll()).thenReturn(List.of());

        // When
        List<Payment> result = paymentService.getAllPayments();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(paymentRepository).findAll();
    }

    @Test
    void getPaymentById_ShouldReturnPayment_WhenPaymentExists() {
        // Given
        Payment expectedPayment = createTestPayment();
        expectedPayment.setPaymentId(PAYMENT_ID);
        
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(expectedPayment));

        // When
        Optional<Payment> result = paymentService.getPaymentById(PAYMENT_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedPayment, result.get());
        assertEquals(PAYMENT_ID, result.get().getPaymentId());
        assertEquals(PAYMENT_AMOUNT, result.get().getAmount());
        
        verify(paymentRepository).findById(PAYMENT_ID);
    }

    @Test
    void getPaymentById_ShouldReturnEmpty_WhenPaymentNotFound() {
        // Given
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        // When
        Optional<Payment> result = paymentService.getPaymentById(PAYMENT_ID);

        // Then
        assertFalse(result.isPresent());
        
        verify(paymentRepository).findById(PAYMENT_ID);
    }

    @Test
    void updatePayment_ShouldReturnUpdatedPayment_WhenPaymentExists() {
        // Given
        Payment updatedPaymentData = new Payment();
        updatedPaymentData.setAmount(BigDecimal.valueOf(1500.00));
        updatedPaymentData.setPaymentDate(LocalDateTime.now().plusDays(1));
        
        Payment expectedUpdatedPayment = new Payment();
        expectedUpdatedPayment.setPaymentId(PAYMENT_ID);
        expectedUpdatedPayment.setAmount(BigDecimal.valueOf(1500.00));
        expectedUpdatedPayment.setPaymentDate(LocalDateTime.now().plusDays(1));
        
        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedUpdatedPayment);

        // When
        Payment result = paymentService.updatePayment(PAYMENT_ID, updatedPaymentData);

        // Then
        assertNotNull(result);
        assertEquals(PAYMENT_ID, result.getPaymentId());
        assertEquals(BigDecimal.valueOf(1500.00), result.getAmount());
        
        verify(paymentRepository).existsById(PAYMENT_ID);
        verify(paymentRepository).save(any(Payment.class));
        
        // Verify that the payment ID was set on the payment object
        assertEquals(PAYMENT_ID, updatedPaymentData.getPaymentId());
    }

    @Test
    void updatePayment_ShouldThrowException_WhenPaymentNotFound() {
        // Given
        Payment updatedPaymentData = new Payment();
        updatedPaymentData.setAmount(BigDecimal.valueOf(1500.00));
        
        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentService.updatePayment(PAYMENT_ID, updatedPaymentData));
        
        assertEquals("Payment not found with ID: " + PAYMENT_ID, exception.getMessage());
        
        verify(paymentRepository).existsById(PAYMENT_ID);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updatePayment_ShouldSetPaymentId_OnProvidedPayment() {
        // Given
        Payment updatedPaymentData = new Payment();
        updatedPaymentData.setAmount(BigDecimal.valueOf(2000.00));
        
        Payment savedPayment = new Payment();
        savedPayment.setPaymentId(PAYMENT_ID);
        savedPayment.setAmount(BigDecimal.valueOf(2000.00));
        
        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(true);
        when(paymentRepository.save(updatedPaymentData)).thenReturn(savedPayment);

        // When
        Payment result = paymentService.updatePayment(PAYMENT_ID, updatedPaymentData);

        // Then
        assertEquals(PAYMENT_ID, updatedPaymentData.getPaymentId());
        assertEquals(PAYMENT_ID, result.getPaymentId());
        
        verify(paymentRepository).save(updatedPaymentData);
    }

    @Test
    void deletePayment_ShouldReturnTrue_WhenPaymentExists() {
        // Given
        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(true);

        // When
        boolean result = paymentService.deletePayment(PAYMENT_ID);

        // Then
        assertTrue(result);
        
        verify(paymentRepository).existsById(PAYMENT_ID);
        verify(paymentRepository).deleteById(PAYMENT_ID);
    }

    @Test
    void deletePayment_ShouldReturnFalse_WhenPaymentNotFound() {
        // Given
        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(false);

        // When
        boolean result = paymentService.deletePayment(PAYMENT_ID);

        // Then
        assertFalse(result);
        
        verify(paymentRepository).existsById(PAYMENT_ID);
        verify(paymentRepository, never()).deleteById(any());
    }

    @Test
    void deletePayment_ShouldHandleRepositoryExceptions_WhenDeletionFails() {
        // Given
        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(true);
        doThrow(new RuntimeException("Foreign key constraint violation"))
                .when(paymentRepository).deleteById(PAYMENT_ID);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.deletePayment(PAYMENT_ID));
        
        assertEquals("Foreign key constraint violation", exception.getMessage());
        
        verify(paymentRepository).existsById(PAYMENT_ID);
        verify(paymentRepository).deleteById(PAYMENT_ID);
    }

    @Test
    void createPayment_ShouldHandleNullAmount_Gracefully() {
        // Given
        Payment payment = createTestPayment();
        payment.setAmount(null);
        
        Payment savedPayment = createTestPayment();
        savedPayment.setPaymentId(PAYMENT_ID);
        savedPayment.setAmount(null);
        
        when(paymentRepository.save(payment)).thenReturn(savedPayment);

        // When
        Payment result = paymentService.createPayment(payment);

        // Then
        assertNotNull(result);
        assertNull(result.getAmount());
        
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPayment_ShouldHandleZeroAmount_Appropriately() {
        // Given
        Payment payment = createTestPayment();
        payment.setAmount(BigDecimal.ZERO);
        
        Payment savedPayment = createTestPayment();
        savedPayment.setPaymentId(PAYMENT_ID);
        savedPayment.setAmount(BigDecimal.ZERO);
        
        when(paymentRepository.save(payment)).thenReturn(savedPayment);

        // When
        Payment result = paymentService.createPayment(payment);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getAmount());
        
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPayment_ShouldHandleLargeAmounts_Correctly() {
        // Given
        BigDecimal largeAmount = new BigDecimal("999999999.99");
        Payment payment = createTestPayment();
        payment.setAmount(largeAmount);
        
        Payment savedPayment = createTestPayment();
        savedPayment.setPaymentId(PAYMENT_ID);
        savedPayment.setAmount(largeAmount);
        
        when(paymentRepository.save(payment)).thenReturn(savedPayment);

        // When
        Payment result = paymentService.createPayment(payment);

        // Then
        assertNotNull(result);
        assertEquals(largeAmount, result.getAmount());
        
        verify(paymentRepository).save(payment);
    }

    @Test
    void updatePayment_ShouldHandleRepositoryExceptions_WhenSaveFails() {
        // Given
        Payment updatedPaymentData = new Payment();
        updatedPaymentData.setAmount(BigDecimal.valueOf(1500.00));
        
        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenThrow(new RuntimeException("Database constraint error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.updatePayment(PAYMENT_ID, updatedPaymentData));
        
        assertEquals("Database constraint error", exception.getMessage());
        
        verify(paymentRepository).existsById(PAYMENT_ID);
        verify(paymentRepository).save(any(Payment.class));
    }

    // Helper methods
    private Payment createTestPayment() {
        Payment payment = new Payment();
        payment.setAmount(PAYMENT_AMOUNT);
        payment.setPaymentDate(PAYMENT_DATE);
        return payment;
    }

    private Payment createTestPaymentWithId(Long id, BigDecimal amount) {
        Payment payment = createTestPayment();
        payment.setPaymentId(id);
        payment.setAmount(amount);
        return payment;
    }
}