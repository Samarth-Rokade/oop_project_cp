package ui;

import model.Farmer;
import model.ListingStatus;
import model.OrderStatus;
import model.ReportPeriod;
import model.Reports.ProductSales;
import model.Reports.SaleLine;
import service.ReportService;
import util.Formats;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Farmer's sales: headline figures, product-wise performance and the
 * full sales history for a chosen period, each exportable to CSV.
 */
public class FarmerSalesPanel extends JPanel {

    private final Farmer farmer;
    private final ReportService reports;

    private final JComboBox<ReportPeriod> periodBox = new JComboBox<>(ReportPeriod.values());
    private final StatCard salesCard = new StatCard("Sales value", Theme.PRIMARY);
    private final StatCard deliveredCard = new StatCard("Delivered (completed)", Theme.PRIMARY_DARK);
    private final StatCard ordersCard = new StatCard("Orders", new Color(0x1565C0));
    private final StatCard averageCard = new StatCard("Average order", new Color(0x6A1B9A));
    private final JTabbedPane tabs = new JTabbedPane();

    private final BeanTableModel<ProductSales> productModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Product", ProductSales::productName),
            new BeanTableModel.Column<>("Category", ProductSales::categoryName),
            new BeanTableModel.Column<>("Orders", ProductSales::orders, Integer.class),
            new BeanTableModel.Column<>("Units sold", ProductSales::unitsSold, BigDecimal.class),
            new BeanTableModel.Column<>("Unit", ProductSales::unitCode),
            new BeanTableModel.Column<>("Revenue", ProductSales::revenue, BigDecimal.class),
            new BeanTableModel.Column<>("In stock", ProductSales::stock, BigDecimal.class),
            new BeanTableModel.Column<>("Status", ProductSales::status, ListingStatus.class));
    private final BeanTableModel<SaleLine> historyModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Order", SaleLine::orderId, Integer.class),
            new BeanTableModel.Column<>("Date", SaleLine::orderDate, LocalDateTime.class),
            new BeanTableModel.Column<>("Buyer", SaleLine::buyerName),
            new BeanTableModel.Column<>("Product", SaleLine::productName),
            new BeanTableModel.Column<>("Quantity", SaleLine::quantity, BigDecimal.class),
            new BeanTableModel.Column<>("Unit", SaleLine::unitCode),
            new BeanTableModel.Column<>("Unit price", SaleLine::unitPrice, BigDecimal.class),
            new BeanTableModel.Column<>("Line total", SaleLine::lineTotal, BigDecimal.class),
            new BeanTableModel.Column<>("Order status", SaleLine::status, OrderStatus.class));

    public FarmerSalesPanel(Farmer farmer, ReportService reports) {
        this.farmer = farmer;
        this.reports = reports;
        Pages.setUp(this);

        periodBox.setSelectedItem(ReportPeriod.LAST_30_DAYS);
        periodBox.addActionListener(e -> reload());

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        for (StatCard c : new StatCard[]{salesCard, deliveredCard, ordersCard, averageCard}) {
            cards.add(c);
        }

        JTable productTable = new JTable(productModel);
        Tables.style(productTable);
        Tables.numberColumn(productTable, 2, 3, 6);
        Tables.moneyColumn(productTable, 5);
        Tables.statusColumn(productTable, 7);
        productTable.setAutoCreateRowSorter(true);
        Pages.widths(productTable, 160, 130, 70, 90, 70, 120, 90, 130);

        JTable historyTable = new JTable(historyModel);
        Tables.style(historyTable);
        Tables.dateTimeColumn(historyTable, 1);
        Tables.numberColumn(historyTable, 0, 4);
        Tables.moneyColumn(historyTable, 6, 7);
        Tables.statusColumn(historyTable, 8);
        historyTable.setAutoCreateRowSorter(true);
        Pages.widths(historyTable, 60, 150, 170, 130, 80, 60, 100, 110, 120);

        tabs.addTab("By product", Tables.scroll(productTable));
        tabs.addTab("Sales history", Tables.scroll(historyTable));

        JLabel note = new JLabel("Sales value counts orders that were not rejected, cancelled or failed. "
                + "Click a column header to sort.");
        note.setForeground(Theme.TEXT_MUTED);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        center.add(note, BorderLayout.SOUTH);

        JButton export = new JButton("Export CSV...");
        export.addActionListener(e -> {
            String period = ((ReportPeriod) periodBox.getSelectedItem()).name().toLowerCase();
            if (tabs.getSelectedIndex() == 0) {
                Exports.exportTable(this, productModel, "sales_by_product_" + period);
            } else {
                Exports.exportTable(this, historyModel, "sales_history_" + period);
            }
        });

        add(Pages.header("Sales", "How your products are selling.", Pages.row(new JLabel("Period"), periodBox)),
                BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(Pages.actionBar(new JComponent[0], export), BorderLayout.SOUTH);
    }

    public void reload() {
        ReportPeriod period = (ReportPeriod) periodBox.getSelectedItem();
        Async.run(this, () -> reports.farmerReport(farmer, period), r -> {
            salesCard.setValue(Formats.money(r.summary().salesValue()));
            deliveredCard.setValue(Formats.money(r.summary().deliveredValue()));
            ordersCard.setValue(String.valueOf(r.summary().orders()));
            averageCard.setValue(Formats.money(r.summary().averageOrder()));
            productModel.setRows(r.products());
            historyModel.setRows(r.history());
            tabs.setTitleAt(1, "Sales history (" + r.history().size() + ")");
        });
    }
}
