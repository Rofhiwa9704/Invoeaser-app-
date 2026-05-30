package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Invoice;

/**
 * Handles actual email sending logic for invoices.
 */
public interface InvoiceMailerService {

    /**
     * Sends the invoice email using SMTP (Spring Mail).
     *
     * @param invoice the invoice entity to send
     */
    void sendInvoiceEmail(Invoice invoice);
}
