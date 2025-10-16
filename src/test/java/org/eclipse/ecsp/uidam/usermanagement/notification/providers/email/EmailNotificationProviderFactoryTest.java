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

package org.eclipse.ecsp.uidam.usermanagement.notification.providers.email;

import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.impl.IgniteEmailNotificationProvider;
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.impl.InternalEmailNotificationProvider;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailNotificationProviderFactory.
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationProviderFactoryTest {

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private InternalEmailNotificationProvider internalProvider;

    @Mock
    private IgniteEmailNotificationProvider igniteProvider;

    @InjectMocks
    private EmailNotificationProviderFactory factory;

    private static final String TENANT_ECSP = "ecsp";
    private static final String TENANT_SDP = "sdp";

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGetProvider_shouldReturnInternalWhenConfiguredAsInternal() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.EmailProviderProperties emailProps = 
                mock(NotificationProperties.EmailProviderProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getEmail()).thenReturn(emailProps);
        when(emailProps.getProvider()).thenReturn("internal");

        final EmailNotificationProvider result = factory.getProvider();

        assertNotNull(result);
        assertSame(internalProvider, result);
    }

    @Test
    void testGetProvider_shouldReturnIgniteWhenConfiguredAsIgnite() {
        TenantContext.setCurrentTenant(TENANT_SDP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.EmailProviderProperties emailProps =
                mock(NotificationProperties.EmailProviderProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getEmail()).thenReturn(emailProps);
        when(emailProps.getProvider()).thenReturn("ignite");

        final EmailNotificationProvider result = factory.getProvider();

        assertNotNull(result);
        assertSame(igniteProvider, result);
    }

    @Test
    void testGetProvider_shouldReturnInternalWhenProviderNotSpecified() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.EmailProviderProperties emailProps =
                mock(NotificationProperties.EmailProviderProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getEmail()).thenReturn(emailProps);
        when(emailProps.getProvider()).thenReturn(null);

        final EmailNotificationProvider result = factory.getProvider();

        assertNotNull(result);
        assertSame(internalProvider, result);
    }

    @Test
    void testGetProvider_shouldThrowExceptionWhenInvalidProviderType() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.EmailProviderProperties emailProps =
                mock(NotificationProperties.EmailProviderProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getEmail()).thenReturn(emailProps);
        when(emailProps.getProvider()).thenReturn("invalid-provider");

        final ApplicationRuntimeException exception = assertThrows(
                ApplicationRuntimeException.class,
                () -> factory.getProvider()
        );

        assertTrue(exception.getMessage().contains("Unsupported email provider type"));
        assertTrue(exception.getMessage().contains("invalid-provider"));
    }
}
