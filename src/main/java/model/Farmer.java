package model;

import java.time.LocalDateTime;

public class Farmer extends User {

    /** Values match the CHECK constraint on farmer_profile.verification_status. */
    public enum VerificationStatus { PENDING, VERIFIED, REJECTED }

    private String farmName;
    private String farmAddress;
    private VerificationStatus verificationStatus;

    public Farmer(int userId, String fullName, String email, String phone, String passwordHash,
                  AccountStatus accountStatus, LocalDateTime createdAt,
                  String farmName, String farmAddress, VerificationStatus verificationStatus) {
        super(userId, fullName, email, phone, passwordHash, accountStatus, createdAt);
        this.farmName = farmName;
        this.farmAddress = farmAddress;
        this.verificationStatus = verificationStatus;
    }

    @Override
    public Role getRole() {
        return Role.FARMER;
    }

    @Override
    public String getDashboardTitle() {
        return "Farmer Dashboard - " + farmName;
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getFarmAddress() {
        return farmAddress;
    }

    public void setFarmAddress(String farmAddress) {
        this.farmAddress = farmAddress;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
