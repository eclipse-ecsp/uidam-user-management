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

import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.CreateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.UpdateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccountMapperTest {
    private AccountMapper accountMapper = AccountMapper.ACCOUNT_MAPPER;

    @Test
    void testMapToAccount() {

        // Create a new AccountDto object
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setRoles(new HashSet<>(Arrays.asList("Role1", "Role2")));

        // RoleName id Mapping
        Map<String, BigInteger> roleNameIdMapping = new HashMap<>();
        roleNameIdMapping.put("Role1", ROLE_ID_1);
        roleNameIdMapping.put("Role2", ROLE_ID_2);

        // Map the AccountDto to AccountEntity
        AccountEntity accountEntity = accountMapper.mapToAccount(accountDto, roleNameIdMapping);

        // Verify the mapping
        assertNotNull(accountEntity);
        assertEquals(Set.of(ROLE_ID_1, ROLE_ID_2), accountEntity.getDefaultRoles());
    }

    @Test
    void testMapToAccountResponse() {
        // Create a new AccountEntity object
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(ACCOUNT_ID_VALUE);

        // Map the AccountEntity to AccountResponse
        CreateAccountResponse accountResponse = accountMapper.mapToCreateAccountResponse(accountEntity);

        // Verify the mapping
        assertNotNull(accountResponse);
        assertEquals(ACCOUNT_ID_VALUE.toString(), accountResponse.getId());
    }

    @Test
    void testMapAccountToDefaultRoles() {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));

        // RoleName id Mapping
        Map<String, BigInteger> roleNameIdMapping = new HashMap<>();
        roleNameIdMapping.put("Role1", ROLE_ID_1);
        roleNameIdMapping.put("Role2", ROLE_ID_2);

        // Call the mapAccountToDefaultRoles method
        Set<String> actualDeafultRoles = accountMapper.mapDefaultRolesToResponse(accountEntity, roleNameIdMapping);
        // Create a new AccountDto object
        Set<String> expected = new HashSet<String>(Arrays.asList("Role1", "Role2"));

        // Verify the result
        assertEquals(expected, actualDeafultRoles);
    }

    /**
     * In this test case we will validate if Roles, ParentID and Status was updated.
     * Rest of the fields should not be updated. Ensure Accout name and id will not
     * be updated along with other fields.
     */
    @Test
    void testUpdate() {
        UpdateAccountDto accountDto = new UpdateAccountDto();
        accountDto.setRoles(new HashSet<>(Arrays.asList("Role1", "Role3")));
        accountDto.setParentId(String.valueOf(PARENT_ID_VALUE));
        accountDto.setStatus(AccountStatus.BLOCKED);

        // RoleName id Mapping
        Map<String, BigInteger> roleNameIdMapping = new HashMap<>();
        roleNameIdMapping.put("Role1", ROLE_ID_1);
        roleNameIdMapping.put("Role2", ROLE_ID_2);
        roleNameIdMapping.put("Role3", ROLE_ID_3);

        AccountEntity entityToBeUpdated = new AccountEntity();
        entityToBeUpdated.setId(ACCOUNT_ID_VALUE);
        entityToBeUpdated.setAccountName("Accname1");
        entityToBeUpdated.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        entityToBeUpdated.setParentId(PARENT_ID_VALUE);
        entityToBeUpdated.setStatus(AccountStatus.ACTIVE);
        entityToBeUpdated.setCreatedBy("User1");
        entityToBeUpdated.setUpdatedBy("User2");
        final long dateCreateTs = 1709301796L;
        final long dateUpdateTs = 1709560996L;
        Timestamp dateCreate = new Timestamp(dateCreateTs);
        Timestamp dateUpdate = new Timestamp(dateUpdateTs);
        entityToBeUpdated.setCreateDate(dateCreate);
        entityToBeUpdated.setUpdateDate(dateUpdate);
        accountMapper.updateAccountEntity(accountDto, entityToBeUpdated, roleNameIdMapping);

        // Verify if only roles, parentID and status was updated rest SHOULD NOT BE
        // UPDATED
        assertEquals(ACCOUNT_ID_VALUE, entityToBeUpdated.getId());
        assertEquals("Accname1", entityToBeUpdated.getAccountName());
        assertEquals(Set.of(ROLE_ID_1, ROLE_ID_3), entityToBeUpdated.getDefaultRoles());
        assertEquals(PARENT_ID_VALUE, entityToBeUpdated.getParentId());
        assertEquals(AccountStatus.BLOCKED, entityToBeUpdated.getStatus());
        assertEquals("User1", entityToBeUpdated.getCreatedBy());
        assertEquals("User2", entityToBeUpdated.getUpdatedBy());
        assertEquals(dateCreate, entityToBeUpdated.getCreateDate());
        assertEquals(dateUpdate, entityToBeUpdated.getUpdateDate());

    }

    /**
     * In this test case we will validate if ParentID and Status has been updated.
     * As roles is null it should not be updated
     */
    @Test
    void testUpdateWithRolesNull() {

        UpdateAccountDto accountDto = new UpdateAccountDto();
        accountDto.setParentId(String.valueOf(PARENT_ID_VALUE));
        accountDto.setStatus(AccountStatus.BLOCKED);

        // RoleName id Mapping
        Map<String, BigInteger> roleNameIdMapping = new HashMap<>();
        roleNameIdMapping.put("Role1", ROLE_ID_1);
        roleNameIdMapping.put("Role2", ROLE_ID_2);

        AccountEntity entityToBeUpdated = new AccountEntity();
        entityToBeUpdated.setId(ACCOUNT_ID_VALUE);
        entityToBeUpdated.setAccountName("Accname1");
        entityToBeUpdated.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        entityToBeUpdated.setParentId(PARENT_ID_VALUE);
        entityToBeUpdated.setStatus(AccountStatus.ACTIVE);
        entityToBeUpdated.setCreatedBy("User1");
        entityToBeUpdated.setUpdatedBy("User2");
        final long dateCreateTs = 1709301796L;
        final long dateUpdateTs = 1709560996L;
        Timestamp dateCreate = new Timestamp(dateCreateTs);
        Timestamp dateUpdate = new Timestamp(dateUpdateTs);
        entityToBeUpdated.setCreateDate(dateCreate);
        entityToBeUpdated.setUpdateDate(dateUpdate);
        accountMapper.updateAccountEntity(accountDto, entityToBeUpdated, roleNameIdMapping);

        // Verify if only roles, parentID and status was updated rest SHOULD NOT BE
        // UPDATED
        assertEquals(ACCOUNT_ID_VALUE, entityToBeUpdated.getId());
        assertEquals("Accname1", entityToBeUpdated.getAccountName());
        assertEquals(Set.of(ROLE_ID_1, ROLE_ID_2), entityToBeUpdated.getDefaultRoles());
        assertEquals(PARENT_ID_VALUE, entityToBeUpdated.getParentId());
        assertEquals(AccountStatus.BLOCKED, entityToBeUpdated.getStatus());
        assertEquals("User1", entityToBeUpdated.getCreatedBy());
        assertEquals("User2", entityToBeUpdated.getUpdatedBy());
        assertEquals(dateCreate, entityToBeUpdated.getCreateDate());
        assertEquals(dateUpdate, entityToBeUpdated.getUpdateDate());

    }

    /**
     * In this test case we will validate if Roles and Status has been updated. As
     * parent is null it should not be updated
     */
    @Test
    void testUpdateWithParentIdNull() {

        UpdateAccountDto accountDto = new UpdateAccountDto();
        accountDto.setRoles(new HashSet<>(Arrays.asList("Role1", "Role3")));
        accountDto.setStatus(AccountStatus.BLOCKED);

        // RoleName id Mapping
        Map<String, BigInteger> roleNameIdMapping = new HashMap<>();
        roleNameIdMapping.put("Role1", ROLE_ID_1);
        roleNameIdMapping.put("Role2", ROLE_ID_2);
        roleNameIdMapping.put("Role3", ROLE_ID_3);

        AccountEntity entityToBeUpdated = new AccountEntity();
        entityToBeUpdated.setId(ACCOUNT_ID_VALUE);
        entityToBeUpdated.setAccountName("Accname1");
        entityToBeUpdated.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        entityToBeUpdated.setParentId(PARENT_ID_VALUE);
        entityToBeUpdated.setStatus(AccountStatus.ACTIVE);
        entityToBeUpdated.setCreatedBy("User1");
        entityToBeUpdated.setUpdatedBy("User2");
        final long dateCreateTs = 1709301796L;
        final long dateUpdateTs = 1709560996L;
        Timestamp dateCreate = new Timestamp(dateCreateTs);
        Timestamp dateUpdate = new Timestamp(dateUpdateTs);
        entityToBeUpdated.setCreateDate(dateCreate);
        entityToBeUpdated.setUpdateDate(dateUpdate);
        accountMapper.updateAccountEntity(accountDto, entityToBeUpdated, roleNameIdMapping);

        // Verify if only roles, parentID and status was updated rest SHOULD NOT BE
        // UPDATED
        assertEquals(ACCOUNT_ID_VALUE, entityToBeUpdated.getId());
        assertEquals("Accname1", entityToBeUpdated.getAccountName());
        assertEquals(Set.of(ROLE_ID_1, ROLE_ID_3), entityToBeUpdated.getDefaultRoles());
        assertEquals(PARENT_ID_VALUE, entityToBeUpdated.getParentId());
        assertEquals(AccountStatus.BLOCKED, entityToBeUpdated.getStatus());
        assertEquals("User1", entityToBeUpdated.getCreatedBy());
        assertEquals("User2", entityToBeUpdated.getUpdatedBy());
        assertEquals(dateCreate, entityToBeUpdated.getCreateDate());
        assertEquals(dateUpdate, entityToBeUpdated.getUpdateDate());

    }

    /**
     * In this test case we will validate if Roles and ParentId has been updated. As
     * Status is null it should not be updated
     */
    @Test
    void testUpdateWithStatusNull() {
        UpdateAccountDto accountDto = new UpdateAccountDto();
        accountDto.setRoles(new HashSet<>(Arrays.asList("Role1", "Role3")));
        accountDto.setParentId(String.valueOf(PARENT_ID_VALUE_1));

        // RoleName id Mapping
        Map<String, BigInteger> roleNameIdMapping = new HashMap<>();
        roleNameIdMapping.put("Role1", ROLE_ID_1);
        roleNameIdMapping.put("Role2", ROLE_ID_2);
        roleNameIdMapping.put("Role3", ROLE_ID_3);

        AccountEntity entityToBeUpdated = new AccountEntity();
        entityToBeUpdated.setId(ACCOUNT_ID_VALUE);
        entityToBeUpdated.setAccountName("Accname1");
        entityToBeUpdated.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        entityToBeUpdated.setParentId(PARENT_ID_VALUE);
        entityToBeUpdated.setStatus(AccountStatus.ACTIVE);
        entityToBeUpdated.setCreatedBy("User1");
        entityToBeUpdated.setUpdatedBy("User2");
        final long dateCreateTs = 1709301796L;
        final long dateUpdateTs = 1709560996L;
        Timestamp dateCreate = new Timestamp(dateCreateTs);
        Timestamp dateUpdate = new Timestamp(dateUpdateTs);
        entityToBeUpdated.setCreateDate(dateCreate);
        entityToBeUpdated.setUpdateDate(dateUpdate);
        accountMapper.updateAccountEntity(accountDto, entityToBeUpdated, roleNameIdMapping);

        // Verify if only roles, parentID and status was updated rest SHOULD NOT BE
        // UPDATED
        assertEquals(ACCOUNT_ID_VALUE, entityToBeUpdated.getId());
        assertEquals("Accname1", entityToBeUpdated.getAccountName());
        assertEquals(Set.of(ROLE_ID_1, ROLE_ID_3), entityToBeUpdated.getDefaultRoles());
        assertEquals(PARENT_ID_VALUE_1, entityToBeUpdated.getParentId());
        assertEquals(AccountStatus.ACTIVE, entityToBeUpdated.getStatus());
        assertEquals("User1", entityToBeUpdated.getCreatedBy());
        assertEquals("User2", entityToBeUpdated.getUpdatedBy());
        assertEquals(dateCreate, entityToBeUpdated.getCreateDate());
        assertEquals(dateUpdate, entityToBeUpdated.getUpdateDate());

    }

    /**
     * In this test case we will validate the conversion of id to UUID.
     */
    @Test
    void convertStringToUuidTest() {
        assertNull(AccountMapper.convertStringToId(""));
        assertEquals(ACCOUNT_ID_VALUE, AccountMapper.convertStringToId(String.valueOf(ACCOUNT_ID_VALUE)));

    }
}
