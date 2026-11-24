package ui;

import java.awt.Color;
import java.awt.Font;

/** Shared colours and fonts so every screen looks consistent. */
public final class Theme {

    public static final Color PRIMARY       = new Color(0x2E7D32); // farm green
    public static final Color PRIMARY_DARK  = new Color(0x1B5E20);
    public static final Color PRIMARY_LIGHT = new Color(0xE8F5E9);
    public static final Color BACKGROUND    = new Color(0xF5F7F5);
    public static final Color SURFACE       = Color.WHITE;
    public static final Color TEXT_MUTED    = new Color(0x6B7280);
    public static final Color ERROR         = new Color(0xC62828);

    public static final Font TITLE    = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    public static final Font SUBTITLE = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font HEADING  = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private Theme() {
    }
}
