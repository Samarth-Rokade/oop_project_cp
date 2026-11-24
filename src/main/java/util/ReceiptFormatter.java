package util;

import model.Order;
import model.OrderItem;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/** Plain-text order receipt (spec 9.12: "saving an application report or receipt"). */
public final class ReceiptFormatter {

    private static final int WIDTH = 64;
    private static final String RULE = "=".repeat(WIDTH);
    private static final String LINE = "-".repeat(WIDTH);

    private ReceiptFormatter() {
    }

    public static String format(Order o, List<OrderItem> items, LocalDateTime generatedAt) {
        StringBuilder r = new StringBuilder();
        r.append(RULE).append('\n')
         .append(center("FARMER-TO-BUYER MARKETPLACE")).append('\n')
         .append(center("ORDER RECEIPT")).append('\n')
         .append(RULE).append('\n');
        field(r, "Order", "#" + o.getOrderId());
        field(r, "Placed on", Formats.dateTime(o.getOrderDate()));
        field(r, "Buyer", o.getBuyerName());
        field(r, "Farm", o.getFarmNames());
        field(r, "Deliver to", o.getDeliverTo());
        field(r, "Status", o.getStatus().getLabel());
        r.append(LINE).append('\n');
        r.append(String.format("%-26s %12s %11s %12s%n", "Item", "Quantity", "Price", "Amount"));
        r.append(LINE).append('\n');

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem i : items) {
            r.append(String.format("%-26s %12s %11s %12s%n",
                    cut(i.getProductName(), 26),
                    Formats.quantity(i.getQuantity()) + " " + i.getUnitCode(),
                    Formats.money(i.getUnitPriceAtOrder()),
                    Formats.money(i.getLineTotal())));
            total = total.add(i.getLineTotal());
        }
        r.append(LINE).append('\n');
        r.append(String.format("%-51s %12s%n", "TOTAL", Formats.money(total)));
        r.append(LINE).append('\n');
        if (o.getPaymentMethod() != null) {
            field(r, "Payment", o.getPaymentMethod() + " - " + o.getPaymentStatus());
        }
        if (o.getDeliveryPartner() != null) {
            field(r, "Shipped via", o.getDeliveryPartner()
                    + (o.getTrackingReference() == null ? "" : " (" + o.getTrackingReference() + ")"));
        }
        r.append(RULE).append('\n')
         .append("Payments in this course project are simulated; no money was charged.\n")
         .append("Generated ").append(Formats.dateTime(generatedAt)).append('\n');
        return r.toString();
    }

    public static void save(Path file, String receipt) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, receipt, StandardCharsets.UTF_8);
    }

    private static void field(StringBuilder r, String label, String value) {
        r.append(String.format("%-13s %s%n", label + ":", value == null ? "-" : value));
    }

    private static String center(String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(pad) + text;
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
