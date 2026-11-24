package model;

import java.time.LocalDateTime;

/**
 * Common data and behaviour for every account (spec 9.2).
 * Abstract: a plain "User" never exists - it is always a Farmer, Buyer or Admin.
 */
public abstract class User {

    private final int userId;
    private String fullName;
    private final String email;
    private String phone;
    private final String passwordHash;
    private AccountStatus accountStatus;
    private final LocalDateTime createdAt;

    protected User(int userId, String fullName, String email, String phone,
                   String passwordHash, AccountStatus accountStatus, LocalDateTime createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
    }

    /** Each subclass reports its own role (polymorphism). */
    public abstract Role getRole();

    /** Title shown on the dashboard window for this kind of user. */
    public abstract String getDashboardTitle();

    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.fullName = fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** Only the authentication service should need this. */
    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return fullName + " <" + email + "> [" + getRole() + "]";
    }
}
