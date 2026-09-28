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

package org.eclipse.ecsp.uidam.usermanagement.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ENUM with client status.
 */
@Schema(type = "string", allowableValues = {"approved", "deleted", "registered", "rejected"})
public enum ClientStatus {

    APPROVED("approved"), DELETED("deleted"), REGISTERED("registered"), REJECTED("rejected");

    String value;

    private ClientStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Resolve a client status from its JSON value or enum name.
     *
     * @param value client status value from the request payload.
     * @return matching ClientStatus enum.
     */
    @JsonCreator
    public static ClientStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ClientStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid client status: " + value);
    }

}
