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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService.PasswordValidationInput;

import java.util.Map;

/**
 * This class is used to validate password against username. It checks if the password contains a sequence of characters
 * from the username.
 */
public class UsernameExclusionPolicyHandler extends PasswordPolicyHandler {

    private static final int INT_3 = 3;

    private static final Log LOGGER = LogFactory.getLog(UsernameExclusionPolicyHandler.class);

    private final int noOfCharsSeqinUserField;

    /**
     * Constructor to initialize the handler with rules.
     *
     * @param rules Map containing configuration rules for the handler.
     */
    public UsernameExclusionPolicyHandler(Map<String, Object> rules) {
        this.noOfCharsSeqinUserField = toInt(rules.get("noOfCharsSeqinUserField"), INT_3);
    }

    /**
     * Validates the password against the username. It checks if the password contains a sequence of characters from the
     * username.
     *
     * @param input The password validation input containing password and username.
     * @return true if the password is valid, false otherwise.
     */
    @Override
    protected boolean doHandle(PasswordValidationInput input) {
        LOGGER.debug("Validating password against username exclusion policy,for user: " + input.username());
        String password = input.password();
        String username = input.username();
        if (username == null || username.length() < noOfCharsSeqinUserField) {
            String message = "Username is null or too short to check for sequences.";
            setErrorMessage(message);
            LOGGER.warn(message);
            return true; // No validation needed if username is null or too short
        }

        for (int i = 0; i <= username.length() - noOfCharsSeqinUserField; i++) {
            String sequence = username.substring(i, i + noOfCharsSeqinUserField);
            if (password.toLowerCase().contains(sequence.toLowerCase())) {
                String errorMessage = "Password must not contain a sequence of " + noOfCharsSeqinUserField
                        + " characters from the username.";
                setErrorMessage(errorMessage);
                LOGGER.warn(errorMessage);
                return false;
            }
        }
        return true;
    }

}
