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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SizePolicyHandlerTest {

    private static final int INT_16 = 16;
    private static final int INT_8 = 8;
    private SizePolicyHandler sizePolicyHandler;

    @BeforeEach
    void setUp() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("minLength", INT_8);
        rules.put("maxLength", INT_16);
        sizePolicyHandler = new SizePolicyHandler(rules);
    }

    @Test
    void testPasswordWithinValidRange() {
        PasswordValidationInput input = new PasswordValidationInput(
                null, "ValidPass", null);
        assertTrue(sizePolicyHandler.doHandle(input), "Password within valid range should pass validation.");
    }

    @Test
    void testPasswordTooShort() {
        PasswordValidationInput input = new PasswordValidationInput(
                null, "Short", null);
        assertFalse(sizePolicyHandler.doHandle(input), "Password shorter than minimum length should fail validation.");
    }

    @Test
    void testPasswordTooLong() {
        PasswordValidationInput input = new PasswordValidationInput(
                null, "ThisPasswordIsWayTooLong", null);
        assertFalse(sizePolicyHandler.doHandle(input), "Password longer than maximum length should fail validation.");
    }

    @Test
    void testErrorMessage() {
        String expectedMessage = "Password must be between 8 and 16 characters.";
        assertEquals(expectedMessage, sizePolicyHandler.getErrorMessage(),
                "Error message should match the expected format.");
    }
}