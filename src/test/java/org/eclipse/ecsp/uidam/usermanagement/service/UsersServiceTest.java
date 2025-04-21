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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import io.prometheus.client.CollectorRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.BaseResponseFromAuthorization;
import org.eclipse.ecsp.uidam.usermanagement.cache.CacheTokenService;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.dao.UserManagementDao;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAddressEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeValueEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEvents;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.InActiveUserException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserAccountRoleMappingException;
import org.eclipse.ecsp.uidam.usermanagement.mapper.UserMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.CloudProfilesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.EmailVerificationRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeValueRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserEventRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserRecoverySecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.impl.UsersServiceImpl;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.FederatedUserDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserChangeStatusRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV1;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserMetaDataRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersDeleteFilter;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserDetailsResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserMetaDataResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV2;
import org.eclipse.ecsp.uidam.usermanagement.utilities.PatchMap;
import org.eclipse.ecsp.uidam.usermanagement.utilities.RoleAssociationUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CAPTCHA_REQUIRED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_INPUT_ROLE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.ACTION_FORBIDDEN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.FIELD_DATA_IS_INVALID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.FIELD_IS_UNIQUE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.FIELD_NOT_FOUND;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_PASS_CANNOT_CONTAIN_USERNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_USERNAME_CANNOT_START_WITH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_USERNAME_PATTERN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.USER_IS_BLOCKED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.USER_NOT_VERIFIED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.USER_ROLES_NOT_FOUND;
import static org.eclipse.ecsp.uidam.usermanagement.enums.Gender.MALE;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.STATUS;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ADDRESS1_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ADDRESS2_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ATTR_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ATTR_ID_VALUE_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ATTR_ID_VALUE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.BIRTH_DATE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.BIRTH_DATE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.CITY;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.CITY_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.COUNTRY_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.DEV_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.EMAIL_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.FEDERATED_PREFIX;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.FIRST_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.FIRST_NAME_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.GENDER;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.INTERVAL_FOR_LAST_PASSWORD_UPDATE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.INVALID_ROLE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LAST_NAME_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOCALE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.MAX_PASSWORD_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.MIN_PASSWORD_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.MODIFIED_CITY;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.MODIFIED_FIRST_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PASSWORD_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PHONE_NUMBER_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.POSTAL_CODE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_SELF_MNG;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.STATE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE_3;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_NAME_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Utilities.asJsonString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UsersServiceImpl.class, JacksonAutoConfiguration.class})
@MockBean(JpaMetamodelMappingContext.class)
class UsersServiceTest {

    @Autowired
    private UsersService usersService;
    @MockBean
    private UsersRepository userRepository;
    @MockBean
    private AccountRepository accountRepository;
    @MockBean
    private UserAttributeRepository userAttributeRepository;
    @MockBean
    private UserAttributeValueRepository userAttributeValueRepository;
    @MockBean
    private UserEventRepository userEventRepository;
    @MockBean
    private ApplicationProperties applicationProperties;
    @MockBean
    private EntityManagerFactory entityManagerFactory;
    @MockBean
    private UserManagementDao userManagementDao;
    @MockBean
    private EntityManager entityManager;
    @MockBean
    private UserRecoverySecretRepository userRecoverySecretRepository;
    @MockBean
    private EmailNotificationService emailNotificationService;
    @MockBean
    private CacheTokenService cacheTokenService;
    @MockBean
    private AuthorizationServerClient authorizationServerClient;
    @MockBean
    protected RestTemplate restTemplate;
    @MockBean
    RolesRepository rolesRepository;
    @MockBean
    private RolesService rolesService;
    @MockBean
    ClientRegistration clientRegistrationService;
    @MockBean
    CloudProfilesRepository cloudProfilesRepository;
    @MockBean
    EmailVerificationRepository emailVerificationRepository;

    private String passwordEncoder = "SHA-256";

    private static final int INDEX_0 = 0;
    private static final int INDEX_1 = 1;
    private static final int INDEX_2 = 2;
    private static final int INDEX_3 = 3;
    private static final int INDEX_4 = 4;
    private static final int INDEX_5 = 5;
    private static final String ONE = "1";
    private static final String TWO = "2";
    private static final int CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES_VALUE = 3;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int ADDITIONAL_ATTRIBUTE_SIZE = 3;
    private static final int USER_META_RESPONSE_SIZE_1 = 30;
    private static final int MANDATORY_ATTRIBUTE_COUNT_1 = 12;
    private static final int USER_META_RESPONSE_SIZE_2 = 27;
    private static final int MANDATORY_ATTRIBUTE_COUNT_2 = 11;
    private static final String SCOPE_ID = "1";
    private static final int ONE_HUNDRED_INT = 100;
    private static final int TWO_THOUSAND_INT = 2000;
    private static final String API_VERSION_1 = "v1";
    private static final String API_VERSION_2 = "v2";

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    public static UserDtoV1 createUserPost(UserStatus status) {
        UserDtoV1 userDto = new UserDtoV1();
        fillUserRequest(userDto);
        userDto.setPassword(PASSWORD_VALUE);
        userDto.setUserName(USER_NAME_VALUE);
        userDto.setRoles(Set.of(ROLE_2, ROLE_VALUE));
        userDto.setStatus(status);
        userDto.setAud("k8s-portal");
        return userDto;
    }

    public static UserDtoV1 createExternalUserPost(UserStatus status) {
        UserDtoV1 userDto = new UserDtoV1();
        fillUserRequest(userDto);
        userDto.setUserName(USER_NAME_VALUE);
        userDto.setRoles(Set.of(ROLE_VALUE));
        userDto.setStatus(status);
        userDto.setAud("k8s-portal");
        userDto.setIsExternalUser(true);
        return userDto;
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

    private static RoleListRepresentation createOneRoleListDtoRepresentation() {
        Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        scopeDto.setId(SCOPE_ID);
        RoleCreateResponse roleDto1 = new RoleCreateResponse();
        roleDto1.setName(ROLE_VALUE);
        roleDto1.setId(ROLE_ID_2);
        roleDto1.setScopes(Collections.singletonList(scopeDto));

        Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        roleDtoList.add(roleDto1);

        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }

    public static RoleListRepresentation createRoleListDtoRepresentation() {
        Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        scopeDto.setId(SCOPE_ID);
        RoleCreateResponse roleDto = new RoleCreateResponse();
        roleDto.setName(ROLE_2);
        roleDto.setId(ROLE_ID_1);
        roleDto.setScopes(Collections.singletonList(scopeDto));
        RoleCreateResponse roleDto1 = new RoleCreateResponse();
        roleDto1.setName(ROLE_VALUE);
        roleDto1.setId(ROLE_ID_2);
        roleDto1.setScopes(Collections.singletonList(scopeDto));

        Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        roleDtoList.add(roleDto);
        roleDtoList.add(roleDto1);

        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }

    public static UserEntity createUserEntity(UserStatus status) {
        UserDtoV1 userDto = createUserPost(status);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userDto);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_1));
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        return userEntity;
    }

    static UserAccountRoleMappingEntity createUserAccountRoleMappingEntity(BigInteger roleId) {
        return new UserAccountRoleMappingEntity(roleId, null, ACCOUNT_ID_VALUE, "Test Admin");
    }

    public static UsersGetFilterV1 createUsersGetFilter() {
        UsersGetFilterV1 usersGetFilter = new UsersGetFilterV1();
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

    public static PatchMap createPatchMap() {
        PatchMap patchMap = new PatchMap();
        patchMap.put(FIRST_NAME, MODIFIED_FIRST_NAME);
        patchMap.put(CITY, MODIFIED_CITY);
        patchMap.put(BIRTH_DATE, String.valueOf(LocalDate.now()));
        patchMap.put(GENDER, "FEMALE");
        patchMap.put(STATUS.getField(), "PENDING");
        return patchMap;
    }

    private UserEntity mockUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID_VALUE);

        UserAccountRoleMappingEntity u = new UserAccountRoleMappingEntity();
        u.setAccountId(ACCOUNT_ID_VALUE);
        u.setRoleId(ROLE_ID_2);
        u.setUserId(user.getId());

        List<UserAccountRoleMappingEntity> ul = new ArrayList<>();
        ul.add(u);
        user.setAccountRoleMapping(ul);
        return user;
    }

    @Test
    void testAddUserUsernameInPassword() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setPassword(USER_NAME_VALUE + "@11");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        try {
            usersService.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(INVALID_INPUT_PASS_CANNOT_CONTAIN_USERNAME, e.getKey());
            return;
        }
        fail("Expected ApplicationRuntimeException");
    }

    @Test
    void testInvalidUserName() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setUserName("$hakjsgka");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        try {
            usersService.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(INVALID_INPUT_USERNAME_PATTERN, e.getKey());
            return;
        }
        fail("Expected ApplicationRuntimeException");
    }

    /*
    description = "Test non-federated username start with \"federated_\""
     */
    @Test
    void testAddUserStartsWithIllegalPrefix() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setUserName(FEDERATED_PREFIX + USER_NAME_VALUE);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        try {
            usersService.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(INVALID_INPUT_USERNAME_CANNOT_START_WITH, e.getKey());
            assertEquals(FEDERATED_PREFIX, e.getParameters()[0]);
            return;
        }

        fail("Expected ApplicationRuntimeException");
    }

    /*
    description = "Test non-federated username does not start with \"federated_\""
     */
    @Test
    void testAddUserStartsWithIllegalPrefix1() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setUserName(FEDERATED_PREFIX);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        try {
            usersService.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(INVALID_INPUT_USERNAME_CANNOT_START_WITH, e.getKey());
            assertEquals(FEDERATED_PREFIX, e.getParameters()[0]);
            return;
        }

        fail("Expected ApplicationRuntimeException");
    }

    @Test
    void testAddUserAlreadyExists() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());

        try {
            usersService.addUser(userPost, null, false);
        } catch (DataIntegrityViolationException e) {
            e.getMessage().contains("duplicate key value violates unique constraint");
            return;
        }

        fail("Expected ApplicationRuntimeException");
    }

    @Test
    void testAddUserDefaultAccountNotExists() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(userPost.getRoles());
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.empty());

        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());

        assertThrows(ResourceNotFoundException.class, () -> usersService.addUser(userPost, null, false));
    }

    @Test
    void testAddUserSuccess() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(userPost.getRoles());
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());

        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addUser(userPost, null, false);
        assertEquals(userResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(userResponse.getEmail(), receivedResponse.getEmail());
        assertEquals(userResponse.getRoles(), receivedResponse.getRoles());
    }

    @Test
    void testAddUserSuccessExternalUser() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setIsExternalUser(true);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(userPost.getRoles());
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());

        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addUser(userPost, null, false);
        assertEquals(userResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(userResponse.getEmail(), receivedResponse.getEmail());
        assertEquals(userResponse.getRoles(), receivedResponse.getRoles());
    }

    @Test
    void testAddUserSuccessSessionUserIdNotNull() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setIsExternalUser(false);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(userPost.getRoles());
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        UsersService usersService1 = Mockito.spy(usersService);
        Mockito.doReturn(userResponse).when(usersService1)
            .getUser(any(BigInteger.class), eq(API_VERSION_1));
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        UserResponseV1 receivedResponse = (UserResponseV1) usersService1.addUser(userPost, USER_ID_VALUE, false);
        assertEquals(userResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(userResponse.getEmail(), receivedResponse.getEmail());
        assertEquals(userResponse.getRoles(), receivedResponse.getRoles());
    }

    @Test
    void testAddUserClientValidationFail() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setIsExternalUser(false);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);;
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(userPost.getRoles());
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);

        UsersService usersService1 = Mockito.spy(usersService);
        Mockito.doReturn(userResponse).when(usersService1)
            .getUser(any(BigInteger.class), eq(API_VERSION_1));
        try {
            usersService1.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException applicationRuntimeException) {
            assertEquals(ACTION_FORBIDDEN, applicationRuntimeException.getKey());
        }
    }

    @Test
    void testAddUserWhenRoleDoesNotExistFailure() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setRoles(Collections.singleton("INVALID_ROLE"));
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);

        try {
            usersService.addUser(userPost, USER_ID_VALUE, false);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(USER_ROLES_NOT_FOUND, exception.getKey());
            return;
        }
    }

    @Test
    void testAddUserExceptionWhenInvalidClient() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        try {
            usersService.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(ACTION_FORBIDDEN, exception.getKey());
            return;
        }
    }

    @Test
    void testAddUserWhenMandatoryAdditionalAttributeMissing()
        throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        try {
            usersService.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(MISSING_MANDATORY_PARAMETERS, exception.getKey());
            return;
        }
    }

    @Test
    void testAddUserWhenDuplicateAdditionalAttributeData() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setAdditionalAttributes("mandatoryAttribute", "xyz");
        userPost.setAdditionalAttributes("uniqueAttribute", "hello");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity());

        try {
            usersService.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(FIELD_IS_UNIQUE, exception.getKey());
            return;
        }
    }

    @Test
    void testAddUserWhenAdditionalAttributeNotExists() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setAdditionalAttributes("mandatoryAttribute", "xyz");
        userPost.setAdditionalAttributes("unsavedAttribute", "hello");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity());
        try {
            usersService.addUser(userPost, null, false);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(FIELD_NOT_FOUND, exception.getKey());
            return;
        }
    }

    @Test
    void testAddUserWhenAdditionalAttributeDataInvalid() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        userPost.setAdditionalAttributes("mandatoryAttribute", "123");
        userPost.setAdditionalAttributes("uniqueAttribute", "hello");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        userAttributeEntities.get(INDEX_0).setRegex("[A-Z]+");
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(Collections.EMPTY_LIST);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity());
        try {
            usersService.addUser(userPost, USER_ID_VALUE, false);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus(), "Status should be 400 because it's BL validation");
            assertEquals(FIELD_DATA_IS_INVALID, exception.getKey());
            return;
        }
    }

    @Test
    void testAddUserWithAdditionalAttributeDataSuccess() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(userPost.getRoles());
        userPost.setAdditionalAttributes("mandatoryAttribute", "hello");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(Collections.EMPTY_LIST);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(userAttributeValueRepository
            .saveAll(anyList())).thenReturn(createUserAttributeValueData());
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addUser(userPost, USER_ID_VALUE, false);
        assertEquals("hello", receivedResponse.getAdditionalAttributes().get("mandatoryAttribute"));
    }

    @Test
    void testAddUserWhenAdditionalAttributeDataNotString() throws NoSuchAlgorithmException, ResourceNotFoundException {
        UserDtoV1 userPost = createUserPost(UserStatus.ACTIVE);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(userPost.getRoles());
        Map attr1 = Collections.singletonMap("attr1", "data");
        List attr2 = Collections.singletonList("data2");
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        userPost.setAdditionalAttributes("mandatoryAttribute", attr1);
        userPost.setAdditionalAttributes("uniqueAttribute", attr2);
        userPost.setAdditionalAttributes("readOnlyAttribute", timestamp);

        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        userAttributeEntities.get(INDEX_0).setTypes("jsonb");
        userAttributeEntities.get(INDEX_1).setTypes("_abc");
        userAttributeEntities.get(INDEX_2).setTypes("timestamp");
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        userAttributeValueEntities.get(INDEX_0).setValue(asJsonString(attr1));
        userAttributeValueEntities.get(INDEX_1).setValue(StringUtils.join(attr2, ","));
        userAttributeValueEntities.get(INDEX_2).setValue(String.valueOf(timestamp));
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(userAttributeValueRepository
            .saveAll(anyList())).thenReturn(userAttributeValueEntities);

        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(clientRegistrationService.getRegisteredClient(anyString(), anyString()))
            .thenReturn(Optional.of(isClientAllowToManageUsersResponse()));
        when(applicationProperties.getPasswordEncoder()).thenReturn(passwordEncoder);
        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(Collections.EMPTY_LIST);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);

        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        when(applicationProperties.getMaxPasswordLength()).thenReturn(MAX_PASSWORD_LENGTH);
        when(applicationProperties.getMinPasswordLength()).thenReturn(MIN_PASSWORD_LENGTH);
        when(applicationProperties.getPasswordUpdateTimeInterval()).thenReturn(INTERVAL_FOR_LAST_PASSWORD_UPDATE);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addUser(userPost, null, false);
        assertEquals(asJsonString(attr1), asJsonString(
            receivedResponse.getAdditionalAttributes().get("mandatoryAttribute")));
    }

    @Test
    void getUserFailure() {
        when(userRepository.findById(any(BigInteger.class)))
            .thenReturn(Optional.empty());
        try {
            usersService.getUser(USER_ID_VALUE, API_VERSION_1);
        } catch (ResourceNotFoundException exception) {
            assertEquals("User not found for userId: " + USER_ID_VALUE, exception.getMessage());
            return;
        }

        fail("Expected ApplicationRuntimeException");
    }

    @Test
    void getUserSuccess() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
            .thenReturn(userEntity);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        UserResponseV1 result = (UserResponseV1) usersService.getUser(USER_ID_VALUE, API_VERSION_1);
        assertEquals(result.getUserName(), userEntity.getUserName());
        assertEquals(result.getEmail(), userEntity.getEmail());
        Set<String> roleList = result.getRoles();
        assertEquals(INDEX_2, roleList.size());
        assertTrue(roleList.containsAll(Arrays.asList(ROLE_VALUE, ROLE_2)));
    }

    @Test
    void getUserByUserNameSuccess() throws ResourceNotFoundException, InActiveUserException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);

        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(any(String.class), any(UserStatus.class)))
            .thenReturn(userEntity);

        UserEvents userEvent = new UserEvents();
        userEvent.setEventGeneratedAt(Instant.now());
        userEvent.setEventStatus("Failure");
        userEvent.setEventType("Login_Attempt");
        userEvent.setEventMessage("Bad Credentials");
        UserEvents userEventSuccess = new UserEvents();
        userEventSuccess.setEventGeneratedAt(Instant.now());
        userEventSuccess.setEventStatus("Success");
        userEventSuccess.setEventType("Login_Attempt");
        userEventSuccess.setEventMessage("Login Success");
        List<UserEvents> userEventEntity = new ArrayList<>();
        userEventEntity.add(userEvent);
        userEventEntity.add(userEventSuccess);
        UserAttributeEntity userAttributeEntity = Mockito.mock(UserAttributeEntity.class);
        UserAttributeValueEntity userAttributeValueEntity = Mockito.mock(UserAttributeValueEntity.class);
        when(userAttributeRepository.findByName(anyString())).thenReturn(userAttributeEntity);
        when(userAttributeEntity.getId()).thenReturn(ATTR_ID_VALUE_2);
        when(userAttributeValueRepository.findByUserIdAndAttributeId(any(BigInteger.class),
            BigInteger.valueOf(anyInt())))
            .thenReturn(userAttributeValueEntity);
        when(userAttributeValueEntity.getValue()).thenReturn("true");
        when(applicationProperties.getMaxAllowedLoginAttempts()).thenReturn("3");
        when(userEventRepository.findUserEventsByUserIdAndEventType(any(BigInteger.class), any(String.class),
            any(Integer.class))).thenReturn(userEventEntity);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(applicationProperties.getCaptchaEnforceAfterNoOfFailures())
            .thenReturn(CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES_VALUE);
        UserDetailsResponse result = usersService.getUserByUserName(USER_NAME_VALUE);
        assertEquals(userEntity.getUserName(), result.getUserName());
        assertEquals(String.valueOf(userEntity.getId()), result.getId());
        assertEquals(CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES_VALUE,
            result.getCaptcha().get(CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES));
    }

    @Test
    void getUserByUserNameNullCaptchaReturnedFromDb() throws ResourceNotFoundException, InActiveUserException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);

        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(any(String.class), any(UserStatus.class)))
            .thenReturn(userEntity);

        UserEvents userEvent = new UserEvents();
        userEvent.setEventGeneratedAt(Instant.now());
        userEvent.setEventStatus("Failure");
        userEvent.setEventType("Login_Attempt");
        userEvent.setEventMessage("Bad Credentials");
        UserEvents userEventSuccess = new UserEvents();
        userEventSuccess.setEventGeneratedAt(Instant.now());
        userEventSuccess.setEventStatus("Success");
        userEventSuccess.setEventType("Login_Attempt");
        userEventSuccess.setEventMessage("Login Success");
        List<UserEvents> userEventEntity = new ArrayList<>();
        userEventEntity.add(userEvent);
        userEventEntity.add(userEventSuccess);
        UserAttributeEntity userAttributeEntity = Mockito.mock(UserAttributeEntity.class);
        when(userAttributeRepository.findByName(anyString())).thenReturn(userAttributeEntity);
        when(userAttributeEntity.getId()).thenReturn(ATTR_ID_VALUE_2);
        when(userAttributeValueRepository.findByUserIdAndAttributeId(any(BigInteger.class),
            BigInteger.valueOf(anyInt())))
            .thenReturn(null);
        when(applicationProperties.getMaxAllowedLoginAttempts()).thenReturn("3");
        when(userEventRepository.findUserEventsByUserIdAndEventType(any(BigInteger.class), any(String.class),
            any(Integer.class))).thenReturn(userEventEntity);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(applicationProperties.getCaptchaEnforceAfterNoOfFailures()).thenReturn(null);
        UserDetailsResponse result = usersService.getUserByUserName(USER_NAME_VALUE);
        assertEquals(userEntity.getUserName(), result.getUserName());
        assertEquals(String.valueOf(userEntity.getId()), result.getId());
        assertEquals(null, result.getCaptcha().get(CAPTCHA_REQUIRED));
        assertEquals(null, result.getCaptcha().get(CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES));
    }

    @Test
    void getUserByUserNameCaptchaAttributeNotPresentInDb() throws ResourceNotFoundException, InActiveUserException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);

        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(any(String.class), any(UserStatus.class)))
            .thenReturn(userEntity);

        UserEvents userEvent = new UserEvents();
        userEvent.setEventGeneratedAt(Instant.now());
        userEvent.setEventStatus("Failure");
        userEvent.setEventType("Login_Attempt");
        userEvent.setEventMessage("Bad Credentials");
        UserEvents userEventSuccess = new UserEvents();
        userEventSuccess.setEventGeneratedAt(Instant.now());
        userEventSuccess.setEventStatus("Success");
        userEventSuccess.setEventType("Login_Attempt");
        userEventSuccess.setEventMessage("Login Success");

        List<UserEvents> userEventEntity = new ArrayList<>();
        userEventEntity.add(userEvent);
        userEventEntity.add(userEventSuccess);
        when(userAttributeRepository.findByName(anyString())).thenReturn(null);
        when(userAttributeValueRepository.findByUserIdAndAttributeId(any(BigInteger.class),
            BigInteger.valueOf(anyInt())))
            .thenReturn(null);
        when(applicationProperties.getMaxAllowedLoginAttempts()).thenReturn("3");
        when(userEventRepository.findUserEventsByUserIdAndEventType(any(BigInteger.class), any(String.class),
            any(Integer.class))).thenReturn(userEventEntity);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(applicationProperties.getCaptchaEnforceAfterNoOfFailures()).thenReturn(null);
        UserDetailsResponse result = usersService.getUserByUserName(USER_NAME_VALUE);
        assertEquals(userEntity.getUserName(), result.getUserName());
        assertEquals(String.valueOf(userEntity.getId()), result.getId());
        assertEquals(null, result.getCaptcha().get(CAPTCHA_REQUIRED));
        assertEquals(null, result.getCaptcha().get(CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES));
    }

    @Test
    void getUserByUserNameResourceNotFound() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.PENDING);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(any(String.class), any(UserStatus.class)))
            .thenReturn(userEntity);
        try {
            usersService.getUserByUserName(USER_NAME_VALUE);
        } catch (InActiveUserException exception) {
            assertEquals(USER_NOT_VERIFIED, exception.getMessage());
        }
    }

    @Test
    void getUserByUserNameUserStatusPending() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.PENDING);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(any(String.class), any(UserStatus.class)))
            .thenReturn(userEntity);
        try {
            usersService.getUserByUserName(USER_NAME_VALUE);
        } catch (InActiveUserException exception) {
            assertEquals(USER_NOT_VERIFIED, exception.getMessage());
        }
    }

    @Test
    void getUserByUserNameUserStatusLocked() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.BLOCKED);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(any(String.class), any(UserStatus.class)))
            .thenReturn(userEntity);
        try {
            usersService.getUserByUserName(USER_NAME_VALUE);
        } catch (InActiveUserException exception) {
            assertEquals(USER_IS_BLOCKED, exception.getMessage());
        }
    }

    @Test
    void getUserAdditionalAttributeSuccess() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);

        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
            .thenReturn(userEntity);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userAttributeValueRepository.findAllByUserIdIn(Collections.singletonList(USER_ID_VALUE))).thenReturn(
            createUserAttributeValueData());
        when(userAttributeRepository.findAllById(anyList())).thenReturn(createUserAttributeMetaData());
        UserResponseV1 result = (UserResponseV1) usersService.getUser(USER_ID_VALUE, API_VERSION_1);
        assertEquals(result.getUserName(), userEntity.getUserName());
        assertEquals(result.getEmail(), userEntity.getEmail());
        assertEquals("hello", result.getAdditionalAttributes().get("mandatoryAttribute"));
    }

    @Test
    void getUsersSuccess() throws ResourceNotFoundException {
        UsersGetFilterV1 usersGetFilter = createUsersGetFilter();
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        Page page = Mockito.mock(Page.class);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));

        List<UserResponseV1> userResponses = usersService.getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE,
                UsersGetFilterBase.UserGetFilterEnum.ROLES.getField(), ASCENDING, false, null).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
    }

    @Test
    void getUsersSuccessWithIgnoreCase() throws ResourceNotFoundException {
        UsersGetFilterV1 usersGetFilter = createUsersGetFilter();
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        Page page = Mockito.mock(Page.class);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));

        List<UserResponseV1> userResponses = usersService.getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE,
                UsersGetFilterBase.UserGetFilterEnum.ROLES.getField(), ASCENDING, true, null).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
    }

    @Test
    void getUsersSuccessWithPrefix() throws ResourceNotFoundException {
        UsersGetFilterV1 usersGetFilter = createUsersGetFilter();
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        Page page = Mockito.mock(Page.class);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));

        List<UserResponseV1> userResponses = usersService.getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE,
                UsersGetFilterBase.UserGetFilterEnum.ROLES.getField(), ASCENDING, false, SearchType.PREFIX).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
    }

    @Test
    void getUsersSuccessWithSuffix() throws ResourceNotFoundException {
        UsersGetFilterV1 usersGetFilter = createUsersGetFilter();
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        Page page = Mockito.mock(Page.class);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));

        List<UserResponseV1> userResponses = usersService.getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE,
                UsersGetFilterBase.UserGetFilterEnum.ROLES.getField(), ASCENDING, false, SearchType.SUFFIX).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
    }

    @Test
    void getUsersSuccessWithContains() throws ResourceNotFoundException {
        UsersGetFilterV1 usersGetFilter = createUsersGetFilter();
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        Page page = Mockito.mock(Page.class);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));

        List<UserResponseV1> userResponses = usersService.getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE,
                UsersGetFilterBase.UserGetFilterEnum.ROLES.getField(), ASCENDING, false, SearchType.CONTAINS).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
    }

    @Test
    void getUsersSuccessWithoutSortBy() throws ResourceNotFoundException {
        UsersGetFilterV1 usersGetFilter = createUsersGetFilter();
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        Page page = Mockito.mock(Page.class);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));

        List<UserResponseV1> userResponses = usersService
            .getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE, null, ASCENDING, false, SearchType.CONTAINS).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
    }

    @Test
    void getUsersSuccessEmptyFilter() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        Page page = Mockito.mock(Page.class);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));

        List<UserResponseV1> userResponses = usersService
            .getUsers(new UsersGetFilterV1(), 0, DEFAULT_PAGE_SIZE, null, ASCENDING, false, SearchType.CONTAINS)
            .stream().map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
    }

    @Test
    void getUsersSuccessByAdditionalAttribute() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        UsersGetFilterV1 usersGetFilter = new UsersGetFilterV1();
        usersGetFilter.setAdditionalAttributes(Map.of("mandatoryAttribute", Collections.singleton("data")));
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        userEntity.setId(userAttributeValueEntities.get(INDEX_0).getUserId());

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        Page page = Mockito.mock(Page.class);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(userAttributeRepository.findAll()).thenReturn(createUserAttributeMetaData());
        when(userAttributeValueRepository.findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));
        when(userAttributeValueRepository.findAllByUserIdIn(anyList())).thenReturn(userAttributeValueEntities);
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(userAttributeRepository.findAllById(anyList())).thenReturn(userAttributeEntities);

        List<UserResponseV1> userResponses = usersService
            .getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE, null, ASCENDING, false, SearchType.CONTAINS).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
        assertEquals(ADDITIONAL_ATTRIBUTE_SIZE, userResponses.get(INDEX_0).getAdditionalAttributes().size());
    }

    @Test
    void getUsersSuccessByAdditionalAttributeWithIgnoreCase() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        UsersGetFilterV1 usersGetFilter = new UsersGetFilterV1();
        usersGetFilter.setAdditionalAttributes(Map.of("mandatoryAttribute", Collections.singleton("data")));
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        userEntity.setId(userAttributeValueEntities.get(INDEX_0).getUserId());

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        Page page = Mockito.mock(Page.class);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(userAttributeRepository.findAll()).thenReturn(createUserAttributeMetaData());
        when(userAttributeValueRepository.findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));
        when(userAttributeValueRepository.findAllByUserIdIn(anyList())).thenReturn(userAttributeValueEntities);
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(userAttributeRepository.findAllById(anyList())).thenReturn(userAttributeEntities);

        List<UserResponseV1> userResponses = usersService
            .getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE, null, ASCENDING, true, null).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
        assertEquals(ADDITIONAL_ATTRIBUTE_SIZE, userResponses.get(INDEX_0).getAdditionalAttributes().size());
    }

    @Test
    void getUsersSuccessByAdditionalAttributeWithPrefix() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        UsersGetFilterV1 usersGetFilter = new UsersGetFilterV1();
        usersGetFilter.setAdditionalAttributes(Map.of("mandatoryAttribute", Collections.singleton("data")));
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        userEntity.setId(userAttributeValueEntities.get(INDEX_0).getUserId());

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        Page page = Mockito.mock(Page.class);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(userAttributeRepository.findAll()).thenReturn(createUserAttributeMetaData());
        when(userAttributeValueRepository.findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));
        when(userAttributeValueRepository.findAllByUserIdIn(anyList())).thenReturn(userAttributeValueEntities);
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(userAttributeRepository.findAllById(anyList())).thenReturn(userAttributeEntities);

        List<UserResponseV1> userResponses = usersService
            .getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE, null, ASCENDING, false, SearchType.PREFIX).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
        assertEquals(ADDITIONAL_ATTRIBUTE_SIZE, userResponses.get(INDEX_0).getAdditionalAttributes().size());
    }

    @Test
    void getUsersSuccessByAdditionalAttributeWithSuffix() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        UsersGetFilterV1 usersGetFilter = new UsersGetFilterV1();
        usersGetFilter.setAdditionalAttributes(Map.of("mandatoryAttribute", Collections.singleton("data")));
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        userEntity.setId(userAttributeValueEntities.get(INDEX_0).getUserId());

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        Page page = Mockito.mock(Page.class);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(userAttributeRepository.findAll()).thenReturn(createUserAttributeMetaData());
        when(userAttributeValueRepository.findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));
        when(userAttributeValueRepository.findAllByUserIdIn(anyList())).thenReturn(userAttributeValueEntities);
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(userAttributeRepository.findAllById(anyList())).thenReturn(userAttributeEntities);

        List<UserResponseV1> userResponses = usersService
            .getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE, null, ASCENDING, false, SearchType.SUFFIX).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
        assertEquals(ADDITIONAL_ATTRIBUTE_SIZE, userResponses.get(INDEX_0).getAdditionalAttributes().size());
    }

    @Test
    void getUsersSuccessByAdditionalAttributeWithContains() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        UsersGetFilterV1 usersGetFilter = new UsersGetFilterV1();
        usersGetFilter.setAdditionalAttributes(Map.of("mandatoryAttribute", Collections.singleton("data")));
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        userEntity.setId(userAttributeValueEntities.get(INDEX_0).getUserId());

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        Page page = Mockito.mock(Page.class);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(userAttributeRepository.findAll()).thenReturn(createUserAttributeMetaData());
        when(userAttributeValueRepository.findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));
        when(userAttributeValueRepository.findAllByUserIdIn(anyList())).thenReturn(userAttributeValueEntities);
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(userAttributeRepository.findAllById(anyList())).thenReturn(userAttributeEntities);

        List<UserResponseV1> userResponses = usersService
            .getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE, null, ASCENDING, false, SearchType.CONTAINS).stream()
            .map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
        assertEquals(ADDITIONAL_ATTRIBUTE_SIZE, userResponses.get(INDEX_0).getAdditionalAttributes().size());
    }

    @Test
    void getUsersSuccessByAdditionalAttributeWithSortBy() throws ResourceNotFoundException {
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        UsersGetFilterV1 usersGetFilter = new UsersGetFilterV1();
        usersGetFilter.setAdditionalAttributes(Map.of("mandatoryAttribute", Collections.singleton("data")));
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        userEntity.setId(userAttributeValueEntities.get(INDEX_0).getUserId());

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
            .thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        Page page = Mockito.mock(Page.class);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        when(userAttributeRepository.findAll()).thenReturn(createUserAttributeMetaData());
        when(userAttributeValueRepository.findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(page.getContent()).thenReturn(Collections.singletonList(userEntity));
        when(userAttributeValueRepository.findAllByUserIdIn(anyList())).thenReturn(userAttributeValueEntities);
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(userAttributeRepository.findAllById(anyList())).thenReturn(userAttributeEntities);

        List<UserResponseV1> userResponses = usersService
            .getUsers(usersGetFilter, 0, DEFAULT_PAGE_SIZE,
                String.valueOf(UsersGetFilterBase.UserGetFilterEnum.CITIES), ASCENDING, false, null)
            .stream().map(UserResponseV1.class::cast).toList();
        assertEquals(1, userResponses.size());
        assertEquals(USER_NAME_VALUE, userResponses.get(INDEX_0).getUserName());
        assertEquals(CITY_VALUE, userResponses.get(INDEX_0).getCity());
        assertEquals(FIRST_NAME_VALUE, userResponses.get(INDEX_0).getFirstName());
        assertEquals(EMAIL_VALUE, userResponses.get(INDEX_0).getEmail());
        assertEquals(STATE_VALUE, userResponses.get(INDEX_0).getState());
        assertEquals(ADDITIONAL_ATTRIBUTE_SIZE, userResponses.get(INDEX_0).getAdditionalAttributes().size());
    }

    @Test
    void deleteUsersFailureUserNotFound() {

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any(UserStatus.class))).thenReturn(null);
        assertThrows(ResourceNotFoundException.class,
            () -> usersService.deleteUser(USER_ID_VALUE, false, USER_ID_VALUE_2));
    }

    @Test
    void deleteUserDeactivatedUserNotFound() {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findAllByIdInAndStatusNot(anySet(), any(UserStatus.class))).thenReturn(null);
        assertThrows(ResourceNotFoundException.class,
            () -> usersService.deleteUser(USER_ID_VALUE, false, USER_ID_VALUE_2));
    }

    @Test
    void deleteUserSuccess() throws ResourceNotFoundException {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findAllByIdInAndStatusNot(anySet(), any(UserStatus.class))).thenReturn(List.of(userEntity));
        List<UserEntity> savedEntities = new ArrayList<>();
        savedEntities.add(userEntity);
        when(userRepository.saveAll(anyList())).thenReturn(savedEntities);
        //when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(cacheTokenService.getAccessToken()).thenReturn("dummyToken");
        BaseResponseFromAuthorization baseResponseFromAuthorization = new BaseResponseFromAuthorization();
        baseResponseFromAuthorization.setHttpStatus(OK);

        when(authorizationServerClient.revokeTokenByAdmin(anyString(), anyString()))
            .thenReturn(baseResponseFromAuthorization);

        UserResponseV1 userResponse = usersService.deleteUser(USER_ID_VALUE, false, USER_ID_VALUE);
        assertEquals(UserStatus.DELETED, userResponse.getStatus());
    }

    @Test
    void deleteExternalUserSuccess() throws ResourceNotFoundException {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setIsExternalUser(true);
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class))).thenReturn(userEntity);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        userEntity.setStatus(UserStatus.DELETED);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        UserResponseV1 userResponse = usersService.deleteUser(USER_ID_VALUE, true, USER_ID_VALUE);
        assertEquals(UserStatus.DELETED, userResponse.getStatus());
    }

    @Test
    void deleteExternalUserFailure() {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class))).thenReturn(null);
        assertThrows(ResourceNotFoundException.class,
            () -> usersService.deleteUser(USER_ID_VALUE, true, USER_ID_VALUE_3));
    }

    @Test
    void deleteUsersFailureDeleteFailed() {

        UsersDeleteFilter usersDeleteFilter = new UsersDeleteFilter(Set.of(USER_ID_VALUE));
        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(ATTR_ID_VALUE_1);
        when(userRepository.findAllByIdInAndStatusNot(anySet(), any(UserStatus.class))).thenReturn(List.of(userEntity));
        assertThrows(ResourceNotFoundException.class,
            () -> usersService.deleteUsers(usersDeleteFilter));
    }

    @Test
    void getUserMetaDataSuccess() {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(userAttributeRepository.findAll()).thenReturn(createUserAttributeMetaData());
        List<UserMetaDataResponse> userMetaDataResponseList = usersService.getUserMetaData();
        assertEquals(USER_META_RESPONSE_SIZE_1, userMetaDataResponseList.size());
        int additionalAttributeCount = userMetaDataResponseList.stream()
            .filter(UserMetaDataResponse::getDynamicAttribute)
            .toList()
            .size();
        assertEquals(ADDITIONAL_ATTRIBUTE_SIZE, additionalAttributeCount);
        int mandatoryAttributeCount = userMetaDataResponseList.stream()
            .filter(UserMetaDataResponse::getMandatory)
            .toList()
            .size();
        assertEquals(MANDATORY_ATTRIBUTE_COUNT_1, mandatoryAttributeCount);
    }

    @Test //SANDHYA FIX THIS
    void getUserMetaDataNoAdditionalAttributeSuccess() {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        List<UserMetaDataResponse> userMetaDataResponseList = usersService.getUserMetaData();
        assertEquals(USER_META_RESPONSE_SIZE_2, userMetaDataResponseList.size());
        int additionalAttributeCount = userMetaDataResponseList.stream()
            .filter(UserMetaDataResponse::getDynamicAttribute)
            .toList()
            .size();
        assertEquals(0, additionalAttributeCount);
        int mandatoryAttributeCount = userMetaDataResponseList.stream()
            .filter(UserMetaDataResponse::getMandatory)
            .toList()
            .size();
        assertEquals(MANDATORY_ATTRIBUTE_COUNT_2, mandatoryAttributeCount);
    }

    @Test
    void putUserAttributeModifySuccess() {
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        when(userAttributeRepository.saveAll(anyList())).thenReturn(userAttributeEntities);

        List<UserMetaDataResponse> metaDataResponses = usersService.putUserMetaData(createUserRequestMetaData());
        assertEquals(ADDITIONAL_ATTRIBUTE_SIZE, metaDataResponses.size());
        assertEquals(true, metaDataResponses.get(INDEX_0).getMandatory());
        assertEquals(true, metaDataResponses.get(INDEX_1).getUnique());
        assertEquals(true, metaDataResponses.get(INDEX_2).getReadOnly());
    }

    @Test
    void putUserAttributeAddSuccess() {
        UserMetaDataRequest userMetaDataRequest = new UserMetaDataRequest("new_attribute", true,
            false, false, true, true, "varchar", ".*");
        List<UserAttributeEntity> userAttributeEntities =
            List.of(UserMapper.USER_MAPPER.mapToMetaDataEntity(userMetaDataRequest));
        when(userAttributeRepository.findAll()).thenReturn(createUserAttributeMetaData());
        when(userAttributeRepository.saveAll(anyList())).thenReturn(userAttributeEntities);
        List<UserMetaDataResponse> metaDataResponses = usersService.putUserMetaData(List.of(userMetaDataRequest));
        assertEquals(1, metaDataResponses.size());
        assertEquals(true, metaDataResponses.get(INDEX_0).getMandatory());
        assertEquals(false, metaDataResponses.get(INDEX_0).getUnique());
        assertEquals(false, metaDataResponses.get(INDEX_0).getReadOnly());
    }

    List<UserAttributeEntity> createUserAttributeMetaData() {
        UserAttributeEntity mandatoryEntityMetaData =
            new UserAttributeEntity(ATTR_ID_VALUE_1, "mandatoryAttribute", true, false,
                false, true, true, "varchar", ".*",
                "system", null, "system", null);
        UserAttributeEntity uniqueEntityMetaData =
            new UserAttributeEntity(ATTR_ID_VALUE_2, "uniqueAttribute", false, true,
                false, true, true, "varchar", ".*",
                "system", null, "system", null);
        UserAttributeEntity readOnlyEntityMetaData =
            new UserAttributeEntity(ATTR_ID_VALUE, "readOnlyAttribute", false, false,
                true, true, true, "varchar", ".*",
                "system", null, "system", null);
        return List.of(mandatoryEntityMetaData, uniqueEntityMetaData, readOnlyEntityMetaData);
    }

    List<UserMetaDataRequest> createUserRequestMetaData() {
        UserMetaDataRequest mandatoryMetaData =
            new UserMetaDataRequest("mandatoryAttribute", true, false, false,
                true, true, "varchar", ".*");
        UserMetaDataRequest uniqueMetaData =
            new UserMetaDataRequest("uniqueAttribute", false, true, false,
                true, true, "varchar", ".*");
        UserMetaDataRequest readOnlyMetaData =
            new UserMetaDataRequest("readOnlyAttribute", false, false, true,
                true, true, "varchar", ".*");
        return List.of(mandatoryMetaData, uniqueMetaData, readOnlyMetaData);
    }

    List<UserAttributeValueEntity> createUserAttributeValueData() {
        UserAttributeValueEntity mandatoryValueData =
            new UserAttributeValueEntity(USER_ID_VALUE, USER_ID_VALUE, ATTR_ID_VALUE_1,
                "hello", "system");
        UserAttributeValueEntity uniqueValue =
            new UserAttributeValueEntity(USER_ID_VALUE_2, USER_ID_VALUE, ATTR_ID_VALUE_2, "xyz",
                "system");
        UserAttributeValueEntity readOnlyData =
            new UserAttributeValueEntity(USER_ID_VALUE_3, USER_ID_VALUE, ATTR_ID_VALUE, "hhm",
                "system");

        return List.of(mandatoryValueData, uniqueValue, readOnlyData);
    }

    private RegisteredClientDetails isClientAllowToManageUsersResponse() {
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
    void changeUserStatusSuccess() {

        UserEntity userEntity = createUserEntity(UserStatus.PENDING);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(true);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any())).thenReturn(userEntity);

        List<UserResponseV1> result = usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        assertEquals(UserStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void changeUserStatusDeactivatedSuccess() {

        UserEntity userEntity = createUserEntity(UserStatus.DEACTIVATED);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(true);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any())).thenReturn(userEntity);

        List<UserResponseV1> result = usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        assertEquals(UserStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void changeUserStatusRejectedSuccess_WithFalseFlag() {

        UserEntity userEntity = createUserEntity(UserStatus.REJECTED);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(false);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any())).thenReturn(userEntity);

        List<UserResponseV1> result = usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        assertEquals(UserStatus.REJECTED, result.get(0).getStatus());
    }

    @Test
    void changeUserStatusEmptyRequest() {

        List<UserResponseV1> result = usersService.changeUserStatus(null, null);
        assertTrue(CollectionUtils.isEmpty(result));
    }

    @Test
    void changeUserStatusEmptyIds() {
        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);

        List<UserResponseV1> result = usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        assertTrue(CollectionUtils.isEmpty(result));
    }

    @Test
    void changeUserStatusWithRevokeTokenSuccess() {

        UserEntity userEntity = createUserEntity(UserStatus.PENDING);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(cacheTokenService.getAccessToken()).thenReturn("dummyToken");

        BaseResponseFromAuthorization baseResponseFromAuthorization = new BaseResponseFromAuthorization();
        baseResponseFromAuthorization.setHttpStatus(OK);

        when(authorizationServerClient.revokeTokenByAdmin(anyString(), anyString()))
            .thenReturn(baseResponseFromAuthorization);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any()))
            .thenReturn(userEntity);

        List<UserResponseV1> result = usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        assertEquals(UserStatus.REJECTED, result.get(0).getStatus());
    }

    @Test
    void changeUserStatusActiveWithRevokeTokenSuccess() {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(cacheTokenService.getAccessToken()).thenReturn("dummyToken");

        BaseResponseFromAuthorization baseResponseFromAuthorization = new BaseResponseFromAuthorization();
        baseResponseFromAuthorization.setHttpStatus(OK);

        when(authorizationServerClient.revokeTokenByAdmin(anyString(), anyString()))
            .thenReturn(baseResponseFromAuthorization);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any()))
            .thenReturn(userEntity);

        List<UserResponseV1> result = usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        assertEquals(UserStatus.DEACTIVATED, result.get(0).getStatus());
    }

    @Test
    void changeUserStatusPendingWithNullFlag() {

        UserEntity userEntity = createUserEntity(UserStatus.PENDING);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(null);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any()))
            .thenReturn(userEntity);

        List<UserResponseV1> result = usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        assertEquals(UserStatus.REJECTED, result.get(0).getStatus());
    }

    @Test
    void changeUserStatusActiveWithRevokeTokenFailure() {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(cacheTokenService.getAccessToken()).thenReturn("dummyToken");

        BaseResponseFromAuthorization baseResponseFromAuthorization = new BaseResponseFromAuthorization();
        baseResponseFromAuthorization.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        when(authorizationServerClient.revokeTokenByAdmin(anyString(), anyString()))
            .thenReturn(baseResponseFromAuthorization);

        try {
            usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        } catch (ApplicationRuntimeException e) {
            assertEquals(INTERNAL_SERVER_ERROR, e.getHttpStatus(), "Exception while revoking token");
        }
    }

    @Test
    void changeUserStatusActiveWithUidamFetchTokenFailure() {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(cacheTokenService.getAccessToken())
            .thenThrow(new ApplicationRuntimeException("error while fetching token from authorization-server",
                INTERNAL_SERVER_ERROR));

        try {
            usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        } catch (ApplicationRuntimeException e) {
            assertEquals(INTERNAL_SERVER_ERROR, e.getHttpStatus(),
                "error while fetching token from authorization-server");
        }
    }

    @Test
    void changeUserStatusActiveWithIncorrectUsername() {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("admin:");

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        BaseResponseFromAuthorization baseResponseFromAuthorization = new BaseResponseFromAuthorization();
        baseResponseFromAuthorization.setHttpStatus(OK);

        when(authorizationServerClient.revokeTokenByAdmin(anyString(), anyString()))
            .thenReturn(baseResponseFromAuthorization);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any()))
            .thenReturn(userEntity);

        try {
            usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus(), INVALID_INPUT_USERNAME_PATTERN);
        }
    }

    @Test
    void changeUserStatusActiveWithNullToken() {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setApproved(false);
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));

        when(userRepository.findAllByIdInAndStatusNot(anySet(), any()))
            .thenReturn(List.of(userEntity));
        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(cacheTokenService.getAccessToken()).thenReturn(null);

        BaseResponseFromAuthorization baseResponseFromAuthorization = new BaseResponseFromAuthorization();
        baseResponseFromAuthorization.setHttpStatus(OK);

        when(authorizationServerClient.revokeTokenByAdmin(anyString(), anyString()))
            .thenReturn(baseResponseFromAuthorization);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        when(userRepository.save(any()))
            .thenReturn(userEntity);

        try {
            usersService.changeUserStatus(userChangeStatusRequest, USER_ID_VALUE);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus());
        }
    }

    @Test
    void testAddExternalUserWithInvalidIsExternalUser() throws ResourceNotFoundException {
        UserDtoV1 externalUserPost = createExternalUserPost(UserStatus.ACTIVE);
        externalUserPost.setIsExternalUser(false);
        try {
            usersService.addExternalUser(externalUserPost, null);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus());
            assertEquals(INVALID_EXTERNAL_USER, e.getKey());
            return;
        }
    }

    @Test
    void testAddExternalUserWithNullIsExternalUser() throws ResourceNotFoundException {
        UserDtoV1 externalUserPost = createExternalUserPost(UserStatus.ACTIVE);
        externalUserPost.setIsExternalUser(null);
        try {
            usersService.addExternalUser(externalUserPost, null);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus());
            assertEquals(INVALID_EXTERNAL_USER, e.getKey());
            return;
        }
    }

    @Test
    void testAddExternalUserWithInvalidRole() throws ResourceNotFoundException {
        UserDtoV1 externalUserPost = createExternalUserPost(UserStatus.ACTIVE);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn(INVALID_ROLE_VALUE);
        try {
            usersService.addExternalUser(externalUserPost, null);
        } catch (ApplicationRuntimeException e) {
            assertEquals(BAD_REQUEST, e.getHttpStatus());
            assertEquals(INVALID_INPUT_ROLE, e.getKey());
            return;
        }
    }

    public static RoleListRepresentation createSingleRoleListDtoRepresentation() {
        Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        scopeDto.setId(SCOPE_ID);
        RoleCreateResponse roleDto1 = new RoleCreateResponse();
        roleDto1.setName(ROLE_VALUE);
        roleDto1.setId(ROLE_ID_2);
        roleDto1.setScopes(Collections.singletonList(scopeDto));

        Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        roleDtoList.add(roleDto1);

        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }

    @Test
    void testAddExternalUserSuccess() throws ResourceNotFoundException {
        UserDtoV1 externalUserPost = createExternalUserPost(UserStatus.ACTIVE);

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(externalUserPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);

        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(externalUserPost.getRoles());
        RoleListRepresentation roleListDto = createSingleRoleListDtoRepresentation();

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn(ROLE_VALUE);
        when(applicationProperties.getExternalUserDefaultStatus()).thenReturn("PENDING");
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createOneRoleListDtoRepresentation());

        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addExternalUser(externalUserPost,
            USER_ID_VALUE);
        assertEquals(userResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(userResponse.getEmail(), receivedResponse.getEmail());
        assertEquals(userResponse.getRoles(), receivedResponse.getRoles());
        assertEquals(UserStatus.PENDING, receivedResponse.getStatus());
    }

    @Test
    void testAddExternalUserSuccessWithoutExternalStatusInConfig() throws ResourceNotFoundException {
        UserDtoV1 externalUserPost = createExternalUserPost(UserStatus.ACTIVE);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(externalUserPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);

        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(externalUserPost.getRoles());
        RoleListRepresentation roleListDto = createSingleRoleListDtoRepresentation();

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn(ROLE_VALUE);
        when(applicationProperties.getExternalUserDefaultStatus()).thenReturn(null);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_VALUE);
        role.setId(ROLE_ID_1);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createOneRoleListDtoRepresentation());

        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addExternalUser(externalUserPost,
            USER_ID_VALUE);
        assertEquals(userResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(userResponse.getEmail(), receivedResponse.getEmail());
        assertEquals(userResponse.getRoles(), receivedResponse.getRoles());
        assertEquals(userResponse.getStatus(), receivedResponse.getStatus());
    }

    @Test
    void testAddExternalUserWhenRoleDoesNotExistFailure() throws ResourceNotFoundException {
        UserDtoV1 externalUserPost = createExternalUserPost(UserStatus.ACTIVE);
        externalUserPost.setRoles(Collections.singleton("INVALID_ROLE"));
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn("INVALID_ROLE");
        when(applicationProperties.getExternalUserDefaultStatus()).thenReturn("PENDING");
        try {
            usersService.addExternalUser(externalUserPost, null);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(USER_ROLES_NOT_FOUND, exception.getKey());
            return;
        }
    }

    @Test
    void testAddExternalUserWhenMandatoryAdditionalAttributeMissing() throws ResourceNotFoundException {
        UserDtoV1 userPost = createExternalUserPost(UserStatus.ACTIVE);
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn(ROLE_VALUE);
        when(applicationProperties.getExternalUserDefaultStatus()).thenReturn("PENDING");
        try {
            usersService.addExternalUser(userPost, null);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(MISSING_MANDATORY_PARAMETERS, exception.getKey());
            return;
        }
    }

    @Test
    void testAddExternalUserWhenDuplicateAdditionalAttributeData() throws ResourceNotFoundException {
        UserDtoV1 userPost = createExternalUserPost(UserStatus.ACTIVE);
        userPost.setAdditionalAttributes("mandatoryAttribute", "xyz");
        userPost.setAdditionalAttributes("uniqueAttribute", "hello");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn(ROLE_VALUE);
        when(applicationProperties.getExternalUserDefaultStatus()).thenReturn("PENDING");
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        try {
            usersService.addExternalUser(userPost, null);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(FIELD_IS_UNIQUE, exception.getKey());
            return;
        }
    }

    @Test
    void testAddExternalUserWhenAdditionalAttributeNotExists() throws ResourceNotFoundException {
        UserDtoV1 userPost = createExternalUserPost(UserStatus.ACTIVE);
        userPost.setAdditionalAttributes("mandatoryAttribute", "xyz");
        userPost.setAdditionalAttributes("unsavedAttribute", "hello");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        List<UserAttributeValueEntity> userAttributeValueEntities = createUserAttributeValueData();
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn(ROLE_VALUE);
        when(applicationProperties.getExternalUserDefaultStatus()).thenReturn("PENDING");
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);

        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(userAttributeValueEntities);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        try {
            usersService.addExternalUser(userPost, null);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(FIELD_NOT_FOUND, exception.getKey());
            return;
        }
    }

    @Test
    void testAddExternalUserWhenAdditionalAttributeDataInvalid() throws ResourceNotFoundException {
        UserDtoV1 userPost = createExternalUserPost(UserStatus.ACTIVE);
        userPost.setAdditionalAttributes("mandatoryAttribute", "123");
        userPost.setAdditionalAttributes("uniqueAttribute", "hello");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        userAttributeEntities.get(INDEX_0).setRegex("[A-Z]+");
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(Collections.EMPTY_LIST);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn(ROLE_VALUE);
        when(applicationProperties.getExternalUserDefaultStatus()).thenReturn("PENDING");
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);
        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        try {
            usersService.addExternalUser(userPost, null);
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(FIELD_DATA_IS_INVALID, exception.getKey());
            return;
        }
    }

    @Test
    void testAddExternalUserWithAdditionalAttributeDataSuccess() throws ResourceNotFoundException {
        UserDtoV1 userPost = createExternalUserPost(UserStatus.ACTIVE);
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userPost);
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);

        userPost.setAdditionalAttributes("mandatoryAttribute", "hello");
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();

        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn(ROLE_VALUE);
        when(applicationProperties.getExternalUserDefaultStatus()).thenReturn("PENDING");
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));
        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());

        List<UserAttributeEntity> userAttributeEntities = createUserAttributeMetaData();
        when(userAttributeValueRepository
            .findAll(any(Specification.class))).thenReturn(Collections.EMPTY_LIST);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntities);
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.save(any(UserEntity.class)))
            .thenReturn(userEntity);
        when(userAttributeValueRepository
            .saveAll(anyList())).thenReturn(createUserAttributeValueData());
        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addExternalUser(userPost, null);
        assertEquals("hello", receivedResponse.getAdditionalAttributes().get("mandatoryAttribute"));
    }

    @Test
    void deleteExternalUserInvalidExternalUserFailure() {

        UserEntity userEntity = createUserEntity(UserStatus.ACTIVE);
        userEntity.setIsExternalUser(false);
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class))).thenReturn(userEntity);
        assertThrows(ApplicationRuntimeException.class,
            () -> usersService.deleteUser(USER_ID_VALUE, true, USER_ID_VALUE));
    }

    void addUserAccountRoleMapping(UserEntity user) {
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_1));
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        user.setAccountRoleMapping(l);
    }


    public static FederatedUserDto createFederatedUserPost(UserStatus status) {
        FederatedUserDto federatedUserDto = new FederatedUserDto();
        fillUserRequest(federatedUserDto);
        federatedUserDto.setUserName(USER_NAME_VALUE);
        federatedUserDto.setRoles(Set.of(ROLE_VALUE));
        federatedUserDto.setStatus(status);
        federatedUserDto.setAud("k8s-portal");
        federatedUserDto.setIsExternalUser(true);
        federatedUserDto.setIdentityProviderName("google");
        return federatedUserDto;
    }

    @Test
    void testAddFederatedUserSuccess() throws ResourceNotFoundException {

        FederatedUserDto federatedUserPost = createFederatedUserPost(UserStatus.ACTIVE);

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(federatedUserPost);
        userEntity.setUserName("google_johnd");
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(federatedUserPost.getRoles());
        RoleListRepresentation roleListDto = createSingleRoleListDtoRepresentation();

        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(false);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));

        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createOneRoleListDtoRepresentation());

        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addFederatedUser(federatedUserPost,
            USER_ID_VALUE);
        assertEquals(userResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(userResponse.getEmail(), receivedResponse.getEmail());
        assertEquals(userResponse.getRoles(), receivedResponse.getRoles());
        assertEquals(userResponse.getStatus(), receivedResponse.getStatus());
    }

    @Test
    void testAddFederatedUser_usernameExist() {

        FederatedUserDto federatedUserPost = createFederatedUserPost(UserStatus.ACTIVE);

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(federatedUserPost);
        userEntity.setUserName("google_johnd");
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);

        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(federatedUserPost.getRoles());
        when(rolesService.getRoleById(anySet())).thenReturn(createRoleListDtoRepresentation());
        RoleListRepresentation roleListDto = createSingleRoleListDtoRepresentation();
        when(userRepository.findByUserName(anyString())).thenReturn(List.of(userEntity));

        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(false);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));

        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createOneRoleListDtoRepresentation());


        assertThrows(ApplicationRuntimeException.class,
            () -> usersService.addFederatedUser(federatedUserPost, USER_ID_VALUE));
    }

    @Test
    void testAddFederatedUserSuccess_validateIsExternalUserFailure() {

        FederatedUserDto federatedUserPost = createFederatedUserPost(UserStatus.ACTIVE);
        federatedUserPost.setIsExternalUser(false);

        assertThrows(ApplicationRuntimeException.class,
            () -> usersService.addFederatedUser(federatedUserPost, USER_ID_VALUE));
    }

    @Test
    void testAddFederatedUserSuccess_withGetIsUserStatusLifeCycleEnabledTrue() throws ResourceNotFoundException {

        FederatedUserDto federatedUserPost = createFederatedUserPost(UserStatus.PENDING);

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(federatedUserPost);
        userEntity.setUserName("google_johnd");
        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(createUserAccountRoleMappingEntity(ROLE_ID_2));
        userEntity.setAccountRoleMapping(l);
        UserResponseV1 userResponse = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
        userResponse.setRoles(federatedUserPost.getRoles());
        RoleListRepresentation roleListDto = createSingleRoleListDtoRepresentation();

        when(applicationProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean())).thenReturn(roleListDto);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);

        AccountEntity a = new AccountEntity();
        a.setAccountName("TestAccount");
        a.setId(ACCOUNT_ID_VALUE);
        when(accountRepository.findByAccountName(any(String.class))).thenReturn(Optional.of(a));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(a));

        RolesEntity role = new RolesEntity();
        role.setName(ROLE_2);
        role.setId(ROLE_ID_2);
        when(rolesRepository.getRolesByName(any(String.class))).thenReturn(role);
        when(rolesService.getRoleById(anySet())).thenReturn(createOneRoleListDtoRepresentation());

        UserResponseV1 receivedResponse = (UserResponseV1) usersService.addFederatedUser(federatedUserPost,
            USER_ID_VALUE);
        assertEquals(userResponse.getUserName(), receivedResponse.getUserName());
        assertEquals(userResponse.getEmail(), receivedResponse.getEmail());
        assertEquals(userResponse.getRoles(), receivedResponse.getRoles());
        assertEquals(userResponse.getStatus(), receivedResponse.getStatus());
    }

    private List<UserAccountRoleMappingEntity> getAccountRoleMapping(UserEntity userEntity) {
        UserAccountRoleMappingEntity uar = new UserAccountRoleMappingEntity();
        uar.setAccountId(ACCOUNT_ID_VALUE);
        uar.setRoleId(ROLE_ID_1);
        uar.setUserId(userEntity.getId());
        List<UserAccountRoleMappingEntity> accountRoleMapping = userEntity.getAccountRoleMapping();
        accountRoleMapping.add(uar);
        return accountRoleMapping;
    }

    private RoleListRepresentation getAdminUserRole() {
        RoleCreateResponse role = new RoleCreateResponse();
        role.setId(ROLE_ID_2);
        role.setName("TENANT_ADMIN");
        role.setDescription("roles");
        RoleCreateResponse role1 = new RoleCreateResponse();
        role1.setId(ROLE_ID_1);
        role1.setName("BUSINESS_ADMIN");
        role1.setDescription("roles");
        Set<RoleCreateResponse> roles = new HashSet<>();
        roles.add(role);
        roles.add(role1);
        List<Scope> scopes = new ArrayList<>();
        Scope scope = new Scope();
        scope.setId("1");
        scope.setName("TenantSystem");
        scopes.add(scope);
        role.setScopes(scopes);
        List<Scope> scopes1 = new ArrayList<>();
        Scope scope1 = new Scope();
        scope1.setId("1");
        scope1.setName("ManageUsers");
        scopes1.add(scope);
        role1.setScopes(scopes);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();

        roleRepresentation.setRoles(roles);
        List<ResponseMessage> messages = new ArrayList<>();
        roleRepresentation.setMessages(messages);
        return roleRepresentation;
    }

    @Test
    void editExternalUserFailure_UserNotFound() throws IOException, ResourceNotFoundException {

        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(null);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
            + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
            + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);

        assertThrows(ResourceNotFoundException.class,
            () -> usersService.editUser(USER_ID_VALUE, jsonPatch, USER_ID_VALUE, true, "v1"));
    }

    @Test
    void editExternalUserFailure_NotExternalUser() throws IOException, ResourceNotFoundException,
        UserAccountRoleMappingException {
        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setIsExternalUser(false);
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
            + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
            + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        try {
            usersService.editUser(USER_ID_VALUE, jsonPatch, USER_ID_VALUE, true, "v1");
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(INVALID_EXTERNAL_USER, exception.getKey());
            return;
        }
    }

    @Test
    void editExternalUserFailure_InvalidPermittedRole() throws IOException, ResourceNotFoundException,
        UserAccountRoleMappingException {

        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setIsExternalUser(true);
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        List<UserAccountRoleMappingEntity> accountRoleMapping = getAccountRoleMapping(userEntity);
        userEntity.setAccountRoleMapping(accountRoleMapping);
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        RoleCreateResponse role = new RoleCreateResponse();
        role.setId(ROLE_ID_1);
        role.setName("BUSINESS_ADMIN");
        role.setDescription("roles");
        Set<RoleCreateResponse> roles = new HashSet<>();
        roles.add(role);
        List<Scope> scopes = new ArrayList<>();
        Scope scope = new Scope();
        scope.setId("1");
        scope.setName("IgniteSystem");
        scopes.add(scope);
        role.setScopes(scopes);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();

        roleRepresentation.setRoles(roles);
        List<ResponseMessage> messages = new ArrayList<>();
        roleRepresentation.setMessages(messages);
        when(rolesService.getRoleById(Set.of(ROLE_ID_1))).thenReturn(roleRepresentation);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn("BUSINESS_ADMIN1");
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
            + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
            + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        try {
            usersService.editUser(USER_ID_VALUE, jsonPatch, USER_ID_VALUE, true, "v1");
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(INVALID_INPUT_ROLE, exception.getKey());
            return;
        }
    }

    @Test
    void editExternalUserFailure_ForbiddenField() throws IOException, ResourceNotFoundException,
        UserAccountRoleMappingException {

        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setIsExternalUser(true);
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        List<UserAccountRoleMappingEntity> accountRoleMapping = getAccountRoleMapping(userEntity);
        userEntity.setAccountRoleMapping(accountRoleMapping);
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        RoleCreateResponse role = new RoleCreateResponse();
        role.setId(ROLE_ID_1);
        role.setName("BUSINESS_ADMIN");
        role.setDescription("roles");
        Set<RoleCreateResponse> roles = new HashSet<>();
        roles.add(role);
        List<Scope> scopes = new ArrayList<>();
        Scope scope = new Scope();
        scope.setId("1");
        scope.setName("IgniteSystem");
        scopes.add(scope);
        role.setScopes(scopes);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();

        roleRepresentation.setRoles(roles);
        List<ResponseMessage> messages = new ArrayList<>();
        roleRepresentation.setMessages(messages);
        when(rolesService.getRoleById(Set.of(ROLE_ID_1))).thenReturn(roleRepresentation);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn("BUSINESS_ADMIN");
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
            .thenReturn(attributeValueList);
        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
            Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
            .thenReturn(getAdminUserRole());
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
            + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
            + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);

        when(userRepository.save(Mockito.any())).thenReturn(userEntity);
        try {
            usersService.editUser(USER_ID_VALUE, jsonPatch, USER_ID_VALUE, true, "v1");
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(ACTION_FORBIDDEN, exception.getKey());
            return;
        }
    }

    @Test
    void editExternalUserSuccess() throws IOException, ResourceNotFoundException,
        UserAccountRoleMappingException {

        UserEntity userEntity = new UserEntity();
        userEntity.setFirstName("John");
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setIsExternalUser(true);
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        List<UserAccountRoleMappingEntity> accountRoleMapping = getAccountRoleMapping(userEntity);
        userEntity.setAccountRoleMapping(accountRoleMapping);
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        RoleCreateResponse role = new RoleCreateResponse();
        role.setId(ROLE_ID_1);
        role.setName("BUSINESS_ADMIN");
        role.setDescription("roles");
        Set<RoleCreateResponse> roles = new HashSet<>();
        roles.add(role);
        List<Scope> scopes = new ArrayList<>();
        Scope scope = new Scope();
        scope.setId("1");
        scope.setName("IgniteSystem");
        scopes.add(scope);
        role.setScopes(scopes);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();

        roleRepresentation.setRoles(roles);
        List<ResponseMessage> messages = new ArrayList<>();
        roleRepresentation.setMessages(messages);
        when(rolesService.getRoleById(Set.of(ROLE_ID_1))).thenReturn(roleRepresentation);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn("BUSINESS_ADMIN");
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
            .thenReturn(attributeValueList);
        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
            Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
            .thenReturn(getAdminUserRole());
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);
        UserEntity updatedUserEntity = userEntity;
        updatedUserEntity.setFirstName("JohnSEorro");
        updatedUserEntity.getUserAddresses().get(0).setAddress1("HELLWORLD2");
        when(userRepository.save(Mockito.any())).thenReturn(updatedUserEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
            + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
            + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        UserResponseV1 userResponse = (UserResponseV1) usersService.editUser(USER_ID_VALUE, jsonPatch,
            USER_ID_VALUE, true, "v1");
        assertEquals(updatedUserEntity.getFirstName(), userResponse.getFirstName());
        assertEquals(updatedUserEntity.getUserAddresses().get(0).getAddress1(), userResponse.getAddress1());
    }

    private List<UserAttributeEntity> getUserAttributeDetails() {
        UserAttributeEntity userAttributeEntity = new UserAttributeEntity();
        userAttributeEntity.setDynamicAttribute(true);
        userAttributeEntity.setReadOnly(false);
        userAttributeEntity.setId(ATTR_ID_VALUE);
        userAttributeEntity.setName("additionalattribute");
        userAttributeEntity.setRegex(".*");
        userAttributeEntity.setTypes("varchar");
        userAttributeEntity.setMandatory(false);
        userAttributeEntity.setSearchable(true);
        userAttributeEntity.setIsUnique(false);
        List<UserAttributeEntity> userAttributeEntityList = new ArrayList<>();
        userAttributeEntityList.add(userAttributeEntity);
        return userAttributeEntityList;
    }

    @Test
    void editExternalUserSuccessWithPatchAdditonalAttributes() throws IOException, ResourceNotFoundException,
        UserAccountRoleMappingException {

        UserEntity userEntity = new UserEntity();
        userEntity.setFirstName("John");
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setIsExternalUser(true);
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        List<UserAccountRoleMappingEntity> accountRoleMapping = getAccountRoleMapping(userEntity);
        userEntity.setAccountRoleMapping(accountRoleMapping);
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        RoleCreateResponse role = new RoleCreateResponse();
        role.setId(ROLE_ID_1);
        role.setName("BUSINESS_ADMIN");
        role.setDescription("roles");
        Set<RoleCreateResponse> roles = new HashSet<>();
        roles.add(role);
        List<Scope> scopes = new ArrayList<>();
        Scope scope = new Scope();
        scope.setId("1");
        scope.setName("IgniteSystem");
        scopes.add(scope);
        role.setScopes(scopes);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();

        roleRepresentation.setRoles(roles);
        List<ResponseMessage> messages = new ArrayList<>();
        roleRepresentation.setMessages(messages);
        when(rolesService.getRoleById(Set.of(ROLE_ID_1))).thenReturn(roleRepresentation);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn("BUSINESS_ADMIN");
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
            .thenReturn(attributeValueList);
        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
            Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
            .thenReturn(getAdminUserRole());
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);
        List<UserAttributeEntity> userAttributeEntityList = getUserAttributeDetails();
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntityList);
        when(userAttributeRepository.findAllById(any())).thenReturn(userAttributeEntityList);
        UserEntity updatedUserEntity = userEntity;
        updatedUserEntity.setFirstName("JohnSEorro");
        when(userRepository.save(Mockito.any())).thenReturn(updatedUserEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"

            + "{\"op\":\"replace\",\"path\":\"/additionalattribute\",\"value\":\"Attribute\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        UserResponseV1 userResponse = (UserResponseV1) usersService.editUser(USER_ID_VALUE, jsonPatch,
            USER_ID_VALUE, true, "v1");
        assertEquals(updatedUserEntity.getFirstName(), userResponse.getFirstName());
    }

    @Test
    void editExternalUserFailure_WithPatchAdditonalAttributesReadOnly() throws IOException, ResourceNotFoundException,
        UserAccountRoleMappingException {

        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setIsExternalUser(true);
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        List<UserAccountRoleMappingEntity> accountRoleMapping = getAccountRoleMapping(userEntity);
        userEntity.setAccountRoleMapping(accountRoleMapping);
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        RoleCreateResponse role = new RoleCreateResponse();
        role.setId(ROLE_ID_1);
        role.setName("BUSINESS_ADMIN");
        role.setDescription("roles");
        Set<RoleCreateResponse> roles = new HashSet<>();
        roles.add(role);
        List<Scope> scopes = new ArrayList<>();
        Scope scope = new Scope();
        scope.setId("1");
        scope.setName("IgniteSystem");
        scopes.add(scope);
        role.setScopes(scopes);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();

        roleRepresentation.setRoles(roles);
        List<ResponseMessage> messages = new ArrayList<>();
        roleRepresentation.setMessages(messages);
        when(rolesService.getRoleById(Set.of(ROLE_ID_1))).thenReturn(roleRepresentation);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn("BUSINESS_ADMIN");
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
            .thenReturn(attributeValueList);
        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
            Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
            .thenReturn(getAdminUserRole());
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);
        List<UserAttributeEntity> userAttributeEntityList = getUserAttributeDetails();
        userAttributeEntityList.get(0).setReadOnly(true);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntityList);
        when(userAttributeRepository.findAllById(any())).thenReturn(userAttributeEntityList);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"

            + "{\"op\":\"replace\",\"path\":\"/additionalattribute\",\"value\":\"Attribute\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        try {
            usersService.editUser(USER_ID_VALUE, jsonPatch, USER_ID_VALUE, true, "v1");
        } catch (ApplicationRuntimeException exception) {
            assertEquals(BAD_REQUEST, exception.getHttpStatus());
            assertEquals(ACTION_FORBIDDEN, exception.getKey());
            return;
        }
    }

    @Test
    void editExternalUserSuccess_ReplaceRole() throws IOException, ResourceNotFoundException,
        UserAccountRoleMappingException {

        UserEntity userEntity = new UserEntity();
        userEntity.setFirstName("John");
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setIsExternalUser(true);
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        List<UserAccountRoleMappingEntity> accountRoleMapping = getAccountRoleMapping(userEntity);
        userEntity.setAccountRoleMapping(accountRoleMapping);
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        RoleCreateResponse role = new RoleCreateResponse();
        role.setId(ROLE_ID_1);
        role.setName("BUSINESS_ADMIN");
        role.setDescription("roles");
        Set<RoleCreateResponse> roles = new HashSet<>();
        roles.add(role);
        List<Scope> scopes = new ArrayList<>();
        Scope scope = new Scope();
        scope.setId("1");
        scope.setName("IgniteSystem");
        scopes.add(scope);
        role.setScopes(scopes);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();

        roleRepresentation.setRoles(roles);
        List<ResponseMessage> messages = new ArrayList<>();
        roleRepresentation.setMessages(messages);
        when(rolesService.getRoleById(Set.of(ROLE_ID_1))).thenReturn(roleRepresentation);
        when(applicationProperties.getExternalUserPermittedRoles()).thenReturn("BUSINESS_ADMIN");
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
            .thenReturn(attributeValueList);
        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
            Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
            .thenReturn(getAdminUserRole());
        when(applicationProperties.getUserDefaultAccountId()).thenReturn(ACCOUNT_ID_VALUE_1);
        UserEntity updatedUserEntity = userEntity;
        updatedUserEntity.setFirstName("JohnSEorro");
        updatedUserEntity.getUserAddresses().get(0).setAddress1("HELLWORLD2");
        when(userRepository.save(Mockito.any())).thenReturn(updatedUserEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
            + "{\"op\":\"replace\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
            + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        UserResponseV1 userResponse = (UserResponseV1) usersService.editUser(USER_ID_VALUE, jsonPatch,
            USER_ID_VALUE, true, "v1");
        assertEquals(updatedUserEntity.getFirstName(), userResponse.getFirstName());
        assertEquals(updatedUserEntity.getUserAddresses().get(0).getAddress1(), userResponse.getAddress1());
    }

    @Test
    void validateAndCreateAccountRoleMappings()
        throws IOException, UserAccountRoleMappingException, ResourceNotFoundException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readValue(
                "{\"op\":\"remove\",\"path\":\"/account/1/GUEST\",\"value\":\"\"}", JsonNode.class);
        List<JsonNode> opList = new ArrayList<>();
        opList.add(node);

        List<RolesEntity> commonRolesEntities = List.of(
                RoleAssociationUtilities.addRoles("Bussiness_admin", "BUSSINESS_ADMIN", INDEX_1),
                RoleAssociationUtilities.addRoles("Guest", "GUEST", INDEX_2),
                RoleAssociationUtilities.addRoles("Tenant", "TENANT", INDEX_3),
                RoleAssociationUtilities.addRoles("Admin", "ADMIN", INDEX_4),
                RoleAssociationUtilities.addRoles("Vehicle Owner", "VEHICLE_OWNER", INDEX_5));

        List<RolesEntity> userRoles = new ArrayList<>();
        userRoles.add(commonRolesEntities.get(1));
        when(rolesRepository.findByNameInAndIsDeleted(any(HashSet.class), any(Boolean.class)))
            .thenReturn(userRoles);


        List<AccountEntity> accountEntities = List.of(
                RoleAssociationUtilities.addAccount("Ignite_Account", AccountStatus.ACTIVE,
                        Set.of(commonRolesEntities.get(INDEX_0).getId(),
                               commonRolesEntities.get(INDEX_2).getId()), INDEX_1),
                RoleAssociationUtilities.addAccount("UIDAM_Account", AccountStatus.ACTIVE,
                        Set.of(commonRolesEntities.get(INDEX_0).getId(),
                               commonRolesEntities.get(INDEX_1).getId(),
                               commonRolesEntities.get(INDEX_2).getId()), INDEX_2),
                RoleAssociationUtilities.addAccount("AiLabs_Account", AccountStatus.ACTIVE,
                        Set.of(commonRolesEntities.get(INDEX_1).getId(),
                               commonRolesEntities.get(INDEX_3).getId()), INDEX_3),
                RoleAssociationUtilities.addAccount("Analytics_Account", AccountStatus.DELETED,
                        Set.of(commonRolesEntities.get(INDEX_0).getId(),
                               commonRolesEntities.get(INDEX_3).getId(),
                               commonRolesEntities.get(INDEX_4).getId()), INDEX_4));

        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
            .thenReturn(List.of(accountEntities.get(INDEX_0)));

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
               RoleAssociationUtilities.addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        commonRolesEntities.get(INDEX_1).getId(), new BigInteger(ONE)));
        userRoleMappingEntityList.add(
               RoleAssociationUtilities.addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        commonRolesEntities.get(INDEX_1).getId(), new BigInteger(TWO)));

        UserEntity loggedInuserEntity = RoleAssociationUtilities.createUser(
                "Ignite_User", "Ignite_Password", "ignite_admin@harman.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesService.getRoleById(anySet()))
            .thenReturn(RoleAssociationUtilities.createRoleListDtoRepresentation(
                    commonRolesEntities, INDEX_1));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class))).thenReturn(new ArrayList<>());

        UserEntity user = RoleAssociationUtilities.createUser("Ignite_User", "Ignite_Password", "ignite@harman.com");
        user.setId(USER_ID_VALUE);
        user.setAccountRoleMapping(userRoleMappingEntityList);

        List<UserAccountRoleMappingEntity> uarmList = ((UsersServiceImpl) usersService)
                .validateAndCreateAccountRoleMappings(LOGGED_IN_USER_ID_VALUE, user, opList, objectMapper);
        assertEquals(1,  uarmList.size());
        assertEquals(user.getId(), uarmList.get(INDEX_0).getUserId());
        //Only the second role mapping in the user above is retained.
        assertEquals(user.getAccountRoleMapping().get(INDEX_1).getAccountId(), uarmList.get(INDEX_0).getAccountId());
    }


    @Test
    void editUserV2Success()
        throws IOException, UserAccountRoleMappingException, ResourceNotFoundException {
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
                        Set.of(commonRolesEntities.get(INDEX_0).getId(),
                               commonRolesEntities.get(INDEX_2).getId()), INDEX_1),
                RoleAssociationUtilities.addAccount("UIDAM_Account", AccountStatus.ACTIVE,
                        Set.of(commonRolesEntities.get(INDEX_0).getId(),
                               commonRolesEntities.get(INDEX_1).getId(),
                               commonRolesEntities.get(INDEX_2).getId()), INDEX_2),
                RoleAssociationUtilities.addAccount("AiLabs_Account", AccountStatus.ACTIVE,
                        Set.of(commonRolesEntities.get(INDEX_1).getId(),
                               commonRolesEntities.get(INDEX_3).getId()), INDEX_3),
                RoleAssociationUtilities.addAccount("Analytics_Account", AccountStatus.DELETED,
                        Set.of(commonRolesEntities.get(INDEX_0).getId(),
                               commonRolesEntities.get(INDEX_3).getId(),
                               commonRolesEntities.get(INDEX_4).getId()), INDEX_4));

        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
            .thenReturn(List.of(accountEntities.get(INDEX_0)));
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(accountEntities.get(INDEX_0)));

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
               RoleAssociationUtilities.addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        commonRolesEntities.get(INDEX_1).getId(), new BigInteger(ONE)));
        userRoleMappingEntityList.add(
               RoleAssociationUtilities.addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        commonRolesEntities.get(INDEX_1).getId(), new BigInteger(TWO)));

        UserEntity loggedInuserEntity = RoleAssociationUtilities.createUser(
                "Ignite_User", "Ignite_Password", "ignite_admin@harman.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesService.getRoleById(anySet()))
            .thenReturn(RoleAssociationUtilities.createRoleListDtoRepresentation(
                    commonRolesEntities, INDEX_4));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

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
        assertDoesNotThrow(() -> {
            usersService.editUser(USER_ID_VALUE, jsonPatch, USER_ID_VALUE, false, API_VERSION_2);
        });
    }
}
