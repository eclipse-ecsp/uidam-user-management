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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating tenant-specific JavaMailSender instances.
 * Manages a cache of mail senders per tenant to avoid recreating them on every request.
 * 
 * <p>This factory is used by InternalEmailNotificationProvider to get tenant-specific
 * SMTP configurations. It's always created (no conditional) to support multi-tenant
 * provider selection where different tenants can use different email providers.
 * 
 * <p>Relies on TenantResolutionFilter to set the correct tenant in TenantContext:
 * <ul>
 *   <li>Multi-tenancy enabled: TenantContext contains tenant from request</li>
 *   <li>Multi-tenancy disabled: TenantContext contains default tenant from tenant.default property</li>
 * </ul>
 *
 * @see org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.EmailNotificationProviderFactory
 * @see org.eclipse.ecsp.uidam.usermanagement.filter.TenantResolutionFilter
 */
@Slf4j
@Component
public class TenantAwareJavaMailSenderFactory {

    private final TenantConfigurationService tenantConfigurationService;
    private final Map<String, JavaMailSender> mailSenderCache = new ConcurrentHashMap<>();

    public TenantAwareJavaMailSenderFactory(TenantConfigurationService tenantConfigurationService) {
        this.tenantConfigurationService = tenantConfigurationService;
    }

    /**
     * Get or create a JavaMailSender for the current tenant.
     * 
     * <p>Tenant ID is retrieved from TenantContext, which is already set by TenantResolutionFilter
     * based on multi-tenancy configuration (either from request or default tenant).
     *
     * @return JavaMailSender configured for the current tenant
     * @throws IllegalStateException if tenant properties are not found for the current tenant
     */
    public JavaMailSender getMailSender() {
        String tenantId = TenantContext.getCurrentTenant();
        UserManagementTenantProperties tenantProperties = tenantConfigurationService.getTenantProperties();
        
        if (tenantProperties == null) {
            throw new IllegalStateException(
                "No tenant properties found for tenant: " + tenantId 
                + ". Tenant must be configured in application properties.");
        }

        return mailSenderCache.computeIfAbsent(tenantId, key -> createMailSender(tenantProperties));
    }

    /**
     * Create a new JavaMailSender instance configured with tenant-specific properties.
     *
     * @param tenantProperties the tenant properties containing email configuration
     * @return configured JavaMailSender
     */
    private JavaMailSender createMailSender(UserManagementTenantProperties tenantProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        NotificationProperties notificationProps = tenantProperties.getNotification();
        if (notificationProps == null || notificationProps.getEmail() == null) {
            log.error("Email configuration not found for tenant, cannot create mail sender");
            throw new IllegalStateException("Email configuration not found for tenant");
        }

        NotificationProperties.EmailProviderProperties emailProps = notificationProps.getEmail();

        // Set basic properties
        if (StringUtils.hasText(emailProps.getHost())) {
            mailSender.setHost(emailProps.getHost());
            log.debug("Mail sender host configured: {}", emailProps.getHost());
        }
        
        if (emailProps.getPort() != null) {
            mailSender.setPort(emailProps.getPort());
            log.debug("Mail sender port configured: {}", emailProps.getPort());
        }
        
        if (StringUtils.hasText(emailProps.getUsername())) {
            mailSender.setUsername(emailProps.getUsername());
            log.debug("Mail sender username configured: {}", emailProps.getUsername());
        }
        
        if (StringUtils.hasText(emailProps.getPassword())) {
            mailSender.setPassword(emailProps.getPassword());
        }

        // Set JavaMail properties
        Properties javaMailProperties = mailSender.getJavaMailProperties();
        if (emailProps.getProperties() != null && !emailProps.getProperties().isEmpty()) {
            emailProps.getProperties().forEach((key, value) -> {
                javaMailProperties.put(key, value);
                log.debug("Mail property set: {} = {}", key, value);
            });
        }

        log.info("Successfully created JavaMailSender for tenant with host: {}, port: {}", 
                emailProps.getHost(), emailProps.getPort());

        return mailSender;
    }

    /**
     * Clear the cache for a specific tenant. Useful when configuration changes.
     *
     * @param tenantId the tenant ID
     */
    public void clearCache(String tenantId) {
        mailSenderCache.remove(tenantId);
        log.info("Cleared JavaMailSender cache for tenant: {}", tenantId);
    }

    /**
     * Clear all cached mail senders.
     */
    public void clearAllCache() {
        mailSenderCache.clear();
        log.info("Cleared all JavaMailSender cache");
    }
}
