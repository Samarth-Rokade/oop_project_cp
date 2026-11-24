package ui;

import model.PriceHistory;
import model.Product;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.List;

/** Read-only list of a product's past price changes. */
public class PriceHistoryDialog extends JDialog {

    public PriceHistoryDialog(Window owner, Product product, List<PriceHistory> history) {
        super(owner, "Price history - " + product.getProductName(), ModalityType.APPLICATION_MODAL);

        JLabel heading = new JLabel(product.getProductName() + "  •  current price "
                + Formats.money(product.getPricePerUnit()) + " / " + product.getUnitCode());
        heading.setFont(Theme.HEADING);
        heading.setBorder(BorderFactory.createEmptyBorder(14, 16, 10, 16));

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Changed on", "Old price", "New price", "Change", "Changed by"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        history.forEach(h -> model.addRow(new Object[]{
                Formats.dateTime(h.getChangedAt()),
                Formats.money(h.getOldPrice()),
                Formats.money(h.getNewPrice()),
                h.getChangePercent(),
                h.getChangedByName() == null ? "-" : h.getChangedByName()
        }));

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(3).setCellRenderer(new ChangeRenderer());

        Component center;
        if (history.isEmpty()) {
            JLabel empty = new JLabel("No price changes recorded yet.", SwingConstants.CENTER);
            empty.setForeground(Theme.TEXT_MUTED);
            center = empty;
        } else {
            center = new JScrollPane(table);
        }

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(close);

        getContentPane().add(heading, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(close);

        setSize(new Dimension(640, 360));
        setLocationRelativeTo(owner);
    }

    /** Price increases in red, decreases in green. */
    private static class ChangeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            BigDecimal pct = (BigDecimal) value;
            setText((pct.signum() > 0 ? "▲ +" : pct.signum() < 0 ? "▼ " : "") + pct.toPlainString() + " %");
            if (!isSelected) {
                setForeground(pct.signum() > 0 ? new Color(0xC62828)
                        : pct.signum() < 0 ? new Color(0x2E7D32) : table.getForeground());
            }
            return this;
        }
    }
}
