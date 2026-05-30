package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.TenantBrandingDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.TenantBranding;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Service interface for managing tenant branding configurations.
 */
public interface TenantBrandingService {

    /**
     * Get branding configuration for a tenant.
     *
     * @param tenantId the tenant ID
     * @return Optional containing the branding DTO if found
     */
    Optional<TenantBrandingDto> getBrandingByTenantId(Long tenantId);

    /**
     * Get active branding configuration for a tenant.
     *
     * @param tenantId the tenant ID
     * @return Optional containing the active branding DTO if found
     */
    Optional<TenantBrandingDto> getActiveBrandingByTenantId(Long tenantId);

    /**
     * Get branding entity for PDF generation (internal use).
     *
     * @param tenantId the tenant ID
     * @return Optional containing the branding entity if found
     */
    Optional<TenantBranding> getBrandingEntityByTenantId(Long tenantId);

    /**
     * Create or update branding configuration for a tenant.
     *
     * @param tenantId   the tenant ID
     * @param brandingDto the branding configuration
     * @return the saved branding DTO
     */
    TenantBrandingDto saveOrUpdateBranding(Long tenantId, TenantBrandingDto brandingDto);

    /**
     * Upload logo file for a tenant.
     *
     * @param tenantId the tenant ID
     * @param logoFile the logo file
     * @return the updated branding DTO
     */
    TenantBrandingDto uploadLogo(Long tenantId, MultipartFile logoFile);

    /**
     * Remove logo from tenant branding.
     *
     * @param tenantId the tenant ID
     * @return the updated branding DTO
     */
    TenantBrandingDto removeLogo(Long tenantId);

    /**
     * Delete branding configuration for a tenant.
     *
     * @param tenantId the tenant ID
     */
    void deleteBranding(Long tenantId);

    /**
     * Check if branding configuration exists for a tenant.
     *
     * @param tenantId the tenant ID
     * @return true if branding exists, false otherwise
     */
    boolean existsByTenantId(Long tenantId);

    /**
     * Get default branding configuration.
     *
     * @return default branding DTO
     */
    TenantBrandingDto getDefaultBranding();
}