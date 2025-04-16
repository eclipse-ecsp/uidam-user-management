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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;

/**
 * Represents an account data transfer object. This class contains 
 * accountName information to be set on the new account. Extends AccountDtoBase.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateAccountDto extends AccountDtoBase {

    @Schema(description = AccountApiConstants.FieldComments.ACCOUNT_NAME, requiredMode = Schema.RequiredMode.REQUIRED, 
            example = "sampleAccount")
    @NotBlank(message = LocalizationKey.MISSING_MANDATORY_PARAMETERS)
    @Size(min = AccountApiConstants.MIN_ACCOUNTNAME_LENGTH, max = AccountApiConstants.MAX_ACCOUNTNAME_LENGTH, 
        message = LocalizationKey.INVALID_LENGTH)
    protected String accountName;

}
