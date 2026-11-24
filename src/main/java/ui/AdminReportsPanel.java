package ui;

import model.Admin;
import model.Farmer;
import model.ListingStatus;
import model.ReportPeriod;
import model.Reports.CategorySales;
import model.Reports.FarmerSales;
import model.Reports.ProductSales;
import service.ReportService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.math.BigDecimal;

/** Admin: farmer-wise, product-wise and category-wise sales reports (spec 4.1). */
public class AdminReportsPanel extends JPanel {

    private final Admin admin;
    private final ReportService reports;
    private final JComboBox<ReportPeriod> periodBox = new JComboBox<>(ReportPeriod.values());
    private final JTabbedPane tabs = new JTabbedPane();

    private final BeanTableModel<FarmerSales> farmerModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Farm", FarmerSales::farmName),
            new BeanTableModel.Column<>("Farmer", FarmerSales::farmerName),
            new BeanTableModel.Column<>("Verification", FarmerSales::verification, Farmer.VerificationStatus.class),
            new BeanTableModel.Column<>("Products", FarmerSales::products, Integer.class),
            new BeanTableModel.Column<>("Orders", FarmerSales::orders, Integer.class),
            new BeanTableModel.Column<>("Sales value", FarmerSales::salesValue, BigDecimal.class),
            new BeanTableModel.Column<>("Delivered value", FarmerSales::deliveredValue, BigDecimal.class));
    private final BeanTableModel<ProductSales> productModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Product", ProductSales::productName),
            new BeanTableModel.Column<>("Farm", ProductSales::farmName),
            new BeanTableModel.Column<>("Category", ProductSales::categoryName),
            new BeanTableModel.Column<>("Orders", ProductSales::orders, Integer.class),
            new BeanTableModel.Column<>("Units sold", ProductSales::unitsSold, BigDecimal.class),
            new BeanTableModel.Column<>("Unit", ProductSales::unitCode),
            new BeanTableModel.Column<>("Revenue", ProductSales::revenue, BigDecimal.class),
            new BeanTableModel.Column<>("In stock", ProductSales::stock, BigDecimal.class),
            new BeanTableModel.Column<>("Status", ProductSales::status, ListingStatus.class));
    private final BeanTableModel<CategorySales> categoryModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Category", CategorySales::categoryName),
            new BeanTableModel.Column<>("Products", CategorySales::products, Integer.class),
            new BeanTableModel.Column<>("Orders", CategorySales::orders, Integer.class),
            new BeanTableModel.Column<>("Revenue", CategorySales::revenue, BigDecimal.class));

    public AdminReportsPanel(Admin admin, ReportService reports) {
        this.admin = admin;
        this.reports = reports;
        Pages.setUp(this);

        periodBox.setSelectedItem(ReportPeriod.ALL_TIME);
        periodBox.addActionListener(e -> reload());

        JTable farmerTable = new JTable(farmerModel);
        Tables.style(farmerTable);
        Tables.statusColumn(farmerTable, 2);
        Tables.numberColumn(farmerTable, 3, 4);
        Tables.moneyColumn(farmerTable, 5, 6);
        farmerTable.setAutoCreateRowSorter(true);
        Pages.widths(farmerTable, 190, 160, 110, 80, 80, 130, 130);

        JTable productTable = new JTable(productModel);
        Tables.style(productTable);
        Tables.numberColumn(productTable, 3, 4, 7);
        Tables.moneyColumn(productTable, 6);
        Tables.statusColumn(productTable, 8);
        productTable.setAutoCreateRowSorter(true);
        Pages.widths(productTable, 150, 170, 120, 70, 90, 70, 110, 80, 120);

        JTable categoryTable = new JTable(categoryModel);
        Tables.style(categoryTable);
        Tables.numberColumn(categoryTable, 1, 2);
        Tables.moneyColumn(categoryTable, 3);
        categoryTable.setAutoCreateRowSorter(true);

        tabs.addTab("Farmer-wise", Tables.scroll(farmerTable));
        tabs.addTab("Product-wise", Tables.scroll(productTable));
        tabs.addTab("Category-wise", Tables.scroll(categoryTable));

        JLabel note = new JLabel("Sales value counts orders that were not rejected, cancelled or failed. "
                + "Click a column header to sort.");
        note.setForeground(Theme.TEXT_MUTED);
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(tabs, BorderLayout.CENTER);
        center.add(note, BorderLayout.SOUTH);

        JButton export = new JButton("Export CSV...");
        export.addActionListener(e -> {
            String period = ((ReportPeriod) periodBox.getSelectedItem()).name().toLowerCase();
            switch (tabs.getSelectedIndex()) {
                case 0 -> Exports.exportTable(this, farmerModel, "farmer_report_" + period);
                case 1 -> Exports.exportTable(this, productModel, "product_report_" + period);
                default -> Exports.exportTable(this, categoryModel, "category_report_" + period);
            }
        });

        add(Pages.header("Reports", "Sales across the marketplace.", Pages.row(new JLabel("Period"), periodBox)),
                BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(Pages.actionBar(new JComponent[0], export), BorderLayout.SOUTH);
    }

    public void reload() {
        ReportPeriod period = (ReportPeriod) periodBox.getSelectedItem();
        Async.run(this, () -> reports.marketReport(admin, period), r -> {
            farmerModel.setRows(r.farmers());
            productModel.setRows(r.products());
            categoryModel.setRows(r.categories());
        });
    }
}
