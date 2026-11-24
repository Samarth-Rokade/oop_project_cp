package ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Optional;

/** Small building blocks shared by the admin pages. */
final class Pages {

    private Pages() {
    }

    static void setUp(JPanel page) {
        page.setLayout(new BorderLayout());
        page.setBackground(Theme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
    }

    /** Title + muted subtitle, with an optional row of controls underneath. */
    static JPanel header(String title, String subtitle, JComponent controls) {
        JLabel t = new JLabel(title);
        t.setFont(Theme.TITLE);
        JLabel s = new JLabel(subtitle);
        s.setForeground(Theme.TEXT_MUTED);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(t);
        titles.add(s);

        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        header.add(titles, BorderLayout.NORTH);
        if (controls != null) {
            header.add(controls, BorderLayout.CENTER);
        }
        return header;
    }

    static JPanel row(JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        for (JComponent c : components) {
            row.add(c);
        }
        return row;
    }

    /** Bottom bar: components on the left, buttons on the right. */
    static JPanel actionBar(JComponent[] left, JComponent... right) {
        JPanel l = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        l.setOpaque(false);
        for (JComponent c : left) {
            l.add(c);
        }
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        r.setOpaque(false);
        for (JComponent c : right) {
            r.add(c);
        }
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        bar.add(l, BorderLayout.WEST);
        bar.add(r, BorderLayout.EAST);
        return bar;
    }

    /** The selected row object of a BeanTableModel-backed table (sorting-safe). */
    static <T> Optional<T> selected(JTable table, BeanTableModel<T> model) {
        int view = table.getSelectedRow();
        if (view < 0) {
            return Optional.empty();
        }
        return Optional.of(model.getRow(table.convertRowIndexToModel(view)));
    }

    static void widths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    static String capitalise(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String lower = s.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
