package ui;

import model.Buyer;
import model.Farmer;
import model.User;
import service.AuthenticationService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Main window after login: top bar plus the dashboard for the user's role.
 */
public class DashboardFrame extends JFrame {

    public DashboardFrame(User user, AuthenticationService authService) {
        super(user.getDashboardTitle());       // polymorphic call
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);

        // ----- top bar -----
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.PRIMARY);
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel appName = new JLabel("🌾  Farmer-to-Buyer Marketplace");
        appName.setFont(Theme.HEADING);
        appName.setForeground(Color.WHITE);

        JLabel who = new JLabel(user.getFullName() + "  •  " + user.getRole());
        who.setForeground(Theme.PRIMARY_LIGHT);
        JButton logout = new JButton("Log out");
        logout.addActionListener(e -> {
            authService.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(who);
        right.add(logout);

        topBar.add(appName, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);

        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(createContent(user), BorderLayout.CENTER);
    }

    /** Picks the dashboard for the user's role. */
    private static JPanel createContent(User user) {
        if (user instanceof Farmer farmer) {
            return new FarmerDashboard(farmer);
        }
        if (user instanceof Buyer buyer) {
            return new BuyerDashboard(buyer);
        }
        // Admin dashboard comes in a later step.
        JLabel body = new JLabel("<html><div style='text-align:center'>"
                + "<h2>" + user.getDashboardTitle() + "</h2>"
                + "<p>" + describe(user) + "</p>"
                + "<p style='color:#6B7280'>This screen is a placeholder - build the "
                + user.getRole().name().charAt(0) + user.getRole().name().substring(1).toLowerCase()
                + " dashboard here.</p></div></html>", SwingConstants.CENTER);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BACKGROUND);
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private static String describe(User user) {
        if (user instanceof Farmer farmer) {
            return "Farm: " + farmer.getFarmName() + " &nbsp;|&nbsp; Verification: "
                    + farmer.getVerificationStatus();
        } else if (user instanceof Buyer buyer) {
            return "Buyer type: " + buyer.getBuyerType();
        }
        return "Logged in as " + user.getEmail();
    }
}
