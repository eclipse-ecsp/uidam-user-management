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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the PasswordPolicyHandler class.
 */
class PasswordPolicyHandlerTest {

    private static final int INT_3 = 3;
    private static final int INT_5 = 5;
    private static final int INT_10 = 10;
    private static final int INT_0 = 0;
    private static final int INT_42 = 42;
    private PasswordPolicyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PasswordPolicyHandler() {
            @Override
            protected boolean doHandle(PasswordValidationInput input) {
                // Simple validation logic for testing
                return input != null && input.password() != null && !input.password().isEmpty();
            }
        };
    }

    @Test
    void testSetAndGetErrorMessage() {
        String errorMessage = "Password is invalid";
        handler.setErrorMessage(errorMessage);
        assertEquals(errorMessage, handler.getErrorMessage());
    }

    @Test
    void testSetNextHandler() {
        PasswordPolicyHandler nextHandler = new PasswordPolicyHandler() {
            @Override
            protected boolean doHandle(PasswordValidationInput input) {
                return true;
            }
        };
        handler.setNextHandler(nextHandler);
        assertNotNull(handler.next);
        assertEquals(nextHandler, handler.next);
    }

    @Test
    void testHandleValidPassword()  {
        PasswordValidationInput input = new PasswordValidationInput(null, "ValidPassword123", null);
        assertTrue(handler.handle(input));
    }

    @Test
    void testHandleInvalidPassword() {
        PasswordValidationInput input = new PasswordValidationInput(null, "", null);
        handler.setErrorMessage("Password cannot be empty");
        handler.handle(input);
        assertEquals("Password cannot be empty", handler.getErrorMessage());
    }

    @Test
    void testToInt() {
        assertEquals(INT_42, handler.toInt("42", INT_0));
        assertEquals(INT_0, handler.toInt("invalid", INT_0));
        assertEquals(INT_10, handler.toInt(null, INT_10));
        assertEquals(INT_5, handler.toInt(INT_5, INT_0));
    }

    @Test
    void testToCharSet() {
        Set<Character> charSet = handler.toCharSet("abc");
        assertEquals(INT_3, charSet.size());
        assertTrue(charSet.contains('a'));
        assertTrue(charSet.contains('b'));
        assertTrue(charSet.contains('c'));
    }

    @Test
    void testToString() {
        assertEquals("test", handler.toString("test", "default"));
        assertEquals("default", handler.toString(null, "default"));
    }
}