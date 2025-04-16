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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.Set;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_COUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_ELEMENT_LENGTH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_NULL_ELEMENT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.MAX_ACCOUNTNAME_LENGTH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.MINIMUM_PATTERN_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MAX_PER_SET;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest.MIN_LENGTH;

/**
 * UsersGetFilter pojo to filter user based on criteria.
 */
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class UsersGetFilterV2 extends UsersGetFilterBase {
    
    @Schema(description = "List of Account Names")
    @Size(min = MIN_LENGTH, max = MAX_PER_SET, message = INVALID_COUNT)
    @Valid
    private Set<@NotNull(message = INVALID_NULL_ELEMENT)
        @Size(min = MINIMUM_PATTERN_LENGTH, max = MAX_ACCOUNTNAME_LENGTH,
        message = INVALID_ELEMENT_LENGTH) String> accountNames;   
    
}
