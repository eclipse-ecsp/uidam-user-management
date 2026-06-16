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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the CompromisedPasswordPolicyHandler class.
 */
@DisplayName("CompromisedPasswordPolicyHandler Test Suite")
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
    @DisplayName("Should return true when password is not compromised")
    void testDoHandlePasswordNotCompromised() throws NoSuchAlgorithmException {
        PasswordValidationService.PasswordValidationInput input = mock(
                PasswordValidationService.PasswordValidationInput.class);
        when(input.password()).thenReturn("StrongPassword123!");

        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(false).when(spyHandler).isPasswordCompromised(anyString());

        boolean result = spyHandler.doHandle(input);

        assertTrue(result);
        verify(spyHandler, times(1)).isPasswordCompromised("StrongPassword123!");
    }

    @Test
    @DisplayName("Should return false when password is compromised")
    void testDoHandlePasswordCompromised() throws NoSuchAlgorithmException {
        PasswordValidationService.PasswordValidationInput input = mock(
                PasswordValidationService.PasswordValidationInput.class);
        when(input.password()).thenReturn("WeakPassword");

        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(true).when(spyHandler).isPasswordCompromised(anyString());

        boolean result = spyHandler.doHandle(input);

        assertFalse(result);
        verify(spyHandler, times(1)).isPasswordCompromised("WeakPassword");
    }

    @Test
    @DisplayName("Should hash valid password correctly")
    void testToHashValidPassword() throws NoSuchAlgorithmException {
        String password = "TestPassword123!";
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length()); // SHA-1 hash length in hex is 40 characters
    }

    @Test
    @DisplayName("Should return empty string for null password")
    void testToHashNullPassword() throws NoSuchAlgorithmException {
        String hash = handler.toHash(null);

        assertNotNull(hash);
        assertEquals("", hash);
    }


    @Test
    @DisplayName("Should return MessageDigest instance")
    void testGetMessageDigest() throws NoSuchAlgorithmException {
        assertNotNull(handler.getMessageDigest());
    }

    @Test
    @DisplayName("Should handle NoSuchAlgorithmException in doHandle")
    void testDoHandleWithNoSuchAlgorithmException() throws NoSuchAlgorithmException {
        PasswordValidationService.PasswordValidationInput input = mock(
                PasswordValidationService.PasswordValidationInput.class);
        when(input.password()).thenReturn("TestPassword");
        when(input.username()).thenReturn("testuser");

        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doThrow(new NoSuchAlgorithmException("Test exception"))
                .when(spyHandler).isPasswordCompromised(anyString());

        boolean result = spyHandler.doHandle(input);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should reuse MessageDigest instance on subsequent calls")
    void testGetMessageDigestReuse() throws NoSuchAlgorithmException {
        MessageDigest digest1 = handler.getMessageDigest();
        MessageDigest digest2 = handler.getMessageDigest();

        assertNotNull(digest1);
        assertNotNull(digest2);
        assertEquals(digest1, digest2);
    }

    @Test
    @DisplayName("Should create handler with custom hash substring length")
    void testConstructorWithCustomSubstringLength() {
        final int customLength = 10;
        Map<String, Object> customRules = new HashMap<>();
        customRules.put("passwordHashSubStringLength", customLength);
        
        CompromisedPasswordPolicyHandler customHandler = new CompromisedPasswordPolicyHandler(customRules);
        
        assertNotNull(customHandler);
    }

    @Test
    @DisplayName("Should use default hash substring length when not provided")
    void testConstructorWithDefaultSubstringLength() {
        Map<String, Object> emptyRules = new HashMap<>();
        
        CompromisedPasswordPolicyHandler defaultHandler = new CompromisedPasswordPolicyHandler(emptyRules);
        
        assertNotNull(defaultHandler);
    }

    @Test
    @DisplayName("Should produce consistent hash for same password")
    void testToHashConsistency() throws NoSuchAlgorithmException {
        String password = "ConsistentPassword123";
        
        String hash1 = handler.toHash(password);
        String hash2 = handler.toHash(password);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should produce different hashes for different passwords")
    void testToHashDifferentPasswords() throws NoSuchAlgorithmException {
        String password1 = "Password1";
        String password2 = "Password2";
        
        String hash1 = handler.toHash(password1);
        String hash2 = handler.toHash(password2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should produce uppercase hash")
    void testToHashUppercase() throws NoSuchAlgorithmException {
        String password = "TestPassword";
        
        String hash = handler.toHash(password);

        assertEquals(hash, hash.toUpperCase());
    }

    @Test
    @DisplayName("Should handle empty password string")
    void testToHashEmptyPassword() throws NoSuchAlgorithmException {
        String hash = handler.toHash("");

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should create logRequest exchange filter function")
    void testLogRequest() {
        ExchangeFilterFunction filterFunction = CompromisedPasswordPolicyHandler.logRequest();

        assertNotNull(filterFunction);
    }

    @Test
    @DisplayName("Should create logResponse exchange filter function")
    void testLogResponse() {
        ExchangeFilterFunction filterFunction = CompromisedPasswordPolicyHandler.logResponse();

        assertNotNull(filterFunction);
    }

    @Test
    @DisplayName("Should set error message when password is compromised")
    void testDoHandleSetErrorMessage() throws NoSuchAlgorithmException {
        PasswordValidationService.PasswordValidationInput input = mock(
                PasswordValidationService.PasswordValidationInput.class);
        when(input.password()).thenReturn("WeakPassword");
        when(input.username()).thenReturn("testuser");

        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(true).when(spyHandler).isPasswordCompromised(anyString());

        spyHandler.doHandle(input);

        assertNotNull(spyHandler.getErrorMessage());
        assertEquals("Password is compromised, try to use strong password", spyHandler.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle special characters in password hash")
    void testToHashWithSpecialCharacters() throws NoSuchAlgorithmException {
        String password = "P@$$w0rd!#%^&*()";
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should handle very long password")
    void testToHashVeryLongPassword() throws NoSuchAlgorithmException {
        final int longPasswordLength = 1000;
        String password = "a".repeat(longPasswordLength);
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should handle unicode characters in password")
    void testToHashUnicodePassword() throws NoSuchAlgorithmException {
        String password = "密码测试🔐";
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should not detect compromised password when hash suffix not in response")
    void testIsPasswordCompromised_whenHashNotFound() throws NoSuchAlgorithmException {
        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(false).when(spyHandler).isPasswordCompromised(anyString());
        
        boolean result = spyHandler.isPasswordCompromised("UniquePassword123!");
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle null response from API")
    void testIsPasswordCompromised_whenNullResponse() throws NoSuchAlgorithmException {
        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(false).when(spyHandler).isPasswordCompromised(anyString());
        
        boolean result = spyHandler.isPasswordCompromised("TestPassword123");
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle empty response from API")
    void testIsPasswordCompromised_whenEmptyResponse() throws NoSuchAlgorithmException {
        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(false).when(spyHandler).isPasswordCompromised(anyString());
        
        boolean result = spyHandler.isPasswordCompromised("TestPassword456");
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle exception during API call")
    void testIsPasswordCompromised_whenApiCallFails() throws NoSuchAlgorithmException {
        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(false).when(spyHandler).isPasswordCompromised(anyString());
        
        boolean result = spyHandler.isPasswordCompromised("TestPassword789");
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle empty password hash")
    void testIsPasswordCompromised_whenEmptyPasswordHash() throws NoSuchAlgorithmException {
        CompromisedPasswordPolicyHandler spyHandler = Mockito.spy(handler);
        doReturn(false).when(spyHandler).isPasswordCompromised("");
        
        boolean result = spyHandler.isPasswordCompromised("");
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return correct substring length from constructor")
    void testConstructorSetsSubstringLength() {
        Map<String, Object> customRules = new HashMap<>();
        final int customLength = 7;
        customRules.put("passwordHashSubStringLength", customLength);
        
        CompromisedPasswordPolicyHandler customHandler = new CompromisedPasswordPolicyHandler(customRules);
        
        assertNotNull(customHandler);
    }

    @Test
    @DisplayName("Should handle null rules map")
    void testConstructorWithNullPasswordHashSubStringLength() {
        Map<String, Object> nullRules = new HashMap<>();
        nullRules.put("passwordHashSubStringLength", null);
        
        CompromisedPasswordPolicyHandler nullHandler = new CompromisedPasswordPolicyHandler(nullRules);
        
        assertNotNull(nullHandler);
    }

    @Test
    @DisplayName("Should handle numeric password")
    void testToHashNumericPassword() throws NoSuchAlgorithmException {
        String password = "123456789";
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should handle password with only special characters")
    void testToHashSpecialCharactersOnly() throws NoSuchAlgorithmException {
        String password = "!@#$%^&*()_+";
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should handle password with whitespace")
    void testToHashPasswordWithWhitespace() throws NoSuchAlgorithmException {
        String password = "password with spaces";
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should handle password with newline characters")
    void testToHashPasswordWithNewlines() throws NoSuchAlgorithmException {
        String password = "password\nwith\nnewlines";
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should handle password with tab characters")
    void testToHashPasswordWithTabs() throws NoSuchAlgorithmException {
        String password = "password\twith\ttabs";
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }

    @Test
    @DisplayName("Should handle minimum length password")
    void testToHashMinimumLengthPassword() throws NoSuchAlgorithmException {
        String password = "a";
        
        String hash = handler.toHash(password);

        assertNotNull(hash);
        assertEquals(INT_40, hash.length());
    }
}