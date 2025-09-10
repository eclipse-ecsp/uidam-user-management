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

import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.ClientRegistrationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Test class for AesEncryptionDecryption utility.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AesEncryptionDecryptionTest {

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private UserManagementTenantProperties tenantProperties;

    @Mock
    private ClientRegistrationProperties clientRegistrationProperties;

    @InjectMocks
    private AesEncryptionDecryption aesEncryptionDecryption;

    private static final String TEST_SECRET_KEY = "testSecretKey12345";
    private static final String TEST_SECRET_SALT = "testSalt123";
    private static final String TEST_DATA = "testData";
    private static final int NONCE_LENGTH_12 = 12;
    private static final int NONCE_LENGTH_16 = 16;

    private void setupMocks() {
        // Setup mocks only when needed
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getClientRegistration()).thenReturn(clientRegistrationProperties);
        when(clientRegistrationProperties.getSecretKey()).thenReturn(TEST_SECRET_KEY);
        when(clientRegistrationProperties.getSecretSalt()).thenReturn(TEST_SECRET_SALT);
    }

    @Test
    void testEncryptDecrypt_RoundTrip_WorksCorrectly() {
        // Arrange
        setupMocks();

        // Act
        String encryptedData = aesEncryptionDecryption.encrypt(TEST_DATA);
        final String decryptedData = aesEncryptionDecryption.decrypt(encryptedData);

        // Assert
        assertNotNull(encryptedData);
        assertFalse(encryptedData.isEmpty());
        assertNotEquals(TEST_DATA, encryptedData); // Should be encrypted
        assertEquals(TEST_DATA, decryptedData); // Should decrypt back to original
    }

    @Test
    void testEncrypt_WithNullData_ReturnsEmptyString() {
        // Arrange
        setupMocks();

        // Act
        String result = aesEncryptionDecryption.encrypt(null);

        // Assert
        assertEquals("", result);
    }

    @Test
    void testDecrypt_WithNullData_ReturnsEmptyString() {
        // Arrange
        setupMocks();

        // Act
        String result = aesEncryptionDecryption.decrypt(null);

        // Assert
        assertEquals("", result);
    }

    @Test
    void testDecrypt_WithInvalidData_ThrowsRuntimeException() {
        // Arrange
        setupMocks();
        String invalidEncryptedData = "invalidBase64Data";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            aesEncryptionDecryption.decrypt(invalidEncryptedData);
        });
    }

    @Test
    void testGetRandomNonce_ReturnsCorrectLength() {
        // Act
        byte[] nonce = AesEncryptionDecryption.getRandomNonce(NONCE_LENGTH_12);

        // Assert
        assertNotNull(nonce);
        assertEquals(NONCE_LENGTH_12, nonce.length);
    }

    @Test
    void testGetRandomNonce_GeneratesDifferentValues() {
        // Act
        byte[] nonce1 = AesEncryptionDecryption.getRandomNonce(NONCE_LENGTH_16);
        byte[] nonce2 = AesEncryptionDecryption.getRandomNonce(NONCE_LENGTH_16);

        // Assert
        assertNotNull(nonce1);
        assertNotNull(nonce2);
        assertEquals(NONCE_LENGTH_16, nonce1.length);
        assertEquals(NONCE_LENGTH_16, nonce2.length);
        assertFalse(java.util.Arrays.equals(nonce1, nonce2)); // Should be different
    }

    @Test
    void testEncrypt_WithEmptyString_WorksCorrectly() {
        // Arrange
        setupMocks();

        // Act
        String encryptedData = aesEncryptionDecryption.encrypt("");
        String decryptedData = aesEncryptionDecryption.decrypt(encryptedData);

        // Assert
        assertNotNull(encryptedData);
        assertFalse(encryptedData.isEmpty());
        assertEquals("", decryptedData);
    }

    @Test
    void testEncryptDecrypt_WithSpecialCharacters_WorksCorrectly() {
        // Arrange
        setupMocks();
        String specialData = "Special@#$%^&*()_+-={}[]|\\:;\"'<>?,./";

        // Act
        String encryptedData = aesEncryptionDecryption.encrypt(specialData);
        String decryptedData = aesEncryptionDecryption.decrypt(encryptedData);

        // Assert
        assertNotNull(encryptedData);
        assertFalse(encryptedData.isEmpty());
        assertEquals(specialData, decryptedData);
    }
}
