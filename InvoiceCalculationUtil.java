package co.za.kingstechco.kingstechco.invoeaserapp.util;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Invoice;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.InvoiceItem;

import java.math.BigDecimal;
import java.util.Objects;

public class InvoiceCalculationUtil {

    public static void calculateItemLineTotal(InvoiceItem item) {
        if (item.getHourlyRate() != null && item.getHoursWorked() != null) {
            item.setLineTotal(item.getHourlyRate().multiply(BigDecimal.valueOf(item.getHoursWorked())));
        } else if (item.getUnitPrice() != null && item.getQuantity() != null) {
            item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
    }

    public static void calculateInvoiceTotal(Invoice invoice) {
        invoice.getInvoiceItems().forEach(InvoiceCalculationUtil::calculateItemLineTotal);
        BigDecimal total = invoice.getInvoiceItems().stream()
                .map(InvoiceItem::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setTotalAmount(total);
    }
}

