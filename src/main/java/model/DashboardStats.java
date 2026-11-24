package model;

import java.math.BigDecimal;
import java.util.Map;

/** Numbers for the admin overview page. */
public record DashboardStats(int farmers, int buyers, int pendingVerifications, int liveListings,
                             int lowStockListings, int openOrders, BigDecimal deliveredValue,
                             BigDecimal paidValue, Map<OrderStatus, Integer> ordersByStatus) {
}
