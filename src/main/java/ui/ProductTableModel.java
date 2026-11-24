package ui;

import model.ListingStatus;
import model.Product;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Table model for a list of products. Columns return real types
 * (BigDecimal, LocalDate...) so sorting is numeric/chronological;
 * {@link ProductCellRenderer} does the formatting.
 */
public class ProductTableModel extends AbstractTableModel {

    static final int COL_NAME = 0;
    static final int COL_CATEGORY = 1;
    static final int COL_STOCK = 2;
    static final int COL_PRICE = 3;
    static final int COL_HARVEST = 4;
    static final int COL_STATUS = 5;
    static final int COL_UPDATED = 6;

    private static final String[] COLUMNS =
            {"Product", "Category", "Stock", "Price / unit", "Harvested", "Status", "Last updated"};
    private static final Class<?>[] TYPES =
            {String.class, String.class, BigDecimal.class, BigDecimal.class,
             LocalDate.class, ListingStatus.class, LocalDateTime.class};

    private List<Product> products = new ArrayList<>();

    public void setProducts(List<Product> products) {
        this.products = new ArrayList<>(products);
        fireTableDataChanged();
    }

    public Product getProductAt(int modelRow) {
        return products.get(modelRow);
    }

    public Optional<Integer> indexOf(int productId) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getProductId() == productId) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    @Override
    public int getRowCount() {
        return products.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return TYPES[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Product p = products.get(row);
        return switch (column) {
            case COL_NAME -> p.getProductName();
            case COL_CATEGORY -> p.getCategoryName();
            case COL_STOCK -> p.getAvailableQuantity();
            case COL_PRICE -> p.getPricePerUnit();
            case COL_HARVEST -> p.getHarvestDate();
            case COL_STATUS -> p.getListingStatus();
            case COL_UPDATED -> p.getUpdatedAt();
            default -> null;
        };
    }
}
