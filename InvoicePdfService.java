package co.za.kingstechco.kingstechco.invoeaserapp.service;

import org.springframework.core.io.ByteArrayResource;

import java.util.UUID;

public interface InvoicePdfService {
    /**
     * Generates a PDF invoice using Invoice for the user
     * @param invoiceId Invoice identifier
     * @return Byte array invoice
     */
    ByteArrayResource generatePdf(Long invoiceId);
}
