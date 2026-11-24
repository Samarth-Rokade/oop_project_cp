package ui;

import model.Admin;
import model.Order;
import model.OrderItem;
import model.OrderStatus;
import model.Payment;
import model.PaymentStatus;
import service.AdminService;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/** Admin: read-only view of every order and every payment record. */
public class AdminOrdersPanel extends JPanel {

    private static final String ALL = "All statuses";

    private record Data(List<Order> orders, List<Payment> payments) {
    }

    private final Admin admin;
    private final AdminService service;
    private List<Order> allOrders = new ArrayList<>();
    private int itemsSequence;

    private final JComboBox<Object> statusBox = new JComboBox<>();
    private final JLabel orderCount = new JLabel(" ");
    private final BeanTableModel<Order> orderModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Order", o -> "#" + o.getOrderId()),
            new BeanTableModel.Column<>("Placed on", o -> Formats.dateTime(o.getOrderDate())),
            new BeanTableModel.Column<>("Buyer", Order::getBuyerName),
            new BeanTableModel.Column<>("Farm", Order::getFarmNames),
            new BeanTableModel.Column<>("Items", Order::getItemCount),
            new BeanTableModel.Column<>("Total", o -> Formats.money(o.getTotalAmount())),
            new BeanTableModel.Column<>("Status", Order::getStatus),
            new BeanTableModel.Column<>("Payment", o -> o.getPaymentStatus() == null ? "-" : o.getPaymentStatus()),
            new BeanTableModel.Column<>("Delivery", o -> o.getDeliveryStatus() == null ? "-" : Pages.capitalise(o.getDeliveryStatus())));
    private final JTable orderTable = new JTable(orderModel);
    private final BeanTableModel<OrderItem> itemModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Product", OrderItem::getProductName),
            new BeanTableModel.Column<>("Quantity", i -> Formats.quantity(i.getQuantity()) + " " + i.getUnitCode()),
            new BeanTableModel.Column<>("Unit price", i -> Formats.money(i.getUnitPriceAtOrder())),
            new BeanTableModel.Column<>("Line total", i -> Formats.money(i.getLineTotal())));
    private final JTable itemTable = new JTable(itemModel);
    private final JLabel orderDetail = new JLabel(" ");

    private final JLabel paymentSummary = new JLabel(" ");
    private final BeanTableModel<Payment> paymentModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Payment", p -> "#" + p.paymentId()),
            new BeanTableModel.Column<>("Order", p -> "#" + p.orderId()),
            new BeanTableModel.Column<>("Buyer", Payment::buyerName),
            new BeanTableModel.Column<>("Method", Payment::method),
            new BeanTableModel.Column<>("Amount", p -> Formats.money(p.amount())),
            new BeanTableModel.Column<>("Status", Payment::status),
            new BeanTableModel.Column<>("Paid on", p -> p.paymentDate() == null ? "-" : Formats.dateTime(p.paymentDate())),
            new BeanTableModel.Column<>("Reference", p -> p.transactionReference() == null ? "-" : p.transactionReference()));
    private final JTable paymentTable = new JTable(paymentModel);

    public AdminOrdersPanel(Admin admin, AdminService service) {
        this.admin = admin;
        this.service = service;
        Pages.setUp(this);

        statusBox.addItem(ALL);
        for (OrderStatus s : OrderStatus.values()) {
            statusBox.addItem(s);
        }
        statusBox.addActionListener(e -> applyFilter());
        orderCount.setForeground(Theme.TEXT_MUTED);

        Tables.style(orderTable);
        Tables.alignRight(orderTable, 4, 5);
        Tables.statusColumn(orderTable, 6);
        Tables.statusColumn(orderTable, 7);
        Pages.widths(orderTable, 60, 150, 170, 160, 50, 100, 120, 90, 90);
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showItems();
            }
        });
        Tables.style(itemTable);
        Tables.alignRight(itemTable, 1, 2, 3);
        orderDetail.setBorder(BorderFactory.createEmptyBorder(10, 2, 8, 2));

        JPanel itemsPanel = new JPanel(new BorderLayout());
        itemsPanel.setOpaque(false);
        itemsPanel.add(orderDetail, BorderLayout.NORTH);
        itemsPanel.add(Tables.scroll(itemTable), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Tables.scroll(orderTable), itemsPanel);
        split.setResizeWeight(0.6);
        split.setBorder(null);

        JPanel ordersTab = new JPanel(new BorderLayout(0, 10));
        ordersTab.setOpaque(false);
        ordersTab.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        ordersTab.add(Pages.row(new JLabel("Show"), statusBox, orderCount), BorderLayout.NORTH);
        ordersTab.add(split, BorderLayout.CENTER);

        Tables.style(paymentTable);
        Tables.alignRight(paymentTable, 4);
        Tables.statusColumn(paymentTable, 5);
        Pages.widths(paymentTable, 70, 60, 170, 160, 100, 90, 150, 140);
        JPanel paymentsTab = new JPanel(new BorderLayout(0, 10));
        paymentsTab.setOpaque(false);
        paymentsTab.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        paymentsTab.add(paymentSummary, BorderLayout.NORTH);
        paymentsTab.add(Tables.scroll(paymentTable), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Orders", ordersTab);
        tabs.addTab("Payments", paymentsTab);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());

        add(Pages.header("Orders & Payments", "Read-only view of all marketplace orders and simulated payments.", null),
                BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(Pages.actionBar(new JComponent[]{refresh}), BorderLayout.SOUTH);
    }

    public void reload() {
        Async.run(this, () -> new Data(service.getOrders(admin), service.getPayments(admin)), data -> {
            allOrders = data.orders();
            applyFilter();
            paymentModel.setRows(data.payments());
            summarisePayments(data.payments());
        });
    }

    private void applyFilter() {
        Object status = statusBox.getSelectedItem();
        List<Order> rows = allOrders.stream()
                .filter(o -> !(status instanceof OrderStatus) || o.getStatus() == status)
                .collect(Collectors.toList());
        orderModel.setRows(rows);
        orderCount.setText(rows.size() + " of " + allOrders.size() + " orders");
        if (!rows.isEmpty()) {
            orderTable.setRowSelectionInterval(0, 0);
        } else {
            showItems();
        }
    }

    /** Totals per payment status, e.g. "Paid ₹26,380.00 (4)". */
    private void summarisePayments(List<Payment> payments) {
        Map<PaymentStatus, BigDecimal> totals = new EnumMap<>(PaymentStatus.class);
        Map<PaymentStatus, Long> counts = payments.stream()
                .collect(Collectors.groupingBy(Payment::status, () -> new EnumMap<>(PaymentStatus.class), Collectors.counting()));
        payments.forEach(p -> totals.merge(p.status(), p.amount(), BigDecimal::add));
        StringJoiner text = new StringJoiner("   •   ");
        totals.forEach((s, amount) -> text.add(s + " " + Formats.money(amount) + " (" + counts.get(s) + ")"));
        paymentSummary.setText(payments.isEmpty() ? "No payments yet." : text.toString());
    }

    private void showItems() {
        itemModel.setRows(List.of());
        Pages.selected(orderTable, orderModel).ifPresentOrElse(o -> {
            orderDetail.setText("<html><b>Order #" + o.getOrderId() + "</b> • " + o.getBuyerName()
                    + " • deliver to " + o.getDeliverTo()
                    + (o.getPaymentMethod() == null ? "" : " • " + o.getPaymentMethod()) + "</html>");
            int mine = ++itemsSequence;
            Async.run(this, () -> service.getOrderItems(admin, o.getOrderId()), items -> {
                if (mine == itemsSequence) {
                    itemModel.setRows(items);
                }
            });
        }, () -> orderDetail.setText(" "));
    }
}
