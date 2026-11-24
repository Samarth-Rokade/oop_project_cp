package service;

import exception.ValidationException;
import interfaces.ProductOperations;
import model.ListingStatus;
import model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure rule tests - no database needed. */
class ProductServiceValidationTest {

    private static Product validProduct() {
        Product p = new Product();
        p.setProductName("Tomato");
        p.setCategoryId(1);
        p.setUnitCode("KG");
        p.setAvailableQuantity(new BigDecimal("100"));
        p.setPricePerUnit(new BigDecimal("28.50"));
        p.setHarvestDate(LocalDate.now().minusDays(1));
        return p;
    }

    @Test
    void validProductPasses() {
        assertDoesNotThrow(() -> ProductService.validate(validProduct()));
    }

    @Test
    void zeroQuantityIsAllowed() {
        Product p = validProduct();
        p.setAvailableQuantity(BigDecimal.ZERO);
        assertDoesNotThrow(() -> ProductService.validate(p));
    }

    @Test
    void rejectsNegativeQuantity() {
        Product p = validProduct();
        p.setAvailableQuantity(new BigDecimal("-1"));
        ValidationException e = assertThrows(ValidationException.class, () -> ProductService.validate(p));
        assertTrue(e.getMessage().contains("negative"));
    }

    @Test
    void rejectsZeroOrNegativePrice() {
        Product p = validProduct();
        p.setPricePerUnit(BigDecimal.ZERO);
        assertThrows(ValidationException.class, () -> ProductService.validate(p));
        p.setPricePerUnit(new BigDecimal("-5"));
        assertThrows(ValidationException.class, () -> ProductService.validate(p));
    }

    @Test
    void rejectsMoreThanTwoDecimals() {
        Product p = validProduct();
        p.setPricePerUnit(new BigDecimal("10.555"));
        assertThrows(ValidationException.class, () -> ProductService.validate(p));
    }

    @Test
    void allowsTrailingZerosBeyondTwoDecimals() {
        Product p = validProduct();
        p.setPricePerUnit(new BigDecimal("10.500"));
        assertDoesNotThrow(() -> ProductService.validate(p));
    }

    @Test
    void rejectsFutureHarvestDate() {
        Product p = validProduct();
        p.setHarvestDate(LocalDate.now().plusDays(1));
        assertThrows(ValidationException.class, () -> ProductService.validate(p));
    }

    @Test
    void reportsAllProblemsTogether() {
        Product p = new Product();
        p.setAvailableQuantity(null);
        ValidationException e = assertThrows(ValidationException.class, () -> ProductService.validate(p));
        String[] lines = e.getMessage().split("\n");
        assertEquals(5, lines.length, e.getMessage());   // name, category, unit, quantity, price
    }

    @Test
    void stockRuleSwitchesBetweenActiveAndOutOfStock() {
        Product p = validProduct();
        p.setListingStatus(ListingStatus.ACTIVE);
        p.setAvailableQuantity(BigDecimal.ZERO);
        p.applyStockRule();
        assertEquals(ListingStatus.OUT_OF_STOCK, p.getListingStatus());

        p.setAvailableQuantity(new BigDecimal("5"));
        p.applyStockRule();
        assertEquals(ListingStatus.ACTIVE, p.getListingStatus());
    }

    @Test
    void stockRuleLeavesHiddenListingsHidden() {
        Product p = validProduct();
        p.setListingStatus(ListingStatus.INACTIVE);
        p.setAvailableQuantity(BigDecimal.ZERO);
        p.applyStockRule();
        assertEquals(ListingStatus.INACTIVE, p.getListingStatus());
    }

    @Test
    void lowStockThreshold() {
        assertTrue(ProductOperations.isLowStock(new BigDecimal("9.99")));
        assertFalse(ProductOperations.isLowStock(BigDecimal.TEN));
        assertFalse(ProductOperations.isLowStock(BigDecimal.ZERO));   // that is "out of stock", not "low"
    }
}
