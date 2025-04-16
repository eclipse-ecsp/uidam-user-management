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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.config.EmailNotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationConfig;
import org.eclipse.ecsp.uidam.usermanagement.constants.NotificationConstants;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * internal notification config resolver fetch config file contains notification template.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "notification.config.resolver", havingValue = "internal", matchIfMissing = true)
public class InternalNotificationConfigResolver implements NotificationConfigResolver {

    private static final Map<String, NotificationConfig> NOTIFICATION_CONFIG_MAP = new HashMap<>();

    @Value("${notification.config.path:classpath:notification-config.json}")
    private String notificationConfigPath;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * load the config during application startup.
     *
     * @throws IOException if error occurred during fetching config file
     */
    @PostConstruct
    public void prepareNotificationConfig() throws IOException {
        Resource resource = resourceLoader.getResource(notificationConfigPath);
        if (resource.exists()) {
            List<NotificationConfig> notificationConfig = objectMapper.readValue(resource.getContentAsByteArray(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfig.class));
            notificationConfig.stream().forEach(i -> NOTIFICATION_CONFIG_MAP.put(i.getNotificationId(), i));
        } else {
            throw new FileNotFoundException("notification config not found: " + notificationConfigPath);
        }

    }

    @Override
    public Optional<NotificationConfig> getConfig(String notificationId) {
        return Optional.ofNullable(getNotificationConfig(notificationId));
    }

    @Override
    public List<String> getNotificationChannels(String notificationId) {
        NotificationConfig notificationConfig = getNotificationConfig(notificationId);
        List<String> channels = new ArrayList<>();
        if (null != notificationConfig && notificationConfig.getEnabled()
                && null != notificationConfig.getEmail()
                && Boolean.TRUE.equals(notificationConfig.getEmail().getEnabled())) {
            channels.add(NotificationConstants.EMAIL);
        }
        return channels;
    }

    @Override
    public Optional<EmailNotificationTemplateConfig> getEmailTemplate(String notificationId, String userLocale) {
        Optional<NotificationConfig> notificationConfigObj = getConfig(notificationId);
        if (notificationConfigObj.isPresent()) {
            NotificationConfig notificationConfig = notificationConfigObj.get();
            NotificationConfig.EmailConfig emailConfig = notificationConfig.getEmail();
            if (null != emailConfig && emailConfig.getEnabled()
                    && !CollectionUtils.isEmpty(emailConfig.getTemplates())) {
                EmailNotificationTemplateConfig template = emailConfig.getTemplates()
                        .stream().filter(i -> i.getLocale().equals(userLocale)).findFirst()
                        .or(() -> emailConfig.getTemplates().stream()
                                .filter(EmailNotificationTemplateConfig::isDefault)
                                .findFirst())
                        .orElse(null);
                return Optional.ofNullable(template);
            } else {
                log.error("Notification config not found or email channel is not enabled for notificationId: {},"
                        + " cannot proceed further..", notificationId);
            }
        } else {
            log.error("Notification config not found for notificationId: {}, cannot proceed further..", notificationId);
        }
        return Optional.empty();
    }

    private NotificationConfig getNotificationConfig(String notificationId) {
        NotificationConfig notificationConfigObj = NOTIFICATION_CONFIG_MAP.get(notificationId);
        if (Objects.isNull(notificationConfigObj)) {
            log.error("Notification config not found for notificationId: {}", notificationId);
        }
        return notificationConfigObj;
    }


}
