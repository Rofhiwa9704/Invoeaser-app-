package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.TenantUsageStatsDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UsageTrendDto;
import co.za.kingstechco.kingstechco.invoeaserapp.enums.UsageActionType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Service interface for tracking tenant usage across different actions and time periods.
 * Provides methods for incrementing usage counters and retrieving usage statistics.
 */
public interface TenantUsageTrackingService {

    /**
     * Records a single usage action for a tenant in the current month.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action being tracked
     */
    void recordUsage(Long tenantId, UsageActionType actionType);

    /**
     * Records multiple usage actions for a tenant in the current month.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action being tracked
     * @param count the number of actions to record
     */
    void recordUsage(Long tenantId, UsageActionType actionType, long count);

    /**
     * Records a usage action for a tenant in a specific month.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action being tracked
     * @param yearMonth the specific month to record usage for
     */
    void recordUsage(Long tenantId, UsageActionType actionType, YearMonth yearMonth);

    /**
     * Records multiple usage actions for a tenant in a specific month.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action being tracked
     * @param count the number of actions to record
     * @param yearMonth the specific month to record usage for
     */
    void recordUsage(Long tenantId, UsageActionType actionType, long count, YearMonth yearMonth);

    /**
     * Gets the current month's usage count for a tenant and action type.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action
     * @return the usage count for the current month
     */
    long getCurrentMonthUsage(Long tenantId, UsageActionType actionType);

    /**
     * Gets usage count for a tenant and action type in a specific month.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action
     * @param yearMonth the specific month
     * @return the usage count for the specified month
     */
    long getUsageForMonth(Long tenantId, UsageActionType actionType, YearMonth yearMonth);

    /**
     * Gets total usage count for a tenant and action type across all periods.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action
     * @return the total usage count across all periods
     */
    long getTotalUsage(Long tenantId, UsageActionType actionType);

    /**
     * Gets comprehensive usage statistics for a tenant in a specific month.
     *
     * @param tenantId the tenant ID
     * @param yearMonth the specific month
     * @return usage statistics grouped by action type
     */
    TenantUsageStatsDto getUsageStatsForMonth(Long tenantId, YearMonth yearMonth);

    /**
     * Gets comprehensive usage statistics for a tenant across multiple months.
     *
     * @param tenantId the tenant ID
     * @param startMonth the start month (inclusive)
     * @param endMonth the end month (inclusive)
     * @return usage statistics for the specified period
     */
    List<TenantUsageStatsDto> getUsageStatsForPeriod(Long tenantId, YearMonth startMonth, YearMonth endMonth);

    /**
     * Gets usage trends for a tenant and action type over recent months.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action
     * @param monthsBack the number of months to look back
     * @return usage trends showing monthly progression
     */
    List<UsageTrendDto> getUsageTrends(Long tenantId, UsageActionType actionType, int monthsBack);

    /**
     * Gets all usage statistics for a tenant across all periods and action types.
     *
     * @param tenantId the tenant ID
     * @return complete usage history for the tenant
     */
    List<TenantUsageStatsDto> getAllUsageStats(Long tenantId);

    /**
     * Gets aggregated usage across all action types for a tenant in the current month.
     *
     * @param tenantId the tenant ID
     * @return map of action types to usage counts for current month
     */
    Map<UsageActionType, Long> getCurrentMonthUsageByActionType(Long tenantId);

    /**
     * Gets top tenants by usage for a specific action type and month.
     *
     * @param actionType the type of action
     * @param yearMonth the specific month
     * @param limit the maximum number of tenants to return
     * @return list of tenant IDs and their usage counts, ordered by usage descending
     */
    List<TenantUsageStatsDto> getTopTenantsByUsage(UsageActionType actionType, YearMonth yearMonth, int limit);

    /**
     * Cleans up old usage tracking records beyond a certain retention period.
     *
     * @param retentionMonths the number of months to retain
     * @return the number of records deleted
     */
    int cleanupOldRecords(int retentionMonths);

    /**
     * Checks if a tenant has exceeded their usage limits for a specific action type.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action
     * @param limit the usage limit to check against
     * @return true if usage exceeds the limit, false otherwise
     */
    boolean hasExceededUsageLimit(Long tenantId, UsageActionType actionType, long limit);

    /**
     * Gets usage percentage for a tenant against a specified limit.
     *
     * @param tenantId the tenant ID
     * @param actionType the type of action
     * @param limit the usage limit
     * @return percentage of limit used (0-100+)
     */
    double getUsagePercentage(Long tenantId, UsageActionType actionType, long limit);
}