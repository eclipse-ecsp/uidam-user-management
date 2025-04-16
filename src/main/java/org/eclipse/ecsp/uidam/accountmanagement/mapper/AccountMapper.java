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

package org.eclipse.ecsp.uidam.accountmanagement.mapper;

import io.micrometer.common.util.StringUtils;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.CreateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.UpdateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.GetAccountApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.util.CollectionUtils;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * The AccountMapper interface is responsible for mapping between AccountDto and
 * AccountEntity objects. It provides methods for mapping AccountDto to
 * AccountEntity and vice versa, as well as mapping AccountEntity to
 * AccountResponse.
 */

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AccountMapper {

    AccountMapper ACCOUNT_MAPPER = Mappers.getMapper(AccountMapper.class);


    // Convert from CreateAccountDto to AccountEntity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountName", source = "accountDto.accountName")
    @Mapping(target = "parentId", source = "accountDto.parentId", qualifiedByName = "convertStringToId")
    @Mapping(target = "defaultRoles",
        expression = "java(mapToAccountDefaultRoles(accountDto.getRoles(),roleNameWithId))")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    AccountEntity mapToAccount(CreateAccountDto accountDto, final Map<String, BigInteger> roleNameWithId);

    /**
     * mapToAccountDefaultRoles.
     *
     * @param rolesFromDto   Input request roles.
     * @param roleNameWithId rolename and Id map.
     * @return Set of default account rolesId.
     */
    default Set<BigInteger> mapToAccountDefaultRoles(Set<String> rolesFromDto,
                                                     final Map<String, BigInteger> roleNameWithId) {
        return Optional.ofNullable(rolesFromDto).orElse(Collections.emptySet()).stream()
                .map(roleName -> roleNameWithId.get(roleName.trim()))
                .collect(Collectors.toSet());
    }

    // Convert from AccountEntity to CreateAccountResponse
    @Mapping(target = "id", source = "accountEntity.id", qualifiedByName = "convertIdToString")
    CreateAccountResponse mapToCreateAccountResponse(AccountEntity accountEntity);

    // Convert from AccountEntity to GetAccountApiResponse
    @Mapping(target = "id", source = "accountEntity.id", qualifiedByName = "convertIdToString")
    @Mapping(target = "accountName", source = "accountEntity.accountName")
    @Mapping(target = "parentId", source = "accountEntity.parentId", qualifiedByName = "convertIdToString")
    @Mapping(target = "roles", expression = "java(mapDefaultRolesToResponse(accountEntity,roleNameWithId))")
    @Mapping(target = "status", source = "accountEntity.status")
    @Mapping(target = "createdBy", source = "accountEntity.createdBy")
    @Mapping(target = "createDate", source = "accountEntity.createDate")
    @Mapping(target = "updatedBy", source = "accountEntity.updatedBy")
    @Mapping(target = "updateDate", source = "accountEntity.updateDate")
    GetAccountApiResponse mapToGetAccountApiResponse(AccountEntity accountEntity,
            final Map<String, BigInteger> roleNameWithId);

    /**
     * Map default roles from entity to response.
     *
     * @param accountEntity accountEntity object from the database.
     * @return return roles as set of Strings.
     */
    default Set<String> mapDefaultRolesToResponse(AccountEntity accountEntity,
                                                  final Map<String, BigInteger> roleNameWithId) {
        if (roleNameWithId == null || roleNameWithId.isEmpty()) {
            return new HashSet<>();
        }
        return roleNameWithId.entrySet().stream()
                .filter(entry -> !CollectionUtils.isEmpty(accountEntity.getDefaultRoles())
                        && accountEntity.getDefaultRoles().contains(entry.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toSet());

    }


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountName", ignore = true)
    @Mapping(target = "parentId", source = "updateAccountDto.parentId", qualifiedByName = "convertStringToId")
    @Mapping(target = "defaultRoles",
            expression = "java(updateAccountDto.getRoles() != null ? "
            + "mapToAccountDefaultRoles(updateAccountDto.getRoles(), roleNameWithId) : entity.getDefaultRoles())")
    @Mapping(target = "status", source = "updateAccountDto.status")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    void updateAccountEntity(UpdateAccountDto updateAccountDto, @MappingTarget AccountEntity entity,
            final Map<String, BigInteger> roleNameWithId);

    @Named("convertToString")
    static String convertToString(Set<String> roles) {
        return Optional.ofNullable(roles).orElse(Collections.emptySet()).stream()
                .map(String::trim).collect(Collectors.joining(","));
    }

    @Named("convertStringToId")
    static BigInteger convertStringToId(String id) {
        return StringUtils.isNotEmpty(id) ? new BigInteger(id) :  null;
    }

    @Named("convertIdToString")
    static String convertIdToString(BigInteger id) {
        return id != null ? id.toString() : null;
    }

    default Timestamp mapDateToTimestamp(Date date) {
        return date != null ? new Timestamp(date.getTime()) : null;
    }
}
