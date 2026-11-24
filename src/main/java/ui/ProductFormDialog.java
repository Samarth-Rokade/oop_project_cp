package ui;

import exception.ValidationException;
import model.Category;
import model.ListingStatus;
import model.Product;
import model.Unit;
import util.Formats;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerDateModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
 * Modal form to add a new product or edit an existing one.
 * Validation errors from the service are shown inside the form.
 */
public class ProductFormDialog extends JDialog {

    /** What happens on "Save"; may throw ValidationException. */
    @FunctionalInterface
    public interface SaveAction {
        Product save(Product product) throws Exception;
    }

    private final Product original;
    private final SaveAction saveAction;
    private final Consumer<Product> onSaved;

    private final JLabel errorLabel = new JLabel();
    private final JTextField nameField = new JTextField(24);
    private final JComboBox<Category> categoryBox;
    private final JComboBox<Unit> unitBox;
    private final JTextField quantityField = new JTextField(10);
    private final JTextField priceField = new JTextField(10);
    private final JSpinner harvestSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JCheckBox visibleBox = new JCheckBox("Visible to buyers");
    private final JTextArea descriptionArea = new JTextArea(3, 24);
    private final JTextField imageField = new JTextField(18);
    private final JButton saveButton = new JButton("Save");

    public ProductFormDialog(Window owner, Product existing, List<Category> categories, List<Unit> units,
                             SaveAction saveAction, Consumer<Product> onSaved) {
        super(owner, existing == null ? "Add product" : "Edit product", ModalityType.APPLICATION_MODAL);
        this.original = existing;
        this.saveAction = saveAction;
        this.onSaved = onSaved;
        this.categoryBox = new JComboBox<>(categories.toArray(new Category[0]));
        this.unitBox = new JComboBox<>(units.toArray(new Unit[0]));

        buildUi();
        fillFields();

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        harvestSpinner.setEditor(new JSpinner.DateEditor(harvestSpinner, "dd-MM-yyyy"));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        nameField.putClientProperty("JTextField.placeholderText", "e.g. Tomato");
        quantityField.putClientProperty("JTextField.placeholderText", "0");
        priceField.putClientProperty("JTextField.placeholderText", "0.00");
        imageField.putClientProperty("JTextField.placeholderText", "optional");
        visibleBox.setSelected(true);

        errorLabel.setForeground(Theme.ERROR);
        errorLabel.setVisible(false);

        JButton browse = new JButton("Browse...");
        browse.addActionListener(e -> chooseImage());
        JPanel imageRow = new JPanel(new BorderLayout(6, 0));
        imageRow.add(imageField, BorderLayout.CENTER);
        imageRow.add(browse, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        int row = 0;
        addFull(form, errorLabel, row++);
        addRow(form, "Product name *", nameField, row++);
        addRow(form, "Category *", categoryBox, row++);
        addRow(form, "Unit *", unitBox, row++);
        addRow(form, "Available quantity *", quantityField, row++);
        addRow(form, "Price per unit (₹) *", priceField, row++);
        addRow(form, "Harvest date", harvestSpinner, row++);
        addRow(form, "Description", new JScrollPane(descriptionArea), row++);
        addRow(form, "Image", imageRow, row++);
        addRow(form, "", visibleBox, row);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        saveButton.setBackground(Theme.PRIMARY);
        saveButton.setForeground(java.awt.Color.WHITE);
        saveButton.addActionListener(e -> save());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 12));
        buttons.add(cancel);
        buttons.add(saveButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static void addRow(JPanel form, String label, JComponent field, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(5, 0, 5, 12);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        form.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        form.add(field, c);
    }

    private static void addFull(JPanel form, JComponent component, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);
        form.add(component, c);
    }

    private void fillFields() {
        if (original == null) {
            return;
        }
        nameField.setText(original.getProductName());
        selectCategory(original.getCategoryId());
        selectUnit(original.getUnitCode());
        quantityField.setText(Formats.quantity(original.getAvailableQuantity()));
        priceField.setText(original.getPricePerUnit().toPlainString());
        if (original.getHarvestDate() != null) {
            harvestSpinner.setValue(toDate(original.getHarvestDate()));
        }
        visibleBox.setSelected(original.getListingStatus() != ListingStatus.INACTIVE);
        descriptionArea.setText(original.getDescription() == null ? "" : original.getDescription());
        imageField.setText(original.getImagePath() == null ? "" : original.getImagePath());
    }

    private void selectCategory(int categoryId) {
        for (int i = 0; i < categoryBox.getItemCount(); i++) {
            if (categoryBox.getItemAt(i).getCategoryId() == categoryId) {
                categoryBox.setSelectedIndex(i);
            }
        }
    }

    private void selectUnit(String unitCode) {
        for (int i = 0; i < unitBox.getItemCount(); i++) {
            if (unitBox.getItemAt(i).getUnitCode().equals(unitCode)) {
                unitBox.setSelectedIndex(i);
            }
        }
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images (png, jpg)", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            imageField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void save() {
        Product product;
        try {
            product = readForm();
        } catch (ValidationException e) {
            showErrors(e.getMessage());
            return;
        }

        saveButton.setEnabled(false);
        Async.run(this, () -> saveAction.save(product),
                saved -> {
                    dispose();
                    onSaved.accept(saved);
                },
                error -> {
                    saveButton.setEnabled(true);
                    if (error instanceof ValidationException) {
                        showErrors(error.getMessage());
                    } else {
                        Dialogs.showError(this, error);
                    }
                });
    }

    /** Builds a Product from the fields; reports unreadable numbers. */
    private Product readForm() throws ValidationException {
        Product p = original == null ? new Product() : new Product(original);
        StringJoiner errors = new StringJoiner("\n");

        p.setProductName(nameField.getText());
        Category category = (Category) categoryBox.getSelectedItem();
        p.setCategoryId(category == null ? 0 : category.getCategoryId());
        p.setCategoryName(category == null ? null : category.getCategoryName());
        Unit unit = (Unit) unitBox.getSelectedItem();
        p.setUnitCode(unit == null ? null : unit.getUnitCode());
        p.setAvailableQuantity(parseNumber(quantityField.getText(), "Quantity", errors));
        p.setPricePerUnit(parseNumber(priceField.getText(), "Price", errors));
        p.setHarvestDate(toLocalDate((Date) harvestSpinner.getValue()));
        p.setDescription(descriptionArea.getText());
        p.setImagePath(imageField.getText());
        p.setListingStatus(visibleBox.isSelected() ? ListingStatus.ACTIVE : ListingStatus.INACTIVE);

        if (errors.length() > 0) {
            throw new ValidationException(errors.toString());
        }
        return p;
    }

    private static BigDecimal parseNumber(String text, String fieldName, StringJoiner errors) {
        String cleaned = text == null ? "" : text.trim().replace(",", "");
        if (cleaned.isEmpty()) {
            errors.add(fieldName + " is required.");
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            errors.add(fieldName + " must be a number, e.g. 25 or 12.50.");
            return null;
        }
    }

    private void showErrors(String message) {
        errorLabel.setText("<html>" + message.replace("&", "&amp;").replace("<", "&lt;")
                .replace("\n", "<br>") + "</html>");
        errorLabel.setVisible(true);
        pack();
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
