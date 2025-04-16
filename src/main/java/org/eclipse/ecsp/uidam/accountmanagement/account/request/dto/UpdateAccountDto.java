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

package org.eclipse.ecsp.uidam.accountmanagement.account.request.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STATUS_FIELD_DESCRIPTION;

/**
 * Represents an account data transfer object. This class contains status 
 * to be updated on the account. Extends AccountDtoBase.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateAccountDto extends AccountDtoBase {

    @Schema(description = STATUS_FIELD_DESCRIPTION, example = "PENDING", requiredMode = Schema.RequiredMode.REQUIRED)
    protected AccountStatus status;

}
