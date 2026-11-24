package ui;

import exception.ValidationException;
import model.Category;
import model.Farmer;
import model.ListingStatus;
import model.Product;
import model.Unit;
import service.LookupService;
import service.ProductService;
import util.Formats;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * "My Products" screen for a farmer: summary cards, search/filter,
 * product table and actions (add, edit, stock, visibility, price history).
 *
 * Keyboard: Ctrl+N add, Enter edit, Ctrl+F search, F5 refresh.
 */
public class ProductPanel extends JPanel {

    private static final String ALL_STATUSES = "All statuses";
    private static final String ALL_CATEGORIES = "All categories";

    private final Farmer farmer;
    private final ProductService productService;
    private final LookupService lookupService;

    private List<Product> allProducts = new ArrayList<>();

    private final ProductTableModel tableModel = new ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JLabel countLabel = new JLabel(" ");

    private final StatCard totalCard = new StatCard("Total listings", Theme.PRIMARY_DARK);
    private final StatCard visibleCard = new StatCard("Visible to buyers", Theme.PRIMARY);
    private final StatCard lowCard = new StatCard("Low stock", new Color(0xE65100));
    private final StatCard outCard = new StatCard("Out of stock", new Color(0xC62828));

    private final JButton editButton = new JButton("Edit");
    private final JButton stockButton = new JButton("Update stock");
    private final JButton visibilityButton = new JButton("Hide from buyers");
    private final JButton historyButton = new JButton("Price history");
    private final JButton deleteButton = new JButton("Delete");

    public ProductPanel(Farmer farmer, ProductService productService, LookupService lookupService) {
        super(new BorderLayout());
        this.farmer = farmer;
        this.productService = productService;
        this.lookupService = lookupService;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        add(buildTop(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        installKeyboardShortcuts();
        updateActionButtons();

        reload(null);
    }

    // ------------------------------------------------------------------ layout

    private JPanel buildTop() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("My Products");
        title.setFont(Theme.TITLE);
        JLabel subtitle = new JLabel("Add listings, keep stock up to date and control what buyers see.");
        subtitle.setForeground(Theme.TEXT_MUTED);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(subtitle);

        JButton addButton = new JButton("+  Add product");
        addButton.setBackground(Theme.PRIMARY);
        addButton.setForeground(Color.WHITE);
        addButton.setFont(addButton.getFont().deriveFont(Font.BOLD));
        addButton.setToolTipText("Add a new product (Ctrl+N)");
        addButton.addActionListener(e -> openAddDialog());
        JButton importButton = new JButton("Import CSV...");
        importButton.setToolTipText("Add many products at once from a spreadsheet");
        importButton.addActionListener(e -> new ImportProductsDialog(SwingUtilities.getWindowAncestor(this), farmer,
                new service.ProductImportService(lookupService), () -> reload(null)).setVisible(true));
        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        addWrap.setOpaque(false);
        addWrap.add(importButton);
        addWrap.add(addButton);

        titleRow.add(titles, BorderLayout.WEST);
        titleRow.add(addWrap, BorderLayout.EAST);

        // Summary cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(totalCard);
        cards.add(visibleCard);
        cards.add(lowCard);
        cards.add(outCard);

        // Filters
        searchField.putClientProperty("JTextField.placeholderText", "Search products  (Ctrl+F)");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        statusFilter.addItem(ALL_STATUSES);
        for (ListingStatus s : ListingStatus.values()) {
            statusFilter.addItem(s);
        }
        statusFilter.addActionListener(e -> applyFilters());
        categoryFilter.addItem(ALL_CATEGORIES);
        categoryFilter.addActionListener(e -> applyFilters());

        JButton refresh = new JButton("Refresh");
        refresh.setToolTipText("Reload from the database (F5)");
        refresh.addActionListener(e -> reload(selectedProduct().map(Product::getProductId).orElse(null)));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(statusFilter);
        filters.add(categoryFilter);
        filters.add(refresh);

        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.setOpaque(false);
        filterRow.add(filters, BorderLayout.WEST);
        countLabel.setForeground(Theme.TEXT_MUTED);
        filterRow.add(countLabel, BorderLayout.EAST);

        for (JComponent c : new JComponent[]{titleRow, cards, filterRow}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        top.add(titleRow);
        top.add(Box.createVerticalStrut(16));
        top.add(cards);
        top.add(Box.createVerticalStrut(18));
        top.add(filterRow);
        top.add(Box.createVerticalStrut(10));
        return top;
    }

    private JScrollPane buildTable() {
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setReorderingAllowed(false);
        ProductCellRenderer renderer = new ProductCellRenderer();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        int[] widths = {170, 120, 130, 140, 110, 140, 170};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.setRowSorter(new TableRowSorter<>(tableModel));

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });
        // Mouse: double-click a row to edit it
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
                        && table.rowAtPoint(e.getPoint()) >= 0) {
                    openEditDialog();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE0E0E0)));
        return scroll;
    }

    private JPanel buildActions() {
        editButton.addActionListener(e -> openEditDialog());
        stockButton.addActionListener(e -> promptStockUpdate());
        visibilityButton.addActionListener(e -> toggleVisibility());
        historyButton.addActionListener(e -> showPriceHistory());
        deleteButton.setForeground(Theme.ERROR);
        deleteButton.setToolTipText("Only products that were never ordered can be deleted (Delete key)");
        deleteButton.addActionListener(e -> deleteSelected());

        JLabel hint = new JLabel("Tip: double-click a row to edit it.");
        hint.setForeground(Theme.TEXT_MUTED);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(deleteButton);
        buttons.add(historyButton);
        buttons.add(visibilityButton);
        buttons.add(stockButton);
        buttons.add(editButton);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        bar.add(hint, BorderLayout.WEST);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private void installKeyboardShortcuts() {
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "add", this::openAddDialog);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "search", searchField::requestFocusInWindow);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh",
                () -> reload(selectedProduct().map(Product::getProductId).orElse(null)));

        // Enter on the table edits the selected row instead of moving down
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editRow");
        table.getActionMap().put("editRow", action(this::openEditDialog));
        table.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteRow");
        table.getActionMap().put("deleteRow", action(this::deleteSelected));
    }

    private void bindKey(KeyStroke key, String name, Runnable runnable) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(key, name);
        getActionMap().put(name, action(runnable));
    }

    private static AbstractAction action(Runnable runnable) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        };
    }

    // ------------------------------------------------------------------ data

    /** Loads products from the database, then reselects the given product id. */
    private void reload(Integer selectId) {
        Async.run(this, () -> productService.getProductsOf(farmer), products -> {
            allProducts = products;
            refreshCategoryFilter();
            updateStats();
            applyFilters();
            if (selectId != null) {
                select(selectId);
            }
        });
    }

    /**
     * In-memory filtering with composed Predicates and the Stream API
     * (spec 9.14). No extra database round-trip while typing.
     */
    private void applyFilters() {
        String text = searchField.getText().trim().toLowerCase();
        Object status = statusFilter.getSelectedItem();
        Object category = categoryFilter.getSelectedItem();

        Predicate<Product> matchesText = p -> text.isEmpty()
                || p.getProductName().toLowerCase().contains(text)
                || (p.getDescription() != null && p.getDescription().toLowerCase().contains(text));
        Predicate<Product> matchesStatus = p -> !(status instanceof ListingStatus)
                || p.getListingStatus() == status;
        Predicate<Product> matchesCategory = p -> category == null || ALL_CATEGORIES.equals(category)
                || category.equals(p.getCategoryName());

        Integer keep = selectedProduct().map(Product::getProductId).orElse(null);
        List<Product> visible = allProducts.stream()
                .filter(matchesText.and(matchesStatus).and(matchesCategory))
                .collect(Collectors.toList());
        tableModel.setProducts(visible);
        if (keep != null) {
            select(keep);
        }
        countLabel.setText("Showing " + visible.size() + " of " + allProducts.size());
        updateActionButtons();
    }

    private void refreshCategoryFilter() {
        Object selected = categoryFilter.getSelectedItem();
        List<String> names = allProducts.stream()
                .map(Product::getCategoryName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        categoryFilter.removeAllItems();
        categoryFilter.addItem(ALL_CATEGORIES);
        names.forEach(categoryFilter::addItem);
        categoryFilter.setSelectedItem(names.contains(selected) ? selected : ALL_CATEGORIES);
    }

    private void updateStats() {
        totalCard.setValue(String.valueOf(allProducts.size()));
        visibleCard.setValue(String.valueOf(allProducts.stream().filter(Product::isVisibleToBuyers).count()));
        lowCard.setValue(String.valueOf(allProducts.stream()
                .filter(p -> p.getListingStatus() != ListingStatus.REMOVED)
                .filter(productService::isLowStock)
                .count()));
        outCard.setValue(String.valueOf(allProducts.stream()
                .filter(p -> p.getListingStatus() == ListingStatus.OUT_OF_STOCK)
                .count()));
    }

    private Optional<Product> selectedProduct() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return Optional.empty();
        }
        return Optional.of(tableModel.getProductAt(table.convertRowIndexToModel(viewRow)));
    }

    private void select(int productId) {
        tableModel.indexOf(productId).ifPresent(modelRow -> {
            int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow >= 0) {
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
            }
        });
    }

    private void updateActionButtons() {
        Optional<Product> selected = selectedProduct();
        boolean editable = selected.map(p -> p.getListingStatus() != ListingStatus.REMOVED).orElse(false);
        editButton.setEnabled(editable);
        stockButton.setEnabled(editable);
        visibilityButton.setEnabled(editable);
        historyButton.setEnabled(selected.isPresent());
        deleteButton.setEnabled(editable);
        visibilityButton.setText(selected.map(p -> p.getListingStatus() == ListingStatus.INACTIVE)
                .orElse(false) ? "Show to buyers" : "Hide from buyers");
    }

    // ------------------------------------------------------------------ actions

    private void openAddDialog() {
        withLookups((categories, units) -> new ProductFormDialog(
                SwingUtilities.getWindowAncestor(this), null, categories, units,
                product -> productService.addProduct(farmer, product),
                saved -> {
                    reload(saved.getProductId());
                    Dialogs.info(this, "\"" + saved.getProductName() + "\" was added."
                            + (farmer.isVerified() ? "" : "\nBuyers will see it once your account is verified."));
                }).setVisible(true));
    }

    private void openEditDialog() {
        selectedProduct()
                .filter(p -> p.getListingStatus() != ListingStatus.REMOVED)
                .ifPresent(product -> withLookups((categories, units) -> new ProductFormDialog(
                        SwingUtilities.getWindowAncestor(this), product, categories, units,
                        edited -> productService.updateProduct(farmer, edited),
                        saved -> reload(saved.getProductId())).setVisible(true)));
    }

    private void promptStockUpdate() {
        selectedProduct().ifPresent(product -> {
            Object input = JOptionPane.showInputDialog(this,
                    "New available quantity for " + product.getProductName()
                            + " (in " + product.getUnitCode() + "):",
                    "Update stock", JOptionPane.PLAIN_MESSAGE, null, null,
                    Formats.quantity(product.getAvailableQuantity()));
            if (input == null) {
                return;                                   // cancelled
            }
            BigDecimal quantity;
            try {
                quantity = new BigDecimal(input.toString().trim().replace(",", ""));
            } catch (NumberFormatException e) {
                Dialogs.showError(this, new ValidationException("Please enter a number, e.g. 25 or 12.5."));
                return;
            }
            Async.run(this, () -> productService.updateStock(farmer, product.getProductId(), quantity),
                    saved -> reload(saved.getProductId()));
        });
    }

    private void toggleVisibility() {
        selectedProduct().ifPresent(product -> {
            boolean makeVisible = product.getListingStatus() == ListingStatus.INACTIVE;
            if (!makeVisible && !Dialogs.confirm(this,
                    "Hide \"" + product.getProductName() + "\" from buyers?\nYou can show it again at any time.")) {
                return;
            }
            Async.run(this, () -> productService.setAvailability(farmer, product.getProductId(), makeVisible),
                    saved -> {
                        reload(saved.getProductId());
                        if (makeVisible && saved.getListingStatus() == ListingStatus.OUT_OF_STOCK) {
                            Dialogs.info(this, "The listing is visible again, but it has no stock.\n"
                                    + "Use \"Update stock\" so buyers can order it.");
                        }
                    });
        });
    }

    private void deleteSelected() {
        selectedProduct()
                .filter(p -> p.getListingStatus() != ListingStatus.REMOVED)
                .ifPresent(product -> {
                    if (!Dialogs.confirm(this, "Delete \"" + product.getProductName() + "\" permanently?\n"
                            + "This also removes its price history and cannot be undone.")) {
                        return;
                    }
                    Async.run(this, () -> {
                        productService.deleteProduct(farmer, product.getProductId());
                        return product.getProductName();
                    }, name -> {
                        reload(null);
                        Dialogs.info(this, "\"" + name + "\" was deleted.");
                    });
                });
    }

    private void showPriceHistory() {
        selectedProduct().ifPresent(product ->
                Async.run(this, () -> productService.getPriceHistory(farmer, product.getProductId()),
                        history -> new PriceHistoryDialog(SwingUtilities.getWindowAncestor(this),
                                product, history).setVisible(true)));
    }

    /** Loads categories and units (cached after the first time), then runs the action. */
    private void withLookups(BiConsumer<List<Category>, List<Unit>> action) {
        Async.run(this,
                () -> new Lookups(lookupService.getCategories(), lookupService.getUnits()),
                lookups -> action.accept(lookups.categories(), lookups.units()));
    }

    private record Lookups(List<Category> categories, List<Unit> units) {
    }
}
