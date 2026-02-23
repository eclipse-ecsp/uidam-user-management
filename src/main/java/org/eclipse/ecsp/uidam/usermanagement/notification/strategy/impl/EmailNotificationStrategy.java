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

package org.eclipse.ecsp.uidam.usermanagement.notification.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.config.EmailNotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.constants.NotificationConstants;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.TemplateParser;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.TemplateParserFactory;
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.EmailNotificationProvider;
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.EmailNotificationProviderFactory;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolverFactory;
import org.eclipse.ecsp.uidam.usermanagement.notification.strategy.NotificationStrategy;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NotificationNonRegisteredUser;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Strategy for email notification.
 * Uses EmailNotificationProviderFactory to select the appropriate provider at runtime
 * based on tenant configuration (internal vs ignite).
 * Uses NotificationConfigResolverFactory to select the appropriate config resolver per tenant.
 */
@Slf4j
@Component(NotificationConstants.EMAIL)
public class EmailNotificationStrategy implements NotificationStrategy {
    public static final String UIDAM = "uidam";
    private final EmailNotificationProviderFactory emailNotificationProviderFactory;
    private final NotificationConfigResolverFactory notificationConfigResolverFactory;
    private final TemplateParserFactory templateParserFactory;
    private final TenantConfigurationService tenantConfigurationService;

    /**
     * Initialize {@link EmailNotificationStrategy}.
     *
     * @param emailNotificationProviderFactory factory for selecting email notification provider
     * @param notificationConfigResolverFactory factory for selecting notification config resolver
     * @param templateParserFactory      factory for selecting template parser
     * @param tenantConfigurationService tenant configuration service
     */
    public EmailNotificationStrategy(final EmailNotificationProviderFactory emailNotificationProviderFactory,
                                     final NotificationConfigResolverFactory notificationConfigResolverFactory,
                                     final TemplateParserFactory templateParserFactory,
                                     TenantConfigurationService tenantConfigurationService) {
        this.emailNotificationProviderFactory = emailNotificationProviderFactory;
        this.notificationConfigResolverFactory = notificationConfigResolverFactory;
        this.templateParserFactory = templateParserFactory;
        this.tenantConfigurationService = tenantConfigurationService;
    }

    @Override
    public boolean send(final NotificationNonRegisteredUser request) {
        //perform basic validation, if required and call provider
        Objects.requireNonNull(request, "Notification Request should not be null");
        Objects.requireNonNull(request.getRecipients(),
                "recipient list should not be null, at least one recipient should be provided");
        
        // Get the appropriate config resolver for the current tenant
        NotificationConfigResolver notificationConfigResolver = notificationConfigResolverFactory.getResolver();
        
        request.getRecipients().forEach(recipient -> {
            Optional<EmailNotificationTemplateConfig> emailTemplate =
                    notificationConfigResolver.getEmailTemplate(request.getNotificationId(),
                            recipient.getLocale());
            //process template
            if (emailTemplate.isPresent()) {
                final EmailNotificationTemplateConfig template = emailTemplate.get();
                final Map<String, Object> parsedEmailBodyMap = new HashMap<>();
                parsedEmailBodyMap.put("sender", template.getFrom());
                
                // Get tenant-specific template parser
                final TemplateParser templateParser = this.templateParserFactory.getParser();
                
                if (!CollectionUtils.isEmpty(template.getBody())) {
                    template.getBody().entrySet().forEach(entry -> {
                        final String parsedTemplateContent = templateParser.parseText(entry.getValue(),
                                recipient.getData());
                        parsedEmailBodyMap.put(entry.getKey(), parsedTemplateContent);
                    });
                }
                if (!StringUtils.isEmpty(template.getSubject())) {
                    parsedEmailBodyMap.put("subject",
                            templateParser.parseText(template.getSubject(), recipient.getData()));
                }
                String tenantLogoPath = tenantConfigurationService.getTenantProperties().getEmailLogoPath();
                String tenantCopyRight = tenantConfigurationService.getTenantProperties().getEmailCopyright();
                if (!StringUtils.isEmpty(tenantLogoPath)) {
                    parsedEmailBodyMap.put("Image-hdr_brand", tenantLogoPath);
                }
                parsedEmailBodyMap.put("copyright", tenantCopyRight);

                recipient.getData().put(UIDAM, parsedEmailBodyMap);
            } else {
                log.error("Template not found for email notification: {}, cannot proceed further..",
                        request.getNotificationId());
                throw new ApplicationRuntimeException(String.format("Template not found for email notification: %s",
                        request.getNotificationId()));
            }
        });
        // Get tenant-specific provider and call notification provider
        EmailNotificationProvider provider = this.emailNotificationProviderFactory.getProvider();
        return provider.sendEmailNotification(request);
    }

    @Override
    public String getStrategyName() {
        return NotificationConstants.EMAIL;
    }
}
