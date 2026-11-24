package service;

import org.junit.jupiter.api.Test;
import service.RegistrationService.AccountDetails;

import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sign-up rules - no database needed. */
class RegistrationValidationTest {

    private static String errorsFor(String name, String email, String phone, String pw, String confirm) {
        StringJoiner errors = new StringJoiner("\n");
        RegistrationService.validateAccount(new AccountDetails(name, email, phone,
                pw == null ? null : pw.toCharArray(), confirm == null ? null : confirm.toCharArray()), errors);
        return errors.toString();
    }

    @Test
    void validAccountHasNoErrors() {
        assertEquals("", errorsFor("Asha Pawar", "Asha@Example.com", "9876543210", "secret123", "secret123"));
        assertEquals("", errorsFor("Asha Pawar", "asha@example.co.in", "", "secret123", "secret123"));   // phone optional
    }

    @Test
    void passwordPolicy() {
        assertTrue(RegistrationService.passwordProblem("short1".toCharArray()).isPresent());
        assertTrue(RegistrationService.passwordProblem("onlyletters".toCharArray()).isPresent());
        assertTrue(RegistrationService.passwordProblem("12345678".toCharArray()).isPresent());
        assertTrue(RegistrationService.passwordProblem(new char[0]).isPresent());
        assertFalse(RegistrationService.passwordProblem("farmer2026".toCharArray()).isPresent());
    }

    @Test
    void passwordsMustMatch() {
        assertTrue(errorsFor("Asha", "a@b.com", "", "secret123", "secret124").contains("do not match"));
    }

    @Test
    void invalidEmailAndPhone() {
        assertTrue(errorsFor("Asha", "not-an-email", "", "secret123", "secret123").contains("valid email"));
        assertTrue(errorsFor("Asha", "a@b", "", "secret123", "secret123").contains("valid email"));
        assertTrue(errorsFor("Asha", "a@b.com", "12345", "secret123", "secret123").contains("10-digit"));
        assertTrue(errorsFor("Asha", "a@b.com", "5876543210", "secret123", "secret123").contains("10-digit"));
    }

    @Test
    void reportsEveryProblemAtOnce() {
        String errors = errorsFor("", "", "1", "x", "y");
        assertEquals(5, errors.split("\n").length, errors);   // name, email, phone, password, mismatch
    }

    @Test
    void emailIsNormalised() {
        assertEquals("asha@example.com", RegistrationService.normaliseEmail("  Asha@Example.COM "));
    }
}
