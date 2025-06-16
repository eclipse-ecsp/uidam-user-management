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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailVerificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.ExternalUserDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.FederatedUserDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserChangeStatusRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV1;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersDeleteFilter;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserDetailsResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserMetaDataResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.DESCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IGNORE_CASE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_NUMBER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_NUMBER_DEFAULT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_SIZE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_SIZE_DEFAULT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_BY_USERNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_CHANGE_STATUS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_FEDERATED_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_FILTER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_USER_ATTRIBUTES;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SEARCH_TYPE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SLASH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TENANT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERS_SELF_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.VERSION_V1;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;
import static org.eclipse.ecsp.uidam.usermanagement.enums.Gender.MALE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ADDRESS1_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ADDRESS2_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.CITY_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.COUNTRY_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.DEV_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.EMAIL_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.FIRST_NAME_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LAST_NAME_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOCALE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PHONE_NUMBER_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.POSTAL_CODE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.STATE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_NAME_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Utilities.asJsonString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test cases for UsersController.
 */
@Slf4j
@WebMvcTest({ UsersController.class })
@MockBean(JpaMetamodelMappingContext.class)
public class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersController usersController;

    @MockBean
    private UsersService usersService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    PasswordValidationService passwordValidationService;
    
    private static final String API_VERSION_1 = "v1";

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    /**
     * Method to return mock data response of user metadata.
     *
     * @return UserMetaDataResponse
     */
    public static UserMetaDataResponse getUserMetaDataResponse() {
        return UserMetaDataResponse.builder().name("attribute1").mandatory(Boolean.TRUE).unique(Boolean.FALSE)
                .readOnly(Boolean.FALSE).searchable(Boolean.TRUE).dynamicAttribute(Boolean.FALSE).type("String")
                .regex(".*").build();
    }

    /**
     * Create mock user filter request.
     *
     * @return UsersGetFilter
     */
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

    /**
     * Return mock user response data.
     *
     * @return UserResponse
     */
    public static UserResponseBase getUserResponse() {
        UserResponseV1 userResponse = new UserResponseV1();
        userResponse.setUserName("johnd");
        userResponse.setStatus(UserStatus.ACTIVE);
        userResponse.setEmail("john.doe@domain.com");
        return userResponse;
    }

    @Test
    @Order(1)
    void contextLoad() {
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        map.forEach((key, value) -> log.info("{} {}", key, value));
        Assert.notEmpty(map, "Handler mappings map is empty. Controller APIs are not exposed.");
        Assert.notNull(usersController, "UsersController not created");
        Assert.notNull(usersService, "usersService not created");
    }

    @Test
    void addUserSuccess() throws Exception {
        when(usersService.addUser(any(UserDtoV1.class), any(BigInteger.class), eq(false))).thenReturn(
            getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getUserDto()))
                .header(CORRELATION_ID, "12345")
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isCreated()).andExpectAll(
                MockMvcResultMatchers.jsonPath("$.userName").value("johnd"),
                MockMvcResultMatchers.jsonPath("$.status").value(UserStatus.ACTIVE.name()),
                MockMvcResultMatchers.jsonPath("$.email").value("john.doe@domain.com"));

        verify(emailVerificationService, times(1)).resendEmailVerification(any(UserResponseV1.class));
    }

    @Test
    void addUserCorrelationIdMissing() throws Exception {
        when(usersService.addUser(any(UserDtoV1.class), any(BigInteger.class), eq(false))).thenReturn(
                getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(getUserDto()))
                    .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                    .header(TENANT_ID, "tenant1"))
                .andExpect(status().isCreated()).andExpectAll(
                    MockMvcResultMatchers.jsonPath("$.userName").value("johnd"),
                    MockMvcResultMatchers.jsonPath("$.status").value(UserStatus.ACTIVE.name()),
                    MockMvcResultMatchers.jsonPath("$.email").value("john.doe@domain.com"));

        verify(emailVerificationService, times(1)).resendEmailVerification(any(UserResponseV1.class));
    }

    @Test
    void addUserMissingMandatoryParam() throws Exception {
        when(usersService.addUser(any(UserDtoV1.class), any(BigInteger.class), eq(false))).thenReturn(
            getUserResponse());
        UserDtoV1 userDto = getUserDto();
        userDto.setRoles(null);
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(userDto))
                .header(CORRELATION_ID, "12345")
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.roles", is(MISSING_MANDATORY_PARAMETERS)));

        verify(emailVerificationService, times(0)).resendEmailVerification(any(UserResponseV1.class));
    }

    @Test
    void getUserSuccess() throws Exception {
        when(usersService.getUser(any(BigInteger.class), eq(API_VERSION_1))).thenReturn(
            getUserResponse());

        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + SLASH + USER_ID_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID, "12345")
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk())
            .andExpectAll(
                MockMvcResultMatchers.jsonPath("$.userName").value("johnd"),
                MockMvcResultMatchers.jsonPath("$.status").value(UserStatus.ACTIVE.name()),
                MockMvcResultMatchers.jsonPath("$.email").value("john.doe@domain.com"));
    }

    @Test
    void getExternalUserSuccess() throws Exception {
        when(usersService.getUser(any(BigInteger.class), eq(API_VERSION_1))).thenReturn(
            getUserResponse());

        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + PATH_EXTERNAL_USER + SLASH + USER_ID_VALUE)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .header(CORRELATION_ID, "12345")
                            .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk())
            .andExpectAll(
                MockMvcResultMatchers.jsonPath("$.userName").value("johnd"),
                MockMvcResultMatchers.jsonPath("$.status").value(UserStatus.ACTIVE.name()),
                MockMvcResultMatchers.jsonPath("$.email").value("john.doe@domain.com"));
    }

    @Test
    void getUserSuccessCorrelationIdMissing() throws Exception {
        when(usersService.getUser(any(BigInteger.class), eq(API_VERSION_1))).thenReturn(
            getUserResponse());

        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + SLASH + USER_ID_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk());
    }

    @Test
    void getUserInvalidUserId() throws Exception {
        when(usersService.getUser(any(BigInteger.class), eq(API_VERSION_1))).thenReturn(
            getUserResponse());
        String userId = "shjgakhfsdahy";
        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + SLASH + userId)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID, "12345")
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isBadRequest())
            .andExpectAll(
                MockMvcResultMatchers.jsonPath("$.detail").value("Failed to convert 'id' "
                                                                     + "with value: 'shjgakhfsdahy'"));
    }

    @Test
    void getUserDetailsNotFound() throws Exception {

        when(usersService.getUser(any(BigInteger.class), eq(API_VERSION_1)))
                .thenThrow(new ResourceNotFoundException(USER, "userId", String.valueOf(USER_ID_VALUE)));
        mockMvc.perform(
                get(VERSION_V1 + USER_RESOURCE_PATH + SLASH + USER_ID_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header(CORRELATION_ID, "12345").header(TENANT_ID, "tenant1").header("user_id", "DummyUser"))
                .andExpect(status().isNotFound()).andExpectAll(
                        MockMvcResultMatchers.jsonPath("$.message")
                        .value("User not found for " + "userId: " + USER_ID_VALUE));
    }

    @Test
    void getUsers() throws Exception {
        when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), anyString(),
            anyString(), anyBoolean(), any())).thenReturn(List.of(getUserResponse()));

        this.mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_FILTER)
                .contentType(APPLICATION_JSON)
                .content(asJsonString(createUsersGetFilter()))
                .header(CORRELATION_ID, "123")
                .header(TENANT_ID, "tenant1")
                .param(PAGE_NUMBER, PAGE_NUMBER_DEFAULT)
                .param(PAGE_SIZE, PAGE_SIZE_DEFAULT)
                .param(SORT_ORDER, DESCENDING)
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
            .andExpect(status().isOk());
    }

    @Test
    void getUsersCorrelationIdMissing() throws Exception {
        when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), anyString(),
            anyString(), anyBoolean(), any())).thenReturn(List.of(getUserResponse()));

        this.mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_FILTER)
                .contentType(APPLICATION_JSON)
                .content(asJsonString(createUsersGetFilter()))
                .header(TENANT_ID, "tenant1")
                .param(PAGE_NUMBER, PAGE_NUMBER_DEFAULT)
                .param(PAGE_SIZE, PAGE_SIZE_DEFAULT)
                .param(SORT_ORDER, DESCENDING)
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
            .andExpect(status().isOk());
    }

    @Test
    void getUsersWithIgnoreCase() throws Exception {
        when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), anyString(),
            anyString(), anyBoolean(), any())).thenReturn(List.of(getUserResponse()));

        this.mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_FILTER)
                .contentType(APPLICATION_JSON)
                .content(asJsonString(createUsersGetFilter()))
                .header(CORRELATION_ID, "123")
                .header(TENANT_ID, "tenant1")
                .param(PAGE_NUMBER, PAGE_NUMBER_DEFAULT)
                .param(PAGE_SIZE, PAGE_SIZE_DEFAULT)
                .param(SORT_ORDER, DESCENDING)
                .param(IGNORE_CASE, "true")
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
            .andExpect(status().isOk());
    }

    @Test
    void getUsersWithContains() throws Exception {
        when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), anyString(),
            anyString(), anyBoolean(), any())).thenReturn(List.of(getUserResponse()));

        this.mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_FILTER)
                .contentType(APPLICATION_JSON)
                .content(asJsonString(createUsersGetFilter()))
                .header(CORRELATION_ID, "123")
                .header(TENANT_ID, "tenant1")
                .param(PAGE_NUMBER, PAGE_NUMBER_DEFAULT)
                .param(PAGE_SIZE, PAGE_SIZE_DEFAULT)
                .param(SORT_ORDER, DESCENDING)
                .param(IGNORE_CASE, "true")
                .param(SEARCH_TYPE, SearchType.CONTAINS.toString())
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
            .andExpect(status().isOk());
    }

    @Test
    void getUsersWithPrefix() throws Exception {
        when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), anyString(),
            anyString(), anyBoolean(), any())).thenReturn(List.of(getUserResponse()));

        this.mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_FILTER)
                .contentType(APPLICATION_JSON)
                .content(asJsonString(createUsersGetFilter()))
                .header(CORRELATION_ID, "123")
                .header(TENANT_ID, "tenant1")
                .param(PAGE_NUMBER, PAGE_NUMBER_DEFAULT)
                .param(PAGE_SIZE, PAGE_SIZE_DEFAULT)
                .param(SORT_ORDER, DESCENDING)
                .param(IGNORE_CASE, "true")
                .param(SEARCH_TYPE, SearchType.PREFIX.toString())
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
            .andExpect(status().isOk());
    }

    @Test
    void getUsersWithSuffix() throws Exception {
        when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), anyString(),
            anyString(), anyBoolean(), any())).thenReturn(List.of(getUserResponse()));

        this.mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_FILTER)
                .contentType(APPLICATION_JSON)
                .content(asJsonString(createUsersGetFilter()))
                .header(CORRELATION_ID, "123")
                .header(TENANT_ID, "tenant1")
                .param(PAGE_NUMBER, PAGE_NUMBER_DEFAULT)
                .param(PAGE_SIZE, PAGE_SIZE_DEFAULT)
                .param(SORT_ORDER, DESCENDING)
                .param(IGNORE_CASE, "true")
                .param(SEARCH_TYPE, SearchType.SUFFIX.toString())
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
            .andExpect(status().isOk());
    }

    @Test
    void deleteUserFailure() throws Exception {

        when(usersService.deleteUser(any(BigInteger.class), any(Boolean.class), any(BigInteger.class)))
                .thenThrow(new ResourceNotFoundException(USER, "userId", String.valueOf(USER_ID_VALUE)));
        this.mockMvc
                .perform(delete(VERSION_V1 + USER_RESOURCE_PATH + SLASH + USER_ID_VALUE)
                        .contentType(APPLICATION_JSON)
                        .header(CORRELATION_ID, "123").header(EXTERNAL_USER, false)
                    .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                        .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
                .andExpect(status().isNotFound()).andExpectAll(
                        MockMvcResultMatchers.jsonPath("$.message").value("User not found for " + "userId: "
                + USER_ID_VALUE));
    }

    @Test
    void deleteUserSuccess() throws Exception {
        UserResponseV1 userResponse = (UserResponseV1) getUserResponse();
        userResponse.setStatus(UserStatus.DEACTIVATED);
        when(usersService.deleteUser(any(BigInteger.class), any(Boolean.class), any(BigInteger.class)))
                .thenReturn(userResponse);
        this.mockMvc
                .perform(delete(VERSION_V1 + USER_RESOURCE_PATH + SLASH + USER_ID_VALUE_2).contentType(APPLICATION_JSON)
                        .header(CORRELATION_ID, "123").header(EXTERNAL_USER, false)
                        .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                        .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
                .andExpect(status().isOk())
                .andExpectAll(MockMvcResultMatchers.jsonPath("$.status").value("DEACTIVATED"));
    }

    @Test
    void deleteUserCorrelationIdMissing() throws Exception {
        UserResponseV1 userResponse = (UserResponseV1) getUserResponse();
        userResponse.setStatus(UserStatus.DEACTIVATED);
        when(usersService.deleteUser(any(BigInteger.class), any(Boolean.class), any())).thenReturn(userResponse);
        this.mockMvc.perform(delete(VERSION_V1 + USER_RESOURCE_PATH + SLASH + USER_ID_VALUE)
                .contentType(APPLICATION_JSON).header(EXTERNAL_USER, false).header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE))).andExpect(status().isOk());
    }

    @Test
    void getUserByUserNameSuccess() throws Exception {
        when(usersService.getUserByUserName(any(String.class))).thenReturn(
            getUserDetailsResponse());
        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + "/johnd" + PATH_BY_USERNAME)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID, "12345"))
            .andExpect(status().isOk())
            .andExpectAll(
                MockMvcResultMatchers.jsonPath("$.userName").value("johnd"),
                MockMvcResultMatchers.jsonPath("$.status").value(UserStatus.ACTIVE.name()),
                MockMvcResultMatchers.jsonPath("$.passwordEncoder").value("SHA-256"));
    }

    @Test
    void getSelfUserSuccess() throws Exception {
        when(usersService.getUser(any(BigInteger.class), eq(API_VERSION_1))).thenReturn(
            getUserResponse());
        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID, "12345")
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk())
            .andExpectAll(
                MockMvcResultMatchers.jsonPath("$.userName").value("johnd"),
                MockMvcResultMatchers.jsonPath("$.status").value(UserStatus.ACTIVE.name()),
                MockMvcResultMatchers.jsonPath("$.email").value("john.doe@domain.com"));
    }

    @Test
    void getSelfUserCorrelationIdMissing() throws Exception {
        when(usersService.getUser(any(BigInteger.class), eq(API_VERSION_1))).thenReturn(
            getUserResponse());
        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk());
    }

    @Test
    void deleteSelfUserSuccess() throws Exception {
        UserResponseV1 userResponse = (UserResponseV1) getUserResponse();
        userResponse.setStatus(UserStatus.DEACTIVATED);
        when(usersService.deleteUser(any(BigInteger.class), any(Boolean.class), any())).thenReturn(userResponse);
        this.mockMvc
                .perform(delete(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH).contentType(APPLICATION_JSON)
                        .header(CORRELATION_ID, "123").header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                        .header(EXTERNAL_USER, false).accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
                .andExpect(status().isOk())
                .andExpectAll(MockMvcResultMatchers.jsonPath("$.status").value("DEACTIVATED"));
    }

    @Test
    void deleteSelfUserCorrelationIdMissing() throws Exception {
        UserResponseV1 userResponse = (UserResponseV1) getUserResponse();
        userResponse.setStatus(UserStatus.DEACTIVATED);
        when(usersService.deleteUser(any(BigInteger.class), any(Boolean.class), any(BigInteger.class)))
                .thenReturn(userResponse);
        this.mockMvc.perform(delete(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH).contentType(APPLICATION_JSON)
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE).header(EXTERNAL_USER, false)
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE))).andExpect(status().isOk());
    }

    @Test
    void getUserAttributesSuccess() throws Exception {
        when(usersService.getUserMetaData()).thenReturn(List.of(getUserMetaDataResponse()));
        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + PATH_USER_ATTRIBUTES)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID, "12345")
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk())
            .andExpectAll(
                MockMvcResultMatchers.jsonPath("$[0].name").value("attribute1"),
                MockMvcResultMatchers.jsonPath("$[0].mandatory").value(Boolean.TRUE),
                MockMvcResultMatchers.jsonPath("$[0].unique").value(Boolean.FALSE),
                MockMvcResultMatchers.jsonPath("$[0].readOnly").value(Boolean.FALSE),
                MockMvcResultMatchers.jsonPath("$[0].searchable").value(Boolean.TRUE),
                MockMvcResultMatchers.jsonPath("$[0].dynamicAttribute").value(Boolean.FALSE),
                MockMvcResultMatchers.jsonPath("$[0].type").value("String"),
                MockMvcResultMatchers.jsonPath("$[0].regex").value(".*"));
    }

    @Test
    void getUserAttributesCorrelationIdMissing() throws Exception {
        when(usersService.getUserMetaData()).thenReturn(List.of(getUserMetaDataResponse()));
        mockMvc.perform(get(VERSION_V1 + USER_RESOURCE_PATH + PATH_USER_ATTRIBUTES)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk());
    }

    @Test
    void putUserAttributesSuccess() throws Exception {
        when(usersService.putUserMetaData(anyList())).thenReturn(List.of(getUserMetaDataResponse()));
        String content = "[\n"
            + "  {\n"
            + "    \"name\": \"isMarried\",\n"
            + "    \"mandatory\": false,\n"
            + "    \"unique\": false,\n"
            + "    \"readOnly\": false,\n"
            + "    \"searchable\": true,\n"
            + "    \"type\": \"TEXT\",\n"
            + "    \"regex\": \"[a-zA-Z]{1,13}\"\n"
            + "  }\n"
            + "]";
        mockMvc.perform(put(VERSION_V1 + USER_RESOURCE_PATH + PATH_USER_ATTRIBUTES)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(content)
                .header(CORRELATION_ID, "12345")
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk())
            .andExpectAll(
                MockMvcResultMatchers.jsonPath("$[0].name").value("attribute1"),
                MockMvcResultMatchers.jsonPath("$[0].mandatory").value(Boolean.TRUE),
                MockMvcResultMatchers.jsonPath("$[0].unique").value(Boolean.FALSE),
                MockMvcResultMatchers.jsonPath("$[0].readOnly").value(Boolean.FALSE),
                MockMvcResultMatchers.jsonPath("$[0].searchable").value(Boolean.TRUE),
                MockMvcResultMatchers.jsonPath("$[0].dynamicAttribute").value(Boolean.FALSE),
                MockMvcResultMatchers.jsonPath("$[0].type").value("String"),
                MockMvcResultMatchers.jsonPath("$[0].regex").value(".*"));
    }

    @Test
    void putUserAttributesCorrelationIdMissing() throws Exception {
        when(usersService.putUserMetaData(anyList())).thenReturn(List.of(getUserMetaDataResponse()));
        String content = "[\n"
            + "  {\n"
            + "    \"name\": \"isMarried\",\n"
            + "    \"mandatory\": false,\n"
            + "    \"unique\": false,\n"
            + "    \"readOnly\": false,\n"
            + "    \"searchable\": true,\n"
            + "    \"type\": \"TEXT\",\n"
            + "    \"regex\": \"[a-zA-Z]{1,13}\"\n"
            + "  }\n"
            + "]";
        mockMvc.perform(put(VERSION_V1 + USER_RESOURCE_PATH + PATH_USER_ATTRIBUTES)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(content)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk());
    }

    @Test
    void deleteUsersByFilterSuccess() throws Exception {
        List<UserResponseV1> list = new ArrayList<>();
        UserResponseV1 userResponse = (UserResponseV1) getUserResponse();
        userResponse.setStatus(UserStatus.DELETED);
        list.add(userResponse);
        when(usersService.deleteUsers(any(UsersDeleteFilter.class))).thenReturn(list);
        this.mockMvc.perform(delete(VERSION_V1 + USER_RESOURCE_PATH).contentType(APPLICATION_JSON)
                .content(asJsonString(getUsersDeleteFilter())).header(CORRELATION_ID, "123")
                .accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE))).andExpect(status().isOk());
    }

    private UserDtoV1 getUserDto() {
        UserDtoV1 userDto = new UserDtoV1();
        userDto.setUserName("johnd");
        userDto.setIsExternalUser(false);
        userDto.setRoles(Collections.singleton("VEHICLE_OWNER"));
        userDto.setPassword("Asxuzzs#2391");
        userDto.setStatus(UserStatus.ACTIVE);
        userDto.setEmail("john.doe@domain.com");
        return userDto;
    }

    private UserDetailsResponse getUserDetailsResponse() {
        UserDetailsResponse userResponse = new UserDetailsResponse();
        userResponse.setUserName("johnd");
        userResponse.setStatus(UserStatus.ACTIVE.name());
        userResponse.setPasswordEncoder("SHA-256");
        userResponse.setPassword("pashghdfayshhs7865");
        userResponse.setSalt("asgfajdgs==");
        return userResponse;
    }

    private UsersDeleteFilter getUsersDeleteFilter() {
        UsersDeleteFilter usersDeleteFilter = new UsersDeleteFilter();
        usersDeleteFilter.setIds(Set.of(USER_ID_VALUE));
        return usersDeleteFilter;
    }

    @Test
    void changeUserStatusSuccess() throws Exception {
        when(usersService.changeUserStatus(any(UserChangeStatusRequest.class), any()))
            .thenReturn(List.of((UserResponseV1) getUserResponse()));

        UserChangeStatusRequest userChangeStatusRequest = new UserChangeStatusRequest();
        userChangeStatusRequest.setIds(Set.of(USER_ID_VALUE));
        userChangeStatusRequest.setApproved(true);

        mockMvc.perform(patch(VERSION_V1 + USER_RESOURCE_PATH + PATH_CHANGE_STATUS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(asJsonString(userChangeStatusRequest))
            .header(CORRELATION_ID, "12345")
            .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
        ).andExpect(status().isOk());
    }

    private UserDtoV1 getSelfUserDto() {
        UserDtoV1 userDto = new UserDtoV1();
        userDto.setUserName("johnd");
        userDto.setPassword("Asxuzzs#2391");
        userDto.setIsExternalUser(false);
        userDto.setRoles(Collections.singleton("VEHICLE_OWNER"));
        userDto.setStatus(UserStatus.ACTIVE);
        userDto.setEmail("john.doe@domain.com");
        return userDto;
    }
    
    private UserDtoV1 getExternalUserDto() {
        UserDtoV1 userDto = new UserDtoV1();
        userDto.setUserName("johnd");
        userDto.setIsExternalUser(true);
        userDto.setRoles(Collections.singleton("VEHICLE_OWNER"));
        userDto.setStatus(UserStatus.ACTIVE);
        userDto.setEmail("john.doe@domain.com");
        return userDto;
    }

    @Test
    void addExternalUserSuccess() throws Exception {
        when(usersService.addExternalUser(any(ExternalUserDto.class), any(BigInteger.class))).thenReturn(
            getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_EXTERNAL_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getExternalUserDto()))
                .header(CORRELATION_ID, "12345")
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk()).andExpectAll(
                MockMvcResultMatchers.jsonPath("$.userName").value("johnd"),
                MockMvcResultMatchers.jsonPath("$.status").value(UserStatus.ACTIVE.name()),
                MockMvcResultMatchers.jsonPath("$.email").value("john.doe@domain.com"));
    }

    @Test
    void deleteExternalUserSuccess() throws Exception {
        UserResponseV1 userResponse = (UserResponseV1) getUserResponse();
        userResponse.setStatus(UserStatus.DELETED);
        when(usersService.deleteExternalUser(any(BigInteger.class), any(BigInteger.class))).thenReturn(userResponse);
        this.mockMvc.perform(delete(VERSION_V1 + USER_RESOURCE_PATH + PATH_EXTERNAL_USER + SLASH + USER_ID_VALUE)
                .contentType(APPLICATION_JSON).header(CORRELATION_ID, "123").header(EXTERNAL_USER, false)
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE).accept(MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE)))
                .andExpect(status().isOk()).andExpectAll(MockMvcResultMatchers.jsonPath("$.status").value("DELETED"));
    }

    @Test
    void addExternalUserPasswordValidation() throws Exception {
        UserDtoV1 userDto = getExternalUserDto();
        userDto.setIsExternalUser(true);
        userDto.setPassword("TestPass@22");
        when(usersService.addExternalUser(any(ExternalUserDto.class), any(BigInteger.class)))
                .thenReturn(getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_EXTERNAL_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(asJsonString(userDto))
                .header(CORRELATION_ID, "12345").header(LOGGED_IN_USER_ID, USER_ID_VALUE).header(TENANT_ID, "tenant1"))
                .andExpect(status().isOk());
    }

    @Test
    void addUserNullPasswordValidation() throws Exception {
        UserDtoV1 userDto = getExternalUserDto();
        userDto.setIsExternalUser(false);
        userDto.setPassword(null);
        when(usersService.addExternalUser(any(UserDtoV1.class), any(BigInteger.class))).thenReturn(getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH).contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(userDto)).header(CORRELATION_ID, "12345").header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .header(TENANT_ID, "tenant1")).andExpect(status().isBadRequest());
    }

    @Test
    void addUserWrongPatternPasswordValidation() throws Exception {
        UserDtoV1 userDto = getExternalUserDto();
        userDto.setIsExternalUser(false);
        when(usersService.addExternalUser(any(UserDtoV1.class), any(BigInteger.class))).thenReturn(getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH).contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(userDto)).header(CORRELATION_ID, "12345").header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .header(TENANT_ID, "tenant1")).andExpect(status().isBadRequest());
    }

    @Test
    void addExternalUserInvalidPasswordValidation() throws Exception {
        UserDtoV1 userDto = getExternalUserDto();
        userDto.setIsExternalUser(true);
        userDto.setPassword("pass");
        when(usersService.addExternalUser(any(UserDtoV1.class), any(BigInteger.class))).thenReturn(getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_EXTERNAL_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(asJsonString(userDto))
                .header(CORRELATION_ID, "12345").header(LOGGED_IN_USER_ID, USER_ID_VALUE).header(TENANT_ID, "tenant1"))
                .andExpect(status().isOk());
    }

    private UserDtoV1 getFederatedUserDto() {
        FederatedUserDto federatedUserDto = new FederatedUserDto();
        federatedUserDto.setUserName("johnd");
        federatedUserDto.setIsExternalUser(true);
        federatedUserDto.setRoles(Collections.singleton("VEHICLE_OWNER"));
        federatedUserDto.setEmail("john.doe@domain.com");
        federatedUserDto.setIdentityProviderName("google");
        return federatedUserDto;
    }

    @Test
    void addFederatedUserSuccess() throws Exception {
        when(usersService.addFederatedUser(any(FederatedUserDto.class), any(BigInteger.class))).thenReturn(
            getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_FEDERATED_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getFederatedUserDto()))
                .header(CORRELATION_ID, "12345")
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk());
    }

    private UserDtoV1 getFederatedUserDtoMissingIdp() {
        FederatedUserDto federatedUserDto = new FederatedUserDto();
        federatedUserDto.setUserName("johnd");
        federatedUserDto.setIsExternalUser(true);
        federatedUserDto.setRoles(Collections.singleton("VEHICLE_OWNER"));
        federatedUserDto.setEmail("john.doe@domain.com");
        return federatedUserDto;
    }

    @Test
    void addFederatedUser_MissingIdp() throws Exception {
        when(usersService.addFederatedUser(any(FederatedUserDto.class), any(BigInteger.class))).thenReturn(
            getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + PATH_FEDERATED_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getFederatedUserDtoMissingIdp()))
                .header(CORRELATION_ID, "12345")
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
                .header(TENANT_ID, "tenant1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void editExternalUserSuccess() throws Exception {
        UserResponseV1 userResponse = (UserResponseV1) getUserResponse();
        ObjectMapper objectMapper = new ObjectMapper();
        String decryptValue = "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"JohnSEorro\"},"
            + "{\"op\":\"add\",\"path\":\"/roles\",\"value\":[\"BUSINESS_ADMIN\"]},"
            + "{\"op\":\"replace\",\"path\":\"/address1\",\"value\":\"HELLWORLD2\"}]";
        JsonNode node = objectMapper.readValue(decryptValue, JsonNode.class);
        JsonPatch jsonPatch = JsonPatch.fromJson(node);

        when(usersService.editUser(any(BigInteger.class), any(JsonPatch.class), any(BigInteger.class),
                any(Boolean.class), any(String.class)))
            .thenReturn(userResponse);
        this.mockMvc.perform(patch(VERSION_V1 + USER_RESOURCE_PATH + PATH_EXTERNAL_USER + SLASH + USER_ID_VALUE)
                .contentType("application/json-patch+json").content(asJsonString(jsonPatch))
                .header(LOGGED_IN_USER_ID, USER_ID_VALUE))
            .andExpect(status().isOk());
    }

    @Test
    void selfAddUserSuccess() throws Exception {
        when(usersService.addUser(any(UserDtoV1.class), eq(null), eq(true))).thenReturn(getUserResponse());
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getSelfUserDto())))
            .andExpect(status().isCreated());
    }

    @Test
    void selfAddUserAlreadyExists() throws Exception {
        when(usersService.addUser(any(UserDtoV1.class), eq(null),
            eq(true))).thenThrow(new RecordAlreadyExistsException("User already exists"));
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getSelfUserDto())))
            .andExpect(status().isConflict());
    }

    @Test
    void selfAddUserInvalidData() throws Exception {
        UserDtoV1 userDto = getExternalUserDto();
        userDto.setEmail("invalid-email");
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(userDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void selfAddUserMissingMandatoryField() throws Exception {
        UserDtoV1 userDto = getExternalUserDto();
        userDto.setUserName(null);
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(userDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void selfAddUserApplicationRuntimeException() throws Exception {
        when(usersService.addUser(any(UserDtoV1.class), eq(null), eq(true)))
            .thenThrow(new RuntimeException("Generic exception"));
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getExternalUserDto())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void selfAddUserEmailVerificationFailure() throws Exception {
        UserResponseV1 userResponse = (UserResponseV1) getUserResponse();
        when(usersService.addUser(any(UserDtoV1.class), eq(null), eq(true)))
            .thenReturn(userResponse);
        doThrow(new RuntimeException("Email verification failed"))
            .when(emailVerificationService).resendEmailVerification(any(UserResponseV1.class));
        mockMvc.perform(post(VERSION_V1 + USER_RESOURCE_PATH + USERS_SELF_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getSelfUserDto())))
            .andExpect(status().isCreated());
    }
}
