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
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.config.TenantAwareJavaMailSenderFactory;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NonRegisteredUserData;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.NotificationNonRegisteredUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * test for sending mail notification with spring mail.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = InternalEmailNotificationTest.AppConfig.class)
@TestPropertySource("classpath:application-notification.properties")
@MockBean(AccountRepository.class)
@org.springframework.test.context.TestExecutionListeners(
    listeners = org.eclipse.ecsp.uidam.common.test.TenantContextTestExecutionListener.class,
    mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@org.springframework.context.annotation.Import(org.eclipse.ecsp.uidam.common.test.TestTenantConfiguration.class)
class InternalEmailNotificationTest {

    @Autowired
    private NotificationManager notificationManager;

    @MockBean
    private TenantAwareJavaMailSenderFactory tenantAwareJavaMailSenderFactory;
    
    @MockBean
    private JavaMailSender javaMailSender;
    
    @MockBean
    private TenantConfigurationService tenantConfigurationService;
    
    @MockBean
    PasswordValidationService passwordValidationService;

    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    /**
     * application test config.
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
        // Mock the factory to return our mocked JavaMailSender
        org.mockito.Mockito.when(tenantAwareJavaMailSenderFactory.getMailSender())
            .thenReturn(javaMailSender);
        
        // Mock JavaMailSender to prevent actual email sending
        doReturn(new MimeMessage((Session) null)).when(javaMailSender).createMimeMessage();
        
        // Configure email provider to use Internal
        org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
            .EmailProviderProperties emailProps = new org.eclipse.ecsp.uidam.usermanagement.config
            .tenantproperties.NotificationProperties.EmailProviderProperties();
        emailProps.setProvider("internal");
        emailProps.setHost("smtp.test.com");
        final int smtpPort = 587;
        emailProps.setPort(smtpPort);
        emailProps.setUsername("test@test.com");
        emailProps.setPassword("testpass");
        
        // Configure template engine properties
        org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
            .TemplateEngineProperties templateProps = new org.eclipse.ecsp.uidam.usermanagement.config
            .tenantproperties.NotificationProperties.TemplateEngineProperties();
        templateProps.setEngine("thymeleaf");
        templateProps.setFormat("HTML");
        templateProps.setResolver("CLASSPATH");
        templateProps.setPrefix("/notification/");
        templateProps.setSuffix(".html");
        
        // Configure notification config properties
        org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
            .NotificationConfigProperties configProps = new org.eclipse.ecsp.uidam.usermanagement.config
            .tenantproperties.NotificationProperties.NotificationConfigProperties();
        configProps.setResolver("internal");
        configProps.setPath("classpath:/notification/uidam-notification-config.json");
        
        // Configure notification properties
        final org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties
            notifProps = new org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties
            .NotificationProperties();
        notifProps.setEmail(emailProps);
        notifProps.setTemplate(templateProps);
        notifProps.setConfig(configProps);
        
        // Configure tenant properties
        final org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties
            .UserManagementTenantProperties tenantProps = new org.eclipse.ecsp.uidam.usermanagement
            .config.tenantproperties.UserManagementTenantProperties();
        tenantProps.setNotification(notifProps);
        
        org.mockito.Mockito.when(tenantConfigurationService.getTenantProperties())
            .thenReturn(tenantProps);
    }

    @Test
    void testSuccess() throws MessagingException {
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
        data.put("name", "John");
        data.put("lastName", "Doe");
        data.put("username", "john");
        data.put("emailLink", "http://localhost:8080/sendEmailverifylinksample112343e");
        data.put("sender", "example.1@example.com");
        HashMap<String, Object> uidamMap = new HashMap<>();
        uidamMap.put("uidam", data);
        notificationData.setData(uidamMap);
        request.setRecipients(List.of(notificationData));

        ArgumentCaptor<MimeMessage> argumentCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        assertTrue(notificationManager.sendNotification(request));
        verify(javaMailSender, atLeastOnce()).send(argumentCaptor.capture());
        assertEquals("UIDAM Email Verification", argumentCaptor.getValue().getSubject());
    }

    @Test
    void testDefaultLocale() throws MessagingException {
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
        data.put("name", "John");
        data.put("lastName", "Doe");
        data.put("username", "JohnDoe");
        data.put("emailLink", "http://localhost:8080/sendEmailverifylinksample112343e");
        data.put("sender", "example.1@example.com");
        HashMap<String, Object> uidamMap = new HashMap<>();
        uidamMap.put("uidam", data);
        notificationData.setData(uidamMap);
        request.setRecipients(List.of(notificationData));

        ArgumentCaptor<MimeMessage> argumentCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        assertTrue(notificationManager.sendNotification(request));
        verify(javaMailSender, atLeastOnce()).send(argumentCaptor.capture());
        assertEquals("UIDAM Email Verification", argumentCaptor.getValue().getSubject());
    }

    @Test
    void testInvalidNotificationId() {
        doReturn(new MimeMessage((Session) null)).when(javaMailSender).createMimeMessage();
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
        notificationData.setData(Map.of("firstName", "John", "lastName", "Doe"));
        request.setRecipients(List.of(notificationData));
        assertThrows(TemplateNotFoundException.class, () -> notificationManager.sendNotification(request));

    }
}
