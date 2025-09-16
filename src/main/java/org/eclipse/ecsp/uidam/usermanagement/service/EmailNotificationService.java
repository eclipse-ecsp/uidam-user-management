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

package org.eclipse.ecsp.uidam.usermanagement.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.notification.NotificationManager;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NonRegisteredUserData;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NotificationNonRegisteredUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * Service to send EmailNotification to users.
 */
@Service
@AllArgsConstructor
@Slf4j
public class EmailNotificationService {
    private static final String NOTIFICATION_VERSION = "1.0";

    @Autowired
    private NotificationManager notificationManager;

    /**
     * Method to send email notification to the users.
     *
     * @param userDetails userDetails map with user information as email.
     * @param notificationId notificationId for which the email to be sent
     * @param notificationData the placeholders of the notification payload
     */
    @Async
    public void sendNotification(Map<String, String> userDetails,
                                 String notificationId,
                                 Map<String, Object> notificationData) {
        NotificationNonRegisteredUser requestData = new NotificationNonRegisteredUser();
        requestData.setNotificationId(notificationId);
        requestData.setVersion(NOTIFICATION_VERSION);
        NonRegisteredUserData recipientInfo = new NonRegisteredUserData();
        recipientInfo.setEmail(userDetails.get(ApiConstants.EMAIL_ADDRESS));
        recipientInfo.setData(notificationData);
        requestData.setRecipients(List.of(recipientInfo));
        notificationManager.sendNotification(requestData);
    }
}
