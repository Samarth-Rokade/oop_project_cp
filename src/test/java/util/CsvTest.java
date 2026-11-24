package util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvTest {

    @Test
    void escapesOnlyWhenNeeded() {
        assertEquals("Tomato", Csv.escape("Tomato"));
        assertEquals("\"Pune, Maharashtra\"", Csv.escape("Pune, Maharashtra"));
        assertEquals("\"5\"\" pot\"", Csv.escape("5\" pot"));
        assertEquals("\"line1\nline2\"", Csv.escape("line1\nline2"));
    }

    @Test
    void formatsValuesPlainly() {
        assertEquals("1234.50", Csv.format(new BigDecimal("1234.50")));
        assertEquals("2026-11-18", Csv.format(LocalDate.of(2026, 11, 18)));
        assertEquals("", Csv.format(null));
    }

    @Test
    void parsesQuotesCommasAndLineBreaks() {
        String text = "name,description,price\r\n"
                + "Tomato,\"Red, firm\",28\r\n"
                + "\"Mango \"\"Alphonso\"\"\",\"two\nlines\",480\n"
                + "\n"                                   // blank line is skipped
                + "Onion,,32";
        List<List<String>> rows = Csv.parse(text);
        assertEquals(4, rows.size());
        assertEquals(List.of("Tomato", "Red, firm", "28"), rows.get(1));
        assertEquals(List.of("Mango \"Alphonso\"", "two\nlines", "480"), rows.get(2));
        assertEquals(List.of("Onion", "", "32"), rows.get(3));
    }

    @Test
    void roundTripThroughAFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("out/report.csv");
        Csv.write(file, List.of("Farm", "Revenue"), List.of(
                List.of("Patil Organic Farms", new BigDecimal("2680.00")),
                List.of("Deshmukh, Agro", new BigDecimal("23310.00"))));

        byte[] bytes = Files.readAllBytes(file);
        assertTrue(new String(bytes, StandardCharsets.UTF_8).startsWith("﻿"), "UTF-8 BOM for Excel");

        List<List<String>> rows = Csv.read(file);
        assertEquals(List.of("Farm", "Revenue"), rows.get(0));
        assertEquals(List.of("Deshmukh, Agro", "23310.00"), rows.get(2));
    }
}
