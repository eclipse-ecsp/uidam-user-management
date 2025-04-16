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

package org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.config.EmailNotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.NotificationException;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.TemplateParser;
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.EmailNotificationProvider;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NotificationNonRegisteredUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import static org.eclipse.ecsp.uidam.usermanagement.constants.NotificationConstants.NOTIFICATION_EMAIL_PROVIDER;

/**
 * sending email notification using spring mail.
 */
@ConditionalOnProperty(name = NOTIFICATION_EMAIL_PROVIDER, havingValue = "internal", matchIfMissing = true)
@Component
@Slf4j
public class InternalEmailNotificationProvider implements EmailNotificationProvider {
    private final JavaMailSender javaMailSender;
    @Autowired
    private NotificationConfigResolver configResolver;

    @Autowired
    private TemplateParser templateParser;

    public InternalEmailNotificationProvider(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    //prepare AWS notification objects and send email notification
    @Override
    public boolean sendEmailNotification(NotificationNonRegisteredUser request) {
        final AtomicBoolean isSent = new AtomicBoolean(false);
        //get notification config
        Objects.requireNonNull(request, "Notification Request should not be null");
        Objects.requireNonNull(request.getRecipients(),
                "recipient list should not be null, at least one recipient should be provided");
        request.getRecipients().forEach(recipient -> {
            Optional<EmailNotificationTemplateConfig> emailTemplate = configResolver.getEmailTemplate(
                    request.getNotificationId(),
                    recipient.getLocale());
            //process template
            if (emailTemplate.isPresent()) {
                EmailNotificationTemplateConfig template = emailTemplate.get();
                Map<String, Object> parsedEmailBodyMap = (Map<String, Object>) recipient.getData().get("uidam");
                String parseEmailBody = null;
                if (!StringUtils.isEmpty(template.getReferenceHtml())) {
                    parseEmailBody = templateParser.parseTemplate(
                            template.getReferenceHtml(), recipient.getData());
                } else {
                    parseEmailBody = parsedEmailBodyMap.entrySet().stream()
                            .map(Map.Entry::getValue)
                            .map(Object::toString)
                            .collect(Collectors.joining());
                }
                Map<String, Resource> images = new HashMap<>();
                if (!CollectionUtils.isEmpty(template.getImages())) {
                    template.getImages().entrySet().forEach(i -> {
                        Resource resource = templateParser.getFile(i.getValue());
                        if (null != resource) {
                            images.put(i.getKey(), resource);
                        }
                    });
                }
                //send notification using spring mail
                boolean result = sendEmail(template.getFrom(),
                        recipient.getEmail(),
                        (String) parsedEmailBodyMap.get("subject"),
                        parseEmailBody, images);
                isSent.set(result);
            } else {
                log.error("Template not found for email notification: {}, cannot proceed further..",
                        request.getNotificationId());
                //throw an error
                throw new ApplicationRuntimeException(String.format("Template not found for email notification: %s",
                        request.getNotificationId()));
            }
        });
        return isSent.get();
    }

    private boolean sendEmail(String fromEmail,
                              String toEmail,
                              String subject,
                              String body,
                              Map<String, Resource> images) {
        try {
            MimeMessage mimeMsg = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, MimeMessageHelper.MULTIPART_MODE_MIXED, "UTF-8");
            helper.setFrom(new InternetAddress(fromEmail));
            helper.setText(body, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            if (!CollectionUtils.isEmpty(images)) {
                for (Map.Entry<String, Resource> image : images.entrySet()) {
                    helper.addInline(image.getKey(), image.getValue());
                }
            }
            javaMailSender.send(mimeMsg);
            log.info("Successfully sent email notification to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error occurred while sending email", e);
            throw new NotificationException("An Error occurred while sending email notification", e);
        }
        // set sender email
        return true;
    }
}
