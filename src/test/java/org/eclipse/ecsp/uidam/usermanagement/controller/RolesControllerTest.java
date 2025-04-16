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

import io.prometheus.client.CollectorRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolePatch;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolesCreateRequestDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.entity.RoleScopeMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.ScopesEntity;
import org.eclipse.ecsp.uidam.usermanagement.exception.PermissionDeniedException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ScopeNotExists;
import org.eclipse.ecsp.uidam.usermanagement.mapper.ScopeMapper;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RolesController.class)
@MockBean(JpaMetamodelMappingContext.class)
class RolesControllerTest {

    private static final BigInteger SCOPE_ID_1 = new BigInteger("145911385530649019822702644100150");
    private static final BigInteger SCOPE_ID_2 = new BigInteger("145911385590649019822702644100150");
    private static final BigInteger SCOPE_ID_3 = new BigInteger("145911385690649019822702644100150");
    private static final BigInteger ROLE_ID = new BigInteger("145911385590649014822702644100150");

    public static final int EXPECTED = 400;
    @MockBean
    private RolesService roleService;
    @InjectMocks
    RolesController rolesController;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void testCreateRoleMissingMendatoryNameFieldRequest() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"new test role\", \"scopeNames\":[\"dummyScope\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.name", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testCreateRoleMissingMendatoryDescriptionFieldRequest() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"DummyRole\", \"scopeNames\":[\"dummyScope\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.description", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testCreateRoleMissingMendatoryScopeNameFieldRequest() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"DummyRole\",\"description\":\"new test role\"}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.scopeNames", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testCreateRoleMissingUserIdRequestHeader() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"DummyRole\",\"description\":\"new test role\", \"scopeNames\":[\"dummyScope\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_REQUEST_HEADER)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is(ApiConstants.LOGGED_IN_USER_ID)));
    }

    @Test
    void testCreateDuplicateRoleRecord() throws Exception {
        when(roleService.createRole(Mockito.any(RolesCreateRequestDto.class), Mockito.anyString(), Mockito.anyString()))
            .thenThrow(RecordAlreadyExistsException.class);
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"DummyRole\",\"description\":\"new test role\", \"scopeNames\":[\"dummyScope\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.FIELD_IS_UNIQUE)));
    }

    void testCreateRoleValidRequestWithoutCorrelationId() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"DummyRole\",\"description\":\"new test role\", \"scopeNames\":[\"dummyScope\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_CORRELATION_ID)));
    }

    @Test
    void testCreateRoleMappedScopeDoesNotExists() throws Exception {
        List<String> scopesList = Arrays.asList("dummyScope");
        ScopeNotExists exception = new ScopeNotExists(scopesList.toString());
        when(roleService.createRole(Mockito.any(RolesCreateRequestDto.class), Mockito.anyString(), Mockito.anyString()))
            .thenThrow(exception);

        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"DummyRole\",\"description\":\"new test role\", \"scopeNames\":[\"dummyScope\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SCOPE_DOES_NOT_EXIST)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is("[dummyScope]")));
    }

    @Test
    void testCreateRoleUserNotAllowed() throws Exception {
        when(roleService.createRole(Mockito.any(RolesCreateRequestDto.class), Mockito.anyString(), Mockito.anyString()))
            .thenThrow(PermissionDeniedException.class);

        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"DummyRole\",\"description\":\"new test role\", \"scopeNames\":[\"dummyScope\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.PERMISSION_DENIED)));
    }

    @Test
    void testCreateRoleSuccess() throws Exception {
        when(roleService.createRole(Mockito.any(RolesCreateRequestDto.class), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(prepareResponse(createEntityResponse(), LocalizationKey.SUCCESS_KEY));
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"dummyRole\",\"description\":\"role description\", "
                    + "\"scopeNames\":[\"dummyScope1\", \"dummyScope2\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("dummyRole")))
            .andExpect(jsonPath("$.results[0].description", is("role description")))
            .andExpect(jsonPath("$.results[0].scopes[0].name", is("dummyScope1")))
            .andExpect(jsonPath("$.results[0].scopes[1].name", is("dummyScope2")));
    }

    @Test
    void testGetRoleApiEmptyRoleName() throws Exception {
        mockMvc.perform(get(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH + "/    ")
                .accept(MediaType.APPLICATION_JSON).header(ApiConstants.CORRELATION_ID, "12345")).andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is("{jakarta.validation.constraints.NotBlank.message}")));
    }

    @Test
    void testGetRoleApiSuccess() throws Exception {
        when(roleService.getRole(Mockito.anyString())).thenReturn(
            prepareResponse(createEntityResponse(), LocalizationKey.SUCCESS_KEY));
        mockMvc.perform(
                get(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH + "/VEHICLE_OWNER")
                    .accept(MediaType.APPLICATION_JSON)
                    .header(ApiConstants.CORRELATION_ID, "12345"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("dummyRole")))
            .andExpect(jsonPath("$.results[0].description", is("role description")))
            .andExpect(jsonPath("$.results[0].scopes[0].name", is("dummyScope1")))
            .andExpect(jsonPath("$.results[0].scopes[1].name", is("dummyScope2")));
    }

    @Test
    void testGetRoleApiEntityNotFound() throws Exception {
        when(roleService.getRole(Mockito.anyString()))
            .thenThrow(new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE));
        mockMvc.perform(get(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + "/VEHICLE_OWNER")
                .accept(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.GET_ENTITY_FAILURE)));
    }

    @Test
    void testGetRoleByIdApiEmptyRoleId() throws Exception {

        mockMvc.perform(post(
                ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH + ApiConstants.ROLES_RESOURCE_BY_ID_PATH)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345").content("{\"roleId\":[]}")).andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.roleId", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testGetRoleByIdApiRecordNotFound() throws Exception {
        when(roleService.getRoleById(Mockito.anySet()))
            .thenThrow(new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE));
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + ApiConstants.ROLES_RESOURCE_BY_ID_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345")
                .content("{\"roleId\":[1, 2]}"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.GET_ENTITY_FAILURE)));
    }

    @Test
    void testGetRoleByIdApiSuccess() throws Exception {
        when(roleService.getRoleById(Mockito.anySet()))
            .thenReturn(prepareResponse(createGetRoleByIdResponse(), LocalizationKey.SUCCESS_KEY));
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + ApiConstants.ROLES_RESOURCE_BY_ID_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345")
                .content("{\"roleId\":[1, 2]}"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("dummyRole1")))
            .andExpect(jsonPath("$.results[0].description", is("role description")))
            .andExpect(jsonPath("$.results[0].scopes[0].name", is("dummyScope1")))
            .andExpect(jsonPath("$.results[0].scopes[1].name", is("dummyScope2")));
    }

    List<RoleCreateResponse> createGetRoleByIdResponse() {
        ArrayList<RoleCreateResponse> responseList = new ArrayList<>();
        ArrayList<RoleScopeMappingEntity> list1 = new ArrayList<>();
        ScopesEntity scopeEntity1 = new ScopesEntity(SCOPE_ID_1, "dummyScope1",
            "dummy scope description", true, false, list1,
            "dummyUser", new Date(), null, null);
        ScopesEntity scopeEntity2 = new ScopesEntity(SCOPE_ID_3, "dummyScope2",
            "dummy scope description", true, false, list1,
            "dummyUser", new Date(), null, null);
        RolesEntity roleCreateResult1 = new RolesEntity(ROLE_ID, "dummyRole1",
            "dummy role description", list1, "dummyUser",
            new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMapping1 = new RoleScopeMappingEntity(roleCreateResult1, scopeEntity1,
            "dummyUser", null);
        RoleScopeMappingEntity roleScopeMapping2 = new RoleScopeMappingEntity(roleCreateResult1, scopeEntity2,
            "dummyUser", null);

        list1.add(roleScopeMapping1);
        list1.add(roleScopeMapping2);

        RoleCreateResponse roleDto1 = new RoleCreateResponse(ROLE_ID, "dummyRole1", "role description", list1, null);
        responseList.add(roleDto1);
        return responseList;
    }

    private RoleListRepresentation prepareResponse(List<RoleCreateResponse> roleDtos, String message) {
        Set<RoleCreateResponse> roleSet = null;
        if (roleDtos != null && !roleDtos.isEmpty()) {
            roleSet = new HashSet<>();
            for (RoleCreateResponse role : roleDtos) {
                List<ScopesEntity> scopeNames = role.getRoleScopeMapping().stream().map(scope -> scope.getScope())
                    .toList();
                List<Scope> scopes = ScopeMapper.MAPPER.mapToScope(scopeNames);
                role.setScopes(scopes);
                roleSet.add(role);
            }
        }

        List<ResponseMessage> responseMsgList = new ArrayList<>();
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setKey(message);
        responseMsgList.add(responseMessage);

        RoleListRepresentation response = new RoleListRepresentation();
        response.setRoles(roleSet);
        response.setMessages(responseMsgList);
        return response;
    }

    @Test
    void testFilterRolesApiSuccess() throws Exception {
        when(roleService.filterRoles(Mockito.anySet(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean()))
            .thenReturn(prepareResponse(createEntityResponse(), LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + ApiConstants.ROLES_SCOPES_FILTER_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"roles\":[\"dummyRole1\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("dummyRole")))
            .andExpect(jsonPath("$.results[0].description", is("role description")))
            .andExpect(jsonPath("$.results[0].scopes[0].name", is("dummyScope1")));
    }

    @Test
    void testFilterRolesApiRecordNotFound() throws Exception {
        when(roleService.filterRoles(Mockito.anySet(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean()))
            .thenThrow(new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE));

        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + ApiConstants.ROLES_SCOPES_FILTER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345")
                .content("{\"roles\":[\"dummyRole1\"]}"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.GET_ENTITY_FAILURE)));
    }

    @Test
    void testFilterRolesApiEmptyRoles() throws Exception {
        mockMvc.perform(post(
                ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH + ApiConstants.ROLES_SCOPES_FILTER_PATH)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345").content("{\"roles\":[]}")).andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.roles", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    // Update Role junits
    @Test
    void testUpdateRoleMissingCorrelationId() throws Exception {
        when(roleService.updateRole(Mockito.anyString(), Mockito.any(RolePatch.class), Mockito.anyString(),
            Mockito.anyString())).thenReturn(prepareResponse(createEntityResponse(), LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + "/dummyRole")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"role description\",\"scopeNames\":[\"dummyScope1\",\"dummyScope2\"]}")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("dummyRole")))
            .andExpect(jsonPath("$.results[0].description", is("role description")))
            .andExpect(jsonPath("$.results[0].scopes[0].name", is("dummyScope1")))
            .andExpect(jsonPath("$.results[0].scopes[1].name", is("dummyScope2")));
    }

    @Test
    void testUpdateRoleMissingUserIdInHeader() throws Exception {
        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH + "/VEHICLE_OWNER")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"scopeNames\":[\"ManageUsers\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_REQUEST_HEADER)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is(ApiConstants.LOGGED_IN_USER_ID)));
    }

    @Test
    void testUpdateRoleBlankRoleName() throws Exception {
        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH + "/    ")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"scopeNames\":[\"ManageUsers\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is("{jakarta.validation.constraints.NotBlank.message}")));
    }

    @Test
    void testUpdateRoleMissingScopeName() throws Exception {
        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH + "/VEHICLE_OWNER")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content("{\"description\":\"update description\"}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.scopeNames", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testUpdateRoleEmptyScopeName() throws Exception {
        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH + "/VEHICLE_OWNER")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"scopeNames\":[]}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.scopeNames", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testUpdateRoleRecordNotFound() throws Exception {
        when(roleService.updateRole(Mockito.anyString(), Mockito.any(RolePatch.class), Mockito.anyString(),
            Mockito.anyString())).thenThrow(new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE));

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + "/VEHICLE_OWNER")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"scopeNames\":[\"ManageUsers\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.GET_ENTITY_FAILURE)));
    }

    @Test
    void testUpdateRoleScopeNotExists() throws Exception {
        when(roleService.updateRole(Mockito.anyString(), Mockito.any(RolePatch.class), Mockito.anyString(),
            Mockito.anyString())).thenThrow(ScopeNotExists.class);

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + "/VEHICLE_OWNER")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"scopeNames\":[\"ManageUsers\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SCOPE_DOES_NOT_EXIST)));
    }

    @Test
    void testUpdateRolePermissionDenide() throws Exception {
        when(roleService.updateRole(Mockito.anyString(), Mockito.any(RolePatch.class), Mockito.anyString(),
            Mockito.anyString())).thenThrow(PermissionDeniedException.class);

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + "/VEHICLE_OWNER")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"scopeNames\":[\"ManageUsers\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.PERMISSION_DENIED)));
    }

    @Test
    void testUpdateRoleSuccess() throws Exception {
        when(roleService.updateRole(Mockito.anyString(), Mockito.any(RolePatch.class), Mockito.anyString(),
            Mockito.anyString())).thenReturn(prepareResponse(createEntityResponse(), LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + "/dummyRole")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"role description\",\"scopeNames\":[\"dummyScope1\",\"dummyScope2\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("dummyRole")))
            .andExpect(jsonPath("$.results[0].description", is("role description")))
            .andExpect(jsonPath("$.results[0].scopes[0].name", is("dummyScope1")))
            .andExpect(jsonPath("$.results[0].scopes[1].name", is("dummyScope2")));
    }

    @Test
    void testUpdateRoleMissingDescriptionSuccess() throws Exception {
        when(roleService.updateRole(Mockito.anyString(), Mockito.any(RolePatch.class), Mockito.anyString(),
            Mockito.anyString())).thenReturn(prepareResponse(createEntityResponse(), LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + "/dummyRole")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"scopeNames\":[\"dummyScope1\",\"dummyScope2\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "OAuth2ClientMgmt, ManageUserRolesAndPermissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("dummyRole")))
            .andExpect(jsonPath("$.results[0].description", is("role description")))
            .andExpect(jsonPath("$.results[0].scopes[0].name", is("dummyScope1")))
            .andExpect(jsonPath("$.results[0].scopes[1].name", is("dummyScope2")));
    }

    @Test
    void testFilterRolesApiWithoutCorrelationId() throws Exception {
        when(roleService.filterRoles(Mockito.anySet(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean()))
            .thenReturn(prepareResponse(createEntityResponse(), LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.ROLES_RESOURCE_PATH
                + ApiConstants.ROLES_SCOPES_FILTER_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"roles\":[\"dummyRole1\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("dummyRole")))
            .andExpect(jsonPath("$.results[0].description", is("role description")))
            .andExpect(jsonPath("$.results[0].scopes[0].name", is("dummyScope1")));
    }

    List<RoleCreateResponse> createEntityResponse() {
        List<RoleScopeMappingEntity> roleScopeMappingEntities = new ArrayList<>();
        ScopesEntity scopesEntity1 = new ScopesEntity(SCOPE_ID_1, "dummyScope1",
            "dummy scope description", true, false,
            roleScopeMappingEntities, "dummyUser1", new Date(), null, null);
        ScopesEntity scopeEntity2 = new ScopesEntity(SCOPE_ID_2, "dummyScope2",
            "dummy scope description", true, false,
            roleScopeMappingEntities, "dummyUser", new Date(), null, null);
        RolesEntity rolesEntity = new RolesEntity(ROLE_ID, "dummyRole",
            "dummy role description", roleScopeMappingEntities,
            "dummyUser1", new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMappingEntity1 = new RoleScopeMappingEntity(rolesEntity, scopesEntity1,
            "dummyUser1", null);
        RoleScopeMappingEntity roleScopeMappingEntity2 = new RoleScopeMappingEntity(rolesEntity, scopeEntity2,
            "dummyUser", null);
        roleScopeMappingEntities.add(roleScopeMappingEntity1);
        roleScopeMappingEntities.add(roleScopeMappingEntity2);
        RoleCreateResponse roleCreateResponse = new RoleCreateResponse(ROLE_ID, "dummyRole", "role description",
            roleScopeMappingEntities, null);

        List<RoleCreateResponse> roleCreateResponseList = new ArrayList<>();
        roleCreateResponseList.add(roleCreateResponse);

        return roleCreateResponseList;
    }


    @Test
    void shouldDeleteRoleSuccessfully() {
        String roleName = "testRole";
        String userId = "testUser";
        String scopes = "testScopes";
        RoleListRepresentation roleListRepresentation = new RoleListRepresentation();
        when(roleService.deleteRole(roleName, userId, scopes)).thenReturn(roleListRepresentation);

        ResponseEntity<RoleListRepresentation> response = rolesController.deleteRole(roleName, userId, scopes);

        assertEquals(roleListRepresentation, response.getBody());
        verify(roleService, times(1)).deleteRole(roleName, userId, scopes);
    }

    @Test
    void shouldHandleEmptyRoleName() {
        String roleName = "";
        String userId = "testUser";
        String scopes = "testScopes";

        ResponseEntity<RoleListRepresentation> response = rolesController.deleteRole(roleName, userId, scopes);

        assertEquals(EXPECTED, response.getStatusCodeValue());
        verify(roleService, times(0)).deleteRole(roleName, userId, scopes);
    }

}
