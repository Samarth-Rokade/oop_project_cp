package exception;

import model.OrderStatus;

/** A status change that the order lifecycle does not allow (spec 7.3, 9.10). */
public class InvalidOrderStatusException extends ValidationException {

    public InvalidOrderStatusException(int orderId, OrderStatus from, OrderStatus to) {
        super(message(orderId, from, to));
    }

    private static String message(int orderId, OrderStatus from, OrderStatus to) {
        if (from == to) {
            return "Order #" + orderId + " is already " + from.getLabel().toLowerCase() + ".";
        }
        return "Order #" + orderId + " is " + from.getLabel().toLowerCase()
                + ", so it cannot be " + to.getLabel().toLowerCase() + ".";
    }
}
