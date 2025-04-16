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

package org.eclipse.ecsp.uidam.usermanagement.auth.response.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.usermanagement.entity.RoleScopeMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.utilities.BigIntegerToStringSerializer;
import java.math.BigInteger;
import java.util.List;

/**
 * Pojo class for create role response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleCreateResponse {
    @JsonSerialize(using = BigIntegerToStringSerializer.class)
    private BigInteger id;

    private String name;
    private String description;
    @JsonIgnore
    private List<RoleScopeMappingEntity> roleScopeMapping;

    private List<Scope> scopes;

}
