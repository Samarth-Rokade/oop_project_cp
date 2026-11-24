package ui;

import exception.AuthenticationException;
import model.User;
import service.AuthenticationService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Login window. Login runs on a background thread (SwingWorker) so the
 * window never freezes while the database is contacted.
 */
public class LoginFrame extends JFrame {

    private final AuthenticationService authService = new AuthenticationService();

    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("Log in");
    private final JLabel messageLabel = new JLabel(" ");

    public LoginFrame() {
        super("Digital Farmer-to-Buyer Marketplace");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildBrandPanel(), BorderLayout.WEST);
        root.add(buildFormPanel(), BorderLayout.CENTER);
        setContentPane(root);

        // Enter in either field submits the form
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(e -> attemptLogin());
    }

    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.PRIMARY);
        panel.setPreferredSize(new Dimension(380, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("🌾");      // sheaf of rice emoji
        logo.setFont(logo.getFont().deriveFont(54f));
        logo.setForeground(java.awt.Color.WHITE);
        JLabel name = new JLabel("<html>Farmer-to-Buyer<br>Marketplace</html>");
        name.setFont(Theme.TITLE.deriveFont(28f));
        name.setForeground(java.awt.Color.WHITE);
        JLabel tagline = new JLabel("Connecting fresh supply with trusted demand.");
        tagline.setFont(Theme.SUBTITLE);
        tagline.setForeground(Theme.PRIMARY_LIGHT);

        for (JLabel l : new JLabel[]{logo, name, tagline}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        text.add(logo);
        text.add(Box.createVerticalStrut(12));
        text.add(name);
        text.add(Box.createVerticalStrut(10));
        text.add(tagline);
        panel.add(text);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.BACKGROUND);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0xE0E0E0)),
                BorderFactory.createEmptyBorder(32, 36, 32, 36)));

        JLabel title = new JLabel("Welcome back");
        title.setFont(Theme.TITLE);
        JLabel subtitle = new JLabel("Log in as a farmer, buyer or administrator");
        subtitle.setFont(Theme.SUBTITLE);
        subtitle.setForeground(Theme.TEXT_MUTED);

        emailField.putClientProperty("JTextField.placeholderText", "you@example.com");
        passwordField.putClientProperty("JTextField.placeholderText", "Password");
        passwordField.putClientProperty("JPasswordField.showRevealButton", true);
        for (JTextField f : new JTextField[]{emailField, passwordField}) {
            f.setMaximumSize(new Dimension(320, 38));
            f.setPreferredSize(new Dimension(320, 38));
        }

        loginButton.setBackground(Theme.PRIMARY);
        loginButton.setForeground(java.awt.Color.WHITE);
        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD, 14f));
        loginButton.setMaximumSize(new Dimension(320, 40));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        messageLabel.setForeground(Theme.ERROR);

        JLabel hint = new JLabel("<html><small>Demo: admin@market.com, ramesh@farm.com, "
                + "priya@buyer.com<br>Password for all demo accounts: password123</small></html>");
        hint.setForeground(Theme.TEXT_MUTED);

        Component[] rows = {
                title, Box.createVerticalStrut(4), subtitle, Box.createVerticalStrut(24),
                new JLabel("Email"), Box.createVerticalStrut(6), emailField, Box.createVerticalStrut(14),
                new JLabel("Password"), Box.createVerticalStrut(6), passwordField, Box.createVerticalStrut(10),
                messageLabel, Box.createVerticalStrut(10), loginButton, Box.createVerticalStrut(20), hint
        };
        for (Component c : rows) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(c);
        }
        outer.add(card);
        return outer;
    }

    private void attemptLogin() {
        String email = emailField.getText();
        char[] password = passwordField.getPassword();

        setBusy(true);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                try {
                    return authService.login(email, password);   // runs off the EDT
                } finally {
                    Arrays.fill(password, '\0');                  // wipe the password copy
                }
            }

            @Override
            protected void done() {                               // back on the EDT
                setBusy(false);
                try {
                    User user = get();
                    new DashboardFrame(user, authService).setVisible(true);
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(e.getCause());
                }
            }
        }.execute();
    }

    private void showError(Throwable cause) {
        if (cause instanceof AuthenticationException) {
            messageLabel.setText(cause.getMessage());
        } else {
            cause.printStackTrace();
            messageLabel.setText("<html>Cannot reach the database. Is Docker running?<br>"
                    + "Run: docker compose up -d</html>");
        }
        passwordField.setText("");
        passwordField.requestFocusInWindow();
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        loginButton.setText(busy ? "Logging in..." : "Log in");
        if (busy) {
            messageLabel.setText(" ");
        }
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
