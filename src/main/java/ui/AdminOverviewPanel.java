package ui;

import model.Admin;
import model.AuditEntry;
import model.DashboardStats;
import model.OrderStatus;
import service.AdminService;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Admin home: key numbers, orders by status and the latest activity. */
public class AdminOverviewPanel extends JPanel {

    private record Overview(DashboardStats stats, List<AuditEntry> recent) {
    }

    private record StatusCount(OrderStatus status, int count) {
    }

    private final Admin admin;
    private final AdminService service;

    private final StatCard farmers = new StatCard("Farmers", Theme.PRIMARY_DARK);
    private final StatCard buyers = new StatCard("Buyers", Theme.PRIMARY_DARK);
    private final StatCard pending = new StatCard("Farms awaiting verification", new Color(0xE65100));
    private final StatCard live = new StatCard("Products on sale", Theme.PRIMARY);
    private final StatCard lowStock = new StatCard("Low-stock listings", new Color(0xE65100));
    private final StatCard openOrders = new StatCard("Open orders", new Color(0x1565C0));
    private final StatCard delivered = new StatCard("Delivered order value", Theme.PRIMARY);
    private final StatCard paid = new StatCard("Payments received (simulated)", Theme.PRIMARY);

    private final BeanTableModel<StatusCount> statusModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Status", StatusCount::status),
            new BeanTableModel.Column<>("Orders", StatusCount::count));
    private final BeanTableModel<AuditEntry> activityModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("When", e -> Formats.dateTime(e.actionTime())),
            new BeanTableModel.Column<>("Who", e -> e.userName() == null ? "System" : e.userName()),
            new BeanTableModel.Column<>("What", e -> Pages.capitalise(e.action())),
            new BeanTableModel.Column<>("Details", AuditEntry::details));

    public AdminOverviewPanel(Admin admin, AdminService service, Consumer<String> openPage) {
        this.admin = admin;
        this.service = service;
        Pages.setUp(this);

        pending.onClick(() -> openPage.accept("users"), "Open Users to verify farms");
        openOrders.onClick(() -> openPage.accept("orders"), "Open Orders & Payments");

        JPanel cards = new JPanel(new GridLayout(2, 4, 14, 14));
        cards.setOpaque(false);
        for (StatCard c : new StatCard[]{farmers, buyers, pending, live, lowStock, openOrders, delivered, paid}) {
            cards.add(c);
        }

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(Pages.header("Overview", "Marketplace at a glance.", null), BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JTable statusTable = new JTable(statusModel);
        Tables.style(statusTable);
        Tables.statusColumn(statusTable, 0);
        Tables.alignRight(statusTable, 1);
        JTable activityTable = new JTable(activityModel);
        Tables.style(activityTable);
        Pages.widths(activityTable, 150, 150, 140, 360);

        JPanel lower = new JPanel(new BorderLayout(16, 0));
        lower.setOpaque(false);
        lower.add(titled("Orders by status", Tables.scroll(statusTable), 260), BorderLayout.WEST);
        lower.add(titled("Recent activity", Tables.scroll(activityTable), 0), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(lower, BorderLayout.CENTER);
        add(Pages.actionBar(new javax.swing.JComponent[]{refresh}), BorderLayout.SOUTH);
    }

    private static JPanel titled(String title, java.awt.Component body, int width) {
        JLabel label = new JLabel(title);
        label.setFont(Theme.HEADING);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        if (width > 0) {
            panel.setPreferredSize(new java.awt.Dimension(width, 0));
        }
        return panel;
    }

    public void reload() {
        Async.run(this, () -> new Overview(service.getStats(admin), service.getActivity(admin, null, null)), o -> {
            DashboardStats s = o.stats();
            farmers.setValue(String.valueOf(s.farmers()));
            buyers.setValue(String.valueOf(s.buyers()));
            pending.setValue(String.valueOf(s.pendingVerifications()));
            live.setValue(String.valueOf(s.liveListings()));
            lowStock.setValue(String.valueOf(s.lowStockListings()));
            openOrders.setValue(String.valueOf(s.openOrders()));
            delivered.setValue(Formats.money(s.deliveredValue()));
            paid.setValue(Formats.money(s.paidValue()));

            List<StatusCount> rows = new ArrayList<>();
            for (Map.Entry<OrderStatus, Integer> e : s.ordersByStatus().entrySet()) {
                rows.add(new StatusCount(e.getKey(), e.getValue()));
            }
            statusModel.setRows(rows);
            activityModel.setRows(o.recent().subList(0, Math.min(12, o.recent().size())));
        });
    }
}
