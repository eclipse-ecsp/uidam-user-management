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

package org.eclipse.ecsp.uidam.usermanagement.notification.resolver;

import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.impl.IgniteNotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.impl.InternalNotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NotificationConfigResolverFactory.
 */
@ExtendWith(MockitoExtension.class)
class NotificationConfigResolverFactoryTest {

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private InternalNotificationConfigResolver internalResolver;

    @Mock
    private IgniteNotificationConfigResolver igniteResolver;

    @InjectMocks
    private NotificationConfigResolverFactory factory;

    private static final String TENANT_ECSP = "ecsp";
    private static final String TENANT_SDP = "sdp";

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGetResolver_shouldReturnInternalWhenConfiguredAsInternal() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.NotificationConfigProperties configProps =
                mock(NotificationProperties.NotificationConfigProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getConfig()).thenReturn(configProps);
        when(configProps.getResolver()).thenReturn("internal");

        final NotificationConfigResolver result = factory.getResolver();

        assertNotNull(result);
        assertSame(internalResolver, result);
    }

    @Test
    void testGetResolver_shouldReturnIgniteWhenConfiguredAsIgnite() {
        TenantContext.setCurrentTenant(TENANT_SDP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.NotificationConfigProperties configProps =
                mock(NotificationProperties.NotificationConfigProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getConfig()).thenReturn(configProps);
        when(configProps.getResolver()).thenReturn("ignite");

        final NotificationConfigResolver result = factory.getResolver();

        assertNotNull(result);
        assertSame(igniteResolver, result);
    }

    @Test
    void testGetResolver_shouldReturnInternalWhenResolverNotSpecified() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.NotificationConfigProperties configProps =
                mock(NotificationProperties.NotificationConfigProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getConfig()).thenReturn(configProps);
        when(configProps.getResolver()).thenReturn(null);

        final NotificationConfigResolver result = factory.getResolver();

        assertNotNull(result);
        assertSame(internalResolver, result);
    }

    @Test
    void testGetResolver_shouldReturnInternalWhenConfigPropsNull() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getConfig()).thenReturn(null);

        final NotificationConfigResolver result = factory.getResolver();

        assertNotNull(result);
        assertSame(internalResolver, result);
    }

    @Test
    void testGetResolver_shouldReturnInternalWhenUnknownResolverType() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.NotificationConfigProperties configProps =
                mock(NotificationProperties.NotificationConfigProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getConfig()).thenReturn(configProps);
        when(configProps.getResolver()).thenReturn("unknown-resolver");

        final NotificationConfigResolver result = factory.getResolver();

        assertNotNull(result);
        assertSame(internalResolver, result);
    }

    @Test
    void testGetResolver_shouldHandleCaseInsensitiveResolverType() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.NotificationConfigProperties configProps =
                mock(NotificationProperties.NotificationConfigProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getConfig()).thenReturn(configProps);
        when(configProps.getResolver()).thenReturn("IGNITE");

        final NotificationConfigResolver result = factory.getResolver();

        assertNotNull(result);
        assertSame(igniteResolver, result);
    }
}
