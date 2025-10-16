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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.stereotype.Component;

/**
 * Factory for creating tenant-aware NotificationTemplateConfig instances.
 * Reads configuration from tenant-specific properties.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantAwareNotificationTemplateConfigFactory {

    private final TenantConfigurationService tenantConfigurationService;

    /**
     * Creates a NotificationTemplateConfig instance based on current tenant's configuration.
     *
     * @return NotificationTemplateConfig populated with tenant-specific values
     */
    public NotificationTemplateConfig getConfig() {
        String tenantId = TenantContext.getCurrentTenant();
        NotificationProperties.TemplateEngineProperties templateProps = 
            tenantConfigurationService.getTenantProperties().getNotification().getTemplate();

        if (templateProps == null) {
            log.warn("Template configuration not found for tenant '{}', using defaults", tenantId);
            return createDefaultConfig();
        }

        NotificationTemplateConfig config = new NotificationTemplateConfig();
        
        // Set resolver
        if (templateProps.getResolver() != null) {
            config.setResolver(NotificationTemplateConfig.Resolver.valueOf(
                templateProps.getResolver().toUpperCase()));
        }
        
        // Set format
        if (templateProps.getFormat() != null) {
            config.setFormat(NotificationTemplateConfig.Format.valueOf(
                templateProps.getFormat().toUpperCase()));
        }
        
        // Set prefix and suffix
        if (templateProps.getPrefix() != null) {
            config.setPrefix(templateProps.getPrefix());
        }
        if (templateProps.getSuffix() != null) {
            config.setSuffix(templateProps.getSuffix());
        }
        
        log.debug("Created template config for tenant '{}': resolver={}, format={}, prefix={}, suffix={}",
            tenantId, config.getResolver(), config.getFormat(), config.getPrefix(), config.getSuffix());
        
        return config;
    }

    /**
     * Creates a default NotificationTemplateConfig with standard values.
     *
     * @return NotificationTemplateConfig with default values
     */
    private NotificationTemplateConfig createDefaultConfig() {
        NotificationTemplateConfig config = new NotificationTemplateConfig();
        config.setResolver(NotificationTemplateConfig.Resolver.CLASSPATH);
        config.setFormat(NotificationTemplateConfig.Format.HTML);
        config.setPrefix("/notification/");
        config.setSuffix(".html");
        return config;
    }
}
