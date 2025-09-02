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

package org.eclipse.ecsp.uidam.accountmanagement.service.impl;

import org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.exception.AccountManagementException;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAccountRoleMappingRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_DOES_NOT_EXIST;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_DOES_NOT_EXIST_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_DELETE_ASSOCIATED_ACCOUNT_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_DELETE_DEFAULT_ACCOUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_DELETE_DEFAULT_ACCOUNT_MSG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UNKNOWN_DB_ERROR_MSG;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_SELF_MNG;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_DEFAULT_ACCOUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Account delete service.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { AccountServiceImpl.class })
@MockBean(JpaMetamodelMappingContext.class)
@TestPropertySource("classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountDeleteServiceTest {

    private static final String API_VERSION_1 = "v1";

    private AccountServiceImpl accountsService;

    @MockBean
    private UsersService userService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private TenantConfigurationService tenantConfigurationService;

    @MockBean
    private UserManagementTenantProperties tenantProperties;

    @MockBean
    private RolesService rolesService;

    @MockBean
    private UserAccountRoleMappingRepository userAccountRoleMappingRepository;

    private static final String USER_DEFAULT_ACCOUNTID = "112313385530649019824702444100150";

    @BeforeEach
    public void setupTenantConfiguration() {
        MockitoAnnotations.openMocks(this);
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(new BigInteger(USER_DEFAULT_ACCOUNTID));
        accountEntity.setAccountName(USER_DEFAULT_ACCOUNT);
        
        // Mock tenant configuration for lazy loading cache
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getUserDefaultAccountName()).thenReturn(USER_DEFAULT_ACCOUNT);
        when(accountRepository.findByAccountName(USER_DEFAULT_ACCOUNT)).thenReturn(Optional.of(accountEntity));
        
        accountsService = new AccountServiceImpl(accountRepository, rolesService, tenantConfigurationService,
                userAccountRoleMappingRepository);
        // Note: postConstruct() method was replaced with lazy loading cache
        // No need to call postConstruct() anymore as account ID is resolved on-demand
    }

    @Test
    void testUserAssociatedAccountForDelete() {
        AccountEntity accountEntity =  new AccountEntity();
        accountEntity.setId(ACCOUNT_ID_VALUE);
        when(userAccountRoleMappingRepository.existsByAccountId(ACCOUNT_ID_VALUE)).thenReturn(true);
        when(accountRepository.findByIdAndStatusNot(ACCOUNT_ID_VALUE, AccountStatus.DELETED))
                .thenReturn(Optional.of(accountEntity));
        AccountManagementException accountManagementException = assertThrows(AccountManagementException.class, () -> {
            accountsService.deleteAccount(BigInteger.ONE, ACCOUNT_ID_VALUE);
        });

        assertEquals(HttpStatus.BAD_REQUEST, accountManagementException.getHttpStatus());
        assertEquals(MessageFormat.format(CANNOT_DELETE_ASSOCIATED_ACCOUNT_MSG, ACCOUNT_ID_VALUE.toString()),
                accountManagementException.getMessage());
    }

    @Test
    void testDeleteAccountForException() {
        AccountEntity accountEntity =  new AccountEntity();
        accountEntity.setId(ACCOUNT_ID_VALUE);
        accountEntity.setAccountName("test_account");
        when(userAccountRoleMappingRepository.existsByAccountId(ACCOUNT_ID_VALUE)).thenReturn(false);
        when(accountRepository.findByIdAndStatusNot(ACCOUNT_ID_VALUE, AccountStatus.DELETED))
                .thenReturn(Optional.of(accountEntity));
        when(accountRepository.save(accountEntity)).thenThrow(new RuntimeException());
        AccountManagementException accountManagementException = assertThrows(AccountManagementException.class, () -> {
            accountsService.deleteAccount(BigInteger.ONE, ACCOUNT_ID_VALUE);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, accountManagementException.getHttpStatus());
        assertEquals(MessageFormat.format(UNKNOWN_DB_ERROR_MSG, ACCOUNT_ID_VALUE.toString()),
                accountManagementException.getMessage());
    }

    @Test
    void testDeleteDeletedAccount() throws ResourceNotFoundException,
            AccountManagementException {
        // Arrange

        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        Set<String> roles = new HashSet<>(Set.of(role1, role2));

        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(roles);

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");

        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);
        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.empty());
        // Act
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.deleteAccount(LOGGED_IN_USER_ID_VALUE, ACCOUNT_ID_VALUE);
        });
        assertEquals(ACCOUNT_DOES_NOT_EXIST, exception.getCode());
        assertEquals(MessageFormat.format(ACCOUNT_DOES_NOT_EXIST_MSG, ACCOUNT_ID_VALUE.toString()),
                exception.getMessage());
        assertEquals(AccountApiConstants.ACCOUNT_ID, exception.getProperty().get(0).getKey());
        assertEquals(ACCOUNT_ID_VALUE.toString(), exception.getProperty().get(0).getValues().get(0));
    }

    @Test
    void testDeleteNonExistentAccount() throws ResourceNotFoundException,
            AccountManagementException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        Set<String> roles = new HashSet<>(Set.of(role1, role2));

        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(roles);

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);
        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.empty());

        // Act
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.deleteAccount(LOGGED_IN_USER_ID_VALUE, ACCOUNT_ID_VALUE);
        });

        assertEquals(ACCOUNT_DOES_NOT_EXIST, exception.getCode());
        assertEquals(MessageFormat.format(ACCOUNT_DOES_NOT_EXIST_MSG, ACCOUNT_ID_VALUE.toString()),
                exception.getMessage());
        assertEquals(AccountApiConstants.ACCOUNT_ID, exception.getProperty().get(0).getKey());
        assertEquals(ACCOUNT_ID_VALUE.toString(), exception.getProperty().get(0).getValues().get(0));
    }

    @Test
    void testDeleteUserDefaultAccount() throws ResourceNotFoundException,
            AccountManagementException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        Set<String> roles = new HashSet<>(Set.of(role1, role2));
        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(roles);

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);
        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName("Test default");
        savedEntity.setId(new BigInteger(USER_DEFAULT_ACCOUNTID));
        savedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.of(savedEntity));

        // Act
        BigInteger accountId = new BigInteger(USER_DEFAULT_ACCOUNTID);
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.deleteAccount(LOGGED_IN_USER_ID_VALUE, accountId);
        });

        assertEquals(CANNOT_DELETE_DEFAULT_ACCOUNT, exception.getCode());
        assertEquals(MessageFormat.format(CANNOT_DELETE_DEFAULT_ACCOUNT_MSG, accountId.toString()),
                exception.getMessage());
        assertEquals(AccountApiConstants.ACCOUNT_ID, exception.getProperty().get(0).getKey());
        assertEquals(accountId.toString(), exception.getProperty().get(0).getValues().get(0));
    }

    @Test
    void testDeleteAccount() throws AccountManagementException, ResourceNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        Set<String> roles = new HashSet<>(Set.of(role1, role2));
        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(roles);

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);

        String accountName = "Test Account";
        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);
        savedEntity.setId(ACCOUNT_ID_VALUE);
        savedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.of(savedEntity));
        AccountEntity deletedEntity = new AccountEntity();
        deletedEntity.setAccountName(accountName);
        deletedEntity.setId(ACCOUNT_ID_VALUE);
        deletedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        deletedEntity.setStatus(AccountStatus.DELETED);
        when(accountRepository.save(savedEntity)).thenReturn(deletedEntity);

        // Map Roles role1,role2 to RoleListRepresentation
        roleListDto = createRoleListDtoRepresentation(role1, role2);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        accountsService.deleteAccount(LOGGED_IN_USER_ID_VALUE, ACCOUNT_ID_VALUE);
        verify(accountRepository, atMost(1)).save(savedEntity);
    }

    private static RoleListRepresentation createRoleListDtoRepresentation(String roleName1, String roleName2) {
        RoleCreateResponse vehicleOwnerRole = new RoleCreateResponse();
        RoleCreateResponse role2Dto = new RoleCreateResponse();
        Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        vehicleOwnerRole.setName(roleName1);
        role2Dto.setName(roleName2);
        role2Dto.setId(ROLE_ID_1);
        vehicleOwnerRole.setId(ROLE_ID_2);
        role2Dto.setScopes(Collections.singletonList(scopeDto));
        vehicleOwnerRole.setScopes(Collections.singletonList(scopeDto));

        Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        roleDtoList.add(role2Dto);
        roleDtoList.add(vehicleOwnerRole);

        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }
}
