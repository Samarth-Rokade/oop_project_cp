package ui;

import exception.ValidationException;
import model.Buyer;
import model.Farmer;
import model.User;
import service.ProfileService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Form for a farmer or buyer to edit their own profile. Shows the extra
 * fields for the user's role; email is shown but cannot be changed.
 */
public class ProfileDialog extends JDialog {

    private final JLabel errorLabel = new JLabel();
    private final JTextField nameField = new JTextField(26);
    private final JTextField phoneField = new JTextField(26);
    private final JTextField farmNameField = new JTextField(26);
    private final JTextField farmAddressField = new JTextField(26);
    private final JComboBox<Buyer.BuyerType> buyerTypeBox = new JComboBox<>(Buyer.BuyerType.values());
    private final JButton saveButton = new JButton("Save changes");

    private final User user;
    private final ProfileService service;
    private final Runnable onSaved;

    public ProfileDialog(Window owner, User user, ProfileService service, Runnable onSaved) {
        super(owner, "Edit profile", ModalityType.APPLICATION_MODAL);
        this.user = user;
        this.service = service;
        this.onSaved = onSaved;

        nameField.setText(user.getFullName());
        phoneField.setText(user.getPhone() == null ? "" : user.getPhone());
        phoneField.putClientProperty("JTextField.placeholderText", "10-digit mobile (optional)");
        JTextField emailField = new JTextField(user.getEmail(), 26);
        emailField.setEditable(false);
        emailField.setToolTipText("Email is your login id and cannot be changed");
        errorLabel.setForeground(Theme.ERROR);
        errorLabel.setVisible(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 6, 20));
        GridBagConstraints full = new GridBagConstraints();
        full.gridwidth = 2;
        full.fill = GridBagConstraints.HORIZONTAL;
        full.insets = new Insets(0, 0, 8, 0);
        form.add(errorLabel, full);
        int row = 1;
        row = addRow(form, "Full name *", nameField, row);
        row = addRow(form, "Email", emailField, row);
        row = addRow(form, "Phone", phoneField, row);
        if (user instanceof Farmer farmer) {
            farmNameField.setText(farmer.getFarmName());
            farmAddressField.setText(farmer.getFarmAddress());
            row = addRow(form, "Farm name *", farmNameField, row);
            addRow(form, "Farm address *", farmAddressField, row);
        } else if (user instanceof Buyer buyer) {
            buyerTypeBox.setSelectedItem(buyer.getBuyerType());
            addRow(form, "Buyer type *", buyerTypeBox, row);
        }

        saveButton.setBackground(Theme.PRIMARY);
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> save());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.add(cancel);
        buttons.add(saveButton);

        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    private void save() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String farmName = farmNameField.getText();
        String farmAddress = farmAddressField.getText();
        Buyer.BuyerType type = (Buyer.BuyerType) buyerTypeBox.getSelectedItem();

        saveButton.setEnabled(false);
        Async.run(this, () -> {
            if (user instanceof Farmer farmer) {
                service.updateFarmer(farmer, name, phone, farmName, farmAddress);
            } else if (user instanceof Buyer buyer) {
                service.updateBuyer(buyer, name, phone, type);
            }
            return null;
        }, ignored -> {
            dispose();
            onSaved.run();
        }, error -> {
            saveButton.setEnabled(true);
            if (error instanceof ValidationException) {
                errorLabel.setText("<html>" + error.getMessage().replace("\n", "<br>") + "</html>");
                errorLabel.setVisible(true);
                pack();
            } else {
                Dialogs.showError(this, error);
            }
        });
    }

    private static int addRow(JPanel form, String label, JComponent field, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 12);
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        form.add(field, c);
        return row + 1;
    }
}
