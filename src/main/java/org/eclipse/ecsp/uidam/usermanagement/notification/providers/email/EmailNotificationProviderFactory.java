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

package org.eclipse.ecsp.uidam.usermanagement.notification.providers.email;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.impl.IgniteEmailNotificationProvider;
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.impl.InternalEmailNotificationProvider;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Factory for selecting the appropriate EmailNotificationProvider based on tenant configuration.
 * This allows different tenants to use different email providers (internal vs ignite).
 * 
 * <p>Example configurations:
 * <ul>
 *   <li>ECSP tenant: tenant.tenants.ecsp.notification.email.provider=internal</li>
 *   <li>SDP tenant: tenant.tenants.sdp.notification.email.provider=ignite</li>
 * </ul>
 * 
 * <p>The factory retrieves the current tenant from TenantContext and selects the appropriate
 * provider implementation at runtime based on the tenant's configuration.
 */
@Slf4j
@Component
public class EmailNotificationProviderFactory {

    private final TenantConfigurationService tenantConfigurationService;
    private final InternalEmailNotificationProvider internalProvider;
    private final IgniteEmailNotificationProvider igniteProvider;

    /**
     * Constructor for EmailNotificationProviderFactory.
     * Both providers are injected and available for runtime selection.
     *
     * @param tenantConfigurationService service for retrieving tenant configuration
     * @param internalProvider the internal email provider (Spring Mail)
     * @param igniteProvider the ignite notification center provider
     */
    public EmailNotificationProviderFactory(
            TenantConfigurationService tenantConfigurationService,
            InternalEmailNotificationProvider internalProvider,
            IgniteEmailNotificationProvider igniteProvider) {
        this.tenantConfigurationService = tenantConfigurationService;
        this.internalProvider = internalProvider;
        this.igniteProvider = igniteProvider;
        log.info("EmailNotificationProviderFactory initialized with both internal and ignite providers");
    }

    /**
     * Get the appropriate EmailNotificationProvider for the current tenant.
     * The provider is selected based on the tenant's notification.email.provider configuration.
     *
     * @return EmailNotificationProvider configured for the current tenant
     * @throws ApplicationRuntimeException if provider type is not configured or is invalid
     */
    public EmailNotificationProvider getProvider() {
        String tenantId = TenantContext.getCurrentTenant();
        UserManagementTenantProperties tenantProperties = tenantConfigurationService.getTenantProperties();

        if (tenantProperties == null) {
            log.warn("No tenant properties found for tenant: {}, using default tenant", tenantId);
            tenantId = tenantConfigurationService.getDefaultTenantId();
            tenantProperties = tenantConfigurationService.getDefaultTenantProperties();
        }

        if (tenantProperties == null) {
            throw new ApplicationRuntimeException(
                    "Unable to retrieve tenant properties for tenant: " + tenantId);
        }

        String providerType = getProviderType(tenantProperties);
        log.debug("Selecting email provider '{}' for tenant '{}'", providerType, tenantId);

        return selectProvider(providerType, tenantId);
    }

    /**
     * Get the provider type from tenant properties.
     * Defaults to "internal" if not specified.
     *
     * @param tenantProperties the tenant properties
     * @return the provider type (internal or ignite)
     */
    private String getProviderType(UserManagementTenantProperties tenantProperties) {
        NotificationProperties notificationProps = tenantProperties.getNotification();
        
        if (notificationProps == null || notificationProps.getEmail() == null) {
            log.warn("Email notification configuration not found, defaulting to 'internal' provider");
            return "internal";
        }

        String providerType = notificationProps.getEmail().getProvider();
        
        if (!StringUtils.hasText(providerType)) {
            log.debug("Provider type not specified, defaulting to 'internal'");
            return "internal";
        }

        return providerType.toLowerCase().trim();
    }

    /**
     * Select the provider implementation based on the provider type.
     *
     * @param providerType the provider type (internal or ignite)
     * @param tenantId the tenant ID (for logging)
     * @return the selected EmailNotificationProvider
     * @throws ApplicationRuntimeException if provider type is not supported
     */
    private EmailNotificationProvider selectProvider(String providerType, String tenantId) {
        switch (providerType) {
            case "internal":
                log.info("Using InternalEmailNotificationProvider for tenant '{}'", tenantId);
                return internalProvider;
                
            case "ignite":
                log.info("Using IgniteEmailNotificationProvider for tenant '{}'", tenantId);
                return igniteProvider;
                
            default:
                String errorMsg = String.format(
                        "Unsupported email provider type '%s' for tenant '%s'. "
                        + "Supported types are: 'internal', 'ignite'",
                        providerType, tenantId);
                log.error(errorMsg);
                throw new ApplicationRuntimeException(errorMsg);
        }
    }

    /**
     * Get the provider for a specific tenant ID (useful for testing).
     *
     * @param tenantId the tenant ID
     * @return EmailNotificationProvider for the specified tenant
     */
    public EmailNotificationProvider getProviderForTenant(String tenantId) {
        UserManagementTenantProperties tenantProperties = 
                tenantConfigurationService.getTenantProperties(tenantId);

        if (tenantProperties == null) {
            throw new ApplicationRuntimeException(
                    "Tenant properties not found for tenant: " + tenantId);
        }

        String providerType = getProviderType(tenantProperties);
        return selectProvider(providerType, tenantId);
    }
}
