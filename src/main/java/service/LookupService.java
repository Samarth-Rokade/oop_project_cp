package service;

import dao.CategoryDAO;
import dao.UnitDAO;
import model.Category;
import model.Unit;

import java.util.List;

/**
 * Categories and units change rarely, so they are loaded once and cached.
 * Call {@link #refresh()} after the admin edits them.
 */
public class LookupService {

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final UnitDAO unitDAO = new UnitDAO();

    private List<Category> categories;
    private List<Unit> units;

    public synchronized List<Category> getCategories() {
        if (categories == null) {
            categories = List.copyOf(categoryDAO.findAll());
        }
        return categories;
    }

    public synchronized List<Unit> getUnits() {
        if (units == null) {
            units = List.copyOf(unitDAO.findAll());
        }
        return units;
    }

    public synchronized void refresh() {
        categories = null;
        units = null;
    }
}
