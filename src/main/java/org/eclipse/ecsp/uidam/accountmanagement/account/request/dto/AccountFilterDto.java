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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;

import java.math.BigInteger;
import java.util.Set;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_COUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_ELEMENT_LENGTH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_NULL_ELEMENT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_ROLE_PATTERN;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.MAX_ACCOUNTNAME_LENGTH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.MAX_ROLE_LENGTH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.MINIMUM_PATTERN_LENGTH;


/**
 * Represents an account filter data transfer object. This class contains information
 * about list of account's Name, Id's, Parent Id's, roles and status which will be used for filtering
 * the accounts
 */
@NoArgsConstructor
@Getter
@Setter
public class AccountFilterDto {

    public static final int MAX_PER_SET = 50;
    public static final int MIN_PER_SET = 1;


    @Schema(description = "List of Account IDs")
    @Size(min = MIN_PER_SET, max = MAX_PER_SET, message = INVALID_COUNT)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT) BigInteger> ids;

    @Schema(description = "List of Account Names")
    @Size(min = MIN_PER_SET, max = MAX_PER_SET, message = INVALID_COUNT)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MINIMUM_PATTERN_LENGTH, max = MAX_ACCOUNTNAME_LENGTH,
        message = INVALID_ELEMENT_LENGTH) String> accountNames;

    @Schema(description = "List of Parent Ids")
    @Size(min = MIN_PER_SET, max = MAX_PER_SET, message = INVALID_COUNT)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT) BigInteger> parentIds;

    @Size(min = MIN_PER_SET, max = MAX_PER_SET, message = INVALID_COUNT)
    @Valid
    @Schema(description = "List of Account roles")
    private Set<@NotNull(message = INVALID_NULL_ELEMENT)    
        @Size(min = MINIMUM_PATTERN_LENGTH, max = MAX_ROLE_LENGTH,
        message = INVALID_ELEMENT_LENGTH)
        @Pattern(regexp = "[A-Za-z_-]+", message = INVALID_ROLE_PATTERN) String> roles;
    
    @Size(min = MIN_PER_SET, max = MAX_PER_SET, message = INVALID_COUNT)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT) AccountStatus> status;

    /**
     * Enum constants used search by field of the dto.
     *
     */
    public enum AccountFilterDtoEnum {
        IDS("id"),
        ACCOUNT_NAMES("accountName"),
        PARENTIDS("parentId"),
        ROLES("defaultRoles"),
        STATUS("status");

        private String field;

        AccountFilterDtoEnum(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }
}
