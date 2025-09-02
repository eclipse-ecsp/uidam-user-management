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

import java.util.List;

/**
 * Tenant-specific properties for User Management service.
 * Contains all configuration that varies by tenant.
 * Based on analysis of ApplicationProperties.java and application.properties.
 */
@Getter
@Setter
public class UserManagementTenantProperties {
    
    // Core Identity Properties
    private String accountName;
    private String accountType;
    
    // User Management Properties
    private String passwordEncoder;
    private String maxAllowedLoginAttempts;
    private Boolean isUserStatusLifeCycleEnabled;
    private String externalUserPermittedRoles;
    private String externalUserDefaultStatus;
    private String userDefaultAccountName;
    private Boolean additionalAttrCheckEnabledForSignUp;
    
    // Email Verification Properties
    private Boolean isEmailVerificationEnabled;
    private String emailVerificationUrl;
    private String emailVerificationNotificationId;
    private Long emailVerificationExpDays;
    private List<String> emailRegexPatternExclude;
    private String authServerEmailVerificationResponseUrl;
    
    // Password Recovery Properties
    private Long recoverySecretExpiresInMinutes;
    private String passwordRecoveryNotificationId;
    private String authServerResetResponseUrl;
    
    // Captcha Properties
    private Integer captchaEnforceAfterNoOfFailures;
    
    // Nested Configuration Objects
    private DatabaseProperties database;
    private NotificationProperties notification;
    private AuthServerProperties authServer;
    private ClientRegistrationProperties clientRegistration;
    private AuthProperties auth;
    private LiquibaseProperties liquibase;
    
    /**
     * Liquibase-specific properties for this tenant.
     */
    @Getter
    @Setter
    public static class LiquibaseProperties {
        private String changeLog;
        private String defaultSchema;
        private ParametersProperties parameters;
        
        /**
         * Parameters for Liquibase configuration.
         */
        @Getter
        @Setter
        public static class ParametersProperties {
            private String schema;
            private String initialDataClientSecret;
            private String initialDataUserSalt;
            private String initialDataUserPwd;
        }
    }
}
