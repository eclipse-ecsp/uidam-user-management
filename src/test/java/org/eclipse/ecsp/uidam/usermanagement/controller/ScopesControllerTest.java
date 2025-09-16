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
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopeDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopePatch;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.service.ScopesService;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ScopeListRepresentation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScopesController.class)
@MockBean(JpaMetamodelMappingContext.class)
class ScopesControllerTest {

    @MockBean
    private ScopesService scopeService;

    @MockBean
    private TenantConfigurationService tenantConfigurationService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }
    // Create Scope junits

    @Test
    void testCreateScopeMissingMendatoryNameFieldRequest() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"new test scope\",\"administrative\":true}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.name", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testCreateScopeMissingMendatoryDescriptionFieldRequest() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"ManageUsers\",\"administrative\":true}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.description", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testCreateScopeMissingMendatoryAdministrativeFieldRequest() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"ManageUsers\",\"description\":\"new test scope\"}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.administrative", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testCreateScopeMissingUserIdInHeader() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"ManageUsers\",\"description\":\"new test scope\"}")
                .header(ApiConstants.CORRELATION_ID, "12345").header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_REQUEST_HEADER)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is(ApiConstants.LOGGED_IN_USER_ID)));
    }

    @Test
    void testCreateScopeMissingScopeInHeader() throws Exception {
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"ManageUsers\",\"description\":\"new test scope\"}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_REQUEST_HEADER)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is("scope")));
    }

    @Test
    void testCreateScopeInvalidRequest() throws Exception {
        when(scopeService.validatePermissions(Mockito.any(String.class))).thenReturn(false);
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"ManageUsers\",\"description\":\"new test scope\",\"administrative\":true}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.PERMISSION_DENIED)));
    }

    @Test
    void testCreateScopeValidRequest() throws Exception {
        when(scopeService.validatePermissions(Mockito.any(String.class))).thenReturn(true);
        when(scopeService.addScope(Mockito.any(ScopeDto.class), Mockito.anyString()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"ManageUsers\",\"description\":\"new test scope\",\"administrative\":true}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("ManageUsers")));
    }

    @Test
    void testCreateScopeValidRequestWithoutCorrelationId() throws Exception {
        when(scopeService.validatePermissions(Mockito.any(String.class))).thenReturn(true);
        when(scopeService.addScope(Mockito.any(ScopeDto.class), Mockito.anyString()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"ManageUsers\",\"description\":\"new test scope\",\"administrative\":true}")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("ManageUsers")));
    }

    @Test
    void testCreateDuplicateScopeRecord() throws Exception {
        when(scopeService.validatePermissions(Mockito.any(String.class))).thenReturn(true);
        when(scopeService.addScope(Mockito.any(ScopeDto.class), Mockito.anyString()))
            .thenThrow(RecordAlreadyExistsException.class);
        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"name\":\"ManageUsers\",\"description\":\"new test scope\",\"administrative\":true}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.FIELD_IS_UNIQUE)));
    }

    private ScopeListRepresentation createDummyResponse(String message) {
        Scope scope = new Scope();
        scope.setName("ManageUsers");
        scope.setDescription("new test scope");
        scope.setAdministrative(true);
        scope.setPredefined(true);
        List<ResponseMessage> responseMsgList = new ArrayList<>();
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setKey(message);
        responseMsgList.add(responseMessage);

        Set<Scope> scopeSet = new HashSet<>();
        scopeSet.add(scope);
        ScopeListRepresentation dummyResponse = new ScopeListRepresentation();
        dummyResponse.setScopes(LocalizationKey.SUCCESS_KEY.equals(message) ? scopeSet : null);
        dummyResponse.setMessages(responseMsgList);
        return dummyResponse;
    }

    @Test
    void testGetScopeApi() throws Exception {
        when(scopeService.getScope(Mockito.anyString()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));
        mockMvc.perform(get(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .accept(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("ManageUsers")))
            .andExpect(jsonPath("$.results[0].description", is("new test scope")))
            .andExpect(jsonPath("$.results[0].administrative", is(true)))
            .andExpect(jsonPath("$.results[0].predefined", is(true)));
    }

    @Test
    void testGetScopeApiRcordNotFound() throws Exception {
        when(scopeService.getScope(Mockito.anyString()))
            .thenThrow(new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE));

        mockMvc.perform(get(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers1")
                .accept(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.GET_ENTITY_FAILURE)));
    }

    @Test
    void testGetScopeApiWithoutCorrelationId() throws Exception {
        mockMvc.perform(get(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH + "/ManageUsers")
            .accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());
    }

    // Update Scope junits
    @Test
    void testUpdateScopeMissingCorrelationId() throws Exception {
        when(scopeService.validatePermissions(Mockito.anyString())).thenReturn(true);
        when(scopeService.updateScope(Mockito.anyString(), Mockito.any(ScopePatch.class), Mockito.anyString()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"new test scope\",\"administrative\":true}")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("ManageUsers")))
            .andExpect(jsonPath("$.results[0].description", is("new test scope")));
    }

    @Test
    void testUpdateScopeBlankScopeName() throws Exception {
        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH + "/    ")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"administrative\":false}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is("{jakarta.validation.constraints.NotBlank.message}")));
    }

    @Test
    void testUpdateScopeMissingScopeInHeader() throws Exception {
        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"administrative\":false}")
                .header(ApiConstants.CORRELATION_ID, "12345").header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_REQUEST_HEADER)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is("scope")));
    }

    @Test
    void testUpdateScopeMissingUserIdInHeader() throws Exception {
        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"administrative\":false}")
                .header(ApiConstants.CORRELATION_ID, "12345").header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_REQUEST_HEADER)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is(ApiConstants.LOGGED_IN_USER_ID)));
    }

    @Test
    void testUpdateScopeUserNotAllowed() throws Exception {
        when(scopeService.validatePermissions(Mockito.anyString())).thenReturn(false);
        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"administrative\":false}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUser"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.PERMISSION_DENIED)));
    }

    @Test
    void testUpdateScopeRecordNotFound() throws Exception {
        when(scopeService.validatePermissions(Mockito.anyString())).thenReturn(true);
        when(scopeService.updateScope(Mockito.anyString(), Mockito.any(ScopePatch.class), Mockito.anyString()))
            .thenThrow(new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE));

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"update description\",\"administrative\":false}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.GET_ENTITY_FAILURE)));
    }

    @Test
    void testUpdateScopeSuccess() throws Exception {
        when(scopeService.validatePermissions(Mockito.anyString())).thenReturn(true);
        when(scopeService.updateScope(Mockito.anyString(), Mockito.any(ScopePatch.class), Mockito.anyString()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(patch(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"description\":\"new test scope\",\"administrative\":true}")
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("ManageUsers")))
            .andExpect(jsonPath("$.results[0].description", is("new test scope")));
    }

    // Delete Scope junits
    @Test
    void testDeleteScopeMissingCorrelationId() throws Exception {
        when(scopeService.validatePermissions(Mockito.anyString())).thenReturn(true);
        when(scopeService.deleteScope(Mockito.anyString()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(delete(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)));
    }

    @Test
    void testDeleteScopeBlankScopeName() throws Exception {
        mockMvc.perform(delete(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH + "/    ")
                .contentType(MediaType.APPLICATION_JSON_VALUE).header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser").header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is("{jakarta.validation.constraints.NotBlank.message}")));
    }

    @Test
    void testDeleteScopeMissingUserIdInHeader() throws Exception {
        mockMvc.perform(delete(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE).header(ApiConstants.CORRELATION_ID, "12345")
                .header("scope", "ManageUserRolesAndPermissions")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_REQUEST_HEADER)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is(ApiConstants.LOGGED_IN_USER_ID)));
    }

    @Test
    void testDeleteScopeMissingScopeInHeader() throws Exception {
        mockMvc.perform(delete(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE).header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.MISSING_REQUEST_HEADER)))
            .andExpect(jsonPath("$.messages[0].parameters[0]", is("scope")));
    }

    @Test
    void testDeleteScopeUserNotAllowed() throws Exception {
        when(scopeService.validatePermissions(Mockito.anyString())).thenReturn(false);
        mockMvc.perform(delete(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRoles"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.PERMISSION_DENIED)));
    }

    @Test
    void testDeleteScopeRecordNotFound() throws Exception {
        when(scopeService.validatePermissions(Mockito.anyString())).thenReturn(true);
        when(scopeService.deleteScope(Mockito.anyString()))
            .thenThrow(new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE));

        mockMvc.perform(delete(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.GET_ENTITY_FAILURE)));
    }

    @Test
    void testDeleteScopeSuccess() throws Exception {
        when(scopeService.validatePermissions(Mockito.anyString())).thenReturn(true);
        when(scopeService.deleteScope(Mockito.anyString()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(delete(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + "/ManageUsers")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(ApiConstants.CORRELATION_ID, "12345")
                .header(ApiConstants.LOGGED_IN_USER_ID, "DummyUser")
                .header("scope", "ManageUserRolesAndPermissions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)));
    }

    @Test
    void testFilterScopesApiSuccess() throws Exception {
        when(scopeService.filterScopes(Mockito.anySet(), Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + ApiConstants.ROLES_SCOPES_FILTER_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"scopes\":[\"dummyScope1\"]}")
                .header(ApiConstants.CORRELATION_ID, "12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("ManageUsers")))
            .andExpect(jsonPath("$.results[0].description", is("new test scope")));
    }

    @Test
    void testFilterScopesApiRecordNotFound() throws Exception {
        when(scopeService.filterScopes(Mockito.anySet(), Mockito.anyInt(), Mockito.anyInt()))
            .thenThrow(new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE));

        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + ApiConstants.ROLES_SCOPES_FILTER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345")
                .content("{\"scopes\":[\"dummyScope1\"]}"))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.GET_ENTITY_FAILURE)));
    }

    @Test
    void testFilterScopesApiEmptyScopes() throws Exception {
        mockMvc.perform(post(
                ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH + ApiConstants.ROLES_SCOPES_FILTER_PATH)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .header(ApiConstants.CORRELATION_ID, "12345").content("{\"scopes\":[]}")).andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.scopes", is(LocalizationKey.MISSING_MANDATORY_PARAMETERS)));
    }

    @Test
    void testFilterScopesApiWithoutCorrelationId() throws Exception {
        when(scopeService.filterScopes(Mockito.anySet(), Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(createDummyResponse(LocalizationKey.SUCCESS_KEY));

        mockMvc.perform(post(ApiConstants.API_VERSION + ApiConstants.SCOPE_RESOURCE_PATH
                + ApiConstants.ROLES_SCOPES_FILTER_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"scopes\":[\"dummyScope1\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages[0].key", is(LocalizationKey.SUCCESS_KEY)))
            .andExpect(jsonPath("$.results[0].name", is("ManageUsers")))
            .andExpect(jsonPath("$.results[0].description", is("new test scope")));
    }

}
