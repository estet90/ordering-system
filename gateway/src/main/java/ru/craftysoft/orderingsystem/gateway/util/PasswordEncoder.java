package ru.craftysoft.orderingsystem.gateway.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordEncoder {

    private static final byte[] salt = new byte[16];
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final SecretKeyFactory secretKeyFactory;

    static {
        try {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encode(@Nonnull String password) {
        Objects.requireNonNull(password);
        var spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        byte[] hash;
        try {
            hash = secretKeyFactory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return encoder.encodeToString(hash);
    }

}
