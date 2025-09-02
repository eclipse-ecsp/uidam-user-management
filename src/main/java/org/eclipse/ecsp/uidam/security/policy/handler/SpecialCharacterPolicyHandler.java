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

import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService.PasswordValidationInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * This class is used to check the number of special characters in a password. It ensures that the password contains a
 * minimum number of special characters from an allowed set and excludes certain specified characters.
 */
public class SpecialCharacterPolicyHandler extends PasswordPolicyHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecialCharacterPolicyHandler.class);
    private static final int INT_MINUS_ONE = -1;
    private final int minSpecialChars;
    private final String excluded;
    private final String allowed;

    /**
     * Constructor to initialize SpecialCharsPolicyHandler with rules.
     *
     * @param rules The rules for special character validation.
     */
    public SpecialCharacterPolicyHandler(Map<String, Object> rules) {
        this.minSpecialChars = toInt(rules.get("minSpecialChars"), 1);
        this.excluded = toString(rules.get("excludedSpecialChars"), "");
        this.allowed = toString(rules.get("allowedSpecialChars"), "!@#$%^&*()_-+=<>?");
    }

    /**
     * Validates the password based on the number of special characters it contains.
     *
     * @param input The {@link PasswordValidationInput} containing the password to validate.
     * @return {@code true} if the password meets the minimum requirements for special characters.
     */
    @Override
    protected boolean doHandle(PasswordValidationInput input) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Validating password for special characters,for user: {}", input.username());
        }
        int specialCount = 0;
        for (char c : input.password().toCharArray()) {
            if (allowed.indexOf(c) >= 0 && excluded.indexOf(c) == INT_MINUS_ONE) {
                specialCount++;
            }
        }
        return specialCount >= minSpecialChars;
    }

    @Override
    protected String getErrorMessage() {
        return String.format(
                "Password must contain at least %d special character(s) from allowed set and exclude [%s].",
                minSpecialChars, excluded);
    }
}
