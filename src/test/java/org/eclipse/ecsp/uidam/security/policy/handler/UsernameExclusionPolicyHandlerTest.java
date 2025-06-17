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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsernameExclusionPolicyHandlerTest {

    private static final int INT_3 = 3;
    private UsernameExclusionPolicyHandler handler;

    @BeforeEach
    void setUp() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("noOfCharsSeqinUserField", INT_3);
        handler = new UsernameExclusionPolicyHandler(rules);
    }

    @Test
    void testPasswordValidWhenUsernameIsNull() {
        assertTrue(handler.doHandle(getHandlerInput(null, "securePassword123", null)));
    }

    @Test
    void testPasswordValidWhenUsernameTooShort() {
        assertTrue(handler.doHandle(getHandlerInput("ab", "securePassword123", null)));
    }

    private PasswordValidationInput getHandlerInput(String username, String password,
            Timestamp lastUpdateTime) {
        return new PasswordValidationInput(username, password, lastUpdateTime);
    }

    @Test
    void testPasswordInvalidWhenContainsUsernameSequence() {
        assertFalse(handler.doHandle(getHandlerInput("abc", "myPasswordAbc123", null)));
    }

    @Test
    void testPasswordValidWhenNoUsernameSequence() {
        assertTrue(handler.doHandle(getHandlerInput("abc", "securePassword123", null)));
    }

    @Test
    void testPasswordValidationCaseInsensitive() {
        assertFalse(handler.doHandle(getHandlerInput("AbC", "mypasswordabc123", null)));
    }
}