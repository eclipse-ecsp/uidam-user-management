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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientStatus;
import java.util.Set;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_ELEMENT_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_NULL_ELEMENT;

/**
 * Filter criteria used to search registered oauth2 clients.
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ClientFilterDto {

    public static final int MIN_PER_SET = 1;
    public static final int MAX_PER_SET = 50;
    public static final int MIN_ELEMENT_LENGTH = 1;
    public static final int MAX_CLIENT_ID_LENGTH = 255;
    public static final int MAX_CLIENT_NAME_LENGTH = 255;

    @Schema(description = "List of client ids")
    @Size(min = MIN_PER_SET, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_ELEMENT_LENGTH, max = MAX_CLIENT_ID_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> clientIds;

    @Schema(description = "List of client names")
    @Size(min = MIN_PER_SET, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_ELEMENT_LENGTH, max = MAX_CLIENT_NAME_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> clientNames;

    @ArraySchema(schema = @Schema(implementation = ClientStatus.class,
        allowableValues = {"approved", "deleted", "registered", "rejected"}))
    @Size(min = MIN_PER_SET, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT) ClientStatus> statuses;

    /**
     * Client entity fields available for filtering and sorting.
     */
    public enum ClientFilterDtoEnum {
        CLIENT_IDS("clientId"),
        CLIENT_NAMES("clientName"),
        STATUS("status"),
        CREATED_DATE("createDate"),
        UPDATED_DATE("updateDate");

        private final String field;

        ClientFilterDtoEnum(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }
}
