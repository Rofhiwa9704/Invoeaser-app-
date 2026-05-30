package co.za.kingstechco.kingstechco.invoeaserapp.util;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.EmailSendAuditDto;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

public class EaserUtil {
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    public static final String RETRY_SUCCESS = "RETRY_SUCCESS";
    public static final String RETRY_FAILED = "RETRY_FAILED";
    public static final String DAILY = "DAILY";
    public static final String WEEKLY = "WEEKLY";
    public static final String MONTHLY = "MONTHLY";
    public static final List<String> CURRENCIES = List.of("ZAR", "USD", "EUR", "GBP");

    public static String escape(String input) {
        return input == null ? "" : input.replace("\"", "\"\"").replace(",", " ");
    }

    public static LocalDate getTargetDate(LocalDate date) {
        return (date != null) ? date : LocalDate.now();
    }

    public static void writeCsv(HttpServletResponse response, List<EmailSendAuditDto> audits) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=email_audit_log.csv");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,Tenant ID,Invoice ID,Recipient Email,Status,Error Message,Timestamp");

            for (EmailSendAuditDto audit : audits) {
                writer.printf(
                        "%d,%d,%d,%s,%s,%s,%s%n",
                        audit.getId(),
                        audit.getTenantId(),
                        audit.getInvoiceId(),
                        escape(audit.getRecipientEmail()),
                        audit.getStatus(),
                        escape(audit.getErrorMessage()),
                        audit.getTimestamp()
                );
            }
        }
    }

}
