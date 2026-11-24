package model;

public class Category {

    private final int categoryId;
    private final String categoryName;
    private final String description;

    public Category(int categoryId, String categoryName, String description) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getDescription() {
        return description;
    }

    /** JComboBox shows toString(). */
    @Override
    public String toString() {
        return categoryName;
    }
}
