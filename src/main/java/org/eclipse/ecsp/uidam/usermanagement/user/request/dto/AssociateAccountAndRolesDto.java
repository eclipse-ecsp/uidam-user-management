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

package org.eclipse.ecsp.uidam.usermanagement.user.request.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.usermanagement.enums.OperationPriority;

import java.math.BigInteger;
import java.util.UUID;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASSOCIATE_USER_ACCOUNT_ROLE_REGEX;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_OP_PATTERN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_PATH_PATTERN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.OP_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.OP_PATTERN_FOR_ROLE_ASSOCIATION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;

/**
 * AssociateAccountAndRolesDto to associate user with account and roles.
 */
@Getter
@Setter
@NoArgsConstructor
public class AssociateAccountAndRolesDto {
    private static final long serialVersionUID = 5133282341755331556L;

    @Schema(description = OP_FIELD_DESCRIPTION, example = "ADD",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = MISSING_MANDATORY_PARAMETERS)
    @Pattern(regexp = OP_PATTERN_FOR_ROLE_ASSOCIATION, message = INVALID_OP_PATTERN)
    private  String op;
    @NotBlank(message = MISSING_MANDATORY_PARAMETERS)
    @Pattern(regexp = ASSOCIATE_USER_ACCOUNT_ROLE_REGEX, message = INVALID_PATH_PATTERN)
    private  String path;

    private  String value;

    @Schema(hidden = true)
    private OperationPriority operationPriority;
    @Schema(hidden = true)
    private BigInteger accountId;

    /**
     * Created dto object for the payload.
     *
     * @param op operation
     * @param path path of the payload
     * @param value value of the payload which has account name or role name
     */
    public AssociateAccountAndRolesDto(String op, String path, String value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }

}