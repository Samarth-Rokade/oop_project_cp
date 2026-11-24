package ui;

import model.OrderStatus;
import model.PaymentStatus;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/** Shared table styling and renderers. */
public final class Tables {

    private Tables() {
    }

    public static void style(JTable table) {
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new PaddedRenderer(SwingConstants.LEFT));
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE0E0E0)));
        return scroll;
    }

    /** Right-aligned column (money, quantities). */
    public static void alignRight(JTable table, int... columns) {
        for (int c : columns) {
            table.getColumnModel().getColumn(c).setCellRenderer(new PaddedRenderer(SwingConstants.RIGHT));
        }
    }

    public static void statusColumn(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new StatusRenderer());
    }

    public static Color colorOf(OrderStatus status) {
        return switch (status) {
            case PLACED -> new Color(0x1565C0);
            case CONFIRMED, PACKED, DISPATCHED -> new Color(0x6A1B9A);
            case DELIVERED -> new Color(0x2E7D32);
            case REJECTED, CANCELLED, DELIVERY_FAILED -> new Color(0xC62828);
        };
    }

    private static class PaddedRenderer extends DefaultTableCellRenderer {
        PaddedRenderer(int alignment) {
            setHorizontalAlignment(alignment);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            return label;
        }
    }

    /** Coloured dot + label for OrderStatus / PaymentStatus values. */
    private static class StatusRenderer extends PaddedRenderer {
        StatusRenderer() {
            super(SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof OrderStatus status) {
                label.setText("●  " + status.getLabel());
                if (!isSelected) {
                    label.setForeground(colorOf(status));
                }
            } else if (value instanceof PaymentStatus status && !isSelected) {
                label.setForeground(switch (status) {
                    case PAID -> new Color(0x2E7D32);
                    case PENDING -> new Color(0xE65100);
                    default -> Theme.TEXT_MUTED;
                });
            } else if (!isSelected) {
                label.setForeground(table.getForeground());
            }
            return label;
        }
    }
}
