package ui;

import model.Product;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BiPredicate;

/** Full product information with seller details and an "Add to cart" action. */
public class ProductDetailsDialog extends JDialog {

    public ProductDetailsDialog(Window owner, Product p, BiPredicate<Product, BigDecimal> onAdd) {
        super(owner, p.getProductName(), ModalityType.APPLICATION_MODAL);

        JLabel title = new JLabel(p.getProductName());
        title.setFont(Theme.TITLE);
        JLabel price = new JLabel("<html><span style='font-size:16pt;font-weight:bold;color:#2E7D32'>"
                + Formats.money(p.getPricePerUnit()) + "</span><span style='color:#6B7280'> per "
                + p.getUnitCode() + "</span></html>");

        JPanel info = new JPanel(new GridBagLayout());
        info.setOpaque(false);
        String[][] rows = {
                {"Category", p.getCategoryName()},
                {"Available", Formats.quantity(p.getAvailableQuantity()) + " " + p.getUnitCode()},
                {"Harvested", p.getHarvestDate() == null ? "-" : Formats.date(p.getHarvestDate())},
                {"Farm", p.getFarmName()},
                {"Farmer", p.getFarmerName()},
        };
        int r = 0;
        for (String[] row : rows) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = r++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(4, 0, 4, 18);
            JLabel key = new JLabel(row[0]);
            key.setForeground(Theme.TEXT_MUTED);
            info.add(key, c);
            c.gridx = 1;
            c.weightx = 1;
            info.add(new JLabel(row[1]), c);
        }

        String desc = p.getDescription() == null || p.getDescription().isBlank()
                ? "No description provided." : p.getDescription();
        JLabel description = new JLabel("<html><div style='width:340px'>" + escape(desc) + "</div></html>");

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(price, BorderLayout.CENTER);
        center.add(top, BorderLayout.NORTH);
        center.add(info, BorderLayout.CENTER);
        center.add(description, BorderLayout.SOUTH);
        center.setBorder(BorderFactory.createEmptyBorder(20, 22, 10, 22));

        double max = p.getAvailableQuantity().doubleValue();
        JSpinner qty = new JSpinner(new SpinnerNumberModel(Math.min(1.0, max), 0.01, max, 1.0));
        qty.setEditor(new JSpinner.NumberEditor(qty, "0.##"));
        JButton add = new JButton("Add to cart");
        add.setBackground(Theme.PRIMARY);
        add.setForeground(Color.WHITE);
        add.addActionListener(e -> {
            BigDecimal quantity = BigDecimal.valueOf(((Number) qty.getValue()).doubleValue())
                    .setScale(2, RoundingMode.HALF_UP);
            if (onAdd.test(p, quantity)) {
                dispose();
            }
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.add(new JLabel("Quantity"));
        buttons.add(qty);
        buttons.add(new JLabel(p.getUnitCode()));
        buttons.add(close);
        buttons.add(add);

        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(add);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
