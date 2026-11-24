package ui;

import model.AccountStatus;
import model.Admin;
import model.Farmer;
import model.Role;
import model.UserSummary;
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
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Admin: list users, verify/reject farms, suspend/reactivate accounts. */
public class AdminUsersPanel extends JPanel {

    private enum Show {
        ALL("Everyone", u -> true),
        PENDING("Farms awaiting verification", u -> u.verificationStatus() == Farmer.VerificationStatus.PENDING),
        FARMERS("Farmers", u -> u.role() == Role.FARMER),
        BUYERS("Buyers", u -> u.role() == Role.BUYER),
        SUSPENDED("Suspended", u -> u.accountStatus() == AccountStatus.SUSPENDED);

        final String label;
        final Predicate<UserSummary> test;

        Show(String label, Predicate<UserSummary> test) {
            this.label = label;
            this.test = test;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Admin admin;
    private final AdminService service;
    private final Runnable onChanged;
    private List<UserSummary> all = new ArrayList<>();

    private final JTextField search = new JTextField(18);
    private final JComboBox<Show> show = new JComboBox<>(Show.values());
    private final JLabel count = new JLabel(" ");
    private final BeanTableModel<UserSummary> model = new BeanTableModel<>(
            new BeanTableModel.Column<>("Name", UserSummary::fullName),
            new BeanTableModel.Column<>("Email", UserSummary::email),
            new BeanTableModel.Column<>("Phone", u -> u.phone() == null ? "-" : u.phone()),
            new BeanTableModel.Column<>("Role", u -> Pages.capitalise(u.role().name())),
            new BeanTableModel.Column<>("Farm / buyer type", u -> u.isFarmer() ? u.farmName()
                    : u.buyerType() == null ? "" : Pages.capitalise(u.buyerType())),
            new BeanTableModel.Column<>("Account", UserSummary::accountStatus),
            new BeanTableModel.Column<>("Farm verification", u -> u.verificationStatus() == null ? "" : u.verificationStatus()),
            new BeanTableModel.Column<>("Joined", u -> Formats.date(u.createdAt().toLocalDate())));
    private final JTable table = new JTable(model);

    private final JButton verify = new JButton("Verify farm");
    private final JButton reject = new JButton("Reject farm...");
    private final JButton suspend = new JButton("Suspend...");
    private final JButton reactivate = new JButton("Reactivate");

    public AdminUsersPanel(Admin admin, AdminService service, Runnable onChanged) {
        this.admin = admin;
        this.service = service;
        this.onChanged = onChanged;
        Pages.setUp(this);

        search.putClientProperty("JTextField.placeholderText", "Search name, email or farm");
        search.putClientProperty("JTextField.showClearButton", true);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        show.addActionListener(e -> applyFilter());
        count.setForeground(Theme.TEXT_MUTED);

        Tables.style(table);
        Tables.statusColumn(table, 5);
        Tables.statusColumn(table, 6);
        Pages.widths(table, 160, 200, 100, 70, 170, 100, 130, 100);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());

        verify.setBackground(Theme.PRIMARY);
        verify.setForeground(Color.WHITE);
        verify.addActionListener(e -> selected().ifPresent(u ->
                act(() -> service.verifyFarmer(admin, u.userId()), u.farmName() + " is now verified. Its products are visible to buyers.")));
        reject.addActionListener(e -> selected().ifPresent(u -> reason("Reject the farm \"" + u.farmName() + "\"?\nReason:")
                .ifPresent(r -> act(() -> service.rejectFarmer(admin, u.userId(), r), null))));
        suspend.setForeground(Theme.ERROR);
        suspend.addActionListener(e -> selected().ifPresent(u -> reason("Suspend " + u.fullName() + "?\n"
                + "They will not be able to log in" + (u.isFarmer() ? " and their products will be hidden" : "") + ".\nReason:")
                .ifPresent(r -> act(() -> service.suspendUser(admin, u.userId(), r), null))));
        reactivate.addActionListener(e -> selected().ifPresent(u ->
                act(() -> service.reactivateUser(admin, u.userId()), null)));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());

        add(Pages.header("Users", "Verify new farms and manage accounts. Administrator accounts cannot be changed here.",
                Pages.row(search, show, count)), BorderLayout.NORTH);
        add(Tables.scroll(table), BorderLayout.CENTER);
        add(Pages.actionBar(new JComponent[]{refresh}, suspend, reactivate, reject, verify), BorderLayout.SOUTH);
        updateButtons();
    }

    /** Pre-selects the "awaiting verification" filter (used from the overview). */
    public void showPendingFarms() {
        show.setSelectedItem(Show.PENDING);
    }

    public void reload() {
        Integer keep = selected().map(UserSummary::userId).orElse(null);
        Async.run(this, () -> service.getUsers(admin), users -> {
            all = users;
            applyFilter();
            if (keep != null) {
                model.indexOf(u -> u.userId() == keep).ifPresent(i -> table.setRowSelectionInterval(i, i));
            }
        });
    }

    private void applyFilter() {
        String text = search.getText().trim().toLowerCase();
        Show filter = (Show) show.getSelectedItem();
        Predicate<UserSummary> matchesText = u -> text.isEmpty()
                || u.fullName().toLowerCase().contains(text)
                || u.email().toLowerCase().contains(text)
                || (u.farmName() != null && u.farmName().toLowerCase().contains(text));
        List<UserSummary> rows = all.stream().filter(filter.test.and(matchesText)).collect(Collectors.toList());
        model.setRows(rows);
        count.setText(rows.size() + " of " + all.size());
        updateButtons();
    }

    private Optional<UserSummary> selected() {
        return Pages.selected(table, model);
    }

    private void updateButtons() {
        Optional<UserSummary> u = selected();
        boolean editable = u.map(x -> !x.isAdmin()).orElse(false);
        boolean farmer = u.map(UserSummary::isFarmer).orElse(false);
        Farmer.VerificationStatus v = u.map(UserSummary::verificationStatus).orElse(null);
        AccountStatus status = u.map(UserSummary::accountStatus).orElse(null);
        verify.setEnabled(farmer && v != Farmer.VerificationStatus.VERIFIED && status != AccountStatus.SUSPENDED);
        reject.setEnabled(farmer && v == Farmer.VerificationStatus.PENDING);
        suspend.setEnabled(editable && status != AccountStatus.SUSPENDED);
        reactivate.setEnabled(editable && status == AccountStatus.SUSPENDED);
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
            onChanged.run();
            if (doneMessage != null) {
                Dialogs.info(this, doneMessage);
            }
        });
    }

    private Optional<String> reason(String message) {
        String r = JOptionPane.showInputDialog(this, message, "Please confirm", JOptionPane.WARNING_MESSAGE);
        if (r == null) {
            return Optional.empty();
        }
        if (r.isBlank()) {
            Dialogs.showError(this, new exception.ValidationException("Please give a short reason."));
            return Optional.empty();
        }
        return Optional.of(r.trim());
    }
}
