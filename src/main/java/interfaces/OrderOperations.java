package interfaces;

import exception.ValidationException;
import model.Buyer;
import model.Cart;
import model.Order;
import model.OrderItem;
import model.PaymentMethod;

import java.util.List;

/** What a buyer can do with orders (spec 9.5). Farmer/admin operations are added in later steps. */
public interface OrderOperations {

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
}
