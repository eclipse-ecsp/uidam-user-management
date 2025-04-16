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

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.OperationPriority;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserAccountRoleMappingException;
import org.eclipse.ecsp.uidam.usermanagement.exception.handler.ErrorProperty;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.AssociateAccountAndRolesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ADD_OPERATION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ADD_ROLE_PATH_REGEX;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.AMPERSAND;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASSOCIATE_ACCOUNT_PATH_REGEX;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASSOCIATION_REQ_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_PAYLOAD_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_PAYLOAD_FOR_ACCOUNT_ROLE_ASSOCIATION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.REMOVE_OPERATION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.REMOVE_ROLE_PATH_REGEX;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SLASH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ACCOUNT_ROLE_ASSOCIATION_CODE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 *  UserAccountRoleAssociationValidator Utility.
 */
public class UserAccountRoleAssociationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountRoleAssociationValidator.class);

    private UserAccountRoleAssociationValidator() {
        throw new ApplicationRuntimeException("UserAccountRoleAssociationValidator cannot be instantiated!!");
    }

    /**
     * validate the payload and extract the accountId, accountName and role name from it.
     *
     * @param associationRequest payload request
     * @param userId userId of user
     * @param accountIdSet stores accountId from request payload
     * @param roleNameSet stores roleNames from request payload
     * @throws UserAccountRoleMappingException for invalid payload
     */
    public static void validatePayloadForAccountIdAndName(
        List<AssociateAccountAndRolesDto> associationRequest, BigInteger userId,
        Set<BigInteger> accountIdSet, Set<String> roleNameSet)
            throws UserAccountRoleMappingException {
        List<AssociateAccountAndRolesDto> associateAccountAndRolesDtoList = new ArrayList<>();
        List<ErrorProperty> errorPropertyList = new ArrayList<>();
        Set<String> duplicatePayloadSet = new HashSet<>();
        LOGGER.debug("Validating AssociateUserAccountRolePayload for user: {}", userId);
        associationRequest.stream().forEach(associateRole -> {
            String op = associateRole.getOp().toLowerCase();
            String path = associateRole.getPath();
            String value = associateRole.getValue();
            if (op.equals(REMOVE_OPERATION) && path.matches(REMOVE_ROLE_PATH_REGEX)) {
                associateRole.setOperationPriority(OperationPriority.ONE);
                getAccountAndRoleFromPath(accountIdSet, roleNameSet, associateRole);
            } else if (op.equals(REMOVE_OPERATION) && path.matches(ASSOCIATE_ACCOUNT_PATH_REGEX)) {
                associateRole.setOperationPriority(OperationPriority.TWO);
                BigInteger accountId = new BigInteger(getAccountIdFromPath(path));
                accountIdSet.add(accountId);
                associateRole.setAccountId(accountId);
            } else if (op.equals(ADD_OPERATION) && path.matches(ADD_ROLE_PATH_REGEX)
                    && StringUtils.isNotBlank(value)) {
                associateRole.setOperationPriority(OperationPriority.THREE);
                getAccountAndRoleFromPath(accountIdSet, roleNameSet, associateRole);
            } else {
                errorPropertyList.add(new ErrorProperty("user_id", List.of(
                        MessageFormat.format(INVALID_PAYLOAD_FOR_ACCOUNT_ROLE_ASSOCIATION,
                                userId.toString(), associateRole.toString()))));
            }
            if (duplicatePayloadSet.add(associateRole.getOp() + path + associateRole.getValue())) {
                associateAccountAndRolesDtoList.add(associateRole);
            }
        });

        if (!errorPropertyList.isEmpty()) {
            LOGGER.error(ASSOCIATION_REQ_ERROR_MESSAGE, INVALID_PAYLOAD_ERROR_MESSAGE, errorPropertyList);
            throw new UserAccountRoleMappingException(USER_ACCOUNT_ROLE_ASSOCIATION_CODE,
                    INVALID_PAYLOAD_ERROR_MESSAGE, errorPropertyList, BAD_REQUEST);
        }
        associationRequest.clear();
        associationRequest.addAll(associateAccountAndRolesDtoList);
    }

    private static void getAccountAndRoleFromPath(Set<BigInteger> accountIdSet, Set<String> roleNameSet,
            AssociateAccountAndRolesDto associateRole) {
        String path = associateRole.getPath();
        BigInteger accountId = new BigInteger(getAccountIdFromPath(path));
        associateRole.setAccountId(accountId);
        accountIdSet.add(accountId);

        String roleName = associateRole.getValue(); // get the value from the operation
        if (StringUtils.isEmpty(roleName)) { //Path is /account/<1234>/<ROLE_NAME>
            roleName = path.substring(path.lastIndexOf(SLASH) + 1);
            roleNameSet.add(roleName);
            associateRole.setValue(roleName);
        } else {
            roleNameSet.add(roleName);
        }
    }

    protected static String getAccountIdFromPath(String path) {
        int secondIndex = path.indexOf(SLASH, path.indexOf(SLASH) + 1); //second occurrence of /
        int lastIndex = path.lastIndexOf(SLASH);
        //There are two kinds of paths expected. /account/1/roleName and /account/1
        if (secondIndex != lastIndex) {
            return path.substring(secondIndex + 1, lastIndex);
        } else {
            return path.substring(secondIndex + 1);
        }
    }

    /**
     * Validate roles present in the payload with the db data.
     *
     * @param roleNameSet       stores roleNames of request payload
     * @param rolesEntityList   contains RolesEntity data
     * @param errorPropertyList stores error Properties as a list
     */
    public static void validateRolesFromDb(Set<String> roleNameSet,
            List<RolesEntity> rolesEntityList, List<ErrorProperty> errorPropertyList,
            Map<String, BigInteger> roleNameAndIdMap) {
        LOGGER.debug("Validating Roles in the request payload");
        rolesEntityList.forEach(rolesEntity -> roleNameAndIdMap.put(rolesEntity.getName(), rolesEntity.getId()));
        if (!roleNameAndIdMap.keySet().containsAll(roleNameSet)) {
            errorPropertyList.add(new ErrorProperty("role_ids", new ArrayList<>(roleNameSet)));
        }
    }

    /**
     * validate account ids and names provided in the payload data with the db.
     *
     * @param accountIdSet        stores accountIds of request payload
     * @param accountEntityList   contains AccountEntity data
     * @param errorPropertyList   stores error property as a list
     */
    public static void validateAccountIdFromDb(Set<BigInteger> accountIdSet,
            List<AccountEntity> accountEntityList,
            List<ErrorProperty> errorPropertyList) {
        Set<BigInteger> accountIdsFromDbSet = new HashSet<>();
        LOGGER.debug("Validating AccountId and Names for the request payload");
        accountEntityList.forEach(accountEntity -> accountIdsFromDbSet.add(accountEntity.getId()));
        if (!accountIdsFromDbSet.containsAll(accountIdSet)) {
            errorPropertyList.add(new ErrorProperty("account_ids",
                    new ArrayList<>(convertBigIntegertoString(accountIdSet))));
        }
    }

    private static Set<String> convertBigIntegertoString(Set<BigInteger> set) {
        return set.stream().map(BigInteger::toString).collect(Collectors.toSet());
    }

    /**
     *  Maps UserAccountRoleMappingEntity with accountId.
     *
     * @param userRoleMappingEntityList contains Associated role mapping entities for the users
     * @param accountIdRoleIdMap maps accountId with roleIds
     */
    public static Map<String, UserAccountRoleMappingEntity> mapAccountToEntityForUser(
        List<UserAccountRoleMappingEntity> userRoleMappingEntityList,
            Map<BigInteger, List<BigInteger>> accountIdRoleIdMap) {
        Map<String, UserAccountRoleMappingEntity> accountIdRoleIdEntityMap = new HashMap<>();
        userRoleMappingEntityList.forEach(userRoleMappingEntity -> {
            accountIdRoleIdMap.computeIfAbsent(userRoleMappingEntity.getAccountId(),
                    val -> new ArrayList<>()).add(userRoleMappingEntity.getRoleId());
            accountIdRoleIdEntityMap.put(userRoleMappingEntity.getAccountId() + AMPERSAND
                    + userRoleMappingEntity.getRoleId(), userRoleMappingEntity);
        });
        return accountIdRoleIdEntityMap;
    }
}
