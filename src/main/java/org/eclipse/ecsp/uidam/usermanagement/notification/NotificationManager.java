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

package org.eclipse.ecsp.uidam.usermanagement.notification;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolver;
import org.eclipse.ecsp.uidam.usermanagement.notification.resolver.NotificationConfigResolverFactory;
import org.eclipse.ecsp.uidam.usermanagement.notification.strategy.NotificationStrategy;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NotificationNonRegisteredUser;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;

/**
 * Sends notifications for different channels based on the strategy available.
 * Supports multi-tenant notification config resolution.
 */
@Slf4j
@Component
public class NotificationManager {
    private final Map<String, NotificationStrategy> strategyMap;
    private final NotificationConfigResolverFactory configResolverFactory;

    public NotificationManager(Map<String, NotificationStrategy> strategyMap,
                               NotificationConfigResolverFactory configResolverFactory) {
        this.strategyMap = strategyMap;
        this.configResolverFactory = configResolverFactory;
    }

    /**
     * send notification to user.
     *
     * @param request notification request
     * @return true if the notification sent successfully otherwise false
     */
    public boolean sendNotification(NotificationNonRegisteredUser request) {
        log.info("Request for notification with notificationId: {}, sessionId: {}, requestId: {}",
                request.getNotificationId(), request.getSessionId(), request.getRequestId());
        
        // Get the appropriate config resolver for the current tenant
        NotificationConfigResolver configResolver = configResolverFactory.getResolver();
        
        // get the notification config for that notificationId
        List<String> notificationChannels = configResolver.getNotificationChannels(request.getNotificationId());

        if (CollectionUtils.isEmpty(notificationChannels)) {
            log.error("Notification channels not available for notificationId: {}, sessionId: {}, requestId: {}",
                    request.getNotificationId(), request.getSessionId(), request.getRequestId());
            throw new TemplateNotFoundException("Notification Config not found for notificationId: "
                    + request.getNotificationId(), null);
        }
        log.debug("Found notification channels: {} for notificationId: {}, sessionId: {}, requestId: {}",
                notificationChannels, request.getNotificationId(), request.getSessionId(), request.getRequestId());

        // maintain status for each notification channel
        boolean isSent = false;

        // check with notification needs to send to which channels and call the appropriate notification strategy
        for (String channel : notificationChannels) {
            if (strategyMap.containsKey(channel)) {
                log.info("sending {} notification to channel: {} , sessionId: {}, requestId: {}",
                        request.getNotificationId(), channel, request.getSessionId(), request.getRequestId());
                isSent = strategyMap.get(channel).send(request);
                log.info("Sent {} notification to channel: {} , sessionId: {}, requestId: {}",
                        request.getNotificationId(), channel, request.getSessionId(), request.getRequestId());
            } else {
                log.warn("notification channel: {} not supported for notificationId: {}, sessionId: {}, requestId: {}",
                        channel, request.getNotificationId(), request.getSessionId(), request.getRequestId());
            }
        }
        return isSent;
    }
}
