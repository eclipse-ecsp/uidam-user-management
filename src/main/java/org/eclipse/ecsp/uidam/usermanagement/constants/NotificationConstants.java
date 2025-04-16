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

package org.eclipse.ecsp.uidam.usermanagement.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * contains contants for notification properties.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public enum NotificationConstants {
    ;
    public static final String NOTIFICATION_EMAIL_PROVIDER = "notification.email.provider";
    public static final String NOTIFICATION_EMAIL_PROVIDER_HOST = "notification.email.provider.host";
    public static final String NOTIFICATION_EMAIL_PROVIDER_PORT = "notification.email.provider.port";
    public static final String NOTIFICATION_EMAIL_PROVIDER_USERNAME = "notification.email.provider.username";
    public static final String NOTIFICATION_EMAIL_PROVIDER_PASSWORD = "notification.email.provider.passwd";
    public static final String NOTIFICATION_EMAIL_PROVIDER_PROPERTIES_PREFIX =
            "notification.email.provider.properties.";
    public static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    public static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    public static final String SMTP = "smtp";
    public static final String SMS = "sms";
    public static final String PUSH = "push";
    public static final String EMAIL = "email";
}
