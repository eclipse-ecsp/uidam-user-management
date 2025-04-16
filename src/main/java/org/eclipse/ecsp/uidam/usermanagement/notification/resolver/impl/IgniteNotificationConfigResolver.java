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
import org.eclipse.ecsp.uidam.usermanagement.constants.NotificationConstants;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

/**
 * ]
 * notifcation config resolve which fetch the configuration from notification center.
 */
@Component
@ConditionalOnProperty(name = "notification.config.resolver", havingValue = "ignite")
public class IgniteNotificationConfigResolver implements NotificationConfigResolver {

    @Override
    public Optional<NotificationConfig> getConfig(String notificationId) {
        return Optional.empty();
    }

    @Override
    public List<String> getNotificationChannels(String notificationId) {
        //call notification api to get the channel enable for the notificationId
        return List.of(NotificationConstants.EMAIL);
    }

    @Override
    public Optional<EmailNotificationTemplateConfig> getEmailTemplate(String notificationId,
                                                                      String locale) {
        // handled by notification center
        return Optional.empty();
    }
}
