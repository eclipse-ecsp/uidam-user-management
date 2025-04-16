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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import java.util.Set;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * pojo class for roles api response.
 */
@JsonInclude(NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RoleListRepresentation extends BaseRepresentation {

    @JsonProperty(value = ApiConstants.RESULTS)
    private Set<RoleCreateResponse> roles;

    /**
     * method to set error message, if any.
     *
     * @param error - error message
     * @return RoleListRepresentation
     */
    public static RoleListRepresentation error(String error) {
        RoleListRepresentation errorRepresentation = new RoleListRepresentation();
        errorRepresentation.addMessage(new ResponseMessage(error));
        return errorRepresentation;
    }

    /**
     * method to set error message with parameters, if any.
     *
     * @param error - error message
     * @param params - parameters
     * @return RoleListRepresentation
     */
    public static RoleListRepresentation error(String error, String params) {
        RoleListRepresentation errorRepresentation = new RoleListRepresentation();
        errorRepresentation.addMessage(new ResponseMessage(error, params));
        return errorRepresentation;
    }
}
