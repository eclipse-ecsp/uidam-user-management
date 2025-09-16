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

import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.repository.PasswordHistoryRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PasswordPolicyHandlerFactory.
 */
class PasswordPolicyHandlerFactoryTest {

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private UserManagementTenantProperties tenantProperties;

    private PasswordPolicyHandlerFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        factory = new PasswordPolicyHandlerFactory(passwordHistoryRepository, tenantConfigurationService);
    }

    @Test
    void testCreateHandler_SizePolicy() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        when(policy.getKey()).thenReturn("size");
        when(policy.getValidationRules()).thenReturn(new HashMap<>());

        PasswordPolicyHandler handler = factory.createHandler(policy);

        assertNotNull(handler);
        assertTrue(handler instanceof SizePolicyHandler);
    }

    @Test
    void testCreateHandler_SpecialCharsPolicy() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        when(policy.getKey()).thenReturn("specialChars");
        when(policy.getValidationRules()).thenReturn(new HashMap<>());

        PasswordPolicyHandler handler = factory.createHandler(policy);

        assertNotNull(handler);
        assertTrue(handler instanceof SpecialCharacterPolicyHandler);
    }

    @Test
    void testCreateHandler_ComplexityPolicy() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        when(policy.getKey()).thenReturn("complexity");
        when(policy.getValidationRules()).thenReturn(new HashMap<>());

        PasswordPolicyHandler handler = factory.createHandler(policy);

        assertNotNull(handler);
        assertTrue(handler instanceof ComplexityPolicyHandler);
    }

    @Test
    void testCreateHandler_ExpirationPolicy() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        when(policy.getKey()).thenReturn("expiration");
        when(policy.getValidationRules()).thenReturn(new HashMap<>());
        when(tenantProperties.getPasswordEncoder()).thenReturn("SHA-256");

        PasswordPolicyHandler handler = factory.createHandler(policy);

        assertNotNull(handler);
        assertTrue(handler instanceof ExpirationPolicyHandler);
    }

    @Test
    void testCreateHandler_UsernameSequenceExclusionPolicy() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        when(policy.getKey()).thenReturn("usernameSequenceExclusion");
        when(policy.getValidationRules()).thenReturn(new HashMap<>());

        PasswordPolicyHandler handler = factory.createHandler(policy);

        assertNotNull(handler);
        assertTrue(handler instanceof UsernameExclusionPolicyHandler);
    }

    @Test
    void testCreateHandler_PasswordLastUpdateValidationPolicy() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        when(policy.getKey()).thenReturn("passwordLastUpdateValidation");
        when(policy.getValidationRules()).thenReturn(new HashMap<>());

        PasswordPolicyHandler handler = factory.createHandler(policy);

        assertNotNull(handler);
        assertTrue(handler instanceof LastUpdateValidationPolicyHandler);
    }

    @Test
    void testCreateHandler_CompromisedPasswordPolicy() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        when(policy.getKey()).thenReturn("CompromisedPassword");
        when(policy.getValidationRules()).thenReturn(new HashMap<>());

        PasswordPolicyHandler handler = factory.createHandler(policy);

        assertNotNull(handler);
        assertTrue(handler instanceof CompromisedPasswordPolicyHandler);
    }

    @Test
    void testCreateHandler_UnknownPolicyKey() {
        PasswordPolicy policy = mock(PasswordPolicy.class);
        when(policy.getKey()).thenReturn("unknownKey");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> factory.createHandler(policy));
        assertEquals("Unknown policy key: unknownKey", exception.getMessage());
    }

    @Test
    void testCreateHandler_NullPolicy() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> factory.createHandler(null));
        assertEquals("policy must not be null", exception.getMessage());
    }
}