package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A (simulated) payment record, with the buyer's name for display. */
public record Payment(int paymentId, int orderId, String buyerName, PaymentMethod method, BigDecimal amount,
                      PaymentStatus status, LocalDateTime paymentDate, String transactionReference) {
}
