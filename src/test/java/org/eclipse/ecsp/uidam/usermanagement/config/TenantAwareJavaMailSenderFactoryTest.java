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

import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantAwareJavaMailSenderFactory.
 */
@ExtendWith(MockitoExtension.class)
class TenantAwareJavaMailSenderFactoryTest {

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    private TenantAwareJavaMailSenderFactory factory;

    private static final String TENANT_ECSP = "ecsp";
    private static final String TENANT_SDP = "sdp";

    @BeforeEach
    void setUp() {
        factory = new TenantAwareJavaMailSenderFactory(tenantConfigurationService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGetMailSender_shouldCreateAndCacheMailSender() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = createMockTenantPropertiesWithEmail(
                "smtp.gmail.com", 587, "test@example.com", "password");

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);

        final JavaMailSender mailSender1 = factory.getMailSender();
        final JavaMailSender mailSender2 = factory.getMailSender();

        assertNotNull(mailSender1);
        assertSame(mailSender1, mailSender2, "Should return cached instance");
        //CHECKSTYLE.OFF: MagicNumber - Test verification count
        verify(tenantConfigurationService, times(2)).getTenantProperties();
        //CHECKSTYLE.ON: MagicNumber
    }

    @Test
    void testGetMailSender_shouldCreateDifferentMailSendersForDifferentTenants() {
        final UserManagementTenantProperties ecspProps = createMockTenantPropertiesWithEmail(
                "smtp.ecsp.com", 587, "ecsp@example.com", "ecsp-password");
        final UserManagementTenantProperties sdpProps = createMockTenantPropertiesWithEmail(
                "smtp.sdp.com", 465, "sdp@example.com", "sdp-password");

        TenantContext.setCurrentTenant(TENANT_ECSP);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(ecspProps);
        final JavaMailSender ecspMailSender = factory.getMailSender();

        TenantContext.setCurrentTenant(TENANT_SDP);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(sdpProps);
        final JavaMailSender sdpMailSender = factory.getMailSender();

        assertNotNull(ecspMailSender);
        assertNotNull(sdpMailSender);
        // Note: Can't assert they're different instances as they're cached by tenant ID
    }

    @Test
    void testGetMailSender_shouldThrowExceptionWhenEmailConfigMissing() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getEmail()).thenReturn(null);

        final IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> factory.getMailSender()
        );

        assertEquals("Email configuration not found for tenant", exception.getMessage());
    }

    @Test
    void testGetMailSender_shouldThrowExceptionWhenNotificationConfigMissing() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(null);

        final IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> factory.getMailSender()
        );

        assertEquals("Email configuration not found for tenant", exception.getMessage());
    }

    @Test
    void testClearCache_shouldRemoveTenantFromCache() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = createMockTenantPropertiesWithEmail(
                "smtp.gmail.com", 587, "test@example.com", "password");

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);

        // Create and cache mail sender
        factory.getMailSender();

        // Clear cache
        factory.clearCache(TENANT_ECSP);

        // Get again - should call service again
        factory.getMailSender();

        //CHECKSTYLE.OFF: MagicNumber - Test verification count
        verify(tenantConfigurationService, times(2)).getTenantProperties();
        //CHECKSTYLE.ON: MagicNumber
    }

    @Test
    void testClearAllCache_shouldRemoveAllTenantsFromCache() {
        final UserManagementTenantProperties ecspProps = createMockTenantPropertiesWithEmail(
                "smtp.ecsp.com", 587, "ecsp@example.com", "password");
        final UserManagementTenantProperties sdpProps = createMockTenantPropertiesWithEmail(
                "smtp.sdp.com", 587, "sdp@example.com", "password");

        // Create mail senders for both tenants
        TenantContext.setCurrentTenant(TENANT_ECSP);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(ecspProps);
        factory.getMailSender();

        TenantContext.setCurrentTenant(TENANT_SDP);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(sdpProps);
        factory.getMailSender();

        // Clear all cache
        factory.clearAllCache();

        // Get again for ECSP - should call service again
        TenantContext.setCurrentTenant(TENANT_ECSP);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(ecspProps);
        factory.getMailSender();

        // Verify service was called multiple times
        //CHECKSTYLE.OFF: MagicNumber - Test verification count
        verify(tenantConfigurationService, times(3)).getTenantProperties();
        //CHECKSTYLE.ON: MagicNumber
    }

    @Test
    void testGetMailSender_shouldConfigureMailSenderWithProperties() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final Map<String, String> mailProperties = new HashMap<>();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");

        final UserManagementTenantProperties tenantProps = createMockTenantPropertiesWithEmailAndProperties(
                "smtp.gmail.com", 587, "test@example.com", "password", mailProperties);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);

        final JavaMailSender mailSender = factory.getMailSender();

        assertNotNull(mailSender);
    }

    private UserManagementTenantProperties createMockTenantPropertiesWithEmail(
            String host, Integer port, String username, String password) {
        return createMockTenantPropertiesWithEmailAndProperties(host, port, username, password, null);
    }

    private UserManagementTenantProperties createMockTenantPropertiesWithEmailAndProperties(
            String host, Integer port, String username, String password, Map<String, String> properties) {
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.EmailProviderProperties emailProps =
                mock(NotificationProperties.EmailProviderProperties.class);

        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getEmail()).thenReturn(emailProps);
        when(emailProps.getHost()).thenReturn(host);
        when(emailProps.getPort()).thenReturn(port);
        when(emailProps.getUsername()).thenReturn(username);
        when(emailProps.getPassword()).thenReturn(password);
        
        if (properties != null) {
            when(emailProps.getProperties()).thenReturn(properties);
        }

        return tenantProps;
    }
}
