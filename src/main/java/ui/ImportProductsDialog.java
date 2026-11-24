package ui;

import model.Farmer;
import service.ProductImportService;
import service.ProductImportService.ImportPreview;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.nio.file.Path;

/**
 * Choose a CSV file -> see what will be imported and what is wrong ->
 * import the valid rows in one go.
 */
public class ImportProductsDialog extends JDialog {

    private final Farmer farmer;
    private final ProductImportService service;
    private final Runnable onImported;

    private final JLabel fileLabel = new JLabel("No file chosen.");
    private final JLabel summary = new JLabel(" ");
    private final JTextArea details = new JTextArea(12, 60);
    private final JButton importButton = new JButton("Import");
    private ImportPreview preview;

    public ImportProductsDialog(Window owner, Farmer farmer, ProductImportService service, Runnable onImported) {
        super(owner, "Import products from CSV", ModalityType.APPLICATION_MODAL);
        this.farmer = farmer;
        this.service = service;
        this.onImported = onImported;

        JLabel help = new JLabel("<html>The first row must be a header with the columns <b>product_name, category, "
                + "unit, quantity, price</b> and optionally <b>harvest_date</b> (YYYY-MM-DD) and <b>description</b>.<br>"
                + "Categories and units must match the ones in the product form. Save a template to see an example.</html>");
        JButton choose = new JButton("Choose file...");
        choose.addActionListener(e -> chooseFile());
        JButton template = new JButton("Save template...");
        template.putClientProperty("JButton.buttonType", "borderless");
        template.addActionListener(e -> saveTemplate());

        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fileRow.add(choose);
        fileRow.add(fileLabel);
        fileRow.add(template);

        summary.setFont(summary.getFont().deriveFont(Font.BOLD));
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.add(help, BorderLayout.NORTH);
        top.add(fileRow, BorderLayout.CENTER);
        top.add(summary, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(details), BorderLayout.CENTER);

        importButton.setEnabled(false);
        importButton.setBackground(Theme.PRIMARY);
        importButton.setForeground(Color.WHITE);
        importButton.addActionListener(e -> runImport());
        JButton cancel = new JButton("Close");
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.add(cancel);
        buttons.add(importButton);

        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setMinimumSize(new Dimension(720, 480));
        pack();
        setLocationRelativeTo(owner);
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser(Exports.EXPORT_DIR.toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path file = chooser.getSelectedFile().toPath();
        fileLabel.setText(file.getFileName().toString());
        importButton.setEnabled(false);
        Async.run(this, () -> service.preview(file), this::showPreview, error -> {
            preview = null;
            summary.setText("This file cannot be imported.");
            summary.setForeground(Theme.ERROR);
            details.setText(error.getMessage());
            if (!(error instanceof exception.ValidationException)) {
                Dialogs.showError(this, error);
            }
        });
    }

    private void showPreview(ImportPreview p) {
        preview = p;
        StringBuilder text = new StringBuilder();
        if (p.hasProblems()) {
            text.append("These rows will be SKIPPED - fix them in the file and choose it again:\n\n");
            p.problems().forEach(line -> text.append("  ").append(line).append('\n'));
            text.append('\n');
        }
        if (!p.valid().isEmpty()) {
            text.append("Ready to import:\n\n");
            p.valid().forEach(r -> text.append(String.format("  Row %-4d %-24s %10s %-7s @ %10s%n",
                    r.line(), r.product().getProductName(),
                    r.product().getAvailableQuantity().stripTrailingZeros().toPlainString(),
                    r.product().getUnitCode(), "₹" + r.product().getPricePerUnit().toPlainString())));
        }
        details.setText(text.toString());
        details.setCaretPosition(0);
        summary.setForeground(p.hasProblems() ? new Color(0xE65100) : Theme.PRIMARY_DARK);
        summary.setText(p.valid().size() + " of " + p.totalRows() + " rows are ready"
                + (p.hasProblems() ? ", " + p.problems().size() + " have problems." : "."));
        importButton.setText("Import " + p.valid().size() + (p.valid().size() == 1 ? " product" : " products"));
        importButton.setEnabled(!p.valid().isEmpty());
    }

    private void runImport() {
        if (preview == null) {
            return;
        }
        if (preview.hasProblems() && !Dialogs.confirm(this, preview.problems().size()
                + " rows have problems and will be skipped. Import the other " + preview.valid().size() + "?")) {
            return;
        }
        importButton.setEnabled(false);
        Async.run(this, () -> service.importProducts(farmer, preview.valid()), count -> {
            dispose();
            onImported.run();
            Dialogs.info(getOwner(), count + " products were imported."
                    + (farmer.isVerified() ? "" : "\nBuyers will see them once your farm is verified."));
        }, error -> {
            importButton.setEnabled(true);
            Dialogs.showError(this, error);
        });
    }

    private void saveTemplate() {
        Exports.chooseSaveFile(this, "product_import_template", "csv", "CSV template").ifPresent(path ->
                Async.run(this, () -> {
                    ProductImportService.writeTemplate(path);
                    return path;
                }, saved -> Exports.savedMessage(this, saved, "Template saved")));
    }
}
