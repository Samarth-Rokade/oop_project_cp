package interfaces;

import exception.ValidationException;
import model.Buyer;
import model.Cart;
import model.Farmer;
import model.Order;
import model.OrderItem;
import model.PaymentMethod;

import java.util.List;

/**
 * Order operations for buyers and farmers (spec 9.5).
 *
 * getOrdersOf / getItems are overloaded (spec 9.6): the same name works
 * for a Buyer (orders they placed) and a Farmer (orders they received).
 */
public interface OrderOperations {

    // ----- buyer

    /**
     * Places the cart as one order per farm, in a single transaction.
     *
     * @return the new order ids
     */
    List<Integer> placeOrder(Buyer buyer, Cart cart, int addressId, PaymentMethod method)
            throws ValidationException;

    List<Order> getOrdersOf(Buyer buyer);

    List<OrderItem> getItems(Buyer buyer, Order order) throws ValidationException;

    void cancelOrder(Buyer buyer, int orderId) throws ValidationException;

    // ----- farmer

    List<Order> getOrdersOf(Farmer farmer);

    List<OrderItem> getItems(Farmer farmer, Order order) throws ValidationException;

    int countNewOrders(Farmer farmer);

    void acceptOrder(Farmer farmer, int orderId) throws ValidationException;

    void rejectOrder(Farmer farmer, int orderId, String reason) throws ValidationException;

    void markPacked(Farmer farmer, int orderId) throws ValidationException;

    void dispatchOrder(Farmer farmer, int orderId, String deliveryPartner, String trackingReference)
            throws ValidationException;

    void markDelivered(Farmer farmer, int orderId) throws ValidationException;

    void markDeliveryFailed(Farmer farmer, int orderId, String reason) throws ValidationException;
}
