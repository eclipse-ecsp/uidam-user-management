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
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ROLE_FIELD_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;

/**
 * User Request Dto for version v1.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserDtoV1 extends UserDtoBase {  
    
    @JsonView({ UserDtoViews.UserDtoV1View.class, UserDtoViews.UserDtoV1ExternalView.class,
      UserDtoViews.UserDtoV1SelfView.class, UserDtoViews.UserDtoV1FederatedView.class })
    @Schema(description = ROLE_FIELD_DESCRIPTION, example = "[\"VEHICLE_OWNER\"]",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = MISSING_MANDATORY_PARAMETERS)
    protected Set<String> roles;
    
    @Override
    public String toString() {
        return "UserPost [userName=" + super.userName + ", password=" + "********" + ", role=" + roles 
            + ", status=" + super.status + "]";
    }
}
