package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.InvoiceDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.BillingSchedule;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.Invoice;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing invoices.
 * Provides methods for creating, updating, retrieving, deleting, and scheduling invoices.
 */
public interface InvoiceService {

    /**
     * Creates a new invoice for the specified customer and service provider.
     *
     * @param tenantId The ID of the tenant (organization) the customer belongs to.
     * @param dto Object containing all invoice details.
     * @return The created {@link Invoice} entity, saved to the repository.
     */
    Invoice createInvoiceFromDto(Long tenantId, InvoiceDto dto);

    /**
     * Updates an existing invoice.
     *
     * @param tenantId The ID of the tenant (organization) the customer belongs to.
     * @param invoiceId The ID of the invoice to update.
     * @param invoiceDto The invoice data transfer object with updated data.
     * @return The updated invoice.
     */
    Invoice updateInvoice(Long tenantId, Long invoiceId, @Valid InvoiceDto invoiceDto);
    /**
     * Generates and persists an invoice based on a given billing schedule and evaluation date.
     * <p>
     * This method is used for automated invoice creation triggered by a schedule.
     * It uses the schedule's rate, frequency, and tenant configuration to construct an invoice draft.
     * </p>
     *
     * @param schedule the {@link BillingSchedule} containing invoicing rules
     * @param date the target date for which the invoice should be generated
     * @return the generated {@link Invoice} entity
     */
    Invoice generateFromSchedule(BillingSchedule schedule, LocalDate date);
    /**
     * Retrieves all invoices for a given tenant.
     *
     * @param tenantId The ID of the tenant (organization) the customer belongs to.
     * @return A list of all invoices.
     */
    List<Invoice> getAllInvoices(Long tenantId);

    /**
     * Retrieves a single invoice by its ID.
     *
     * @param tenantId The ID of the tenant (organization) the customer belongs to.
     * @param invoiceId The ID of the invoice to retrieve.
     * @return An optional containing the invoice, or empty if not found.
     */
    Optional<Invoice> getInvoiceById(Long tenantId, Long invoiceId);

    /**
     * Deletes an invoice by its ID.
     *
     * @param tenantId The ID of the tenant (organization) the customer belongs to.
     * @param invoiceId The ID of the invoice to delete.
     * @return true if the invoice was deleted, false otherwise.
     */
    boolean deleteInvoice(Long tenantId, Long invoiceId);

    /**
     * Schedules the sending of an invoice.
     *
     * @param tenantId The ID of the tenant (organization) the customer belongs to.
     * @param invoiceId The ID of the invoice to send.
     * @param scheduleDateTime The date and time to send the invoice.
     */
    void scheduleInvoiceSending(Long tenantId, Long invoiceId, LocalDateTime scheduleDateTime);

    /**
     * Sends an invoice via email.
     *
     * @param tenantId The ID of the tenant (organization) the customer belongs to.
     * @param invoiceId The ID of the invoice to send.
     * @param sendImmediately Predicate for sending the invoice immediately.
     * @param scheduledTime The time scheduled for sending the invoice.
     */
    void sendInvoice(Long tenantId, Long invoiceId, boolean sendImmediately, LocalDateTime scheduledTime);

    /**
     * Gets invoice due Date options for user (7, 14, 30, 45)
     * @return A list of integers as invoice due date options
     */
    List<Integer> getDueDateOptions();

    /**
     * Checks whether an invoice has already been generated for a given billing schedule and date.
     *
     * @param schedule The billing schedule to check against.
     * @param date     The invoice date to check for.
     * @return true if an invoice already exists, false otherwise.
     */
    boolean invoiceAlreadyGenerated(BillingSchedule schedule, LocalDate date);

    /**
     * Generates a PDF invoice for the given invoice ID.
     *
     * @param tenantId   the tenant ID
     * @param invoiceId  the invoice ID
     * @return the PDF file as a byte array
     */
    byte[] generateInvoicePdf(Long tenantId, Long invoiceId);
}
