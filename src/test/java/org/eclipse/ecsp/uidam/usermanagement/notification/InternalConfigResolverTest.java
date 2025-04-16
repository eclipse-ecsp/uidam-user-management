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
import jakarta.mail.MessagingException;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.usermanagement.config.EmailNotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationConfig;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * test resolving the notification config using internal impl.
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = InternalConfigResolverTest.AppConfig.class)
@TestPropertySource(properties = "notification.config.resolver=internal")
@TestPropertySource("classpath:application-notification.properties")
@MockBean(AccountRepository.class)
class InternalConfigResolverTest {

    @Autowired
    private NotificationConfigResolver notificationConfigResolver;

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
    public void setup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void testGetConfigSuccess() throws MessagingException, IOException {
        Optional<NotificationConfig> config = notificationConfigResolver.getConfig(
                "UIDAM_USER_VERIFY_ACCOUNT");
        assertTrue(config.isPresent());
    }

    @Test
    void testSuccess() throws MessagingException, IOException {
        List<String> channels = notificationConfigResolver.getNotificationChannels(
                "UIDAM_USER_VERIFY_ACCOUNT");
        assertFalse(channels.isEmpty());
        assertEquals(1, channels.size());
        assertEquals("email", channels.get(0));
    }

    @Test
    void testTemplateByUserLocale() throws MessagingException {
        Optional<EmailNotificationTemplateConfig> config = notificationConfigResolver.getEmailTemplate(
                "UIDAM_USER_VERIFY_ACCOUNT", "en_IN");
        assertTrue(config.isPresent());
        EmailNotificationTemplateConfig template = config.get();
        assertEquals("en_IN", template.getLocale());
    }

    @Test
    void testTemplateByDefaultLocale() throws MessagingException {
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
