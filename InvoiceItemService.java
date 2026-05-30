package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.InvoiceItem;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing invoice items.
 */
public interface InvoiceItemService {

    /**
     * Creates a new invoice item.
     *
     * @param invoiceItem the invoice item to create.
     * @return the created invoice item.
     */
    InvoiceItem createInvoiceItem(InvoiceItem invoiceItem);

    /**
     * Retrieves all invoice items for a given invoice.
     *
     * @param invoiceId the ID of the invoice.
     * @return the list of invoice items.
     */
    List<InvoiceItem> getInvoiceItemsByInvoiceId(Long invoiceId);

    /**
     * Retrieves an invoice item by its ID.
     *
     * @param invoiceItemId the ID of the invoice item to retrieve.
     * @return an optional containing the invoice item, or empty if not found.
     */
    Optional<InvoiceItem> getInvoiceItemById(Long invoiceItemId);

    /**
     * Updates an existing invoice item.
     *
     * @param invoiceItemId the ID of the invoice item to update.
     * @param invoiceItem the updated invoice item data.
     * @return the updated invoice item.
     */
    InvoiceItem updateInvoiceItem(Long invoiceItemId, InvoiceItem invoiceItem);

    /**
     * Deletes an invoice item by its ID.
     *
     * @param invoiceItemId the ID of the invoice item to delete.
     * @return true if deleted, false otherwise.
     */
    boolean deleteInvoiceItem(Long invoiceItemId);
}
