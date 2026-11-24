package model;

import java.time.LocalDateTime;

public class Buyer extends User {

    /** Values match the CHECK constraint on buyer_profile.buyer_type. */
    public enum BuyerType { INDIVIDUAL, RETAILER, WHOLESALER, RESTAURANT }

    private BuyerType buyerType;

    public Buyer(int userId, String fullName, String email, String phone, String passwordHash,
                 AccountStatus accountStatus, LocalDateTime createdAt, BuyerType buyerType) {
        super(userId, fullName, email, phone, passwordHash, accountStatus, createdAt);
        this.buyerType = buyerType;
    }

    @Override
    public Role getRole() {
        return Role.BUYER;
    }

    @Override
    public String getDashboardTitle() {
        return "Buyer Dashboard - " + getFullName();
    }

    public BuyerType getBuyerType() {
        return buyerType;
    }

    public void setBuyerType(BuyerType buyerType) {
        this.buyerType = buyerType;
    }
}
