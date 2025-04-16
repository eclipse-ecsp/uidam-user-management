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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigInteger;
import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_IDS_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_NULL_ELEMENT;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_PER_SET;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MIN_LENGTH;

/**
 * User change status request data.
 */
@Data
public class UserChangeStatusRequest {
    @Schema(description = USER_IDS_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT) BigInteger> ids;
    private boolean approved;
}
