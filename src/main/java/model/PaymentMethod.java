package model;

/**
 * Payment methods. All online payments are SIMULATED for this course
 * project - no real money is transferred.
 */
public enum PaymentMethod {
    UPI("UPI (simulated)", true),
    CARD("Card (simulated)", true),
    NETBANKING("Net banking (simulated)", true),
    COD("Cash on delivery", false);

    private final String label;
    private final boolean paidOnline;

    PaymentMethod(String label, boolean paidOnline) {
        this.label = label;
        this.paidOnline = paidOnline;
    }

    /** Online methods are marked PAID immediately; COD stays PENDING. */
    public boolean isPaidOnline() {
        return paidOnline;
    }

    @Override
    public String toString() {
        return label;
    }
}
