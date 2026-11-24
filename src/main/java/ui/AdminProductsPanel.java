package ui;

import model.Admin;
import model.ListingStatus;
import model.Product;
import service.AdminService;
import util.Formats;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Admin: every listing in the marketplace, with remove/restore moderation. */
public class AdminProductsPanel extends JPanel {

    private static final String ALL = "All statuses";

    private final Admin admin;
    private final AdminService service;
    private List<Product> all = new ArrayList<>();

    private final JTextField search = new JTextField(18);
    private final JComboBox<Object> statusBox = new JComboBox<>();
    private final JLabel count = new JLabel(" ");
    private final BeanTableModel<Product> model = new BeanTableModel<>(
            new BeanTableModel.Column<>("Product", Product::getProductName),
            new BeanTableModel.Column<>("Farm", Product::getFarmName),
            new BeanTableModel.Column<>("Category", Product::getCategoryName),
            new BeanTableModel.Column<>("Stock", p -> Formats.quantity(p.getAvailableQuantity()) + " " + p.getUnitCode()),
            new BeanTableModel.Column<>("Price", p -> Formats.money(p.getPricePerUnit()) + " / " + p.getUnitCode()),
            new BeanTableModel.Column<>("Status", Product::getListingStatus),
            new BeanTableModel.Column<>("Last updated", p -> Formats.dateTime(p.getUpdatedAt())));
    private final JTable table = new JTable(model);
    private final JLabel description = new JLabel(" ");
    private final JButton remove = new JButton("Remove listing...");
    private final JButton restore = new JButton("Restore");

    public AdminProductsPanel(Admin admin, AdminService service) {
        this.admin = admin;
        this.service = service;
        Pages.setUp(this);

        search.putClientProperty("JTextField.placeholderText", "Search product or farm");
        search.putClientProperty("JTextField.showClearButton", true);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        statusBox.addItem(ALL);
        for (ListingStatus s : ListingStatus.values()) {
            statusBox.addItem(s);
        }
        statusBox.addActionListener(e -> applyFilter());
        count.setForeground(Theme.TEXT_MUTED);

        Tables.style(table);
        Tables.alignRight(table, 3, 4);
        Tables.statusColumn(table, 5);
        Pages.widths(table, 150, 170, 120, 120, 140, 140, 160);
        table.getSelectionModel().addListSelectionListener(e -> updateSelection());
        description.setForeground(Theme.TEXT_MUTED);

        remove.setForeground(Theme.ERROR);
        remove.addActionListener(e -> selected().ifPresent(this::removeListing));
        restore.addActionListener(e -> selected().ifPresent(p -> act(() -> service.restoreListing(admin, p.getProductId()),
                "\"" + p.getProductName() + "\" was restored as hidden. The farmer can show it to buyers again.")));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(Tables.scroll(table), BorderLayout.CENTER);
        center.add(description, BorderLayout.SOUTH);

        add(Pages.header("Products", "Every listing in the marketplace. Remove listings that break the rules.",
                Pages.row(search, statusBox, count)), BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(Pages.actionBar(new JComponent[]{refresh}, restore, remove), BorderLayout.SOUTH);
        updateSelection();
    }

    public void reload() {
        Integer keep = selected().map(Product::getProductId).orElse(null);
        Async.run(this, () -> service.getProducts(admin), products -> {
            all = products;
            applyFilter();
            if (keep != null) {
                model.indexOf(p -> p.getProductId() == keep).ifPresent(i -> {
                    int view = table.convertRowIndexToView(i);
                    table.setRowSelectionInterval(view, view);
                });
            }
        });
    }

    private void applyFilter() {
        String text = search.getText().trim().toLowerCase();
        Object status = statusBox.getSelectedItem();
        Predicate<Product> matches = p -> (text.isEmpty()
                || p.getProductName().toLowerCase().contains(text)
                || p.getFarmName().toLowerCase().contains(text))
                && (!(status instanceof ListingStatus) || p.getListingStatus() == status);
        List<Product> rows = all.stream().filter(matches).collect(Collectors.toList());
        model.setRows(rows);
        count.setText(rows.size() + " of " + all.size());
        updateSelection();
    }

    private Optional<Product> selected() {
        return Pages.selected(table, model);
    }

    private void updateSelection() {
        Optional<Product> p = selected();
        remove.setEnabled(p.map(x -> x.getListingStatus() != ListingStatus.REMOVED).orElse(false));
        restore.setEnabled(p.map(x -> x.getListingStatus() == ListingStatus.REMOVED).orElse(false));
        description.setText(p.map(x -> "<html>" + x.getProductName() + " by " + x.getFarmerName() + ": "
                + (x.getDescription() == null ? "<i>no description</i>" : x.getDescription()) + "</html>").orElse(" "));
    }

    private void removeListing(Product p) {
        String reason = JOptionPane.showInputDialog(this, "Remove \"" + p.getProductName() + "\" by " + p.getFarmName()
                + " from the marketplace?\nThe farmer will not be able to show it again.\n\nReason:",
                "Remove listing", JOptionPane.WARNING_MESSAGE);
        if (reason == null) {
            return;
        }
        act(() -> service.removeListing(admin, p.getProductId(), reason), null);
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private void act(Action action, String doneMessage) {
        Async.run(this, () -> {
            action.run();
            return true;
        }, ok -> {
            reload();
            if (doneMessage != null) {
                Dialogs.info(this, doneMessage);
            }
        });
    }
}
