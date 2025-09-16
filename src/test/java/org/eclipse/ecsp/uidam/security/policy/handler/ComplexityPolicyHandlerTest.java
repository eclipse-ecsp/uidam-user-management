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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the ComplexityPolicyHandler class.
 */
class ComplexityPolicyHandlerTest {

    private static final int INT_3 = 3;
    private static final int INT_2 = 2;

    @Test
    void testPasswordMeetsComplexityRequirements() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minUppercase", INT_2);
        rules.put("minLowercase", INT_3);
        rules.put("minDigits", 1);

        ComplexityPolicyHandler handler = new ComplexityPolicyHandler(rules);
        PasswordValidationService.PasswordValidationInput input = new PasswordValidationService.PasswordValidationInput(
                null, "AaBbc1", null);

        assertTrue(handler.doHandle(input), "Password should meet the complexity requirements.");
    }

    @ParameterizedTest 
    @CsvSource({ "aabb1, Password should fail the uppercase requirement.",
        "AABB1, Password should fail the lowercase requirement.",
        "AaBbCc, Password should fail the digit requirement." })
    void testPasswordFailsComplexityRequirements(String password, String expectedReason) {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minUppercase", INT_2);
        rules.put("minLowercase", INT_3);
        rules.put("minDigits", 1);

        ComplexityPolicyHandler handler = new ComplexityPolicyHandler(rules);
        PasswordValidationService.PasswordValidationInput input = new PasswordValidationService.PasswordValidationInput(
                null, password, null);

        assertFalse(handler.doHandle(input), expectedReason);
    }

    @Test
    void testDefaultRules() {
        Map<String, Object> rules = new HashMap<>();

        ComplexityPolicyHandler handler = new ComplexityPolicyHandler(rules);
        PasswordValidationService.PasswordValidationInput input = new PasswordValidationService.PasswordValidationInput(
                null, "Aa1", null);

        assertTrue(handler.doHandle(input), "Password should meet the default complexity requirements.");
    }

    @Test
    void testErrorMessage() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minUppercase", INT_2);
        rules.put("minLowercase", INT_3);
        rules.put("minDigits", 1);

        ComplexityPolicyHandler handler = new ComplexityPolicyHandler(rules);

        String expectedMessage = "Password must contain at least 2 uppercase, 3 lowercase, and 1 digit(s).";
        assertEquals(expectedMessage, handler.getErrorMessage(), "Error message should match the expected format.");
    }
}