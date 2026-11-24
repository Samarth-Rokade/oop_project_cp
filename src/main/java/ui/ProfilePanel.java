package ui;

import model.User;
import service.ProfileService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.function.Supplier;

/**
 * Profile page for a farmer or buyer: a key/value card plus an
 * "Edit Profile" button. After a save the card is rebuilt from the
 * (already updated) user object.
 */
public class ProfilePanel extends JPanel {

    private final String titleText;
    private final User user;
    private final Supplier<String[][]> rows;
    private final ProfileService service = new ProfileService();
    private final JPanel north = new JPanel(new BorderLayout(0, 16));

    /** @param rows builds the label/value pairs from the user's current details */
    public ProfilePanel(String titleText, User user, Supplier<String[][]> rows) {
        super(new BorderLayout());
        this.titleText = titleText;
        this.user = user;
        this.rows = rows;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        north.setOpaque(false);
        add(north, BorderLayout.NORTH);
        rebuild();
    }

    private void rebuild() {
        north.removeAll();

        JLabel title = new JLabel(titleText);
        title.setFont(Theme.TITLE);
        JButton edit = new JButton("Edit Profile");
        edit.setBackground(Theme.PRIMARY);
        edit.setForeground(Color.WHITE);
        edit.addActionListener(e -> openEditor());
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        JPanel buttonWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonWrap.setOpaque(false);
        buttonWrap.add(edit);
        header.add(buttonWrap, BorderLayout.EAST);

        String[][] data = rows.get();
        JPanel grid = new JPanel(new GridLayout(data.length, 2, 12, 10));
        grid.setBackground(Theme.SURFACE);
        grid.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E0E0)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        for (String[] row : data) {
            JLabel key = new JLabel(row[0]);
            key.setForeground(Theme.TEXT_MUTED);
            grid.add(key);
            grid.add(new JLabel(row[1]));
        }

        north.add(header, BorderLayout.NORTH);
        north.add(grid, BorderLayout.CENTER);
        north.revalidate();
        north.repaint();
    }

    private void openEditor() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new ProfileDialog(owner, user, service, () -> {
            rebuild();
            if (owner instanceof DashboardFrame frame) {
                frame.refreshUserDetails();                 // farm or buyer name may have changed
            }
            Dialogs.info(this, "Your profile has been updated.");
        }).setVisible(true);
    }
}
