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
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.BaseResponseFromAuthorization;
import org.eclipse.ecsp.uidam.usermanagement.cache.CacheTokenService;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAddressEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeValueEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeValueRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserEventRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.AuthorizationServerClient;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserEventsDto;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ATTR_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ATTR_ID_VALUE_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ENTITY_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE_3;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE_4;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test to validate UserEvent Class.
 */
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "3600000")
@TestExecutionListeners(listeners = {
    org.eclipse.ecsp.uidam.common.test.TenantContextTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.springframework.context.annotation.Import(org.eclipse.ecsp.uidam.common.test.TestTenantConfiguration.class)
class UserEventTest {
    @MockBean
    UsersRepository usersRepository;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    CacheTokenService cacheTokenService;

    @MockBean
    AuthorizationServerClient authorizationServerClient;

    @MockBean
    private RolesService rolesService;

    @MockBean
    private UserAttributeValueRepository userAttributeValueRepository;

    @MockBean
    private UserAttributeRepository userAttributeRepository;

    @MockBean
    private UserEventRepository userEventRepository;

    @MockBean
    AccountRepository accountRepository;
    
    @MockBean
    PasswordValidationService passwordValidationService;

    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    private static final long ROLE_ID = 2L;

    @BeforeEach
    public void setup() {
        // Configure WebTestClient with default tenantId header for all requests
        webTestClient = webTestClient.mutate()
                .defaultHeader("tenantId", "ecsp")
                .build();
    }

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void addUserEventTestLoginSuccess() {
        UserEventsDto userEventsDto = new UserEventsDto();
        userEventsDto.setEventType("LOGIN_ATTEMPT");
        userEventsDto.setEventStatus("SUCCESS");
        userEventsDto.setEventMessage("Login successfully!");
        when(userEventRepository.save(any())).thenReturn(null);
        webTestClient.post().uri("/v1/users/{id}/events", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(userEventsDto).exchange().expectStatus().isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void addUserEventTestMissingEventsData() {
        UserEventsDto userEventsDto = new UserEventsDto();

        webTestClient.post().uri("/v1/users/{id}/events", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(userEventsDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addUserEventTestLoginFailedEvent() {
        UserEventsDto userEventsDto = new UserEventsDto();
        userEventsDto.setEventType("LOGIN_ATTEMPT");
        userEventsDto.setEventStatus("FAILURE");
        userEventsDto.setEventMessage("Login successfully!");
        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);

        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        when(userEventRepository.save(any())).thenReturn(null);
        when(usersRepository.findById(USER_ID_VALUE)).thenReturn(Optional.of(userEntity));
        webTestClient.post().uri("/v1/users/{id}/events", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(userEventsDto).exchange().expectStatus().isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void addEditUserSuccess() throws IOException {


        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
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
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
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
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
                Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
                .thenReturn(getAdminUserRole());
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
                + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
                + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);

        when(usersRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.OK);
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

    @Test
    void addEditUserSuccessReplaceRole() throws IOException {


        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
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
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
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
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
                Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
                .thenReturn(getAdminUserRole());
        when(cacheTokenService.getAccessToken()).thenReturn("token");
        BaseResponseFromAuthorization authResp = new BaseResponseFromAuthorization();
        authResp.setHttpStatus(HttpStatus.OK);
        when(authorizationServerClient.revokeTokenByAdmin(Mockito.eq("token"), Mockito.anyString()))
                .thenReturn(authResp);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
                + "{\"op\":\"replace\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
                + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);

        when(usersRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void addEditUserExceptionCase() throws IOException {


        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        userEntity.setAccountRoleMapping(getAccountRoleMapping(userEntity));
        ;
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
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
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstNames\",\"value\":\"JohnSEorro\"},"
                + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
                + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addEditUserExceptionCaseResourceNotFound() throws IOException {


        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        userEntity.setAccountRoleMapping(getAccountRoleMapping(userEntity));
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(null);
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
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstNames\",\"value\":\"JohnSEorro\"},"
                + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
                + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addEditUserExceptionCaseCheckForbiddenField() throws IOException {


        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        userEntity.setAccountRoleMapping(getAccountRoleMapping(userEntity));
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
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
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/userName\",\"value\":\"JohnSEorro\"},"
                + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
                + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addEditUserExceptionCaseCheckUserAuthorized() throws IOException {

        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
        UserEntity userEntity1 = new UserEntity();
        userEntity1.setStatus(UserStatus.ACTIVE);
        userEntity1.setId(USER_ENTITY_ID_VALUE);
        userEntity1.setUserName("test");
        userEntity1.setUserPassword("usp");
        Set<BigInteger> roleId = new HashSet<>();
        roleId.add(ROLE_ID_1);
        userEntity1.setAccountRoleMapping(getAccountRoleMapping(userEntity1));
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<BigInteger> roleIds = new HashSet<>();
        roleIds.add(ROLE_ID_2);
        userEntity.setAccountRoleMapping(getAccountRoleMapping(userEntity));
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        userEntity1.setUserAddresses(addresses);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE_4, UserStatus.DELETED)).thenReturn(userEntity1);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();
        roleRepresentation.setRoles(getUserRole());
        when(rolesService.getRoleById(Set.of(ROLE_ID_1))).thenReturn(roleRepresentation);
        when(rolesService.getRoleById(Set.of(ROLE_ID_2))).thenReturn(getAdminUserRole());
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        Set<String> roleName = new HashSet<>();
        roleName.add("TENANT_ADMIN");
        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
                Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
                .thenReturn(getAdminUserRole());
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/userName\",\"value\":\"JohnSEorro\"},"
                + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
                + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addEditUserExceptionCaseCheckUserNotAuthorized() throws IOException {

        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
        UserEntity userEntity1 = new UserEntity();
        userEntity1.setStatus(UserStatus.ACTIVE);

        userEntity1.setId(USER_ENTITY_ID_VALUE);
        userEntity1.setUserName("test");
        userEntity1.setUserPassword("usp");
        Set<Long> roleId = new HashSet<>();
        roleId.add(ROLE_ID);
        userEntity1.setAccountRoleMapping(getAccountRoleMapping(userEntity1));
        ;
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        userEntity.setAccountRoleMapping(getAccountRoleMapping(userEntity));
        ;
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        userEntity1.setUserAddresses(addresses);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE_3, UserStatus.DELETED)).thenReturn(userEntity1);
        RoleListRepresentation roleRepresentation = new RoleListRepresentation();
        roleRepresentation.setRoles(getUserRole());
        when(rolesService.getRoleById(Set.of(ROLE_ID_1))).thenReturn(roleRepresentation);
        when(rolesService.getRoleById(Set.of(ROLE_ID_2))).thenReturn(getAdminUserRole());
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);

        when(rolesService.filterRoles(Mockito.any(), Mockito.eq(Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT)),
                Mockito.eq(Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT)), Mockito.eq(false)))
                .thenReturn(getAdminUserRole());
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/userName\",\"value\":\"JohnSEorro\"},"
                + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
                + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addEditUserSuccessWithPatchAdditonalAttributes() throws IOException {

        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");

        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        userEntity.setAccountRoleMapping(getAccountRoleMapping(userEntity));
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
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
        List<UserAttributeValueEntity> attributeValueList = getUserAttributeValueDetails(String.valueOf(USER_ID_VALUE));
        when(userAttributeValueRepository.findAllByUserIdAndAttributeIdIn(userEntity.getId(),
                List.of(USER_ENTITY_ID_VALUE))).thenReturn(attributeValueList);
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        List<UserAttributeEntity> userAttributeEntityList = getUserAttributeDetails();
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntityList);
        when(userAttributeRepository.findAllById(any())).thenReturn(userAttributeEntityList);
        when(usersRepository.save(Mockito.any())).thenReturn(userEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"

                + "{\"op\":\"replace\",\"path\":\"/additionalattribute\",\"value\":\"Attribute\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    private Set<RoleCreateResponse> getUserRole() {
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
        return roles;
    }

    @Test
    void addEditUserSuccessWithAdditonalAttributes() throws IOException {

        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        userEntity.setAccountRoleMapping(getAccountRoleMapping(userEntity));
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
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
        List<UserAttributeValueEntity> attributeValueList = getUserAttributeValueDetails(String.valueOf(USER_ID_VALUE));
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        List<UserAttributeEntity> userAttributeEntityList = getUserAttributeDetails();
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntityList);
        when(userAttributeRepository.findAllById(any())).thenReturn(userAttributeEntityList);
        when(usersRepository.save(Mockito.any())).thenReturn(userEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"

                + "{\"op\":\"replace\",\"path\":\"/additionalattribute\",\"value\":\"Attribute\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void addEditUserExceptionWithAdditonalAttributesReadOnly() throws IOException {

        UserEntity userEntity = new UserEntity();
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setId(USER_ID_VALUE);
        userEntity.setUserName("test");
        userEntity.setUserPassword("usp");
        UserAddressEntity userAddress = new UserAddressEntity();
        userAddress.setAddress1("address1");
        userAddress.setCity("City");
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1L);
        userEntity.setAccountRoleMapping(getAccountRoleMapping(userEntity));
        List<UserAddressEntity> addresses = new ArrayList<>();
        addresses.add(userAddress);
        userEntity.setUserAddresses(addresses);
        when(usersRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
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
        List<UserAttributeValueEntity> attributeValueList = getUserAttributeValueDetails(String.valueOf(USER_ID_VALUE));
        when(userAttributeValueRepository.findAllByUserIdIn(List.of(USER_ID_VALUE)))
                .thenReturn(attributeValueList);
        List<UserAttributeEntity> userAttributeEntityList = getUserAttributeDetails();
        userAttributeEntityList.get(0).setReadOnly(true);
        when(userAttributeRepository.findAll()).thenReturn(userAttributeEntityList);
        when(userAttributeRepository.findAllById(List.of(ATTR_ID_VALUE_1))).thenReturn(userAttributeEntityList);
        when(usersRepository.save(Mockito.any())).thenReturn(userEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"

                + "{\"op\":\"replace\",\"path\":\"/additionalattribute\",\"value\":\"Attribute\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);
        webTestClient.patch().uri("/v1/users/{id}", USER_ID_VALUE).headers(http -> {
            http.add("Content-Type", "application/json-patch+json");
            http.add("user-id", String.valueOf(USER_ID_VALUE));
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(jsonPatch).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private List<UserAttributeValueEntity> getUserAttributeValueDetails(String userId) {
        UserAttributeValueEntity userAttributeValueEntity = new UserAttributeValueEntity();
        userAttributeValueEntity.setAttributeId(ATTR_ID_VALUE);
        userAttributeValueEntity.setId(ATTR_ID_VALUE);
        userAttributeValueEntity.setValue("additional");
        userAttributeValueEntity.setUserId(USER_ID_VALUE);
        List<UserAttributeValueEntity> attributeValueList = new ArrayList<>();
        attributeValueList.add(userAttributeValueEntity);
        return attributeValueList;
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

    private RoleListRepresentation getAdminUserRole() {
        RoleCreateResponse role = new RoleCreateResponse();
        role.setId(ROLE_ID_1);
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
}
