package ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for every role's dashboard: a sidebar of pages on the left
 * and a CardLayout showing the selected page. Subclasses only add pages.
 */
public abstract class SidebarDashboard extends JPanel {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel sidebar = new JPanel();
    private final JPanel main = new JPanel(new BorderLayout());
    private final ButtonGroup navGroup = new ButtonGroup();
    private final Map<String, JToggleButton> navButtons = new LinkedHashMap<>();

    protected SidebarDashboard() {
        super(new BorderLayout());
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SURFACE);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xE0E0E0)),
                BorderFactory.createEmptyBorder(16, 10, 16, 10)));
        main.add(content, BorderLayout.CENTER);
        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);
    }

    protected void addPage(String key, String title, JComponent page) {
        content.add(page, key);

        JToggleButton button = new JToggleButton(title);
        button.putClientProperty("JButton.buttonType", "borderless");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setFocusable(false);
        button.addActionListener(e -> showPage(key));
        navGroup.add(button);
        navButtons.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(4));

        if (navButtons.size() == 1) {
            button.setSelected(true);
        }
    }

    /** Shows a page and selects its sidebar button. */
    public void showPage(String key) {
        JToggleButton button = navButtons.get(key);
        if (button == null) {
            return;
        }
        button.setSelected(true);
        cards.show(content, key);
        onPageShown(key);
    }

    /** Changes a sidebar label, e.g. "Cart (3)". */
    protected void setPageTitle(String key, String title) {
        JToggleButton button = navButtons.get(key);
        if (button != null) {
            button.setText(title);
        }
    }

    /** Optional banner above the page content. */
    protected void setBanner(JComponent banner) {
        main.add(banner, BorderLayout.NORTH);
    }

    /** Hook: called after a page becomes visible (e.g. to refresh its data). */
    protected void onPageShown(String key) {
    }

    /** Simple "coming soon" page. */
    protected static JPanel placeholder(String title, String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BACKGROUND);
        panel.add(new JLabel("<html><div style='text-align:center'><h2>" + title + "</h2>"
                + "<p style='color:#6B7280'>" + text + "</p></div></html>"));
        return panel;
    }
}
