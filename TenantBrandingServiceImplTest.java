package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.TenantBrandingDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.TenantBranding;
import co.za.kingstechco.kingstechco.invoeaserapp.mapper.dtoMapper.TenantBrandingMapper;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.TenantBrandingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantBrandingServiceImplTest {

    @Mock
    private TenantBrandingRepository brandingRepository;

    @Mock
    private TenantBrandingMapper brandingMapper;

    @InjectMocks
    private TenantBrandingServiceImpl brandingService;

    private TenantBranding testBranding;
    private TenantBrandingDto testBrandingDto;
    private Long testTenantId = 1L;

    @BeforeEach
    void setUp() {
        testBranding = TenantBranding.builder()
                .brandingId(1L)
                .tenantId(testTenantId)
                .primaryColor("#007bff")
                .secondaryColor("#6c757d")
                .accentColor("#28a745")
                .headerBackgroundColor("#f8f9fa")
                .headerTextColor("#212529")
                .fontFamily("Arial, sans-serif")
                .logoWidth(150)
                .logoHeight(60)
                .active(true)
                .build();

        testBrandingDto = TenantBrandingDto.builder()
                .brandingId(1L)
                .tenantId(testTenantId)
                .primaryColor("#007bff")
                .secondaryColor("#6c757d")
                .accentColor("#28a745")
                .headerBackgroundColor("#f8f9fa")
                .headerTextColor("#212529")
                .fontFamily("Arial, sans-serif")
                .logoWidth(150)
                .logoHeight(60)
                .active(true)
                .build();
    }

    @Test
    void getBrandingByTenantId_ShouldReturnBranding_WhenExists() {
        // Arrange
        when(brandingRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(testBranding));
        when(brandingMapper.toDto(testBranding)).thenReturn(testBrandingDto);

        // Act
        Optional<TenantBrandingDto> result = brandingService.getBrandingByTenantId(testTenantId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testBrandingDto, result.get());
        verify(brandingRepository).findByTenantId(testTenantId);
        verify(brandingMapper).toDto(testBranding);
    }

    @Test
    void getBrandingByTenantId_ShouldReturnEmpty_WhenNotExists() {
        // Arrange
        when(brandingRepository.findByTenantId(testTenantId)).thenReturn(Optional.empty());

        // Act
        Optional<TenantBrandingDto> result = brandingService.getBrandingByTenantId(testTenantId);

        // Assert
        assertFalse(result.isPresent());
        verify(brandingRepository).findByTenantId(testTenantId);
        verify(brandingMapper, never()).toDto(any());
    }

    @Test
    void saveOrUpdateBranding_ShouldCreateNew_WhenNotExists() {
        // Arrange
        when(brandingRepository.findByTenantId(testTenantId)).thenReturn(Optional.empty());
        when(brandingMapper.toEntity(testBrandingDto)).thenReturn(testBranding);
        when(brandingRepository.save(testBranding)).thenReturn(testBranding);
        when(brandingMapper.toDto(testBranding)).thenReturn(testBrandingDto);

        // Act
        TenantBrandingDto result = brandingService.saveOrUpdateBranding(testTenantId, testBrandingDto);

        // Assert
        assertNotNull(result);
        assertEquals(testBrandingDto, result);
        verify(brandingRepository).findByTenantId(testTenantId);
        verify(brandingMapper).toEntity(testBrandingDto);
        verify(brandingRepository).save(testBranding);
        verify(brandingMapper).toDto(testBranding);
    }

    @Test
    void saveOrUpdateBranding_ShouldUpdateExisting_WhenExists() {
        // Arrange
        when(brandingRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(testBranding));
        when(brandingRepository.save(testBranding)).thenReturn(testBranding);
        when(brandingMapper.toDto(testBranding)).thenReturn(testBrandingDto);

        // Act
        TenantBrandingDto result = brandingService.saveOrUpdateBranding(testTenantId, testBrandingDto);

        // Assert
        assertNotNull(result);
        assertEquals(testBrandingDto, result);
        verify(brandingRepository).findByTenantId(testTenantId);
        verify(brandingMapper).updateEntityFromDto(testBrandingDto, testBranding);
        verify(brandingRepository).save(testBranding);
        verify(brandingMapper).toDto(testBranding);
    }

    @Test
    void uploadLogo_ShouldSaveLogo_WhenValidFile() {
        // Arrange
        MockMultipartFile logoFile = new MockMultipartFile(
                "logo", "test-logo.png", "image/png", "test image data".getBytes());
        
        when(brandingRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(testBranding));
        when(brandingRepository.save(any(TenantBranding.class))).thenReturn(testBranding);
        when(brandingMapper.toDto(testBranding)).thenReturn(testBrandingDto);

        // Act
        TenantBrandingDto result = brandingService.uploadLogo(testTenantId, logoFile);

        // Assert
        assertNotNull(result);
        verify(brandingRepository).save(any(TenantBranding.class));
        verify(brandingMapper).toDto(testBranding);
    }

    @Test
    void uploadLogo_ShouldThrowException_WhenFileIsEmpty() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "logo", "empty.png", "image/png", new byte[0]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> brandingService.uploadLogo(testTenantId, emptyFile));
    }

    @Test
    void uploadLogo_ShouldThrowException_WhenFileIsTooLarge() {
        // Arrange
        byte[] largeData = new byte[6 * 1024 * 1024]; // 6MB (exceeds 5MB limit)
        MockMultipartFile largeFile = new MockMultipartFile(
                "logo", "large.png", "image/png", largeData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> brandingService.uploadLogo(testTenantId, largeFile));
    }

    @Test
    void uploadLogo_ShouldThrowException_WhenInvalidFileType() {
        // Arrange
        MockMultipartFile invalidFile = new MockMultipartFile(
                "logo", "test.txt", "text/plain", "test data".getBytes());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> brandingService.uploadLogo(testTenantId, invalidFile));
    }

    @Test
    void getDefaultBranding_ShouldReturnDefaultValues() {
        // Act
        TenantBrandingDto result = brandingService.getDefaultBranding();

        // Assert
        assertNotNull(result);
        assertEquals("#007bff", result.getPrimaryColor());
        assertEquals("#6c757d", result.getSecondaryColor());
        assertEquals("#28a745", result.getAccentColor());
        assertEquals("#f8f9fa", result.getHeaderBackgroundColor());
        assertEquals("#212529", result.getHeaderTextColor());
        assertEquals("Arial, sans-serif", result.getFontFamily());
        assertEquals(150, result.getLogoWidth());
        assertEquals(60, result.getLogoHeight());
        assertTrue(result.isActive());
    }

    @Test
    void existsByTenantId_ShouldReturnTrue_WhenExists() {
        // Arrange
        when(brandingRepository.existsByTenantId(testTenantId)).thenReturn(true);

        // Act
        boolean result = brandingService.existsByTenantId(testTenantId);

        // Assert
        assertTrue(result);
        verify(brandingRepository).existsByTenantId(testTenantId);
    }

    @Test
    void existsByTenantId_ShouldReturnFalse_WhenNotExists() {
        // Arrange
        when(brandingRepository.existsByTenantId(testTenantId)).thenReturn(false);

        // Act
        boolean result = brandingService.existsByTenantId(testTenantId);

        // Assert
        assertFalse(result);
        verify(brandingRepository).existsByTenantId(testTenantId);
    }
}