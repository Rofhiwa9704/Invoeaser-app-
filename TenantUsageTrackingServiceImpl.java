package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.TenantUsageStatsDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UsageTrendDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.TenantUsageTracking;
import co.za.kingstechco.kingstechco.invoeaserapp.enums.UsageActionType;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.TenantUsageTrackingRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.service.TenantUsageTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of tenant usage tracking service.
 * Provides comprehensive usage tracking with optimized database operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TenantUsageTrackingServiceImpl implements TenantUsageTrackingService {

    private final TenantUsageTrackingRepository usageTrackingRepository;

    @Override
    public void recordUsage(Long tenantId, UsageActionType actionType) {
        recordUsage(tenantId, actionType, 1L, YearMonth.now());
    }

    @Override
    public void recordUsage(Long tenantId, UsageActionType actionType, long count) {
        recordUsage(tenantId, actionType, count, YearMonth.now());
    }

    @Override
    public void recordUsage(Long tenantId, UsageActionType actionType, YearMonth yearMonth) {
        recordUsage(tenantId, actionType, 1L, yearMonth);
    }

    @Override
    public void recordUsage(Long tenantId, UsageActionType actionType, long count, YearMonth yearMonth) {
        if (tenantId == null || actionType == null || count <= 0) {
            log.warn("Invalid usage recording parameters: tenantId={}, actionType={}, count={}", 
                    tenantId, actionType, count);
            return;
        }

        try {
            // Try atomic upsert first for better performance
            recordUsageAtomic(tenantId, actionType, count, yearMonth);
            
            log.debug("Recorded {} {} actions for tenant {} in period {}", 
                    count, actionType, tenantId, yearMonth);
                    
        } catch (Exception e) {
            // Fallback to JPA-based approach if atomic operation fails
            log.debug("Atomic usage recording failed, falling back to JPA approach: {}", e.getMessage());
            recordUsageWithJpa(tenantId, actionType, count, yearMonth);
        }
    }

    private void recordUsageAtomic(Long tenantId, UsageActionType actionType, long count, YearMonth yearMonth) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Use native query for atomic upsert
            usageTrackingRepository.incrementUsageAtomic(
                    tenantId, 
                    actionType.name(), 
                    yearMonth.getYear(), 
                    yearMonth.getMonthValue(), 
                    now
            );
            
            // If we need to increment by more than 1, do additional increments
            if (count > 1) {
                for (int i = 1; i < count; i++) {
                    usageTrackingRepository.incrementUsageAtomic(
                            tenantId, 
                            actionType.name(), 
                            yearMonth.getYear(), 
                            yearMonth.getMonthValue(), 
                            now
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Atomic usage recording failed for tenant {} action {} period {}: {}", 
                    tenantId, actionType, yearMonth, e.getMessage());
            throw e;
        }
    }

    private void recordUsageWithJpa(Long tenantId, UsageActionType actionType, long count, YearMonth yearMonth) {
        try {
            Optional<TenantUsageTracking> existingRecord = usageTrackingRepository
                    .findByTenantIdAndActionTypeAndPeriodYearAndPeriodMonth(
                            tenantId, actionType, yearMonth.getYear(), yearMonth.getMonthValue()
                    );

            if (existingRecord.isPresent()) {
                TenantUsageTracking tracking = existingRecord.get();
                tracking.incrementUsageBy(count);
                usageTrackingRepository.save(tracking);
                
                log.debug("Updated existing usage record: {} {} actions for tenant {} in {}", 
                        count, actionType, tenantId, yearMonth);
            } else {
                TenantUsageTracking newTracking = TenantUsageTracking.builder()
                        .tenantId(tenantId)
                        .actionType(actionType)
                        .periodYear(yearMonth.getYear())
                        .periodMonth(yearMonth.getMonthValue())
                        .usageCount(count)
                        .firstRecordedAt(LocalDateTime.now())
                        .lastRecordedAt(LocalDateTime.now())
                        .build();

                usageTrackingRepository.save(newTracking);
                
                log.debug("Created new usage record: {} {} actions for tenant {} in {}", 
                        count, actionType, tenantId, yearMonth);
            }
        } catch (DataIntegrityViolationException e) {
            // Handle race condition - try to update existing record
            log.debug("Concurrent creation detected, attempting update for tenant {} action {} period {}", 
                    tenantId, actionType, yearMonth);
            
            Optional<TenantUsageTracking> existingRecord = usageTrackingRepository
                    .findByTenantIdAndActionTypeAndPeriodYearAndPeriodMonth(
                            tenantId, actionType, yearMonth.getYear(), yearMonth.getMonthValue()
                    );
            
            if (existingRecord.isPresent()) {
                TenantUsageTracking tracking = existingRecord.get();
                tracking.incrementUsageBy(count);
                usageTrackingRepository.save(tracking);
            } else {
                log.error("Failed to find existing record after constraint violation for tenant {} action {} period {}", 
                        tenantId, actionType, yearMonth);
                throw e;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getCurrentMonthUsage(Long tenantId, UsageActionType actionType) {
        return getUsageForMonth(tenantId, actionType, YearMonth.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUsageForMonth(Long tenantId, UsageActionType actionType, YearMonth yearMonth) {
        Long usage = usageTrackingRepository.getUsageCountByTenantAndActionTypeAndPeriod(
                tenantId, actionType, yearMonth.getYear(), yearMonth.getMonthValue()
        );
        return usage != null ? usage : 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUsage(Long tenantId, UsageActionType actionType) {
        Long usage = usageTrackingRepository.getTotalUsageCountByTenantAndActionType(tenantId, actionType);
        return usage != null ? usage : 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUsageStatsDto getUsageStatsForMonth(Long tenantId, YearMonth yearMonth) {
        List<TenantUsageTracking> records = usageTrackingRepository
                .findByTenantIdAndPeriodYearAndPeriodMonth(
                        tenantId, yearMonth.getYear(), yearMonth.getMonthValue()
                );

        return buildUsageStatsDto(tenantId, yearMonth, records);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantUsageStatsDto> getUsageStatsForPeriod(Long tenantId, YearMonth startMonth, YearMonth endMonth) {
        List<TenantUsageTracking> records = usageTrackingRepository.findByTenantIdAndPeriodRange(
                tenantId,
                startMonth.getYear(), startMonth.getMonthValue(),
                endMonth.getYear(), endMonth.getMonthValue()
        );

        // Group by period
        Map<YearMonth, List<TenantUsageTracking>> recordsByPeriod = records.stream()
                .collect(Collectors.groupingBy(record -> 
                        YearMonth.of(record.getPeriodYear(), record.getPeriodMonth())
                ));

        return recordsByPeriod.entrySet().stream()
                .map(entry -> buildUsageStatsDto(tenantId, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TenantUsageStatsDto::getPeriod).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsageTrendDto> getUsageTrends(Long tenantId, UsageActionType actionType, int monthsBack) {
        List<Object[]> trendData = usageTrackingRepository.getUsageTrend(tenantId, actionType, monthsBack);
        
        List<UsageTrendDto> trends = new ArrayList<>();
        Long previousUsage = null;
        
        for (Object[] row : trendData) {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            Long usage = (Long) row[2];
            
            YearMonth period = YearMonth.of(year, month);
            Double growthRate = null;
            
            if (previousUsage != null) {
                if (previousUsage > 0) {
                    growthRate = ((usage.doubleValue() - previousUsage.doubleValue()) / previousUsage.doubleValue()) * 100.0;
                } else {
                    growthRate = usage > 0 ? 100.0 : 0.0;
                }
            }
            
            trends.add(UsageTrendDto.builder()
                    .tenantId(tenantId)
                    .actionType(actionType)
                    .period(period)
                    .usageCount(usage)
                    .growthRate(growthRate)
                    .previousPeriodUsage(previousUsage)
                    .build());
            
            previousUsage = usage;
        }
        
        return trends;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantUsageStatsDto> getAllUsageStats(Long tenantId) {
        List<TenantUsageTracking> records = usageTrackingRepository
                .findByTenantIdOrderByPeriodYearDescPeriodMonthDesc(tenantId);

        Map<YearMonth, List<TenantUsageTracking>> recordsByPeriod = records.stream()
                .collect(Collectors.groupingBy(record -> 
                        YearMonth.of(record.getPeriodYear(), record.getPeriodMonth())
                ));

        return recordsByPeriod.entrySet().stream()
                .map(entry -> buildUsageStatsDto(tenantId, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TenantUsageStatsDto::getPeriod).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UsageActionType, Long> getCurrentMonthUsageByActionType(Long tenantId) {
        YearMonth currentMonth = YearMonth.now();
        List<Object[]> stats = usageTrackingRepository.getUsageStatsByTenantAndPeriod(
                tenantId, currentMonth.getYear(), currentMonth.getMonthValue()
        );

        return stats.stream()
                .collect(Collectors.toMap(
                        row -> (UsageActionType) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantUsageStatsDto> getTopTenantsByUsage(UsageActionType actionType, YearMonth yearMonth, int limit) {
        List<Object[]> topTenants = usageTrackingRepository.getTopTenantsByUsage(
                actionType, yearMonth.getYear(), yearMonth.getMonthValue()
        );

        return topTenants.stream()
                .limit(limit)
                .map(row -> {
                    Long tenantId = (Long) row[0];
                    Long totalUsage = (Long) row[1];
                    
                    Map<UsageActionType, Long> usageMap = new HashMap<>();
                    usageMap.put(actionType, totalUsage);
                    
                    return TenantUsageStatsDto.builder()
                            .tenantId(tenantId)
                            .period(yearMonth)
                            .usageByActionType(usageMap)
                            .totalUsage(totalUsage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public int cleanupOldRecords(int retentionMonths) {
        YearMonth cutoffMonth = YearMonth.now().minusMonths(retentionMonths);
        int deletedCount = usageTrackingRepository.deleteOldRecords(
                cutoffMonth.getYear(), cutoffMonth.getMonthValue()
        );
        
        log.info("Cleaned up {} old usage tracking records before {}", deletedCount, cutoffMonth);
        return deletedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasExceededUsageLimit(Long tenantId, UsageActionType actionType, long limit) {
        return getCurrentMonthUsage(tenantId, actionType) > limit;
    }

    @Override
    @Transactional(readOnly = true)
    public double getUsagePercentage(Long tenantId, UsageActionType actionType, long limit) {
        if (limit <= 0) return 0.0;
        long currentUsage = getCurrentMonthUsage(tenantId, actionType);
        return (currentUsage / (double) limit) * 100.0;
    }

    private TenantUsageStatsDto buildUsageStatsDto(Long tenantId, YearMonth period, List<TenantUsageTracking> records) {
        Map<UsageActionType, Long> usageByActionType = records.stream()
                .collect(Collectors.toMap(
                        TenantUsageTracking::getActionType,
                        TenantUsageTracking::getUsageCount
                ));

        long totalUsage = usageByActionType.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        LocalDateTime firstRecorded = records.stream()
                .map(TenantUsageTracking::getFirstRecordedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime lastRecorded = records.stream()
                .map(TenantUsageTracking::getLastRecordedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return TenantUsageStatsDto.builder()
                .tenantId(tenantId)
                .period(period)
                .usageByActionType(usageByActionType)
                .totalUsage(totalUsage)
                .firstRecordedAt(firstRecorded)
                .lastRecordedAt(lastRecorded)
                .emailsSent(usageByActionType.getOrDefault(UsageActionType.EMAIL_SENT, 0L))
                .emailsFailed(usageByActionType.getOrDefault(UsageActionType.EMAIL_FAILED, 0L))
                .emailsRetried(usageByActionType.getOrDefault(UsageActionType.EMAIL_RETRY, 0L))
                .invoicesCreated(usageByActionType.getOrDefault(UsageActionType.INVOICE_CREATED, 0L))
                .invoicesUpdated(usageByActionType.getOrDefault(UsageActionType.INVOICE_UPDATED, 0L))
                .invoicesDeleted(usageByActionType.getOrDefault(UsageActionType.INVOICE_DELETED, 0L))
                .pdfsGenerated(usageByActionType.getOrDefault(UsageActionType.PDF_GENERATED, 0L))
                .pdfsRendered(usageByActionType.getOrDefault(UsageActionType.PDF_RENDERED, 0L))
                .apiCalls(usageByActionType.getOrDefault(UsageActionType.API_CALL, 0L))
                .storageUploads(usageByActionType.getOrDefault(UsageActionType.STORAGE_UPLOAD, 0L))
                .storageDownloads(usageByActionType.getOrDefault(UsageActionType.STORAGE_DOWNLOAD, 0L))
                .loginsSuccess(usageByActionType.getOrDefault(UsageActionType.LOGIN_SUCCESS, 0L))
                .loginsFailed(usageByActionType.getOrDefault(UsageActionType.LOGIN_FAILED, 0L))
                .reportsGenerated(usageByActionType.getOrDefault(UsageActionType.REPORT_GENERATED, 0L))
                .dataExports(usageByActionType.getOrDefault(UsageActionType.DATA_EXPORT, 0L))
                .build();
    }
}