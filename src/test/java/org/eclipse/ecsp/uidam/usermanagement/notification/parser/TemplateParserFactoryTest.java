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

package org.eclipse.ecsp.uidam.usermanagement.notification.parser;

import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.impl.MustacheTemplateParserImpl;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.impl.ThymeleafTemplateParserImpl;
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
 * Unit tests for TemplateParserFactory.
 */
@ExtendWith(MockitoExtension.class)
class TemplateParserFactoryTest {

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private ThymeleafTemplateParserImpl thymeleafParser;

    @Mock
    private MustacheTemplateParserImpl mustacheParser;

    @InjectMocks
    private TemplateParserFactory factory;

    private static final String TENANT_ECSP = "ecsp";
    private static final String TENANT_SDP = "sdp";

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGetParser_shouldReturnThymeleafWhenConfiguredAsThymeleaf() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.TemplateEngineProperties templateProps =
                mock(NotificationProperties.TemplateEngineProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getTemplate()).thenReturn(templateProps);
        when(templateProps.getEngine()).thenReturn("thymeleaf");

        final TemplateParser result = factory.getParser();

        assertNotNull(result);
        assertSame(thymeleafParser, result);
    }

    @Test
    void testGetParser_shouldReturnMustacheWhenConfiguredAsMustache() {
        TenantContext.setCurrentTenant(TENANT_SDP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.TemplateEngineProperties templateProps =
                mock(NotificationProperties.TemplateEngineProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getTemplate()).thenReturn(templateProps);
        when(templateProps.getEngine()).thenReturn("mustache");

        final TemplateParser result = factory.getParser();

        assertNotNull(result);
        assertSame(mustacheParser, result);
    }

    @Test
    void testGetParser_shouldReturnMustacheWhenEngineNotSpecified() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.TemplateEngineProperties templateProps =
                mock(NotificationProperties.TemplateEngineProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getTemplate()).thenReturn(templateProps);
        when(templateProps.getEngine()).thenReturn(null);

        final TemplateParser result = factory.getParser();

        assertNotNull(result);
        assertSame(mustacheParser, result);
    }

    @Test
    void testGetParser_shouldReturnMustacheWhenTemplateConfigNull() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getTemplate()).thenReturn(null);

        final TemplateParser result = factory.getParser();

        assertNotNull(result);
        assertSame(mustacheParser, result);
    }

    @Test
    void testGetParser_shouldReturnMustacheWhenUnknownEngine() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.TemplateEngineProperties templateProps =
                mock(NotificationProperties.TemplateEngineProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getTemplate()).thenReturn(templateProps);
        when(templateProps.getEngine()).thenReturn("unknown-engine");

        final TemplateParser result = factory.getParser();

        assertNotNull(result);
        assertSame(mustacheParser, result);
    }

    @Test
    void testGetParser_shouldHandleCaseInsensitiveEngineType() {
        TenantContext.setCurrentTenant(TENANT_ECSP);
        final UserManagementTenantProperties tenantProps = mock(UserManagementTenantProperties.class);
        final NotificationProperties notifProps = mock(NotificationProperties.class);
        final NotificationProperties.TemplateEngineProperties templateProps =
                mock(NotificationProperties.TemplateEngineProperties.class);

        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
        when(tenantProps.getNotification()).thenReturn(notifProps);
        when(notifProps.getTemplate()).thenReturn(templateProps);
        when(templateProps.getEngine()).thenReturn("THYMELEAF");

        final TemplateParser result = factory.getParser();

        assertNotNull(result);
        assertSame(thymeleafParser, result);
    }
}
