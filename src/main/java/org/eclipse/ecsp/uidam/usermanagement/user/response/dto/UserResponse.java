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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.eclipse.ecsp.uidam.usermanagement.enums.Gender;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static lombok.AccessLevel.PROTECTED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STRING;

/**
 * UserResponse Pojo.
 */
@Getter
@Setter
@FieldDefaults(level = PROTECTED)
public class UserResponse {
    @Schema(description = "user id", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id;
    @Schema(description = "userName", requiredMode = Schema.RequiredMode.REQUIRED)
    String userName;
    @Schema(description = "Roles", requiredMode = Schema.RequiredMode.REQUIRED)
    Set<String> roles;
    @Schema(description = "status", requiredMode = Schema.RequiredMode.REQUIRED)
    UserStatus status;
    @Schema(description = "firstName")
    String firstName;
    @Schema(description = "lastName")
    String lastName;
    @Schema(description = "country")
    String country;
    @Schema(description = "state")
    String state;
    @Schema(description = "city")
    String city;
    @Schema(description = "address1")
    String address1;
    @Schema(description = "address2")
    String address2;
    @Schema(description = "postal code")
    String postalCode;
    @Schema(description = "user's phone number")
    String phoneNumber;
    @Schema(description = "user's Email")
    String email;
    @Schema(description = "user's gender", type = STRING, allowableValues = "MALE, FEMALE")
    Gender gender;
    @Schema(description = "Birth Date in ISO 8601 format (yyyy-MM-dd)", example = "1997-10-13")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    LocalDate birthDate;
    @Schema(description = "user's locale")
    String locale;
    /**
     * field for notification micro-service, is the user allow to receive notifications.
     */
    @Schema(description = "did the user approve to receive notifications", example = "true")
    Boolean notificationConsent;
    /**
     * Time zone of the user.
     */
    @Schema(description = "IST", example = "IST")
    String timeZone;
    @Schema(description = "List of user's devices IDs")
    Set<String> devIds = new HashSet<>();
    Map<String, Object> additionalAttributes = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes;
    }

    /**
     * Setter for additional attributes.
     *
     * @param name attribute name
     * @param value attribute value
     */
    @JsonAnySetter
    public void setAdditionalAttributes(String name, Object value) {
        if (value != null) {
            additionalAttributes.put(name, value);
        }
    }
}
