package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.EmailRetryLogDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.EmailRetryStatsDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.EmailRetryLog;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.EmailSendAudit;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.EmailRetryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailRetryLogServiceImplTest {

    @Mock
    private EmailRetryLogRepository emailRetryLogRepository;

    @InjectMocks
    private EmailRetryLogServiceImpl emailRetryLogService;

    private EmailSendAudit mockAudit;
    private EmailRetryLog mockRetryLog;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        mockAudit = new EmailSendAudit();
        mockAudit.setId(1L);
        mockAudit.setTenantId(100L);
        mockAudit.setInvoiceId(200L);
        mockAudit.setRecipientEmail("test@example.com");
        mockAudit.setErrorMessage("Original error");

        mockRetryLog = EmailRetryLog.builder()
                .id(1L)
                .tenantId(100L)
                .originalAuditId(1L)
                .invoiceId(200L)
                .recipientEmail("test@example.com")
                .retryTimestamp(now)
                .attemptCount(1)
                .retryStatus(EmailRetryLog.RetryStatus.INITIATED)
                .retryTrigger("MANUAL")
                .retryReason("Test retry")
                .originalFailureReason("Original error")
                .build();
    }

    @Test
    void logRetryInitiation_FirstAttempt_ShouldCreateRetryLogWithAttemptCount1() {
        when(emailRetryLogRepository.findByOriginalAuditIdOrderByRetryTimestampDesc(1L))
                .thenReturn(List.of());
        when(emailRetryLogRepository.save(any(EmailRetryLog.class)))
                .thenReturn(mockRetryLog);

        EmailRetryLog result = emailRetryLogService.logRetryInitiation(
                mockAudit, "MANUAL", "Test retry", 123L);

        assertNotNull(result);
        assertEquals(1, result.getAttemptCount());
        assertEquals("MANUAL", result.getRetryTrigger());
        assertEquals("Test retry", result.getRetryReason());
        assertEquals(EmailRetryLog.RetryStatus.INITIATED, result.getRetryStatus());

        verify(emailRetryLogRepository).save(argThat(retryLog -> 
                retryLog.getAttemptCount() == 1 &&
                retryLog.getTenantId().equals(100L) &&
                retryLog.getOriginalAuditId().equals(1L)));
    }

    @Test
    void logRetryInitiation_SecondAttempt_ShouldCreateRetryLogWithAttemptCount2() {
        EmailRetryLog existingRetry = EmailRetryLog.builder()
                .attemptCount(1)
                .retryTimestamp(now.minusMinutes(5))
                .build();

        when(emailRetryLogRepository.findByOriginalAuditIdOrderByRetryTimestampDesc(1L))
                .thenReturn(List.of(existingRetry));
        
        EmailRetryLog secondAttempt = EmailRetryLog.builder()
                .id(2L)
                .attemptCount(2)
                .build();
        when(emailRetryLogRepository.save(any(EmailRetryLog.class)))
                .thenReturn(secondAttempt);

        EmailRetryLog result = emailRetryLogService.logRetryInitiation(
                mockAudit, "SCHEDULED", "Scheduled retry", null);

        assertEquals(2, result.getAttemptCount());
        
        verify(emailRetryLogRepository).save(argThat(retryLog -> 
                retryLog.getAttemptCount() == 2));
    }

    @Test
    void updateRetryResult_ValidId_ShouldUpdateRetryLog() {
        when(emailRetryLogRepository.findById(1L))
                .thenReturn(Optional.of(mockRetryLog));

        emailRetryLogService.updateRetryResult(1L, EmailRetryLog.RetryStatus.SUCCESS, 
                null, now.minusSeconds(30));

        verify(emailRetryLogRepository).save(mockRetryLog);
        verify(emailRetryLogRepository).findById(1L);
    }

    @Test
    void updateRetryResult_InvalidId_ShouldNotThrowException() {
        when(emailRetryLogRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> 
                emailRetryLogService.updateRetryResult(999L, EmailRetryLog.RetryStatus.FAILED, 
                        "Error", now.minusSeconds(30)));

        verify(emailRetryLogRepository, never()).save(any());
    }

    @Test
    void getRetryLogsByTenant_ShouldReturnPagedResults() {
        List<EmailRetryLog> retryLogs = List.of(mockRetryLog);
        Page<EmailRetryLog> page = new PageImpl<>(retryLogs);
        Pageable pageable = PageRequest.of(0, 10);

        when(emailRetryLogRepository.findByTenantIdOrderByRetryTimestampDesc(100L, pageable))
                .thenReturn(page);

        Page<EmailRetryLogDto> result = emailRetryLogService.getRetryLogsByTenant(100L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(100L, result.getContent().get(0).getTenantId());

        verify(emailRetryLogRepository).findByTenantIdOrderByRetryTimestampDesc(100L, pageable);
    }

    @Test
    void getRetryLogsByTenantWithFilters_WithDateRange_ShouldApplyFilters() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();
        List<EmailRetryLog> retryLogs = List.of(mockRetryLog);
        Page<EmailRetryLog> page = new PageImpl<>(retryLogs);
        Pageable pageable = PageRequest.of(0, 10);

        when(emailRetryLogRepository.findByTenantIdAndStatusAndDateRange(
                eq(100L), eq(EmailRetryLog.RetryStatus.SUCCESS), any(), any(), eq(pageable)))
                .thenReturn(page);

        Page<EmailRetryLogDto> result = emailRetryLogService.getRetryLogsByTenantWithFilters(
                100L, EmailRetryLog.RetryStatus.SUCCESS, from, to, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        verify(emailRetryLogRepository).findByTenantIdAndStatusAndDateRange(
                eq(100L), eq(EmailRetryLog.RetryStatus.SUCCESS), any(), any(), eq(pageable));
    }

    @Test
    void getAllRetryLogsByTenant_ShouldReturnAllLogs() {
        List<EmailRetryLog> retryLogs = Arrays.asList(mockRetryLog, mockRetryLog);
        when(emailRetryLogRepository.findByTenantIdOrderByRetryTimestampDesc(100L))
                .thenReturn(retryLogs);

        List<EmailRetryLogDto> result = emailRetryLogService.getAllRetryLogsByTenant(100L);

        assertNotNull(result);
        assertEquals(2, result.size());
        
        verify(emailRetryLogRepository).findByTenantIdOrderByRetryTimestampDesc(100L);
    }

    @Test
    void getRetryLogsByOriginalAudit_ShouldFilterByTenant() {
        List<EmailRetryLog> retryLogs = List.of(mockRetryLog);
        when(emailRetryLogRepository.findByOriginalAuditIdOrderByRetryTimestampDesc(1L))
                .thenReturn(retryLogs);

        List<EmailRetryLogDto> result = emailRetryLogService.getRetryLogsByOriginalAudit(100L, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getTenantId());

        verify(emailRetryLogRepository).findByOriginalAuditIdOrderByRetryTimestampDesc(1L);
    }

    @Test
    void getRetryLogsByOriginalAudit_DifferentTenant_ShouldFilterOut() {
        List<EmailRetryLog> retryLogs = List.of(mockRetryLog);
        when(emailRetryLogRepository.findByOriginalAuditIdOrderByRetryTimestampDesc(1L))
                .thenReturn(retryLogs);

        List<EmailRetryLogDto> result = emailRetryLogService.getRetryLogsByOriginalAudit(999L, 1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRetryLogsByInvoice_ShouldReturnCorrectLogs() {
        List<EmailRetryLog> retryLogs = List.of(mockRetryLog);
        when(emailRetryLogRepository.findByTenantIdAndInvoiceIdOrderByRetryTimestampDesc(100L, 200L))
                .thenReturn(retryLogs);

        List<EmailRetryLogDto> result = emailRetryLogService.getRetryLogsByInvoice(100L, 200L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getInvoiceId());

        verify(emailRetryLogRepository).findByTenantIdAndInvoiceIdOrderByRetryTimestampDesc(100L, 200L);
    }

    @Test
    void getRetryLogsByRecipient_ShouldReturnCorrectLogs() {
        List<EmailRetryLog> retryLogs = List.of(mockRetryLog);
        when(emailRetryLogRepository.findByTenantIdAndRecipientEmailOrderByRetryTimestampDesc(
                100L, "test@example.com"))
                .thenReturn(retryLogs);

        List<EmailRetryLogDto> result = emailRetryLogService.getRetryLogsByRecipient(
                100L, "test@example.com");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getRecipientEmail());

        verify(emailRetryLogRepository).findByTenantIdAndRecipientEmailOrderByRetryTimestampDesc(
                100L, "test@example.com");
    }

    @Test
    void getRetryStatsByTenant_ShouldCalculateCorrectStats() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        Object[] successRow = new Object[]{EmailRetryLog.RetryStatus.SUCCESS, 5L, 2000.0};
        Object[] failedRow = new Object[]{EmailRetryLog.RetryStatus.FAILED, 3L, 1500.0};
        List<Object[]> statusStats = Arrays.asList(successRow, failedRow);

        Object[] multipleRetryRow = new Object[]{1L, 2L};
        List<Object[]> multipleRetries = new ArrayList<>();
        multipleRetries.add(multipleRetryRow);

        when(emailRetryLogRepository.getRetryStatsByStatus(eq(100L), any(), any()))
                .thenReturn(statusStats);
        when(emailRetryLogRepository.getMultipleRetryAttempts(100L))
                .thenReturn(multipleRetries);

        EmailRetryStatsDto result = emailRetryLogService.getRetryStatsByTenant(100L, from, to);

        assertNotNull(result);
        assertEquals(100L, result.getTenantId());
        assertEquals(8L, result.getTotalRetries());
        assertEquals(5L, result.getSuccessfulRetries());
        assertEquals(3L, result.getFailedRetries());
        assertEquals(1L, result.getUniqueEmailsWithRetries());
        assertEquals(1L, result.getEmailsWithMultipleRetries());

        verify(emailRetryLogRepository).getRetryStatsByStatus(eq(100L), any(), any());
        verify(emailRetryLogRepository).getMultipleRetryAttempts(100L);
    }

    @Test
    void getEmailsWithMultipleRetries_ShouldReturnOnlyMultipleRetries() {
        Object[] singleRetryRow = {1L, 1L};
        Object[] multipleRetryRow = {2L, 3L};
        List<Object[]> multipleRetries = List.of(singleRetryRow, multipleRetryRow);

        EmailRetryLog retryLogForMultiple = EmailRetryLog.builder()
                .id(2L)
                .originalAuditId(2L)
                .attemptCount(3)
                .build();

        when(emailRetryLogRepository.getMultipleRetryAttempts(100L))
                .thenReturn(multipleRetries);
        when(emailRetryLogRepository.findByOriginalAuditIdOrderByRetryTimestampDesc(2L))
                .thenReturn(List.of(retryLogForMultiple));

        List<EmailRetryLogDto> result = emailRetryLogService.getEmailsWithMultipleRetries(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getOriginalAuditId());

        verify(emailRetryLogRepository).getMultipleRetryAttempts(100L);
    }

    @Test
    void getRecentFailedRetries_ShouldReturnRecentFailures() {
        List<EmailRetryLog> failedRetries = List.of(mockRetryLog);
        when(emailRetryLogRepository.findRecentFailedRetries(eq(100L), any()))
                .thenReturn(failedRetries);

        List<EmailRetryLogDto> result = emailRetryLogService.getRecentFailedRetries(100L, 24);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(emailRetryLogRepository).findRecentFailedRetries(eq(100L), any());
    }

    @Test
    void getRetryLogsByTrigger_ShouldReturnFilteredResults() {
        List<EmailRetryLog> retryLogs = List.of(mockRetryLog);
        Page<EmailRetryLog> page = new PageImpl<>(retryLogs);
        Pageable pageable = PageRequest.of(0, 10);

        when(emailRetryLogRepository.findByTenantIdAndRetryTriggerOrderByRetryTimestampDesc(
                100L, "MANUAL", pageable))
                .thenReturn(page);

        Page<EmailRetryLogDto> result = emailRetryLogService.getRetryLogsByTrigger(
                100L, "MANUAL", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        verify(emailRetryLogRepository).findByTenantIdAndRetryTriggerOrderByRetryTimestampDesc(
                100L, "MANUAL", pageable);
    }

    @Test
    void getRetryCountByStatus_ShouldReturnCorrectCount() {
        when(emailRetryLogRepository.countByTenantIdAndRetryStatus(100L, EmailRetryLog.RetryStatus.SUCCESS))
                .thenReturn(5L);

        long result = emailRetryLogService.getRetryCountByStatus(100L, EmailRetryLog.RetryStatus.SUCCESS);

        assertEquals(5L, result);
        verify(emailRetryLogRepository).countByTenantIdAndRetryStatus(100L, EmailRetryLog.RetryStatus.SUCCESS);
    }

    @Test
    void getTotalRetryCount_ShouldReturnCorrectCount() {
        when(emailRetryLogRepository.countByTenantId(100L))
                .thenReturn(10L);

        long result = emailRetryLogService.getTotalRetryCount(100L);

        assertEquals(10L, result);
        verify(emailRetryLogRepository).countByTenantId(100L);
    }

    @Test
    void cleanupOldRetryLogs_ShouldDeleteOldLogs() {
        EmailRetryLog oldLog = EmailRetryLog.builder()
                .id(1L)
                .retryTimestamp(LocalDateTime.now().minusDays(35)) // Older than 30 days
                .build();
        
        List<EmailRetryLog> oldLogs = List.of(oldLog);
        when(emailRetryLogRepository.findAll())
                .thenReturn(oldLogs);

        int result = emailRetryLogService.cleanupOldRetryLogs(30);

        assertEquals(1, result);
        verify(emailRetryLogRepository).deleteByRetryTimestampBefore(any());
    }
}