package ui;

import model.Admin;
import service.AdminService;

/** Administrator home. */
public class AdminDashboard extends SidebarDashboard {

    private final Admin admin;
    private final AdminService service = new AdminService();
    private final AdminOverviewPanel overview;
    private final AdminUsersPanel users;
    private final AdminProductsPanel products;
    private final AdminCatalogPanel catalog;
    private final AdminOrdersPanel orders;
    private final AuditLogPanel activity;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        users = new AdminUsersPanel(admin, service, this::refreshPendingCount);
        AdminUsersPanel usersPage = users;
        overview = new AdminOverviewPanel(admin, service, page -> {
            if ("users".equals(page)) {
                usersPage.showPendingFarms();
            }
            showPage(page);
        });
        products = new AdminProductsPanel(admin, service);
        catalog = new AdminCatalogPanel(admin, service);
        orders = new AdminOrdersPanel(admin, service);
        activity = new AuditLogPanel(admin, service);

        addPage("overview", "Overview", overview);
        addPage("users", "Users", users);
        addPage("products", "Products", products);
        addPage("catalog", "Categories & Units", catalog);
        addPage("orders", "Orders & Payments", orders);
        addPage("activity", "Activity log", activity);
        showPage("overview");
        refreshPendingCount();
    }

    @Override
    protected void onPageShown(String key) {
        switch (key) {
            case "overview" -> overview.reload();
            case "users" -> users.reload();
            case "products" -> products.reload();
            case "catalog" -> catalog.reload();
            case "orders" -> orders.reload();
            case "activity" -> activity.reload();
            default -> { }
        }
    }

    /** Sidebar shows "Users (2 to verify)". */
    private void refreshPendingCount() {
        Async.run(this, () -> service.getStats(admin).pendingVerifications(),
                n -> setPageTitle("users", n == 0 ? "Users" : "Users (" + n + " to verify)"),
                error -> { });
    }
}
