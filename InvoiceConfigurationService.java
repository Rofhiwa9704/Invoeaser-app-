package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.InvoiceConfiguration;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing invoice configurations.
 */
public interface InvoiceConfigurationService {

    /**
     * Creates a new invoice configuration.
     *
     * @param invoiceConfiguration the invoice configuration to create.
     * @return the created invoice configuration.
     */
    InvoiceConfiguration createInvoiceConfiguration(InvoiceConfiguration invoiceConfiguration);

    /**
     * Retrieves all invoice configurations for a given tenant.
     *
     * @param tenantId the ID of the tenant.
     * @return the list of invoice configurations.
     */
    List<InvoiceConfiguration> getInvoiceConfigurationsByTenantId(Long tenantId);

    /**
     * Retrieves an invoice configuration by its ID.
     *
     * @param configurationId the ID of the invoice configuration to retrieve.
     * @return an optional containing the invoice configuration, or empty if not found.
     */
    Optional<InvoiceConfiguration> getInvoiceConfigurationById(Long configurationId);

    /**
     * Updates an existing invoice configuration.
     *
     * @param configurationId the ID of the invoice configuration to update.
     * @param invoiceConfiguration the updated invoice configuration data.
     * @return the updated invoice configuration.
     */
    InvoiceConfiguration updateInvoiceConfiguration(Long configurationId, InvoiceConfiguration invoiceConfiguration);

    /**
     * Deletes an invoice configuration by its ID.
     *
     * @param configurationId the ID of the invoice configuration to delete.
     * @return true if deleted, false otherwise.
     */
    boolean deleteInvoiceConfiguration(Long configurationId);
}

