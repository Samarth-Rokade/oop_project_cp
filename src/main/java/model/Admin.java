package model;

import java.time.LocalDateTime;

public class Admin extends User {

    public Admin(int userId, String fullName, String email, String phone, String passwordHash,
                 AccountStatus accountStatus, LocalDateTime createdAt) {
        super(userId, fullName, email, phone, passwordHash, accountStatus, createdAt);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public String getDashboardTitle() {
        return "Administrator Dashboard";
    }
}
