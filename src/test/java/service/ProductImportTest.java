package service;

import exception.ValidationException;
import model.Category;
import model.ListingStatus;
import model.Unit;
import org.junit.jupiter.api.Test;
import service.ProductImportService.ImportPreview;
import util.Csv;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CSV import parsing - no database needed. */
class ProductImportTest {

    private static final List<Category> CATEGORIES = List.of(
            new Category(1, "Vegetables", null), new Category(2, "Fruits", null));
    private static final List<Unit> UNITS = List.of(new Unit("KG", "Kilogram"), new Unit("DOZEN", "Dozen"));

    private static ImportPreview parse(String csv) throws ValidationException {
        return ProductImportService.parse(Csv.parse(csv), CATEGORIES, UNITS);
    }

    @Test
    void readsValidRowsInAnyColumnOrder() throws ValidationException {
        ImportPreview p = parse("""
                Price,Product Name,unit,category,quantity,description
                28.50,Tomato,kg,vegetables,120,"Firm, red"
                55,Banana,Dozen,Fruits,0,
                """);
        assertEquals(2, p.valid().size());
        assertTrue(p.problems().isEmpty());
        var tomato = p.valid().get(0).product();
        assertEquals("Tomato", tomato.getProductName());
        assertEquals(1, tomato.getCategoryId());
        assertEquals("KG", tomato.getUnitCode());
        assertEquals(new BigDecimal("28.50"), tomato.getPricePerUnit());
        assertEquals("Firm, red", tomato.getDescription());
        // zero stock -> listed as out of stock
        assertEquals(ListingStatus.OUT_OF_STOCK, p.valid().get(1).product().getListingStatus());
    }

    @Test
    void reportsBadRowsWithTheirRowNumbers() throws ValidationException {
        ImportPreview p = parse("""
                product_name,category,unit,quantity,price,harvest_date
                Tomato,Vegetables,KG,10,28,2026-11-01
                Grapes,Flowers,KG,10,90,
                Onion,Vegetables,BOX,10,32,
                Potato,Vegetables,KG,ten,22,
                Chilli,Vegetables,KG,5,-4,
                Garlic,Vegetables,KG,5,120,20-11-2026
                """);
        assertEquals(1, p.valid().size());
        assertEquals(5, p.problems().size());
        assertTrue(p.problems().get(0).startsWith("Row 3: unknown category"));
        assertTrue(p.problems().get(1).contains("unknown unit"));
        assertTrue(p.problems().get(2).contains("not a number"));
        assertTrue(p.problems().get(3).contains("greater than zero"));
        assertTrue(p.problems().get(4).contains("harvest_date"));
    }

    @Test
    void rejectsMissingHeaderColumns() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> parse("name,price\nTomato,28\n"));
        assertTrue(e.getMessage().contains("Missing: product_name, category, unit, quantity"));
    }

    @Test
    void rejectsEmptyFiles() {
        assertThrows(ValidationException.class, () -> parse(""));
        assertThrows(ValidationException.class, () -> parse("product_name,category,unit,quantity,price\n"));
    }
}
