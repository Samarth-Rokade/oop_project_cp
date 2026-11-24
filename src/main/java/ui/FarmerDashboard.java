package ui;

import model.Farmer;
import service.LookupService;
import service.ProductService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

/** Farmer home: My Products, Orders, Sales, Farm Profile. */
public class FarmerDashboard extends SidebarDashboard {

    public FarmerDashboard(Farmer farmer) {
        addPage("products", "My Products", new ProductPanel(farmer, new ProductService(), new LookupService()));
        addPage("orders", "Orders", placeholder("Orders", "Incoming orders will appear here (step 3)."));
        addPage("sales", "Sales", placeholder("Sales", "Sales history and product performance (step 4)."));
        addPage("profile", "Farm Profile", profilePage(farmer));
        if (!farmer.isVerified()) {
            setBanner(verificationBanner(farmer));
        }
        showPage("products");
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

    private static JPanel profilePage(Farmer farmer) {
        String[][] rows = {
                {"Farmer", farmer.getFullName()},
                {"Email", farmer.getEmail()},
                {"Phone", farmer.getPhone() == null ? "-" : farmer.getPhone()},
                {"Farm name", farmer.getFarmName()},
                {"Farm address", farmer.getFarmAddress()},
                {"Verification", farmer.getVerificationStatus().name()},
        };
        return infoPage("Farm Profile", rows);
    }

    /** Title plus a two-column key/value card. */
    static JPanel infoPage(String titleText, String[][] rows) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel title = new JLabel(titleText);
        title.setFont(Theme.TITLE);

        JPanel grid = new JPanel(new GridLayout(rows.length, 2, 12, 10));
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
