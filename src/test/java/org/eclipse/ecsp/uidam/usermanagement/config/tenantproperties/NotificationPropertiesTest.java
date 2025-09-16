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

package org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for NotificationProperties.
 */
class NotificationPropertiesTest {

    @Test
    void testNotificationProperties_GettersAndSetters() {
        // Arrange
        NotificationProperties properties = new NotificationProperties();
        String testApiUrl = "https://api.notifications.example.com";
        String testNotificationId = "notification-123";

        // Act
        properties.setNotificationApiUrl(testApiUrl);
        properties.setNotificationId(testNotificationId);

        // Assert
        assertEquals(testApiUrl, properties.getNotificationApiUrl());
        assertEquals(testNotificationId, properties.getNotificationId());
    }

    @Test
    void testEmailProviderProperties_GettersAndSetters() {
        // Arrange
        NotificationProperties.EmailProviderProperties emailProperties = 
                new NotificationProperties.EmailProviderProperties();
        String testProvider = "smtp";
        String testHost = "smtp.example.com";

        // Act
        emailProperties.setProvider(testProvider);
        emailProperties.setHost(testHost);

        // Assert
        assertEquals(testProvider, emailProperties.getProvider());
        assertEquals(testHost, emailProperties.getHost());
    }

    @Test
    void testEmailProviderProperties_DefaultValues() {
        // Arrange & Act
        NotificationProperties.EmailProviderProperties emailProperties = 
                new NotificationProperties.EmailProviderProperties();

        // Assert
        assertEquals("internal", emailProperties.getProvider());
        assertEquals("smtp.gmail.com", emailProperties.getHost());
    }

    @Test
    void testNotificationProperties_EmailProviderIntegration() {
        // Arrange
        NotificationProperties properties = new NotificationProperties();
        NotificationProperties.EmailProviderProperties emailProperties = 
                new NotificationProperties.EmailProviderProperties();

        // Act
        properties.setEmail(emailProperties);

        // Assert
        assertNotNull(properties.getEmail());
        assertEquals(emailProperties, properties.getEmail());
    }

    @Test
    void testNotificationProperties_TemplateEngineIntegration() {
        // Arrange
        NotificationProperties properties = new NotificationProperties();
        NotificationProperties.TemplateEngineProperties templateProperties = 
                new NotificationProperties.TemplateEngineProperties();

        // Act
        properties.setTemplate(templateProperties);

        // Assert
        assertNotNull(properties.getTemplate());
        assertEquals(templateProperties, properties.getTemplate());
    }
}
