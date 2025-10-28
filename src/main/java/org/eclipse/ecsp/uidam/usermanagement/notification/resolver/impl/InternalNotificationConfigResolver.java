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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.config.EmailNotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationConfig;
import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.constants.NotificationConstants;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * internal notification config resolver fetch config file contains notification template.
 * Now supports multi-tenancy by loading tenant-specific notification configurations.
 */
@Slf4j
@Component
public class InternalNotificationConfigResolver implements NotificationConfigResolver {

    // Cache for tenant-specific notification configs: tenantId -> (notificationId -> NotificationConfig)
    private final Map<String, Map<String, NotificationConfig>> tenantNotificationConfigs = new ConcurrentHashMap<>();

    private final TenantConfigurationService tenantConfigurationService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    /**
     * Constructor for InternalNotificationConfigResolver.
     *
     * @param tenantConfigurationService the tenant configuration service
     * @param objectMapper the object mapper for JSON parsing
     * @param resourceLoader the resource loader for loading notification configs
     */
    public InternalNotificationConfigResolver(TenantConfigurationService tenantConfigurationService,
                                             ObjectMapper objectMapper,
                                             ResourceLoader resourceLoader) {
        this.tenantConfigurationService = tenantConfigurationService;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Load notification config for the current tenant (lazy initialization).
     *
     * @param tenantId the tenant ID
     * @return Map of notification configs for the tenant
     */
    private Map<String, NotificationConfig> getOrLoadTenantNotificationConfig(String tenantId) {
        return tenantNotificationConfigs.computeIfAbsent(tenantId, tid -> {
            try {
                // Get tenant-specific notification config path
                String configPath = tenantConfigurationService.getTenantProperties()
                        .getNotification()
                        .getConfig()
                        .getPath();
                
                log.debug("Loading notification config for tenant '{}' from path: {}", tid, configPath);
                
                Resource resource = resourceLoader.getResource(configPath);
                if (!resource.exists()) {
                    log.error("Notification config not found for tenant '{}' at path: {}", tid, configPath);
                    return new HashMap<>();
                }
                
                List<NotificationConfig> notificationConfigs = objectMapper.readValue(
                        resource.getContentAsByteArray(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfig.class));
                
                Map<String, NotificationConfig> configMap = new HashMap<>();
                notificationConfigs.forEach(config -> configMap.put(config.getNotificationId(), config));
                
                log.info("Loaded {} notification configs for tenant '{}'", configMap.size(), tid);
                return configMap;
                
            } catch (IOException e) {
                log.error("Failed to load notification config for tenant '{}': {}", tid, e.getMessage(), e);
                return new HashMap<>();
            }
        });
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

    /**
     * Get notification config for the current tenant.
     *
     * @param notificationId the notification ID
     * @return NotificationConfig or null if not found
     */
    private NotificationConfig getNotificationConfig(String notificationId) {
        String tenantId = TenantContext.getCurrentTenant();
        Map<String, NotificationConfig> tenantConfigs = getOrLoadTenantNotificationConfig(tenantId);
        
        NotificationConfig notificationConfigObj = tenantConfigs.get(notificationId);
        if (Objects.isNull(notificationConfigObj)) {
            log.error("Notification config not found for notificationId: {} in tenant: {}", 
                     notificationId, tenantId);
        }
        return notificationConfigObj;
    }
}
