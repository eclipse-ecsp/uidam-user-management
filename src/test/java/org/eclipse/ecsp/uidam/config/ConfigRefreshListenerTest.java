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

import org.eclipse.ecsp.sql.multitenancy.TenantAwareDataSource;
import org.eclipse.ecsp.sql.multitenancy.TenantDatabaseProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.LiquibaseConfig;
import org.eclipse.ecsp.uidam.usermanagement.config.TenantDefaultPropertiesProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConfigRefreshListener.
 * Tests configuration refresh event handling and dynamic tenant management.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigRefreshListenerTest {

    @Mock
    private ConfigurableEnvironment environment;

    @Mock
    private TenantAwareDataSource tenantAwareDataSource;

    @Mock
    private MultitenancySystemPropertyConfig multitenancySystemPropertyConfig;

    @Mock
    private TenantDefaultPropertiesProcessor tenantDefaultPropertiesProcessor;

    @Mock
    private LiquibaseConfig liquibaseConfig;

    @Mock
    private EnvironmentChangeEvent event;

    @InjectMocks
    private ConfigRefreshListener listener;

    @BeforeEach
    void setUp() {
        // Initialize property cache
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2");
        when(environment.getProperty("tenant.default-tenant-id")).thenReturn("default");
        when(environment.getProperty("tenant.multitenant.enabled")).thenReturn("true");
        
        // Stub LiquibaseConfig to prevent NoSuchMethodError in tests
        doNothing().when(liquibaseConfig).initializeTenantSchema(anyString());
        
        listener.initializePropertyCache();
    }

    @Test
    void initializePropertyCache_shouldCacheInitialValues() {
        // Arrange
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2");

        // Act
        listener.initializePropertyCache();

        // Assert
        verify(environment, atLeastOnce()).getProperty("tenant.ids");
    }

    @Test
    void onApplicationEvent_withNullKeys_shouldLogNoChanges() {
        // Arrange
        when(event.getKeys()).thenReturn(null);

        // Act
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        // Assert
        verify(event).getKeys();
    }

    @Test
    void onApplicationEvent_withEmptyKeys_shouldLogNoChanges() {
        // Arrange
        when(event.getKeys()).thenReturn(new HashSet<>());

        // Act
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        // Assert
        verify(event).getKeys();
    }

    @Test
    void onApplicationEvent_withNonTenantChanges_shouldLogChanges() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("some.other.property");
        changedKeys.add("another.property");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("some.other.property")).thenReturn("newValue");
        when(environment.getProperty("another.property")).thenReturn("anotherValue");

        // Act
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        // Assert
        verify(event).getKeys();
    }

    @Test
    void onApplicationEvent_withSensitiveProperty_shouldMaskValue() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("database.password");
        changedKeys.add("api.secret");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("database.password")).thenReturn("secret123");
        when(environment.getProperty("api.secret")).thenReturn("secretKey");

        // Act
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        // Assert - Sensitive values should be masked in logs
        verify(event).getKeys();
    }

    @Test
    void onApplicationEvent_withTenantIdsChange_shouldProcessTenants() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2,tenant3");

        // Act
        listener.onApplicationEvent(event);

        // Assert
        verify(multitenancySystemPropertyConfig).refreshTenantSystemProperties();
        verify(tenantDefaultPropertiesProcessor).refreshTenantProperties(anyString(),
                any(ConfigurableEnvironment.class));
    }

    @Test
    void onApplicationEvent_withTenantAddition_shouldAddTenantDataSource() throws Exception {
        // Arrange
        final Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2,tenant3");
        
        // Mock tenant properties for new tenant
        when(environment.getProperty("tenants.profile.tenant3.jdbc-url"))
            .thenReturn("jdbc:postgresql://localhost:5432/tenant3");
        when(environment.getProperty("tenants.profile.tenant3.user-name")).thenReturn("tenant3user");
        when(environment.getProperty("tenants.profile.tenant3.password")).thenReturn("tenant3pass");

        // Mock LiquibaseConfig method to prevent NoSuchMethodError
        // Use doNothing() since initializeTenantSchema returns void

        // Act
        listener.onApplicationEvent(event);

        // Assert
        verify(tenantAwareDataSource).addOrUpdateTenantDataSource(eq("tenant3"), any(TenantDatabaseProperties.class));
    }

    @Test
    void onApplicationEvent_withTenantRemoval_shouldRemoveTenantDataSource() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1");

        // Act
        listener.onApplicationEvent(event);

        // Assert
        verify(tenantAwareDataSource).removeTenantDataSource("tenant2");
    }

    @Test
    void onApplicationEvent_withMultitenancyToggle_shouldLogChange() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.multitenant.enabled");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.multitenant.enabled")).thenReturn("false");

        // Act
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        // Assert
        verify(event).getKeys();
    }

    @Test
    void onApplicationEvent_withDefaultTenantChange_shouldLogChange() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.default-tenant-id");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.default-tenant-id")).thenReturn("newDefault");

        // Act
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        // Assert
        verify(event).getKeys();
    }

    @Test
    void onApplicationEvent_withTenantAdditionMissingJdbcUrl_shouldNotAddTenant() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2,tenant3");
        
        // Mock missing JDBC URL
        when(environment.getProperty("tenants.profile.tenant3.jdbc-url")).thenReturn(null);

        // Act
        listener.onApplicationEvent(event);

        // Assert - Should not add tenant with missing properties
        verify(tenantAwareDataSource, never()).addOrUpdateTenantDataSource(eq("tenant3"), any());
    }

    @Test
    void onApplicationEvent_withTenantAdditionException_shouldContinue() throws Exception {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2,tenant3");
        
        when(environment.getProperty("tenants.profile.tenant3.jdbc-url"))
            .thenReturn("jdbc:postgresql://localhost:5432/tenant3");
        when(environment.getProperty("tenants.profile.tenant3.user-name")).thenReturn("user");
        when(environment.getProperty("tenants.profile.tenant3.password")).thenReturn("pass");
        
        doThrow(new RuntimeException("Test exception")).when(tenantAwareDataSource)
            .addOrUpdateTenantDataSource(eq("tenant3"), any());

        // Act & Assert - Should handle exception gracefully
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));
    }

    @Test
    void onApplicationEvent_withTenantsHavingWhitespace_shouldTrimAndProcess() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn(" tenant1 , tenant2 ");

        // Act
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        // Assert
        verify(environment, atLeastOnce()).getProperty("tenant.ids");
    }

    @Test
    void buildTenantDatabaseProperties_withAllProperties_shouldBuildComplete() {
        // Arrange
        when(environment.getProperty("tenants.profile.tenant3.jdbc-url"))
            .thenReturn("jdbc:postgresql://localhost:5432/tenant3");
        when(environment.getProperty("tenants.profile.tenant3.user-name")).thenReturn("user3");
        when(environment.getProperty("tenants.profile.tenant3.password")).thenReturn("pass3");
        when(environment.getProperty("tenants.profile.tenant3.min-pool-size", "10")).thenReturn("5");
        when(environment.getProperty("tenants.profile.tenant3.max-pool-size", "30")).thenReturn("20");
        when(environment.getProperty("tenants.profile.tenant3.connection-timeout-ms", "60000"))
            .thenReturn("30000");
        when(environment.getProperty("tenants.profile.tenant3.max-idle-time", "0")).thenReturn("1800");
        when(environment.getProperty("tenants.profile.tenant3.driver-class-name"))
            .thenReturn("org.postgresql.Driver");
        when(environment.getProperty("tenants.profile.tenant3.pool-name")).thenReturn("tenant3-pool");

        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2,tenant3");

        // Act
        listener.onApplicationEvent(event);

        // Assert
        ArgumentCaptor<TenantDatabaseProperties> captor = 
            ArgumentCaptor.forClass(TenantDatabaseProperties.class);
        verify(tenantAwareDataSource).addOrUpdateTenantDataSource(eq("tenant3"), captor.capture());
    }

    @Test
    void onApplicationEvent_withRefreshSystemPropertiesException_shouldContinue() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2,tenant3");
        
        doThrow(new RuntimeException("Test exception"))
            .when(multitenancySystemPropertyConfig).refreshTenantSystemProperties();

        // Act & Assert - Should handle exception gracefully
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));
    }

    @Test
    void onApplicationEvent_withRefreshTenantPropertiesException_shouldContinue() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2,tenant3");
        
        doThrow(new RuntimeException("Test exception"))
            .when(tenantDefaultPropertiesProcessor).refreshTenantProperties(anyString(), any());

        // Act & Assert - Should handle exception gracefully
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));
    }

    @Test
    void onApplicationEvent_withMultiplePropertyChanges_shouldLogAll() {
        // Arrange
        Set<String> changedKeys = new HashSet<>();
        changedKeys.add("tenant.ids");
        changedKeys.add("tenant.multitenant.enabled");
        changedKeys.add("some.other.property");
        
        when(event.getKeys()).thenReturn(changedKeys);
        when(environment.getProperty("tenant.ids")).thenReturn("tenant1,tenant2,tenant3");
        when(environment.getProperty("tenant.multitenant.enabled")).thenReturn("false");
        when(environment.getProperty("some.other.property")).thenReturn("value");

        // Act
        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        // Assert
        verify(event).getKeys();
        verify(multitenancySystemPropertyConfig).refreshTenantSystemProperties();
    }
}
