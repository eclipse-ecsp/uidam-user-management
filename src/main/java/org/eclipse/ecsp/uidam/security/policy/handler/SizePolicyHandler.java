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

package org.eclipse.ecsp.uidam.security.policy.handler;

import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * This class is used to validate the length of a password based on specified minimum and maximum length rules. It
 * extends the PasswordPolicyHandler class and implements the doHandle method to check the password length.
 */
public class SizePolicyHandler extends PasswordPolicyHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SizePolicyHandler.class);
    private static final int INT_128 = 128;
    private static final int INT_8 = 8;
    private final int minLength;
    private final int maxLength;

    /**
     * Constructor to initialize the LengthPolicyHandler with the specified rules.
     *
     * @param rules A map containing the rules for minimum and maximum password length.
     */
    public SizePolicyHandler(Map<String, Object> rules) {
        this.minLength = toInt(rules.get("minLength"), INT_8);
        this.maxLength = toInt(rules.get("maxLength"), INT_128);
    }

    /**
     * Validates the password length based on the specified minimum and maximum length rules.
     *
     * @param input The input containing the password to validate.
     * @return true if the password length is valid, false otherwise.
     */
    @Override
    protected boolean doHandle(PasswordValidationInput input) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Validating password length for user: {}", input.username());
        }
        String password = input.password();
        return password.length() >= minLength && password.length() <= maxLength;
    }

    /**
     * Returns the error message if the password length is invalid.
     *
     * @return The error message indicating the valid password length range.
     */
    @Override
    protected String getErrorMessage() {
        return String.format("Password must be between %d and %d characters.", minLength, maxLength);
    }
}