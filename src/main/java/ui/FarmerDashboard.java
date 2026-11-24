package ui;

import model.Farmer;
import service.LookupService;
import service.ProductService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;

/**
 * Farmer home: sidebar navigation + CardLayout content area.
 */
public class FarmerDashboard extends JPanel {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final ButtonGroup navGroup = new ButtonGroup();
    private final JPanel sidebar = new JPanel();

    public FarmerDashboard(Farmer farmer) {
        super(new BorderLayout());

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SURFACE);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xE0E0E0)),
                BorderFactory.createEmptyBorder(16, 10, 16, 10)));

        addPage("products", "My Products", new ProductPanel(farmer, new ProductService(), new LookupService()));
        addPage("orders", "Orders", placeholder("Orders", "Incoming orders will appear here (step 3)."));
        addPage("sales", "Sales", placeholder("Sales", "Sales history and product performance (step 4)."));
        addPage("profile", "Farm Profile", profilePage(farmer));
        sidebar.add(Box.createVerticalGlue());

        JPanel main = new JPanel(new BorderLayout());
        if (!farmer.isVerified()) {
            main.add(verificationBanner(farmer), BorderLayout.NORTH);
        }
        main.add(content, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        // first page selected by default
        navGroup.getElements().nextElement().setSelected(true);
        cards.show(content, "products");
    }

    private void addPage(String key, String title, JPanel page) {
        content.add(page, key);

        JToggleButton button = new JToggleButton(title);
        button.putClientProperty("JButton.buttonType", "borderless");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setFocusable(false);
        button.addActionListener(e -> cards.show(content, key));
        navGroup.add(button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(4));
    }

    private static JPanel verificationBanner(Farmer farmer) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(0xFFF3E0));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xFFCC80)),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)));
        String text = switch (farmer.getVerificationStatus()) {
            case REJECTED -> "Your farm verification was rejected. Please contact the administrator.";
            default -> "Your farm is awaiting verification by the administrator. "
                    + "You can add products now - buyers will see them once you are verified.";
        };
        JLabel label = new JLabel("⚠  " + text);
        label.setForeground(new Color(0x8D4A00));
        banner.add(label, BorderLayout.CENTER);
        return banner;
    }

    private static JPanel placeholder(String title, String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BACKGROUND);
        JLabel label = new JLabel("<html><div style='text-align:center'><h2>" + title + "</h2>"
                + "<p style='color:#6B7280'>" + text + "</p></div></html>");
        panel.add(label);
        return panel;
    }

    private static JPanel profilePage(Farmer farmer) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel title = new JLabel("Farm Profile");
        title.setFont(Theme.TITLE);

        String[][] rows = {
                {"Farmer", farmer.getFullName()},
                {"Email", farmer.getEmail()},
                {"Phone", farmer.getPhone() == null ? "-" : farmer.getPhone()},
                {"Farm name", farmer.getFarmName()},
                {"Farm address", farmer.getFarmAddress()},
                {"Verification", farmer.getVerificationStatus().name()},
        };
        JPanel grid = new JPanel(new java.awt.GridLayout(rows.length, 2, 12, 10));
        grid.setBackground(Theme.SURFACE);
        grid.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E0E0)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        for (String[] row : rows) {
            JLabel key = new JLabel(row[0]);
            key.setForeground(Theme.TEXT_MUTED);
            grid.add(key);
            grid.add(new JLabel(row[1]));
        }

        JPanel north = new JPanel(new BorderLayout(0, 16));
        north.setOpaque(false);
        north.add(title, BorderLayout.NORTH);
        north.add(grid, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);
        return panel;
    }
}
