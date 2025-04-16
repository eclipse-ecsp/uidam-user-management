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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfilePatch;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfileRequest;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.response.dto.CloudProfileResponse;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.CloudProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.CLOUD_PROFILE_API_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.PATH_CLOUD_PROFILE_MAP;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.PATH_VARIABLE_CLOUD_PROFILE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ETAG_HEADER_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IF_NONE_MATCH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TENANT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.VERSION_V1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Utilities.asJsonString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest({CloudProfileController.class})
@MockBean(JpaMetamodelMappingContext.class)
class CloudProfileControllerTest {

    private static final String CLOUD_PROFILE_PATH = VERSION_V1 + USER_RESOURCE_PATH + CLOUD_PROFILE_API_PATH;
    private static final String CORRELATION_ID_VALUE = "CORRELATION_ID_VALUE";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CloudProfileController controller;
    @MockBean
    private CloudProfileService cloudProfileService;
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    @Order(1)
    void contextLoad() {
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
            .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping
            .getHandlerMethods();
        Assertions.assertFalse(map.isEmpty(), "No APIs exposed to request handler");
        Assertions.assertNotNull(controller, "Controller not created");
        Assertions.assertNotNull(cloudProfileService, "cloudProfileService not created");
    }

    @Test
    void testGetCloudProfileMapSuccess() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenReturn(List.of(getCloudProfileResponse()));
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_CLOUD_PROFILE_MAP,
                        "test-cloud-profile")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                header().exists(ETAG_HEADER_NAME),
                MockMvcResultMatchers.jsonPath("$[0].id").value("7f9a4ccc-a384-4065-9502-8bf3882010e1"),
                MockMvcResultMatchers.jsonPath("$[0].userId").value("7f9a4cce-a373-4065-9502-7bf3882010e1"),
                MockMvcResultMatchers.jsonPath("$[0].cloudProfileName").value("test-cloud-profile"),
                MockMvcResultMatchers.jsonPath("$[0].cloudProfileData").isMap());
    }

    @Test
    void testGetCloudProfileMapCorrelationIdMissing() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenReturn(List.of(getCloudProfileResponse()));
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_CLOUD_PROFILE_MAP,
                        "test-cloud-profile")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testGetCloudProfileMapEtagFromRequestEqualsResponse() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenReturn(List.of(
            getCloudProfileResponse()));
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_CLOUD_PROFILE_MAP,
                        "test-cloud-profile")
                    .header(IF_NONE_MATCH, "1958984513")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotModified());
    }

    @Test
    void testGetCloudProfileMapReturnsEmptyList() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenReturn(Collections.emptyList());
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_CLOUD_PROFILE_MAP,
                        "test-cloud-profile")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testGetCloudProfileMapFailure() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenAnswer(invocation -> {
            throw new ResourceNotFoundException("profile", "Profile", "TEST");
        });
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_CLOUD_PROFILE_MAP,
                        "TEST")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                header().doesNotExist(ETAG_HEADER_NAME));
    }

    @Test
    void testGetCloudProfileSuccess() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenReturn(List.of(
            getCloudProfileResponse()));
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_VARIABLE_CLOUD_PROFILE,
                        "test-cloud-profile")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                header().exists(ETAG_HEADER_NAME),
                MockMvcResultMatchers.jsonPath("$.map").value("1958984513"));
    }

    @Test
    void testGetCloudProfileCorrelationIdMissing() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenReturn(List.of(
            getCloudProfileResponse()));
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_VARIABLE_CLOUD_PROFILE,
                        "test-cloud-profile")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testGetCloudProfileEtagFromRequestEqualsResponse() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenReturn(List.of(
            getCloudProfileResponse()));
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_VARIABLE_CLOUD_PROFILE,
                        "test-cloud-profile")
                    .header(IF_NONE_MATCH, "1958984513")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotModified());
    }

    @Test
    void testGetCloudProfileReturnsEmptyList() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenReturn(Collections.emptyList());
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_VARIABLE_CLOUD_PROFILE,
                        "test-cloud-profile")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testGetCloudProfileFailure() throws Exception {
        when(cloudProfileService.getCloudProfile(any(), any())).thenAnswer(invocation -> {
            throw new ResourceNotFoundException("profile", "Profile", "TEST");
        });
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH + PATH_VARIABLE_CLOUD_PROFILE,
                        "TEST")
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                header().doesNotExist(ETAG_HEADER_NAME));
    }

    @Test
    void testGetCloudProfileseTagSuccess() throws Exception {
        when(cloudProfileService.getCloudProfiles(any())).thenReturn(getCloudProfiles());
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH)
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(IF_NONE_MATCH, "2018906420")
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
            )
            .andDo(print())
            .andExpect(status().isNotModified());
    }

    @Test
    void testGetCloudProfilesSuccess() throws Exception {
        when(cloudProfileService.getCloudProfiles(any())).thenReturn(getCloudProfiles());
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH)
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                header().exists(ETAG_HEADER_NAME),
                MockMvcResultMatchers.jsonPath("$.facebook").value("-1863697462"));
    }

    @Test
    void testGetCloudProfilesCorrelationIdMissing() throws Exception {
        when(cloudProfileService.getCloudProfiles(any())).thenReturn(getCloudProfiles());
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH)
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
            )
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    void testGetCloudProfilesFailure() throws Exception {
        when(cloudProfileService.getCloudProfiles(any())).thenThrow(ResourceNotFoundException.class);
        this.mockMvc.perform(
                MockMvcRequestBuilders
                    .get(CLOUD_PROFILE_PATH)
                    .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                    .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                    .header(TENANT_ID, "tenant1")
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpectAll(
                header().doesNotExist(ETAG_HEADER_NAME));
    }

    @Test
    void testUpdateCloudProfileSuccess() throws Exception {
        when(cloudProfileService.updateCloudProfile(Mockito.any(),
            Mockito.any(CloudProfileRequest.class), Mockito.anyString(), Mockito.any(BigInteger.class))).thenReturn(
            getCloudProfileResponse());

        sendUpdateRequest()
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                header().exists(ETAG_HEADER_NAME),
                MockMvcResultMatchers.jsonPath("$.id").value("7f9a4ccc-a384-4065-9502-8bf3882010e1"),
                MockMvcResultMatchers.jsonPath("$.userId").value("7f9a4cce-a373-4065-9502-7bf3882010e1"),
                MockMvcResultMatchers.jsonPath("$.cloudProfileName").value("test-cloud-profile"),
                MockMvcResultMatchers.jsonPath("$.cloudProfileData").isMap());
    }

    @Test
    void testUpdateCloudProfileFailure() throws Exception {
        when(cloudProfileService.updateCloudProfile(Mockito.any(),
            Mockito.any(CloudProfileRequest.class), Mockito.anyString(), Mockito.any(BigInteger.class))).thenThrow(
            ResourceNotFoundException.class);

        sendUpdateRequest()
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpectAll(
                header().doesNotExist(ETAG_HEADER_NAME));

    }

    @Test
    void testPatchCloudProfileSuccess() throws Exception {
        when(cloudProfileService.editCloudProfile(Mockito.any(),
            Mockito.any(CloudProfilePatch.class), Mockito.anyString(), Mockito.any(BigInteger.class))).thenReturn(
            getCloudProfileResponse());

        sendPatchRequest()
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                header().exists(ETAG_HEADER_NAME),
                MockMvcResultMatchers.jsonPath("$.id").value("7f9a4ccc-a384-4065-9502-8bf3882010e1"),
                MockMvcResultMatchers.jsonPath("$.userId").value("7f9a4cce-a373-4065-9502-7bf3882010e1"),
                MockMvcResultMatchers.jsonPath("$.cloudProfileName").value("test-cloud-profile"),
                MockMvcResultMatchers.jsonPath("$.cloudProfileData").isMap());
    }

    @Test
    void testPatchCloudProfileFailure() throws Exception {
        when(cloudProfileService.editCloudProfile(Mockito.any(),
            Mockito.any(CloudProfilePatch.class), Mockito.anyString(), Mockito.any(BigInteger.class))).thenThrow(
            ResourceNotFoundException.class);

        sendPatchRequest()
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpectAll(
                header().doesNotExist(ETAG_HEADER_NAME));
    }

    @Test
    void testDeleteCloudProfileSuccess() throws Exception {
        when(cloudProfileService.deleteCloudProfile(Mockito.any(), Mockito.anyString(),
            Mockito.any(BigInteger.class))).thenReturn(
            getCloudProfileResponse());

        sendDeleteRequest()
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                header().exists(ETAG_HEADER_NAME),
                MockMvcResultMatchers.jsonPath("$.id").value("7f9a4ccc-a384-4065-9502-8bf3882010e1"),
                MockMvcResultMatchers.jsonPath("$.userId").value("7f9a4cce-a373-4065-9502-7bf3882010e1"),
                MockMvcResultMatchers.jsonPath("$.cloudProfileName").value("test-cloud-profile"),
                MockMvcResultMatchers.jsonPath("$.cloudProfileData").isMap());
    }

    @Test
    void testDeleteCloudProfileFailure() throws Exception {
        when(cloudProfileService.deleteCloudProfile(Mockito.any(), Mockito.anyString(),
            Mockito.any(BigInteger.class))).thenThrow(
            ResourceNotFoundException.class);

        sendDeleteRequest()
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpectAll(
                header().doesNotExist(ETAG_HEADER_NAME));
    }

    private ResultActions sendUpdateRequest() throws Exception {
        return this.mockMvc.perform(
            MockMvcRequestBuilders
                .put(CLOUD_PROFILE_PATH + PATH_CLOUD_PROFILE_MAP, "test-cloud-profile")
                .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                .header(TENANT_ID, "tenant1")
                .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                .content(asJsonString(getCloudProfileRequest()))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE));
    }

    private ResultActions sendPatchRequest() throws Exception {
        return this.mockMvc.perform(
            MockMvcRequestBuilders
                .patch(CLOUD_PROFILE_PATH + PATH_CLOUD_PROFILE_MAP, "test-cloud-profile")
                .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                .header(TENANT_ID, "tenant1")
                .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                .content(asJsonString(getCloudProfilePatch()))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE));
    }

    private ResultActions sendDeleteRequest() throws Exception {
        return this.mockMvc.perform(
            MockMvcRequestBuilders
                .delete(CLOUD_PROFILE_PATH + PATH_CLOUD_PROFILE_MAP, "test-cloud-profile")
                .header(CORRELATION_ID, CORRELATION_ID_VALUE)
                .header(LOGGED_IN_USER_ID, LOGGED_IN_USER_ID_VALUE)
                .header(TENANT_ID, "tenant1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE));
    }

    private CloudProfileResponse getCloudProfileResponse() {
        CloudProfileResponse response = new CloudProfileResponse();
        Map<String, Object> cloudProfileData = new HashMap<>();
        cloudProfileData.putIfAbsent("email", "test@domain.com");
        response.setCloudProfileData(cloudProfileData);
        response.setCloudProfileName("test-cloud-profile");
        response.setId("7f9a4ccc-a384-4065-9502-8bf3882010e1");
        response.setUserId("7f9a4cce-a373-4065-9502-7bf3882010e1");
        return response;
    }

    private CloudProfileRequest getCloudProfileRequest() {
        CloudProfileRequest cloudProfileRequest = new CloudProfileRequest();
        Map<String, Object> cloudProfileData = new HashMap<>();
        cloudProfileData.putIfAbsent("email", "test@domain.com");
        cloudProfileRequest.setCloudProfileData(cloudProfileData);
        cloudProfileRequest.setCloudProfileName("test-cloud-profile");
        cloudProfileRequest.setUserId(LOGGED_IN_USER_ID_VALUE);
        return cloudProfileRequest;
    }


    private CloudProfilePatch getCloudProfilePatch() {
        CloudProfilePatch patch = new CloudProfilePatch();
        Map<String, Object> cloudProfileData = new HashMap<>();
        cloudProfileData.putIfAbsent("email", "test@domain.com");
        patch.setCloudProfileData(cloudProfileData);
        return patch;
    }

    private Map<String, String> getCloudProfiles() {
        return Map.ofEntries(
            entry("facebook", "-1863697462"));
    }

}
