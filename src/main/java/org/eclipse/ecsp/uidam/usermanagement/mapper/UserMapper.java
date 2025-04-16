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

import org.eclipse.ecsp.uidam.usermanagement.entity.UserAddressEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoBase;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserMetaDataRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserDetailsResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserMetaDataResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV2;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

/**
 * Mapper implementation for userController apis.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    
    UserMapper USER_MAPPER = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "userAddresses", expression = "java(mapToUserAddress(userDto))")    
    UserEntity mapToUser(UserDtoBase userDto);
        
    /**
     * Map userDto to user address entity.
     *
     * @param userDto userDto
     * @return UserAddressEntity
     */
    default List<UserAddressEntity> mapToUserAddress(UserDtoBase userDto) {
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1(userDto.getAddress1());
        userAddress.setAddress2(userDto.getAddress2());
        userAddress.setCity(userDto.getCity());
        userAddress.setCountry(userDto.getCountry());
        userAddress.setTimeZone(userDto.getTimeZone());
        userAddress.setState(userDto.getState());
        userAddress.setPostalCode(userDto.getPostalCode());
        return List.of(userAddress);
    }

    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getCity())", target = "city")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getAddress1())", target = "address1")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getAddress2())", target = "address2")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getCountry())", target = "country")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getTimeZone())", target = "timeZone")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getState())", target = "state")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getPostalCode())", target = "postalCode")
    @Mapping(source = "id", target = "id")    
    UserResponseV1 mapToUserResponseV1(UserEntity userEntity);
    
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getCity())", target = "city")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getAddress1())", target = "address1")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getAddress2())", target = "address2")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getCountry())", target = "country")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getTimeZone())", target = "timeZone")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getState())", target = "state")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getPostalCode())", target = "postalCode")
    @Mapping(source = "id", target = "id")    
    UserResponseV2 mapToUserResponseV2(UserEntity userEntity);
    
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getCity())", target = "city")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getAddress1())", target = "address1")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getAddress2())", target = "address2")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getCountry())", target = "country")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getTimeZone())", target = "timeZone")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
        + "userEntity.getUserAddresses().get(0).getState())", target = "state")
    @Mapping(expression = "java(userEntity.getUserAddresses().isEmpty() ? null : "
            + "userEntity.getUserAddresses().get(0).getPostalCode())", target = "postalCode")
    @Mapping(source = "id", target = "id")    
    UserResponseBase mapToUserResponse(UserEntity userEntity);

    @Mapping(target = "id", expression = "java(mapIdToUserId(userEntity))")
    @Mapping(source = "userPassword", target = "password")
    @Mapping(source = "passwordSalt", target = "salt")
    @Mapping(source = "status", target = "status")
    UserDetailsResponse mapToUserDetailsResponse(UserEntity userEntity);

    @Mapping(source = "types", target = "type")
    @Mapping(source = "isUnique", target = "unique")
    UserMetaDataResponse mapToMetaDataResponse(UserAttributeEntity userAttributeEntity);

    @Mapping(source = "type", target = "types")
    @Mapping(source = "unique", target = "isUnique")
    UserAttributeEntity mapToMetaDataEntity(UserMetaDataRequest userMetaDataRequest);

    @Mapping(source = "type", target = "types")
    @Mapping(source = "unique", target = "isUnique")
    void updateMetaDataEntity(UserMetaDataRequest userMetaDataRequest,
                              @MappingTarget UserAttributeEntity userAttributeEntity);

    default String mapIdToUserId(UserEntity userEntity) {
        return String.valueOf(userEntity.getId());
    }


    @Mapping(target = "pwdChangedtime", ignore = true)
    @Mapping(target = "userAddresses", ignore = true)
    @Mapping(target = "devIds", ignore = true)
    @Mapping(target = "accountRoleMapping", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserEntityData(Map<String, String> userMap, @MappingTarget UserEntity userEntity);

    @Mapping(target = "userEntity", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserAddressEntityData(Map<String, String> key, @MappingTarget UserAddressEntity userAddressEntity);
}