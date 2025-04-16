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

package org.eclipse.ecsp.uidam.usermanagement.mapper;

import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolesCreateRequestDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * This is a mapper class to convert role dto to entity object and vice versa.
 */
@Mapper
public interface RoleMapper {
    RoleMapper MAPPER = Mappers.getMapper(RoleMapper.class);

    RoleCreateResponse mapToRole(RolesEntity rolesEntity);

    RolesEntity mapToEntity(RolesCreateRequestDto request);

    List<RoleCreateResponse> mapToRolesList(List<RolesEntity> scopeModel);
}
