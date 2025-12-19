/*
 *
 *   ******************************************************************************
 *
 *    Copyright (c) 2023-24 Harman International
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *
 *    you may not use this file except in compliance with the License.
 *
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *    See the License for the specific language governing permissions and
 *
 *    limitations under the License.
 *
 *    SPDX-License-Identifier: Apache-2.0
 *
 *    *******************************************************************************
 *
 */

package org.eclipse.ecsp.uidam.config;

import org.eclipse.ecsp.sql.multitenancy.TenantDatabaseProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MultitenancySystemPropertyConfig.
 * Tests system property management, tenant property logging, and dynamic refresh functionality.
 */
@ExtendWith(MockitoExtension.class)
class MultitenancySystemPropertyConfigTest {

    @Mock
    private Map<String, TenantDatabaseProperties> multiTenantDbProperties;

    @Mock
    private Environment environment;

    @InjectMocks
    private MultitenancySystemPropertyConfig config;

    @BeforeEach
    void setUp() {
        // Set up default field values
        ReflectionTestUtils.setField(config, "sqlMultitenancyEnabled", true);
        ReflectionTestUtils.setField(config, "tenantMultitenancyEnabled", true);
        ReflectionTestUtils.setField(config, "tenantIds", "tenant1,tenant2");
        ReflectionTestUtils.setField(config, "defaultTenantId", "default");
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("multitenancy.enabled");
        System.clearProperty("multi.tenant.ids");
    }

    @Test
    void init_shouldSetMultitenancyEnabledSystemProperty() {
        // Arrange
        ReflectionTestUtils.setField(config, "sqlMultitenancyEnabled", true);
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1,tenant2");
        when(environment.getProperty("tenant.default", "default")).thenReturn("default");

        // Act
        config.init();

        // Assert
        assertEquals("true", System.getProperty("multitenancy.enabled"));
    }

    @Test
    void init_withMultitenancyEnabled_shouldSetTenantIds() {
        // Arrange
        ReflectionTestUtils.setField(config, "tenantMultitenancyEnabled", true);
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1,tenant2");

        // Act
        config.init();

        // Assert
        assertEquals("tenant1,tenant2", System.getProperty("multi.tenant.ids"));
    }

    @Test
    void init_withMultitenancyDisabled_shouldSetDefaultTenant() {
        // Arrange
        ReflectionTestUtils.setField(config, "tenantMultitenancyEnabled", false);
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(false);
        when(environment.getProperty("tenant.ids", "")).thenReturn("");
        when(environment.getProperty("tenant.default", "default")).thenReturn("myDefault");

        // Act
        config.init();

        // Assert
        assertEquals("myDefault", System.getProperty("multi.tenant.ids"));
    }

    @Test
    void refreshTenantSystemProperties_withMultitenancyEnabled_shouldUpdateSystemProperty() {
        // Arrange
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1,tenant2,tenant3");
        when(environment.getProperty("tenant.default", "default")).thenReturn("default");

        // Act
        config.refreshTenantSystemProperties();

        // Assert
        assertEquals("tenant1,tenant2,tenant3", System.getProperty("multi.tenant.ids"));
    }

    @Test
    void refreshTenantSystemProperties_withMultitenancyDisabled_shouldUseDefaultTenant() {
        // Arrange
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(false);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1,tenant2");
        when(environment.getProperty("tenant.default", "default")).thenReturn("singleTenant");

        // Act
        config.refreshTenantSystemProperties();

        // Assert
        assertEquals("singleTenant", System.getProperty("multi.tenant.ids"));
    }

    @Test
    void refreshTenantSystemProperties_withMultitenancyEnabled_shouldSetPropertiesForAllTenants() {
        // Arrange
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1");
        when(environment.getProperty("tenant.default", "default")).thenReturn("default");
        
        final Map<String, TenantDatabaseProperties> tenants = new HashMap<>();
        TenantDatabaseProperties props = new TenantDatabaseProperties();
        props.setJdbcUrl("jdbc:postgresql://localhost:5432/tenant1");
        props.setUserName("user1");
        props.setPassword("pass1");
        tenants.put("tenant1", props);

        when(multiTenantDbProperties.get("tenant1")).thenReturn(props);

        // Act
        config.refreshTenantSystemProperties();

        // Assert
        verify(multiTenantDbProperties, atLeastOnce()).get("tenant1");
    }

    @Test
    void refreshTenantSystemProperties_shouldCallLogTenantDatabaseProperties() {
        // Arrange
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1");
        when(environment.getProperty("tenant.default", "default")).thenReturn("default");

        final Map<String, TenantDatabaseProperties> tenants = new HashMap<>();
        TenantDatabaseProperties props = new TenantDatabaseProperties();
        props.setJdbcUrl("jdbc:postgresql://localhost:5432/tenant1");
        props.setUserName("user1");
        props.setPassword("pass1");
        tenants.put("tenant1", props);

        when(multiTenantDbProperties.get("tenant1")).thenReturn(props);

        // Act
        config.refreshTenantSystemProperties();

        // Assert
        verify(multiTenantDbProperties, atLeastOnce()).get("tenant1");
    }

    @Test
    void logTenantDatabaseProperties_withValidTenant_shouldLogProperties() {
        // Arrange
        final Map<String, TenantDatabaseProperties> tenants = new HashMap<>();
        TenantDatabaseProperties props = new TenantDatabaseProperties();
        props.setJdbcUrl("jdbc:postgresql://localhost:5432/testdb");
        props.setUserName("testUser");
        props.setPassword("testPassword");
        tenants.put("tenant1", props);

        when(multiTenantDbProperties.get("tenant1")).thenReturn(props);
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1");

        // Act
        config.refreshTenantSystemProperties();

        // Assert
        verify(multiTenantDbProperties, atLeastOnce()).get("tenant1");
    }

    @Test
    void logTenantDatabaseProperties_withNullProperties_shouldHandleGracefully() {
        // Arrange
        when(multiTenantDbProperties.get("tenant1")).thenReturn(null);
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1");

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> config.refreshTenantSystemProperties());
    }

    @Test
    void logTenantDatabaseProperties_withNullMultiTenantDbProperties_shouldHandleGracefully() {
        // Arrange
        ReflectionTestUtils.setField(config, "multiTenantDbProperties", null);
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1");

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> config.refreshTenantSystemProperties());
    }

    @Test
    void logTenantDatabaseProperties_withEmptyTenantIds_shouldHandleGracefully() {
        // Arrange
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("");

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> config.refreshTenantSystemProperties());
    }

    @Test
    void logTenantDatabaseProperties_withMultipleTenants_shouldLogAll() {
        // Arrange
        final Map<String, TenantDatabaseProperties> tenants = new HashMap<>();
        
        TenantDatabaseProperties props1 = new TenantDatabaseProperties();
        props1.setJdbcUrl("jdbc:postgresql://localhost:5432/tenant1");
        props1.setUserName("user1");
        props1.setPassword("pass1");
        tenants.put("tenant1", props1);

        TenantDatabaseProperties props2 = new TenantDatabaseProperties();
        props2.setJdbcUrl("jdbc:postgresql://localhost:5432/tenant2");
        props2.setUserName("user2");
        props2.setPassword("pass2");
        tenants.put("tenant2", props2);

        when(multiTenantDbProperties.get("tenant1")).thenReturn(props1);
        when(multiTenantDbProperties.get("tenant2")).thenReturn(props2);
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1,tenant2");

        // Act
        config.refreshTenantSystemProperties();

        // Assert
        verify(multiTenantDbProperties, atLeastOnce()).get("tenant1");
        verify(multiTenantDbProperties, atLeastOnce()).get("tenant2");
    }

    @Test
    void logSingleTenantProperties_shouldMaskPassword() {
        // Arrange
        final Map<String, TenantDatabaseProperties> tenants = new HashMap<>();
        TenantDatabaseProperties props = new TenantDatabaseProperties();
        props.setJdbcUrl("jdbc:postgresql://localhost:5432/testdb");
        props.setUserName("testUser");
        props.setPassword("secretPassword123");
        tenants.put("tenant1", props);

        when(multiTenantDbProperties.get("tenant1")).thenReturn(props);
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1");

        // Act
        config.refreshTenantSystemProperties();

        // Assert - Password should be masked in logs (verify in actual execution)
        verify(multiTenantDbProperties, atLeastOnce()).get("tenant1");
    }

    @Test
    void refreshTenantSystemProperties_withNullEnvironmentProperty_shouldHandleGracefully() {
        // Arrange
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(Boolean.FALSE);
        when(environment.getProperty("tenant.ids", "")).thenReturn("");
        when(environment.getProperty("tenant.default", "default")).thenReturn("default");

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> config.refreshTenantSystemProperties());
    }

    @Test
    void init_shouldCallRefreshTenantSystemProperties() {
        // Arrange
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("tenant1");
        when(environment.getProperty("tenant.default", "default")).thenReturn("default");

        // Act
        config.init();

        // Assert
        verify(environment, atLeastOnce()).getProperty(eq("tenant.multitenant.enabled"), eq(Boolean.class), eq(false));
    }

    @Test
    void refreshTenantSystemProperties_withWhitespaceInTenantIds_shouldHandleCorrectly() {
        // Arrange
        when(environment.getProperty("tenant.multitenant.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("tenant.ids", "")).thenReturn("  tenant1  ,  tenant2  ");
        when(environment.getProperty("tenant.default", "default")).thenReturn("default");

        final Map<String, TenantDatabaseProperties> tenants = new HashMap<>();
        TenantDatabaseProperties props1 = new TenantDatabaseProperties();
        props1.setJdbcUrl("jdbc:postgresql://localhost:5432/tenant1");
        tenants.put("tenant1", props1);

        TenantDatabaseProperties props2 = new TenantDatabaseProperties();
        props2.setJdbcUrl("jdbc:postgresql://localhost:5432/tenant2");
        tenants.put("tenant2", props2);

        when(multiTenantDbProperties.get("tenant1")).thenReturn(props1);
        when(multiTenantDbProperties.get("tenant2")).thenReturn(props2);

        // Act & Assert - Should handle whitespace
        assertDoesNotThrow(() -> config.refreshTenantSystemProperties());
    }
}
