package model;

import java.time.LocalDateTime;

/**
 * One row of the admin's user list. Never contains the password hash.
 * verificationStatus / farmName are set for farmers, buyerType for buyers.
 */
public record UserSummary(int userId, String fullName, String email, String phone, Role role,
                          AccountStatus accountStatus, Farmer.VerificationStatus verificationStatus,
                          String farmName, String buyerType, LocalDateTime createdAt) {

    public boolean isFarmer() {
        return role == Role.FARMER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
