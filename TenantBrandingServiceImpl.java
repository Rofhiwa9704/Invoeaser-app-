package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.TenantBrandingDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.TenantBranding;
import co.za.kingstechco.kingstechco.invoeaserapp.exception.ResourceNotFoundException;
import co.za.kingstechco.kingstechco.invoeaserapp.mapper.dtoMapper.TenantBrandingMapper;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.TenantBrandingRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.service.TenantBrandingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static co.za.kingstechco.kingstechco.invoeaserapp.util.TemplateConstants.*;

/**
 * Implementation of TenantBrandingService for managing tenant branding configurations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TenantBrandingServiceImpl implements TenantBrandingService {

    private final TenantBrandingRepository brandingRepository;
    private final TenantBrandingMapper brandingMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantBrandingDto> getBrandingByTenantId(Long tenantId) {
        log.debug("Getting branding configuration for tenant: {}", tenantId);
        return brandingRepository.findByTenantId(tenantId)
                .map(brandingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantBrandingDto> getActiveBrandingByTenantId(Long tenantId) {
        log.debug("Getting active branding configuration for tenant: {}", tenantId);
        return brandingRepository.findByTenantIdAndActiveTrue(tenantId)
                .map(brandingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantBranding> getBrandingEntityByTenantId(Long tenantId) {
        log.debug("Getting branding entity for tenant: {}", tenantId);
        return brandingRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    @Override
    public TenantBrandingDto saveOrUpdateBranding(Long tenantId, TenantBrandingDto brandingDto) {
        log.info("Saving/updating branding configuration for tenant: {}", tenantId);
        
        brandingDto.setTenantId(tenantId);
        
        Optional<TenantBranding> existingBranding = brandingRepository.findByTenantId(tenantId);
        
        TenantBranding branding;
        if (existingBranding.isPresent()) {
            branding = existingBranding.get();
            brandingMapper.updateEntityFromDto(brandingDto, branding);
            log.debug("Updated existing branding configuration for tenant: {}", tenantId);
        } else {
            branding = brandingMapper.toEntity(brandingDto);
            log.debug("Created new branding configuration for tenant: {}", tenantId);
        }
        
        TenantBranding savedBranding = brandingRepository.save(branding);
        return brandingMapper.toDto(savedBranding);
    }

    @Override
    public TenantBrandingDto uploadLogo(Long tenantId, MultipartFile logoFile) {
        log.info("Uploading logo for tenant: {}", tenantId);
        
        validateLogoFile(logoFile);
        
        TenantBranding branding = brandingRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantBranding newBranding = TenantBranding.builder()
                            .tenantId(tenantId)
                            .active(true)
                            .build();
                    return newBranding;
                });
        
        try {
            branding.setLogoData(logoFile.getBytes());
            branding.setLogoFilename(logoFile.getOriginalFilename());
            branding.setLogoContentType(logoFile.getContentType());
            branding.setLogoUrl(null); // Clear URL when uploading file data
            
            TenantBranding savedBranding = brandingRepository.save(branding);
            log.info("Successfully uploaded logo for tenant: {}", tenantId);
            return brandingMapper.toDto(savedBranding);
            
        } catch (IOException e) {
            log.error("Failed to upload logo for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to upload logo file", e);
        }
    }

    @Override
    public TenantBrandingDto removeLogo(Long tenantId) {
        log.info("Removing logo for tenant: {}", tenantId);
        
        TenantBranding branding = brandingRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branding configuration not found for tenant: " + tenantId));
        
        branding.setLogoData(null);
        branding.setLogoFilename(null);
        branding.setLogoContentType(null);
        branding.setLogoUrl(null);
        
        TenantBranding savedBranding = brandingRepository.save(branding);
        log.info("Successfully removed logo for tenant: {}", tenantId);
        return brandingMapper.toDto(savedBranding);
    }

    @Override
    public void deleteBranding(Long tenantId) {
        log.info("Deleting branding configuration for tenant: {}", tenantId);
        brandingRepository.deleteByTenantId(tenantId);
        log.info("Successfully deleted branding configuration for tenant: {}", tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTenantId(Long tenantId) {
        return brandingRepository.existsByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantBrandingDto getDefaultBranding() {
        return TenantBrandingDto.builder()
                .primaryColor(PRIMARY_COLOR)
                .secondaryColor(SECONDARY_COLOR)
                .accentColor(ACCENT_COLOR)
                .headerBackgroundColor(HEADER_BACKGROUND_COLOR)
                .headerTextColor(HEADER_TEXT_COLOR)
                .fontFamily(ARIAL_SANS_SERIF)
                .logoWidth(LOGO_WIDTH)
                .logoHeight(LOGO_HEIGHT)
                .active(true)
                .hasLogo(true).build();
    }

    private void validateLogoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file cannot be empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Logo file size cannot exceed 5MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: JPEG, PNG, GIF, SVG");
        }
        
        log.debug("Logo file validation passed: {} ({})", file.getOriginalFilename(), contentType);
    }
}