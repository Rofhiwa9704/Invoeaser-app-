package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.RecipientDto;

import java.util.List;

public interface RecipientService {

    RecipientDto createRecipient(Long tenantId, RecipientDto dto);

    List<RecipientDto> getAllRecipients(Long tenantId);

    RecipientDto getRecipientById(Long tenantId, Long recipientId);

    RecipientDto updateRecipient(Long tenantId, Long recipientId, RecipientDto dto);

    void deleteRecipient(Long tenantId, Long recipientId);
}
