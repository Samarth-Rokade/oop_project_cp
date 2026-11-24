package model;

import exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTest {

    private static Product product(int id, int farmerId, String price, String stock) {
        Product p = new Product();
        p.setProductId(id);
        p.setFarmerId(farmerId);
        p.setProductName("P" + id);
        p.setUnitCode("KG");
        p.setPricePerUnit(new BigDecimal(price));
        p.setAvailableQuantity(new BigDecimal(stock));
        return p;
    }

    @Test
    void addingSameProductTwiceMergesQuantity() throws ValidationException {
        Cart cart = new Cart();
        Product tomato = product(1, 2, "28.00", "100");
        cart.add(tomato, new BigDecimal("2"));
        cart.add(tomato, new BigDecimal("3.5"));
        assertEquals(1, cart.size());
        assertEquals(new BigDecimal("5.5"), cart.getItems().get(0).getQuantity());
    }

    @Test
    void cannotExceedAvailableStock() throws ValidationException {
        Cart cart = new Cart();
        Product turmeric = product(9, 3, "180.00", "8");
        cart.add(turmeric, new BigDecimal("6"));
        assertThrows(ValidationException.class, () -> cart.add(turmeric, new BigDecimal("3")));
        assertEquals(new BigDecimal("6"), cart.getItems().get(0).getQuantity());   // unchanged
    }

    @Test
    void rejectsZeroNegativeAndTooPreciseQuantities() {
        Cart cart = new Cart();
        Product p = product(1, 2, "10", "100");
        assertThrows(ValidationException.class, () -> cart.add(p, BigDecimal.ZERO));
        assertThrows(ValidationException.class, () -> cart.add(p, new BigDecimal("-1")));
        assertThrows(ValidationException.class, () -> cart.add(p, new BigDecimal("1.234")));
        assertTrue(cart.isEmpty());
    }

    @Test
    void totalAndGroupingByFarm() throws ValidationException {
        Cart cart = new Cart();
        cart.add(product(1, 2, "28.00", "100"), new BigDecimal("5"));      // 140.00
        cart.add(product(5, 3, "480.00", "100"), BigDecimal.ONE);          // 480.00
        cart.add(product(2, 2, "32.00", "100"), new BigDecimal("2.5"));    //  80.00
        assertEquals(new BigDecimal("700.00"), cart.getEstimatedTotal());
        assertEquals(2, cart.groupByFarmer().size());
        assertEquals(2, cart.groupByFarmer().get(2).size());
    }

    @Test
    void listenersAreNotifiedOnChange() throws ValidationException {
        Cart cart = new Cart();
        int[] calls = {0};
        cart.addChangeListener(() -> calls[0]++);
        cart.add(product(1, 2, "10", "10"), BigDecimal.ONE);
        cart.remove(1);
        cart.remove(1);          // already gone - no event
        assertEquals(2, calls[0]);
    }

    @Test
    void refreshDropsProductsNoLongerOnSale() throws ValidationException {
        Cart cart = new Cart();
        cart.add(product(1, 2, "10", "10"), BigDecimal.ONE);
        cart.add(product(2, 2, "20", "10"), BigDecimal.ONE);
        cart.refreshProducts(id -> id == 1 ? product(1, 2, "12", "10") : null);
        assertEquals(1, cart.size());
        assertEquals(new BigDecimal("12"), cart.getItems().get(0).getProduct().getPricePerUnit());
    }
}
