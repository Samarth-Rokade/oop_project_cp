package ui;

import util.Csv;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

/** "Save as" dialogs for CSV exports, receipts and similar files. */
public final class Exports {

    /** Default folder for saved files (git-ignored). */
    static final Path EXPORT_DIR = Path.of("exports");

    private Exports() {
    }

    /**
     * Asks where to save. Suggests exports/&lt;name&gt;_&lt;today&gt;.&lt;ext&gt; and
     * confirms before replacing an existing file.
     */
    public static Optional<Path> chooseSaveFile(Component parent, String baseName, String extension, String description) {
        File dir = EXPORT_DIR.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setDialogTitle("Save " + description);
        chooser.setFileFilter(new FileNameExtensionFilter(description + " (*." + extension + ")", extension));
        chooser.setSelectedFile(new File(dir, baseName + "_" + LocalDate.now() + "." + extension));
        while (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith("." + extension)) {
                file = new File(file.getParentFile(), file.getName() + "." + extension);
            }
            if (!file.exists() || Dialogs.confirm(parent, file.getName() + " already exists. Replace it?")) {
                return Optional.of(file.toPath());
            }
        }
        return Optional.empty();
    }

    /** Exports whatever a BeanTableModel currently shows. */
    public static void exportTable(Component parent, BeanTableModel<?> model, String baseName) {
        if (model.getRowCount() == 0) {
            Dialogs.info(parent, "There is nothing to export.");
            return;
        }
        chooseSaveFile(parent, baseName, "csv", "CSV file").ifPresent(path ->
                Async.run(parent, () -> {
                    Csv.write(path, model.headers(), model.rawRows());      // file I/O off the EDT
                    return path;
                }, saved -> savedMessage(parent, saved, model.getRowCount() + " rows exported")));
    }

    /** "Saved to ..." with a button that opens the folder. */
    static void savedMessage(Component parent, Path saved, String what) {
        Object[] options = {"Open folder", "OK"};
        int choice = JOptionPane.showOptionDialog(parent, what + " to\n" + saved.toAbsolutePath(), "Saved",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[1]);
        if (choice == 0 && Desktop.isDesktopSupported()) {
            try {
                Path folder = saved.toAbsolutePath().getParent();
                if (folder != null && Files.isDirectory(folder)) {
                    Desktop.getDesktop().open(folder.toFile());
                }
            } catch (Exception e) {
                Dialogs.showError(parent, e);
            }
        }
    }
}
