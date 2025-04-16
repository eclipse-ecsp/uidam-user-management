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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import java.util.Set;

/**
 * pojo class for scopes api response.
 */
public class ScopeListRepresentation extends BaseRepresentation {

    @JsonProperty(value = ApiConstants.RESULTS)
    private Set<Scope> scopes;

    /**
     * method to set error message, if any.
     *
     * @param error - error message
     * @return ScopeListRepresentation
     */
    public static ScopeListRepresentation error(String error) {
        ScopeListRepresentation errorRepresentation = new ScopeListRepresentation();
        errorRepresentation.addMessage(new ResponseMessage(error));
        return errorRepresentation;
    }

    public Set<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(Set<Scope> scopes) {
        this.scopes = scopes;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
