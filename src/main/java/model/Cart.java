package model;

import exception.ValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Temporary, in-memory shopping cart (spec 9.1). It is not stored in the
 * database; only a placed order is.
 *
 * Uses a LinkedHashMap keyed by product id: adding the same product twice
 * increases its quantity, and items keep the order they were added in.
 */
public class Cart {

    private final Map<Integer, CartItem> items = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public void add(Product product, BigDecimal quantity) throws ValidationException {
        CartItem existing = items.get(product.getProductId());
        BigDecimal newQuantity = existing == null ? quantity : existing.getQuantity().add(quantity);
        checkQuantity(product, newQuantity);
        if (existing == null) {
            items.put(product.getProductId(), new CartItem(product, quantity));
        } else {
            existing.setQuantity(newQuantity);
        }
        fireChanged();
    }

    public void setQuantity(int productId, BigDecimal quantity) throws ValidationException {
        CartItem item = items.get(productId);
        if (item == null) {
            throw new ValidationException("That product is no longer in your cart.");
        }
        checkQuantity(item.getProduct(), quantity);
        item.setQuantity(quantity);
        fireChanged();
    }

    public void remove(int productId) {
        if (items.remove(productId) != null) {
            fireChanged();
        }
    }

    public void clear() {
        items.clear();
        fireChanged();
    }

    public List<CartItem> getItems() {
        return List.copyOf(items.values());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public BigDecimal getEstimatedTotal() {
        return items.values().stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Items grouped by farm - each group becomes one order. */
    public Map<Integer, List<CartItem>> groupByFarmer() {
        return items.values().stream()
                .collect(Collectors.groupingBy(i -> i.getProduct().getFarmerId(),
                        LinkedHashMap::new, Collectors.toList()));
    }

    /** Replaces products with fresh copies (new price/stock) after a reload. */
    public void refreshProducts(Function<Integer, Product> latest) {
        List<Integer> gone = new ArrayList<>();
        items.replaceAll((id, item) -> {
            Product fresh = latest.apply(id);
            if (fresh == null) {
                gone.add(id);
                return item;
            }
            return new CartItem(fresh, item.getQuantity());
        });
        gone.forEach(items::remove);
        fireChanged();
    }

    /** Lets the UI update the cart badge whenever the cart changes. */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        listeners.forEach(Runnable::run);
    }

    private static void checkQuantity(Product product, BigDecimal quantity) throws ValidationException {
        if (quantity == null || quantity.signum() <= 0) {
            throw new ValidationException("Quantity must be greater than zero.");
        }
        if (quantity.stripTrailingZeros().scale() > 2) {
            throw new ValidationException("Quantity can have at most 2 decimal places.");
        }
        if (quantity.compareTo(product.getAvailableQuantity()) > 0) {
            throw new ValidationException("Only " + product.getAvailableQuantity().stripTrailingZeros().toPlainString()
                    + " " + product.getUnitCode() + " of " + product.getProductName() + " available.");
        }
    }
}
