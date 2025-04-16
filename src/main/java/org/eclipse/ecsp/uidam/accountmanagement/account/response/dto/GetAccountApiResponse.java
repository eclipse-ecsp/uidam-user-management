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

package org.eclipse.ecsp.uidam.accountmanagement.account.response.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;

import java.sql.Timestamp;
import java.util.Set;
import static lombok.AccessLevel.PROTECTED;
/**
 * Represents an account response including auditing fields.
 */

@Getter
@Setter
@FieldDefaults(level = PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetAccountApiResponse {
    @Schema(description = "account id", requiredMode = Schema.RequiredMode.REQUIRED)
    String id;
    @Schema(description = "account name", requiredMode = Schema.RequiredMode.REQUIRED)
    String accountName;
    @Schema(description = "parent id")
    String parentId;
    @Schema(description = "roles", requiredMode = Schema.RequiredMode.REQUIRED)
    Set<String> roles;
    @Schema(description = "status", requiredMode = Schema.RequiredMode.REQUIRED)
    AccountStatus status;
    @Schema(description = "account creation user", requiredMode = Schema.RequiredMode.REQUIRED)
    String createdBy;
    @Schema(description = "account creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED)
    Timestamp createDate;
    @Schema(description = "last updated user")
    String updatedBy;
    @Schema(description = "timestamp of latest account update")
    Timestamp updateDate;
}
