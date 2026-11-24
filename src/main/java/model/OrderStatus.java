package model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle (spec 7.3). Only the transitions listed here are
 * allowed; the service layer checks them before every status change.
 *
 *   PLACED -> CONFIRMED -> PACKED -> DISPATCHED -> DELIVERED
 *   PLACED -> REJECTED | CANCELLED
 *   CONFIRMED -> CANCELLED            (allowed before dispatch)
 *   DISPATCHED -> DELIVERY_FAILED
 */
public enum OrderStatus {
    PLACED("Placed"),
    CONFIRMED("Confirmed"),
    PACKED("Packed"),
    DISPATCHED("Dispatched"),
    DELIVERED("Delivered"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    DELIVERY_FAILED("Delivery failed");

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(PLACED, EnumSet.of(CONFIRMED, REJECTED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(PACKED, CANCELLED));
        TRANSITIONS.put(PACKED, EnumSet.of(DISPATCHED));
        TRANSITIONS.put(DISPATCHED, EnumSet.of(DELIVERED, DELIVERY_FAILED));
        for (OrderStatus s : values()) {
            TRANSITIONS.putIfAbsent(s, EnumSet.noneOf(OrderStatus.class));
        }
    }

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canTransitionTo(OrderStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public Set<OrderStatus> nextStatuses() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }

    /** No further changes possible. */
    public boolean isFinal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    /** Stock was given back to the farmer. */
    public boolean releasesStock() {
        return this == REJECTED || this == CANCELLED;
    }

    @Override
    public String toString() {
        return label;
    }
}
