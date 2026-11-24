package model;

import org.junit.jupiter.api.Test;

import static model.OrderStatus.CANCELLED;
import static model.OrderStatus.CONFIRMED;
import static model.OrderStatus.DELIVERED;
import static model.OrderStatus.DELIVERY_FAILED;
import static model.OrderStatus.DISPATCHED;
import static model.OrderStatus.PACKED;
import static model.OrderStatus.PLACED;
import static model.OrderStatus.REJECTED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The order lifecycle from spec section 7.3. */
class OrderStatusTest {

    @Test
    void happyPath() {
        assertTrue(PLACED.canTransitionTo(CONFIRMED));
        assertTrue(CONFIRMED.canTransitionTo(PACKED));
        assertTrue(PACKED.canTransitionTo(DISPATCHED));
        assertTrue(DISPATCHED.canTransitionTo(DELIVERED));
    }

    @Test
    void cancellationOnlyBeforePacking() {
        assertTrue(PLACED.canTransitionTo(CANCELLED));
        assertTrue(CONFIRMED.canTransitionTo(CANCELLED));
        assertFalse(PACKED.canTransitionTo(CANCELLED));
        assertFalse(DISPATCHED.canTransitionTo(CANCELLED));
        assertFalse(DELIVERED.canTransitionTo(CANCELLED));
    }

    @Test
    void noSkippingOrGoingBack() {
        assertFalse(PLACED.canTransitionTo(DELIVERED));
        assertFalse(PLACED.canTransitionTo(DISPATCHED));
        assertFalse(DELIVERED.canTransitionTo(PLACED));
        assertFalse(CONFIRMED.canTransitionTo(REJECTED));
        assertTrue(DISPATCHED.canTransitionTo(DELIVERY_FAILED));
    }

    @Test
    void finalStates() {
        for (OrderStatus s : new OrderStatus[]{DELIVERED, REJECTED, CANCELLED, DELIVERY_FAILED}) {
            assertTrue(s.isFinal(), s + " should be final");
        }
        assertFalse(PLACED.isFinal());
    }
}
