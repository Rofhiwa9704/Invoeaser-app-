package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.BillingScheduleDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.BillingSchedule;
import co.za.kingstechco.kingstechco.invoeaserapp.exception.ResourceNotFoundException;
import co.za.kingstechco.kingstechco.invoeaserapp.mapper.dtoMapper.BillingScheduleMapper;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.BillingScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingScheduleServiceImplTest {

    @Mock
    private BillingScheduleRepository billingScheduleRepository;
    
    @Mock
    private BillingScheduleMapper billingScheduleMapper;

    private BillingScheduleServiceImpl billingScheduleService;

    private static final Long TENANT_ID = 1L;
    private static final Long SCHEDULE_ID = 100L;
    private static final String CONTRACT_TITLE = "Monthly Service Agreement";
    private static final BigDecimal RATE = BigDecimal.valueOf(2500.00);
    private static final String FREQUENCY = "MONTHLY";

    @BeforeEach
    void setUp() {
        billingScheduleService = new BillingScheduleServiceImpl(billingScheduleRepository, billingScheduleMapper);
    }

    @Test
    void createBillingSchedule_ShouldReturnCreatedSchedule_WhenValidDto() {
        // Given
        BillingScheduleDto dto = createTestBillingScheduleDto();
        BillingSchedule entity = createTestBillingSchedule();
        BillingSchedule savedEntity = createTestBillingSchedule();
        savedEntity.setId(SCHEDULE_ID);
        BillingScheduleDto savedDto = createTestBillingScheduleDto();
        savedDto.setId(SCHEDULE_ID);
        
        when(billingScheduleMapper.toEntity(dto)).thenReturn(entity);
        when(billingScheduleRepository.save(entity)).thenReturn(savedEntity);
        when(billingScheduleMapper.toDto(savedEntity)).thenReturn(savedDto);

        // When
        BillingScheduleDto result = billingScheduleService.createBillingSchedule(TENANT_ID, dto);

        // Then
        assertNotNull(result);
        assertEquals(SCHEDULE_ID, result.getId());
        assertEquals(TENANT_ID, dto.getTenantId()); // Verify tenant ID was set
        
        verify(billingScheduleMapper).toEntity(dto);
        verify(billingScheduleRepository).save(entity);
        verify(billingScheduleMapper).toDto(savedEntity);
    }

    @Test
    void createBillingSchedule_ShouldSetTenantId_OnDto() {
        // Given
        BillingScheduleDto dto = createTestBillingScheduleDto();
        dto.setTenantId(null); // Start with null tenant ID
        
        BillingSchedule entity = createTestBillingSchedule();
        BillingSchedule savedEntity = createTestBillingSchedule();
        BillingScheduleDto savedDto = createTestBillingScheduleDto();
        
        when(billingScheduleMapper.toEntity(dto)).thenReturn(entity);
        when(billingScheduleRepository.save(entity)).thenReturn(savedEntity);
        when(billingScheduleMapper.toDto(savedEntity)).thenReturn(savedDto);

        // When
        billingScheduleService.createBillingSchedule(TENANT_ID, dto);

        // Then
        assertEquals(TENANT_ID, dto.getTenantId()); // Verify tenant ID was set
        
        verify(billingScheduleMapper).toEntity(dto);
    }

    @Test
    void getBillingSchedule_ShouldReturnSchedule_WhenExists() {
        // Given
        BillingSchedule entity = createTestBillingSchedule();
        entity.setId(SCHEDULE_ID);
        entity.setTenantId(TENANT_ID);
        BillingScheduleDto expectedDto = createTestBillingScheduleDto();
        
        when(billingScheduleRepository.findByIdAndTenantId(SCHEDULE_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
        when(billingScheduleMapper.toDto(entity)).thenReturn(expectedDto);

        // When
        BillingScheduleDto result = billingScheduleService.getBillingSchedule(TENANT_ID, SCHEDULE_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedDto, result);
        
        verify(billingScheduleRepository).findByIdAndTenantId(SCHEDULE_ID, TENANT_ID);
        verify(billingScheduleMapper).toDto(entity);
    }

    @Test
    void getBillingSchedule_ShouldThrowException_WhenNotFound() {
        // Given
        when(billingScheduleRepository.findByIdAndTenantId(SCHEDULE_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billingScheduleService.getBillingSchedule(TENANT_ID, SCHEDULE_ID));
        
        assertTrue(exception.getMessage().contains("BillingSchedule not found"));
        assertTrue(exception.getMessage().contains(TENANT_ID.toString()));
        assertTrue(exception.getMessage().contains(SCHEDULE_ID.toString()));
        
        verify(billingScheduleRepository).findByIdAndTenantId(SCHEDULE_ID, TENANT_ID);
        verify(billingScheduleMapper, never()).toDto(any());
    }

    @Test
    void updateBillingSchedule_ShouldReturnUpdatedSchedule_WhenExists() {
        // Given
        BillingScheduleDto updateDto = createTestBillingScheduleDto();
        updateDto.setContractTitle("Updated Service Agreement");
        updateDto.setRate(BigDecimal.valueOf(3000.00));
        
        BillingSchedule existingEntity = createTestBillingSchedule();
        existingEntity.setId(SCHEDULE_ID);
        existingEntity.setTenantId(TENANT_ID);
        
        BillingSchedule updatedEntity = createTestBillingSchedule();
        updatedEntity.setId(SCHEDULE_ID);
        updatedEntity.setContractTitle("Updated Service Agreement");
        updatedEntity.setRate(BigDecimal.valueOf(3000.00));
        
        BillingScheduleDto resultDto = createTestBillingScheduleDto();
        resultDto.setId(SCHEDULE_ID);
        resultDto.setContractTitle("Updated Service Agreement");
        
        when(billingScheduleRepository.findByIdAndTenantId(SCHEDULE_ID, TENANT_ID))
                .thenReturn(Optional.of(existingEntity));
        when(billingScheduleRepository.save(existingEntity)).thenReturn(updatedEntity);
        when(billingScheduleMapper.toDto(updatedEntity)).thenReturn(resultDto);

        // When
        BillingScheduleDto result = billingScheduleService.updateBillingSchedule(TENANT_ID, SCHEDULE_ID, updateDto);

        // Then
        assertNotNull(result);
        assertEquals("Updated Service Agreement", result.getContractTitle());
        
        verify(billingScheduleRepository).findByIdAndTenantId(SCHEDULE_ID, TENANT_ID);
        verify(billingScheduleMapper).updateEntityFromDto(updateDto, existingEntity);
        verify(billingScheduleRepository).save(existingEntity);
        verify(billingScheduleMapper).toDto(updatedEntity);
    }

    @Test
    void updateBillingSchedule_ShouldThrowException_WhenNotFound() {
        // Given
        BillingScheduleDto updateDto = createTestBillingScheduleDto();
        
        when(billingScheduleRepository.findByIdAndTenantId(SCHEDULE_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billingScheduleService.updateBillingSchedule(TENANT_ID, SCHEDULE_ID, updateDto));
        
        assertTrue(exception.getMessage().contains("BillingSchedule not found"));
        
        verify(billingScheduleRepository).findByIdAndTenantId(SCHEDULE_ID, TENANT_ID);
        verify(billingScheduleMapper, never()).updateEntityFromDto(any(), any());
        verify(billingScheduleRepository, never()).save(any());
    }

    @Test
    void deactivateBillingSchedule_ShouldSetActiveToFalse_WhenExists() {
        // Given
        BillingSchedule existingEntity = createTestBillingSchedule();
        existingEntity.setId(SCHEDULE_ID);
        existingEntity.setTenantId(TENANT_ID);
        existingEntity.setActive(true);
        
        when(billingScheduleRepository.findByIdAndTenantId(SCHEDULE_ID, TENANT_ID))
                .thenReturn(Optional.of(existingEntity));
        when(billingScheduleRepository.save(existingEntity)).thenReturn(existingEntity);

        // When
        billingScheduleService.deactivateBillingSchedule(TENANT_ID, SCHEDULE_ID);

        // Then
        assertFalse(existingEntity.isActive());
        
        verify(billingScheduleRepository).findByIdAndTenantId(SCHEDULE_ID, TENANT_ID);
        verify(billingScheduleRepository).save(existingEntity);
    }

    @Test
    void deactivateBillingSchedule_ShouldThrowException_WhenNotFound() {
        // Given
        when(billingScheduleRepository.findByIdAndTenantId(SCHEDULE_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billingScheduleService.deactivateBillingSchedule(TENANT_ID, SCHEDULE_ID));
        
        assertTrue(exception.getMessage().contains("BillingSchedule not found"));
        
        verify(billingScheduleRepository).findByIdAndTenantId(SCHEDULE_ID, TENANT_ID);
        verify(billingScheduleRepository, never()).save(any());
    }

    @Test
    void getAllBillingSchedules_ShouldReturnActiveSchedules_WhenSchedulesExist() {
        // Given
        List<BillingSchedule> entities = Arrays.asList(
                createTestBillingScheduleWithId(1L, "Schedule 1"),
                createTestBillingScheduleWithId(2L, "Schedule 2"),
                createTestBillingScheduleWithId(3L, "Schedule 3")
        );
        
        List<BillingScheduleDto> expectedDtos = Arrays.asList(
                createTestBillingScheduleDtoWithId(1L, "Schedule 1"),
                createTestBillingScheduleDtoWithId(2L, "Schedule 2"),
                createTestBillingScheduleDtoWithId(3L, "Schedule 3")
        );
        
        when(billingScheduleRepository.findAllByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(entities);
        when(billingScheduleMapper.toDto(entities.get(0))).thenReturn(expectedDtos.get(0));
        when(billingScheduleMapper.toDto(entities.get(1))).thenReturn(expectedDtos.get(1));
        when(billingScheduleMapper.toDto(entities.get(2))).thenReturn(expectedDtos.get(2));

        // When
        List<BillingScheduleDto> result = billingScheduleService.getAllBillingSchedules(TENANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Schedule 1", result.get(0).getContractTitle());
        assertEquals("Schedule 2", result.get(1).getContractTitle());
        assertEquals("Schedule 3", result.get(2).getContractTitle());
        
        verify(billingScheduleRepository).findAllByTenantIdAndActiveTrue(TENANT_ID);
        verify(billingScheduleMapper, times(3)).toDto(any(BillingSchedule.class));
    }

    @Test
    void getAllBillingSchedules_ShouldReturnEmptyList_WhenNoActiveSchedules() {
        // Given
        when(billingScheduleRepository.findAllByTenantIdAndActiveTrue(TENANT_ID))
                .thenReturn(List.of());

        // When
        List<BillingScheduleDto> result = billingScheduleService.getAllBillingSchedules(TENANT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(billingScheduleRepository).findAllByTenantIdAndActiveTrue(TENANT_ID);
        verify(billingScheduleMapper, never()).toDto(any());
    }

    @Test
    void getEntityByTenantAndId_ShouldReturnEntity_WhenExistsAndTenantMatches() {
        // Given
        BillingSchedule entity = createTestBillingSchedule();
        entity.setId(SCHEDULE_ID);
        entity.setTenantId(TENANT_ID);
        
        when(billingScheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(entity));

        // When
        BillingSchedule result = billingScheduleService.getEntityByTenantAndId(TENANT_ID, SCHEDULE_ID);

        // Then
        assertNotNull(result);
        assertEquals(entity, result);
        assertEquals(TENANT_ID, result.getTenantId());
        
        verify(billingScheduleRepository).findById(SCHEDULE_ID);
    }

    @Test
    void getEntityByTenantAndId_ShouldThrowException_WhenNotFound() {
        // Given
        when(billingScheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billingScheduleService.getEntityByTenantAndId(TENANT_ID, SCHEDULE_ID));
        
        assertTrue(exception.getMessage().contains("BillingSchedule not found"));
        assertTrue(exception.getMessage().contains(SCHEDULE_ID.toString()));
        assertTrue(exception.getMessage().contains(TENANT_ID.toString()));
        
        verify(billingScheduleRepository).findById(SCHEDULE_ID);
    }

    @Test
    void getEntityByTenantAndId_ShouldThrowException_WhenTenantIdDoesNotMatch() {
        // Given
        BillingSchedule entity = createTestBillingSchedule();
        entity.setId(SCHEDULE_ID);
        entity.setTenantId(999L); // Different tenant ID
        
        when(billingScheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(entity));

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billingScheduleService.getEntityByTenantAndId(TENANT_ID, SCHEDULE_ID));
        
        assertTrue(exception.getMessage().contains("BillingSchedule not found"));
        
        verify(billingScheduleRepository).findById(SCHEDULE_ID);
    }

    @Test
    void createBillingSchedule_ShouldHandleRepositoryExceptions_Gracefully() {
        // Given
        BillingScheduleDto dto = createTestBillingScheduleDto();
        BillingSchedule entity = createTestBillingSchedule();
        
        when(billingScheduleMapper.toEntity(dto)).thenReturn(entity);
        when(billingScheduleRepository.save(entity)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billingScheduleService.createBillingSchedule(TENANT_ID, dto));
        
        assertEquals("Database error", exception.getMessage());
        
        verify(billingScheduleRepository).save(entity);
    }

    // Helper methods
    private BillingScheduleDto createTestBillingScheduleDto() {
        BillingScheduleDto dto = new BillingScheduleDto();
        dto.setContractTitle(CONTRACT_TITLE);
        dto.setRate(RATE);
        dto.setFrequency(FREQUENCY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusYears(1));
        return dto;
    }

    private BillingScheduleDto createTestBillingScheduleDtoWithId(Long id, String title) {
        BillingScheduleDto dto = createTestBillingScheduleDto();
        dto.setId(id);
        dto.setContractTitle(title);
        return dto;
    }

    private BillingSchedule createTestBillingSchedule() {
        BillingSchedule schedule = new BillingSchedule();
        schedule.setContractTitle(CONTRACT_TITLE);
        schedule.setRate(RATE);
        schedule.setFrequency(FREQUENCY);
        schedule.setStartDate(LocalDate.now());
        schedule.setEndDate(LocalDate.now().plusYears(1));
        schedule.setActive(true);
        return schedule;
    }

    private BillingSchedule createTestBillingScheduleWithId(Long id, String title) {
        BillingSchedule schedule = createTestBillingSchedule();
        schedule.setId(id);
        schedule.setContractTitle(title);
        schedule.setTenantId(TENANT_ID);
        return schedule;
    }
}