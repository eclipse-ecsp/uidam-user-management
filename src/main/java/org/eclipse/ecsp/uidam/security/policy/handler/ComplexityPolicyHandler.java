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
 * ComplexityPolicyHandler is a concrete implementation of the PasswordPolicyHandler that enforces password complexity
 * rules. It checks if the password contains a minimum number of uppercase letters, lowercase letters, and digits.
 */
public class ComplexityPolicyHandler extends PasswordPolicyHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexityPolicyHandler.class);
    private final int minUppercase;
    private final int minLowercase;
    private final int minDigits;

    /**
     * Constructs a ComplexityPolicyHandler with the specified password complexity rules.
     *
     * @param rules A map containing the password complexity rules. The expected keys are: 
     *     - "minUppercase": The minimum  number of uppercase letters required (default is 1). 
     *     - "minLowercase": The minimum number of lowercase letters
     *     required (default is 1). - "minDigits": The minimum number of digits required (default is 1).
     */
    public ComplexityPolicyHandler(Map<String, Object> rules) {
        this.minUppercase = toInt(rules.get("minUppercase"), 1);
        this.minLowercase = toInt(rules.get("minLowercase"), 1);
        this.minDigits = toInt(rules.get("minDigits"), 1);
    }

    /**
     * Validates the complexity of a password based on the number of uppercase letters, lowercase letters, and digits it
     * contains.
     *
     * @param input the {@link PasswordValidationInput} containing the password to validate.
     * @return {@code true} if the password meets the minimum requirements for uppercase letters, lowercase letters, and
     *     digits; {@code false} otherwise.
     */
    @Override
    protected boolean doHandle(PasswordValidationInput input) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Validating password complexity for user: {}", input.username());
        }
        int upper = 0;
        int lower = 0;
        int digits = 0;
        for (char c : input.password().toCharArray()) {
            if (Character.isUpperCase(c)) {
                upper++;
            } else if (Character.isLowerCase(c)) {
                lower++;
            } else if (Character.isDigit(c)) {
                digits++;
            }
        }
        return upper >= minUppercase && lower >= minLowercase && digits >= minDigits;
    }

    /**
     * Returns the error message to be displayed when the password does not meet the complexity requirements.
     *
     * @return A string containing the error message.
     */
    @Override
    protected String getErrorMessage() {
        return String.format("Password must contain at least %d uppercase, %d lowercase, and %d digit(s).",
                minUppercase, minLowercase, minDigits);
    }
}
