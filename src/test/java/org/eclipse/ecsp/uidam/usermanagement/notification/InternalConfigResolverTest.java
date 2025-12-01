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

package org.eclipse.ecsp.uidam.usermanagement.notification;

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.config.EmailNotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationConfig;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.impl.InternalNotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * test resolving the notification config using internal impl with multi-tenant support.
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = InternalConfigResolverTest.AppConfig.class)
@TestPropertySource(properties = "notification.config.resolver=internal")
@TestPropertySource("classpath:application-notification.properties")
@MockBean(AccountRepository.class)
@org.springframework.test.context.TestExecutionListeners(
    listeners = org.eclipse.ecsp.uidam.common.test.TenantContextTestExecutionListener.class,
    mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@org.springframework.context.annotation.Import(org.eclipse.ecsp.uidam.common.test.TestTenantConfiguration.class)
class InternalConfigResolverTest {

    @Autowired
    @Qualifier("internalNotificationConfigResolver")
    private NotificationConfigResolver notificationConfigResolver;

    @MockBean
    PasswordValidationService passwordValidationService;
    
    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    @MockBean
    TenantConfigurationService tenantConfigurationService;
    
    /**
     * application test config.
     */
    @Configuration
    @ComponentScan("org.eclipse.ecsp")
    @ImportAutoConfiguration(JacksonAutoConfiguration.class)
    public static class AppConfig {
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }
    
    @BeforeEach
    void setup() {
        // Set up tenant context
        TenantContext.setCurrentTenant("ecsp");
        
        // Configure notification config path for ECSP tenant
        org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
            .NotificationConfigProperties configProps = new org.eclipse.ecsp.uidam.usermanagement.config
            .tenantproperties.NotificationProperties.NotificationConfigProperties();
        configProps.setPath("classpath:/notification/uidam-notification-config.json");
        
        org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
            notificationProps = new org.eclipse.ecsp.uidam.usermanagement.config
            .tenantproperties.NotificationProperties();
        notificationProps.setConfig(configProps);
        
        org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties
            tenantProps = new org.eclipse.ecsp.uidam.usermanagement.config
            .tenantproperties.UserManagementTenantProperties();
        tenantProps.setNotification(notificationProps);
        
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProps);
    }

    @Test
    void testGetConfigSuccess() {
        Optional<NotificationConfig> config = notificationConfigResolver.getConfig(
                "UIDAM_USER_VERIFY_ACCOUNT");
        assertTrue(config.isPresent());
    }

    @Test
    void testSuccess() {
        List<String> channels = notificationConfigResolver.getNotificationChannels(
                "UIDAM_USER_VERIFY_ACCOUNT");
        assertFalse(channels.isEmpty());
        assertEquals(1, channels.size());
        assertEquals("email", channels.get(0));
    }

    @Test
    void testTemplateByUserLocale() {
        Optional<EmailNotificationTemplateConfig> config = notificationConfigResolver.getEmailTemplate(
                "UIDAM_USER_VERIFY_ACCOUNT", "en_IN");
        assertTrue(config.isPresent());
        EmailNotificationTemplateConfig template = config.get();
        assertEquals("en_IN", template.getLocale());
    }

    @Test
    void testTemplateByDefaultLocale() {
        Optional<EmailNotificationTemplateConfig> config = notificationConfigResolver.getEmailTemplate(
                "UIDAM_USER_VERIFY_ACCOUNT", "it_IT");
        assertTrue(config.isPresent());
        EmailNotificationTemplateConfig template = config.get();
        assertEquals("en_US", template.getLocale());
    }

    @Test
    void testInvalidNotificationId() {
        Optional<NotificationConfig> config = notificationConfigResolver.getConfig(
                "UIDAM_USER_VERIFY_ACCOUNT_1");
        assertFalse(config.isPresent());
    }

    @Test
    void testEmailChannelNotAvailable() {
        Optional<NotificationConfig> config = notificationConfigResolver.getConfig(
                "UIDAM_TEST_1UIDAM_TEST_1");
        assertFalse(config.isPresent());
    }

    @Test
    void testChannelsNotAvailable() {
        List<String> config = notificationConfigResolver.getNotificationChannels(
                "UIDAM_TEST_1UIDAM_TEST_1");
        assertTrue(config.isEmpty());
    }
}
