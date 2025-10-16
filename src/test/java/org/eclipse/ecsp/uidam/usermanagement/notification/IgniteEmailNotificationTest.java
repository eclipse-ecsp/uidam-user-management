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

import io.prometheus.client.CollectorRegistry;
import jakarta.mail.MessagingException;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NonRegisteredUserData;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NotificationNonRegisteredUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * notification center mail notification test.
 */
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = IgniteEmailNotificationTest.AppConfig.class)
@TestPropertySource(properties = "notification.email.provider=ignite")
@TestPropertySource("classpath:application-notification.properties")
@MockBean(AccountRepository.class)
class IgniteEmailNotificationTest {

    @Autowired
    private NotificationManager notificationManager;

    @MockBean
    private TenantConfigurationService tenantConfigurationService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockRestServiceServer;
    
    @MockBean
    PasswordValidationService passwordValidationService;

    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    /**
     * application test configuration.
     */
    @Configuration
    @ComponentScan("org.eclipse.ecsp")
    @ImportAutoConfiguration(JacksonAutoConfiguration.class)
    public static class AppConfig {

    }

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @BeforeEach
    public void setup() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        
        // Configure the mock TenantConfigurationService
        final org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties
            tenantProperties = new org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties
                .UserManagementTenantProperties();
        
        final org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
            notificationProperties = new org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties
                .NotificationProperties();
        notificationProperties.setNotificationApiUrl("http://test-notification-api:8080/v1/notifications/nonRegisteredUsers");
        notificationProperties.setNotificationId("TEST_NOTIFICATION_ID");
        
        // Configure email provider to use Ignite
        final org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
            .EmailProviderProperties emailProviderProperties =
            new org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
                .EmailProviderProperties();
        emailProviderProperties.setProvider("ignite");
        notificationProperties.setEmail(emailProviderProperties);
        
        tenantProperties.setNotification(notificationProperties);
        
        org.mockito.Mockito.when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
    }

    @Test
    void testSuccess() throws MessagingException, IOException {
        mockRestServiceServer
                .expect(ExpectedCount.once(),
                        MockRestRequestMatchers.requestTo(tenantConfigurationService.getTenantProperties()
                                .getNotification().getNotificationApiUrl()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK).body("{\"message\": \"success\"}"));
        NotificationNonRegisteredUser request = new NotificationNonRegisteredUser();
        request.setNotificationId("UIDAM_USER_VERIFY_ACCOUNT");
        request.setRequestId(UUID.randomUUID().toString());
        request.setSessionId(UUID.randomUUID().toString());
        request.setVersion("1.0");
        request.setUserId("userId");
        NonRegisteredUserData notificationData = new NonRegisteredUserData();
        notificationData.setBrand("USER");
        notificationData.setEmail("example1@example.com");
        notificationData.setLocale("en_IN");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Abhishek");
        data.put("lastName", "Kumar");
        data.put("username", "Abhishek");
        data.put("emailLink", "http://localhost:8080/sendEmailverifylinksample112343e");
        data.put("sender", "example.1@example.com");
        HashMap<String, Object> uidamMap = new HashMap<>();
        uidamMap.put("uidam", data);
        notificationData.setData(uidamMap);
        request.setRecipients(List.of(notificationData));
        assertTrue(notificationManager.sendNotification(request));

    }

    @Test
    void testError() throws MessagingException, IOException {
        mockRestServiceServer
                .expect(ExpectedCount.once(),
                        MockRestRequestMatchers.requestTo(tenantConfigurationService.getTenantProperties()
                                .getNotification().getNotificationApiUrl()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST)).andRespond(MockRestResponseCreators
                        .withStatus(HttpStatus.BAD_REQUEST).body("{\"message\": \"invalid notification id\"}"));
        NotificationNonRegisteredUser request = new NotificationNonRegisteredUser();
        request.setNotificationId("UIDAM_USER_VERIFY_ACCOUNT");
        request.setRequestId(UUID.randomUUID().toString());
        request.setSessionId(UUID.randomUUID().toString());
        request.setVersion("1.0");
        request.setUserId("userId");
        NonRegisteredUserData notificationData = new NonRegisteredUserData();
        notificationData.setBrand("USER");
        notificationData.setEmail("example1@example.com");
        notificationData.setLocale("en_IN");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Abhishek");
        data.put("lastName", "Kumar");
        data.put("username", "Abhishek");
        data.put("emailLink", "http://localhost:8080/sendEmailverifylinksample112343e");
        data.put("sender", "example.1@example.com");
        HashMap<String, Object> uidamMap = new HashMap<>();
        uidamMap.put("uidam", data);
        notificationData.setData(uidamMap);
        request.setRecipients(List.of(notificationData));
        assertThrows(ApplicationRuntimeException.class, () -> notificationManager.sendNotification(request));
    }

    @Test
    void testDefaultLocale() throws MessagingException {
        mockRestServiceServer
                .expect(ExpectedCount.once(),
                        MockRestRequestMatchers.requestTo(tenantConfigurationService.getTenantProperties()
                                .getNotification().getNotificationApiUrl()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK).body("{\"message\": \"success\"}"));
        NotificationNonRegisteredUser request = new NotificationNonRegisteredUser();
        request.setNotificationId("UIDAM_USER_VERIFY_ACCOUNT");
        request.setRequestId(UUID.randomUUID().toString());
        request.setSessionId(UUID.randomUUID().toString());
        request.setVersion("1.0");
        request.setUserId("userId");
        NonRegisteredUserData notificationData = new NonRegisteredUserData();
        notificationData.setBrand("USER");
        notificationData.setEmail("example1@example.com");
        notificationData.setLocale("it_IT");
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Abhishek");
        data.put("lastName", "Kumar");
        data.put("username", "Abhishek");
        data.put("emailLink", "http://localhost:8080/sendEmailverifylinksample112343e");
        data.put("sender", "example.1@example.com");
        HashMap<String, Object> uidamMap = new HashMap<>();
        uidamMap.put("uidam", data);
        notificationData.setData(uidamMap);
        request.setRecipients(List.of(notificationData));
        assertTrue(notificationManager.sendNotification(request));
    }

    @Test
    void testInvalidNotificationId() {
        NotificationNonRegisteredUser request = new NotificationNonRegisteredUser();
        request.setNotificationId("TEST_NOTIFICATION_2");
        request.setRequestId(UUID.randomUUID().toString());
        request.setSessionId(UUID.randomUUID().toString());
        request.setVersion("1.0");
        request.setUserId("userId");
        NonRegisteredUserData notificationData = new NonRegisteredUserData();
        notificationData.setBrand("USER");
        notificationData.setEmail("example1@example.com");
        notificationData.setLocale("en_IN");
        notificationData.setData(Map.of("firstName", "Abhishek", "lastName", "Kumar"));
        request.setRecipients(List.of(notificationData));
        assertThrows(TemplateNotFoundException.class, () -> notificationManager.sendNotification(request));
    }
}
