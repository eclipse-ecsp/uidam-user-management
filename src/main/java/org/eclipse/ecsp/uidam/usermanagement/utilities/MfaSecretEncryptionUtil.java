/*
 * Copyright (c) 2023 - 2024 Harman International
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utility class for AES-256-GCM encryption/decryption of TOTP MFA secrets.
 *
 * <p>Uses PBKDF2WithHmacSHA256 for key derivation from a configurable per-tenant key and salt.
 * The output is a Base64-encoded blob containing the 12-byte IV followed by the GCM ciphertext.
 *
 * <p>The same key and salt must be configured in both the user-management service (which
 * encrypts on write and decrypts for the enrolment response) and the authorization server
 * (which decrypts the secret received over REST before TOTP validation).
 *
 * <p>Algorithm: AES/GCM/NoPadding, 256-bit key, 128-bit auth tag, 12-byte random IV.
 */
public final class MfaSecretEncryptionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MfaSecretEncryptionUtil.class);

    private static final String ALGORITHM               = "AES";
    private static final String AES_TRANSFORMATION_MODE = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGO     = "PBKDF2WithHmacSHA256";
    private static final int    TAG_LENGTH_BIT          = 128;
    private static final int    IV_LENGTH_BYTE          = 12;
    private static final int    AES_KEY_BIT             = 256;
    private static final int    ITERATION_COUNT         = 65536;
    private static final SecureRandom SECURE_RANDOM     = new SecureRandom();

    private MfaSecretEncryptionUtil() {
        // utility class
    }

    /**
     * Encrypt a plain-text TOTP secret using AES-256-GCM.
     *
     * @param plainSecret the Base32-encoded TOTP secret to encrypt
     * @param encryptionKey  the per-tenant encryption key (PBKDF2 password)
     * @param encryptionSalt the per-tenant salt for PBKDF2 key derivation
     * @return Base64-encoded ciphertext (IV + GCM-ciphertext)
     * @throws MfaEncryptionException if encryption fails
     */
    public static String encrypt(String plainSecret, String encryptionKey, String encryptionSalt) {
        if (plainSecret == null || plainSecret.isEmpty()) {
            return plainSecret;
        }
        try {
            SecretKey secret = deriveKey(encryptionKey, encryptionSalt);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION_MODE);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            SECURE_RANDOM.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] encryptedBytes = cipher.doFinal(plainSecret.getBytes(StandardCharsets.UTF_8));

            // Encoded blob: [12-byte IV][ciphertext]
            byte[] blob = ByteBuffer.allocate(iv.length + encryptedBytes.length)
                    .put(iv)
                    .put(encryptedBytes)
                    .array();

            return Base64.getEncoder().encodeToString(blob);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
                 | NoSuchAlgorithmException | NoSuchPaddingException
                 | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
            LOGGER.error("[MFA] Error encrypting TOTP secret: {}", e.getMessage());
            throw new MfaEncryptionException("Failed to encrypt MFA secret", e);
        }
    }

    /**
     * Decrypt an AES-256-GCM-encrypted TOTP secret.
     *
     * @param encryptedSecret Base64-encoded ciphertext blob (IV + GCM-ciphertext)
     * @param encryptionKey  the per-tenant encryption key (PBKDF2 password)
     * @param encryptionSalt the per-tenant salt for PBKDF2 key derivation
     * @return the original plain-text Base32 TOTP secret
     * @throws MfaEncryptionException if decryption fails
     */
    public static String decrypt(String encryptedSecret, String encryptionKey, String encryptionSalt) {
        if (encryptedSecret == null || encryptedSecret.isEmpty()) {
            return encryptedSecret;
        }
        try {
            byte[] blob = Base64.getDecoder().decode(encryptedSecret);
            ByteBuffer bb = ByteBuffer.wrap(blob);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);

            byte[] ciphertext = new byte[bb.remaining()];
            bb.get(ciphertext);

            SecretKey secret = deriveKey(encryptionKey, encryptionSalt);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION_MODE);
            cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException
                 | NoSuchAlgorithmException | NoSuchPaddingException
                 | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
            LOGGER.error("[MFA] Error decrypting TOTP secret: {}", e.getMessage());
            throw new MfaEncryptionException("Failed to decrypt MFA secret", e);
        }
    }

    /**
     * Derive a 256-bit AES secret key via PBKDF2WithHmacSHA256.
     */
    private static SecretKey deriveKey(String encryptionKey, String encryptionSalt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGO);
        KeySpec spec = new PBEKeySpec(
                encryptionKey.toCharArray(),
                encryptionSalt.getBytes(StandardCharsets.UTF_8),
                ITERATION_COUNT,
                AES_KEY_BIT);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGORITHM);
    }

    /**
     * Runtime exception wrapping encryption/decryption failures.
     */
    public static class MfaEncryptionException extends RuntimeException {
        public MfaEncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
