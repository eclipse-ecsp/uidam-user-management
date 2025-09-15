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

/**
 * Constants for UIDAM metrics.
 */
public interface UidamMetricsConstants {
    String USER_TYPE_SELF = "self";
    String USER_TYPE_EXTERNAL = "external";
    String USER_TYPE_FEDERATED = "federated";
    String USER_TYPE_ADMIN = "admin";

    String TAG_NAME_APPLICATION = "application";
    String TAG_NAME_TENANT_ID = "tenantId";
    String TAG_NAME_API_VERSION = "apiVersion";
    String TAG_VALUE_APPLICATION = "uidam-user-management";
    String DEFAULT_API_VERSION = "v1";
}
