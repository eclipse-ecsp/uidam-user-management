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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link MfaSecretEncryptionUtil}.
 */
@DisplayName("MfaSecretEncryptionUtil Test Suite")
class MfaSecretEncryptionUtilTest {

    private static final String PLAIN_SECRET = "JBSWY3DPEHPK3PXP";
    private static final String ENCRYPTION_KEY  = "TestEncryptionKey12345!";
    private static final String ENCRYPTION_SALT = "TestEncryptionSalt12345";

    @Test
    @DisplayName("Encrypt then decrypt should yield original secret")
    void encryptAndDecrypt_roundTrip() {
        String encrypted = MfaSecretEncryptionUtil.encrypt(PLAIN_SECRET, ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertNotNull(encrypted);
        assertNotEquals(PLAIN_SECRET, encrypted, "Encrypted value must not equal plain text");

        String decrypted = MfaSecretEncryptionUtil.decrypt(encrypted, ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertEquals(PLAIN_SECRET, decrypted, "Decrypted value must match original");
    }

    @Test
    @DisplayName("Different encryptions of the same secret should produce different ciphertexts (random IV)")
    void encrypt_differentOutputEachTime() {
        String enc1 = MfaSecretEncryptionUtil.encrypt(PLAIN_SECRET, ENCRYPTION_KEY, ENCRYPTION_SALT);
        String enc2 = MfaSecretEncryptionUtil.encrypt(PLAIN_SECRET, ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertNotEquals(enc1, enc2, "Two encryptions of the same value must differ due to random IV");
    }

    @Test
    @DisplayName("Decrypt with wrong key should throw MfaEncryptionException")
    void decrypt_wrongKey_throwsException() {
        String encrypted = MfaSecretEncryptionUtil.encrypt(PLAIN_SECRET, ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertThrows(MfaSecretEncryptionUtil.MfaEncryptionException.class,
                () -> MfaSecretEncryptionUtil.decrypt(encrypted, "WrongKey!!!", ENCRYPTION_SALT));
    }

    @Test
    @DisplayName("Decrypt with wrong salt should throw MfaEncryptionException")
    void decrypt_wrongSalt_throwsException() {
        String encrypted = MfaSecretEncryptionUtil.encrypt(PLAIN_SECRET, ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertThrows(MfaSecretEncryptionUtil.MfaEncryptionException.class,
                () -> MfaSecretEncryptionUtil.decrypt(encrypted, ENCRYPTION_KEY, "WrongSalt!!!"));
    }

    @Test
    @DisplayName("Encrypt null input returns null")
    void encrypt_null_returnsNull() {
        String result = MfaSecretEncryptionUtil.encrypt(null, ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertNull(result);
    }

    @Test
    @DisplayName("Decrypt null input returns null")
    void decrypt_null_returnsNull() {
        String result = MfaSecretEncryptionUtil.decrypt(null, ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertNull(result);
    }

    @Test
    @DisplayName("Encrypt empty string returns empty string")
    void encrypt_emptyString_returnsEmpty() {
        String result = MfaSecretEncryptionUtil.encrypt("", ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Decrypt empty string returns empty string")
    void decrypt_emptyString_returnsEmpty() {
        String result = MfaSecretEncryptionUtil.decrypt("", ENCRYPTION_KEY, ENCRYPTION_SALT);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Decrypt corrupted ciphertext throws MfaEncryptionException")
    void decrypt_corruptedData_throwsException() {
        assertThrows(MfaSecretEncryptionUtil.MfaEncryptionException.class,
                () -> MfaSecretEncryptionUtil.decrypt("NotBase64!!!==InvalidData", ENCRYPTION_KEY, ENCRYPTION_SALT));
    }
}
