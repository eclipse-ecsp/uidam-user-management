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

package org.eclipse.ecsp.uidam.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigInteger;
import java.util.List;

/**
 * Class to load application properties from configmap.
 */
@ToString
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class ApplicationProperties {
    private String accountId;
    private String accountName;
    private String accountType;
    private String passwordEncoder;
    private String maxAllowedLoginAttempts;
    private Integer captchaEnforceAfterNoOfFailures;
    private NotificationConfiguration notification;
    private long recoverySecretExpiresInMinutes;
    private String passwordRecoveryNotificationId;
    private Boolean isUserStatusLifeCycleEnabled;
    private Boolean isEmailVerificationEnabled;
    private String emailVerificationUrl;
    private String emailVerificationNotificationId;
    private Long emailVerificationExpDays;
    private List<String> emailRegexPatternExclude;
    private String emailVerificationSubject;
    private String emailVerificationContent;
    private String authServerEmailVerificationResponseUrl;
    private String authServerTokenUrl;
    private String authServerResetResponseUrl;
    private String authServerRevokeTokenUrl;
    private String clientId;
    private String clientSecret;
    private String authorizationServerHostName;
    private long passwordUpdateTimeInterval;
    private int minPasswordLength;
    private int maxPasswordLength;
    private String passwordRegexPattern;
    private BigInteger userDefaultAccountId;
    private String externalUserPermittedRoles;
    private String externalUserDefaultStatus;
    private String userDefaultAccountName;
    private boolean additionalAttrCheckEnabledForSignUp;
}
