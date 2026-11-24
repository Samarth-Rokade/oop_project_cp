package ui;

import interfaces.ProductOperations;
import model.ListingStatus;
import model.Product;
import util.Formats;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Formats product table cells: money, stock with unit, coloured status. */
public class ProductCellRenderer extends DefaultTableCellRenderer {

    private static final Color GREEN = new Color(0x2E7D32);
    private static final Color ORANGE = new Color(0xE65100);
    private static final Color RED = new Color(0xC62828);
    private static final Color GREY = new Color(0x6B7280);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setFont(table.getFont());
        if (!isSelected) {
            label.setForeground(table.getForeground());
        }

        ProductTableModel model = (ProductTableModel) table.getModel();
        Product product = model.getProductAt(table.convertRowIndexToModel(row));
        int modelColumn = table.convertColumnIndexToModel(column);

        switch (modelColumn) {
            case ProductTableModel.COL_NAME -> label.setFont(table.getFont().deriveFont(Font.BOLD));
            case ProductTableModel.COL_STOCK -> {
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                label.setText(Formats.quantity((BigDecimal) value) + "  " + product.getUnitCode());
                if (!isSelected) {
                    BigDecimal qty = (BigDecimal) value;
                    if (qty.signum() <= 0) {
                        label.setForeground(RED);
                    } else if (ProductOperations.isLowStock(qty)) {
                        label.setForeground(ORANGE);
                        label.setText(label.getText() + "  (low)");
                    }
                }
            }
            case ProductTableModel.COL_PRICE -> {
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                label.setText(Formats.money((BigDecimal) value) + " / " + product.getUnitCode());
            }
            case ProductTableModel.COL_HARVEST -> label.setText(Formats.date((LocalDate) value));
            case ProductTableModel.COL_UPDATED -> {
                label.setText(Formats.dateTime((LocalDateTime) value));
                if (!isSelected) {
                    label.setForeground(GREY);
                }
            }
            case ProductTableModel.COL_STATUS -> {
                ListingStatus status = (ListingStatus) value;
                label.setText("●  " + status.getLabel());
                if (!isSelected) {
                    label.setForeground(switch (status) {
                        case ACTIVE -> GREEN;
                        case OUT_OF_STOCK -> RED;
                        case INACTIVE -> GREY;
                        case REMOVED -> ORANGE;
                    });
                }
            }
            default -> { }
        }
        return label;
    }
}
