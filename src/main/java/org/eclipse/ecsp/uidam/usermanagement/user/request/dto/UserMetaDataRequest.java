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
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.ATTRIBUTE_LABEL_REQUIRED_FOR_STATIC;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;

/**
 * UserMetaData Request Pojo.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserMetaDataRequest {
    public static final int MIN_LENGTH = 1;
    public static final int MAX_FIELD_NAME_LENGTH = 50;

    @Schema(description = "name", example = "custom:companyName")
    @NotBlank(message = MISSING_MANDATORY_PARAMETERS)
    @Size(min = MIN_LENGTH, max = MAX_FIELD_NAME_LENGTH, message = INVALID_LENGTH)
    private String name;
    @Schema(description = "mandatory", example = "false")
    private Boolean mandatory;
    @Schema(description = "unique", example = "false")
    private Boolean unique;
    @Schema(description = "readOnly", example = "false")
    private Boolean readOnly;
    @Schema(description = "searchable", example = "true")
    private Boolean searchable;
    @Schema(description = "Marks a field as a dynamic attribute", example = "true", hidden = true)
    private boolean dynamicAttribute;
    @Schema(description = "type", example = "varchar")
    private String type;
    @Schema(description = "regex", example = "[a-zA-Z]{1,13}")
    private String regex;
    @Schema(description = "Human-readable label shown in the signup UI instead of the attribute name",
            example = "Company Name")
    private String attributeLabel;

    /**
     * Validates that {@code attributeLabel} is mandatory when {@code dynamicAttribute} is {@code false}.
     *
     * @return {@code true} if the constraint is satisfied
     */
    @AssertTrue(message = ATTRIBUTE_LABEL_REQUIRED_FOR_STATIC)
    public boolean isAttributeLabelValidWhenStatic() {
        return dynamicAttribute || StringUtils.hasText(attributeLabel);
    }
}
