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
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.validations.ExternalUserPasswordValidation;
import org.eclipse.ecsp.uidam.usermanagement.validations.UserPasswordValidation;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CLIENT_ID_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PASS_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STATUS_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME_REGEXP;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_USERNAME_PATTERN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;


/**
 * User Request base Dto.
 */
@Getter
@Setter
@NoArgsConstructor
@UserPasswordValidation
@ExternalUserPasswordValidation
public class UserDtoBase extends UserRequest {
    @JsonView(UserDtoViews.BaseView.class)
    @Schema(description = USERNAME_FIELD_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED, example = "johnd")
    @NotBlank(message = MISSING_MANDATORY_PARAMETERS)
    @Pattern(regexp = USERNAME_REGEXP, message = INVALID_INPUT_USERNAME_PATTERN)
    @Size(min = MIN_LENGTH, max = MAX_USERNAME_LENGTH, message = INVALID_LENGTH)
    protected String userName;

    @JsonView({ UserDtoViews.UserDtoV1View.class, 
      UserDtoViews.UserDtoV2View.class, UserDtoViews.UserDtoV1SelfView.class })
    @Schema(description = PASS_FIELD_DESCRIPTION, example = "Aa1234", requiredMode = Schema.RequiredMode.REQUIRED)
    protected String password;

    @JsonView(UserDtoViews.BaseView.class)
    @Schema(description = STATUS_FIELD_DESCRIPTION, example = "PENDING", hidden = true)
    protected UserStatus status;

    @Schema(hidden = true)
    protected Boolean isExternalUser = Boolean.valueOf(false);

    @JsonView({ UserDtoViews.UserDtoV1View.class,
      UserDtoViews.UserDtoV2View.class })
    @Schema(description = CLIENT_ID_DESCRIPTION, example = "k8-qa-dmportal")
    @JsonAlias({"aud", "client_id"})
    private String aud;

    public void setUserName(String userName) {
        this.userName = trimToNull(lowerCase(userName));
    }

    @Override
    public String toString() {
        return "UserPost [userName=" + userName + ", password=" + "********" + ", status=" + status + "]";
    }
}
