package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Payment;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing payments.
 */
public interface PaymentService {

    /**
     * Creates a new payment.
     *
     * @param payment the payment to create.
     * @return the created payment.
     */
    Payment createPayment(Payment payment);

    /**
     * Retrieves all payments.
     *
     * @return the list of all payments.
     */
    List<Payment> getAllPayments();

    /**
     * Retrieves a payment by its ID.
     *
     * @param paymentId the ID of the payment to retrieve.
     * @return an optional containing the payment, or empty if not found.
     */
    Optional<Payment> getPaymentById(Long paymentId);

    /**
     * Updates an existing payment.
     *
     * @param paymentId the ID of the payment to update.
     * @param payment the updated payment data.
     * @return the updated payment.
     */
    Payment updatePayment(Long paymentId, Payment payment);

    /**
     * Deletes a payment by its ID.
     *
     * @param paymentId the ID of the payment to delete.
     * @return true if deleted, false otherwise.
     */
    boolean deletePayment(Long paymentId);
}

