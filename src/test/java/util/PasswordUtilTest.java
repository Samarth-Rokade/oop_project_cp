package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashIsNotPlainTextAndVerifies() {
        String hash = PasswordUtil.hash("secret123");
        assertNotEquals("secret123", hash);
        assertTrue(PasswordUtil.matches("secret123", hash));
        assertFalse(PasswordUtil.matches("wrong", hash));
    }

    @Test
    void sampleDataHashMatchesDemoPassword() {
        // Same hash as database/03_sample_data.sql
        String sampleHash = "$2a$10$oN/o.Qfl0fFPRr2Wl560k.VSw.0J6tjFMx6JW1yK8ymPEi3.SFvXu";
        assertTrue(PasswordUtil.matches("password123", sampleHash));
    }

    @Test
    void rejectsNullsAndNonBcryptValues() {
        assertFalse(PasswordUtil.matches(null, "$2a$10$abc"));
        assertFalse(PasswordUtil.matches("x", null));
        assertFalse(PasswordUtil.matches("password123", "password123"));
    }
}
