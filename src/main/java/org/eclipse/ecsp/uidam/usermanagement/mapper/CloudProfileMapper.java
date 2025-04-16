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

import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfilePatch;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfileRequest;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.response.dto.CloudProfileResponse;
import org.eclipse.ecsp.uidam.usermanagement.entity.CloudProfileEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 *  Mapper class for cloud profile.
 */
@Mapper(componentModel = "spring")
public interface CloudProfileMapper {
    CloudProfileMapper CLOUD_PROFILE_MAPPER = Mappers.getMapper(CloudProfileMapper.class);

    CloudProfileResponse cloudProfileEntityToCloudProfileResponse(CloudProfileEntity cloudProfileEntity);

    CloudProfileEntity cloudProfileRequestToCloudProfileEntity(CloudProfileRequest cloudProfileRequest);

    void updateCloudProfileFromDto(CloudProfileRequest dto, @MappingTarget CloudProfileEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCloudProfileFromPatchDto(CloudProfilePatch dto, @MappingTarget CloudProfileEntity entity);

}
