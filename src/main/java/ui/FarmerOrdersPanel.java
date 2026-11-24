package ui;

import exception.ValidationException;
import model.Farmer;
import model.Order;
import model.OrderItem;
import model.OrderStatus;
import service.OrderService;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Farmer's received orders. The main button always offers the next step
 * of the lifecycle: Accept -> Mark packed -> Dispatch -> Mark delivered.
 */
public class FarmerOrdersPanel extends JPanel {

    /** Filter tabs; each one is a Predicate on the order status. */
    private enum Filter {
        NEW("New", EnumSet.of(OrderStatus.PLACED)),
        IN_PROGRESS("In progress", EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PACKED, OrderStatus.DISPATCHED)),
        COMPLETED("Completed", EnumSet.of(OrderStatus.DELIVERED)),
        CLOSED("Rejected / cancelled", EnumSet.of(OrderStatus.REJECTED, OrderStatus.CANCELLED, OrderStatus.DELIVERY_FAILED)),
        ALL("All", EnumSet.allOf(OrderStatus.class));

        final String label;
        final Set<OrderStatus> statuses;

        Filter(String label, Set<OrderStatus> statuses) {
            this.label = label;
            this.statuses = statuses;
        }

        Predicate<Order> predicate() {
            return o -> statuses.contains(o.getStatus());
        }
    }

    private final Farmer farmer;
    private final OrderService orderService;
    private final Runnable onOrdersChanged;

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> shown = new ArrayList<>();
    private Filter filter = Filter.NEW;
    private final List<JToggleButton> filterButtons = new ArrayList<>();

    private final DefaultTableModel ordersModel = readOnlyModel(
            "Order", "Placed on", "Buyer", "Type", "Items", "Total", "Status", "Payment");
    private final JTable ordersTable = new JTable(ordersModel);
    private final DefaultTableModel itemsModel = readOnlyModel("Product", "Quantity", "Unit price", "Line total");
    private final JTable itemsTable = new JTable(itemsModel);
    private final JLabel detailHeader = new JLabel(" ");

    private final JButton nextButton = new JButton("Accept order");
    private final JButton rejectButton = new JButton("Reject");
    private final JButton failedButton = new JButton("Delivery failed");
    private Integer pendingSelection;
    private int detailSequence;

    public FarmerOrdersPanel(Farmer farmer, OrderService orderService, Runnable onOrdersChanged) {
        super(new BorderLayout());
        this.farmer = farmer;
        this.orderService = orderService;
        this.onOrdersChanged = onOrdersChanged;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        updateButtons();
    }

    private JPanel buildTop() {
        JLabel title = new JLabel("Orders");
        title.setFont(Theme.TITLE);
        JLabel subtitle = new JLabel("Accept or reject new orders, then move them along: packed → dispatched → delivered.");
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabs.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Filter f : Filter.values()) {
            JToggleButton b = new JToggleButton(f.label);
            b.putClientProperty("JButton.buttonType", "roundRect");
            b.setFocusable(false);
            b.setSelected(f == filter);
            b.addActionListener(e -> {
                filter = f;
                applyFilter(null);
            });
            group.add(b);
            tabs.add(b);
            filterButtons.add(b);
        }

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(subtitle);

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        top.add(titles, BorderLayout.NORTH);
        top.add(tabs, BorderLayout.CENTER);
        return top;
    }

    private JSplitPane buildCenter() {
        Tables.style(ordersTable);
        Tables.alignRight(ordersTable, 4, 5);
        Tables.statusColumn(ordersTable, 6);
        Tables.statusColumn(ordersTable, 7);
        int[] widths = {70, 150, 180, 100, 60, 110, 120, 90};
        for (int i = 0; i < widths.length; i++) {
            ordersTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedOrder();
            }
        });

        Tables.style(itemsTable);
        Tables.alignRight(itemsTable, 1, 2, 3);

        detailHeader.setBorder(BorderFactory.createEmptyBorder(10, 2, 8, 2));
        JPanel detail = new JPanel(new BorderLayout());
        detail.setOpaque(false);
        detail.add(detailHeader, BorderLayout.NORTH);
        detail.add(Tables.scroll(itemsTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Tables.scroll(ordersTable), detail);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    private JPanel buildActions() {
        nextButton.setBackground(Theme.PRIMARY);
        nextButton.setForeground(Color.WHITE);
        nextButton.setFont(nextButton.getFont().deriveFont(Font.BOLD));
        nextButton.addActionListener(e -> advanceSelected());
        rejectButton.addActionListener(e -> rejectSelected());
        failedButton.addActionListener(e -> deliveryFailedSelected());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(refresh);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(failedButton);
        right.add(rejectButton);
        right.add(nextButton);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ------------------------------------------------------------------ data

    public void reload() {
        Integer select = pendingSelection != null ? pendingSelection : selected().map(Order::getOrderId).orElse(null);
        pendingSelection = null;
        Async.run(this, () -> orderService.getOrdersOf(farmer), list -> {
            allOrders.clear();
            allOrders.addAll(list);
            updateTabLabels();
            applyFilter(select);
        });
    }

    private void updateTabLabels() {
        for (int i = 0; i < Filter.values().length; i++) {
            Filter f = Filter.values()[i];
            long count = allOrders.stream().filter(f.predicate()).count();
            filterButtons.get(i).setText(f == Filter.ALL ? f.label : f.label + "  " + count);
        }
    }

    /** In-memory filter of the loaded orders (Stream + Predicate). */
    private void applyFilter(Integer selectId) {
        shown.clear();
        shown.addAll(allOrders.stream().filter(filter.predicate()).collect(Collectors.toList()));
        ordersModel.setRowCount(0);
        for (Order o : shown) {
            ordersModel.addRow(new Object[]{
                    "#" + o.getOrderId(),
                    Formats.dateTime(o.getOrderDate()),
                    o.getBuyerName(),
                    o.getBuyerType() == null ? "" : o.getBuyerType().charAt(0) + o.getBuyerType().substring(1).toLowerCase(),
                    o.getItemCount(),
                    Formats.money(o.getTotalAmount()),
                    o.getStatus(),
                    o.getPaymentStatus() == null ? "-" : o.getPaymentStatus()
            });
        }
        int row = -1;
        for (int i = 0; i < shown.size(); i++) {
            if (selectId != null && shown.get(i).getOrderId() == selectId) {
                row = i;
            }
        }
        if (row < 0 && !shown.isEmpty()) {
            row = 0;
        }
        if (row >= 0) {
            ordersTable.setRowSelectionInterval(row, row);
            ordersTable.scrollRectToVisible(ordersTable.getCellRect(row, 0, true));
        } else {
            showSelectedOrder();
        }
    }

    private void showSelectedOrder() {
        updateButtons();
        itemsModel.setRowCount(0);
        Optional<Order> selected = selected();
        if (selected.isEmpty()) {
            detailHeader.setText(filter == Filter.NEW ? "No new orders right now." : "No orders here.");
            return;
        }
        Order o = selected.get();
        StringBuilder html = new StringBuilder("<html><b>Order #").append(o.getOrderId()).append("</b> for ")
                .append(esc(o.getBuyerName()));
        if (o.getBuyerPhone() != null) {
            html.append(" (").append(esc(o.getBuyerPhone())).append(")");
        }
        html.append("<br>Deliver to: ").append(esc(o.getDeliverTo()));
        if (o.getPaymentMethod() != null) {
            html.append(" &nbsp;•&nbsp; ").append(o.getPaymentMethod()).append(", ").append(o.getPaymentStatus());
        }
        if (o.getDeliveryPartner() != null) {
            html.append(" &nbsp;•&nbsp; Shipped via ").append(esc(o.getDeliveryPartner()));
            if (o.getTrackingReference() != null) {
                html.append(" (").append(esc(o.getTrackingReference())).append(")");
            }
        }
        detailHeader.setText(html.append("</html>").toString());

        int mySequence = ++detailSequence;
        Async.run(this, () -> orderService.getItems(farmer, o), items -> {
            if (mySequence != detailSequence) {
                return;
            }
            for (OrderItem i : items) {
                itemsModel.addRow(new Object[]{i.getProductName(),
                        Formats.quantity(i.getQuantity()) + " " + i.getUnitCode(),
                        Formats.money(i.getUnitPriceAtOrder()) + " / " + i.getUnitCode(),
                        Formats.money(i.getLineTotal())});
            }
        });
    }

    private Optional<Order> selected() {
        int r = ordersTable.getSelectedRow();
        return r < 0 || r >= shown.size() ? Optional.empty() : Optional.of(shown.get(r));
    }

    /** The main button shows the next lifecycle step for the selected order. */
    private void updateButtons() {
        OrderStatus status = selected().map(Order::getStatus).orElse(null);
        String next = status == null ? null : switch (status) {
            case PLACED -> "Accept order";
            case CONFIRMED -> "Mark as packed";
            case PACKED -> "Dispatch...";
            case DISPATCHED -> "Mark as delivered";
            default -> null;
        };
        nextButton.setVisible(next != null);
        if (next != null) {
            nextButton.setText(next);
        }
        rejectButton.setVisible(status == OrderStatus.PLACED);
        failedButton.setVisible(status == OrderStatus.DISPATCHED);
    }

    // ------------------------------------------------------------------ actions

    @FunctionalInterface
    private interface OrderAction {
        void run(Order order) throws Exception;
    }

    /** Runs an action in the background, then refreshes and keeps the order selected. */
    private void perform(Order order, OrderAction action, String doneMessage) {
        Async.run(this, () -> {
            action.run(order);
            return order.getOrderId();
        }, id -> {
            pendingSelection = id;
            reload();
            onOrdersChanged.run();
            if (doneMessage != null) {
                Dialogs.info(this, doneMessage);
            }
        });
    }

    private void advanceSelected() {
        selected().ifPresent(o -> {
            int id = o.getOrderId();
            switch (o.getStatus()) {
                case PLACED -> perform(o, x -> orderService.acceptOrder(farmer, id), null);
                case CONFIRMED -> perform(o, x -> orderService.markPacked(farmer, id), null);
                case PACKED -> askDispatchDetails(o);
                case DISPATCHED -> {
                    if (Dialogs.confirm(this, "Mark order #" + id + " as delivered?"
                            + (o.getPaymentStatus() == model.PaymentStatus.PENDING
                                    ? "\nThe cash-on-delivery payment will be marked as received." : ""))) {
                        perform(o, x -> orderService.markDelivered(farmer, id), null);
                    }
                }
                default -> { }
            }
        });
    }

    private void askDispatchDetails(Order o) {
        JTextField partner = new JTextField("Own vehicle", 22);
        JTextField tracking = new JTextField(22);
        tracking.putClientProperty("JTextField.placeholderText", "optional");
        JPanel form = new JPanel(new GridLayout(4, 1, 0, 4));
        form.add(new JLabel("Delivery partner *"));
        form.add(partner);
        form.add(new JLabel("Tracking / vehicle number"));
        form.add(tracking);
        int choice = JOptionPane.showConfirmDialog(this, form, "Dispatch order #" + o.getOrderId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            perform(o, x -> orderService.dispatchOrder(farmer, o.getOrderId(), partner.getText(), tracking.getText()), null);
        }
    }

    private void rejectSelected() {
        selected().ifPresent(o -> askReason("Reject order #" + o.getOrderId() + " from " + o.getBuyerName() + "?\n"
                + "The stock goes back to your listings and the buyer's payment is refunded.\n\nReason:")
                .ifPresent(reason -> perform(o, x -> orderService.rejectOrder(farmer, o.getOrderId(), reason),
                        "Order #" + o.getOrderId() + " was rejected.")));
    }

    private void deliveryFailedSelected() {
        selected().ifPresent(o -> askReason("Mark order #" + o.getOrderId() + " as failed delivery?\n"
                + "The payment is refunded. Stock is not added back automatically -\n"
                + "use \"Update stock\" if the goods came back in sellable condition.\n\nReason:")
                .ifPresent(reason -> perform(o, x -> orderService.markDeliveryFailed(farmer, o.getOrderId(), reason),
                        "Order #" + o.getOrderId() + " was marked as failed delivery.")));
    }

    /** Empty Optional when the user cancels. */
    private Optional<String> askReason(String message) {
        while (true) {
            String reason = JOptionPane.showInputDialog(this, message, "Please confirm", JOptionPane.WARNING_MESSAGE);
            if (reason == null) {
                return Optional.empty();
            }
            if (!reason.isBlank()) {
                return Optional.of(reason.trim());
            }
            Dialogs.showError(this, new ValidationException("Please give a short reason."));
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
