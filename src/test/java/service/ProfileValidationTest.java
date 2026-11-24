package service;

import model.Buyer;
import org.junit.jupiter.api.Test;

import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Profile-edit rules - same as sign-up, no database needed. */
class ProfileValidationTest {

    private static String farmerErrors(String name, String phone, String farm, String address) {
        StringJoiner errors = new StringJoiner("\n");
        ProfileService.validateFarmer(name, phone, farm, address, errors);
        return errors.toString();
    }

    private static String buyerErrors(String name, String phone, Buyer.BuyerType type) {
        StringJoiner errors = new StringJoiner("\n");
        ProfileService.validateBuyer(name, phone, type, errors);
        return errors.toString();
    }

    @Test
    void validFarmerProfile() {
        assertEquals("", farmerErrors("Ramesh Patil", "9876543210", "Green Acres", "Baramati, Pune"));
        assertEquals("", farmerErrors("Ramesh Patil", "  ", "Green Acres", "Baramati, Pune"));   // phone optional
    }

    @Test
    void farmerRequiredFields() {
        String errors = farmerErrors(" ", "", "", "");
        assertTrue(errors.contains("Full name is required"));
        assertTrue(errors.contains("Farm name is required"));
        assertTrue(errors.contains("Farm address is required"));
    }

    @Test
    void farmerLengthLimits() {
        String errors = farmerErrors("Asha", "", "x".repeat(121), "y".repeat(256));
        assertTrue(errors.contains("Farm name must be at most 120"));
        assertTrue(errors.contains("Farm address must be at most 255"));
    }

    @Test
    void validBuyerProfile() {
        assertEquals("", buyerErrors("Asha Pawar", "", Buyer.BuyerType.RETAILER));
    }

    @Test
    void buyerRules() {
        String errors = buyerErrors("x".repeat(101), "12345", null);
        assertTrue(errors.contains("Full name must be at most 100"));
        assertTrue(errors.contains("Phone must be a 10-digit"));
        assertTrue(errors.contains("Choose a buyer type"));
    }
}
