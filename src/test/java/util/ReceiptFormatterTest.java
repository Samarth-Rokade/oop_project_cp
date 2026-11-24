package util;

import model.Order;
import model.OrderItem;
import model.OrderStatus;
import model.PaymentMethod;
import model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptFormatterTest {

    private static OrderItem item(String name, String qty, String price, String total) {
        OrderItem i = new OrderItem();
        i.setProductName(name);
        i.setUnitCode("KG");
        i.setQuantity(new BigDecimal(qty));
        i.setUnitPriceAtOrder(new BigDecimal(price));
        i.setLineTotal(new BigDecimal(total));
        return i;
    }

    @Test
    void containsOrderLinesTotalAndPayment() {
        Order o = new Order();
        o.setOrderId(6);
        o.setOrderDate(LocalDateTime.of(2026, 11, 24, 21, 45));
        o.setBuyerName("Priya Kulkarni");
        o.setFarmNames("Patil Organic Farms");
        o.setDeliverTo("Flat 302, Karve Road, Pune - 411004");
        o.setStatus(OrderStatus.DELIVERED);
        o.setPaymentMethod(PaymentMethod.UPI);
        o.setPaymentStatus(PaymentStatus.PAID);

        String receipt = ReceiptFormatter.format(o, List.of(
                item("Tomato", "5", "28.00", "140.00"),
                item("Onion", "2.5", "32.00", "80.00")), LocalDateTime.of(2026, 11, 24, 22, 0));

        assertTrue(receipt.contains("Order:        #6"), receipt);
        assertTrue(receipt.contains("Tomato"));
        assertTrue(receipt.contains("2.5 KG"));
        assertTrue(receipt.matches("(?s).*TOTAL\\s+₹220\\.00.*"), receipt);
        assertTrue(receipt.contains("UPI (simulated) - Paid"));
        assertTrue(receipt.contains("simulated"));
    }
}
