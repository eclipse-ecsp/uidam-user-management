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

package org.eclipse.ecsp.uidam.usermanagement.auth.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import java.util.Set;

/**
 * pojo class for scope create request.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RolesCreateRequestDto {
    @NotBlank(message = LocalizationKey.MISSING_MANDATORY_PARAMETERS)
    private String name;

    @NotBlank(message = LocalizationKey.MISSING_MANDATORY_PARAMETERS)
    private String description;

    @NotEmpty(message = LocalizationKey.MISSING_MANDATORY_PARAMETERS)
    private Set<String> scopeNames;
}
