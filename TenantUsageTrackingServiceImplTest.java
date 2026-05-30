package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.TenantUsageStatsDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UsageTrendDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.TenantUsageTracking;
import co.za.kingstechco.kingstechco.invoeaserapp.enums.UsageActionType;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.TenantUsageTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for TenantUsageTrackingServiceImpl.
 * Tests usage recording, retrieval, analytics, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class TenantUsageTrackingServiceImplTest {

    @Mock
    private TenantUsageTrackingRepository usageTrackingRepository;

    @InjectMocks
    private TenantUsageTrackingServiceImpl usageTrackingService;

    private static final Long TENANT_ID = 1L;
    private static final UsageActionType EMAIL_SENT = UsageActionType.EMAIL_SENT;
    private static final YearMonth CURRENT_MONTH = YearMonth.now();

    private TenantUsageTracking existingUsageRecord;

    @BeforeEach
    void setUp() {
        existingUsageRecord = TenantUsageTracking.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .actionType(EMAIL_SENT)
                .periodYear(CURRENT_MONTH.getYear())
                .periodMonth(CURRENT_MONTH.getMonthValue())
                .usageCount(5L)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .firstRecordedAt(LocalDateTime.now().minusDays(5))
                .lastRecordedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void recordUsage_ShouldCreateNewRecord_WhenRecordDoesNotExist() {
        // Act
        usageTrackingService.recordUsage(TENANT_ID, EMAIL_SENT);

        // Assert - Service should use atomic operation first
        verify(usageTrackingRepository).incrementUsageAtomic(
                eq(TENANT_ID), 
                eq(EMAIL_SENT.name()), 
                eq(CURRENT_MONTH.getYear()), 
                eq(CURRENT_MONTH.getMonthValue()),
                any(LocalDateTime.class));
        
        // Should not use JPA methods when atomic works
        verify(usageTrackingRepository, never()).findByTenantIdAndActionTypeAndPeriodYearAndPeriodMonth(
                any(), any(), anyInt(), anyInt());
        verify(usageTrackingRepository, never()).save(any(TenantUsageTracking.class));
    }

    @Test
    void recordUsage_ShouldUpdateExistingRecord_WhenRecordExists() {
        // Act
        usageTrackingService.recordUsage(TENANT_ID, EMAIL_SENT);

        // Assert - Service should use atomic operation (which handles both create and update)
        verify(usageTrackingRepository).incrementUsageAtomic(
                eq(TENANT_ID), 
                eq(EMAIL_SENT.name()), 
                eq(CURRENT_MONTH.getYear()), 
                eq(CURRENT_MONTH.getMonthValue()),
                any(LocalDateTime.class));
        
        // Should not use JPA methods when atomic works
        verify(usageTrackingRepository, never()).findByTenantIdAndActionTypeAndPeriodYearAndPeriodMonth(
                any(), any(), anyInt(), anyInt());
        verify(usageTrackingRepository, never()).save(any(TenantUsageTracking.class));
    }

    @Test
    void recordUsage_ShouldRecordMultipleUsages_WhenCountProvided() {
        // Act
        usageTrackingService.recordUsage(TENANT_ID, EMAIL_SENT, 3L);

        // Assert - Service should call atomic operation 3 times for count=3
        verify(usageTrackingRepository, times(3)).incrementUsageAtomic(
                eq(TENANT_ID), 
                eq(EMAIL_SENT.name()), 
                eq(CURRENT_MONTH.getYear()), 
                eq(CURRENT_MONTH.getMonthValue()),
                any(LocalDateTime.class));
        
        // Should not use JPA methods when atomic works
        verify(usageTrackingRepository, never()).findByTenantIdAndActionTypeAndPeriodYearAndPeriodMonth(
                any(), any(), anyInt(), anyInt());
        verify(usageTrackingRepository, never()).save(any(TenantUsageTracking.class));
    }

    @Test
    void recordUsage_ShouldHandleAtomicOperation_WhenSupported() {
        // Arrange
        doNothing().when(usageTrackingRepository).incrementUsageAtomic(
                eq(TENANT_ID), eq(EMAIL_SENT.name()), eq(CURRENT_MONTH.getYear()), 
                eq(CURRENT_MONTH.getMonthValue()), any(LocalDateTime.class));

        // Act
        usageTrackingService.recordUsage(TENANT_ID, EMAIL_SENT);

        // Assert
        verify(usageTrackingRepository).incrementUsageAtomic(
                eq(TENANT_ID), eq(EMAIL_SENT.name()), eq(CURRENT_MONTH.getYear()), 
                eq(CURRENT_MONTH.getMonthValue()), any(LocalDateTime.class));
    }

    @Test
    void recordUsage_ShouldIgnoreInvalidParameters() {
        // Act & Assert
        assertDoesNotThrow(() -> usageTrackingService.recordUsage(null, EMAIL_SENT));
        assertDoesNotThrow(() -> usageTrackingService.recordUsage(TENANT_ID, null));
        assertDoesNotThrow(() -> usageTrackingService.recordUsage(TENANT_ID, EMAIL_SENT, 0L));
        assertDoesNotThrow(() -> usageTrackingService.recordUsage(TENANT_ID, EMAIL_SENT, -1L));

        verify(usageTrackingRepository, never()).findByTenantIdAndActionTypeAndPeriodYearAndPeriodMonth(
                any(), any(), any(), any());
        verify(usageTrackingRepository, never()).save(any());
    }

    @Test
    void getCurrentMonthUsage_ShouldReturnUsageCount_WhenRecordExists() {
        // Arrange
        when(usageTrackingRepository.getUsageCountByTenantAndActionTypeAndPeriod(
                TENANT_ID, EMAIL_SENT, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue()))
                .thenReturn(10L);

        // Act
        long usage = usageTrackingService.getCurrentMonthUsage(TENANT_ID, EMAIL_SENT);

        // Assert
        assertEquals(10L, usage);
        verify(usageTrackingRepository).getUsageCountByTenantAndActionTypeAndPeriod(
                TENANT_ID, EMAIL_SENT, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue());
    }

    @Test
    void getCurrentMonthUsage_ShouldReturnZero_WhenNoRecordExists() {
        // Arrange
        when(usageTrackingRepository.getUsageCountByTenantAndActionTypeAndPeriod(
                TENANT_ID, EMAIL_SENT, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue()))
                .thenReturn(null);

        // Act
        long usage = usageTrackingService.getCurrentMonthUsage(TENANT_ID, EMAIL_SENT);

        // Assert
        assertEquals(0L, usage);
    }

    @Test
    void getTotalUsage_ShouldReturnTotalAcrossAllPeriods() {
        // Arrange
        when(usageTrackingRepository.getTotalUsageCountByTenantAndActionType(TENANT_ID, EMAIL_SENT))
                .thenReturn(100L);

        // Act
        long totalUsage = usageTrackingService.getTotalUsage(TENANT_ID, EMAIL_SENT);

        // Assert
        assertEquals(100L, totalUsage);
        verify(usageTrackingRepository).getTotalUsageCountByTenantAndActionType(TENANT_ID, EMAIL_SENT);
    }

    @Test
    void getUsageStatsForMonth_ShouldReturnStatsDto() {
        // Arrange
        List<TenantUsageTracking> records = List.of(
                existingUsageRecord,
                TenantUsageTracking.builder()
                        .tenantId(TENANT_ID)
                        .actionType(UsageActionType.INVOICE_CREATED)
                        .periodYear(CURRENT_MONTH.getYear())
                        .periodMonth(CURRENT_MONTH.getMonthValue())
                        .usageCount(3L)
                        .build()
        );

        when(usageTrackingRepository.findByTenantIdAndPeriodYearAndPeriodMonth(
                TENANT_ID, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue()))
                .thenReturn(records);

        // Act
        TenantUsageStatsDto stats = usageTrackingService.getUsageStatsForMonth(TENANT_ID, CURRENT_MONTH);

        // Assert
        assertNotNull(stats);
        assertEquals(TENANT_ID, stats.getTenantId());
        assertEquals(CURRENT_MONTH, stats.getPeriod());
        assertEquals(8L, stats.getTotalUsage()); // 5 + 3
        assertEquals(5L, stats.getEmailsSent());
        assertEquals(3L, stats.getInvoicesCreated());
    }

    @Test
    void getUsageTrends_ShouldReturnTrendAnalysis() {
        // Arrange
        List<Object[]> trendData = List.of(
                new Object[]{2024, 1, 10L}, // January: 10 usages
                new Object[]{2024, 2, 15L}, // February: 15 usages (50% growth)
                new Object[]{2024, 3, 12L}  // March: 12 usages (-20% decline)
        );

        when(usageTrackingRepository.getUsageTrend(TENANT_ID, EMAIL_SENT, 3))
                .thenReturn(trendData);

        // Act
        List<UsageTrendDto> trends = usageTrackingService.getUsageTrends(TENANT_ID, EMAIL_SENT, 3);

        // Assert
        assertNotNull(trends);
        assertEquals(3, trends.size());

        UsageTrendDto firstTrend = trends.get(0);
        assertEquals(TENANT_ID, firstTrend.getTenantId());
        assertEquals(EMAIL_SENT, firstTrend.getActionType());
        assertEquals(YearMonth.of(2024, 1), firstTrend.getPeriod());
        assertEquals(10L, firstTrend.getUsageCount());
        assertNull(firstTrend.getGrowthRate()); // First period has no previous data

        UsageTrendDto secondTrend = trends.get(1);
        assertEquals(50.0, secondTrend.getGrowthRate(), 0.1); // 50% growth

        UsageTrendDto thirdTrend = trends.get(2);
        assertEquals(-20.0, thirdTrend.getGrowthRate(), 0.1); // 20% decline
    }

    @Test
    void getCurrentMonthUsageByActionType_ShouldReturnUsageBreakdown() {
        // Arrange
        List<Object[]> stats = List.of(
                new Object[]{EMAIL_SENT, 10L},
                new Object[]{UsageActionType.INVOICE_CREATED, 5L},
                new Object[]{UsageActionType.PDF_GENERATED, 8L}
        );

        when(usageTrackingRepository.getUsageStatsByTenantAndPeriod(
                TENANT_ID, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue()))
                .thenReturn(stats);

        // Act
        Map<UsageActionType, Long> breakdown = usageTrackingService.getCurrentMonthUsageByActionType(TENANT_ID);

        // Assert
        assertNotNull(breakdown);
        assertEquals(3, breakdown.size());
        assertEquals(10L, breakdown.get(EMAIL_SENT));
        assertEquals(5L, breakdown.get(UsageActionType.INVOICE_CREATED));
        assertEquals(8L, breakdown.get(UsageActionType.PDF_GENERATED));
    }

    @Test
    void hasExceededUsageLimit_ShouldReturnTrue_WhenLimitExceeded() {
        // Arrange
        when(usageTrackingRepository.getUsageCountByTenantAndActionTypeAndPeriod(
                TENANT_ID, EMAIL_SENT, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue()))
                .thenReturn(150L);

        // Act
        boolean exceeded = usageTrackingService.hasExceededUsageLimit(TENANT_ID, EMAIL_SENT, 100L);

        // Assert
        assertTrue(exceeded);
    }

    @Test
    void hasExceededUsageLimit_ShouldReturnFalse_WhenWithinLimit() {
        // Arrange
        when(usageTrackingRepository.getUsageCountByTenantAndActionTypeAndPeriod(
                TENANT_ID, EMAIL_SENT, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue()))
                .thenReturn(50L);

        // Act
        boolean exceeded = usageTrackingService.hasExceededUsageLimit(TENANT_ID, EMAIL_SENT, 100L);

        // Assert
        assertFalse(exceeded);
    }

    @Test
    void getUsagePercentage_ShouldCalculateCorrectPercentage() {
        // Arrange
        when(usageTrackingRepository.getUsageCountByTenantAndActionTypeAndPeriod(
                TENANT_ID, EMAIL_SENT, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue()))
                .thenReturn(75L);

        // Act
        double percentage = usageTrackingService.getUsagePercentage(TENANT_ID, EMAIL_SENT, 100L);

        // Assert
        assertEquals(75.0, percentage, 0.1);
    }

    @Test
    void getUsagePercentage_ShouldReturnZero_WhenLimitIsZero() {
        // Act
        double percentage = usageTrackingService.getUsagePercentage(TENANT_ID, EMAIL_SENT, 0L);

        // Assert
        assertEquals(0.0, percentage, 0.1);
    }

    @Test
    void cleanupOldRecords_ShouldDeleteOldRecordsAndReturnCount() {
        // Arrange
        when(usageTrackingRepository.deleteOldRecords(any(Integer.class), any(Integer.class)))
                .thenReturn(25);

        // Act
        int deletedCount = usageTrackingService.cleanupOldRecords(12);

        // Assert
        assertEquals(25, deletedCount);
        verify(usageTrackingRepository).deleteOldRecords(any(Integer.class), any(Integer.class));
    }

    @Test
    void getTopTenantsByUsage_ShouldReturnTopTenants() {
        // Arrange
        List<Object[]> topTenants = List.of(
                new Object[]{1L, 100L},
                new Object[]{2L, 85L},
                new Object[]{3L, 70L}
        );

        when(usageTrackingRepository.getTopTenantsByUsage(
                EMAIL_SENT, CURRENT_MONTH.getYear(), CURRENT_MONTH.getMonthValue()))
                .thenReturn(topTenants);

        // Act
        List<TenantUsageStatsDto> result = usageTrackingService.getTopTenantsByUsage(EMAIL_SENT, CURRENT_MONTH, 10);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getTenantId());
        assertEquals(100L, result.get(0).getTotalUsage());
    }
}