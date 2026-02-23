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

package org.eclipse.ecsp.uidam.usermanagement.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.common.metrics.UidamMetricsService;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService.ValidationResult;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.cache.CacheTokenService;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.dao.UserManagementDao;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.mapper.UserMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.CloudProfilesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.EmailVerificationRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.PasswordHistoryRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeValueRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserEventRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserRecoverySecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.impl.UsersServiceImpl;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV2;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV2.UserAccountsAndRoles;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV2;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASCENDING;
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
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.MAX_PASSWORD_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.MIN_PASSWORD_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PASSWORD_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PHONE_NUMBER_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.POSTAL_CODE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_4;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_5;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_SELF_MNG;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.STATE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_NAME_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UsersServiceImpl.class, JacksonAutoConfiguration.class})
@MockBean(JpaMetamodelMappingContext.class)
class UsersServiceV2Test {
    @Autowired
    private UsersService usersService;
    @MockBean
    private RolesService rolesService;
    @MockBean
    private UsersRepository userRepository;
    @MockBean
    private RolesRepository rolesRepository;
    @MockBean
    private AccountRepository accountRepository;
    @MockBean
    private ClientRegistration clientRegistrationService;
    @MockBean
    private TenantConfigurationService tenantConfigurationService;
    @MockBean
    private UserManagementTenantProperties tenantProperties;
    @MockBean
    CacheTokenService cacheTokenService;
    @MockBean
    private UserAttributeRepository userAttributeRepository;
    @MockBean
    private UserAttributeValueRepository userAttributeValueRepository;
    @MockBean
    private UserEventRepository userEventRepository;
    @MockBean
    private EntityManager entityManager;
    @MockBean
    UserRecoverySecretRepository userRecoverySecretRepository;
    @MockBean
    private EntityManagerFactory entityManagerFactory;
    @MockBean
    private UserManagementDao userManagementDao;
    @MockBean
    private AuthorizationServerClient authorizationServerClient;
    @MockBean
    private EmailNotificationService emailNotificationService;
    @MockBean
    private CloudProfilesRepository cloudProfilesRepository;
    @MockBean
    private EmailVerificationRepository emailVerificationRepository;
    @MockBean
    private PasswordHistoryRepository passwordHistoryRepository;

    @MockBean
    PasswordValidationService passwordValidationService;
    
    @MockBean
    PasswordPolicyRepository passwordPolicyRepository;

    @MockBean
    UidamMetricsService uidamMetricsService;

    @MockBean
    org.eclipse.ecsp.uidam.usermanagement.utilities.UserAuditHelper userAuditHelper;

    private String passwordEncoder = "SHA-256";

    private static final long TEST_ROLE_ID_1 = 1L;
    private static final long TEST_ROLE_ID_2 = 2L;
    private static final String SCOPE_ID = "1";
    private static final int ONE_HUNDRED_INT = 100;
    private static final int TWO_THOUSAND_INT = 2000;
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int THREE = 3;
    private static final int INDEX_0 = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;

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

    private static UserDtoV2 createUserPostV2(UserStatus status) {
        UserDtoV2 userDto = new UserDtoV2();
        fillUserRequest(userDto);
        userDto.setPassword(PASSWORD_VALUE);
        userDto.setUserName(USER_NAME_VALUE);
        userDto.setStatus(status);
        userDto.setAud("k8s-portal");

        UserAccountsAndRoles ac = new UserAccountsAndRoles();
        ac.setAccount("TestAccount");
        Set<String> roles = new HashSet<>();
        roles.add(ROLE_2);
        ac.setRoles(roles);
        Set<UserAccountsAndRoles> accounts = new HashSet<>();
        accounts.add(ac);
        userDto.setAccounts(accounts);
        return userDto;
    }

    static UserAccountRoleMappingEntity createUserAccountRoleMappingEntity(BigInteger roleId, BigInteger accountId) {
        return new UserAccountRoleMappingEntity(roleId, null, accountId, "Test Admin");
    }

    private static RoleListRepresentation createRoleListDtoRepresentation() {
        Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        scopeDto.setId(SCOPE_ID);
        RoleCreateResponse roleDto = new RoleCreateResponse();
        roleDto.setName(ROLE_2);
        roleDto.setId(ROLE_ID_2);
        roleDto.setScopes(Collections.singletonList(scopeDto));
        RoleCreateResponse roleDto1 = new RoleCreateResponse();
        roleDto1.setName(ROLE_VALUE);
        roleDto1.setId(ROLE_ID_4);
        roleDto1.setScopes(Collections.singletonList(scopeDto));

        Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        roleDtoList.add(roleDto);
        roleDtoList.add(roleDto1);

        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }

    private RegisteredClientDetails isClientAllowedToManageUsersResponse() {
        RegisteredClientDetails rc = new RegisteredClientDetails();
        rc.setClientId("client1");
        rc.setClientName("name");
        List<String> redirectUrls = new ArrayList<>();
        redirectUrls.add("http://login.com/test");
        rc.setRedirectUris(redirectUrls);
        rc.setClientSecret("secret23");
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        rc.setAuthorizationGrantTypes(grantTypes);
        rc.setCreatedBy("test");
        rc.setAuthorizationCodeValidity(ONE_HUNDRED_INT);
        rc.setRefreshTokenValidity(ONE_HUNDRED_INT);
        Set<String> scopes = new HashSet<>();
        scopes.add(LocalizationKey.MANAGE_USERS_SCOPE);
        rc.setScopes(scopes);
        rc.setAccessTokenValidity(TWO_THOUSAND_INT);
        rc.setAdditionalInformation("abc");
        List<String> authMethods = new ArrayList<>();
        authMethods.add("client_secret_basic");
        rc.setClientAuthenticationMethods(authMethods);
        rc.setAdditionalInformation("abc");
        rc.setRequireAuthorizationConsent(false);
        rc.setStatus("approved");
        return rc;
    }

    @Test
    void testAddUserSuccessV2() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV2 userPost = createUserPostV2(UserStatus.ACTIVE);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_4, ACCOUNT_ID_VALUE));
        userEntity.setAccountRoleMapping(l);

        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(rolesService.getRoleById(anySet())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowedToManageUsersResponse()));
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getPasswordEncoder()).thenReturn(passwordEncoder);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);

        Object[] accountArray = new Object[TWO];
        accountArray[ZERO] = ACCOUNT_ID_VALUE_1;
        accountArray[ONE] = "TestAccount";
        List<Object[]> activeAccounts = new ArrayList<>();
        activeAccounts.add(accountArray);

        when(accountRepository.findIdAndNameByStatusAndAccountNameIn(any(AccountStatus.class), any(HashSet.class)))
                .thenReturn(activeAccounts);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));

        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_5);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        List<RolesEntity> roleEntityList = new ArrayList<>();
        roleEntityList.add(role);
        when(rolesRepository.findByIdIn(any(Set.class))).thenReturn(roleEntityList);
        when(passwordValidationService.validatePassword(anyString(), anyString()))
                .thenReturn(new ValidationResult(true, null));
        UserResponseV2 receivedResponse = (UserResponseV2) usersService.addUser(userPost, null, false);
        UserResponseV2 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV2(userEntity);
        assertEquals(userResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(userResponse.getEmail(), receivedResponse.getEmail());
    }

    @Test
    void  getUserRolesFromUserResponseTest() {
        //UserResponseV1 test
        UserResponseV1 userRespV1 = new UserResponseV1();
        userRespV1.setFirstName("User1");
        userRespV1.setEmail("user1@gmail.com");
        userRespV1.setRoles(Set.of("role1", "role2"));
        Set<String> roles = ((UsersServiceImpl) usersService).getUserRolesFromUserResponse(
            (UserResponseBase) userRespV1);
        Assertions.assertEquals(TWO, roles.size());

        //UserResponseV2 test
        UserResponseV2 userRespV2 = new UserResponseV2();
        userRespV2.setFirstName("User1");
        userRespV2.setEmail("user1@gmail.com");
        UserAccountsAndRoles uar = new UserAccountsAndRoles();
        uar.setAccount("account 1");
        uar.setRoles(Set.of("role1", "role2", "role3"));
        userRespV2.setAccounts(Set.of(uar));
        roles = ((UsersServiceImpl) usersService).getUserRolesFromUserResponse(
            (UserResponseBase) userRespV2);
        Assertions.assertEquals(THREE, roles.size());

    }

    @Test
    void testGetUserSuccessV2() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV2 userPost = createUserPostV2(UserStatus.ACTIVE);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_4, ACCOUNT_ID_VALUE));
        userEntity.setAccountRoleMapping(l);

        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
            .thenReturn(userEntity);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));

        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.getRoleById(anySet())).thenReturn(roleListDto);

        UUID userId = UUID.randomUUID();
        UserResponseV2 receivedResponse = (UserResponseV2) usersService.getUser(USER_ID_VALUE_2, "v2");
        assertEquals(receivedResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(receivedResponse.getEmail(), receivedResponse.getEmail());
        assertEquals(ONE, receivedResponse.getAccounts().size());
        assertEquals("TestAccount", receivedResponse.getAccounts().stream().findFirst().get().getAccount());
        assertEquals(ONE, receivedResponse.getAccounts().stream().findFirst().get().getRoles().size());
    }

    @Test
    void getUsersSuccess() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        Page page = Mockito.mock(Page.class);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));
        AccountEntity a1 = new AccountEntity();
        String accountName1 = "TestAccount1";
        a1.setAccountName(accountName1);
        a1.setId(userEntity.getAccountRoleMapping().get(INDEX_0).getAccountId());
        AccountEntity a2 = new AccountEntity();
        String accountName2 = "TestAccount2";
        a2.setAccountName(accountName2);
        a2.setId(userEntity.getAccountRoleMapping().get(1).getAccountId());
        List<AccountEntity> activeAccounts = new ArrayList<>();
        activeAccounts.add(a1);
        activeAccounts.add(a2);

        when(accountRepository.findAllByStatusAndAccountNameIn(any(AccountStatus.class), any(HashSet.class)))
                .thenReturn(activeAccounts);
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a1)).thenReturn(Optional.of(a2));
        UsersGetFilterV2 usersGetFilter = createUsersGetFilter();
        List<UserResponseV2> userResponses = usersService.getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE,
                UsersGetFilterBase.UserGetFilterEnum.ROLES.getField(), ASCENDING, false, null).stream()
                .map(UserResponseV2.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
        List<UserAccountsAndRoles> accountsAndRoles = new ArrayList<>(userResponses.get(INDEX_0).getAccounts());
        assertTrue(accountsAndRoles.stream().anyMatch(obj -> accountName1.equals(obj.getAccount())));
        assertTrue(accountsAndRoles.stream().anyMatch(obj -> accountName2.equals(obj.getAccount())));
        assertTrue(accountsAndRoles.stream().map(obj -> obj.getRoles()).flatMap(Set::stream)
                .anyMatch(role -> ROLE_VALUE.equals(role)));
        assertTrue(accountsAndRoles.stream().map(obj -> obj.getRoles()).flatMap(Set::stream)
                .anyMatch(role -> ROLE_2.equals(role)));
    }

    public static UserEntity createUserEntity(UserStatus status) {
        UserDtoV2 userDto = createUserPostV2(status);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userDto);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_4, ACCOUNT_ID_VALUE));
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2, ACCOUNT_ID_VALUE_1));
        userEntity.setAccountRoleMapping(l);
        return userEntity;
    }

    public static UsersGetFilterV2 createUsersGetFilter() {
        UsersGetFilterV2 usersGetFilter = new UsersGetFilterV2();
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
