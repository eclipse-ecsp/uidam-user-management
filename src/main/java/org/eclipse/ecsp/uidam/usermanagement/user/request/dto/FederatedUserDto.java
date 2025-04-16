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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IDENTITY_PROVIDER_NAME_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;

/**
 * Federated User Request Dto.
 */
@Getter
@Setter
@NoArgsConstructor
public class FederatedUserDto extends UserDtoV1 {

    @JsonView(UserDtoViews.UserDtoV1FederatedView.class)
    @Schema(description = IDENTITY_PROVIDER_NAME_FIELD_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED,
        example = "Google")
    @NotBlank(message = MISSING_MANDATORY_PARAMETERS)
    @JsonProperty("identity_provider_name")
    protected String identityProviderName;
}
