package ui;

import model.Admin;
import model.AuditEntry;
import service.AdminService;
import util.Formats;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;

/** Admin: searchable audit trail (newest 500 entries). */
public class AuditLogPanel extends JPanel {

    private static final String ALL = "All actions";

    private final Admin admin;
    private final AdminService service;
    private final JComboBox<String> actionBox = new JComboBox<>();
    private final JTextField search = new JTextField(20);
    private final JLabel count = new JLabel(" ");
    private final Timer searchDelay = new Timer(350, e -> load());
    private boolean loadingActions;
    private int sequence;

    private final BeanTableModel<AuditEntry> model = new BeanTableModel<>(
            new BeanTableModel.Column<>("When", e -> Formats.dateTime(e.actionTime())),
            new BeanTableModel.Column<>("Who", e -> e.userName() == null ? "System" : e.userName()),
            new BeanTableModel.Column<>("Action", e -> Pages.capitalise(e.action())),
            new BeanTableModel.Column<>("Record", e -> Pages.capitalise(e.entityType())
                    + (e.entityId() == null ? "" : " #" + e.entityId())),
            new BeanTableModel.Column<>("Details", AuditEntry::details));
    private final JTable table = new JTable(model);

    public AuditLogPanel(Admin admin, AdminService service) {
        this.admin = admin;
        this.service = service;
        Pages.setUp(this);

        searchDelay.setRepeats(false);
        actionBox.addItem(ALL);
        actionBox.addActionListener(e -> {
            if (!loadingActions) {
                load();
            }
        });
        search.putClientProperty("JTextField.placeholderText", "Search person or details");
        search.putClientProperty("JTextField.showClearButton", true);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchDelay.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchDelay.restart(); }
        });
        count.setForeground(Theme.TEXT_MUTED);

        Tables.style(table);
        Pages.widths(table, 160, 160, 140, 140, 420);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());

        add(Pages.header("Activity log", "Important actions recorded by the application and by database triggers.",
                Pages.row(actionBox, search, count)), BorderLayout.NORTH);
        add(Tables.scroll(table), BorderLayout.CENTER);
        add(Pages.actionBar(new JComponent[]{refresh}), BorderLayout.SOUTH);
    }

    public void reload() {
        Async.run(this, () -> service.getActivityTypes(admin), actions -> {
            Object selected = actionBox.getSelectedItem();
            loadingActions = true;
            actionBox.removeAllItems();
            actionBox.addItem(ALL);
            actions.forEach(actionBox::addItem);
            actionBox.setSelectedItem(actions.contains(selected) ? selected : ALL);
            loadingActions = false;
            load();
        });
    }

    private void load() {
        String action = ALL.equals(actionBox.getSelectedItem()) ? null : (String) actionBox.getSelectedItem();
        String text = search.getText();
        int mine = ++sequence;
        Async.run(this, () -> service.getActivity(admin, action, text), entries -> {
            if (mine == sequence) {
                model.setRows(entries);
                count.setText(entries.size() == 500 ? "latest 500 entries" : entries.size() + " entries");
            }
        });
    }
}
