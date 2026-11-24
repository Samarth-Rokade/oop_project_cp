package app;

import com.formdev.flatlaf.FlatLightLaf;
import ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Map;

/**
 * Entry point. Run from the project root so db.properties is found.
 */
public class Main {

    public static void main(String[] args) {
        // Global look and feel: FlatLaf with rounded corners and a green accent.
        FlatLightLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#2E7D32"));
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.rowHeight", 28);

        // Swing components must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
