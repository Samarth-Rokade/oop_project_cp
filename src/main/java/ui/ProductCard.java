package ui;

import model.Product;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * One product tile on the buyer's Browse page.
 */
public class ProductCard extends JPanel {

    private static final Color BORDER = new Color(0xE0E0E0);

    /**
     * @param onAdd     tries to add (product, quantity) to the cart; returns true on success
     * @param onDetails opens the details dialog
     */
    public ProductCard(Product product, BiPredicate<Product, BigDecimal> onAdd, Consumer<Product> onDetails) {
        super(new BorderLayout(0, 10));
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        // ----- header: name + category chip
        JLabel name = new JLabel(product.getProductName());
        name.setFont(Theme.HEADING);
        JLabel chip = new JLabel(product.getCategoryName());
        chip.setOpaque(true);
        chip.setBackground(Theme.PRIMARY_LIGHT);
        chip.setForeground(Theme.PRIMARY_DARK);
        chip.setFont(chip.getFont().deriveFont(11f));
        chip.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(name, BorderLayout.CENTER);
        header.add(chip, BorderLayout.EAST);

        // ----- body: farm, price, stock
        JLabel farm = new JLabel("by " + product.getFarmName());
        farm.setForeground(Theme.TEXT_MUTED);
        JLabel price = new JLabel("<html><span style='font-size:15pt;font-weight:bold;color:#2E7D32'>"
                + Formats.money(product.getPricePerUnit()) + "</span>"
                + "<span style='color:#6B7280'> / " + product.getUnitCode() + "</span></html>");
        String harvest = product.getHarvestDate() == null ? "" : "  •  harvested " + Formats.date(product.getHarvestDate());
        JLabel stock = new JLabel(Formats.quantity(product.getAvailableQuantity()) + " " + product.getUnitCode()
                + " available" + harvest);
        stock.setForeground(Theme.TEXT_MUTED);
        stock.setFont(stock.getFont().deriveFont(11.5f));

        JLabel detailsLink = new JLabel("View details ›");
        detailsLink.setForeground(Theme.PRIMARY);
        detailsLink.setFont(detailsLink.getFont().deriveFont(11.5f));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        for (JLabel l : new JLabel[]{farm, price, stock, detailsLink}) {
            l.setAlignmentX(LEFT_ALIGNMENT);
            body.add(l);
            body.add(Box.createVerticalStrut(4));
        }

        // ----- footer: quantity + buttons
        double max = product.getAvailableQuantity().doubleValue();
        JSpinner qty = new JSpinner(new SpinnerNumberModel(Math.min(1.0, max), 0.01, max, 1.0));
        qty.setEditor(new JSpinner.NumberEditor(qty, "0.##"));
        qty.setPreferredSize(new Dimension(72, qty.getPreferredSize().height));
        qty.setToolTipText("Quantity in " + product.getUnitCode());

        JButton add = new JButton("Add to cart");
        add.setBackground(Theme.PRIMARY);
        add.setForeground(Color.WHITE);
        add.setFont(add.getFont().deriveFont(Font.BOLD));
        add.addActionListener(e -> {
            BigDecimal quantity = BigDecimal.valueOf(((Number) qty.getValue()).doubleValue())
                    .setScale(2, RoundingMode.HALF_UP);
            if (onAdd.test(product, quantity)) {
                flash(add, "Added ✓");
            }
        });

        JPanel quantityRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quantityRow.setOpaque(false);
        quantityRow.add(qty);
        quantityRow.add(new JLabel(product.getUnitCode()));

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(quantityRow, BorderLayout.WEST);
        footer.add(add, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        // Clicking anywhere on the card (not a control) opens the details
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Click for details");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onDetails.accept(product);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.PRIMARY),
                        BorderFactory.createEmptyBorder(14, 16, 14, 16)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!contains(e.getPoint())) {
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER),
                            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
                }
            }
        });
    }

    /** Briefly changes a button's text as feedback. */
    static void flash(JButton button, String text) {
        String original = button.getText();
        button.setText(text);
        button.setEnabled(false);
        Timer timer = new Timer(1200, e -> {
            button.setText(original);
            button.setEnabled(true);
        });
        timer.setRepeats(false);
        timer.start();
    }
}
