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

package org.eclipse.ecsp.uidam.usermanagement.notification.resolver.impl;

import org.eclipse.ecsp.uidam.usermanagement.config.EmailNotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationConfig;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

/**
 * Notification config resolver which fetches the configuration from notification center (Ignite).
 * Supports multi-tenancy - each tenant can independently choose to use Ignite for notification config resolution.
 *
 * <p><b>Note:</b> This implementation is currently not supported. Template configuration is handled
 * directly by the Ignite Notification Center API. If you need to use Ignite for notifications,
 * configure the email provider as 'ignite' instead.
 *
 * @see org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.impl.IgniteEmailNotificationProvider
 */
@Component
public class IgniteNotificationConfigResolver implements NotificationConfigResolver {

    /**
     * Get notification configuration by ID.
     *
     * @param notificationId the notification ID
     * @return Optional of NotificationConfig
     * @throws UnsupportedOperationException this operation is not supported for Ignite resolver
     */
    @Override
    public Optional<NotificationConfig> getConfig(String notificationId) {
        throw new UnsupportedOperationException(
                "getConfig is not supported for Ignite resolver. "
                + "Template configuration is handled by Ignite Notification Center API.");
    }

    /**
     * Get notification channels for a notification ID.
     *
     * @param notificationId the notification ID
     * @return List of notification channels
     * @throws UnsupportedOperationException this operation is not supported for Ignite resolver
     */
    @Override
    public List<String> getNotificationChannels(String notificationId) {
        throw new UnsupportedOperationException(
                "getNotificationChannels is not supported for Ignite resolver. "
                + "Use IgniteEmailNotificationProvider directly for Ignite-based notifications.");
    }

    /**
     * Get email template configuration.
     *
     * @param notificationId the notification ID
     * @param locale the user locale
     * @return Optional of EmailNotificationTemplateConfig
     * @throws UnsupportedOperationException this operation is not supported for Ignite resolver
     */
    @Override
    public Optional<EmailNotificationTemplateConfig> getEmailTemplate(String notificationId,
                                                                      String locale) {
        throw new UnsupportedOperationException(
                "getEmailTemplate is not supported for Ignite resolver. "
                + "Templates are managed and rendered by Ignite Notification Center.");
    }
}
