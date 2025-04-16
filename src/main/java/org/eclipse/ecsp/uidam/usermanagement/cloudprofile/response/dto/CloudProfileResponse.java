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

package org.eclipse.ecsp.uidam.usermanagement.cloudprofile.response.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.util.Map;

/**
 *  CloudProfile response pojo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class CloudProfileResponse {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "user id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "profile name")
    private String cloudProfileName;

    @Schema(description = "cloud profile")
    private Map<String, Object> cloudProfileData;

    private static final int INITIAL_ODD_NUMBER = 17;
    private static final int MULTIPLIER_ODD_NUMBER = 37;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CloudProfileResponse cloudProfile = (CloudProfileResponse) o;

        return new EqualsBuilder()
            .append(id, id)
            .append(userId, cloudProfile.userId)
            .append(cloudProfileName, cloudProfile.cloudProfileName)
            .append(cloudProfileData, cloudProfile.cloudProfileData)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(INITIAL_ODD_NUMBER, MULTIPLIER_ODD_NUMBER)
            .append(id)
            .append(userId)
            .append(cloudProfileName)
            .append(cloudProfileData)
            .toHashCode();
    }
}
