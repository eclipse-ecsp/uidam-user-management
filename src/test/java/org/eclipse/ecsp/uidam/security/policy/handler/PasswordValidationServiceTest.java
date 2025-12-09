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


import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService.ValidationResult;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordValidationServiceTest {

    private PasswordPolicyRepository passwordPolicyRepository;
    private PasswordPolicyHandlerFactory handlerFactory;
    private PasswordValidationService passwordValidationService;

    @BeforeEach
    void setUp() {
        passwordPolicyRepository = mock(PasswordPolicyRepository.class);
        handlerFactory = mock(PasswordPolicyHandlerFactory.class);
        passwordValidationService = new PasswordValidationService(passwordPolicyRepository, handlerFactory);
    }

    @Test
    void testLoadPolicies() {
        PasswordPolicy policy1 = mock(PasswordPolicy.class);
        PasswordPolicy policy2 = mock(PasswordPolicy.class);
        PasswordPolicyHandler handler1 = mock(PasswordPolicyHandler.class);
        PasswordPolicyHandler handler2 = mock(PasswordPolicyHandler.class);

        when(passwordPolicyRepository.findAllByRequiredTrueOrderByPriorityAsc())
                .thenReturn(Optional.of(Arrays.asList(policy1, policy2)));
        when(handlerFactory.createHandler(policy1)).thenReturn(handler1);
        when(handlerFactory.createHandler(policy2)).thenReturn(handler2);

        // Call refreshPolicies instead of loadPolicies since loadPolicies is commented out
        passwordValidationService.refreshPolicies();

        verify(handlerFactory).createHandler(policy1);
        verify(handlerFactory).createHandler(policy2);
    }

    @Test
    void testRefreshPolicies() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        PasswordPolicyHandler handler = mock(PasswordPolicyHandler.class);

        when(passwordPolicyRepository.findAllByRequiredTrueOrderByPriorityAsc())
                .thenReturn(Optional.of(List.of(policy)));
        when(handlerFactory.createHandler(policy)).thenReturn(handler);

        passwordValidationService.refreshPolicies();

        verify(handlerFactory).createHandler(policy);
    }

    @Test
    void testValidatePasswordForNewUser_Valid() {
        PasswordPolicyHandler handler = mock(PasswordPolicyHandler.class);
        when(handler.handle(any())).thenReturn(true);
        when(handler.getErrorMessage()).thenReturn(null);

        passwordValidationService.refreshPolicies();
        passwordValidationService.validatePassword("validPassword", "username");

        assertTrue(passwordValidationService.validatePassword("validPassword", "username").isValid());
    }

    @Test
    void testValidatePasswordForNewUser_Invalid() {
        PasswordPolicy policy = mock(PasswordPolicy.class); // Create a mock policy
        PasswordPolicyHandler handler = mock(ComplexityPolicyHandler.class);
        when(passwordPolicyRepository.findAllByRequiredTrueOrderByPriorityAsc())
                .thenReturn(Optional.of(List.of(policy))); // Return the mock policy
        when(handlerFactory.createHandler(policy)).thenReturn(handler); // Map policy to handler
        when(handler.handle(any())).thenReturn(false);
        when(handler.getErrorMessage()).thenReturn("Invalid password");
        passwordValidationService.refreshPolicies();
        passwordValidationService.validatePassword("invalidPassword", "username");
        assertFalse(passwordValidationService.validatePassword("invalidPassword", "username").isValid());
    }

    @Test
    void testValidatePasswordForExistingUser_Valid() {
        PasswordPolicyHandler handler = mock(PasswordPolicyHandler.class);
        when(handler.handle(any())).thenReturn(true);

        passwordValidationService.refreshPolicies();
        passwordValidationService.validatePassword("validPassword", "username",
                new Timestamp(System.currentTimeMillis()));

        assertTrue(passwordValidationService
                .validatePassword("validPassword", "username", new Timestamp(System.currentTimeMillis())).isValid());
    }

    @Test
    void testValidatePasswordForExistingUser_Invalid() {
        PasswordPolicy policy = mock(PasswordPolicy.class); // Create a mock policy
        PasswordPolicyHandler handler = mock(ComplexityPolicyHandler.class);
        when(passwordPolicyRepository.findAllByRequiredTrueOrderByPriorityAsc())
                .thenReturn(Optional.of(List.of(policy))); // Return the mock policy
        when(handlerFactory.createHandler(policy)).thenReturn(handler); // Map policy to handler
        when(handler.handle(any())).thenReturn(false);
        when(handler.getErrorMessage()).thenReturn("Invalid password");
        passwordValidationService.refreshPolicies();
        passwordValidationService.validatePassword("invalidPassword", "username",
                new Timestamp(System.currentTimeMillis()));
        assertFalse(passwordValidationService
                .validatePassword("invalidPassword", "username", new Timestamp(System.currentTimeMillis())).isValid());
    }

    @Test
    void testValidateUserPasswordExpiry_Valid() {
        PasswordPolicyHandler handler = mock(ExpirationPolicyHandler.class);
        when(handler.handle(any())).thenReturn(true);

        passwordValidationService.refreshPolicies();
        passwordValidationService.validateUserPasswordExpiry("username");

        assertTrue(passwordValidationService.validateUserPasswordExpiry("username").isValid());
    }

    @Test
    void testValidateUserPasswordExpiry_Invalid() {
        PasswordPolicy policy = mock(PasswordPolicy.class); // Create a mock policy
        PasswordPolicyHandler handler = mock(ExpirationPolicyHandler.class);
        when(passwordPolicyRepository.findAllByRequiredTrueOrderByPriorityAsc())
                .thenReturn(Optional.of(List.of(policy))); // Return the mock policy
        when(handlerFactory.createHandler(policy)).thenReturn(handler); // Map policy to handler
        when(handler.handle(any())).thenReturn(false);
        when(handler.getErrorMessage()).thenReturn("Password expired");

        passwordValidationService.refreshPolicies(); // Load policies and create handlers

        // Call the method under test and assert
        ValidationResult result = passwordValidationService.validateUserPasswordExpiry("username");
        assertFalse(result.isValid());
        assertEquals("Password expired", result.errorMessage());
    }
}