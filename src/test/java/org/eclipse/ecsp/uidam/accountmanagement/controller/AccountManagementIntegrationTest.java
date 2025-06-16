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

package org.eclipse.ecsp.uidam.accountmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.AccountFilterDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.CreateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.FilterAccountsApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.GetAccountApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.enums.SortOrder;
import org.eclipse.ecsp.uidam.usermanagement.exception.handler.UnifiedErrorDetails;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAccountRoleMappingRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ALREADY_EXISTS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ALREADY_EXISTS_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ACCOUNT_NAME_PATTERN;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ACCOUNT_NAME_PATTERN_MSG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IGNORE_CASE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_3;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_4;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_5;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_6;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_7;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE_3;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE_4;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE_5;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE_6;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE_7;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_3;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_4;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_5;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_6;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_SELF_MNG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { AccountRepository.class })
@AutoConfigureWebTestClient(timeout = "3600000")
@EnableJpaRepositories(basePackages = "org.eclipse.ecsp")
@ComponentScan(basePackages = { "org.eclipse.ecsp" })
@EntityScan("org.eclipse.ecsp")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountManagementIntegrationTest {

    private static final int INDEX_0 = 0;
    private static final int INDEX_1 = 1;
    private static final int INDEX_2 = 2;
    private static final int INDEX_3 = 3;
    private static final int INDEX_4 = 4;
    private static final int INDEX_5 = 5;
    private static final int HOUR_IN_MS = 3600000;


    private Logger logger = LoggerFactory.getLogger(AccountManagementIntegrationTest.class);
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AccountRepository accountRepository;

    private ObjectMapper mapper = new ObjectMapper();

    @MockBean
    private RolesService rolesService;
    
    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    private List<AccountEntity> accountEntityList;

    @MockBean
    private UserAccountRoleMappingRepository userAccountRoleMappingRepository;
    
    @MockBean
    PasswordValidationService passwordValidationService;

    @BeforeEach
    public void init() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @BeforeAll
    public void beforeAll() {
        accountEntityList = List.of(
        addAccountIntoDb("Test Account Name1", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE,
                PARENT_ID_VALUE_1),
        addAccountIntoDb("Test Account Name2", AccountStatus.ACTIVE, ROLE_ID_2, ACCOUNT_ID_VALUE,
                PARENT_ID_VALUE_2),
        addAccountIntoDb("Test Account Name3", AccountStatus.PENDING, ROLE_ID_3, ACCOUNT_ID_VALUE,
                PARENT_ID_VALUE_3),
        addAccountIntoDb("Test Account Name4", AccountStatus.SUSPENDED, ROLE_ID_4, ACCOUNT_ID_VALUE,
                PARENT_ID_VALUE_4),
        addAccountIntoDb("Test Account Name5", AccountStatus.ACTIVE, ROLE_ID_5, ACCOUNT_ID_VALUE,
                PARENT_ID_VALUE_5),
        addAccountIntoDb("Test Account Name6", AccountStatus.DELETED, ROLE_ID_6, ACCOUNT_ID_VALUE,
                PARENT_ID_VALUE_6)
         );
    }

    private AccountEntity addAccountIntoDb(String accountName, AccountStatus accountStatus, BigInteger roleId,
            BigInteger id, BigInteger parentId) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setAccountName(accountName);
        accountEntity.setStatus(accountStatus);
        if (roleId != null) {
            accountEntity.setDefaultRoles(Set.of(roleId));
        }
        accountEntity.setParentId(parentId);
        accountEntity.setId(id);
        accountEntity.setCreatedBy("Dev_Team");
        accountEntity.setUpdatedBy("QA_Team");
        accountEntity.setCreateDate(new Timestamp(System.currentTimeMillis()));
        accountEntity.setUpdateDate(new Timestamp(System.currentTimeMillis() + HOUR_IN_MS));
        return accountEntity;
    }

    public static RoleListRepresentation createRoleListDtoRepresentation(Map<String, BigInteger> roles) {
        Set<RoleCreateResponse> roleDtoList = roles.entrySet().stream().map(role -> {
            RoleCreateResponse createRole = new RoleCreateResponse();
            Scope scopeDto = new Scope();
            scopeDto.setName(SCOPE_SELF_MNG);
            createRole.setName(role.getKey());
            createRole.setId(role.getValue());
            createRole.setScopes(Collections.singletonList(scopeDto));
            return createRole;
        }).collect(Collectors.toSet());

        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }

    private List<GetAccountApiResponse> convertJsonToAccountFilterListResponse(byte[] arr) {
        List<GetAccountApiResponse> accountResponseList = null;
        try {
            FilterAccountsApiResponse response = mapper.readValue(arr, FilterAccountsApiResponse.class);
            accountResponseList = response.getItems();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return accountResponseList;
    }

    @Test
    void testCreateAccount() throws IOException {
        CreateAccountDto createDto = new CreateAccountDto();
        createDto.setAccountName("TestAccount");

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName("TestAccount")).thenReturn(
                Optional.empty());
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(accountEntity);
        byte[] response = webTestClient.post().uri("/v1/accounts").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(createDto).exchange().expectStatus().isEqualTo(HttpStatus.CREATED).expectBody().returnResult()
                .getResponseBody();
        verify(accountRepository, times(1)).save(any(AccountEntity.class));
        CreateAccountResponse accountResponsedto = mapper.readValue(response, CreateAccountResponse.class);
        Assertions.assertEquals(ACCOUNT_ID_VALUE.toString(), accountResponsedto.getId());
    }

    @Test
    void testUserAssociatedDeleteAccount() {
        AccountEntity accountEntity = addAccountIntoDb("Test Account Name",
                AccountStatus.ACTIVE, BigInteger.ONE, ACCOUNT_ID_VALUE, null);
        when(accountRepository.findByIdAndStatusNot(ACCOUNT_ID_VALUE, AccountStatus.DELETED)).thenReturn(
                Optional.of(accountEntity));
        when(userAccountRoleMappingRepository.existsByAccountId(accountEntity.getId())).thenReturn(true);
        webTestClient
                .delete().uri(uriBuilder -> uriBuilder.path("/v1/accounts/{account_id}").build(accountEntity.getId()))
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    void testCreateAccountExist() throws IOException {
        String accountName = "TestAccount";
        AccountEntity accountEntity = addAccountIntoDb(accountName, AccountStatus.ACTIVE, ROLE_ID_1,
                ACCOUNT_ID_VALUE, null);
        CreateAccountDto createDto = new CreateAccountDto();
        createDto.setAccountName(accountName);
        when(accountRepository.findByAccountName(accountName)).thenReturn(
                Optional.of(accountEntity));
        byte[] response = webTestClient.post().uri("/v1/accounts").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(createDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().returnResult()
                .getResponseBody();
        UnifiedErrorDetails error = mapper.readValue(response, UnifiedErrorDetails.class);
        Assertions.assertEquals("error", error.getStatus());
        Assertions.assertEquals(ACCOUNT_ALREADY_EXISTS, error.getCode());
        assertEquals(MessageFormat.format(ACCOUNT_ALREADY_EXISTS_MSG, accountName), error.getMessage());
        Assertions.assertEquals("account_name", error.getProperties().get(0).getKey());
        Assertions.assertEquals(accountName, error.getProperties().get(0).getValues().get(0));
    }

    @Test
    void testCreateAccountInvalidAccount() throws IOException {
        String accountName = "testaccount*";
        CreateAccountDto createDto = new CreateAccountDto();
        createDto.setAccountName(accountName);
        byte[] response = webTestClient.post().uri("/v1/accounts").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(createDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().returnResult()
                .getResponseBody();
        UnifiedErrorDetails error = mapper.readValue(response, UnifiedErrorDetails.class);
        Assertions.assertEquals("error", error.getStatus());
        Assertions.assertEquals(INVALID_INPUT_ACCOUNT_NAME_PATTERN, error.getCode());
        assertEquals(MessageFormat.format(INVALID_INPUT_ACCOUNT_NAME_PATTERN_MSG, accountName), error.getMessage());
        Assertions.assertEquals("account_name", error.getProperties().get(0).getKey());
        Assertions.assertEquals(accountName, error.getProperties().get(0).getValues().get(0));
    }

    @Test
    void testGetAccountsSuccessForResponseBody() {
        String role = "Test_Role";
        BigInteger roleId = ROLE_ID_1;
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put(role, roleId);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(Set.of(roleId))).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        AccountStatus status = AccountStatus.PENDING;
        accountFilterDto.setParentIds(Set.of(PARENT_ID_VALUE));
        accountFilterDto.setStatus(Set.of(status));
        accountFilterDto.setRoles(Set.of(role));
        String accountName = "Test Account Name";
        accountFilterDto.setAccountNames(Set.of(accountName));
        AccountEntity accountEntity = addAccountIntoDb(accountName, status, roleId, ACCOUNT_ID_VALUE, PARENT_ID_VALUE);
        accountFilterDto.setIds(Set.of(accountEntity.getId()));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(accountEntity));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

        GetAccountApiResponse accountResponse = accountResponseList.get(0);
        Assertions.assertEquals(accountName, accountResponse.getAccountName(),
                "accountResponse account name should not mismatch");
        Assertions.assertEquals(accountEntity.getId().toString(), accountResponse.getId(),
                "accountResponse id  should not mismatch");
        Assertions.assertEquals(Set.of(role), accountResponse.getRoles(), "accountResponse roles should not mismatch");
        Assertions.assertEquals(status, accountResponse.getStatus(), "accountResponse status should not mismatch");
        Assertions.assertEquals(PARENT_ID_VALUE.toString(), accountResponse.getParentId(),
                "accountResponse parent_id should not mismatch");
        Assertions.assertEquals("Dev_Team", accountResponse.getCreatedBy(),
                "accountResponse Created by should not mismatch");
        Assertions.assertEquals("QA_Team", accountResponse.getUpdatedBy(),
                "accountResponse updated by should not mismatch");
        Assertions.assertNotNull(accountResponse.getCreateDate(), "accountResponse create date should not be null");
        Assertions.assertNotNull(accountResponse.getUpdateDate(), "accountResponse update date should not be null");

    }

    @Test
    void testGetAccountsSuccessForStatus() {
        String role = "Role1";
        BigInteger roleId = ROLE_ID_1;
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put(role, roleId);
        AccountEntity accountEntity = addAccountIntoDb("Test Account", AccountStatus.PENDING, roleId,
                ACCOUNT_ID_VALUE, PARENT_ID_VALUE);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(accountEntity));
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(Set.of(roleId))).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");
    }

    @Test
    void testGetAccountsWithNoRoles() {
        String role = "Role1";
        BigInteger roleId = ROLE_ID_1;
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put(role, roleId);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(Set.of(roleId))).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean()))
                .thenThrow(new EntityNotFoundException());
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_One"));
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_0, accountResponseList.size(), "accountResponseList size should be 0");
    }

    @Test
    void testGetAccountsSuccessForId() {
        String role = "Role1";
        BigInteger roleId = ROLE_ID_1;
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put(role, roleId);
        AccountEntity accountEntity = addAccountIntoDb("Test Account", AccountStatus.ACTIVE, roleId, ACCOUNT_ID_VALUE,
                PARENT_ID_VALUE);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(accountEntity));
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(Set.of(roleId))).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setIds(Set.of(accountEntity.getId()));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

        // Negative scenario where id is not matched to any Account
        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setIds(Set.of(ACCOUNT_ID_VALUE));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");

    }

    @Test
    void testGetAccountsSuccessForAccountName() {
        String role = "Role1";
        BigInteger roleId = ROLE_ID_1;
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put(role, roleId);
        String accountName = "Test Account Name";
        AccountEntity accountEntity = addAccountIntoDb(accountName, AccountStatus.PENDING, roleId,
                ACCOUNT_ID_VALUE, PARENT_ID_VALUE);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(accountEntity));
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(Set.of(roleId))).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of(accountName));
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

        // Negative scenario with name that is not matched to any account
        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("DUMMY_ACCOUNT_NAME"));
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");

    }

    @Test
    void testGetAccountsSuccessForRole() {
        String role = "Test_Role";
        BigInteger roleId = ROLE_ID_1;
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put(role, roleId);
        AccountEntity accountEntity = addAccountIntoDb("Test Account Name", AccountStatus.PENDING, roleId,
                ACCOUNT_ID_VALUE, PARENT_ID_VALUE);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(accountEntity));
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(Set.of(roleId))).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of(role));
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Dummy_Role"));
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        // Auth server return no roles
        roleListDto = new RoleListRepresentation();
        when(rolesService.getRoleById(Set.of(roleId))).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");
    }

    @Test
    void testGetAccountsSuccessForParentId() {
        String role = "Test_Role";
        BigInteger roleId = ROLE_ID_1;
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put(role, roleId);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(Set.of(roleId))).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name", AccountStatus.PENDING, roleId, ACCOUNT_ID_VALUE, PARENT_ID_VALUE)
        ));
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setParentIds(Set.of(PARENT_ID_VALUE));
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

        // Negative scenario to check parentId that is not matched to any account
        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setParentIds(Set.of(PARENT_ID_VALUE_1));
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");

    }

    @Test
    void testGetAccountsFilterMultipleAccountNamesAndRoles() {
        Map<String, BigInteger> roleNameId = insertMultiAccountData();
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name1", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE_1,
                        PARENT_ID_VALUE_1),
                addAccountIntoDb("Test Account Name2", AccountStatus.ACTIVE, ROLE_ID_2, ACCOUNT_ID_VALUE_2,
                        PARENT_ID_VALUE_2)
        ));
        // test getAccounts for multiple account names with default status active
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("Test Account Name1", "Test Account Name2"));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_2, accountResponseList.size(), "accountResponseList size should be 2");

        // test getAccounts for multiple Role names with default status active
        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_One", "Test_Role_Two", "Test_Role_Three"));
        response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_2, accountResponseList.size(), "accountResponseList size should be 2");

    }

    @Test
    void testGetAccountsFilterMultipleAccountNamesAndRolesForPending() {
        // Mock Auth server response
        insertMultiAccountData();
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Three", ROLE_ID_3);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name3", AccountStatus.PENDING, ROLE_ID_3, ACCOUNT_ID_VALUE_3,
                        PARENT_ID_VALUE_3)
        ));
        // test getAccounts for multiple Role names with custom status pending
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_One", "Test_Role_Two", "Test_Role_Three"));
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");
        // Mock Auth server response
        roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Three", ROLE_ID_3);
        roleNameId.put("Test_Role_Four", ROLE_ID_4);
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        roleNameId.put("Test_Role_Six", ROLE_ID_6);
        roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name1", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE_1,
                        PARENT_ID_VALUE_1),
                addAccountIntoDb("Test Account Name2", AccountStatus.ACTIVE, ROLE_ID_2, ACCOUNT_ID_VALUE_2,
                        PARENT_ID_VALUE_2),
                addAccountIntoDb("Test Account Name3", AccountStatus.PENDING, ROLE_ID_3, ACCOUNT_ID_VALUE_3,
                        PARENT_ID_VALUE_3),
                addAccountIntoDb("Test Account Name6", AccountStatus.PENDING, ROLE_ID_6, ACCOUNT_ID_VALUE_6,
                        PARENT_ID_VALUE_6)
        ));
        // test getAccounts for multiple Status
        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setStatus(Set.of(AccountStatus.PENDING, AccountStatus.ACTIVE));
        response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_4, accountResponseList.size(), "accountResponseList size should be 4");

    }


    private Map<String, BigInteger> insertMultiAccountData() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Three", ROLE_ID_3);
        roleNameId.put("Test_Role_Four", ROLE_ID_4);
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        roleNameId.put("Test_Role_Six", ROLE_ID_6);
        return roleNameId;
    }

    @Test
    void testAccountsFilterMultipleAccountNamesAndRolesCustomStatus() {
        Map<String, BigInteger> roleNameId = insertMultiAccountData();
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name3", AccountStatus.PENDING, ROLE_ID_3,
                        ACCOUNT_ID_VALUE_3, PARENT_ID_VALUE_3),
                addAccountIntoDb("Test Account Name4", AccountStatus.DELETED, ROLE_ID_4,
                        ACCOUNT_ID_VALUE_4, PARENT_ID_VALUE_4),
                addAccountIntoDb("Test Account Name5", AccountStatus.SUSPENDED, ROLE_ID_5,
                        ACCOUNT_ID_VALUE_5, PARENT_ID_VALUE_5))
        );
        // test getAccounts for multiple Account Names, Role names with custom status
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(
                Set.of("Test_Role_One", "Test_Role_Two", "Test_Role_Three", "Test_Role_Four", "Test_Role_Five"));
        accountFilterDto.setStatus(
                Set.of(AccountStatus.PENDING, AccountStatus.ACTIVE, AccountStatus.DELETED, AccountStatus.SUSPENDED));
        accountFilterDto.setAccountNames(Set.of("Test Account Name1", "Test Account Name2", "Test Account Name3",
                "Test Account Name4", "Test Account Name5"));
        accountFilterDto.setParentIds(Set.of(PARENT_ID_VALUE_3,
                PARENT_ID_VALUE_4, PARENT_ID_VALUE_5));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_3, accountResponseList.size(), "accountResponseList size should be 3");
    }


    @Test
    void testAccountsFilterMultipleAccountNamesAndSameRolesCustomStatus() {
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name1", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE_1,
                        PARENT_ID_VALUE_1),
                addAccountIntoDb("Test Account Name2", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE_2,
                        PARENT_ID_VALUE_2),
                addAccountIntoDb("Test Account Name3", AccountStatus.PENDING, ROLE_ID_3, ACCOUNT_ID_VALUE_3,
                        PARENT_ID_VALUE_3),
                addAccountIntoDb("Test Account Name4", AccountStatus.DELETED, ROLE_ID_4, ACCOUNT_ID_VALUE_4,
                        PARENT_ID_VALUE_4),
                addAccountIntoDb("Test Account Name5", AccountStatus.SUSPENDED, ROLE_ID_5, ACCOUNT_ID_VALUE_5,
                        PARENT_ID_VALUE_5)
        ));

        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        // test getAccounts for multiple Account Names, Role names with custom status
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(
                Set.of("Test_Role_One"));
        accountFilterDto.setStatus(
                Set.of(AccountStatus.PENDING, AccountStatus.ACTIVE, AccountStatus.DELETED, AccountStatus.SUSPENDED));
        accountFilterDto.setAccountNames(Set.of("Test Account Name1", "Test Account Name2", "Test Account Name3",
                "Test Account Name4", "Test Account Name5"));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_2, accountResponseList.size(), "accountResponseList size should be 2");
    }

    @Test
    void testGetAccountsForCaseCheck() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        // test getAccounts for multiple account names to check without case sensitive
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("test account name1", "test account name2"));
        accountFilterDto.setStatus(Set.of(AccountStatus.BLOCKED));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");

        // test getAccounts for multiple account names to check with case sensitive
        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("test account name1", "test account name2"));
        accountFilterDto.setStatus(Set.of(AccountStatus.BLOCKED));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name2", AccountStatus.BLOCKED, ROLE_ID_2, ACCOUNT_ID_VALUE_2,
                        PARENT_ID_VALUE_2)
        ));
        response = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").queryParam(IGNORE_CASE, true).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

    }

    @Test
    void testGetAccountsForFilteringMultipleAccountAndSorting() {
        Map<String, BigInteger> roleNameId = insertDataForMultipleAccountWithSorting();
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        // test getAccounts for multiple account names with default status active and
        // sorting them by roles
        // descending order default
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_One", "Test_Role_Two", "Test_Role_Three", "Test_Role_Four",
                "Test_Role_FIVE", "Test_Role_Six"));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name5", AccountStatus.ACTIVE, ROLE_ID_5, ACCOUNT_ID_VALUE_5,
                        PARENT_ID_VALUE_5),
                addAccountIntoDb("Test Account Name4", AccountStatus.ACTIVE, ROLE_ID_4, ACCOUNT_ID_VALUE_4,
                        PARENT_ID_VALUE_4)
        ));
        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam(IGNORE_CASE, true).queryParam(SORT_BY, AccountFilterDto.AccountFilterDtoEnum.ROLES).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_2, accountResponseList.size(), "accountResponseList size should be 4");

        Assertions.assertTrue(accountResponseList.get(INDEX_0).getRoles().contains("Test_Role_Five"),
                "Role should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_1).getRoles().contains("Test_Role_Four"),
                "Role should not mismatch");
        // Testing for ascending order
        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_One", "Test_Role_Two", "Test_Role_Three", "Test_Role_Four",
                "Test_Role_FIVE", "Test_Role_Six"));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name4", AccountStatus.ACTIVE, ROLE_ID_4, ACCOUNT_ID_VALUE_4,
                        PARENT_ID_VALUE_4),
                addAccountIntoDb("Test Account Name5", AccountStatus.ACTIVE, ROLE_ID_5, ACCOUNT_ID_VALUE_5,
                        PARENT_ID_VALUE_5)
        ));
        response = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").queryParam(IGNORE_CASE, true)
                        .queryParam(SORT_BY, AccountFilterDto.AccountFilterDtoEnum.ROLES)
                        .queryParam(SORT_ORDER, SortOrder.ASC).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_2, accountResponseList.size(), "accountResponseList size should be 4");

        Assertions.assertTrue(accountResponseList.get(INDEX_0).getRoles().contains("Test_Role_Four"),
                "Role should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_1).getRoles().contains("Test_Role_Five"),
                "Role should not mismatch");
    }

    private Map<String, BigInteger> insertDataForMultipleAccountWithSorting() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Three", ROLE_ID_3);
        roleNameId.put("Test_Role_Four", ROLE_ID_4);
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        roleNameId.put("Test_Role_Six", ROLE_ID_6);
        return roleNameId;
    }

    @Test
    void testGetAccountsForFilteringMultipleAccountWithNullRolesInDb() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Three", ROLE_ID_3);
        roleNameId.put("Test_Role_Four", ROLE_ID_4);
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        roleNameId.put("Test_Role_Six", ROLE_ID_6);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name5", AccountStatus.ACTIVE, ROLE_ID_5, ACCOUNT_ID_VALUE_5,
                        PARENT_ID_VALUE_5),
                addAccountIntoDb("Test Account Name4", AccountStatus.ACTIVE, ROLE_ID_4, ACCOUNT_ID_VALUE_4,
                        PARENT_ID_VALUE_4),
                addAccountIntoDb("Test Account Name2", AccountStatus.ACTIVE, ROLE_ID_2, ACCOUNT_ID_VALUE_2,
                        PARENT_ID_VALUE_2)
        ));
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        // test getAccounts for multiple account names with default status active and
        // sorting them by roles
        // descending order default
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setStatus(Set.of(AccountStatus.ACTIVE));
        accountFilterDto.setRoles(Set.of("Test_Role_Two", "Test_Role_Four", "Test_Role_FIVE"));
        accountFilterDto.setAccountNames(Set.of("Test Account Name1", "Test Account Name2", "Test Account Name3",
                "Test Account Name4", "Test Account Name5", "Test Account Name6"));
        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam(IGNORE_CASE, true).queryParam(SORT_BY, AccountFilterDto.AccountFilterDtoEnum.ROLES).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_3, accountResponseList.size(), "accountResponseList size should be 3");

        Assertions.assertTrue(accountResponseList.get(INDEX_0).getRoles().contains("Test_Role_Five"),
                "Role should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_1).getRoles().contains("Test_Role_Four"),
                "Role should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_2).getRoles().contains("Test_Role_Two"),
                "Role should not mismatch");
    }

    @Test
    void testGetAccountsForFilteringSortingExplicitDesc() {
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name5", AccountStatus.ACTIVE, ROLE_ID_5, ACCOUNT_ID_VALUE_5,
                        PARENT_ID_VALUE_5),
                addAccountIntoDb("Test Account Name4", AccountStatus.ACTIVE, ROLE_ID_4, ACCOUNT_ID_VALUE_4,
                        PARENT_ID_VALUE_4),
                addAccountIntoDb("Test Account Name2", AccountStatus.ACTIVE, ROLE_ID_2, ACCOUNT_ID_VALUE_2,
                        PARENT_ID_VALUE_2),
                addAccountIntoDb("Test Account Name1", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE_1,
                        PARENT_ID_VALUE_1)
        ));
        Map<String, BigInteger> roleNameId = insertDataForMultipleAccountWithSorting();
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        // Testing for descending order explicit
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_One", "Test_Role_Two", "Test_Role_Three", "Test_Role_Four",
                "Test_Role_FIVE", "Test_Role_Six"));
        byte[] response = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").queryParam(IGNORE_CASE, true)
                        .queryParam(SORT_BY, AccountFilterDto.AccountFilterDtoEnum.ROLES)
                        .queryParam(SORT_ORDER, SortOrder.DESC).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_4, accountResponseList.size(), "accountResponseList size should be 4");
        Assertions.assertTrue(accountResponseList.get(INDEX_0).getRoles().contains("Test_Role_Five"),
                "Role should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_1).getRoles().contains("Test_Role_Four"),
                "Role should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_2).getRoles().contains("Test_Role_Two"),
                "Role should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_3).getRoles().contains("Test_Role_One"),
                "Role should not mismatch");

    }

    @Test
    void testFilterAccountsDefaultAccountNameSorting() {
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("Test Account Name5", AccountStatus.ACTIVE, ROLE_ID_5, ACCOUNT_ID_VALUE_5,
                        PARENT_ID_VALUE_5),
                addAccountIntoDb("Test Account Name4", AccountStatus.ACTIVE, ROLE_ID_4, ACCOUNT_ID_VALUE_4,
                        PARENT_ID_VALUE_4),
                addAccountIntoDb("Test Account Name2", AccountStatus.ACTIVE, ROLE_ID_2, ACCOUNT_ID_VALUE_2,
                        PARENT_ID_VALUE_2),
                addAccountIntoDb("Test Account Name1", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE_1,
                        PARENT_ID_VALUE_1)
        ));
        Map<String, BigInteger> roleNameId = insertDataForMultipleAccountWithSorting();
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        // Testing for default account name used for sort by
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_One", "Test_Role_Two", "Test_Role_Three", "Test_Role_Four",
                "Test_Role_FIVE", "Test_Role_Six"));
        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam(IGNORE_CASE, true).queryParam(SORT_ORDER, SortOrder.DESC).build()).headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_4, accountResponseList.size(), "accountResponseList size should be 4");

        Assertions.assertTrue(accountResponseList.get(INDEX_0).getAccountName().contains("Test Account Name5"),
                "Account Name should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_1).getAccountName().contains("Test Account Name4"),
                "Account Name should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_2).getAccountName().contains("Test Account Name2"),
                "Account Name should not mismatch");
        Assertions.assertTrue(accountResponseList.get(INDEX_3).getAccountName().contains("Test Account Name1"),
                "Account Name should not mismatch");
    }

    @Test
    void testGetAccountsWithEmptyPayload() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Three", ROLE_ID_3);
        roleNameId.put("Test_Role_Four", ROLE_ID_4);
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        roleNameId.put("Test_Role_Six", ROLE_ID_6);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                accountEntityList.get(INDEX_0),
                accountEntityList.get(INDEX_1),
                accountEntityList.get(INDEX_4)
        ));
        byte[] response = webTestClient.post().uri("/v1/accounts/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(new AccountFilterDto()).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_3, accountResponseList.size(), "accountResponseList size should be 3");
        Assertions.assertEquals(AccountStatus.ACTIVE, accountResponseList.get(INDEX_0).getStatus(),
                "Account Status should be active");
        Assertions.assertEquals(AccountStatus.ACTIVE, accountResponseList.get(INDEX_1).getStatus(),
                "Account Status should be active");
        Assertions.assertEquals(AccountStatus.ACTIVE, accountResponseList.get(INDEX_2).getStatus(),
                "Account Status should be active");
    }

    @Test
    void testGetAccountsForSearchModePreFix() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Three", ROLE_ID_3);
        roleNameId.put("Test_Role_Four", ROLE_ID_4);
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        roleNameId.put("Test_Role_Six", ROLE_ID_6);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                accountEntityList.get(INDEX_0),
                accountEntityList.get(INDEX_1),
                accountEntityList.get(INDEX_4)
        ));
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        // For PREFIX
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role"));

        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam("searchMode", SearchType.PREFIX).build()).headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_3, accountResponseList.size(), "accountResponseList size should be 3");
    }

    @Test
    void testGetAccountsForSearchModeSuffix() {
        // For SUFFIX
        // Mock Auth server response
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                accountEntityList.get(INDEX_0),
                accountEntityList.get(INDEX_1),
                accountEntityList.get(INDEX_4)
        ));
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Two", "FiVe"));
        byte[] response = webTestClient
                .post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                        .queryParam("searchMode", SearchType.SUFFIX).queryParam(IGNORE_CASE, true).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_2, accountResponseList.size(), "accountResponseList size should be 2");

    }

    @Test
    void testGetAccountsForAccountNameValidation() {
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("test___", AccountStatus.ACTIVE, null, ACCOUNT_ID_VALUE_4,
                        PARENT_ID_VALUE_4)
        ));
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("test___", "test_%_"));

        byte[] response = webTestClient
            .post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam("searchMode", SearchType.EQUAL).queryParam(IGNORE_CASE, false).build())
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
            }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
            .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

        Assertions.assertEquals("test___", accountResponseList.get(INDEX_0).getAccountName(),
                "Account Name should not mismatch");
        Assertions.assertEquals(accountResponseList.get(INDEX_0).getParentId(),
            PARENT_ID_VALUE_4.toString(), "Parent Id should not mismatch");

        accountFilterDto.setAccountNames(Set.of("test_"));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        response = webTestClient
            .post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam("searchMode", SearchType.EQUAL).queryParam(IGNORE_CASE, false).build())
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
            }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
            .returnResult().getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_0, accountResponseList.size(), "accountResponseList size should be 0");
    }


    @Test
    void testGetAccountsForAccountName() {
        AccountEntity accountEntity = addAccountIntoDb("test_", AccountStatus.ACTIVE, null, ACCOUNT_ID_VALUE_2,
                PARENT_ID_VALUE_2);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
               accountEntity
        ));
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("test_"));

        byte[] response = webTestClient
            .post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam("searchMode", SearchType.EQUAL).queryParam(IGNORE_CASE, false).build())
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
            }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
            .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

        Assertions.assertEquals("test_", accountResponseList.get(INDEX_0).getAccountName(),
                "Account Name should not mismatch");
        Assertions.assertEquals(accountResponseList.get(INDEX_0).getParentId(),
            PARENT_ID_VALUE_2.toString(), "Parent Id should not mismatch");
    }

    @Test
    void testGetAccountsForAccountNameForContainsOperation() {
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                addAccountIntoDb("tes_t1", AccountStatus.ACTIVE, null, ACCOUNT_ID_VALUE_1,
                        PARENT_ID_VALUE_1),
                addAccountIntoDb("test2_", AccountStatus.ACTIVE, null, ACCOUNT_ID_VALUE_2,
                        PARENT_ID_VALUE_2),
                addAccountIntoDb("te__st3", AccountStatus.ACTIVE, null, ACCOUNT_ID_VALUE_5,
                        PARENT_ID_VALUE_5),
                addAccountIntoDb("_test7", AccountStatus.ACTIVE, null, ACCOUNT_ID_VALUE_7,
                        PARENT_ID_VALUE_7)
        ));

        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("_"));

        byte[] response = webTestClient
            .post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam("searchMode", SearchType.CONTAINS).queryParam(IGNORE_CASE, false).build())
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
            }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
            .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_4, accountResponseList.size(), "accountResponseList size should be 4");

        List<String> accountNameList = accountResponseList.stream()
            .map(account -> account.getAccountName()).toList();
        Assertions.assertTrue(accountNameList.contains("tes_t1"), "Account Name should be present in the list");
        Assertions.assertTrue(accountNameList.contains("test2_"), "Account Name should be present in the list");
        Assertions.assertTrue(accountNameList.contains("te__st3"), "Account Name should be present in the list");
        Assertions.assertTrue(accountNameList.contains("_test7"), "Account Name should be present in the list");
    }


    @Test
    void testGetAccountsForUnderScoreForPrefixOperation() {
        AccountEntity accountEntity = addAccountIntoDb("test_1", AccountStatus.ACTIVE, null, ACCOUNT_ID_VALUE,
                PARENT_ID_VALUE_1);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                accountEntity
        ));

        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("test_"));

        byte[] response = webTestClient
            .post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                .queryParam("searchMode", SearchType.PREFIX).queryParam(IGNORE_CASE, false).build())
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
            }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
            .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");

        Assertions.assertEquals("test_1", accountResponseList.get(INDEX_0).getAccountName(),
                "Account Name should not mismatch");

    }

    @Test
    void testGetAccountsForSearchModeContains() {
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                accountEntityList.get(INDEX_0),
                accountEntityList.get(INDEX_1),
                accountEntityList.get(INDEX_4)
        ));
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        roleNameId.put("Test_Role_Three", ROLE_ID_3);
        roleNameId.put("Test_Role_Four", ROLE_ID_4);
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        roleNameId.put("Test_Role_Six", ROLE_ID_6);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(), Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Role", "FiVe"));

        byte[] response = webTestClient
                .post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                        .queryParam("searchMode", SearchType.CONTAINS).queryParam(IGNORE_CASE, true).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_3, accountResponseList.size(), "accountResponseList size should be 3");

        // For Equals
        // For CONTAINS
        roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_Five", ROLE_ID_5);
        roleNameId.put("Test_Role_Six", ROLE_ID_6);
        roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                accountEntityList.get(INDEX_0),
                accountEntityList.get(INDEX_1),
                accountEntityList.get(INDEX_4),
                accountEntityList.get(INDEX_5)
        ));
        accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_Six", "Test_Role_Five"));
        accountFilterDto.setStatus(Set.of(AccountStatus.ACTIVE, AccountStatus.DELETED));

        response = webTestClient
                .post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter")
                        .queryParam("searchMode", SearchType.EQUAL).queryParam(IGNORE_CASE, true).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_2, accountResponseList.size(), "accountResponseList size should be 2");
    }

    @Test
    void testGetAccountsForDefaultSearchMode() {
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                accountEntityList.get(INDEX_4),
                accountEntityList.get(INDEX_5)
        ));
        Map<String, BigInteger> roleNameIdResultByRoleId = new HashMap<>();
        roleNameIdResultByRoleId.put("Test_Role_Five", ROLE_ID_5);
        roleNameIdResultByRoleId.put("Test_Role_Six", ROLE_ID_6);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameIdResultByRoleId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        // For Default search mode (equals)
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_Six", "Test_Role_Five"));
        accountFilterDto.setStatus(Set.of(AccountStatus.ACTIVE, AccountStatus.DELETED));
        byte[] response = webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").queryParam(IGNORE_CASE, true).build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_2, accountResponseList.size(), "accountResponseList size should be 2");

    }

    @Test
    void testGetAccountsForDeletedStatus() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        AccountEntity accountEntity = addAccountIntoDb("Test Account Name2", AccountStatus.DELETED, ROLE_ID_2,
                ACCOUNT_ID_VALUE, PARENT_ID_VALUE_2);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(accountEntity));
        // For deleted Status
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setStatus(Set.of(AccountStatus.DELETED));

        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(INDEX_1, accountResponseList.size(), "accountResponseList size should be 1");
        Assertions.assertEquals(AccountStatus.DELETED, accountResponseList.get(0).getStatus(),
                "account status should be deleted");
        Assertions.assertEquals("Test Account Name2", accountResponseList.get(0).getAccountName(),
                "account status should be deleted");
    }

    @Test
    void testGetAccountsWithNoActiveAccountsAndDefaultStatusInPayload() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(new AccountFilterDto()).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");
    }

    @Test
    void testGetAccountsForNonexistentRoles() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        RoleListRepresentation roleListDto = new RoleListRepresentation();
        when(rolesService.getRoleById(any())).thenReturn(roleListDto);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setRoles(Set.of("Test_Role_Six", "Test_Role_Five"));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");
    }

    @Test
    void testGetAccountsForNonexistentNames() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setAccountNames(Set.of("Test Account Name3"));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");
    }

    @Test
    void testGetAccountsForNonexistentParentId() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setParentIds(Set.of(PARENT_ID_VALUE_7));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");
    }

    @Test
    void testGetAccountsForNonexistentAccountId() {
        Map<String, BigInteger> roleNameId = new HashMap<>();
        roleNameId.put("Test_Role_One", ROLE_ID_1);
        roleNameId.put("Test_Role_Two", ROLE_ID_2);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation(roleNameId);
        when(rolesService.filterRoles(any(), any(), any(),  Mockito.anyBoolean())).thenReturn(roleListDto);
        AccountFilterDto accountFilterDto = new AccountFilterDto();
        accountFilterDto.setIds(Set.of(ACCOUNT_ID_VALUE));
        when(accountRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        byte[] response = webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/accounts/filter").build())
                .headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(accountFilterDto).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        List<GetAccountApiResponse> accountResponseList = convertJsonToAccountFilterListResponse(response);
        Assertions.assertEquals(0, accountResponseList.size(), "accountResponseList size should be 0");
    }

}
