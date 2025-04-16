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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.uidam.usermanagement.enums.Gender;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_ELEMENT_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_NULL_ELEMENT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_PHONE_NUMBER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_ROLE_PATTERN;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_ADDRESS1_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_ADDRESS2_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_CITY_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_COUNTRY_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_EMAIL_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_LOCALE_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_NAME_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_PER_SET;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_PHONE_NUMBER_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_POSTAL_CODE_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_STATE_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_USERNAME_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MIN_LENGTH;

/**
 * UsersGetFilter Base pojo to filter user based on criteria.
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class UsersGetFilterBase {
    public static final int MAX_TOKEN_LENGTH = 256;

    @Schema(description = "List of user IDs")
    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT) BigInteger> ids;
    @Schema(description = "List of user-names")
    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_USERNAME_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> userNames;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of user roles")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_NAME_LENGTH,
            message = INVALID_ELEMENT_LENGTH)
        @Pattern(regexp = "[A-Za-z_-]+", message = INVALID_ROLE_PATTERN) String> roles;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of first names")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_NAME_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> firstNames;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of last names")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_NAME_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> lastNames;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of countries")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_COUNTRY_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> countries;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of states")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_STATE_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> states;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of cities")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_CITY_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> cities;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of address line 1 addresses")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_ADDRESS1_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> address1;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of address line 2 addresses")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_ADDRESS2_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> address2;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of postal codes")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_POSTAL_CODE_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> postalCodes;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of phone numbers")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_PHONE_NUMBER_LENGTH,
            message = INVALID_PHONE_NUMBER)
        @Pattern(regexp = "[0-9+]+", message = INVALID_PHONE_NUMBER) String> phoneNumbers;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of emails")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_EMAIL_LENGTH, message = INVALID_ELEMENT_LENGTH) String> emails;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of locales")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_LOCALE_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> locales;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "Gender", allowableValues = {"MALE", "FEMALE"})
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT) Gender> gender;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    @Schema(description = "List of device IDs")
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MIN_LENGTH, max = MAX_TOKEN_LENGTH,
            message = INVALID_ELEMENT_LENGTH) String> devIds;

    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_LENGTH)
    @Valid
    protected Set<@NotNull(message = INVALID_NULL_ELEMENT) UserStatus> status;
    private Map<String, Set<String>> additionalAttributes = new HashMap<>();

    /**
     * Setter to setUserName.
     *
     * @param userNames username values.
     */
    public void setUserNames(Set<String> userNames) {
        this.userNames = userNames.stream()
            .map(userName -> trimToNull(lowerCase(userName)))
            .collect(Collectors.toSet());
    }

    @JsonAnyGetter
    public Map<String, Set<String>> getAdditionalAttributes() {
        return additionalAttributes;
    }

    /**
     * Set additional attributes.
     *
     * @param name attribute name
     * @param value attribute value.
     */
    @JsonAnySetter
    public void setAdditionalAttributes(String name, Set<String> value) {
        additionalAttributes.put(name, value);
    }

    /**
     * Enum to define UserFields for filter.
     */
    public enum UserGetFilterEnum {
        IDS("id"),
        USER_NAMES("userName"),
        ROLES("roles"),
        ROLEIDS("roleId"),
        ACCOUNTIDS("accountId"),
        STATUS("status"),
        FIRST_NAMES("firstName"),
        LAST_NAMES("lastName"),
        COUNTRIES("country"),
        STATES("state"),
        CITIES("city"),
        ADDRESS1("address1"),
        ADDRESS2("address2"),
        POSTAL_CODES("postalCode"),
        PHONE_NUMBERS("phoneNumber"),
        EMAILS("email"),
        GENDER("gender"),
        LOCALES("locale"),
        DEV_IDS("devIds"),
        TIMEZONE("timeZone"),
        BIRTHDATE("birthDate"),
        USERSTATUS("status");
        private String field;

        UserGetFilterEnum(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }
}