package model;

import java.time.LocalDate;

/** Time window for sales reports. */
public enum ReportPeriod {
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days"),
    THIS_YEAR("This year"),
    ALL_TIME("All time");

    private final String label;

    ReportPeriod(String label) {
        this.label = label;
    }

    /** First day included, or null for "all time". */
    public LocalDate startDate(LocalDate today) {
        return switch (this) {
            case LAST_7_DAYS -> today.minusDays(6);
            case LAST_30_DAYS -> today.minusDays(29);
            case THIS_YEAR -> today.withDayOfYear(1);
            case ALL_TIME -> null;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
