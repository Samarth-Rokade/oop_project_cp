package service;

import dao.UserDAO;
import exception.DuplicateEntryException;
import exception.ValidationException;
import model.Buyer;
import util.PasswordUtil;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Sign-up for farmers and buyers. Admin accounts are not created here.
 *
 * New farmers start as PENDING and appear to buyers only after the
 * administrator verifies them. Buyers can shop straight away.
 */
public class RegistrationService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");   // Indian mobile number
    static final int MIN_PASSWORD = 8;
    static final int MAX_PASSWORD = 72;                                    // BCrypt ignores bytes after 72

    /** Fields common to every account. */
    public record AccountDetails(String fullName, String email, String phone, char[] password, char[] confirmPassword) {
    }

    private final UserDAO userDAO;

    public RegistrationService() {
        this(new UserDAO());
    }

    public RegistrationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /** @return the new user id */
    public int registerFarmer(AccountDetails account, String farmName, String farmAddress) throws ValidationException {
        StringJoiner errors = new StringJoiner("\n");
        validateAccount(account, errors);
        String farm = trim(farmName);
        String address = trim(farmAddress);
        if (farm.isEmpty()) {
            errors.add("Farm name is required.");
        } else if (farm.length() > 120) {
            errors.add("Farm name must be at most 120 characters.");
        }
        if (address.isEmpty()) {
            errors.add("Farm address is required.");
        } else if (address.length() > 255) {
            errors.add("Farm address must be at most 255 characters.");
        }
        throwIfAny(errors);

        String email = normaliseEmail(account.email());
        checkEmailFree(email);
        try {
            return userDAO.insertFarmer(trim(account.fullName()), email, phoneOrNull(account.phone()),
                    hash(account.password()), farm, address);
        } catch (DuplicateEntryException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    /** @return the new user id */
    public int registerBuyer(AccountDetails account, Buyer.BuyerType type) throws ValidationException {
        StringJoiner errors = new StringJoiner("\n");
        validateAccount(account, errors);
        if (type == null) {
            errors.add("Choose a buyer type.");
        }
        throwIfAny(errors);

        String email = normaliseEmail(account.email());
        checkEmailFree(email);
        try {
            return userDAO.insertBuyer(trim(account.fullName()), email, phoneOrNull(account.phone()),
                    hash(account.password()), type);
        } catch (DuplicateEntryException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ rules (static so they can be unit-tested)

    static void validateAccount(AccountDetails a, StringJoiner errors) {
        String name = trim(a.fullName());
        if (name.isEmpty()) {
            errors.add("Full name is required.");
        } else if (name.length() > 100) {
            errors.add("Full name must be at most 100 characters.");
        }

        String email = normaliseEmail(a.email());
        if (email.isEmpty()) {
            errors.add("Email is required.");
        } else if (email.length() > 150 || !EMAIL.matcher(email).matches()) {
            errors.add("Enter a valid email address, e.g. name@example.com.");
        }

        String phone = trim(a.phone());
        if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) {
            errors.add("Phone must be a 10-digit mobile number starting with 6-9 (or leave it empty).");
        }

        passwordProblem(a.password()).ifPresent(errors::add);
        if (a.password() != null && !Arrays.equals(a.password(), a.confirmPassword())) {
            errors.add("Passwords do not match.");
        }
    }

    /** Password policy: 8-72 characters with at least one letter and one digit. */
    static java.util.Optional<String> passwordProblem(char[] password) {
        if (password == null || password.length == 0) {
            return java.util.Optional.of("Password is required.");
        }
        if (password.length < MIN_PASSWORD || password.length > MAX_PASSWORD) {
            return java.util.Optional.of("Password must be " + MIN_PASSWORD + " to " + MAX_PASSWORD + " characters long.");
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password) {
            letter |= Character.isLetter(c);
            digit |= Character.isDigit(c);
        }
        if (!letter || !digit) {
            return java.util.Optional.of("Password must contain at least one letter and one number.");
        }
        return java.util.Optional.empty();
    }

    private void checkEmailFree(String email) throws ValidationException {
        if (userDAO.emailExists(email)) {
            throw new ValidationException("This email is already registered. Try logging in instead.");
        }
    }

    private static String hash(char[] password) {
        return PasswordUtil.hash(new String(password));
    }

    static String normaliseEmail(String email) {
        return trim(email).toLowerCase();
    }

    private static String phoneOrNull(String phone) {
        String p = trim(phone);
        return p.isEmpty() ? null : p;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static void throwIfAny(StringJoiner errors) throws ValidationException {
        if (errors.length() > 0) {
            throw new ValidationException(errors.toString());
        }
    }
}
