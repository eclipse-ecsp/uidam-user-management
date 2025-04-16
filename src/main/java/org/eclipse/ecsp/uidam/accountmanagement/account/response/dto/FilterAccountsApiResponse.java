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
import java.util.List;
import static lombok.AccessLevel.PROTECTED;

/**
 * Represents an list of account response including auditing fields.
 */

@Getter
@Setter
@FieldDefaults(level = PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterAccountsApiResponse {
    @Schema(description = "List of Filtered Accounts", requiredMode = Schema.RequiredMode.REQUIRED)
    List<GetAccountApiResponse> items;
}
