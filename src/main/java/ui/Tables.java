package ui;

import model.AccountStatus;
import model.Farmer;
import model.ListingStatus;
import model.OrderStatus;
import model.PaymentStatus;

import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;

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

    /** Right-aligned BigDecimal columns shown as money (₹1,234.50). */
    public static void moneyColumn(JTable table, int... columns) {
        formatColumn(table, SwingConstants.RIGHT, v -> v instanceof BigDecimal d ? Formats.money(d) : String.valueOf(v), columns);
    }

    /** Right-aligned numeric columns without trailing zeros (12.5, 40). */
    public static void numberColumn(JTable table, int... columns) {
        formatColumn(table, SwingConstants.RIGHT, v -> v instanceof BigDecimal d ? Formats.quantity(d) : String.valueOf(v), columns);
    }

    public static void dateTimeColumn(JTable table, int... columns) {
        formatColumn(table, SwingConstants.LEFT, v -> v instanceof LocalDateTime t ? Formats.dateTime(t) : String.valueOf(v), columns);
    }

    private static void formatColumn(JTable table, int alignment, Function<Object, String> format, int... columns) {
        for (int c : columns) {
            table.getColumnModel().getColumn(c).setCellRenderer(new PaddedRenderer(alignment) {
                @Override
                protected void setValue(Object value) {
                    setText(value == null ? "" : format.apply(value));
                }
            });
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

    private static final Color GREEN = new Color(0x2E7D32);
    private static final Color ORANGE = new Color(0xE65100);
    private static final Color RED = new Color(0xC62828);

    /** Colour for any of the status enums shown in tables. */
    static Color colorOf(Object value, Color fallback) {
        if (value instanceof OrderStatus s) {
            return colorOf(s);
        }
        if (value instanceof PaymentStatus s) {
            return switch (s) {
                case PAID -> GREEN;
                case PENDING -> ORANGE;
                default -> Theme.TEXT_MUTED;
            };
        }
        if (value instanceof ListingStatus s) {
            return switch (s) {
                case ACTIVE -> GREEN;
                case OUT_OF_STOCK -> RED;
                case INACTIVE -> Theme.TEXT_MUTED;
                case REMOVED -> ORANGE;
            };
        }
        if (value instanceof AccountStatus s) {
            return switch (s) {
                case ACTIVE -> GREEN;
                case PENDING -> ORANGE;
                case SUSPENDED -> RED;
            };
        }
        if (value instanceof Farmer.VerificationStatus s) {
            return switch (s) {
                case VERIFIED -> GREEN;
                case PENDING -> ORANGE;
                case REJECTED -> RED;
            };
        }
        return fallback;
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

    /** Coloured dot + label for the status enums. */
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
            } else if (value instanceof ListingStatus status) {
                label.setText("●  " + status.getLabel());
            } else if (value instanceof AccountStatus || value instanceof Farmer.VerificationStatus) {
                String name = ((Enum<?>) value).name();
                label.setText("●  " + name.charAt(0) + name.substring(1).toLowerCase());
            }
            if (!isSelected) {
                label.setForeground(colorOf(value, table.getForeground()));
            }
            return label;
        }
    }
}
