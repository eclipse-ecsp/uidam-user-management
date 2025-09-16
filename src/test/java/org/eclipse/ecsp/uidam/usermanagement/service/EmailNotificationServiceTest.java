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

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.notification.NotificationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.util.HashMap;
import java.util.Map;

class EmailNotificationServiceTest {
    @Mock
    private TenantConfigurationService tenantConfigurationService;
    @Mock
    private UserManagementTenantProperties tenantProperties;
    @Mock
    private NotificationProperties notificationProperties;
    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @Mock
    private NotificationManager notificationManager;

    private EmailNotificationService emailNotificationServiceSpy;

    @BeforeEach
    void setUp() {
        CollectorRegistry.defaultRegistry.clear();
        MockitoAnnotations.openMocks(this);

        emailNotificationServiceSpy = Mockito.spy(emailNotificationService);
        Mockito.when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        Mockito.when(tenantProperties.getNotification()).thenReturn(notificationProperties);
        Mockito.when(notificationProperties.getNotificationId()).thenReturn("testUserVerify");
        Mockito.when(notificationProperties.getNotificationApiUrl())
            .thenReturn("https://api-gateway.eks-spring-auth.ic.aws.harmandev.com/v1/notifications/nonRegisteredUsers");
    }

    @Test
    void sendNotificationSuccess() {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put(ApiConstants.EMAIL_ADDRESS, "dummyEmail@domain.com");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Abhishek");
        data.put("lastName", "Kumar");
        data.put("username", "Abhishek");
        data.put("emailLink", "http://localhost:8080/sendEmailverifylinksample112343e");
        data.put("sender", "example.1@example.com");
        String notificationId = "UIDAM_USER_VERIFY_ACCOUNT";
        emailNotificationServiceSpy.sendNotification(userDetails, notificationId, data);
        Mockito.verify(notificationManager, Mockito.atLeastOnce()).sendNotification(Mockito.any());
    }

    @Test
    void sendNotificationFailure() {
        Mockito.doThrow(TemplateNotFoundException.class).when(notificationManager).sendNotification(Mockito.any());
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put(ApiConstants.EMAIL_ADDRESS, "dummyEmail@domain.com");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Abhishek");
        data.put("lastName", "Kumar");
        data.put("username", "Abhishek");
        data.put("emailLink", "http://localhost:8080/sendEmailverifylinksample112343e");
        data.put("sender", "example.1@example.com");
        String uidamNotificationId = "UIDAM_notification_id";
        Assertions.assertThrows(TemplateNotFoundException.class, () ->
                emailNotificationServiceSpy.sendNotification(userDetails, uidamNotificationId, data));
    }
}
