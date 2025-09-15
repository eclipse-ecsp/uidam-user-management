/*
 * Copyright (c) 2023 Harman International
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

package org.eclipse.ecsp.uidam.common.metrics;

import lombok.Getter;

/**
 * Enum representing various UIDAM metrics with their names and descriptions.
 */
@Getter
public enum UidamMetrics {

    TOTAL_RESET_PASSWORD_BY_ADMIN("total.reset.password.by.admin",
            "Total reset password by admin"),
    TOTAL_PROFILE_CRITICAL_CHANGES_BY_ADMIN("total.profile.critical.changes.by.admin",
            "Total profile critical changes by admin"),
    TOTAL_RESET_PASSWORD_BY_USER("total.reset.password.by.user",
            "Total reset password by user"),
    TOTAL_FORGOT_PASSWORD_BY_USER("total.forgot.password.by.user",
            "Total forgot password by user"),
    TOTAL_BLOCKED_USERS_EVENT("total.blocked.users.event",
            "Total blocked users event"),
    BLOCKED_USER_EVENT_BY_ADMIN("blocked.user.event.by.admin",
            "Blocked user event by admin"),
    BLOCKED_USERS_EVENT_BY_ATTEMPTS("blocked.users.event.by.attempts",
            "Blocked users event by attempts"),
    TEMP_BLOCKED_USERS_EVENT_BY_ATTEMPTS("temp.blocked.users.event.by.attempts",
            "Temp blocked users event by attempts"),
    PERMANENT_BLOCKED_USERS_EVENT_BY_ATTEMPTS("permanent.blocked.users.event.by.attempts",
            "Permanent blocked users event by attempts"),
    TOTAL_UNBLOCK_USERS_EVENT("total.unblock.users.event",
            "Total unblock users event"),
    TOTAL_UNBLOCK_USERS_EVENT_BY_ADMIN("total.unblock.users.event.by.admin",
            "Total unblock users event by admin"),
    TOTAL_UNBLOCK_USERS_EVENT_BY_EXPIRATION("total.unblock.users.event.by.expiration",
            "Total unblock users event by expiration"),
    TOTAL_ADDED_USERS("total.added.users",
            "Total added users"),
    TOTAL_DELETED_USERS("total.deleted.users",
            "Total deleted users"),
    TOTAL_UPDATED_USERS("total.updated.users",
            "Total updated users");

    private final String metricName;
    private final String description;

    UidamMetrics(String metricName, String description) {
        this.metricName = metricName;
        this.description = description;
    }
}
