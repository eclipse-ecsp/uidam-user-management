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

package org.eclipse.ecsp.uidam.usermanagement.user.response.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IS_VERIFIED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TOKEN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UPDATE_TIME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID_VARIABLE;

/**
 * Email Verification Response.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationResponse {

    @Schema(description = ID, example = "13549632802740079718274133226102",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = USER_ID_VARIABLE, example = "13549632802740079718274133226102",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = TOKEN, example = "bsavshfafsjasa", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @Schema(description = UPDATE_TIME, example = "1900681", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @Schema(description = IS_VERIFIED, example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isVerified;

}
