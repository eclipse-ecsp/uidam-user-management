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
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialCharacterPolicyHandlerTest {

    private static final int INT_3 = 3;
    private static final int INT_2 = 2;

    @Test
    void testPasswordWithSufficientSpecialCharacters() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minSpecialChars", INT_2);
        rules.put("allowedSpecialChars", "!@#$%^&*");
        rules.put("excludedSpecialChars", "");

        SpecialCharacterPolicyHandler handler = new SpecialCharacterPolicyHandler(rules);
        PasswordValidationInput input = new PasswordValidationInput(null, "Pass@word!", null);

        assertTrue(handler.doHandle(input), "Password should pass validation with sufficient special characters.");
    }

    @Test
    void testPasswordWithInsufficientSpecialCharacters() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minSpecialChars", INT_3);
        rules.put("allowedSpecialChars", "!@#$%^&*");
        rules.put("excludedSpecialChars", "");

        SpecialCharacterPolicyHandler handler = new SpecialCharacterPolicyHandler(rules);
        PasswordValidationInput input = new PasswordValidationInput(null, "Pass@word", null);

        assertFalse(handler.doHandle(input), "Password should fail validation with insufficient special characters.");
    }

    @Test
    void testPasswordWithExcludedSpecialCharacters() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minSpecialChars", 1);
        rules.put("allowedSpecialChars", "!@#$%^&*");
        rules.put("excludedSpecialChars", "@");

        SpecialCharacterPolicyHandler handler = new SpecialCharacterPolicyHandler(rules);
        PasswordValidationInput input = new PasswordValidationInput(null, "Pass@word", null);

        assertFalse(handler.doHandle(input), "Password should fail validation due to excluded special characters.");
    }

    @Test
    void testPasswordWithNoSpecialCharacters() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minSpecialChars", 1);
        rules.put("allowedSpecialChars", "!@#$%^&*");
        rules.put("excludedSpecialChars", "");

        SpecialCharacterPolicyHandler handler = new SpecialCharacterPolicyHandler(rules);
        PasswordValidationInput input = new PasswordValidationInput(null, "Password", null);

        assertFalse(handler.doHandle(input), "Password should fail validation with no special characters.");
    }

    @Test
    void testPasswordWithAllowedAndExcludedCharacters() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minSpecialChars", INT_2);
        rules.put("allowedSpecialChars", "!@#$%^&*");
        rules.put("excludedSpecialChars", "#");

        SpecialCharacterPolicyHandler handler = new SpecialCharacterPolicyHandler(rules);
        PasswordValidationInput input = new PasswordValidationInput(null, "Pass!word$", null);

        assertTrue(handler.doHandle(input), "Password should pass validation with allowed special characters.");
    }
}