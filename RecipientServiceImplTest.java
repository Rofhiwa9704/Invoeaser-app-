package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.RecipientDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.Recipient;
import co.za.kingstechco.kingstechco.invoeaserapp.exception.ResourceNotFoundException;
import co.za.kingstechco.kingstechco.invoeaserapp.mapper.dtoMapper.RecipientMapper;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.RecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipientServiceImplTest {

    @Mock
    private RecipientRepository repository;
    
    @Mock
    private RecipientMapper mapper;

    private RecipientServiceImpl recipientService;

    private static final Long TENANT_ID = 1L;
    private static final Long RECIPIENT_ID = 100L;
    private static final String RECIPIENT_EMAIL = "recipient@example.com";
    private static final String RECIPIENT_CONTACT_NAME = "John Doe";
    private static final String RECIPIENT_COMPANY_NAME = "Acme Corp";

    @BeforeEach
    void setUp() {
        recipientService = new RecipientServiceImpl(repository, mapper);
    }

    @Test
    void createRecipient_ShouldReturnCreatedRecipient_WhenValidDto() {
        // Given
        RecipientDto dto = createTestRecipientDto();
        Recipient entity = createTestRecipient();
        Recipient savedEntity = createTestRecipient();
        savedEntity.setRecipientId(RECIPIENT_ID);
        RecipientDto savedDto = createTestRecipientDto();
        savedDto.setRecipientId(RECIPIENT_ID);
        
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(savedDto);

        // When
        RecipientDto result = recipientService.createRecipient(TENANT_ID, dto);

        // Then
        assertNotNull(result);
        assertEquals(RECIPIENT_ID, result.getRecipientId());
        assertEquals(TENANT_ID, dto.getTenantId()); // Verify tenant ID was set
        
        verify(mapper).toEntity(dto);
        verify(repository).save(entity);
        verify(mapper).toDto(savedEntity);
    }

    @Test
    void createRecipient_ShouldSetTenantId_OnDto() {
        // Given
        RecipientDto dto = createTestRecipientDto();
        dto.setTenantId(null); // Start with null tenant ID
        
        Recipient entity = createTestRecipient();
        Recipient savedEntity = createTestRecipient();
        RecipientDto savedDto = createTestRecipientDto();
        
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(savedDto);

        // When
        recipientService.createRecipient(TENANT_ID, dto);

        // Then
        assertEquals(TENANT_ID, dto.getTenantId()); // Verify tenant ID was set
        
        verify(mapper).toEntity(dto);
    }

    @Test
    void createRecipient_ShouldHandleRepositoryExceptions_Gracefully() {
        // Given
        RecipientDto dto = createTestRecipientDto();
        Recipient entity = createTestRecipient();
        
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> recipientService.createRecipient(TENANT_ID, dto));
        
        assertEquals("Database error", exception.getMessage());
        
        verify(repository).save(entity);
    }

    @Test
    void getAllRecipients_ShouldReturnRecipientList_WhenRecipientsExist() {
        // Given
        List<Recipient> entities = Arrays.asList(
                createTestRecipientWithId(1L, "recipient1@example.com", "John Doe"),
                createTestRecipientWithId(2L, "recipient2@example.com", "Jane Smith"),
                createTestRecipientWithId(3L, "recipient3@example.com", "Bob Johnson")
        );
        
        List<RecipientDto> expectedDtos = Arrays.asList(
                createTestRecipientDtoWithId(1L, "recipient1@example.com", "John Doe"),
                createTestRecipientDtoWithId(2L, "recipient2@example.com", "Jane Smith"),
                createTestRecipientDtoWithId(3L, "recipient3@example.com", "Bob Johnson")
        );
        
        when(repository.findAllByTenantId(TENANT_ID)).thenReturn(entities);
        when(mapper.toDto(entities.get(0))).thenReturn(expectedDtos.get(0));
        when(mapper.toDto(entities.get(1))).thenReturn(expectedDtos.get(1));
        when(mapper.toDto(entities.get(2))).thenReturn(expectedDtos.get(2));

        // When
        List<RecipientDto> result = recipientService.getAllRecipients(TENANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("John Doe", result.get(0).getContactName());
        assertEquals("Jane Smith", result.get(1).getContactName());
        assertEquals("Bob Johnson", result.get(2).getContactName());
        
        verify(repository).findAllByTenantId(TENANT_ID);
        verify(mapper, times(3)).toDto(any(Recipient.class));
    }

    @Test
    void getAllRecipients_ShouldReturnEmptyList_WhenNoRecipientsExist() {
        // Given
        when(repository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        // When
        List<RecipientDto> result = recipientService.getAllRecipients(TENANT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(repository).findAllByTenantId(TENANT_ID);
        verify(mapper, never()).toDto(any());
    }

    @Test
    void getRecipientById_ShouldReturnRecipient_WhenExists() {
        // Given
        Recipient entity = createTestRecipient();
        entity.setRecipientId(RECIPIENT_ID);
        entity.setTenantId(TENANT_ID);
        RecipientDto expectedDto = createTestRecipientDto();
        expectedDto.setRecipientId(RECIPIENT_ID);
        
        when(repository.findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID))
                .thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(expectedDto);

        // When
        RecipientDto result = recipientService.getRecipientById(TENANT_ID, RECIPIENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedDto, result);
        assertEquals(RECIPIENT_ID, result.getRecipientId());
        
        verify(repository).findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID);
        verify(mapper).toDto(entity);
    }

    @Test
    void getRecipientById_ShouldThrowException_WhenNotFound() {
        // Given
        when(repository.findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> recipientService.getRecipientById(TENANT_ID, RECIPIENT_ID));
        
        assertEquals("Recipient not found", exception.getMessage());
        
        verify(repository).findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID);
        verify(mapper, never()).toDto(any());
    }

    @Test
    void updateRecipient_ShouldReturnUpdatedRecipient_WhenExists() {
        // Given
        RecipientDto updateDto = createTestRecipientDto();
        updateDto.setContactName("Updated Name");
        updateDto.setEmail("updated@example.com");
        
        Recipient existingEntity = createTestRecipient();
        existingEntity.setRecipientId(RECIPIENT_ID);
        existingEntity.setTenantId(TENANT_ID);
        
        Recipient updatedEntity = createTestRecipient();
        updatedEntity.setRecipientId(RECIPIENT_ID);
        updatedEntity.setContactName("Updated Name");
        updatedEntity.setEmail("updated@example.com");
        
        RecipientDto resultDto = createTestRecipientDto();
        resultDto.setRecipientId(RECIPIENT_ID);
        resultDto.setContactName("Updated Name");
        resultDto.setEmail("updated@example.com");
        
        when(repository.findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID))
                .thenReturn(Optional.of(existingEntity));
        when(repository.save(existingEntity)).thenReturn(updatedEntity);
        when(mapper.toDto(updatedEntity)).thenReturn(resultDto);

        // When
        RecipientDto result = recipientService.updateRecipient(TENANT_ID, RECIPIENT_ID, updateDto);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", result.getContactName());
        assertEquals("updated@example.com", result.getEmail());
        
        verify(repository).findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID);
        verify(mapper).updateFromDto(updateDto, existingEntity);
        verify(repository).save(existingEntity);
        verify(mapper).toDto(updatedEntity);
    }

    @Test
    void updateRecipient_ShouldThrowException_WhenNotFound() {
        // Given
        RecipientDto updateDto = createTestRecipientDto();
        
        when(repository.findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> recipientService.updateRecipient(TENANT_ID, RECIPIENT_ID, updateDto));
        
        assertEquals("Recipient not found", exception.getMessage());
        
        verify(repository).findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID);
        verify(mapper, never()).updateFromDto(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRecipient_ShouldDeleteRecipient_WhenExists() {
        // Given
        Recipient existingEntity = createTestRecipient();
        existingEntity.setRecipientId(RECIPIENT_ID);
        existingEntity.setTenantId(TENANT_ID);
        
        when(repository.findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID))
                .thenReturn(Optional.of(existingEntity));

        // When
        recipientService.deleteRecipient(TENANT_ID, RECIPIENT_ID);

        // Then
        verify(repository).findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID);
        verify(repository).delete(existingEntity);
    }

    @Test
    void deleteRecipient_ShouldThrowException_WhenNotFound() {
        // Given
        when(repository.findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> recipientService.deleteRecipient(TENANT_ID, RECIPIENT_ID));
        
        assertEquals("Recipient not found", exception.getMessage());
        
        verify(repository).findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRecipient_ShouldHandleRepositoryExceptions_WhenDeletionFails() {
        // Given
        Recipient existingEntity = createTestRecipient();
        existingEntity.setRecipientId(RECIPIENT_ID);
        existingEntity.setTenantId(TENANT_ID);
        
        when(repository.findByTenantIdAndRecipientId(TENANT_ID, RECIPIENT_ID))
                .thenReturn(Optional.of(existingEntity));
        doThrow(new RuntimeException("Foreign key constraint violation"))
                .when(repository).delete(existingEntity);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> recipientService.deleteRecipient(TENANT_ID, RECIPIENT_ID));
        
        assertEquals("Foreign key constraint violation", exception.getMessage());
        
        verify(repository).delete(existingEntity);
    }

    @Test
    void createRecipient_ShouldHandleNullValues_Gracefully() {
        // Given
        RecipientDto dto = new RecipientDto();
        dto.setEmail(RECIPIENT_EMAIL); // Only set email
        
        Recipient entity = new Recipient();
        entity.setEmail(RECIPIENT_EMAIL);
        
        Recipient savedEntity = new Recipient();
        savedEntity.setRecipientId(RECIPIENT_ID);
        savedEntity.setEmail(RECIPIENT_EMAIL);
        
        RecipientDto savedDto = new RecipientDto();
        savedDto.setRecipientId(RECIPIENT_ID);
        savedDto.setEmail(RECIPIENT_EMAIL);
        
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(savedDto);

        // When
        RecipientDto result = recipientService.createRecipient(TENANT_ID, dto);

        // Then
        assertNotNull(result);
        assertEquals(RECIPIENT_EMAIL, result.getEmail());
        assertNull(result.getContactName());
        
        verify(repository).save(entity);
    }

    @Test
    void getAllRecipients_ShouldFilterByTenantId_Correctly() {
        // Given
        List<Recipient> tenantRecipients = Arrays.asList(
                createTestRecipientWithTenant(1L),
                createTestRecipientWithTenant(2L)
        );
        
        List<RecipientDto> expectedDtos = Arrays.asList(
                createTestRecipientDtoWithId(1L, "test1@example.com", "Test User 1"),
                createTestRecipientDtoWithId(2L, "test2@example.com", "Test User 2")
        );
        
        when(repository.findAllByTenantId(TENANT_ID)).thenReturn(tenantRecipients);
        when(mapper.toDto(tenantRecipients.get(0))).thenReturn(expectedDtos.get(0));
        when(mapper.toDto(tenantRecipients.get(1))).thenReturn(expectedDtos.get(1));

        // When
        List<RecipientDto> result = recipientService.getAllRecipients(TENANT_ID);

        // Then
        assertEquals(2, result.size());
        assertEquals("Test User 1", result.get(0).getContactName());
        assertEquals("Test User 2", result.get(1).getContactName());
        
        verify(repository).findAllByTenantId(TENANT_ID);
    }

    // Helper methods
    private RecipientDto createTestRecipientDto() {
        RecipientDto dto = new RecipientDto();
        dto.setEmail(RECIPIENT_EMAIL);
        dto.setContactName(RECIPIENT_CONTACT_NAME);
        dto.setCompanyName(RECIPIENT_COMPANY_NAME);
        return dto;
    }

    private RecipientDto createTestRecipientDtoWithId(Long id, String email, String name) {
        RecipientDto dto = createTestRecipientDto();
        dto.setRecipientId(id);
        dto.setEmail(email);
        dto.setContactName(name);
        return dto;
    }

    private Recipient createTestRecipient() {
        Recipient recipient = new Recipient();
        recipient.setEmail(RECIPIENT_EMAIL);
        recipient.setContactName(RECIPIENT_CONTACT_NAME);
        recipient.setCompanyName(RECIPIENT_COMPANY_NAME);
        return recipient;
    }

    private Recipient createTestRecipientWithId(Long id, String email, String name) {
        Recipient recipient = createTestRecipient();
        recipient.setRecipientId(id);
        recipient.setEmail(email);
        recipient.setContactName(name);
        return recipient;
    }

    private Recipient createTestRecipientWithTenant(Long recipientId) {
        Recipient recipient = createTestRecipient();
        recipient.setRecipientId(recipientId);
        recipient.setTenantId(RecipientServiceImplTest.TENANT_ID);
        return recipient;
    }
}