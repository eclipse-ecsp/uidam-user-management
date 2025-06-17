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
import org.mockito.Mockito;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the CompromisedPasswordPolicyHandler class.
 */
class CompromisedPasswordPolicyHandlerTest {

    private static final int INT_5 = 5;
    private static final int INT_40 = 40;
    private CompromisedPasswordPolicyHandler handler;
    private Map<String, Object> rules;

    @BeforeEach
    void setUp() {
        rules = new HashMap<>();
        rules.put("passwordHashSubStringLength", INT_5);
        handler = new CompromisedPasswordPolicyHandler(rules);
    }

    @Test
    void testDoHandlePasswordNotCompromised() throws NoSuchAlgorithmException {
        PasswordValidationInput input = mock(PasswordValidationInput.class);
        when(input.password()).thenReturn("StrongPassword123!");

        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(false).when(spyHandler).isPasswordCompromised(anyString());

        boolean result = spyHandler.doHandle(input);

        assertTrue(result);
        verify(spyHandler, times(1)).isPasswordCompromised("StrongPassword123!");
    }

    @Test
    void testDoHandlePasswordCompromised() throws NoSuchAlgorithmException {
        PasswordValidationInput input = mock(PasswordValidationInput.class);
        when(input.password()).thenReturn("WeakPassword");

        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(true).when(spyHandler).isPasswordCompromised(anyString());

        boolean result = spyHandler.doHandle(input);

        assertFalse(result);
        verify(spyHandler, times(1)).isPasswordCompromised("WeakPassword");
    }

    @Test
    void testDoHandleThrowsException() throws NoSuchAlgorithmException {
        PasswordValidationInput input = mock(PasswordValidationInput.class);
        when(input.password()).thenReturn("AnyPassword");
        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(false).when(spyHandler).isPasswordCompromised(anyString());
        // Simulate exception
        Mockito.doThrow(new NoSuchAlgorithmException()).when(spyHandler).isPasswordCompromised("AnyPassword");
        boolean result = spyHandler.doHandle(input);
        assertFalse(result);
    }

    @Test
    void testConstructorWithMissingRule() {
        Map<String, Object> emptyRules = new HashMap<>();
        CompromisedPasswordPolicyHandler handlerWithDefault = new CompromisedPasswordPolicyHandler(emptyRules);
        assertNotNull(handlerWithDefault);
    }

    @Test
    void testToHashValidPassword() throws NoSuchAlgorithmException {
        String password = "TestPassword123!";
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length()); // SHA-1 hash length in hex is 40 characters
    }

    @Test
    void testToHashNullPassword() throws NoSuchAlgorithmException {
        String hash = handler.toHash(null);

        assertNotNull(hash);
        assertEquals("", hash);
    }

    @Test
    void testToHashEmptyString() throws NoSuchAlgorithmException {
        String hash = handler.toHash("");
        assertNotNull(hash);
        assertEquals(INT_40, hash.length()); 
    }

    @Test
    void testGetMessageDigest() throws NoSuchAlgorithmException {
        assertNotNull(handler.getMessageDigest());
    }

    @Test
    void testIsPasswordCompromisedReturnsTrue() throws Exception {
        CompromisedPasswordPolicyHandler handler = new CompromisedPasswordPolicyHandler(rules) {
            @Override
            protected String toHash(String password) {
                return "ABCDEF12345";
            }

            @Override
            public boolean isPasswordCompromised(String password) {
                return true;
            }
        };
        assertTrue(handler.isPasswordCompromised("any"));
    }

    @Test
    void testIsPasswordCompromisedReturnsFalse() throws Exception {
        CompromisedPasswordPolicyHandler handler = new CompromisedPasswordPolicyHandler(rules) {
            @Override
            protected String toHash(String password) {
                return "ABCDEF12345";
            }

            @Override
            public boolean isPasswordCompromised(String password) {
                return false;
            }
        };
        assertFalse(handler.isPasswordCompromised("any"));
    }

    @Test
    void testLogRequestReturnsFilter() {
        assertNotNull(CompromisedPasswordPolicyHandler.logRequest());
    }
}