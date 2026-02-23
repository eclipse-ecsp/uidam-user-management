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

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Notification configuration properties for each tenant.
 * Supports different notification providers and configurations per tenant.
 */
@Getter
@Setter
public class NotificationProperties {
    
    private String notificationApiUrl;
    private String notificationId;
    
    // Email Provider Configuration
    private EmailProviderProperties email;
    
    // Notification Configuration
    private NotificationConfigProperties config;
    
    // Template Engine Configuration
    private TemplateEngineProperties template;
    
    /**
     * Email provider configuration for tenant-specific email settings.
     */
    @Getter
    @Setter
    public static class EmailProviderProperties {
        private String provider = "internal";
        private String host = "smtp.gmail.com";
        private Integer port = 587;
        private String username;
        private String password;
        private Map<String, String> properties;
    }
    
    /**
     * Notification configuration for template and configuration resolution.
     */
    @Getter
    @Setter
    public static class NotificationConfigProperties {
        private String resolver = "internal";
        private String path = "classpath:/notification/uidam-notification-config.json";
    }
    
    /**
     * Template engine configuration for notification templates.
     */
    @Getter
    @Setter
    public static class TemplateEngineProperties {
        private String engine = "thymeleaf";
        private String format = "HTML";
        private String resolver = "CLASSPATH";
        private String prefix = "/notification/";
        private String suffix = ".html";
    }
}
