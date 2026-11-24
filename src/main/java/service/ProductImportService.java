package service;

import config.DatabaseConfig;
import dao.ProductDAO;
import exception.DataAccessException;
import exception.ValidationException;
import model.Category;
import model.Farmer;
import model.ListingStatus;
import model.Product;
import model.Unit;
import util.Csv;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Bulk-adds products from a CSV file (spec 9.12).
 *
 * Step 1 - preview: every row is read and checked; nothing is saved.
 * Step 2 - import: the valid rows are inserted in ONE transaction.
 *
 * Expected header (column order does not matter, case is ignored):
 *   product_name, category, unit, quantity, price [, harvest_date, description]
 */
public class ProductImportService {

    public static final List<String> REQUIRED = List.of("product_name", "category", "unit", "quantity", "price");
    public static final List<String> OPTIONAL = List.of("harvest_date", "description");
    static final int MAX_ROWS = 500;
    static final long MAX_FILE_BYTES = 1_000_000;

    /** A checked row and the line number it came from. */
    public record ImportRow(int line, Product product) {
    }

    public record ImportPreview(List<ImportRow> valid, List<String> problems, int totalRows) {

        public boolean hasProblems() {
            return !problems.isEmpty();
        }
    }

    private final LookupService lookups;
    private final ProductDAO productDAO = new ProductDAO();

    public ProductImportService(LookupService lookups) {
        this.lookups = lookups;
    }

    public ImportPreview preview(Path file) throws ValidationException {
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                throw new ValidationException("The file is larger than 1 MB. Split it into smaller files.");
            }
            return parse(Csv.read(file), lookups.getCategories(), lookups.getUnits());
        } catch (IOException e) {
            throw new ValidationException("Could not read " + file.getFileName() + ": " + e.getMessage());
        }
    }

    /** Inserts all rows or none. New products start ACTIVE (stock rule applies). */
    public int importProducts(Farmer farmer, List<ImportRow> rows) throws ValidationException {
        if (rows.isEmpty()) {
            throw new ValidationException("There are no valid rows to import.");
        }
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                for (ImportRow row : rows) {
                    Product p = new Product(row.product());
                    p.setFarmerId(farmer.getUserId());
                    productDAO.insert(con, p);
                }
                con.commit();
                return rows.size();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("The import failed and nothing was saved", e);
        }
    }

    /** Writes an example file the farmer can fill in. */
    public static void writeTemplate(Path file) throws IOException {
        List<String> header = new ArrayList<>(REQUIRED);
        header.addAll(OPTIONAL);
        Csv.write(file, header, List.of(
                List.of("Tomato", "Vegetables", "KG", "120", "28.50", LocalDate.now().minusDays(1), "Firm red tomatoes"),
                List.of("Banana", "Fruits", "DOZEN", "40", "55", "", "Robusta, ripe in 2 days")));
    }

    // ------------------------------------------------------------------ parsing (no database - unit-tested)

    static ImportPreview parse(List<List<String>> records, List<Category> categories, List<Unit> units)
            throws ValidationException {
        if (records.isEmpty()) {
            throw new ValidationException("The file is empty.");
        }

        Map<String, Integer> col = new HashMap<>();
        List<String> header = records.get(0);
        for (int i = 0; i < header.size(); i++) {
            col.put(header.get(i).trim().toLowerCase(Locale.ROOT).replace(' ', '_'), i);
        }
        List<String> missing = REQUIRED.stream().filter(h -> !col.containsKey(h)).toList();
        if (!missing.isEmpty()) {
            throw new ValidationException("The first row must be a header with these columns: "
                    + String.join(", ", REQUIRED) + ".\nMissing: " + String.join(", ", missing) + ".");
        }
        int dataRows = records.size() - 1;
        if (dataRows == 0) {
            throw new ValidationException("The file has a header but no products.");
        }
        if (dataRows > MAX_ROWS) {
            throw new ValidationException("At most " + MAX_ROWS + " products can be imported at once (file has " + dataRows + ").");
        }

        List<ImportRow> valid = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (int r = 1; r < records.size(); r++) {
            int line = r + 1;                     // spreadsheet row number
            List<String> rec = records.get(r);
            try {
                valid.add(new ImportRow(line, toProduct(rec, col, categories, units)));
            } catch (ValidationException e) {
                problems.add("Row " + line + ": " + e.getMessage().replace("\n", " "));
            }
        }
        return new ImportPreview(valid, problems, dataRows);
    }

    private static Product toProduct(List<String> rec, Map<String, Integer> col,
                                     List<Category> categories, List<Unit> units) throws ValidationException {
        Product p = new Product();
        p.setProductName(cell(rec, col, "product_name").trim());

        String categoryText = cell(rec, col, "category").trim();
        Category category = categories.stream()
                .filter(c -> c.getCategoryName().equalsIgnoreCase(categoryText))
                .findFirst()
                .orElseThrow(() -> new ValidationException("unknown category \"" + categoryText + "\""));
        p.setCategoryId(category.getCategoryId());
        p.setCategoryName(category.getCategoryName());

        String unitText = cell(rec, col, "unit").trim();
        Unit unit = units.stream()
                .filter(u -> u.getUnitCode().equalsIgnoreCase(unitText) || u.getUnitName().equalsIgnoreCase(unitText))
                .findFirst()
                .orElseThrow(() -> new ValidationException("unknown unit \"" + unitText + "\""));
        p.setUnitCode(unit.getUnitCode());

        p.setAvailableQuantity(number(cell(rec, col, "quantity"), "quantity"));
        p.setPricePerUnit(number(cell(rec, col, "price"), "price"));

        String harvest = cell(rec, col, "harvest_date").trim();
        if (!harvest.isEmpty()) {
            try {
                p.setHarvestDate(LocalDate.parse(harvest));
            } catch (DateTimeParseException e) {
                throw new ValidationException("harvest_date must look like 2026-11-20");
            }
        }
        String description = cell(rec, col, "description").trim();
        p.setDescription(description.isEmpty() ? null : description);
        p.setListingStatus(ListingStatus.ACTIVE);

        ProductService.validate(p);              // same rules as the product form
        p.applyStockRule();
        return p;
    }

    /** Missing trailing cells count as empty. */
    private static String cell(List<String> rec, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        return i == null || i >= rec.size() ? "" : Optional.ofNullable(rec.get(i)).orElse("");
    }

    private static BigDecimal number(String text, String field) throws ValidationException {
        String t = text.trim().replace(",", "").replace("₹", "");
        if (t.isEmpty()) {
            throw new ValidationException(field + " is empty");
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " \"" + text.trim() + "\" is not a number");
        }
    }
}
