package ui;

import model.Buyer;
import model.Order;
import model.OrderItem;
import model.OrderStatus;
import service.OrderService;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The buyer's orders (top) and the items of the selected order (bottom). */
public class BuyerOrdersPanel extends JPanel {

    private final Buyer buyer;
    private final OrderService orderService;

    private final DefaultTableModel ordersModel = readOnlyModel(
            "Order", "Placed on", "Farm", "Items", "Total", "Status", "Payment", "Delivery");
    private final JTable ordersTable = new JTable(ordersModel);
    private final List<Order> orders = new ArrayList<>();

    private final DefaultTableModel itemsModel = readOnlyModel("Product", "Farm", "Quantity", "Unit price", "Line total");
    private final JTable itemsTable = new JTable(itemsModel);
    private final JLabel detailHeader = new JLabel("Select an order to see its items.");

    private final JButton cancelButton = new JButton("Cancel order");
    private final JButton receiptButton = new JButton("Save receipt...");
    private Integer pendingSelection;
    private int detailSequence;

    public BuyerOrdersPanel(Buyer buyer, OrderService orderService) {
        super(new BorderLayout());
        this.buyer = buyer;
        this.orderService = orderService;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel title = new JLabel("My Orders");
        title.setFont(Theme.TITLE);
        JLabel subtitle = new JLabel("Orders can be cancelled until they are packed. Stock goes back to the farmer and payments are refunded.");
        subtitle.setForeground(Theme.TEXT_MUTED);
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(subtitle, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        Tables.style(ordersTable);
        Tables.alignRight(ordersTable, 3, 4);
        Tables.statusColumn(ordersTable, 5);
        Tables.statusColumn(ordersTable, 6);
        int[] widths = {70, 150, 170, 60, 100, 120, 90, 100};
        for (int i = 0; i < widths.length; i++) {
            ordersTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedOrder();
            }
        });

        Tables.style(itemsTable);
        Tables.alignRight(itemsTable, 2, 3, 4);

        detailHeader.setBorder(BorderFactory.createEmptyBorder(10, 2, 8, 2));
        JPanel detail = new JPanel(new BorderLayout());
        detail.setOpaque(false);
        detail.add(detailHeader, BorderLayout.NORTH);
        detail.add(Tables.scroll(itemsTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Tables.scroll(ordersTable), detail);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setOpaque(false);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());
        cancelButton.addActionListener(e -> cancelSelected());
        receiptButton.addActionListener(e -> saveReceipt());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        buttons.add(refresh);
        buttons.add(receiptButton);
        buttons.add(cancelButton);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        updateButtons();
    }

    /** Select this order after the next reload (used right after checkout). */
    public void selectAfterReload(int orderId) {
        pendingSelection = orderId;
    }

    public void reload() {
        Integer select = pendingSelection != null ? pendingSelection
                : selected().map(Order::getOrderId).orElse(null);
        pendingSelection = null;
        Async.run(this, () -> orderService.getOrdersOf(buyer), list -> {
            orders.clear();
            orders.addAll(list);
            ordersModel.setRowCount(0);
            for (Order o : list) {
                ordersModel.addRow(new Object[]{
                        "#" + o.getOrderId(),
                        Formats.dateTime(o.getOrderDate()),
                        o.getFarmNames(),
                        o.getItemCount(),
                        Formats.money(o.getTotalAmount()),
                        o.getStatus(),
                        o.getPaymentStatus() == null ? "-" : o.getPaymentStatus(),
                        o.getDeliveryStatus() == null ? "-" : capitalise(o.getDeliveryStatus())
                });
            }
            int row = 0;
            if (select != null) {
                for (int i = 0; i < orders.size(); i++) {
                    if (orders.get(i).getOrderId() == select) {
                        row = i;
                    }
                }
            }
            if (!orders.isEmpty()) {
                ordersTable.setRowSelectionInterval(row, row);
                ordersTable.scrollRectToVisible(ordersTable.getCellRect(row, 0, true));
            } else {
                showSelectedOrder();
            }
        });
    }

    private void showSelectedOrder() {
        updateButtons();
        itemsModel.setRowCount(0);
        Optional<Order> order = selected();
        if (order.isEmpty()) {
            detailHeader.setText(orders.isEmpty() ? "You have no orders yet." : "Select an order to see its items.");
            return;
        }
        Order o = order.get();
        detailHeader.setText("<html><b>Order #" + o.getOrderId() + "</b> &nbsp;•&nbsp; "
                + "Deliver to " + o.getDeliverTo() + " &nbsp;•&nbsp; "
                + (o.getPaymentMethod() == null ? "" : o.getPaymentMethod() + ", " + o.getPaymentStatus())
                + (o.getDeliveryPartner() == null ? "" : " &nbsp;•&nbsp; Shipped via " + o.getDeliveryPartner()
                        + (o.getTrackingReference() == null ? "" : " (" + o.getTrackingReference() + ")"))
                + "</html>");
        int mySequence = ++detailSequence;
        Async.run(this, () -> orderService.getItems(buyer, o), items -> {
            if (mySequence != detailSequence) {
                return;
            }
            for (OrderItem i : items) {
                itemsModel.addRow(new Object[]{
                        i.getProductName(), i.getFarmName(),
                        Formats.quantity(i.getQuantity()) + " " + i.getUnitCode(),
                        Formats.money(i.getUnitPriceAtOrder()),
                        Formats.money(i.getLineTotal())});
            }
        });
    }

    private void cancelSelected() {
        selected().ifPresent(o -> {
            if (!Dialogs.confirm(this, "Cancel order #" + o.getOrderId() + " (" + Formats.money(o.getTotalAmount()) + ")?\n"
                    + "The stock goes back to the farmer"
                    + (o.getPaymentStatus() == model.PaymentStatus.PAID ? " and your payment will be refunded." : "."))) {
                return;
            }
            Async.run(this, () -> {
                orderService.cancelOrder(buyer, o.getOrderId());
                return o.getOrderId();
            }, id -> {
                selectAfterReload(id);
                reload();
                Dialogs.info(this, "Order #" + id + " was cancelled.");
            });
        });
    }

    private Optional<Order> selected() {
        int r = ordersTable.getSelectedRow();
        return r < 0 || r >= orders.size() ? Optional.empty() : Optional.of(orders.get(r));
    }

    /** Writes a text receipt for the selected order (file I/O runs in the background). */
    private void saveReceipt() {
        selected().ifPresent(o -> Exports.chooseSaveFile(this, "receipt_order_" + o.getOrderId(), "txt", "Receipt")
                .ifPresent(path -> Async.run(this, () -> {
                    String text = util.ReceiptFormatter.format(o, orderService.getItems(buyer, o),
                            java.time.LocalDateTime.now());
                    util.ReceiptFormatter.save(path, text);
                    return path;
                }, saved -> Exports.savedMessage(this, saved, "Receipt for order #" + o.getOrderId() + " saved"))));
    }

    private void updateButtons() {
        receiptButton.setEnabled(selected().isPresent());
        cancelButton.setEnabled(selected().map(o -> o.getStatus().canTransitionTo(OrderStatus.CANCELLED)).orElse(false));
    }

    private static String capitalise(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase().replace('_', ' ');
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
