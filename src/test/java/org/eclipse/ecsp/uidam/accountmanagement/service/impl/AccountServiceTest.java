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

import jakarta.persistence.EntityNotFoundException;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.CreateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.UpdateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.GetAccountApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.exception.AccountManagementException;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAccountRoleMappingRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ALREADY_EXISTS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ALREADY_EXISTS_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_DOES_NOT_EXIST;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_DOES_NOT_EXIST_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ROLE_VALIDATION_FAILURE;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ROLE_VALIDATION_FAILURE_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_UPDATE_DEFAULT_ACCOUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_UPDATE_DEFAULT_ACCOUNT_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_ACCOUNT_STATUS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_ACCOUNT_STATUS_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ACCOUNT_NAME_PATTERN;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ACCOUNT_NAME_PATTERN_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ROLE;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ROLE_MSG;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.CREATED_BY_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_3;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_SELF_MNG;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_DEFAULT_ACCOUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { AccountServiceImpl.class })
@MockBean(JpaMetamodelMappingContext.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountServiceTest {
    private static final String ACCOUNT_NAME = "Test Account";
    private static final String ROLE_NAME_3 = "ROLE_3";
    private static final String API_VERSION_1 = "v1";
    @Autowired
    private AccountServiceImpl accountsService;

    @MockBean
    private UsersService userService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private RolesService rolesService;
    
    @MockBean
    private TenantConfigurationService tenantConfigurationService;

    @MockBean
    private UserManagementTenantProperties tenantProperties;

    @MockBean
    private UserAccountRoleMappingRepository userAccountRoleMappingRepository;
    
    @MockBean
    private org.eclipse.ecsp.uidam.accountmanagement.utilities.AccountAuditHelper accountAuditHelper;

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
                userAccountRoleMappingRepository, accountAuditHelper);
        // Note: postConstruct() method was replaced with lazy loading cache
        // No need to call postConstruct() anymore as account ID is resolved on-demand
    }
    
    @Test
    @DisplayName("Exception - invalid.input.accountname.pattern during account creation")
    void testCreateAccountWithInvalidAccountPattern() throws NoSuchAlgorithmException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        CreateAccountDto accountDto = new CreateAccountDto();
        String accountName = "!!!Test Account!!!";
        // Invalid account
        accountDto.setAccountName(accountName);
        accountDto.setRoles(new HashSet<>(Set.of(role1, role2)));
        

        // Act
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
        });

        assertEquals(INVALID_INPUT_ACCOUNT_NAME_PATTERN, exception.getCode());
        assertEquals(MessageFormat.format(INVALID_INPUT_ACCOUNT_NAME_PATTERN_MSG, accountName), exception.getMessage());
    }

    @Test
    @DisplayName("Exception - invalid.input.role during account creation")
    void testCreateAccountWithFewRoleExist() throws NoSuchAlgorithmException, ResourceNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        // Role Not exist
        String role2 = ROLE_2;
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName("Test Account");
        // Create AccountDto which not match the roleListDto
        Set<String> roles = new HashSet<>(Set.of(role1, role2 + "-Dummy"));
        accountDto.setRoles(roles);

        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));

        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Mock Account creation roles from auth service.
        
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);

        // Act
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
        });

        assertEquals(INVALID_INPUT_ROLE, exception.getCode());
        assertEquals(MessageFormat.format(INVALID_INPUT_ROLE_MSG, roles), exception.getMessage());
    }
    
    @Test
    @DisplayName("Exception - invalid.input.role during account creation")
    void testCreateAccountWithNoRoleExist() throws NoSuchAlgorithmException, ResourceNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        // Role Not exist
        String role2 = ROLE_2;
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName("Test Account");
        // Create AccountDto which not match the roleListDto
        Set<String> roles = new HashSet<>(Set.of(role1, role2 + "-Dummy"));
        accountDto.setRoles(roles);

        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));

        // Mock Account creation roles from auth service.
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean()))
                .thenThrow(new EntityNotFoundException());
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);

        // Act
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
        });

        assertEquals(INVALID_INPUT_ROLE, exception.getCode());
        assertEquals(MessageFormat.format(INVALID_INPUT_ROLE_MSG, roles), exception.getMessage());
    }

    @Test
    void testCreateAccount() throws NoSuchAlgorithmException, AccountManagementException, ResourceNotFoundException,
            UserNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        String accountName = "Test Account";

        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName(accountName);
        Set<String> expectedRoles = new HashSet<>(Set.of(role1, role2));
        accountDto.setRoles(expectedRoles);

        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);
        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);
        savedEntity.setId(ACCOUNT_ID_VALUE);
        savedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);
        // Act
        CreateAccountResponse response = accountsService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
        // Assert
        assertNotNull(response);
        assertNotNull(response.getId());
    }

    @Test
    void testCreateAccountWithNoRoles() throws NoSuchAlgorithmException, AccountManagementException,
            ResourceNotFoundException, UserNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        String accountName = "Test Account";

        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName(accountName);
        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);
        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);
        savedEntity.setId(ACCOUNT_ID_VALUE);
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);
        // Act
        CreateAccountResponse response = accountsService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
        // Assert
        assertNotNull(response);
        assertNotNull(response.getId());
    }

    @Test
    void testCreateAccountWithDuplicateAccount() throws NoSuchAlgorithmException, ResourceNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        String accountName = "Test Account";
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName(accountName);
        accountDto.setRoles(new HashSet<>(Set.of(role1, role2)));

        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);
        AccountEntity savedEntity1 = new AccountEntity();
        savedEntity1.setAccountName(accountName);
        savedEntity1.setId(ACCOUNT_ID_VALUE);
        savedEntity1.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity1.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findByAccountName(any())).thenReturn(Optional.of(savedEntity1));
        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);
        savedEntity.setId(ACCOUNT_ID_VALUE);
        savedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);
        // Act
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
        });
        // Assert
        assertEquals(AccountApiConstants.ACCOUNT_NAME, exception.getProperty().get(0).getKey());
        assertEquals(accountName, exception.getProperty().get(0).getValues().get(0));
        assertEquals(ACCOUNT_ALREADY_EXISTS, exception.getCode());
        assertEquals(MessageFormat.format(ACCOUNT_ALREADY_EXISTS_MSG, accountName), exception.getMessage());
    }

    @Test
    void testCreateAccountWithDeletedOldAccount() throws NoSuchAlgorithmException, ResourceNotFoundException,
            AccountManagementException, UserNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        String accountName = "Test Account";
        Set<String> expectedRoles = new HashSet<>(Set.of(role1, role2));
        
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName(accountName);
        accountDto.setRoles(expectedRoles);

        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(expectedRoles);

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);
        AccountEntity savedEntity1 = new AccountEntity();
        savedEntity1.setAccountName(accountName);
        savedEntity1.setId(ACCOUNT_ID_VALUE);
        savedEntity1.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        // Deleted account in db
        savedEntity1.setStatus(AccountStatus.DELETED);
        List<AccountEntity> dbEntites = new ArrayList<>();
        dbEntites.add(savedEntity1);
        when(accountRepository.findAll()).thenReturn(dbEntites);
        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);
        savedEntity.setId(ACCOUNT_ID_VALUE);
        savedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);
        // Act
        CreateAccountResponse response = accountsService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
        // Assert
        assertNotNull(response);
        assertNotNull(response.getId());
    }

    @Test
    void testCreateAccountWithAccountsDb() throws NoSuchAlgorithmException, ResourceNotFoundException,
            AccountManagementException, UserNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        String accountName = "Test Account";
        Set<String> expectedRoles = new HashSet<>(Set.of(role1, role2));
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName(accountName);
        accountDto.setRoles(expectedRoles);
        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(expectedRoles);

        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // Mock getRoleListByName from authManagementService
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);
        AccountEntity savedEntity1 = new AccountEntity();
        savedEntity1.setAccountName(accountName + "2");

        savedEntity1.setId(ACCOUNT_ID_VALUE);

        savedEntity1.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        // Deleted account in db
        savedEntity1.setStatus(AccountStatus.ACTIVE);
        List<AccountEntity> dbEntites = new ArrayList<>();
        dbEntites.add(savedEntity1);
        when(accountRepository.findAll()).thenReturn(dbEntites);
        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);
        savedEntity.setId(ACCOUNT_ID_VALUE);
        savedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);
        // Act
        CreateAccountResponse response = accountsService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
        // Assert
        assertNotNull(response);
        assertNotNull(response.getId());
    }

    private static RoleListRepresentation createRoleListDtoRepresentation(String roleName1, String roleName2) {
        RoleCreateResponse vehicleOwnerRole = new RoleCreateResponse();
        Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        vehicleOwnerRole.setName(roleName1);
        vehicleOwnerRole.setId(ROLE_ID_1);
        vehicleOwnerRole.setScopes(Collections.singletonList(scopeDto));
        RoleCreateResponse role2Dto = null;
        if (roleName2 != null) {
            role2Dto = new RoleCreateResponse();
            role2Dto.setName(roleName2);
            role2Dto.setId(ROLE_ID_2);
            role2Dto.setScopes(Collections.singletonList(scopeDto));
        }

        Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        roleDtoList.add(vehicleOwnerRole);
        if (role2Dto != null) {
            roleDtoList.add(role2Dto);
        }
        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }
    
    private static RoleListRepresentation createRoleListDtoRepresentation(Map<String, BigInteger> roleNameIdMap) {
        final Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        final Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        roleNameIdMap.entrySet().forEach(entry -> {
            RoleCreateResponse role = new RoleCreateResponse();
            role.setName(entry.getKey());
            role.setId(entry.getValue());
            role.setScopes(Collections.singletonList(scopeDto));
            roleDtoList.add(role);
        });
        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }

    @Test
    void testGetAccount() throws ResourceNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        String accountName = "Test Account";
        
        // Map Roles role1,role2 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, role2);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("ViewAccounts");
        // Mock getRoleListByName from authManagementService
        Set<BigInteger> expectedRoleId = new HashSet<>(Set.of(ROLE_ID_1, ROLE_ID_2));
        when(rolesService.getRoleById(expectedRoleId)).thenReturn(roleListDto);

        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);

        savedEntity.setId(ACCOUNT_ID_VALUE);

        // Set role id in the entity.
        savedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity.setStatus(AccountStatus.ACTIVE);

        String createdBy = "user1";
        String updatedBy = "user2";
        savedEntity.setCreatedBy(createdBy);
        savedEntity.setUpdatedBy(updatedBy);

        Timestamp d1 = Timestamp.valueOf("2000-11-21 00:00:00.0");
        Timestamp d2 = Timestamp.valueOf("2010-01-03 00:00:00.0");
        savedEntity.setCreateDate(d1);
        savedEntity.setUpdateDate(d2);

        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.of(savedEntity));
        // Act
        GetAccountApiResponse response = accountsService.getAccount(ACCOUNT_ID_VALUE);

        // Assert
        ArgumentCaptor<Set<BigInteger>> accountArgument = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(rolesService, times(1)).getRoleById(accountArgument.capture());
        ArgumentCaptor<BigInteger> bigIntegerArgument = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<AccountStatus> statusArgument = ArgumentCaptor.forClass(AccountStatus.class);
        Mockito.verify(accountRepository, times(1)).findByIdAndStatusNot(bigIntegerArgument.capture(),
                statusArgument.capture());
        assertNotNull(response);
        assertEquals(accountName, response.getAccountName());
        Set<String> expectedRoles = new HashSet<>(Set.of(role1, role2));
        assertEquals(expectedRoles, response.getRoles());
        assertEquals(createdBy, response.getCreatedBy());
        assertEquals(updatedBy, response.getUpdatedBy());
        assertEquals(d2, response.getUpdateDate());
        assertEquals(d1, response.getCreateDate());
    }

    @Test
    void getAccountFailure() {
        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.empty());

        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.getAccount(ACCOUNT_ID_VALUE);
        });

        ArgumentCaptor<BigInteger> bigIntegerArgument = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<AccountStatus> statusArgument = ArgumentCaptor.forClass(AccountStatus.class);
        Mockito.verify(accountRepository, times(1)).findByIdAndStatusNot(bigIntegerArgument.capture(),
                statusArgument.capture());
        assertEquals(ACCOUNT_DOES_NOT_EXIST, exception.getCode());
        assertEquals(MessageFormat.format(ACCOUNT_DOES_NOT_EXIST_MSG, ACCOUNT_ID_VALUE.toString()),
                exception.getMessage());
    }

    @Test
    void testUpdateAccount() throws ResourceNotFoundException, UserNotFoundException {

        AccountEntity dbEntity = new AccountEntity();
        dbEntity.setAccountName(ACCOUNT_NAME);
        dbEntity.setId(ACCOUNT_ID_VALUE);
        dbEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        dbEntity.setStatus(AccountStatus.ACTIVE);
        dbEntity.setParentId(PARENT_ID_VALUE);
        dbEntity.setCreatedBy(String.valueOf(CREATED_BY_ID_VALUE));
        // 01/03/2024
        final long createDateTs = 1709271413L;
        Timestamp createDate = new Timestamp(createDateTs);
        dbEntity.setCreateDate(createDate);
        when(accountRepository.findByIdAndStatusNot(ACCOUNT_ID_VALUE, AccountStatus.DELETED))
                .thenReturn(Optional.ofNullable(dbEntity));

        UpdateAccountDto updateAccountDto = new UpdateAccountDto();
        
        String parentIdAsString = String.valueOf(PARENT_ID_VALUE);
        String role1 = ROLE_VALUE;
        Set<String> expectedRoles = new HashSet<>(Set.of(role1, ROLE_NAME_3));
        updateAccountDto.setRoles(expectedRoles);
        updateAccountDto.setStatus(AccountStatus.SUSPENDED);
        updateAccountDto.setParentId(parentIdAsString);

        UserResponseV1 userResponse = new UserResponseV1();
        String role2 = ROLE_2;
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));
        Map<String, BigInteger> entityRoleNameId = new HashMap<>();
        entityRoleNameId.put(role1, ROLE_ID_1);
        entityRoleNameId.put(role2, ROLE_ID_2);
        RoleListRepresentation roleListDtoUserRoles = createRoleListDtoRepresentation(entityRoleNameId);
        roleListDtoUserRoles.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        Map<String, BigInteger> dtoRoleNameId = new HashMap<>();
        dtoRoleNameId.put(role1, ROLE_ID_1);
        dtoRoleNameId.put(ROLE_NAME_3, ROLE_ID_3);
        RoleListRepresentation roleListDtoAccountRoles = createRoleListDtoRepresentation(dtoRoleNameId);
        roleListDtoAccountRoles.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDtoUserRoles);
        when(rolesService.getRoleById(any())).thenReturn(roleListDtoUserRoles);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDtoAccountRoles);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);

        verify(accountRepository, atMost(1)).save(dbEntity);
        when(accountRepository.save(dbEntity)).thenReturn(dbEntity);
        accountsService.updateAccount(ACCOUNT_ID_VALUE, updateAccountDto, LOGGED_IN_USER_ID_VALUE);

        // Validate the fields roles, parentId, status, updated by and updated date is
        // only updated.
        // Rest should not be updated.
        assertEquals(ACCOUNT_NAME, dbEntity.getAccountName());
        assertEquals(ACCOUNT_ID_VALUE, dbEntity.getId());
        assertEquals(PARENT_ID_VALUE, dbEntity.getParentId());
        assertEquals(AccountStatus.SUSPENDED, dbEntity.getStatus());
        assertEquals(createDate, dbEntity.getCreateDate());
        assertEquals(String.valueOf(CREATED_BY_ID_VALUE), dbEntity.getCreatedBy());
        assertTrue(dbEntity.getUpdateDate().after(createDate));
        assertEquals(String.valueOf(LOGGED_IN_USER_ID_VALUE), dbEntity.getUpdatedBy());

    }
    
    @Test
    void testUpdateAccountNoRolesExist() throws ResourceNotFoundException, UserNotFoundException {
        AccountEntity dbEntity = new AccountEntity();
        dbEntity.setAccountName(ACCOUNT_NAME);
        dbEntity.setId(ACCOUNT_ID_VALUE);
        dbEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        dbEntity.setStatus(AccountStatus.ACTIVE);
        dbEntity.setParentId(PARENT_ID_VALUE);
        dbEntity.setCreatedBy(String.valueOf(CREATED_BY_ID_VALUE));
        // 01/03/2024
        final long createDateTs = 1709271413L;
        Timestamp createDate = new Timestamp(createDateTs);
        dbEntity.setCreateDate(createDate);
        when(accountRepository.findByIdAndStatusNot(ACCOUNT_ID_VALUE, AccountStatus.DELETED))
                .thenReturn(Optional.ofNullable(dbEntity));

        UpdateAccountDto updateAccountDto = new UpdateAccountDto();

        String parentIdAsString = String.valueOf(PARENT_ID_VALUE);
        String role1 = ROLE_VALUE;
        Set<String> expectedRoles = new HashSet<>(Set.of(role1 + "-dummy", ROLE_NAME_3 + "-dummy"));
        updateAccountDto.setRoles(expectedRoles);
        updateAccountDto.setStatus(AccountStatus.SUSPENDED);
        updateAccountDto.setParentId(parentIdAsString);

        UserResponseV1 userResponse = new UserResponseV1();
        String role2 = ROLE_2;
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));
        Map<String, BigInteger> entityRoleNameId = new HashMap<>();
        entityRoleNameId.put(role1, ROLE_ID_1);
        entityRoleNameId.put(role2, ROLE_ID_2);
        RoleListRepresentation roleListDtoUserRoles = createRoleListDtoRepresentation(entityRoleNameId);
        roleListDtoUserRoles.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        Map<String, BigInteger> dtoRoleNameId = new HashMap<>();
        dtoRoleNameId.put(role1, ROLE_ID_1);
        dtoRoleNameId.put(ROLE_NAME_3, ROLE_ID_3);
        RoleListRepresentation roleListDtoAccountRoles = createRoleListDtoRepresentation(dtoRoleNameId);
        roleListDtoAccountRoles.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        // For Entity role id to role Name mapping
        when(rolesService.getRoleById(any())).thenReturn(roleListDtoUserRoles);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean()))
                .thenThrow(new EntityNotFoundException());
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);

        verify(accountRepository, atMost(1)).save(dbEntity);
        when(accountRepository.save(dbEntity)).thenReturn(dbEntity);
        // Act
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.updateAccount(ACCOUNT_ID_VALUE, updateAccountDto, LOGGED_IN_USER_ID_VALUE);
        });

        assertEquals(ACCOUNT_ROLE_VALIDATION_FAILURE, exception.getCode());
        assertEquals(MessageFormat.format(ACCOUNT_ROLE_VALIDATION_FAILURE_MSG, ACCOUNT_ID_VALUE.toString()),
                exception.getMessage());

    }
    
    @Test
    void testUpdateAccountWithEmptyRoles() throws ResourceNotFoundException, UserNotFoundException {
        AccountEntity dbEntity = new AccountEntity();
        dbEntity.setAccountName(ACCOUNT_NAME);
        dbEntity.setId(ACCOUNT_ID_VALUE);
        dbEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        dbEntity.setStatus(AccountStatus.ACTIVE);
        dbEntity.setParentId(PARENT_ID_VALUE);
        dbEntity.setCreatedBy(String.valueOf(CREATED_BY_ID_VALUE));
        // 01/03/2024
        final long createDateTs = 1709271413L;
        Timestamp createDate = new Timestamp(createDateTs);
        dbEntity.setCreateDate(createDate);
        when(accountRepository.findByIdAndStatusNot(ACCOUNT_ID_VALUE, AccountStatus.DELETED))
                .thenReturn(Optional.ofNullable(dbEntity));
        UpdateAccountDto updateAccountDto = new UpdateAccountDto();
        String parentIdAsString = String.valueOf(PARENT_ID_VALUE);
        updateAccountDto.setStatus(AccountStatus.SUSPENDED);
        updateAccountDto.setParentId(parentIdAsString);
        updateAccountDto.setRoles(new HashSet<>());
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));
        Map<String, BigInteger> entityRoleNameId = new HashMap<>();
        entityRoleNameId.put(role1, ROLE_ID_1);
        entityRoleNameId.put(role2, ROLE_ID_2);
        RoleListRepresentation roleListDtoUserRoles = createRoleListDtoRepresentation(entityRoleNameId);
        roleListDtoUserRoles.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        Map<String, BigInteger> dtoRoleNameId = new HashMap<>();
        String role3 = "ROLE_3";
        dtoRoleNameId.put(role1, ROLE_ID_1);
        dtoRoleNameId.put(role3, ROLE_ID_3);
        RoleListRepresentation roleListDtoAccountRoles = createRoleListDtoRepresentation(dtoRoleNameId);
        roleListDtoAccountRoles.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        when(rolesService.getRoleById(any())).thenReturn(roleListDtoUserRoles);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDtoUserRoles);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDtoAccountRoles);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);

        verify(accountRepository, atMost(1)).save(dbEntity);
        when(accountRepository.save(dbEntity)).thenReturn(dbEntity);
        accountsService.updateAccount(ACCOUNT_ID_VALUE, updateAccountDto, LOGGED_IN_USER_ID_VALUE);
        // Validate the fields roles, parentId, status, updated by and updated date is
        // only updated.
        // Rest should not be updated.
        assertEquals(ACCOUNT_NAME, dbEntity.getAccountName());
        assertEquals(ACCOUNT_ID_VALUE, dbEntity.getId());
        assertEquals(PARENT_ID_VALUE, dbEntity.getParentId());
        assertEquals(AccountStatus.SUSPENDED, dbEntity.getStatus());
        assertEquals(createDate, dbEntity.getCreateDate());
        assertEquals(String.valueOf(CREATED_BY_ID_VALUE), dbEntity.getCreatedBy());
        assertTrue(dbEntity.getUpdateDate().after(createDate));
        assertEquals(String.valueOf(LOGGED_IN_USER_ID_VALUE), dbEntity.getUpdatedBy());

    }
    
    @Test
    void testUpdateAccountWithoutRoles() throws ResourceNotFoundException, UserNotFoundException {
        AccountEntity dbEntity = new AccountEntity();
        dbEntity.setAccountName(ACCOUNT_NAME);
        dbEntity.setId(ACCOUNT_ID_VALUE);
        dbEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        dbEntity.setStatus(AccountStatus.ACTIVE);
        dbEntity.setParentId(PARENT_ID_VALUE);
        dbEntity.setCreatedBy(String.valueOf(CREATED_BY_ID_VALUE));
        // 01/03/2024
        final long createDateTs = 1709271413L;
        Timestamp createDate = new Timestamp(createDateTs);
        dbEntity.setCreateDate(createDate);
        when(accountRepository.findByIdAndStatusNot(ACCOUNT_ID_VALUE, AccountStatus.DELETED))
                .thenReturn(Optional.ofNullable(dbEntity));
        UpdateAccountDto updateAccountDto = new UpdateAccountDto();
        String parentIdAsString = String.valueOf(PARENT_ID_VALUE);
        updateAccountDto.setStatus(AccountStatus.SUSPENDED);
        updateAccountDto.setParentId(parentIdAsString);
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setFirstName(role2);
        userResponse.setRoles(new HashSet<String>(Set.of(role1, role2)));
        Map<String, BigInteger> entityRoleNameId = new HashMap<>();
        entityRoleNameId.put(role1, ROLE_ID_1);
        entityRoleNameId.put(role2, ROLE_ID_2);
        RoleListRepresentation roleListDtoUserRoles = createRoleListDtoRepresentation(entityRoleNameId);
        roleListDtoUserRoles.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        Map<String, BigInteger> dtoRoleNameId = new HashMap<>();
        String role3 = "ROLE_3";
        dtoRoleNameId.put(role1, ROLE_ID_1);
        dtoRoleNameId.put(role3, ROLE_ID_3);
        RoleListRepresentation roleListDtoAccountRoles = createRoleListDtoRepresentation(dtoRoleNameId);
        roleListDtoAccountRoles.getRoles().stream().findFirst().get().getScopes().get(0).setName("TenantAdmin");
        when(rolesService.getRoleById(any())).thenReturn(roleListDtoUserRoles);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDtoUserRoles);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDtoAccountRoles);
        
        when(userService.getUser(LOGGED_IN_USER_ID_VALUE, API_VERSION_1)).thenReturn(userResponse);

        verify(accountRepository, atMost(1)).save(dbEntity);
        when(accountRepository.save(dbEntity)).thenReturn(dbEntity);
        accountsService.updateAccount(ACCOUNT_ID_VALUE, updateAccountDto, LOGGED_IN_USER_ID_VALUE);
        // Validate the fields roles, parentId, status, updated by and updated date is
        // only updated.
        // Rest should not be updated.
        assertEquals(ACCOUNT_NAME, dbEntity.getAccountName());
        assertEquals(ACCOUNT_ID_VALUE, dbEntity.getId());
        assertEquals(PARENT_ID_VALUE, dbEntity.getParentId());
        assertEquals(AccountStatus.SUSPENDED, dbEntity.getStatus());
        assertEquals(createDate, dbEntity.getCreateDate());
        assertEquals(String.valueOf(CREATED_BY_ID_VALUE), dbEntity.getCreatedBy());
        assertTrue(dbEntity.getUpdateDate().after(createDate));
        assertEquals(String.valueOf(LOGGED_IN_USER_ID_VALUE), dbEntity.getUpdatedBy());

    }

    @Test
    void testUpdateAccountWithAccountStatusDeleted() {
        UpdateAccountDto updateAccount = new UpdateAccountDto();
        updateAccount.setStatus(AccountStatus.DELETED);
        
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.updateAccount(ACCOUNT_ID_VALUE, updateAccount, LOGGED_IN_USER_ID_VALUE);
        });
        assertEquals(INVALID_ACCOUNT_STATUS, exception.getCode());
        assertEquals(MessageFormat.format(INVALID_ACCOUNT_STATUS_MSG, ACCOUNT_ID_VALUE.toString()),
                exception.getMessage());
        assertEquals(AccountApiConstants.ACCOUNT_ID, exception.getProperty().get(0).getKey());
        assertEquals(ACCOUNT_ID_VALUE.toString(), exception.getProperty().get(0).getValues().get(0));
    }

    @Test
    void testUpdateAccountWithNoRecordInDb() {
        UpdateAccountDto updateAccount = new UpdateAccountDto();
        updateAccount.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findByIdAndStatusNot(ACCOUNT_ID_VALUE, AccountStatus.DELETED))
                .thenReturn(Optional.empty());
        
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.updateAccount(ACCOUNT_ID_VALUE, updateAccount, LOGGED_IN_USER_ID_VALUE);
        });
        assertEquals(ACCOUNT_DOES_NOT_EXIST, exception.getCode());
        assertEquals(MessageFormat.format(ACCOUNT_DOES_NOT_EXIST_MSG, ACCOUNT_ID_VALUE.toString()),
                exception.getMessage());
        assertEquals(AccountApiConstants.ACCOUNT_ID, exception.getProperty().get(0).getKey());
        assertEquals(ACCOUNT_ID_VALUE.toString(), exception.getProperty().get(0).getValues().get(0));
    }

    @Test
    void testGetAccountEmptyRole() throws ResourceNotFoundException {
        // Arrange
        String accountName = "Test Account";

        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);
        savedEntity.setId(ACCOUNT_ID_VALUE);
        savedEntity.setDefaultRoles(null);
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.of(savedEntity));
        // Act
        GetAccountApiResponse response = accountsService.getAccount(ACCOUNT_ID_VALUE);
        // Assert
        ArgumentCaptor<BigInteger> bigIntegerArgument = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<AccountStatus> statusArgument = ArgumentCaptor.forClass(AccountStatus.class);
        Mockito.verify(accountRepository, times(1)).findByIdAndStatusNot(bigIntegerArgument.capture(),
                statusArgument.capture());
        assertNotNull(response);
        assertEquals(accountName, response.getAccountName());
        assertTrue(response.getRoles().isEmpty());
    }

    @Test
    void testGetAccountInvalidRole() throws ResourceNotFoundException {
        // Arrange
        String role1 = ROLE_VALUE;
        String role2 = ROLE_2;
        String accountName = "Test Account";
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName(accountName);
        Set<String> expectedRoles = new HashSet<>(Set.of(role1, role2));
        accountDto.setRoles(expectedRoles);
        // Make database entry role id as invalid ,so setting to 3
        // Map Roles role1 to RoleListRepresentation
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(role1, null);
        // Set the scope for the user.
        roleListDto.getRoles().stream().findFirst().get().getScopes().get(0).setName("ViewAccounts");
        // Mock getRoleListByName from authManagementService
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        AccountEntity savedEntity = new AccountEntity();
        savedEntity.setAccountName(accountName);
        savedEntity.setId(ACCOUNT_ID_VALUE);
        savedEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        savedEntity.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.of(savedEntity));

        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.getAccount(ACCOUNT_ID_VALUE);
        });

        ArgumentCaptor<BigInteger> bigIntegerArgument = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<AccountStatus> statusArgument = ArgumentCaptor.forClass(AccountStatus.class);
        Mockito.verify(accountRepository, times(1)).findByIdAndStatusNot(bigIntegerArgument.capture(),
                statusArgument.capture());
        assertEquals(INTERNAL_SERVER_ERROR, exception.getHttpStatus(),
                "Status should be 500 because it's BL validation");
        assertEquals(ACCOUNT_ROLE_VALIDATION_FAILURE, exception.getCode());

    }

    @Test
    void testUpdateUserDefaultAccount() throws NoSuchAlgorithmException, ResourceNotFoundException,
            AccountManagementException, UserNotFoundException {
        String parentIdAsString = String.valueOf(PARENT_ID_VALUE);
        UpdateAccountDto updateAccount = new UpdateAccountDto();
        updateAccount.setParentId(parentIdAsString);

        AccountEntity dbEntity = new AccountEntity();
        dbEntity.setAccountName(ACCOUNT_NAME);
        dbEntity.setId(new BigInteger(USER_DEFAULT_ACCOUNTID));
        dbEntity.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        dbEntity.setStatus(AccountStatus.ACTIVE);
        dbEntity.setParentId(PARENT_ID_VALUE);
        
        when(accountRepository.findByIdAndStatusNot(any(BigInteger.class), eq(AccountStatus.DELETED)))
                .thenReturn(Optional.of(dbEntity));

        BigInteger accountId = new BigInteger(USER_DEFAULT_ACCOUNTID);
        AccountManagementException exception = assertThrows(AccountManagementException.class, () -> {
            accountsService.updateAccount(accountId, updateAccount, LOGGED_IN_USER_ID_VALUE);
        });
        assertEquals(CANNOT_UPDATE_DEFAULT_ACCOUNT, exception.getCode());
        assertEquals(MessageFormat.format(CANNOT_UPDATE_DEFAULT_ACCOUNT_MSG, USER_DEFAULT_ACCOUNTID),
                exception.getMessage());
        assertEquals(AccountApiConstants.ACCOUNT_ID, exception.getProperty().get(0).getKey());
        assertEquals(USER_DEFAULT_ACCOUNTID, exception.getProperty().get(0).getValues().get(0));
    }

}