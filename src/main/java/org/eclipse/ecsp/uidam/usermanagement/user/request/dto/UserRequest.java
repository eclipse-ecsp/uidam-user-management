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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.eclipse.ecsp.uidam.usermanagement.enums.Gender;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import static lombok.AccessLevel.PROTECTED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_EMAIL;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_PHONE_NUMBER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;

/**
 * UserRequest Pojo.
 */
@Getter
@Setter
@FieldDefaults(level = PROTECTED)
@JsonView(UserDtoViews.BaseView.class)
public abstract class UserRequest {
    public static final int MIN_LENGTH = 1;
    public static final int OPTIONAL_MIN_LENGTH = 0;
    public static final int MAX_USERNAME_LENGTH = 254;
    public static final int MAX_ID_LENGTH = 30;
    public static final int MAX_NAME_LENGTH = 49;
    public static final int MAX_COUNTRY_LENGTH = 50;
    public static final int MAX_STATE_LENGTH = 50;
    public static final int MAX_CITY_LENGTH = 50;
    public static final int MAX_ADDRESS1_LENGTH = 50;
    public static final int MAX_ADDRESS2_LENGTH = 100;
    public static final int MAX_POSTAL_CODE_LENGTH = 11;
    public static final int MAX_PHONE_NUMBER_LENGTH = 16;
    public static final int MIN_EMAIL_LENGTH = 3;
    public static final int MAX_EMAIL_LENGTH = 128;
    public static final int MAX_DEV_ID_LENGTH = 256;
    public static final int MAX_PER_SET = 50;
    public static final int MAX_LOCALE_LENGTH = 35;

    
    @Schema(description = "First name. Maximum length = " + MAX_NAME_LENGTH + " chars", example = "John")
    @Size(min = MIN_LENGTH, max = MAX_NAME_LENGTH, message = INVALID_LENGTH)
    String firstName;

    @Schema(description = "Last name. Maximum length = " + MAX_NAME_LENGTH + " chars", example = "Doe")
    @Size(min = OPTIONAL_MIN_LENGTH, max = MAX_NAME_LENGTH, message = INVALID_LENGTH)
    String lastName;

    @Schema(description = "Country. Maximum length = " + MAX_COUNTRY_LENGTH + " chars", example = "USA")
    @Size(min = MIN_LENGTH, max = MAX_COUNTRY_LENGTH, message = INVALID_LENGTH)
    String country;

    @Schema(description = "State. Maximum length = " + MAX_STATE_LENGTH + " chars", example = "Illinois")
    @Size(min = MIN_LENGTH, max = MAX_STATE_LENGTH, message = INVALID_LENGTH)
    String state;

    @Schema(description = "City. Maximum length = " + MAX_CITY_LENGTH + " chars", example = "Chicago")
    @Size(min = MIN_LENGTH, max = MAX_CITY_LENGTH, message = INVALID_LENGTH)
    String city;

    @Schema(description = "Address. Maximum length = " + MAX_ADDRESS1_LENGTH + " chars", example = "5801")
    @Size(min = MIN_LENGTH, max = MAX_ADDRESS1_LENGTH, message = INVALID_LENGTH)
    String address1;

    @Schema(description = "Address. Maximum length = " + MAX_ADDRESS2_LENGTH + " chars", example = "S Ellis Ave")
    @Size(min = MIN_LENGTH, max = MAX_ADDRESS2_LENGTH, message = INVALID_LENGTH)
    String address2;

    @Schema(description = "Postal Code. Maximum length = " + MAX_POSTAL_CODE_LENGTH + " chars", example = "560068")
    @Size(min = MIN_LENGTH, max = MAX_POSTAL_CODE_LENGTH, message = INVALID_LENGTH)
    String postalCode;

    @Schema(description = "Phone number. Minimum length = 5, Maximum length = 16 chars", example = "+17535011234")
    @Pattern(regexp = "^\\+[0-9]{3,14}[0-9]$", message = INVALID_PHONE_NUMBER)
    String phoneNumber;
    
    @Schema(description = "Email address. Minimum length = " + MIN_EMAIL_LENGTH + ", Maximum length = "
        + MAX_EMAIL_LENGTH + " chars", example = "john.doe@domain.com")
    @Size(min = MIN_EMAIL_LENGTH, max = MAX_EMAIL_LENGTH, message = INVALID_LENGTH)
    @Email(message = INVALID_EMAIL)
    @NotEmpty(message = MISSING_MANDATORY_PARAMETERS)
    String email;

    @Schema(description = "Gender")
    Gender gender;

    @Schema(description = "Birth Date in ISO 8601 format (yyyy-MM-dd)", example = "1997-10-13")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    LocalDate birthDate;

    @Schema(description = "Locale. Maximum length = " + MAX_LOCALE_LENGTH + " chars", example = "en_US")
    @Size(min = MIN_LENGTH, max = MAX_LOCALE_LENGTH, message = INVALID_LENGTH)
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

    @Setter(AccessLevel.NONE)
    Map<String, Object> additionalAttributes = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes;
    }

    @JsonAnySetter
    public void setAdditionalAttributes(String name, Object value) {
        additionalAttributes.put(name, value);
    }

}
