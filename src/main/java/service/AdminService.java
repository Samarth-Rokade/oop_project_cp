package service;

import config.DatabaseConfig;
import dao.AdminDAO;
import dao.AuditDAO;
import dao.CategoryDAO;
import dao.OrderDAO;
import dao.UnitDAO;
import exception.DataAccessException;
import exception.DuplicateEntryException;
import exception.ValidationException;
import model.AccountStatus;
import model.Admin;
import model.AuditEntry;
import model.Category;
import model.DashboardStats;
import model.Farmer;
import model.ListingStatus;
import model.Order;
import model.OrderItem;
import model.Payment;
import model.Product;
import model.Unit;
import model.UserSummary;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Everything the administrator can do (spec 5.3). Every method takes the
 * acting Admin, so only an admin object can call it, and every change is
 * written to the audit log in the same transaction.
 */
public class AdminService {

    private final AdminDAO adminDAO = new AdminDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final UnitDAO unitDAO = new UnitDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    // ------------------------------------------------------------------ overview

    public DashboardStats getStats(Admin admin) {
        return adminDAO.loadStats();
    }

    // ------------------------------------------------------------------ users

    public List<UserSummary> getUsers(Admin admin) {
        return adminDAO.findAllUsers();
    }

    /** Verifies a farm: verification VERIFIED and a pending account becomes ACTIVE. */
    public void verifyFarmer(Admin admin, int farmerId) throws ValidationException {
        inTransaction(con -> {
            UserSummary u = lockFarmer(con, farmerId);
            if (u.verificationStatus() == Farmer.VerificationStatus.VERIFIED) {
                throw new ValidationException(u.fullName() + " is already verified.");
            }
            adminDAO.setVerification(con, farmerId, Farmer.VerificationStatus.VERIFIED);
            if (u.accountStatus() == AccountStatus.PENDING) {
                adminDAO.setAccountStatus(con, farmerId, AccountStatus.ACTIVE);
            }
            auditDAO.log(con, admin.getUserId(), "VERIFY_FARMER", "FARMER_PROFILE", farmerId,
                    "Verified " + u.farmName());
        });
    }

    public void rejectFarmer(Admin admin, int farmerId, String reason) throws ValidationException {
        String why = requireText(reason, "reason", 200);
        inTransaction(con -> {
            UserSummary u = lockFarmer(con, farmerId);
            if (u.verificationStatus() == Farmer.VerificationStatus.REJECTED) {
                throw new ValidationException(u.fullName() + " is already rejected.");
            }
            adminDAO.setVerification(con, farmerId, Farmer.VerificationStatus.REJECTED);
            auditDAO.log(con, admin.getUserId(), "REJECT_FARMER", "FARMER_PROFILE", farmerId,
                    u.farmName() + ": " + why);
        });
    }

    /** Suspended users cannot log in; a suspended farmer's products disappear from the catalogue. */
    public void suspendUser(Admin admin, int userId, String reason) throws ValidationException {
        String why = requireText(reason, "reason", 200);
        inTransaction(con -> {
            UserSummary u = lockEditableUser(con, admin, userId);
            if (u.accountStatus() == AccountStatus.SUSPENDED) {
                throw new ValidationException(u.fullName() + " is already suspended.");
            }
            adminDAO.setAccountStatus(con, userId, AccountStatus.SUSPENDED);
            auditDAO.log(con, admin.getUserId(), "SUSPEND_USER", "USER", userId, u.email() + ": " + why);
        });
    }

    public void reactivateUser(Admin admin, int userId) throws ValidationException {
        inTransaction(con -> {
            UserSummary u = lockEditableUser(con, admin, userId);
            if (u.accountStatus() != AccountStatus.SUSPENDED) {
                throw new ValidationException(u.fullName() + " is not suspended.");
            }
            // A farmer who was never verified goes back to PENDING, not ACTIVE
            AccountStatus next = u.isFarmer() && u.verificationStatus() != Farmer.VerificationStatus.VERIFIED
                    ? AccountStatus.PENDING : AccountStatus.ACTIVE;
            adminDAO.setAccountStatus(con, userId, next);
            auditDAO.log(con, admin.getUserId(), "REACTIVATE_USER", "USER", userId, u.email());
        });
    }

    private UserSummary lockFarmer(Connection con, int farmerId) throws SQLException, ValidationException {
        UserSummary u = adminDAO.lockUser(con, farmerId);
        if (u == null || !u.isFarmer()) {
            throw new ValidationException("Farmer not found.");
        }
        return u;
    }

    private UserSummary lockEditableUser(Connection con, Admin admin, int userId)
            throws SQLException, ValidationException {
        UserSummary u = adminDAO.lockUser(con, userId);
        if (u == null) {
            throw new ValidationException("User not found.");
        }
        if (u.isAdmin()) {
            throw new ValidationException(u.userId() == admin.getUserId()
                    ? "You cannot change your own account." : "Administrator accounts cannot be changed here.");
        }
        return u;
    }

    // ------------------------------------------------------------------ product moderation

    public List<Product> getProducts(Admin admin) {
        return adminDAO.findAllProducts();
    }

    /** Takes a listing off the marketplace. The farmer sees "Removed by admin" and cannot re-activate it. */
    public void removeListing(Admin admin, int productId, String reason) throws ValidationException {
        String why = requireText(reason, "reason", 200);
        inTransaction(con -> {
            ListingStatus current = adminDAO.lockProductStatus(con, productId);
            if (current == null) {
                throw new ValidationException("Product not found.");
            }
            if (current == ListingStatus.REMOVED) {
                throw new ValidationException("This listing is already removed.");
            }
            adminDAO.setListingStatus(con, productId, ListingStatus.REMOVED);
            auditDAO.log(con, admin.getUserId(), "REMOVE_LISTING", "PRODUCT", productId, why);
        });
    }

    /** Restores a removed listing as hidden, so the farmer decides when buyers see it again. */
    public void restoreListing(Admin admin, int productId) throws ValidationException {
        inTransaction(con -> {
            ListingStatus current = adminDAO.lockProductStatus(con, productId);
            if (current != ListingStatus.REMOVED) {
                throw new ValidationException("Only removed listings can be restored.");
            }
            adminDAO.setListingStatus(con, productId, ListingStatus.INACTIVE);
            auditDAO.log(con, admin.getUserId(), "RESTORE_LISTING", "PRODUCT", productId, "Restored as hidden");
        });
    }

    // ------------------------------------------------------------------ categories and units

    public List<Category> getCategories(Admin admin) {
        return categoryDAO.findAll();
    }

    public Map<Integer, Integer> getCategoryUsage(Admin admin) {
        return categoryDAO.countProducts();
    }

    /** Adds (categoryId == 0) or edits a category. */
    public void saveCategory(Admin admin, int categoryId, String name, String description) throws ValidationException {
        String n = requireText(name, "category name", 60);
        String d = description == null || description.isBlank() ? null : description.trim();
        if (d != null && d.length() > 255) {
            throw new ValidationException("Description must be at most 255 characters.");
        }
        try {
            if (categoryId == 0) {
                categoryDAO.insert(n, d);
            } else {
                categoryDAO.update(categoryId, n, d);
            }
        } catch (DuplicateEntryException e) {
            throw new ValidationException(e.getMessage());
        }
        logStandalone(admin, categoryId == 0 ? "ADD_CATEGORY" : "EDIT_CATEGORY", "CATEGORY",
                categoryId == 0 ? null : categoryId, n);
    }

    public void deleteCategory(Admin admin, Category category) throws ValidationException {
        if (!categoryDAO.deleteIfUnused(category.getCategoryId())) {
            throw new ValidationException("\"" + category.getCategoryName()
                    + "\" still has products, so it cannot be deleted.");
        }
        logStandalone(admin, "DELETE_CATEGORY", "CATEGORY", category.getCategoryId(), category.getCategoryName());
    }

    public List<Unit> getUnits(Admin admin) {
        return unitDAO.findAll();
    }

    public Map<String, Integer> getUnitUsage(Admin admin) {
        return unitDAO.countProducts();
    }

    public void addUnit(Admin admin, String code, String name) throws ValidationException {
        String c = code == null ? "" : code.trim().toUpperCase();
        if (!c.matches("[A-Z]{1,10}")) {
            throw new ValidationException("Unit code must be 1-10 letters, e.g. KG or BOX.");
        }
        String n = requireText(name, "unit name", 40);
        try {
            unitDAO.insert(c, n);
        } catch (DuplicateEntryException e) {
            throw new ValidationException(e.getMessage());
        }
        logStandalone(admin, "ADD_UNIT", "UNIT", null, c + " - " + n);
    }

    public void renameUnit(Admin admin, String code, String name) throws ValidationException {
        String n = requireText(name, "unit name", 40);
        unitDAO.rename(code, n);
        logStandalone(admin, "EDIT_UNIT", "UNIT", null, code + " - " + n);
    }

    public void deleteUnit(Admin admin, Unit unit) throws ValidationException {
        if (!unitDAO.deleteIfUnused(unit.getUnitCode())) {
            throw new ValidationException("Products are still sold in " + unit.getUnitCode() + ", so it cannot be deleted.");
        }
        logStandalone(admin, "DELETE_UNIT", "UNIT", null, unit.getUnitCode());
    }

    // ------------------------------------------------------------------ orders, payments, audit

    public List<Order> getOrders(Admin admin) {
        return orderDAO.findAll();
    }

    public List<OrderItem> getOrderItems(Admin admin, int orderId) {
        return orderDAO.findItems(orderId);
    }

    public List<Payment> getPayments(Admin admin) {
        return adminDAO.findAllPayments();
    }

    public List<AuditEntry> getActivity(Admin admin, String action, String text) {
        return adminDAO.findAudit(action, text, 500);
    }

    public List<String> getActivityTypes(Admin admin) {
        return adminDAO.findAuditActions();
    }

    // ------------------------------------------------------------------ helpers

    @FunctionalInterface
    private interface TxWork {
        void run(Connection con) throws SQLException, ValidationException;
    }

    private void inTransaction(TxWork work) throws ValidationException {
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                work.run(con);
                con.commit();
            } catch (ValidationException | SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("The change could not be saved", e);
        }
    }

    private void logStandalone(Admin admin, String action, String entity, Integer id, String details) {
        try (Connection con = DatabaseConfig.getConnection()) {
            auditDAO.log(con, admin.getUserId(), action, entity, id, details);
        } catch (SQLException e) {
            throw new DataAccessException("Could not write the activity log", e);
        }
    }

    private static String requireText(String value, String field, int max) throws ValidationException {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            throw new ValidationException("Please enter a " + field + ".");
        }
        if (v.length() > max) {
            throw new ValidationException("The " + field + " must be at most " + max + " characters.");
        }
        return v;
    }
}
