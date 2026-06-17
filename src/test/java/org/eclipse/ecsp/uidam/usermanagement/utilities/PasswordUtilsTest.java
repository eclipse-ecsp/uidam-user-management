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

import org.eclipse.ecsp.uidam.usermanagement.entity.PasswordHistoryEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for PasswordUtils.
 */
@DisplayName("PasswordUtils Test Suite")
class PasswordUtilsTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideSecurePasswordCases")
    @DisplayName("Should produce correct hash behavior")
    void testGetSecurePassword(String description, String password1, String salt1,
            String password2, String salt2, Boolean expectEqual) {
        String hash1 = PasswordUtils.getSecurePassword(password1, salt1, "SHA-256");
        assertNotNull(hash1);
        assertTrue(hash1.length() > 0);
        if (password2 != null) {
            String hash2 = PasswordUtils.getSecurePassword(password2, salt2, "SHA-256");
            if (Boolean.TRUE.equals(expectEqual)) {
                assertEquals(hash1, hash2);
            } else {
                assertNotEquals(hash1, hash2);
            }
        }
    }

    static Stream<Arguments> provideSecurePasswordCases() {
        return Stream.of(
            Arguments.of("SHA-256 hash is not null and not empty", "TestPassword123", "testSalt", null, null, null),
            Arguments.of("same inputs produce same hash", "ConsistentPassword", "consistentSalt",
                    "ConsistentPassword", "consistentSalt", true),
            Arguments.of("different passwords produce different hashes", "Password1", "sameSalt",
                    "Password2", "sameSalt", false),
            Arguments.of("different salts produce different hashes", "samePassword", "salt1",
                    "samePassword", "salt2", false)
        );
    }

    @Test
    @DisplayName("Should generate salt successfully")
    void testGetSalt() {
        // Act
        String salt = PasswordUtils.getSalt();

        // Assert
        assertNotNull(salt);
        assertTrue(salt.length() > 0);
    }

    @Test
    @DisplayName("Should generate unique salts on each call")
    void testGetSaltUniqueness() {
        // Act
        String salt1 = PasswordUtils.getSalt();
        String salt2 = PasswordUtils.getSalt();
        String salt3 = PasswordUtils.getSalt();

        // Assert
        assertNotEquals(salt1, salt2);
        assertNotEquals(salt2, salt3);
        assertNotEquals(salt1, salt3);
    }

    @Test
    @DisplayName("Should validate password when not in history")
    void testIsPasswordValidNewPassword() {
        // Arrange
        String newPassword = "NewPassword123";
        String salt1 = "salt1";
        String salt2 = "salt2";
        
        List<String> salts = Arrays.asList(salt1, salt2);
        List<String> oldPasswords = Arrays.asList(
            PasswordUtils.getSecurePassword("OldPassword1", salt1, "SHA-256"),
            PasswordUtils.getSecurePassword("OldPassword2", salt2, "SHA-256")
        );

        // Act
        boolean result = PasswordUtils.isPasswordValid("SHA-256", newPassword, salts, oldPasswords);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should reject password when in history")
    void testIsPasswordValidPasswordInHistory() {
        // Arrange
        String password = "ReusedPassword";
        String salt = "testSalt";
        
        List<String> salts = Collections.singletonList(salt);
        List<String> oldPasswords = Collections.singletonList(
            PasswordUtils.getSecurePassword(password, salt, "SHA-256")
        );

        // Act
        boolean result = PasswordUtils.isPasswordValid("SHA-256", password, salts, oldPasswords);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should validate password with empty history")
    void testIsPasswordValidEmptyHistory() {
        // Arrange
        String password = "NewPassword";
        List<String> salts = Collections.emptyList();
        List<String> oldPasswords = Collections.emptyList();

        // Act
        boolean result = PasswordUtils.isPasswordValid("SHA-256", password, salts, oldPasswords);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should generate password history entity from user entity")
    void testGenerateUserPasswordHistoryEntity() {
        // Arrange
        UserEntity userEntity = new UserEntity();
        userEntity.setId(BigInteger.valueOf(1));
        userEntity.setUserName("testuser");
        userEntity.setPasswordSalt("testSalt");
        userEntity.setUserPassword("hashedPassword");
        userEntity.setCreatedBy("admin");

        // Act
        PasswordHistoryEntity historyEntity = PasswordUtils.generateUserPasswordHistoryEntity(userEntity);

        // Assert
        assertNotNull(historyEntity);
        assertEquals(userEntity, historyEntity.getUserEntity());
        assertEquals(userEntity.getPasswordSalt(), historyEntity.getPasswordSalt());
        assertEquals(userEntity.getUserPassword(), historyEntity.getUserPassword());
        assertEquals(userEntity.getUserName(), historyEntity.getUserName());
        assertNotNull(historyEntity.getCreateDate());
        assertNotNull(historyEntity.getUpdateDate());
    }

    @Test
    @DisplayName("Should handle SHA-1 algorithm")
    void testGetSecurePasswordSha1() {
        // Arrange
        String password = "TestPassword";
        String salt = "testSalt";

        // Act
        String hash = PasswordUtils.getSecurePassword(password, salt, "SHA-1");

        // Assert
        assertNotNull(hash);
        assertTrue(hash.length() > 0);
    }

    @Test
    @DisplayName("Should handle MD5 algorithm")
    void testGetSecurePasswordMd5() {
        // Arrange
        String password = "TestPassword";
        String salt = "testSalt";

        // Act
        String hash = PasswordUtils.getSecurePassword(password, salt, "MD5");

        // Assert
        assertNotNull(hash);
        assertTrue(hash.length() > 0);
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void testGetSecurePasswordWithSpecialCharacters() {
        // Arrange
        String password = "P@$$w0rd!#%&*()";
        String salt = "specialSalt";

        // Act
        String hash = PasswordUtils.getSecurePassword(password, salt, "SHA-256");

        // Assert
        assertNotNull(hash);
        assertTrue(hash.length() > 0);
    }

    @Test
    @DisplayName("Should validate password with multiple history entries")
    void testIsPasswordValidMultipleHistory() {
        // Arrange
        String newPassword = "CurrentPassword123";
        String salt1 = PasswordUtils.getSalt();
        String salt2 = PasswordUtils.getSalt();
        String salt3 = PasswordUtils.getSalt();
        
        List<String> salts = Arrays.asList(salt1, salt2, salt3);
        List<String> oldPasswords = Arrays.asList(
            PasswordUtils.getSecurePassword("OldPassword1", salt1, "SHA-256"),
            PasswordUtils.getSecurePassword("OldPassword2", salt2, "SHA-256"),
            PasswordUtils.getSecurePassword("OldPassword3", salt3, "SHA-256")
        );

        // Act
        boolean result = PasswordUtils.isPasswordValid("SHA-256", newPassword, salts, oldPasswords);

        // Assert
        assertTrue(result);
    }
}
