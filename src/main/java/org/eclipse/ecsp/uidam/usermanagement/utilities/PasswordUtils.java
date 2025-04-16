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
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.springframework.http.HttpStatus;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
}