package com.novabank.transfer.security;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Turns account numbers into opaque tokens that partner apps can store instead of the real number.
 */
@Service
public class AccountTokenService {

       private static final byte[] TOKEN_KEY = loadKey();

    private static byte[] loadKey() {
        String key = System.getenv("TOKEN_KEY");
        if (key != null && !key.isBlank()) {
            return key.getBytes(StandardCharsets.UTF_8);   // must be 16, 24 or 32 characters
        }
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        return random;
    }

    public String tokenize(String accountNumber) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(TOKEN_KEY, "AES"));
            byte[] encrypted = cipher.doFinal(accountNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not create token", e);
        }
    }
}
