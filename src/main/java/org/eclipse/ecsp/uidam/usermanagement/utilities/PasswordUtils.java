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

import org.apache.axiom.util.base64.Base64Utils;
import org.eclipse.ecsp.uidam.usermanagement.entity.PasswordHistoryEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.springframework.http.HttpStatus;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 *  Password Encryption Utility.
 */
public class PasswordUtils {

    private PasswordUtils() {
        throw new ApplicationRuntimeException("PasswordUtils cannot be instantiated!!");
    }

    private static final int SALT_SIZE = 16;

    /**
     * Get encrypted password hash.
     *
     * @param password password
     * @param salt salt
     * @param passwordEncoder passwordEncoder
     * @return hash
     */
    public static String getSecurePassword(String password, String salt, String passwordEncoder) {
        String generatedPassword;
        try {
            MessageDigest md = MessageDigest.getInstance(passwordEncoder);
            String input = password + salt;
            byte[] bytes = md.digest(input.getBytes());
            generatedPassword = Base64Utils.encode(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new ApplicationRuntimeException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return generatedPassword;
    }

    /**
     * Method to generate random salt.
     *
     * @return salt
     */
    public static String getSalt() {
        byte[] salt = new byte[SALT_SIZE];
        Random random = new SecureRandom();
        random.nextBytes(salt);
        return Base64Utils.encode(salt);
    }

    /**
     * Method to validate user's new password against old passwords.
     *
     * @param passwordEncoder   password encoder.
     * @param newPassword       user's new password.
     * @param userSalts         user salt history.
     * @param userOldPasswords  user old password history.
     * @return true if the new password is not in the history, false otherwise.
     */
    public static boolean isPasswordValid(String passwordEncoder, String newPassword, List<String> userSalts,
                                          List<String> userOldPasswords) {
        return userSalts.stream()
                        .map(salt -> PasswordUtils.getSecurePassword(newPassword, salt, passwordEncoder))
                        .noneMatch(userOldPasswords::contains);
    }


    /**
     * Method to generate user's password history.
     *
     * @param userEntity    user entity.
     */
    public static PasswordHistoryEntity generateUserPasswordHistoryEntity(UserEntity userEntity) {
        PasswordHistoryEntity passwordHistoryEntity = new PasswordHistoryEntity();
        passwordHistoryEntity.setUserEntity(userEntity);
        passwordHistoryEntity.setPasswordSalt(userEntity.getPasswordSalt());
        passwordHistoryEntity.setUserPassword(userEntity.getUserPassword());
        passwordHistoryEntity.setUserName(userEntity.getUserName());
        passwordHistoryEntity.setCreatedBy(userEntity.getCreatedBy());
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        passwordHistoryEntity.setCreateDate(now);
        passwordHistoryEntity.setUpdatedBy("system");
        passwordHistoryEntity.setUpdateDate(now);

        return passwordHistoryEntity;
    }
}