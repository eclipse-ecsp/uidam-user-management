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
 *
 */

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
 * This class defines methods to encrypt and decrypt client secret.
 */
@Component
public class AesEncryptionDecryption {
    private static Logger logger = LoggerFactory.getLogger(AesEncryptionDecryption.class);
    private static final String ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION_MODE = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int AES_KEY_BIT = 256;
    private static final int ITERATION_COUNT = 65536;

    @Value("${client.registration.secret.key:random_secret_key}")
    private String secretKey;

    @Value("${client.registration.secret.salt:random_salt}")
    private String salt;

    /**
     * This method is used to encrypt client secret.
     *
     * @return String
     **/
    public String encrypt(String data) {
        String encryptedText = "";
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), ITERATION_COUNT, AES_KEY_BIT);
            SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGORITHM);

            if (data == null) {
                return encryptedText;
            }

            Cipher encryptCipher = Cipher.getInstance(AES_TRANSFORMATION_MODE);

            // get IV
            byte[] iv = getRandomNonce(IV_LENGTH_BYTE);
            encryptCipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            // encrypted data:
            byte[] encryptedBytes = encryptCipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // prefix IV and Salt to cipher text
            byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length() + encryptedBytes.length).put(iv)
                    .put(salt.getBytes()).put(encryptedBytes).array();

            encryptedText = Base64.getEncoder().encodeToString(cipherTextWithIvSalt);

        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException
                 | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
            logger.error("error while encrypting secret {}", e.getMessage());
            throw new RuntimeException("error while encrypting secret {}" + e.getMessage());
        }

        return encryptedText;
    }

    /**
     * This method is used to decrypt client secret.
     *
     * @return String
     **/
    public String decrypt(String encryptedString) {
        String decryptedText = "";
        try {
            if (encryptedString == null) {
                return decryptedText;
            }

            // separate prefix with IV from the rest of encrypted data
            byte[] encryptedPayload = Base64.getDecoder().decode(encryptedString);
            // get back the iv and salt from the cipher text
            ByteBuffer bb = ByteBuffer.wrap(encryptedPayload);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);
            byte[] secretSalt = new byte[salt.length()];
            bb.get(secretSalt);
            byte[] encryptedBytes = new byte[bb.remaining()];
            bb.get(encryptedBytes);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            Cipher decryptCipher = Cipher.getInstance(AES_TRANSFORMATION_MODE);
            KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), ITERATION_COUNT, AES_KEY_BIT);
            SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGORITHM);
            decryptCipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
            decryptedText = new String(decryptedBytes);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
                | InvalidKeySpecException e) {
            logger.error("error while decrypting secret {} ", e.getMessage());
            throw new RuntimeException("error while decrypting secret {}" + e.getMessage());
        }

        return decryptedText;
    }

    /**
     * This method fill an array with random bytes.
     *
     * @param numBytes byte length
     * @return array filled with random bytes
     */
    public static byte[] getRandomNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }
}