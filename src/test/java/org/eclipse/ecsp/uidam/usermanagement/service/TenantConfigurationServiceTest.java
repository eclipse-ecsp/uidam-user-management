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

package org.eclipse.ecsp.uidam.usermanagement.service;

import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.MultiTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantConfigurationService.
 */
@ExtendWith(MockitoExtension.class)
class TenantConfigurationServiceTest {

    private static final String TEST_TENANT_ID = "tenant1";
    private static final String DEFAULT_TENANT_ID = "default";
    private static final String NON_EXISTENT_TENANT = "nonexistent";

    @Mock
    private MultiTenantProperties multiTenantProperties;

    @Mock
    private UserManagementTenantProperties tenantProperties;

    @Mock
    private UserManagementTenantProperties defaultTenantProperties;

    private TenantConfigurationService tenantConfigurationService;

    @BeforeEach
    void setUp() {
        tenantConfigurationService = new TenantConfigurationService(multiTenantProperties);
    }

    @Test
    void testGetTenantProperties_WithValidTenantId_ReturnsProperties() {
        // Arrange
        when(multiTenantProperties.getTenantProperties(TEST_TENANT_ID)).thenReturn(tenantProperties);

        // Act
        UserManagementTenantProperties result = tenantConfigurationService.getTenantProperties(TEST_TENANT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(tenantProperties, result);
    }

    @Test
    void testGetTenantProperties_WithInvalidTenantId_ReturnsNull() {
        // Arrange
        when(multiTenantProperties.getTenantProperties(NON_EXISTENT_TENANT)).thenReturn(null);

        // Act
        UserManagementTenantProperties result = tenantConfigurationService.getTenantProperties(NON_EXISTENT_TENANT);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetTenantProperties_WithCurrentTenant_ReturnsProperties() {
        // Arrange
        try (MockedStatic<TenantContext> mockedTenantContext = mockStatic(TenantContext.class)) {
            mockedTenantContext.when(TenantContext::getCurrentTenant).thenReturn(TEST_TENANT_ID);
            when(multiTenantProperties.getTenantProperties(TEST_TENANT_ID)).thenReturn(tenantProperties);

            // Act
            UserManagementTenantProperties result = tenantConfigurationService.getTenantProperties();

            // Assert
            assertNotNull(result);
            assertEquals(tenantProperties, result);
        }
    }

    @Test
    void testTenantExists_WithValidTenant_ReturnsTrue() {
        // Arrange
        when(multiTenantProperties.hasTenant(TEST_TENANT_ID)).thenReturn(true);

        // Act
        boolean result = tenantConfigurationService.tenantExists(TEST_TENANT_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    void testTenantExists_WithInvalidTenant_ReturnsFalse() {
        // Arrange
        when(multiTenantProperties.hasTenant(NON_EXISTENT_TENANT)).thenReturn(false);

        // Act
        boolean result = tenantConfigurationService.tenantExists(NON_EXISTENT_TENANT);

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetAllTenantIds_ReturnsSetOfTenantIds() {
        // Arrange
        Set<String> expectedTenantIds = Set.of(TEST_TENANT_ID, DEFAULT_TENANT_ID);
        when(multiTenantProperties.getAllTenantIds()).thenReturn(expectedTenantIds);

        // Act
        Set<String> result = tenantConfigurationService.getAllTenantIds();

        // Assert
        assertNotNull(result);
        assertEquals(expectedTenantIds, result);
    }
}
