package ui;

import exception.ValidationException;
import model.Address;
import model.User;
import service.AddressService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.util.function.Consumer;

/** Form to add a delivery address. */
public class AddressDialog extends JDialog {

    private final JLabel errorLabel = new JLabel();
    private final JTextField lineField = new JTextField(26);
    private final JTextField cityField = new JTextField(14);
    private final JTextField districtField = new JTextField(14);
    private final JTextField stateField = new JTextField("Maharashtra", 14);
    private final JTextField pinField = new JTextField(8);
    private final JCheckBox defaultBox = new JCheckBox("Make this my default address");
    private final JButton saveButton = new JButton("Save address");

    public AddressDialog(Window owner, User user, AddressService service, Consumer<Address> onSaved) {
        super(owner, "New delivery address", ModalityType.APPLICATION_MODAL);

        lineField.putClientProperty("JTextField.placeholderText", "Flat / house, street, area");
        pinField.putClientProperty("JTextField.placeholderText", "411001");
        errorLabel.setForeground(Theme.ERROR);
        errorLabel.setVisible(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 6, 20));
        int row = 0;
        GridBagConstraints full = new GridBagConstraints();
        full.gridwidth = 2;
        full.fill = GridBagConstraints.HORIZONTAL;
        full.insets = new Insets(0, 0, 8, 0);
        form.add(errorLabel, full);
        row++;
        row = addRow(form, "Address *", lineField, row);
        row = addRow(form, "City *", cityField, row);
        row = addRow(form, "District *", districtField, row);
        row = addRow(form, "State *", stateField, row);
        row = addRow(form, "PIN code *", pinField, row);
        addRow(form, "", defaultBox, row);

        saveButton.setBackground(Theme.PRIMARY);
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> {
            Address a = new Address();
            a.setAddressLine(lineField.getText());
            a.setCity(cityField.getText());
            a.setDistrict(districtField.getText());
            a.setState(stateField.getText());
            a.setPostalCode(pinField.getText());
            a.setDefaultAddress(defaultBox.isSelected());
            saveButton.setEnabled(false);
            Async.run(this, () -> service.addAddress(user, a), saved -> {
                dispose();
                onSaved.accept(saved);
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
        });
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
