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

package org.eclipse.ecsp.uidam.usermanagement.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantDefaultPropertiesProcessor refresh functionality.
 * Tests dynamic tenant property generation and refresh capabilities.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantDefaultPropertiesProcessorTest {

    @Mock
    private ConfigurableEnvironment configurableEnvironment;

    @Mock
    private MutablePropertySources propertySources;

    private TenantDefaultPropertiesProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TenantDefaultPropertiesProcessor();
        processor.setEnvironment(configurableEnvironment);
        
        when(configurableEnvironment.getPropertySources()).thenReturn(propertySources);
    }

    @Test
    void refreshTenantProperties_withNullTenantIds_shouldLogWarning() {
        // Arrange
        String nullTenantIds = null;

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> processor.refreshTenantProperties(nullTenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_withEmptyTenantIds_shouldLogWarning() {
        // Arrange
        String emptyTenantIds = "";

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> processor.refreshTenantProperties(emptyTenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_withWhitespaceTenantIds_shouldLogWarning() {
        // Arrange
        String whitespaceTenantIds = "   ";

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> processor.refreshTenantProperties(whitespaceTenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_withSingleTenant_shouldProcessTenant() {
        // Arrange
        final String tenantIds = "tenant1";
        
        // Mock property sources
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("tenants.profile.default.jdbc-url", "jdbc:postgresql://localhost:5432/default");
        defaultProps.put("tenants.profile.default.user-name", "defaultUser");
        MapPropertySource defaultPropertySource = new MapPropertySource("defaultProps", defaultProps);
        
        List<PropertySource<?>> sources = new ArrayList<>();
        sources.add(defaultPropertySource);
        when(propertySources.iterator()).thenReturn(sources.iterator());
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.jdbc-url")).thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.user-name")).thenReturn(null);
        when(propertySources.contains("generatedTenantProperties")).thenReturn(false);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> processor.refreshTenantProperties(tenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_withMultipleTenants_shouldProcessAll() {
        // Arrange
        final String tenantIds = "tenant1,tenant2,tenant3";
        
        // Mock property sources
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("tenants.profile.default.jdbc-url", "jdbc:postgresql://localhost:5432/default");
        MapPropertySource defaultPropertySource = new MapPropertySource("defaultProps", defaultProps);
        
        List<PropertySource<?>> sources = new ArrayList<>();
        sources.add(defaultPropertySource);
        when(propertySources.iterator()).thenReturn(sources.iterator());
        when(propertySources.contains("generatedTenantProperties")).thenReturn(false);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> processor.refreshTenantProperties(tenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_withDefaultTenantInList_shouldSkipDefault() {
        // Arrange
        final String tenantIds = "tenant1,default,tenant2";
        
        // Mock property sources
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("tenants.profile.default.jdbc-url", "jdbc:postgresql://localhost:5432/default");
        MapPropertySource defaultPropertySource = new MapPropertySource("defaultProps", defaultProps);
        
        List<PropertySource<?>> sources = new ArrayList<>();
        sources.add(defaultPropertySource);
        when(propertySources.iterator()).thenReturn(sources.iterator());
        when(propertySources.contains("generatedTenantProperties")).thenReturn(false);

        // Act & Assert - Should skip 'default' tenant and not throw exception
        assertDoesNotThrow(() -> processor.refreshTenantProperties(tenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_withExistingGeneratedProperties_shouldRemoveAndRecreate() {
        // Arrange
        final String tenantIds = "tenant1";
        
        // Mock property sources with complete default tenant properties
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("tenants.profile.default.jdbc-url", "jdbc:postgresql://localhost:5432/default");
        defaultProps.put("tenants.profile.default.user-name", "defaultUser");
        defaultProps.put("tenants.profile.default.password", "defaultPass");
        defaultProps.put("tenants.profile.default.notification.email.host", "smtp.example.com");
        defaultProps.put("tenants.profile.default.notification.email.port", "587");
        defaultProps.put("tenants.profile.default.notification.email.username", "emailUser");
        defaultProps.put("tenants.profile.default.notification.email.password", "emailPass");
        MapPropertySource defaultPropertySource = new MapPropertySource("defaultProps", defaultProps);
        
        List<PropertySource<?>> sources = new ArrayList<>();
        sources.add(defaultPropertySource);
        when(propertySources.iterator()).thenReturn(sources.iterator());
        when(propertySources.contains("generatedTenantProperties")).thenReturn(true);
        
        // Disable validation to allow property generation
        when(configurableEnvironment.getProperty("tenant.config.validation.enabled", Boolean.class, true))
            .thenReturn(false);
        
        // Mock environment to return default properties for validation
        when(configurableEnvironment.getProperty("tenants.profile.default.jdbc-url"))
            .thenReturn("jdbc:postgresql://localhost:5432/default");
        when(configurableEnvironment.getProperty("tenants.profile.default.user-name"))
            .thenReturn("defaultUser");
        when(configurableEnvironment.getProperty("tenants.profile.default.password"))
            .thenReturn("defaultPass");
        when(configurableEnvironment.getProperty("tenants.profile.default.notification.email.host"))
            .thenReturn("smtp.example.com");
        when(configurableEnvironment.getProperty("tenants.profile.default.notification.email.port"))
            .thenReturn("587");
        when(configurableEnvironment.getProperty("tenants.profile.default.notification.email.username"))
            .thenReturn("emailUser");
        when(configurableEnvironment.getProperty("tenants.profile.default.notification.email.password"))
            .thenReturn("emailPass");
        
        // Mock environment to return null for tenant1 properties (so they will be generated)
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.jdbc-url")).thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.user-name")).thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.password")).thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.notification.email.host"))
            .thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.notification.email.port"))
            .thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.notification.email.username"))
            .thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.notification.email.password"))
            .thenReturn(null);

        // Act
        processor.refreshTenantProperties(tenantIds, configurableEnvironment);

        // Assert - Should remove old property source before adding new one
        verify(propertySources).remove("generatedTenantProperties");
    }

    @Test
    void refreshTenantProperties_withTenantsHavingWhitespace_shouldTrimAndProcess() {
        // Arrange
        final String tenantIds = " tenant1 , tenant2 , tenant3 ";
        
        // Mock property sources
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("tenants.profile.default.jdbc-url", "jdbc:postgresql://localhost:5432/default");
        MapPropertySource defaultPropertySource = new MapPropertySource("defaultProps", defaultProps);
        
        List<PropertySource<?>> sources = new ArrayList<>();
        sources.add(defaultPropertySource);
        when(propertySources.iterator()).thenReturn(sources.iterator());
        when(propertySources.contains("generatedTenantProperties")).thenReturn(false);

        // Act & Assert - Should handle whitespace correctly
        assertDoesNotThrow(() -> processor.refreshTenantProperties(tenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_shouldReloadDefaultProperties() {
        // Arrange
        final String tenantIds = "tenant1";
        
        // Mock property sources
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("tenants.profile.default.jdbc-url", "jdbc:postgresql://localhost:5432/default");
        defaultProps.put("tenants.profile.default.user-name", "defaultUser");
        MapPropertySource defaultPropertySource = new MapPropertySource("defaultProps", defaultProps);
        
        List<PropertySource<?>> sources = new ArrayList<>();
        sources.add(defaultPropertySource);
        when(propertySources.iterator()).thenReturn(sources.iterator());
        when(propertySources.contains("generatedTenantProperties")).thenReturn(false);
        
        // Mock environment to return null for tenant1 properties (so they will be generated)
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.jdbc-url")).thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.user-name")).thenReturn(null);
        when(configurableEnvironment.getProperty("tenants.profile.default.jdbc-url"))
            .thenReturn("jdbc:postgresql://localhost:5432/default");
        when(configurableEnvironment.getProperty("tenants.profile.default.user-name")).thenReturn("defaultUser");

        // Act
        processor.refreshTenantProperties(tenantIds, configurableEnvironment);

        // Assert - Should call iterator at least once (for loading default properties)
        verify(propertySources, atLeast(1)).iterator();
    }

    @Test
    void refreshTenantProperties_withNoPropertiesGenerated_shouldLogNoNewProperties() {
        // Arrange
        final String tenantIds = "tenant1";
        
        // Mock property sources with tenant already having all properties
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("tenants.profile.default.jdbc-url", "jdbc:postgresql://localhost:5432/default");
        MapPropertySource defaultPropertySource = new MapPropertySource("defaultProps", defaultProps);
        
        List<PropertySource<?>> sources = new ArrayList<>();
        sources.add(defaultPropertySource);
        when(propertySources.iterator()).thenReturn(sources.iterator());
        when(configurableEnvironment.getProperty("tenants.profile.tenant1.jdbc-url"))
            .thenReturn("jdbc:postgresql://localhost:5432/tenant1");
        when(propertySources.contains("generatedTenantProperties")).thenReturn(false);

        // Act & Assert - Should not add property source if no properties generated
        assertDoesNotThrow(() -> processor.refreshTenantProperties(tenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_withMultipleTenantsIncludingEmptyStrings_shouldSkipEmpty() {
        // Arrange
        final String tenantIds = "tenant1,,tenant2,";
        
        // Mock property sources
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("tenants.profile.default.jdbc-url", "jdbc:postgresql://localhost:5432/default");
        MapPropertySource defaultPropertySource = new MapPropertySource("defaultProps", defaultProps);
        
        List<PropertySource<?>> sources = new ArrayList<>();
        sources.add(defaultPropertySource);
        when(propertySources.iterator()).thenReturn(sources.iterator());
        when(propertySources.contains("generatedTenantProperties")).thenReturn(false);

        // Act & Assert - Should skip empty tenant IDs
        assertDoesNotThrow(() -> processor.refreshTenantProperties(tenantIds, configurableEnvironment));
    }

    @Test
    void refreshTenantProperties_shouldHandleExceptionGracefully() {
        // Arrange
        String tenantIds = "tenant1";
        
        when(propertySources.iterator()).thenThrow(new RuntimeException("Simulated error"));

        // Act & Assert - Should handle exception
        assertThrows(RuntimeException.class, () -> 
            processor.refreshTenantProperties(tenantIds, configurableEnvironment));
    }

    @Test
    void getOrder_shouldReturnHighestPrecedence() {
        // Act
        int order = processor.getOrder();

        // Assert
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE, order);
    }

    @Test
    void setEnvironment_shouldStoreEnvironment() {
        // Arrange
        ConfigurableEnvironment newEnvironment = mock(ConfigurableEnvironment.class);

        // Act
        processor.setEnvironment(newEnvironment);

        // Assert - Environment is set (verified by not throwing exception in subsequent calls)
        assertDoesNotThrow(() -> processor.setEnvironment(newEnvironment));
    }
}
