package service;

import dao.UserDAO;
import exception.ValidationException;
import model.Buyer;
import model.Farmer;

import java.util.StringJoiner;

/**
 * Lets farmers and buyers edit their own profile. Uses the same rules as
 * sign-up. Email (the login id), role and verification status stay fixed.
 *
 * After a successful save the given model object is updated too, so the
 * logged-in session and open screens show the new values.
 */
public class ProfileService {

    private final UserDAO userDAO;

    public ProfileService() {
        this(new UserDAO());
    }

    public ProfileService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void updateFarmer(Farmer farmer, String fullName, String phone,
                             String farmName, String farmAddress) throws ValidationException {
        StringJoiner errors = new StringJoiner("\n");
        validateFarmer(fullName, phone, farmName, farmAddress, errors);
        RegistrationService.throwIfAny(errors);

        String name = RegistrationService.trim(fullName);
        String phoneValue = RegistrationService.phoneOrNull(phone);
        String farm = RegistrationService.trim(farmName);
        String address = RegistrationService.trim(farmAddress);
        userDAO.updateFarmerProfile(farmer.getUserId(), name, phoneValue, farm, address);

        farmer.setFullName(name);
        farmer.setPhone(phoneValue);
        farmer.setFarmName(farm);
        farmer.setFarmAddress(address);
    }

    public void updateBuyer(Buyer buyer, String fullName, String phone, Buyer.BuyerType type)
            throws ValidationException {
        StringJoiner errors = new StringJoiner("\n");
        validateBuyer(fullName, phone, type, errors);
        RegistrationService.throwIfAny(errors);

        String name = RegistrationService.trim(fullName);
        String phoneValue = RegistrationService.phoneOrNull(phone);
        userDAO.updateBuyerProfile(buyer.getUserId(), name, phoneValue, type);

        buyer.setFullName(name);
        buyer.setPhone(phoneValue);
        buyer.setBuyerType(type);
    }

    // ------------------------------------------------------------------ rules (static so they can be unit-tested)

    static void validateFarmer(String fullName, String phone, String farmName, String farmAddress,
                               StringJoiner errors) {
        RegistrationService.validateName(fullName, errors);
        RegistrationService.validatePhone(phone, errors);
        RegistrationService.validateFarm(farmName, farmAddress, errors);
    }

    static void validateBuyer(String fullName, String phone, Buyer.BuyerType type, StringJoiner errors) {
        RegistrationService.validateName(fullName, errors);
        RegistrationService.validatePhone(phone, errors);
        if (type == null) {
            errors.add("Choose a buyer type.");
        }
    }
}
