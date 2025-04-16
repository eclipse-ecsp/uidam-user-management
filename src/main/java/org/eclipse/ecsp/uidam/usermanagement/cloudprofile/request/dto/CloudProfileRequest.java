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

package org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigInteger;
import java.util.Map;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_LENGTH;

/**
 *  CloudProfile Request DTO.
 */
@Data
public class CloudProfileRequest {
    public static final int MIN_LENGTH = 1;
    public static final int MAX_CLOUD_PROFILE_NAME_LENGTH = 49;

    @Schema(description = "User ID.")
    protected BigInteger userId;

    @Schema(description = "Cloud profile name. Maximum length = "
        + MAX_CLOUD_PROFILE_NAME_LENGTH + " chars", example = "userProfile")
    @Size(min = MIN_LENGTH, max = MAX_CLOUD_PROFILE_NAME_LENGTH, message = INVALID_LENGTH)
    protected String cloudProfileName;

    @Schema(description = "cloud profile")
    protected Map<String, Object> cloudProfileData;

    @Schema(hidden = true)
    private String cloudProfileBusinessKey;
}
