package ui;

import model.Admin;
import model.Category;
import model.Unit;
import service.AdminService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Admin: product categories and units of sale. */
public class AdminCatalogPanel extends JPanel {

    private record CatalogData(List<Category> categories, Map<Integer, Integer> categoryUsage,
                               List<Unit> units, Map<String, Integer> unitUsage) {
    }

    private final Admin admin;
    private final AdminService service;
    private Map<Integer, Integer> categoryUsage = Map.of();
    private Map<String, Integer> unitUsage = Map.of();

    private final BeanTableModel<Category> categoryModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Category", Category::getCategoryName),
            new BeanTableModel.Column<>("Description", Category::getDescription),
            new BeanTableModel.Column<>("Products", c -> categoryUsage.getOrDefault(c.getCategoryId(), 0)));
    private final JTable categoryTable = new JTable(categoryModel);

    private final BeanTableModel<Unit> unitModel = new BeanTableModel<>(
            new BeanTableModel.Column<>("Code", Unit::getUnitCode),
            new BeanTableModel.Column<>("Name", Unit::getUnitName),
            new BeanTableModel.Column<>("Products", u -> unitUsage.getOrDefault(u.getUnitCode(), 0)));
    private final JTable unitTable = new JTable(unitModel);

    public AdminCatalogPanel(Admin admin, AdminService service) {
        this.admin = admin;
        this.service = service;
        Pages.setUp(this);

        Tables.style(categoryTable);
        Tables.alignRight(categoryTable, 2);
        Pages.widths(categoryTable, 150, 260, 80);
        Tables.style(unitTable);
        Tables.alignRight(unitTable, 2);
        Pages.widths(unitTable, 90, 200, 80);

        JButton addCategory = new JButton("Add");
        JButton editCategory = new JButton("Edit");
        JButton deleteCategory = new JButton("Delete");
        addCategory.addActionListener(e -> editCategory(null));
        editCategory.addActionListener(e -> Pages.selected(categoryTable, categoryModel).ifPresent(this::editCategory));
        deleteCategory.addActionListener(e -> Pages.selected(categoryTable, categoryModel).ifPresent(c -> {
            if (Dialogs.confirm(this, "Delete the category \"" + c.getCategoryName() + "\"?")) {
                act(() -> service.deleteCategory(admin, c));
            }
        }));

        JButton addUnit = new JButton("Add");
        JButton renameUnit = new JButton("Rename");
        JButton deleteUnit = new JButton("Delete");
        addUnit.addActionListener(e -> addUnit());
        renameUnit.addActionListener(e -> Pages.selected(unitTable, unitModel).ifPresent(this::renameUnit));
        deleteUnit.addActionListener(e -> Pages.selected(unitTable, unitModel).ifPresent(u -> {
            if (Dialogs.confirm(this, "Delete the unit " + u.getUnitCode() + "?")) {
                act(() -> service.deleteUnit(admin, u));
            }
        }));

        JPanel halves = new JPanel(new GridLayout(1, 2, 20, 0));
        halves.setOpaque(false);
        halves.add(section("Categories", categoryTable, addCategory, editCategory, deleteCategory));
        halves.add(section("Units of sale", unitTable, addUnit, renameUnit, deleteUnit));

        add(Pages.header("Categories & Units",
                "Farmers choose from these when listing products. Only unused entries can be deleted.", null),
                BorderLayout.NORTH);
        add(halves, BorderLayout.CENTER);
    }

    private static JPanel section(String title, JTable table, JButton... buttons) {
        JLabel label = new JLabel(title);
        label.setFont(Theme.HEADING);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(Tables.scroll(table), BorderLayout.CENTER);
        panel.add(Pages.actionBar(new JComponent[0], buttons), BorderLayout.SOUTH);
        return panel;
    }

    public void reload() {
        Async.run(this, () -> new CatalogData(service.getCategories(admin), service.getCategoryUsage(admin),
                service.getUnits(admin), service.getUnitUsage(admin)), data -> {
            categoryUsage = data.categoryUsage();
            unitUsage = data.unitUsage();
            categoryModel.setRows(data.categories());
            unitModel.setRows(data.units());
        });
    }

    private void editCategory(Category existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getCategoryName(), 24);
        JTextField description = new JTextField(existing == null || existing.getDescription() == null
                ? "" : existing.getDescription(), 24);
        JPanel form = new JPanel(new GridLayout(4, 1, 0, 4));
        form.add(new JLabel("Name *"));
        form.add(name);
        form.add(new JLabel("Description"));
        form.add(description);
        int choice = JOptionPane.showConfirmDialog(this, form, existing == null ? "Add category" : "Edit category",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            int id = existing == null ? 0 : existing.getCategoryId();
            act(() -> service.saveCategory(admin, id, name.getText(), description.getText()));
        }
    }

    private void addUnit() {
        JTextField code = new JTextField(10);
        JTextField name = new JTextField(20);
        code.putClientProperty("JTextField.placeholderText", "e.g. BOX");
        name.putClientProperty("JTextField.placeholderText", "e.g. Box (10 kg)");
        JPanel form = new JPanel(new GridLayout(4, 1, 0, 4));
        form.add(new JLabel("Code * (letters only)"));
        form.add(code);
        form.add(new JLabel("Name *"));
        form.add(name);
        if (JOptionPane.showConfirmDialog(this, form, "Add unit", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            act(() -> service.addUnit(admin, code.getText(), name.getText()));
        }
    }

    private void renameUnit(Unit unit) {
        Object name = JOptionPane.showInputDialog(this, "New name for " + unit.getUnitCode() + ":", "Rename unit",
                JOptionPane.PLAIN_MESSAGE, null, null, unit.getUnitName());
        if (name != null) {
            act(() -> service.renameUnit(admin, unit.getUnitCode(), name.toString()));
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private void act(Action action) {
        Async.run(this, () -> {
            action.run();
            return true;
        }, ok -> reload());
    }

    @SuppressWarnings("unused")
    private Optional<Category> selectedCategory() {
        return Pages.selected(categoryTable, categoryModel);
    }
}
