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
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.EmailNotificationProvider;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.notification.strategy.NotificationStrategy;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NotificationNonRegisteredUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stategy for email notification.
 */
@Slf4j
@Component(NotificationConstants.EMAIL)
@ConditionalOnProperty("notification.email.provider")
public class EmailNotificationStrategy implements NotificationStrategy {
    public static final String UIDAM = "uidam";
    private final EmailNotificationProvider emailNotificationProvider;
    private final NotificationConfigResolver notificationConfigResolver;
    private final TemplateParser templateParser;

    /**
     * Initialize {@link EmailNotificationStrategy}.
     *
     * @param emailNotificationProvider  email notification provider implementation
     * @param notificationConfigResolver notification config resolver
     * @param templateParser             parse notification template
     */
    public EmailNotificationStrategy(final EmailNotificationProvider emailNotificationProvider,
                                     final NotificationConfigResolver notificationConfigResolver,
                                     final TemplateParser templateParser) {
        this.emailNotificationProvider = emailNotificationProvider;
        this.notificationConfigResolver = notificationConfigResolver;
        this.templateParser = templateParser;
    }

    @Override
    public boolean send(final NotificationNonRegisteredUser request) {
        //perform basic validation, if required and call provider
        Objects.requireNonNull(request, "Notification Request should not be null");
        Objects.requireNonNull(request.getRecipients(),
                "recipient list should not be null, at least one recipient should be provided");
        request.getRecipients().forEach(recipient -> {
            Optional<EmailNotificationTemplateConfig> emailTemplate =
                    this.notificationConfigResolver.getEmailTemplate(request.getNotificationId(),
                            recipient.getLocale());
            //process template
            if (emailTemplate.isPresent()) {
                final EmailNotificationTemplateConfig template = emailTemplate.get();
                final Map<String, Object> parsedEmailBodyMap = new HashMap<>();
                parsedEmailBodyMap.put("sender", template.getFrom());
                if (!CollectionUtils.isEmpty(template.getBody())) {
                    template.getBody().entrySet().forEach(entry -> {
                        final String parsedTemplateContent = this.templateParser.parseText(entry.getValue(),
                                recipient.getData());
                        parsedEmailBodyMap.put(entry.getKey(), parsedTemplateContent);
                    });
                }
                if (!StringUtils.isEmpty(template.getSubject())) {
                    parsedEmailBodyMap.put("subject",
                            this.templateParser.parseText(template.getSubject(), recipient.getData()));
                }
                recipient.getData().put(UIDAM, parsedEmailBodyMap);
            } else {
                log.error("Template not found for email notification: {}, cannot proceed further..",
                        request.getNotificationId());
                throw new ApplicationRuntimeException(String.format("Template not found for email notification: %s",
                        request.getNotificationId()));
            }
        });
        // call notification provider
        return this.emailNotificationProvider.sendEmailNotification(request);
    }

    @Override
    public String getStrategyName() {
        return NotificationConstants.EMAIL;
    }
}
