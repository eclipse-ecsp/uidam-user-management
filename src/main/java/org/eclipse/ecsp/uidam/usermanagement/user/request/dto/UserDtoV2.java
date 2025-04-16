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

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;

import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCOUNTS_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ROLE_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;

/**
 * User Request Dto with accounts and roles info.
 */
@Getter
@Setter
@NoArgsConstructor
@Valid
public class UserDtoV2 extends UserDtoBase {
    
    @JsonView(UserDtoViews.UserDtoV2View.class)
    @Schema(description = ACCOUNTS_FIELD_DESCRIPTION, example = "array of set of account name and roles",
            requiredMode = Schema.RequiredMode.REQUIRED) 
    @NotEmpty(message = MISSING_MANDATORY_PARAMETERS)
    @Valid
    protected Set<UserAccountsAndRoles> accounts;

    /**
     * Inner class to receive account name and roles.
     */
    @Valid
    @JsonView(UserDtoViews.UserDtoV2View.class)
    public static class UserAccountsAndRoles {        
        @Schema(description = AccountApiConstants.FieldComments.ACCOUNT_NAME, 
                requiredMode = Schema.RequiredMode.REQUIRED, 
                example = "devAccount")
        @NotBlank(message = LocalizationKey.MISSING_MANDATORY_PARAMETERS)
        protected String account;
        
        @Schema(description = ROLE_FIELD_DESCRIPTION, example = "[\"VEHICLE_OWNER\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = MISSING_MANDATORY_PARAMETERS)
        protected Set<@NotBlank(message = MISSING_MANDATORY_PARAMETERS) String> roles;

        public UserAccountsAndRoles() {}

        public UserAccountsAndRoles(String account,
                Set<@NotBlank(message = MISSING_MANDATORY_PARAMETERS) String> roles) {
            this.account = account;
            this.roles = roles;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }        
    }
}
