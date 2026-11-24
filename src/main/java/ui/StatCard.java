package ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Summary tile: small title and a large coloured value. Optionally clickable. */
public class StatCard extends JPanel {

    private final JLabel valueLabel = new JLabel("-");

    public StatCard(String title, Color accent) {
        super(new BorderLayout(0, 4));
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 1, 1, new Color(0xE0E0E0)),
                        BorderFactory.createEmptyBorder(12, 14, 12, 14))));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Theme.TEXT_MUTED);
        valueLabel.setFont(Theme.TITLE.deriveFont(26f));
        valueLabel.setForeground(accent);
        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    /** Makes the whole card a link. */
    public StatCard onClick(Runnable action, String tooltip) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(tooltip);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return this;
    }
}
