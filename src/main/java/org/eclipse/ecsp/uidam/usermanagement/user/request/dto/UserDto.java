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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import java.util.Set;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CLIENT_ID_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IS_EXTERNAL_USER_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PASS_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PASS_REGEXP;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ROLE_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STATUS_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME_REGEXP;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_PASS_PATTERN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_USERNAME_PATTERN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;


/**
 * User Request Dto.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserDto extends UserRequest {
    @Schema(description = USERNAME_FIELD_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED, example = "johnd")
    @NotBlank(message = MISSING_MANDATORY_PARAMETERS)
    @Pattern(regexp = USERNAME_REGEXP, message = INVALID_INPUT_USERNAME_PATTERN)
    @Size(min = MIN_LENGTH, max = MAX_USERNAME_LENGTH, message = INVALID_LENGTH)
    protected String userName;

    @Schema(description = PASS_FIELD_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED, example = "Aa1234")
    @Pattern(regexp = PASS_REGEXP, message = INVALID_INPUT_PASS_PATTERN)
    @NotBlank(message = MISSING_MANDATORY_PARAMETERS)
    protected String password;

    @Schema(description = ROLE_FIELD_DESCRIPTION, example = "[\"VEHICLE_OWNER\"]",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = MISSING_MANDATORY_PARAMETERS)
    protected Set<String> roles;

    @Schema(description = STATUS_FIELD_DESCRIPTION, example = "PENDING", hidden = true,
        requiredMode = Schema.RequiredMode.REQUIRED)
    protected UserStatus status;

    @Schema(description = IS_EXTERNAL_USER_DESCRIPTION, example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("is_external_user")
    protected Boolean isExternalUser;

    @Schema(description = CLIENT_ID_DESCRIPTION, example = "k8-qa-dmportal")
    @JsonAlias({"aud", "client_id"})
    private String aud;

    public void setUserName(String userName) {
        this.userName = trimToNull(lowerCase(userName));
    }

    @Override
    public String toString() {
        return "UserPost [userName=" + userName + ", password=" + "********" + ", role=" + roles + ", status=" + status
            + "]";
    }
}
