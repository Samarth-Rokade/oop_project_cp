package util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV support (RFC 4180): fields containing commas, quotes or
 * line breaks are wrapped in double quotes, and quotes are doubled.
 *
 * Files are written as UTF-8 with a byte-order mark so Excel shows the
 * rupee sign and Marathi/Hindi names correctly.
 */
public final class Csv {

    private static final char BOM = '﻿';
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Csv() {
    }

    // ------------------------------------------------------------------ writing

    /** Writes a header row and data rows. Uses try-with-resources so the file is always closed. */
    public static void write(Path file, List<String> header, List<List<Object>> rows) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write(BOM);
            out.write(line(new ArrayList<>(header)));
            out.write("\r\n");
            for (List<Object> row : rows) {
                out.write(line(row));
                out.write("\r\n");
            }
        }
    }

    static String line(List<?> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(format(values.get(i))));
        }
        return sb.toString();
    }

    /** Plain, machine-readable values: 1234.50 not "₹1,234.50". */
    static String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal d) {
            return d.toPlainString();
        }
        if (value instanceof LocalDateTime t) {
            return DATE_TIME.format(t);
        }
        if (value instanceof LocalDate d) {
            return d.toString();
        }
        return value.toString();
    }

    static String escape(String field) {
        boolean needsQuotes = field.contains(",") || field.contains("\"")
                || field.contains("\n") || field.contains("\r")
                || (!field.isEmpty() && (Character.isWhitespace(field.charAt(0))
                        || Character.isWhitespace(field.charAt(field.length() - 1))));
        if (!needsQuotes) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }

    // ------------------------------------------------------------------ reading

    /** Reads every record of a CSV file (the header row is included). */
    public static List<List<String>> read(Path file) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    /**
     * Character-by-character parser, so quoted fields may contain commas,
     * doubled quotes and even line breaks. Blank lines are skipped.
     */
    public static List<List<String>> parse(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false;

        int start = !text.isEmpty() && text.charAt(0) == BOM ? 1 : 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"' && field.length() == 0) {
                inQuotes = true;
                fieldStarted = true;
            } else if (c == ',') {
                record.add(field.toString());
                field.setLength(0);
                fieldStarted = true;
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                if (fieldStarted || field.length() > 0 || !record.isEmpty()) {
                    record.add(field.toString());
                    records.add(record);
                }
                record = new ArrayList<>();
                field.setLength(0);
                fieldStarted = false;
            } else {
                field.append(c);
                fieldStarted = true;
            }
        }
        if (fieldStarted || field.length() > 0 || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
