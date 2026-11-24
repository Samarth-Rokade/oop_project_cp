package ui;

import exception.ValidationException;
import model.Buyer;
import service.RegistrationService;
import service.RegistrationService.AccountDetails;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Sign-up form. The user picks "Farmer" or "Buyer"; a CardLayout swaps
 * the role-specific fields.
 */
public class RegisterDialog extends JDialog {

    private static final String FARMER = "farmer";
    private static final String BUYER = "buyer";

    private final RegistrationService service = new RegistrationService();
    private final BiConsumer<String, char[]> onRegistered;

    private String role = BUYER;
    private final CardLayout roleCards = new CardLayout();
    private final JPanel roleFields = new JPanel(roleCards);

    private final JLabel errorLabel = new JLabel();
    private final JTextField nameField = new JTextField(24);
    private final JTextField emailField = new JTextField(24);
    private final JTextField phoneField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JPasswordField confirmField = new JPasswordField(24);
    private final JTextField farmNameField = new JTextField(24);
    private final JTextArea farmAddressArea = new JTextArea(2, 24);
    private final JComboBox<Buyer.BuyerType> buyerTypeBox = new JComboBox<>(Buyer.BuyerType.values());
    private final JButton createButton = new JButton("Create account");

    /**
     * @param onRegistered called with (email, password) after the account is created,
     *                     so the login screen can sign the user in
     */
    public RegisterDialog(Window owner, BiConsumer<String, char[]> onRegistered) {
        super(owner, "Create an account", ModalityType.APPLICATION_MODAL);
        this.onRegistered = onRegistered;

        JLabel title = new JLabel("Create your account");
        title.setFont(Theme.TITLE);
        JLabel subtitle = new JLabel("Farmers list their produce; buyers order directly from farms.");
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
        header.add(roleChooser(), BorderLayout.SOUTH);
        header.setBorder(BorderFactory.createEmptyBorder(18, 22, 6, 22));

        nameField.putClientProperty("JTextField.placeholderText", "As it should appear to others");
        emailField.putClientProperty("JTextField.placeholderText", "you@example.com");
        phoneField.putClientProperty("JTextField.placeholderText", "10-digit mobile (optional)");
        passwordField.putClientProperty("JTextField.placeholderText", "At least 8 characters, letters and numbers");
        passwordField.putClientProperty("JPasswordField.showRevealButton", true);
        confirmField.putClientProperty("JPasswordField.showRevealButton", true);
        farmNameField.putClientProperty("JTextField.placeholderText", "e.g. Patil Organic Farms");
        farmAddressArea.setLineWrap(true);
        farmAddressArea.setWrapStyleWord(true);
        errorLabel.setForeground(Theme.ERROR);
        errorLabel.setVisible(false);

        JPanel common = new JPanel(new GridBagLayout());
        int row = 0;
        GridBagConstraints full = new GridBagConstraints();
        full.gridwidth = 2;
        full.fill = GridBagConstraints.HORIZONTAL;
        full.insets = new Insets(0, 0, 8, 0);
        common.add(errorLabel, full);
        row++;
        row = addRow(common, "Full name *", nameField, row);
        row = addRow(common, "Email *", emailField, row);
        row = addRow(common, "Phone", phoneField, row);
        row = addRow(common, "Password *", passwordField, row);
        addRow(common, "Confirm password *", confirmField, row);

        JPanel farmerPanel = new JPanel(new GridBagLayout());
        int fr = addRow(farmerPanel, "Farm name *", farmNameField, 0);
        addRow(farmerPanel, "Farm address *", new JScrollPane(farmAddressArea), fr);
        JLabel note = new JLabel("<html><small>New farms are checked by the administrator before buyers can see "
                + "their products. You can add products while you wait.</small></html>");
        note.setForeground(Theme.TEXT_MUTED);
        GridBagConstraints nc = new GridBagConstraints();
        nc.gridy = fr + 1;
        nc.gridwidth = 2;
        nc.fill = GridBagConstraints.HORIZONTAL;
        nc.insets = new Insets(4, 0, 0, 0);
        farmerPanel.add(note, nc);

        JPanel buyerPanel = new JPanel(new GridBagLayout());
        addRow(buyerPanel, "I am buying as *", buyerTypeBox, 0);

        roleFields.add(farmerPanel, FARMER);
        roleFields.add(buyerPanel, BUYER);
        roleCards.show(roleFields, BUYER);

        JPanel form = new JPanel(new BorderLayout(0, 6));
        form.setBorder(BorderFactory.createEmptyBorder(6, 22, 6, 22));
        form.add(common, BorderLayout.NORTH);
        form.add(roleFields, BorderLayout.CENTER);

        createButton.setBackground(Theme.PRIMARY);
        createButton.setForeground(Color.WHITE);
        createButton.setFont(createButton.getFont().deriveFont(Font.BOLD));
        createButton.addActionListener(e -> register());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 12));
        buttons.add(cancel);
        buttons.add(createButton);

        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(createButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel roleChooser() {
        JToggleButton buyer = new JToggleButton("I want to buy");
        JToggleButton farmer = new JToggleButton("I am a farmer");
        ButtonGroup group = new ButtonGroup();
        group.add(buyer);
        group.add(farmer);
        buyer.setSelected(true);
        buyer.addActionListener(e -> switchRole(BUYER));
        farmer.addActionListener(e -> switchRole(FARMER));
        for (JToggleButton b : new JToggleButton[]{buyer, farmer}) {
            b.setFocusable(false);
            b.setFont(b.getFont().deriveFont(Font.BOLD));
        }
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 4, 0));
        panel.add(buyer);
        panel.add(farmer);
        return panel;
    }

    private void switchRole(String newRole) {
        role = newRole;
        roleCards.show(roleFields, newRole);
        errorLabel.setVisible(false);
        pack();
    }

    private static int addRow(JPanel form, String label, JComponent field, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 12);
        JLabel l = new JLabel(label);
        l.setPreferredSize(new java.awt.Dimension(130, l.getPreferredSize().height));
        form.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        form.add(field, c);
        return row + 1;
    }

    private void register() {
        String email = emailField.getText();
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();
        AccountDetails account = new AccountDetails(nameField.getText(), email, phoneField.getText(), password, confirm);
        String chosenRole = role;
        String farmName = farmNameField.getText();
        String farmAddress = farmAddressArea.getText();
        Buyer.BuyerType type = (Buyer.BuyerType) buyerTypeBox.getSelectedItem();

        createButton.setEnabled(false);
        Async.run(this, () -> FARMER.equals(chosenRole)
                        ? service.registerFarmer(account, farmName, farmAddress)
                        : service.registerBuyer(account, type),
                userId -> {
                    Arrays.fill(confirm, '\0');
                    dispose();
                    if (FARMER.equals(chosenRole)) {
                        Dialogs.info(getOwner(), "Welcome! Your farm account was created.\n"
                                + "The administrator will verify your farm - until then buyers cannot see your products.");
                    }
                    onRegistered.accept(email.trim().toLowerCase(), password);
                },
                error -> {
                    createButton.setEnabled(true);
                    Arrays.fill(password, '\0');
                    Arrays.fill(confirm, '\0');
                    if (error instanceof ValidationException) {
                        errorLabel.setText("<html>" + error.getMessage().replace("&", "&amp;")
                                .replace("<", "&lt;").replace("\n", "<br>") + "</html>");
                        errorLabel.setVisible(true);
                        pack();
                    } else {
                        Dialogs.showError(this, error);
                    }
                });
    }
}
