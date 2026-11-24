package ui;

import model.Buyer;
import model.Cart;
import service.AddressService;
import service.CatalogService;
import service.LookupService;
import service.OrderService;

/** Buyer home: Browse Products, Cart, My Orders, Profile. */
public class BuyerDashboard extends SidebarDashboard {

    private final Cart cart = new Cart();
    private final CartPanel cartPanel;
    private final BuyerOrdersPanel ordersPanel;

    public BuyerDashboard(Buyer buyer) {
        CatalogService catalogService = new CatalogService();
        OrderService orderService = new OrderService();

        ordersPanel = new BuyerOrdersPanel(buyer, orderService);
        cartPanel = new CartPanel(buyer, cart, orderService, new AddressService(), catalogService, orderIds -> {
            ordersPanel.selectAfterReload(orderIds.get(0));
            showPage("orders");
        });

        addPage("browse", "Browse Products", new BrowsePanel(catalogService, new LookupService(), cart));
        addPage("cart", "Cart", cartPanel);
        addPage("orders", "My Orders", ordersPanel);
        addPage("profile", "Profile", new ProfilePanel("Profile", buyer, () -> new String[][]{
                {"Name", buyer.getFullName()},
                {"Email", buyer.getEmail()},
                {"Phone", buyer.getPhone() == null ? "-" : buyer.getPhone()},
                {"Buyer type", buyer.getBuyerType().name().charAt(0) + buyer.getBuyerType().name().substring(1).toLowerCase()},
        }));

        // Keep the sidebar label in sync: "Cart (3)"
        cart.addChangeListener(() -> setPageTitle("cart", cart.isEmpty() ? "Cart" : "Cart (" + cart.size() + ")"));
        showPage("browse");
    }

    @Override
    protected void onPageShown(String key) {
        switch (key) {
            case "cart" -> cartPanel.onShow();
            case "orders" -> ordersPanel.reload();
            default -> { }
        }
    }
}
