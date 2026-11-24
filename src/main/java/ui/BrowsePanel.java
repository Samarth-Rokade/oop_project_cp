package ui;

import dao.ProductSearch;
import exception.ValidationException;
import model.Cart;
import model.Category;
import model.FarmSummary;
import model.Product;
import service.CatalogService;
import service.LookupService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.List;

/**
 * Buyer's product catalogue: filters on top, product cards below.
 * Filtering is done by the database (CatalogDAO builds the WHERE clause).
 */
public class BrowsePanel extends JPanel {

    private static final int CARD_WIDTH = 300;

    private final CatalogService catalogService;
    private final LookupService lookupService;
    private final Cart cart;

    private final JTextField searchField = new JTextField(16);
    private final JComboBox<Object> categoryBox = new JComboBox<>();
    private final JComboBox<Object> farmBox = new JComboBox<>();
    private final JTextField minPriceField = new JTextField(5);
    private final JTextField maxPriceField = new JTextField(5);
    private final JComboBox<ProductSearch.Sort> sortBox = new JComboBox<>(ProductSearch.Sort.values());
    private final JLabel resultLabel = new JLabel(" ");

    private final GridLayout gridLayout = new GridLayout(0, 3, 14, 14);
    private final JPanel grid = new ScrollableGrid(gridLayout);
    private final Timer searchDelay;

    /** Increases with every search so late, out-of-date results are ignored. */
    private int searchSequence;

    public BrowsePanel(CatalogService catalogService, LookupService lookupService, Cart cart) {
        super(new BorderLayout());
        this.catalogService = catalogService;
        this.lookupService = lookupService;
        this.cart = cart;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        // Wait until the user pauses typing before querying the database
        searchDelay = new Timer(350, e -> runSearch());
        searchDelay.setRepeats(false);

        add(buildTop(), BorderLayout.NORTH);
        add(buildResults(), BorderLayout.CENTER);

        loadFilterOptions();
        runSearch();
    }

    private JPanel buildTop() {
        JLabel title = new JLabel("Fresh from the farm");
        title.setFont(Theme.TITLE);
        JLabel subtitle = new JLabel("Buy directly from verified farmers. Prices are set by the farmer.");
        subtitle.setForeground(Theme.TEXT_MUTED);

        searchField.putClientProperty("JTextField.placeholderText", "Search products or farms");
        searchField.putClientProperty("JTextField.showClearButton", true);
        minPriceField.putClientProperty("JTextField.placeholderText", "Min ₹");
        maxPriceField.putClientProperty("JTextField.placeholderText", "Max ₹");
        DocumentListener typing = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchDelay.restart(); }
        };
        searchField.getDocument().addDocumentListener(typing);
        minPriceField.getDocument().addDocumentListener(typing);
        maxPriceField.getDocument().addDocumentListener(typing);

        categoryBox.addItem("All categories");
        farmBox.addItem("All farms");
        categoryBox.addActionListener(e -> runSearch());
        farmBox.addActionListener(e -> runSearch());
        sortBox.addActionListener(e -> runSearch());

        JButton clear = new JButton("Clear filters");
        clear.putClientProperty("JButton.buttonType", "borderless");
        clear.addActionListener(e -> clearFilters());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(categoryBox);
        filters.add(farmBox);
        filters.add(minPriceField);
        filters.add(new JLabel("-"));
        filters.add(maxPriceField);
        filters.add(sortBox);
        filters.add(clear);

        resultLabel.setForeground(Theme.TEXT_MUTED);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{title, subtitle, filters, resultLabel}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        top.add(title);
        top.add(Box.createVerticalStrut(4));
        top.add(subtitle);
        top.add(Box.createVerticalStrut(16));
        top.add(filters);
        top.add(Box.createVerticalStrut(10));
        top.add(resultLabel);
        top.add(Box.createVerticalStrut(10));

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        getActionMap().put("focusSearch", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchField.requestFocusInWindow();
            }
        });
        return top;
    }

    private JScrollPane buildResults() {
        grid.setOpaque(false);
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        // Responsive grid: as many columns as fit the window width
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int columns = Math.max(1, scroll.getViewport().getWidth() / CARD_WIDTH);
                if (columns != gridLayout.getColumns()) {
                    gridLayout.setColumns(columns);
                    grid.revalidate();
                }
            }
        });
        return scroll;
    }

    private void loadFilterOptions() {
        Async.run(this, () -> new FilterOptions(lookupService.getCategories(), catalogService.getFarms()), options -> {
            options.categories().forEach(categoryBox::addItem);
            options.farms().forEach(farmBox::addItem);
        });
    }

    private record FilterOptions(List<Category> categories, List<FarmSummary> farms) {
    }

    /** Reads the filters and queries the database in the background. */
    public void runSearch() {
        ProductSearch search;
        try {
            search = new ProductSearch()
                    .text(searchField.getText())
                    .categoryId(categoryBox.getSelectedItem() instanceof Category c ? c.getCategoryId() : null)
                    .farmerId(farmBox.getSelectedItem() instanceof FarmSummary f ? f.farmerId() : null)
                    .minPrice(parsePrice(minPriceField.getText(), "Minimum price"))
                    .maxPrice(parsePrice(maxPriceField.getText(), "Maximum price"))
                    .sort((ProductSearch.Sort) sortBox.getSelectedItem());
        } catch (ValidationException e) {
            resultLabel.setText(e.getMessage());
            return;
        }

        int mySequence = ++searchSequence;
        Async.run(this, () -> catalogService.search(search), products -> {
            if (mySequence == searchSequence) {          // ignore results of an older search
                showResults(products);
            }
        }, error -> {
            if (error instanceof ValidationException) {
                resultLabel.setText(error.getMessage());
            } else {
                Dialogs.showError(this, error);
            }
        });
    }

    private void showResults(List<Product> products) {
        grid.removeAll();
        for (Product p : products) {
            grid.add(new ProductCard(p, this::addToCart, this::showDetails));
        }
        if (products.isEmpty()) {
            JLabel empty = new JLabel("No products match your filters.", SwingConstants.CENTER);
            empty.setForeground(Theme.TEXT_MUTED);
            grid.add(empty);
        }
        resultLabel.setText(products.size() + (products.size() == 1 ? " product" : " products"));
        grid.revalidate();
        grid.repaint();
    }

    /** Shared by cards and the details dialog. Returns true if added. */
    private boolean addToCart(Product product, BigDecimal quantity) {
        try {
            cart.add(product, quantity);
            return true;
        } catch (ValidationException e) {
            Dialogs.showError(this, e);
            return false;
        }
    }

    private void showDetails(Product product) {
        new ProductDetailsDialog(SwingUtilities.getWindowAncestor(this), product, this::addToCart).setVisible(true);
    }

    private void clearFilters() {
        searchField.setText("");
        minPriceField.setText("");
        maxPriceField.setText("");
        categoryBox.setSelectedIndex(0);
        farmBox.setSelectedIndex(0);
        sortBox.setSelectedIndex(0);
        runSearch();
    }

    private static BigDecimal parsePrice(String text, String field) throws ValidationException {
        String t = text.trim().replace(",", "");
        if (t.isEmpty()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(t);
            if (value.signum() < 0) {
                throw new ValidationException(field + " cannot be negative.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number.");
        }
    }

    /** Panel that fills the viewport width so the grid wraps instead of scrolling sideways. */
    private static class ScrollableGrid extends JPanel implements Scrollable {
        ScrollableGrid(GridLayout layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
