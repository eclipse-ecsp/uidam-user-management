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

package org.eclipse.ecsp.uidam.usermanagement.config;

import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantAwareNotificationTemplateConfigFactory.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAwareNotificationTemplateConfigFactory Test Suite")
class TenantAwareNotificationTemplateConfigFactoryTest {

    private TenantAwareNotificationTemplateConfigFactory factory;

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private UserManagementTenantProperties tenantProperties;

    @Mock
    private NotificationProperties notificationProperties;

    @Mock
    private NotificationProperties.TemplateEngineProperties templateEngineProperties;

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        factory = new TenantAwareNotificationTemplateConfigFactory(tenantConfigurationService);
        tenantContextMock = mockStatic(TenantContext.class);
    }

    @AfterEach
    void tearDown() {
        if (tenantContextMock != null) {
            tenantContextMock.close();
        }
    }

    @Test
    @DisplayName("Should create config with all properties from tenant configuration")
    void testGetConfigWithCompleteConfiguration() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        when(notificationProperties.getTemplate()).thenReturn(templateEngineProperties);
        when(templateEngineProperties.getResolver()).thenReturn("FILE");
        when(templateEngineProperties.getFormat()).thenReturn("XML");
        when(templateEngineProperties.getPrefix()).thenReturn("/custom/");
        when(templateEngineProperties.getSuffix()).thenReturn(".xml");

        // Act
        NotificationTemplateConfig config = factory.getConfig();

        // Assert
        assertNotNull(config);
        assertEquals(NotificationTemplateConfig.Resolver.FILE, config.getResolver());
        assertEquals(NotificationTemplateConfig.Format.XML, config.getFormat());
        assertEquals("/custom/", config.getPrefix());
        assertEquals(".xml", config.getSuffix());
    }

    @Test
    @DisplayName("Should create default config when template properties are null")
    void testGetConfigWithNullTemplateProperties() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        when(notificationProperties.getTemplate()).thenReturn(null);

        // Act
        NotificationTemplateConfig config = factory.getConfig();

        // Assert
        assertNotNull(config);
        assertEquals(NotificationTemplateConfig.Resolver.CLASSPATH, config.getResolver());
        assertEquals(NotificationTemplateConfig.Format.HTML, config.getFormat());
        assertEquals("/notification/", config.getPrefix());
        assertEquals(".html", config.getSuffix());
    }

    @Test
    @DisplayName("Should throw exception when tenant ID is null")
    void testGetConfigWithNullTenantId() {
        // Arrange
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> factory.getConfig());
        assertEquals("Tenant ID not found in TenantContext", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when tenant properties not found")
    void testGetConfigWithNullTenantProperties() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> factory.getConfig());
        assertEquals("No tenant properties found for tenant: tenant1. "
                + "Tenant must be configured in application properties.", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle partial template properties - resolver only")
    void testGetConfigWithResolverOnly() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        when(notificationProperties.getTemplate()).thenReturn(templateEngineProperties);
        when(templateEngineProperties.getResolver()).thenReturn("URL");
        when(templateEngineProperties.getFormat()).thenReturn(null);
        when(templateEngineProperties.getPrefix()).thenReturn(null);
        when(templateEngineProperties.getSuffix()).thenReturn(null);

        // Act
        NotificationTemplateConfig config = factory.getConfig();

        // Assert
        assertNotNull(config);
        assertEquals(NotificationTemplateConfig.Resolver.URL, config.getResolver());
    }

    @Test
    @DisplayName("Should handle partial template properties - format only")
    void testGetConfigWithFormatOnly() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        when(notificationProperties.getTemplate()).thenReturn(templateEngineProperties);
        when(templateEngineProperties.getResolver()).thenReturn(null);
        when(templateEngineProperties.getFormat()).thenReturn("TEXT");
        when(templateEngineProperties.getPrefix()).thenReturn(null);
        when(templateEngineProperties.getSuffix()).thenReturn(null);

        // Act
        NotificationTemplateConfig config = factory.getConfig();

        // Assert
        assertNotNull(config);
        assertEquals(NotificationTemplateConfig.Format.TEXT, config.getFormat());
    }

    @Test
    @DisplayName("Should handle partial template properties - prefix only")
    void testGetConfigWithPrefixOnly() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        when(notificationProperties.getTemplate()).thenReturn(templateEngineProperties);
        when(templateEngineProperties.getResolver()).thenReturn(null);
        when(templateEngineProperties.getFormat()).thenReturn(null);
        when(templateEngineProperties.getPrefix()).thenReturn("/email/");
        when(templateEngineProperties.getSuffix()).thenReturn(null);

        // Act
        NotificationTemplateConfig config = factory.getConfig();

        // Assert
        assertNotNull(config);
        assertEquals("/email/", config.getPrefix());
    }

    @Test
    @DisplayName("Should handle partial template properties - suffix only")
    void testGetConfigWithSuffixOnly() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        when(notificationProperties.getTemplate()).thenReturn(templateEngineProperties);
        when(templateEngineProperties.getResolver()).thenReturn(null);
        when(templateEngineProperties.getFormat()).thenReturn(null);
        when(templateEngineProperties.getPrefix()).thenReturn(null);
        when(templateEngineProperties.getSuffix()).thenReturn(".txt");

        // Act
        NotificationTemplateConfig config = factory.getConfig();

        // Assert
        assertNotNull(config);
        assertEquals(".txt", config.getSuffix());
    }

    @Test
    @DisplayName("Should handle lowercase resolver enum values")
    void testGetConfigWithLowercaseResolver() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        when(notificationProperties.getTemplate()).thenReturn(templateEngineProperties);
        when(templateEngineProperties.getResolver()).thenReturn("classpath");
        when(templateEngineProperties.getFormat()).thenReturn(null);
        when(templateEngineProperties.getPrefix()).thenReturn(null);
        when(templateEngineProperties.getSuffix()).thenReturn(null);

        // Act
        NotificationTemplateConfig config = factory.getConfig();

        // Assert
        assertNotNull(config);
        assertEquals(NotificationTemplateConfig.Resolver.CLASSPATH, config.getResolver());
    }

    @Test
    @DisplayName("Should handle lowercase format enum values")
    void testGetConfigWithLowercaseFormat() {
        // Arrange
        String tenantId = "tenant1";
        tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn(tenantId);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        when(notificationProperties.getTemplate()).thenReturn(templateEngineProperties);
        when(templateEngineProperties.getResolver()).thenReturn(null);
        when(templateEngineProperties.getFormat()).thenReturn("html");
        when(templateEngineProperties.getPrefix()).thenReturn(null);
        when(templateEngineProperties.getSuffix()).thenReturn(null);

        // Act
        NotificationTemplateConfig config = factory.getConfig();

        // Assert
        assertNotNull(config);
        assertEquals(NotificationTemplateConfig.Format.HTML, config.getFormat());
    }
}
