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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.notification.providers.email.EmailNotificationProvider;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NotificationNonRegisteredUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.REQUEST_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.NotificationConstants.NOTIFICATION_EMAIL_PROVIDER;

/**
 * EmailNotificationProvider is responsible for sending email using notification center.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = NOTIFICATION_EMAIL_PROVIDER, havingValue = "ignite")
public class IgniteEmailNotificationProvider implements EmailNotificationProvider {
    private static final String LOG_PATTERN =
            "request method: {}, request URI: {}, request headers: {}, request body: {}";
    private  ApplicationProperties applicationProperties;
    private  RestTemplate restTemplate;

    /**
     * Constructor to initialize {@link IgniteEmailNotificationProvider}.
     *
     * @param applicationProperties application configs
     * @param restTemplate          for rest api call
     */
    public IgniteEmailNotificationProvider(ApplicationProperties applicationProperties,
                                           RestTemplate restTemplate) {
        this.applicationProperties = applicationProperties;
        this.restTemplate = restTemplate;
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
    }

    //prepare objects and send notification  email notification
    @Override
    public boolean sendEmailNotification(NotificationNonRegisteredUser request) {
        log.info("sending email notification using ignite notification center...");
        String notificationApiUrl =
                StringUtils.isNotBlank(applicationProperties.getNotification().getNotificationApiUrl())
                        ? applicationProperties.getNotification().getNotificationApiUrl() : "";
        try {
            request.setNotificationId(applicationProperties.getNotification().getNotificationId());
            String requestPayload = new ObjectMapper().writeValueAsString(request);
            HttpHeaders headers = new HttpHeaders();
            headers.add(REQUEST_ID, UUID.randomUUID().toString());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<String> httpEntity = new HttpEntity<>(requestPayload, headers);
            log.info(LOG_PATTERN, HttpMethod.POST, notificationApiUrl, headers, request);
            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(notificationApiUrl, HttpMethod.POST, httpEntity, String.class);
            String responseData = responseEntity.getBody();
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new ApplicationRuntimeException(responseData, responseEntity.getStatusCode().toString());
            }
            log.info("Email was sent successfully message from notification center: {}", responseData);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse request payload", e);
            throw new ApplicationRuntimeException("Unable to parse request payload", e);
        } catch (RestClientException e) {
            log.error("Error occurred while sending notification", e);
            throw new ApplicationRuntimeException("Error occurred while sending notification", e);
        }
        return true;
    }
}
