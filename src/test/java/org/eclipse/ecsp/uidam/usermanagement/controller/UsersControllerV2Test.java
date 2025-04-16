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

package org.eclipse.ecsp.uidam.usermanagement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import io.prometheus.client.CollectorRegistry;
import jakarta.transaction.Transactional;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.RoleScopeMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.ScopesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeValueRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailVerificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV2;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV2.UserAccountsAndRoles;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV2;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV2;
import org.eclipse.ecsp.uidam.usermanagement.utilities.RoleAssociationUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.DESCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_NUMBER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_NUMBER_DEFAULT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_SIZE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_SIZE_DEFAULT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SLASH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TENANT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.enums.Gender.MALE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ADDRESS1_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ADDRESS2_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.BIRTH_DATE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.CITY_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.COUNTRY_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.DEV_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.EMAIL_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.FIRST_NAME_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.INTERVAL_FOR_LAST_PASSWORD_UPDATE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LAST_NAME_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOCALE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.MAX_PASSWORD_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.MIN_PASSWORD_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PASSWORD_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PHONE_NUMBER_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.POSTAL_CODE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_SELF_MNG;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.STATE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_NAME_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UsersControllerV2Test.
 *
 * @author sputhanveett
 *
 */
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { AccountRepository.class, UsersRepository.class })
@AutoConfigureWebTestClient(timeout = "3600000")
@EnableJpaRepositories(basePackages = "org.eclipse.ecsp")
@ComponentScan(basePackages = { "org.eclipse.ecsp" })
@EntityScan("org.eclipse.ecsp")
class UsersControllerV2Test {

    private static final int HOUR_IN_MS = 3600000;

    private static final int INDEX_0 = 0;
    private static final int INDEX_1 = 1;
    private static final int INDEX_2 = 2;
    private static final int INDEX_3 = 3;
    private static final int INDEX_4 = 4;
    private static final int INDEX_5 = 5;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private UsersRepository userRepository;

    @MockBean
    private RolesRepository rolesRepository;

    @MockBean
    private UserAttributeValueRepository userAttributeValueRepository;

    @MockBean
    private UserAttributeRepository userAttributeRepository;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private ApplicationProperties applicationProperties;

    @MockBean
    private RolesService rolesService;

    private String passwordEncoder = "SHA-256";

    /**
     * Init before each test.
     */
    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        CollectorRegistry.defaultRegistry.clear();
        addRolesIntoDb();
    }

    @Transactional
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

    /**
     * addRolesIntoDb.
     *
     */
    @Transactional
    private void addRolesIntoDb() {
        RolesEntity role = new RolesEntity();
        role.setId(ROLE_ID_1);
        role.setName("VEHICLE_OWNER");
        role.setDescription("VEHICLE_OWNER");
        RoleScopeMappingEntity rs = new RoleScopeMappingEntity();
        ScopesEntity s = new ScopesEntity();
        s.setName(SCOPE_SELF_MNG);
        s.setId(ROLE_ID_1);
        rs.setScope(s);
        role.setCreatedBy("Dev_Team");
        role.setUpdatedBy("QA_Team");
        role.setCreateDate(new Timestamp(System.currentTimeMillis()));
        role.setUpdateDate(new Timestamp(System.currentTimeMillis() + HOUR_IN_MS));
        when(rolesRepository.findByNameInAndIsDeleted(any(), anyBoolean())).thenReturn(List.of(role));
        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(role));
        when(userAttributeValueRepository.findAllByUserIdIn(any())).thenReturn(List.of());
        when(userAttributeRepository.findAll()).thenReturn(List.of());
    }

    private static void fillUserRequest(UserRequest userRequest) {
        userRequest.setFirstName(FIRST_NAME_VALUE);
        userRequest.setLastName(LAST_NAME_VALUE);
        userRequest.setCountry(COUNTRY_VALUE);
        userRequest.setState(STATE_VALUE);
        userRequest.setCity(CITY_VALUE);
        userRequest.setAddress1(ADDRESS1_VALUE);
        userRequest.setAddress2(ADDRESS2_VALUE);
        userRequest.setPostalCode(POSTAL_CODE_VALUE);
        userRequest.setPhoneNumber(PHONE_NUMBER_VALUE);
        userRequest.setEmail(EMAIL_VALUE);
        userRequest.setGender(MALE);
        userRequest.setBirthDate(BIRTH_DATE_VALUE);
        userRequest.setLocale(LOCALE_VALUE);
    }

    private static UserDtoV2 createUserPostV2(UserStatus status, String accountName, String roleName) {
        UserDtoV2 userDto = new UserDtoV2();
        fillUserRequest(userDto);
        userDto.setPassword(PASSWORD_VALUE);
        userDto.setUserName(USER_NAME_VALUE);
        userDto.setStatus(status);
        userDto.setAud("k8s-portal");

        UserAccountsAndRoles ac = new UserAccountsAndRoles();
        ac.setAccount(accountName);
        Set<String> roles = new HashSet<>();
        roles.add(roleName);
        ac.setRoles(roles);
        Set<UserAccountsAndRoles> accounts = new HashSet<>();
        accounts.add(ac);
        userDto.setAccounts(accounts);
        return userDto;
    }

    @Test
    void testCreateUserAccountsEmpty() throws IOException {
        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", ROLE_VALUE);
        userDto.setAccounts(new HashSet<UserAccountsAndRoles>());
        try {
            webTestClient.post().uri("/v2/users").headers(http -> {
                http.add("Content-Type", "application/json");
                http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
            }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody().returnResult().getResponseBody();
        } catch (Exception e) {
            //MethodArgumentNotValidException since Accounts is empty.
            Assertions.assertNotNull(e);
        }
    }

    @Test
    void testCreateUserAccountNameEmpty() throws IOException {
        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, " ", ROLE_VALUE);
        userDto.setAccounts(new HashSet<UserAccountsAndRoles>());
        try {
            webTestClient.post().uri("/v2/users").headers(http -> {
                http.add("Content-Type", "application/json");
                http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
            }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody().returnResult().getResponseBody();
        } catch (Exception e) {
            //MethodArgumentNotValidException since Accounts is empty.
            Assertions.assertNotNull(e);
        }
    }

    @Test
    void testCreateUserAccountRolesEmpty() throws IOException {
        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", " ");
        userDto.setAccounts(new HashSet<UserAccountsAndRoles>());
        try {
            webTestClient.post().uri("/v2/users").headers(http -> {
                http.add("Content-Type", "application/json");
                http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
            }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody().returnResult().getResponseBody();
        } catch (Exception e) {
            //MethodArgumentNotValidException since Accounts is empty.
            Assertions.assertNotNull(e);
        }
    }

    private UserEntity mockUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID_VALUE);

        UserAccountRoleMappingEntity u = new UserAccountRoleMappingEntity();
        u.setAccountId(ACCOUNT_ID_VALUE);
        u.setRoleId(ROLE_ID_1);
        u.setUserId(user.getId());

        List<UserAccountRoleMappingEntity> ul = new ArrayList<>();
        ul.add(u);
        user.setAccountRoleMapping(ul);
        user.setUserAddresses(new ArrayList<>());
        return user;
    }

    private UserEntity loggedInUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(LOGGED_IN_USER_ID_VALUE);

        UserAccountRoleMappingEntity u = new UserAccountRoleMappingEntity();
        u.setAccountId(ACCOUNT_ID_VALUE);
        u.setRoleId(ROLE_ID_1);
        u.setUserId(user.getId());

        List<UserAccountRoleMappingEntity> ul = new ArrayList<>();
        ul.add(u);
        user.setAccountRoleMapping(ul);
        user.setUserAddresses(new ArrayList<>());
        return user;
    }

    @Test
    void testCreateUserSuccess() throws IOException, ResourceNotFoundException {
        addAccountIntoDb("TestAccount", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE, null);
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity());
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(loggedInUserEntity());
        Set<RoleCreateResponse> roles = new HashSet<>();
        RoleCreateResponse role = new RoleCreateResponse();
        role.setName("VEHICLE_OWNER");
        role.setId(ROLE_ID_1);
        roles.add(role);
        RoleListRepresentation rolesRepresentation = new RoleListRepresentation(roles);
        when(rolesService.filterRoles(any(HashSet.class), any(Integer.class), any(Integer.class), anyBoolean()))
                .thenReturn(rolesRepresentation);
        when(rolesService.getRoleById(any(HashSet.class))).thenReturn(rolesRepresentation);

        Object[] accountArray = new Object[INDEX_2];
        accountArray[INDEX_0] = ACCOUNT_ID_VALUE;
        accountArray[INDEX_1] = "TestAccount";
        List<Object[]> activeAccounts = new ArrayList<>();
        activeAccounts.add(accountArray);
        when(accountRepository
                .findIdAndNameByStatusAndAccountNameIn(any(AccountStatus.class), anySet())).thenReturn(activeAccounts);
        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", ROLE_VALUE);
        webTestClient.post().uri("/v2/users").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.CREATED).expectBody().returnResult()
                .getResponseBody();
        verify(emailVerificationService, times(1)).resendEmailVerification(any(UserResponseV2.class));

    }

    @Test
    void testCreateUserSuccess_2Accounts() throws IOException {
        addAccountIntoDb("TestAccount", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE_1, null);
        addAccountIntoDb("TestAccount2", AccountStatus.ACTIVE, ROLE_ID_1, ACCOUNT_ID_VALUE, null);
        UserAccountsAndRoles ac = new UserAccountsAndRoles();
        ac.setAccount("TestAccount");
        Set<String> roles = new HashSet<>();
        roles.add(ROLE_VALUE);
        ac.setRoles(roles);
        Set<UserAccountsAndRoles> accounts = new HashSet<>();
        accounts.add(ac);

        ac = new UserAccountsAndRoles();
        ac.setAccount("TestAccount2");
        roles = new HashSet<>();
        roles.add(ROLE_VALUE);
        ac.setRoles(roles);
        accounts.add(ac);

        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", ROLE_VALUE);
        userDto.setAccounts(accounts);
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity());
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(loggedInUserEntity());
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);
        Set<RoleCreateResponse> roles1 = new HashSet<>();
        RoleCreateResponse role = new RoleCreateResponse();
        role.setName("VEHICLE_OWNER");
        role.setId(ROLE_ID_1);
        roles1.add(role);
        RoleListRepresentation rolesRepresentation = new RoleListRepresentation(roles1);
        when(rolesService.filterRoles(any(HashSet.class), any(Integer.class), any(Integer.class), anyBoolean()))
                .thenReturn(rolesRepresentation);
        when(rolesService.getRoleById(any(HashSet.class))).thenReturn(rolesRepresentation);
        Object[] accountArray = new Object[INDEX_2];
        accountArray[INDEX_0] = ACCOUNT_ID_VALUE;
        accountArray[INDEX_1] = "TestAccount2";
        List<Object[]> activeAccounts = new ArrayList<>();
        activeAccounts.add(accountArray);
        accountArray = new Object[INDEX_2];
        accountArray[INDEX_0] = ACCOUNT_ID_VALUE_1;
        accountArray[INDEX_1] = "TestAccount";
        activeAccounts.add(accountArray);
        when(accountRepository
                .findIdAndNameByStatusAndAccountNameIn(any(AccountStatus.class), anySet())).thenReturn(activeAccounts);

        webTestClient.post().uri("/v2/users").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.CREATED).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void testCreateUserAccountInvalid() throws IOException {
        addAccountIntoDb("TestAccount", AccountStatus.DELETED, ROLE_ID_1, ACCOUNT_ID_VALUE, null);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(loggedInUserEntity());
        Set<RoleCreateResponse> roles = new HashSet<>();
        RoleCreateResponse role = new RoleCreateResponse();
        role.setName("VEHICLE_OWNER");
        role.setId(ROLE_ID_1);
        roles.add(role);
        RoleListRepresentation rolesRepresentation = new RoleListRepresentation(roles);
        when(rolesService.filterRoles(any(HashSet.class), any(Integer.class), any(Integer.class), anyBoolean()))
                .thenReturn(rolesRepresentation);
        when(rolesService.getRoleById(any(HashSet.class))).thenReturn(rolesRepresentation);
        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", ROLE_VALUE);
        when(accountRepository
                .findIdAndNameByStatusAndAccountNameIn(any(AccountStatus.class), anySet())).thenReturn(List.of());
        webTestClient.post().uri("/v2/users").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void testCreateUserRoleInvalid() throws IOException {
        addAccountIntoDb("TestAccount", AccountStatus.DELETED, ROLE_ID_1, ACCOUNT_ID_VALUE, null);

        UserAccountsAndRoles ac = new UserAccountsAndRoles();
        ac.setAccount("TestAccount");
        Set<String> roles = new HashSet<>();
        roles.add("NONEXISTENT_ROLE");
        ac.setRoles(roles);
        Set<UserAccountsAndRoles> accounts = new HashSet<>();
        accounts.add(ac);

        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", ROLE_VALUE);
        userDto.setAccounts(accounts);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(loggedInUserEntity());
        Set<RoleCreateResponse> roles1 = new HashSet<>();
        RoleCreateResponse role = new RoleCreateResponse();
        role.setName("VEHICLE_OWNERS");
        role.setId(ROLE_ID_1);
        roles1.add(role);
        RoleListRepresentation rolesRepresentation = new RoleListRepresentation(roles1);
        when(rolesService.filterRoles(any(HashSet.class), any(Integer.class), any(Integer.class), anyBoolean()))
                .thenReturn(rolesRepresentation);
        when(rolesService.getRoleById(any(HashSet.class))).thenReturn(rolesRepresentation);
        Object[] accountArray = new Object[INDEX_2];
        accountArray[INDEX_0] = ACCOUNT_ID_VALUE;
        accountArray[INDEX_1] = "TestAccount";
        List<Object[]> activeAccounts = new ArrayList<>();
        activeAccounts.add(accountArray);
        when(accountRepository
                .findIdAndNameByStatusAndAccountNameIn(any(AccountStatus.class), anySet())).thenReturn(activeAccounts);

        webTestClient.post().uri("/v2/users").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void testCreateUserEmptyRoles() throws IOException {
        addAccountIntoDb("TestAccount", AccountStatus.DELETED, ROLE_ID_1, ACCOUNT_ID_VALUE, null);
        UserAccountsAndRoles ac = new UserAccountsAndRoles();
        ac.setAccount("TestAccount");
        Set<String> roles = new HashSet<>();
        ac.setRoles(roles);
        Set<UserAccountsAndRoles> accounts = new HashSet<>();
        accounts.add(ac);

        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", ROLE_VALUE);
        userDto.setAccounts(accounts);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(loggedInUserEntity());
        Object[] accountArray = new Object[INDEX_2];
        accountArray[INDEX_0] = ACCOUNT_ID_VALUE;
        accountArray[INDEX_1] = "TestAccount";
        List<Object[]> activeAccounts = new ArrayList<>();
        activeAccounts.add(accountArray);
        when(accountRepository
                .findIdAndNameByStatusAndAccountNameIn(any(AccountStatus.class), anySet())).thenReturn(activeAccounts);

        webTestClient.post().uri("/v2/users").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void testCreateUserEmptyAccountName() throws IOException {
        addAccountIntoDb("TestAccount", AccountStatus.DELETED, ROLE_ID_1, ACCOUNT_ID_VALUE, null);
        UserAccountsAndRoles ac = new UserAccountsAndRoles();
        Set<String> roles = new HashSet<>();
        roles.add(ROLE_VALUE);
        ac.setRoles(roles);
        Set<UserAccountsAndRoles> accounts = new HashSet<>();
        accounts.add(ac);

        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", ROLE_VALUE);
        userDto.setAccounts(accounts);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(loggedInUserEntity());

        webTestClient.post().uri("/v2/users").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void testCreateUserNonExistentAccount() throws IOException {
        UserAccountsAndRoles ac = new UserAccountsAndRoles();
        ac.setAccount("NonExistentAccount");
        Set<String> roles = new HashSet<>();
        roles.add(ROLE_VALUE);
        ac.setRoles(roles);
        Set<UserAccountsAndRoles> accounts = new HashSet<>();
        accounts.add(ac);

        UserDtoV2 userDto = createUserPostV2(UserStatus.ACTIVE, "TestAccount", ROLE_VALUE);
        userDto.setAccounts(accounts);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(loggedInUserEntity());
        Set<RoleCreateResponse> roles1 = new HashSet<>();
        RoleCreateResponse role = new RoleCreateResponse();
        role.setName("VEHICLE_OWNER");
        role.setId(ROLE_ID_1);
        roles1.add(role);
        RoleListRepresentation rolesRepresentation = new RoleListRepresentation(roles1);
        when(rolesService.filterRoles(any(HashSet.class), any(Integer.class), any(Integer.class), anyBoolean()))
                .thenReturn(rolesRepresentation);
        when(rolesService.getRoleById(any(HashSet.class))).thenReturn(rolesRepresentation);
        when(accountRepository
                .findIdAndNameByStatusAndAccountNameIn(any(AccountStatus.class), anySet())).thenReturn(List.of());

        webTestClient.post().uri("/v2/users").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).bodyValue(userDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void testGetUserSuccess() throws Exception {
        UserEntity userEntity = mockUserEntity();
        AccountEntity actEntity = addAccountIntoDb("TestAccount", AccountStatus.ACTIVE, ROLE_ID_1,
                userEntity.getAccountRoleMapping().get(0).getAccountId(), null);
        userEntity.getAccountRoleMapping().get(0).setAccountId(actEntity.getId());
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class))).thenReturn(userEntity);
        Set<RoleCreateResponse> roles = new HashSet<>();
        RoleCreateResponse role = new RoleCreateResponse();
        role.setName("VEHICLE_OWNER");
        role.setId(ROLE_ID_1);
        roles.add(role);
        RoleListRepresentation rolesRepresentation = new RoleListRepresentation(roles);
        when(rolesService.filterRoles(any(HashSet.class), any(Integer.class), any(Integer.class), anyBoolean()))
                .thenReturn(rolesRepresentation);
        when(rolesService.getRoleById(any(HashSet.class))).thenReturn(rolesRepresentation);
        when(accountRepository.findById(any())).thenReturn(Optional.of(actEntity));
        webTestClient.get().uri("/v2/users" + SLASH + userEntity.getId()).headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void testGetUserNotFound() throws Exception {
        String userId = "0123456789";
        when(userRepository.findByIdAndStatusNot(new BigInteger(userId), UserStatus.DELETED)).thenReturn(null);
        webTestClient.get().uri("/v2/users" + SLASH + userId).headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void testGetUserInvalidUserId() throws Exception {
        String userId = "shjgakhfsdahy";
        webTestClient.get().uri("/v2/users" + SLASH + userId).headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void getUsers() throws Exception {
        UserEntity userEntity = mockUserEntity();
        AccountEntity actEntity = addAccountIntoDb("TestAccount", AccountStatus.ACTIVE, ROLE_ID_1,
                userEntity.getAccountRoleMapping().get(0).getAccountId(), null);
        userEntity.getAccountRoleMapping().get(0).setAccountId(actEntity.getId());
        List<UserEntity> result = List.of(userEntity);
        Page page = Mockito.mock(Page.class);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(page.getContent()).thenReturn(result);
        when(accountRepository.findById(any())).thenReturn(Optional.of(actEntity));
        Set<RoleCreateResponse> roles = new HashSet<>();
        RoleCreateResponse role = new RoleCreateResponse();
        role.setName("VEHICLE_OWNER");
        role.setId(ROLE_ID_1);
        roles.add(role);
        RoleListRepresentation rolesRepresentation = new RoleListRepresentation(roles);
        when(rolesService.getRoleById(any(HashSet.class))).thenReturn(rolesRepresentation);
        String searchAccountName = "TestAccount";
        UsersGetFilterV2 userV2SearchFilter = createUsersGetFilterV2(searchAccountName, null);
        webTestClient.post().uri("/v2/users/filter").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(TENANT_ID, "tenant1");
            http.add(PAGE_NUMBER, PAGE_NUMBER_DEFAULT);
            http.add(PAGE_SIZE, PAGE_SIZE_DEFAULT);
            http.add(SORT_ORDER, DESCENDING);
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, "1234567890");
        }).bodyValue(userV2SearchFilter).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();
    }

    @Test
    void editUserSuccess() throws Exception {
        List<RolesEntity> commonRolesEntities = List.of(
                RoleAssociationUtilities.addRoles("Bussiness_admin", "BUSSINESS_ADMIN", INDEX_1),
                RoleAssociationUtilities.addRoles("Guest", "GUEST", INDEX_2),
                RoleAssociationUtilities.addRoles("Tenant", "TENANT", INDEX_3),
                RoleAssociationUtilities.addRoles("Admin", "ADMIN", INDEX_4),
                RoleAssociationUtilities.addRoles("Vehicle Owner", "VEHICLE_OWNER", INDEX_5));

        List<RolesEntity> userRoles = new ArrayList<>();
        userRoles.add(commonRolesEntities.get(INDEX_4));
        when(rolesRepository.findByNameInAndIsDeleted(any(HashSet.class), any(Boolean.class))).thenReturn(userRoles);

        List<AccountEntity> accountEntities = List.of(
                RoleAssociationUtilities.addAccount("Ignite_Account", AccountStatus.ACTIVE,
                Set.of(commonRolesEntities.get(INDEX_0).getId(), commonRolesEntities.get(INDEX_2).getId()), INDEX_1),
                RoleAssociationUtilities.addAccount("UIDAM_Account", AccountStatus.ACTIVE,
                Set.of(commonRolesEntities.get(INDEX_0).getId(), commonRolesEntities.get(INDEX_1).getId(),
                       commonRolesEntities.get(INDEX_2).getId()), INDEX_2),
                RoleAssociationUtilities.addAccount("AiLabs_Account", AccountStatus.ACTIVE,
                Set.of(commonRolesEntities.get(INDEX_1).getId(), commonRolesEntities.get(INDEX_3).getId()), INDEX_3),
                RoleAssociationUtilities.addAccount("Analytics_Account", AccountStatus.DELETED,
                Set.of(commonRolesEntities.get(INDEX_0).getId(), commonRolesEntities.get(INDEX_3).getId(),
                       commonRolesEntities.get(INDEX_4).getId()), INDEX_4));

        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
            .thenReturn(List.of(accountEntities.get(INDEX_0)));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(accountEntities.get(INDEX_0)));

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                RoleAssociationUtilities.addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        commonRolesEntities.get(INDEX_1).getId(), new BigInteger("1")));
        userRoleMappingEntityList.add(
                RoleAssociationUtilities.addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        commonRolesEntities.get(INDEX_1).getId(), new BigInteger("2")));

        UserEntity loggedInuser = RoleAssociationUtilities.createUser(
                "Ignite_User", "Ignite_Password", "ignite_admin@harman.com");
        loggedInuser.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuser.setUserAddresses(new ArrayList<>());
        loggedInuser.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesService.getRoleById(anySet()))
            .thenReturn(RoleAssociationUtilities.createRoleListDtoRepresentation(commonRolesEntities, INDEX_4));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuser);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class))).thenReturn(new ArrayList<>());

        UserEntity user = RoleAssociationUtilities.createUser("Ignite_User", "Ignite_Password", "ignite@harman.com");
        user.setId(USER_ID_VALUE);
        user.setUserAddresses(new ArrayList<>());
        user.setAccountRoleMapping(userRoleMappingEntityList);

        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class))).thenReturn(user);
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        String mappingPatch = "[{\"op\":\"add\",\"path\":\"/account/1/roleName\", \"value\":\"VEHICLE_OWNER\"}]";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readValue(mappingPatch, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);

        webTestClient.patch().uri("/v2/users/" + "1").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody().returnResult()
                .getResponseBody();
    }

    /**
     * Return mock user response data.
     *
     * @return UserResponse
     */
    public static UserResponseBase getUserResponse() {
        UserResponseV2 userResponse = new UserResponseV2();
        userResponse.setUserName("johnd");
        userResponse.setStatus(UserStatus.ACTIVE);
        userResponse.setEmail("john.doe@domain.com");
        UserAccountsAndRoles userAccountRole = new UserAccountsAndRoles();
        userAccountRole.setAccount("TestAccount");
        userAccountRole.setRoles(Set.of(ROLE_VALUE));
        userResponse.setAccounts(Set.of(userAccountRole));
        return userResponse;
    }

    /**
     * Create mock user filter request.
     *
     * @return UsersGetFilter
     */
    public static UsersGetFilterV2 createUsersGetFilterV2(String accountNameSearch, String roleNameSearch) {
        UsersGetFilterV2 usersGetFilter = new UsersGetFilterV2();
        usersGetFilter.setAccountNames(Collections.singleton(accountNameSearch));
        usersGetFilter.setIds(Collections.singleton(USER_ID_VALUE));
        usersGetFilter.setUserNames(Collections.singleton(USER_NAME_VALUE));
        usersGetFilter.setRoles(Collections.singleton(ROLE_VALUE));
        usersGetFilter.setFirstNames(Collections.singleton(FIRST_NAME_VALUE));
        usersGetFilter.setLastNames(Collections.singleton(LAST_NAME_VALUE));
        usersGetFilter.setCountries(Collections.singleton(COUNTRY_VALUE));
        usersGetFilter.setStates(Collections.singleton(STATE_VALUE));
        usersGetFilter.setCities(Collections.singleton(CITY_VALUE));
        usersGetFilter.setAddress1(Collections.singleton(ADDRESS1_VALUE));
        usersGetFilter.setAddress2(Collections.singleton(ADDRESS2_VALUE));
        usersGetFilter.setPostalCodes(Collections.singleton(POSTAL_CODE_VALUE));
        usersGetFilter.setDevIds(Collections.singleton(DEV_ID_VALUE));
        usersGetFilter.setEmails(Collections.singleton(EMAIL_VALUE));
        usersGetFilter.setGender(Collections.singleton(MALE));
        usersGetFilter.setLocales(Collections.singleton(LOCALE_VALUE));
        usersGetFilter.setPhoneNumbers(Collections.singleton((PHONE_NUMBER_VALUE)));
        return usersGetFilter;
    }

}
