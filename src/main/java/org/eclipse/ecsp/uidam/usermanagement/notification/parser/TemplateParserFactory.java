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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.impl.MustacheTemplateParserImpl;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.impl.ThymeleafTemplateParserImpl;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the appropriate template parser based on tenant configuration.
 * Supports runtime selection between Thymeleaf and Mustache template engines.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateParserFactory {

    private final TenantConfigurationService tenantConfigurationService;
    private final ThymeleafTemplateParserImpl thymeleafParser;
    private final MustacheTemplateParserImpl mustacheParser;

    /**
     * Gets the appropriate template parser based on tenant configuration.
     * 
     * <p>Relies on TenantResolutionFilter to set the correct tenant in TenantContext.
     * The filter handles both multi-tenancy modes:
     * <ul>
     *   <li>Multi-tenancy enabled: TenantContext contains tenant from request</li>
     *   <li>Multi-tenancy disabled: TenantContext contains default tenant</li>
     * </ul>
     *
     * @return the configured template parser for the current tenant
     * @throws IllegalStateException if tenant properties are not found
     */
    public TemplateParser getParser() {
        String tenantId = TenantContext.getCurrentTenant();
        
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in TenantContext");
        }
        
        UserManagementTenantProperties tenantProperties = tenantConfigurationService.getTenantProperties();
        if (tenantProperties == null) {
            throw new IllegalStateException(
                "No tenant properties found for tenant: " + tenantId 
                + ". Tenant must be configured in application properties.");
        }
        
        NotificationProperties.TemplateEngineProperties templateConfig =
            tenantProperties.getNotification().getTemplate();

        if (templateConfig == null || templateConfig.getEngine() == null) {
            log.warn("Template engine configuration not found for tenant '{}', defaulting to Mustache",
                tenantId);
            return mustacheParser;
        }

        String engine = templateConfig.getEngine().toLowerCase();
        log.debug("Selecting template parser '{}' for tenant '{}'", engine, tenantId);

        return switch (engine) {
            case "thymeleaf" -> {
                log.info("Using ThymeleafTemplateParser for tenant '{}'", tenantId);
                yield thymeleafParser;
            }
            case "mustache" -> {
                log.info("Using MustacheTemplateParser for tenant '{}'", tenantId);
                yield mustacheParser;
            }
            default -> {
                log.warn("Unknown template engine '{}' for tenant '{}', defaulting to Mustache",
                    engine, tenantId);
                yield mustacheParser;
            }
        };
    }
}
