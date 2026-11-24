package ui;

import exception.ValidationException;
import model.Address;
import model.Buyer;
import model.Cart;
import model.CartItem;
import model.PaymentMethod;
import model.Product;
import service.AddressService;
import service.CatalogService;
import service.OrderService;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
 * Cart contents plus checkout: address, payment method, review and place.
 */
public class CartPanel extends JPanel {

    private static final String EMPTY_MESSAGE = "Your cart is empty. Open \"Browse Products\" to add something.";

    private final Buyer buyer;
    private final Cart cart;
    private final OrderService orderService;
    private final AddressService addressService;
    private final CatalogService catalogService;
    private final Consumer<List<Integer>> onOrderPlaced;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Product", "Farm", "Price", "Quantity", "Line total"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final List<CartItem> rows = new ArrayList<>();

    private final JLabel noticeLabel = new JLabel(" ");
    private final JLabel itemsLabel = new JLabel();
    private final JLabel farmsLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();
    private final JComboBox<Address> addressBox = new JComboBox<>();
    private final JComboBox<PaymentMethod> paymentBox = new JComboBox<>(PaymentMethod.values());
    private final JButton placeButton = new JButton("Review & place order");
    private final JButton changeQtyButton = new JButton("Change quantity");
    private final JButton removeButton = new JButton("Remove");
    private final JButton clearButton = new JButton("Clear cart");

    private boolean addressesLoaded;

    public CartPanel(Buyer buyer, Cart cart, OrderService orderService, AddressService addressService,
                     CatalogService catalogService, Consumer<List<Integer>> onOrderPlaced) {
        super(new BorderLayout(20, 0));
        this.buyer = buyer;
        this.cart = cart;
        this.orderService = orderService;
        this.addressService = addressService;
        this.catalogService = catalogService;
        this.onOrderPlaced = onOrderPlaced;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        add(buildCartSide(), BorderLayout.CENTER);
        add(buildCheckoutCard(), BorderLayout.EAST);

        cart.addChangeListener(this::refreshTable);
        refreshTable();
    }

    private JPanel buildCartSide() {
        JLabel title = new JLabel("Your Cart");
        title.setFont(Theme.TITLE);
        noticeLabel.setForeground(new Color(0xE65100));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(noticeLabel, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        Tables.style(table);
        Tables.alignRight(table, 2, 3, 4);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());

        changeQtyButton.addActionListener(e -> changeQuantity());
        removeButton.addActionListener(e -> selected().ifPresent(i -> cart.remove(i.getProduct().getProductId())));
        clearButton.addActionListener(e -> {
            if (Dialogs.confirm(this, "Remove everything from your cart?")) {
                cart.clear();
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(clearButton);
        buttons.add(removeButton);
        buttons.add(changeQtyButton);
        buttons.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.add(top, BorderLayout.NORTH);
        side.add(Tables.scroll(table), BorderLayout.CENTER);
        side.add(buttons, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildCheckoutCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E0E0)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JLabel heading = new JLabel("Order summary");
        heading.setFont(Theme.HEADING);
        totalLabel.setFont(Theme.TITLE.deriveFont(26f));
        totalLabel.setForeground(Theme.PRIMARY_DARK);
        farmsLabel.setForeground(Theme.TEXT_MUTED);

        JButton newAddress = new JButton("+ New address");
        newAddress.putClientProperty("JButton.buttonType", "borderless");
        newAddress.addActionListener(e -> new AddressDialog(SwingUtilities.getWindowAncestor(this),
                buyer, addressService, saved -> loadAddresses(saved.getAddressId())).setVisible(true));

        JLabel simulated = new JLabel("<html><small>Payments are simulated for this project - "
                + "no real money is charged.</small></html>");
        simulated.setForeground(Theme.TEXT_MUTED);

        placeButton.setBackground(Theme.PRIMARY);
        placeButton.setForeground(Color.WHITE);
        placeButton.setFont(placeButton.getFont().deriveFont(Font.BOLD, 14f));
        placeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        placeButton.addActionListener(e -> reviewAndPlace());

        addressBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        paymentBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JComponent[] parts = {
                heading, spacer(12), itemsLabel, spacer(4), farmsLabel, spacer(14),
                new JLabel("Estimated total"), totalLabel, spacer(18),
                new JLabel("Deliver to"), spacer(6), addressBox, newAddress, spacer(10),
                new JLabel("Payment method"), spacer(6), paymentBox, spacer(6), simulated, spacer(18),
                placeButton
        };
        for (JComponent c : parts) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            card.add(c);
        }
        // Fixed width; the card keeps its natural height at the top
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(320, 0));
        wrap.add(card, BorderLayout.NORTH);
        return wrap;
    }

    private static JComponent spacer(int height) {
        return (JComponent) Box.createVerticalStrut(height);
    }

    // ------------------------------------------------------------------

    /** Called whenever the Cart page is opened. */
    public void onShow() {
        if (!addressesLoaded) {
            loadAddresses(null);
        }
        refreshPricesAndStock();
    }

    private void loadAddresses(Integer selectId) {
        Async.run(this, () -> addressService.getAddresses(buyer), addresses -> {
            addressesLoaded = true;
            addressBox.removeAllItems();
            addresses.forEach(addressBox::addItem);
            for (Address a : addresses) {
                if (selectId != null ? a.getAddressId() == selectId : a.isDefaultAddress()) {
                    addressBox.setSelectedItem(a);
                }
            }
        });
    }

    /** Re-reads prices and stock so the cart never shows stale information. */
    private void refreshPricesAndStock() {
        if (cart.isEmpty()) {
            return;
        }
        Map<Integer, BigDecimal> oldPrices = new HashMap<>();
        cart.getItems().forEach(i -> oldPrices.put(i.getProduct().getProductId(), i.getProduct().getPricePerUnit()));

        Async.run(this, catalogService::currentListings, current -> {
            int before = cart.size();
            cart.refreshProducts(current::get);
            long priceChanges = cart.getItems().stream()
                    .filter(i -> i.getProduct().getPricePerUnit().compareTo(oldPrices.get(i.getProduct().getProductId())) != 0)
                    .count();
            StringJoiner notice = new StringJoiner("  ");
            if (cart.size() < before) {
                notice.add((before - cart.size()) + " item(s) are no longer available and were removed.");
            }
            if (priceChanges > 0) {
                notice.add(priceChanges + " price(s) changed since you added them.");
            }
            noticeLabel.setText(notice.length() == 0 ? " " : "⚠  " + notice);
        });
    }

    private void refreshTable() {
        Integer keep = selected().map(i -> i.getProduct().getProductId()).orElse(null);
        rows.clear();
        rows.addAll(cart.getItems());
        tableModel.setRowCount(0);
        for (CartItem i : rows) {
            Product p = i.getProduct();
            String qty = Formats.quantity(i.getQuantity()) + " " + p.getUnitCode();
            if (i.getQuantity().compareTo(p.getAvailableQuantity()) > 0) {
                qty += "  (only " + Formats.quantity(p.getAvailableQuantity()) + " left)";
            }
            tableModel.addRow(new Object[]{p.getProductName(), p.getFarmName(),
                    Formats.money(p.getPricePerUnit()) + " / " + p.getUnitCode(), qty,
                    Formats.money(i.getLineTotal())});
        }
        if (keep != null) {
            for (int r = 0; r < rows.size(); r++) {
                if (rows.get(r).getProduct().getProductId() == keep) {
                    table.setRowSelectionInterval(r, r);
                }
            }
        }

        int farms = cart.groupByFarmer().size();
        itemsLabel.setText(cart.size() + (cart.size() == 1 ? " item" : " items"));
        farmsLabel.setText(farms <= 1 ? (farms == 0 ? "Your cart is empty." : "From 1 farm")
                : "From " + farms + " farms - placed as " + farms + " separate orders");
        totalLabel.setText(Formats.money(cart.getEstimatedTotal()));
        if (cart.isEmpty()) {
            noticeLabel.setText(EMPTY_MESSAGE);
        } else if (EMPTY_MESSAGE.equals(noticeLabel.getText())) {
            noticeLabel.setText(" ");
        }
        updateButtons();
    }

    private java.util.Optional<CartItem> selected() {
        int r = table.getSelectedRow();
        return r < 0 || r >= rows.size() ? java.util.Optional.empty() : java.util.Optional.of(rows.get(r));
    }

    private void updateButtons() {
        boolean hasSelection = selected().isPresent();
        changeQtyButton.setEnabled(hasSelection);
        removeButton.setEnabled(hasSelection);
        clearButton.setEnabled(!cart.isEmpty());
        placeButton.setEnabled(!cart.isEmpty());
    }

    private void changeQuantity() {
        selected().ifPresent(item -> {
            Product p = item.getProduct();
            Object input = JOptionPane.showInputDialog(this,
                    "Quantity of " + p.getProductName() + " (" + p.getUnitCode() + "), max "
                            + Formats.quantity(p.getAvailableQuantity()) + ":",
                    "Change quantity", JOptionPane.PLAIN_MESSAGE, null, null, Formats.quantity(item.getQuantity()));
            if (input == null) {
                return;
            }
            try {
                cart.setQuantity(p.getProductId(), new BigDecimal(input.toString().trim()));
            } catch (NumberFormatException e) {
                Dialogs.showError(this, new ValidationException("Please enter a number, e.g. 2 or 2.5."));
            } catch (ValidationException e) {
                Dialogs.showError(this, e);
            }
        });
    }

    // ------------------------------------------------------------------ checkout

    private void reviewAndPlace() {
        Address address = (Address) addressBox.getSelectedItem();
        PaymentMethod method = (PaymentMethod) paymentBox.getSelectedItem();
        if (address == null) {
            Dialogs.showError(this, new ValidationException("Add a delivery address first (\"+ New address\")."));
            return;
        }

        int choice = JOptionPane.showOptionDialog(this, reviewHtml(address, method), "Review your order",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Place order", "Back to cart"}, "Place order");
        if (choice != 0) {
            return;
        }

        placeButton.setEnabled(false);
        placeButton.setText("Placing order...");
        Async.run(this, () -> orderService.placeOrder(buyer, cart, address.getAddressId(), method), orderIds -> {
            placeButton.setText("Review & place order");
            cart.clear();
            noticeLabel.setText(" ");
            StringJoiner ids = new StringJoiner(", #", "#", "");
            orderIds.forEach(id -> ids.add(String.valueOf(id)));
            Dialogs.info(this, (orderIds.size() == 1 ? "Order " : "Orders ") + ids + " placed successfully.\n"
                    + "The farmer will confirm it soon.");
            onOrderPlaced.accept(orderIds);
        }, error -> {
            placeButton.setText("Review & place order");
            updateButtons();
            Dialogs.showError(this, error);
            refreshPricesAndStock();             // show the buyer what changed
        });
    }

    /** Items grouped by farm, as they will become separate orders. */
    private String reviewHtml(Address address, PaymentMethod method) {
        StringBuilder html = new StringBuilder("<html><div style='width:420px'>");
        Map<Integer, List<CartItem>> groups = cart.groupByFarmer();
        int n = 1;
        for (List<CartItem> items : groups.values()) {
            BigDecimal subtotal = items.stream().map(CartItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            html.append("<b>")
                .append(groups.size() > 1 ? "Order " + n++ + " - " : "")
                .append(items.get(0).getProduct().getFarmName()).append("</b><table width='100%'>");
            for (CartItem i : items) {
                Product p = i.getProduct();
                html.append("<tr><td>").append(p.getProductName()).append("</td><td>")
                    .append(Formats.quantity(i.getQuantity())).append(' ').append(p.getUnitCode())
                    .append(" × ").append(Formats.money(p.getPricePerUnit()))
                    .append("</td><td align='right'>").append(Formats.money(i.getLineTotal())).append("</td></tr>");
            }
            html.append("<tr><td colspan='2'><i>Subtotal</i></td><td align='right'><i>")
                .append(Formats.money(subtotal)).append("</i></td></tr></table><br>");
        }
        html.append("<b>Total: ").append(Formats.money(cart.getEstimatedTotal())).append("</b><br><br>")
            .append("Deliver to: ").append(address).append("<br>")
            .append("Payment: ").append(method).append("<br><br>")
            .append("<small>Stock and prices are checked again when you place the order.</small>")
            .append("</div></html>");
        return html.toString();
    }
}
