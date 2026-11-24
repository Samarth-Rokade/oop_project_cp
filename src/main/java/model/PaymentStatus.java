package model;

/** Payment lifecycle (spec 7.4): PENDING -> PAID, plus FAILED / REFUNDED / CANCELLED. */
public enum PaymentStatus {
    PENDING("Pending"),
    PAID("Paid"),
    FAILED("Failed"),
    REFUNDED("Refunded"),
    CANCELLED("Cancelled");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
