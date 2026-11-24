package service;

import dao.AddressDAO;
import exception.ValidationException;
import model.Address;
import model.User;

import java.util.List;
import java.util.StringJoiner;

public class AddressService {

    private final AddressDAO addressDAO;

    public AddressService() {
        this(new AddressDAO());
    }

    public AddressService(AddressDAO addressDAO) {
        this.addressDAO = addressDAO;
    }

    public List<Address> getAddresses(User user) {
        return addressDAO.findByUser(user.getUserId());
    }

    public Address addAddress(User user, Address address) throws ValidationException {
        address.setUserId(user.getUserId());
        address.setAddressLine(trim(address.getAddressLine()));
        address.setCity(trim(address.getCity()));
        address.setDistrict(trim(address.getDistrict()));
        address.setState(trim(address.getState()));
        address.setPostalCode(trim(address.getPostalCode()));
        validate(address);

        // The first address is always the default
        if (getAddresses(user).isEmpty()) {
            address.setDefaultAddress(true);
        }
        address.setAddressId(addressDAO.insert(address));
        return address;
    }

    public static void validate(Address a) throws ValidationException {
        StringJoiner errors = new StringJoiner("\n");
        required(errors, a.getAddressLine(), "Address", 255);
        required(errors, a.getCity(), "City", 80);
        required(errors, a.getDistrict(), "District", 80);
        required(errors, a.getState(), "State", 80);
        if (a.getPostalCode() == null || !a.getPostalCode().matches("[1-9][0-9]{5}")) {
            errors.add("PIN code must be 6 digits and cannot start with 0.");
        }
        if (errors.length() > 0) {
            throw new ValidationException(errors.toString());
        }
    }

    private static void required(StringJoiner errors, String value, String field, int max) {
        if (value == null || value.isEmpty()) {
            errors.add(field + " is required.");
        } else if (value.length() > max) {
            errors.add(field + " must be at most " + max + " characters.");
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
