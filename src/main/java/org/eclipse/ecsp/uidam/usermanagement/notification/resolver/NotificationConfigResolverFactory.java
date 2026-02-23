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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.impl.IgniteNotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.impl.InternalNotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the appropriate notification config resolver based on tenant configuration.
 * Supports runtime selection between Internal (file-based) and Ignite (API-based) resolvers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConfigResolverFactory {

    private final TenantConfigurationService tenantConfigurationService;
    private final InternalNotificationConfigResolver internalResolver;
    private final IgniteNotificationConfigResolver igniteResolver;

    /**
     * Gets the appropriate notification config resolver based on tenant configuration.
     * 
     * <p>Relies on TenantResolutionFilter to set the correct tenant in TenantContext.
     * The filter handles both multi-tenancy modes:
     * <ul>
     *   <li>Multi-tenancy enabled: TenantContext contains tenant from request</li>
     *   <li>Multi-tenancy disabled: TenantContext contains default tenant</li>
     * </ul>
     *
     * @return the configured notification config resolver for the current tenant
     * @throws IllegalStateException if tenant properties are not found
     */
    public NotificationConfigResolver getResolver() {
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
        
        NotificationProperties.NotificationConfigProperties configProps = 
            tenantProperties.getNotification().getConfig();

        if (configProps == null || configProps.getResolver() == null) {
            log.warn("Notification config resolver not found for tenant '{}', defaulting to internal",
                tenantId);
            return internalResolver;
        }

        String resolver = configProps.getResolver().toLowerCase();
        log.debug("Selecting notification config resolver '{}' for tenant '{}'", resolver, tenantId);

        return switch (resolver) {
            case "internal" -> {
                log.info("Using InternalNotificationConfigResolver for tenant '{}'", tenantId);
                yield internalResolver;
            }
            case "ignite" -> {
                log.info("Using IgniteNotificationConfigResolver for tenant '{}'", tenantId);
                yield igniteResolver;
            }
            default -> {
                log.warn("Unknown notification config resolver '{}' for tenant '{}', defaulting to internal",
                    resolver, tenantId);
                yield internalResolver;
            }
        };
    }
}
