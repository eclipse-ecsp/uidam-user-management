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

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfilePatch;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfileRequest;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.response.dto.CloudProfileResponse;
import org.eclipse.ecsp.uidam.usermanagement.controller.UsersControllerTest;
import org.eclipse.ecsp.uidam.usermanagement.entity.CloudProfileEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.CloudProfilesRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.impl.CloudProfileServiceImpl;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CloudProfileServiceImpl.class})
@MockBean(JpaMetamodelMappingContext.class)
class CloudProfileServiceImplTest {

    @Autowired
    private CloudProfileServiceImpl service;

    @MockBean
    private UsersService usersService;

    @MockBean
    private CloudProfilesRepository repository;

    private static final BigInteger CLOUD_PROFILE_ID = new BigInteger("157236105403847391232405464474353");

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void testGetCloudProfileSuccess() throws ResourceNotFoundException {
        Mockito.when(repository.findAllByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString()))
            .thenReturn(Optional.of(List.of(getEntity())));
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        List<CloudProfileResponse> responses = service.getCloudProfile(CLOUD_PROFILE_ID, "test");
        Assertions.assertNotNull(responses);
        responses.forEach(
            response -> Assertions.assertEquals("test-cloud-profile", response.getCloudProfileName())
        );
    }

    @Test
    void testGetCloudProfileFailureNotInDb_Test1() throws ResourceNotFoundException {
        when(repository.findAllByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString())).thenAnswer(
            invocation -> {
                throw new ResourceNotFoundException("CloudProfile", "cloudProfileBusinessKey",
                    String.valueOf(UUID.randomUUID()));
            });
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        assertThrows(
            ResourceNotFoundException.class,
            () -> service.getCloudProfile(new BigInteger("157236105403847391232405464474353"), "test"),
            "Expected getCloudProfile to throw, but it didn't"
        );
    }

    @Test
    void testGetCloudProfileFailureNotInDb_Test2() throws ResourceNotFoundException {
        when(repository.findAllByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString())).thenReturn(
            Optional.empty());
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        assertThrows(
            ResourceNotFoundException.class,
            () -> service.getCloudProfile(new BigInteger("157236105403847391232405464474353"), "test"),
            "Expected getCloudProfile to throw, but it didn't"
        );
    }

    @Test
    void testGetCloudProfilesSuccess() throws ResourceNotFoundException {
        when(repository.findByUserIdIsAndStatusIsNot(any(BigInteger.class), anyString(), any(Pageable.class)))
            .thenReturn(
            getCloudProfiles());
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        Map<String, String> response = service.getCloudProfiles(new BigInteger("157236105403847391232405464474353"));
        Assertions.assertNotNull(response);
        assertEquals(1, response.size());
    }

    @Test
    void testGetCloudProfilesFailureEmptyCase() throws ResourceNotFoundException {
        when(repository.findByUserIdIsAndStatusIsNot(any(BigInteger.class), anyString(), any(Pageable.class)))
            .thenReturn(Optional.of(new ArrayList<>()));
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        Map<String, String> response = service.getCloudProfiles(new BigInteger("157236105403847391232405464474353"));
        Assertions.assertNotNull(response);
        assertEquals(0, response.size());
    }

    @Test
    void testAddCloudProfileSuccess() throws ResourceNotFoundException {
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(repository.findAllByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString()))
            .thenReturn(Optional.of(Collections.emptyList()));
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        Mockito.when(repository.save(any(CloudProfileEntity.class))).thenReturn(getEntity());
        CloudProfileResponse response = service.addCloudProfile(getCloudProfileRequest());
        Assertions.assertNotNull(response);
        Assertions.assertEquals("test-cloud-profile", response.getCloudProfileName());
    }

    @Test
    void testAddCloudProfileFailure() throws ResourceNotFoundException {
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenThrow(new ResourceNotFoundException(
            USER, "userId", "7f9a4cce-a373-4065-9502-7bf3882010e1"));
        assertThrows(
            ResourceNotFoundException.class,
            () -> service.addCloudProfile(getCloudProfileRequest()),
            "User not found for userId: 7f9a4cce-a373-4065-9502-7bf3882010e1"
        );
    }

    @Test
    void testAddCloudProfileExists() throws ResourceNotFoundException {
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(repository.findAllByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString()))
            .thenReturn(Optional.of(List.of(getEntity())));
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        assertThrows(
            ApplicationRuntimeException.class,
            () -> service.addCloudProfile(getCloudProfileRequest()),
            "Profile with name test-cloud-profile already exists for the user: 7f9a4cce-a373-4065-9502-7bf3882010e1"
        );
    }

    @Test
    void testUpdateCloudProfileFailureProfileNotFound() throws ResourceNotFoundException {
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(repository.findAllByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString()))
            .thenReturn(Optional.of(Collections.emptyList()));
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        Mockito.when(repository.save(any(CloudProfileEntity.class))).thenReturn(getEntity());
        CloudProfileResponse cloudProfileResponse = service.updateCloudProfile(null, getCloudProfileRequest(),
            "test-cloud-profile", new BigInteger("157236105403847391232405464474353"));
        Assertions.assertEquals("test-cloud-profile", cloudProfileResponse.getCloudProfileName());
        verify(repository, times(1)).save(any());
    }


    @Test
    void testUpdateCloudProfileFailureUserNotFound() throws ResourceNotFoundException {
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenThrow(new ResourceNotFoundException(
            USER, "userId", "7f9a4cce-a373-4065-9502-7bf3882010e1"));
        assertThrows(
            ResourceNotFoundException.class,
            () -> service.updateCloudProfile(null, getCloudProfileRequest(),
                "test-cloud-profile", new BigInteger("157236105403847391232405464474353")),
            "User not found for userId: 7f9a4cce-a373-4065-9502-7bf3882010e1"
        );

        verify(repository, times(0)).save(any());
    }

    @Test
    void testUpdateCloudProfileSuccess() throws ResourceNotFoundException {
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(repository.findByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString()))
            .thenReturn(Optional.of(getEntity()));
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        when(repository.save(Mockito.any(CloudProfileEntity.class)))
            .thenAnswer(i -> i.getArguments()[0]);
        CloudProfileResponse response = service.updateCloudProfile(null, getCloudProfileRequest(),
            "test-cloud-profile-1", new BigInteger("157236105403847391232405464474353"));
        verify(repository, times(1)).save(any());
        Assertions.assertNotNull(response);
        Assertions.assertEquals("test-cloud-profile", response.getCloudProfileName());
        Assertions.assertEquals("157236105403847391232405464474353", response.getUserId());
    }

    @Test
    void testDeleteCloudProfileFailureWhenNotInDb() {
        when(repository.findByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString())).thenReturn(
            Optional.empty());
        assertThrows(
            ResourceNotFoundException.class,
            () -> service.deleteCloudProfile(null, "test-cloud-profile",
                CLOUD_PROFILE_ID),
            "Cloud Profile exists for Id: 157236105403847391232405464474353"
        );
    }

    @Test
    void testDeleteCloudProfileSuccess() throws ResourceNotFoundException {
        when(repository.findByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString())).thenReturn(
            Optional.of(getEntity()));
        CloudProfileResponse response = service.deleteCloudProfile(null, "test-cloud-profile",
            CLOUD_PROFILE_ID);
        Assertions.assertNotNull(response);
        Assertions.assertEquals("test-cloud-profile", response.getCloudProfileName());
        Assertions.assertEquals("157236105403847391232405464474353", response.getId());
        Assertions.assertEquals("157236105403847391232405464474353", response.getUserId());
    }

    @Test
    void testPatchCloudProfileSuccess() throws ResourceNotFoundException {
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(repository.findByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString()))
            .thenReturn(Optional.of(getEntity()));
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        when(repository.save(any())).thenReturn(getEntity());
        CloudProfileResponse response = service.editCloudProfile(null,
            new CloudProfilePatch(getEntity().getCloudProfileData()), "test-cloud-profile",
            new BigInteger("157236105403847391232405464474353"));
        verify(repository, times(1)).save(any());
        Assertions.assertNotNull(response);
        Assertions.assertEquals("test-cloud-profile", response.getCloudProfileName());
        Assertions.assertEquals("157236105403847391232405464474353", response.getId());
        Assertions.assertEquals("157236105403847391232405464474353", response.getUserId());
    }

    @Test
    void testPatchCloudProfileDoesNotExistsSuccess() throws ResourceNotFoundException {
        List<UserResponseBase> userResponses = List.of(UsersControllerTest.getUserResponse());
        Mockito.when(repository.findByCloudProfileBusinessKeyAndStatusIsNot(anyString(), anyString()))
            .thenReturn(Optional.empty());
        Mockito.when(usersService.getUsers(any(UsersGetFilterV1.class), anyInt(), anyInt(), any(),
            anyString(), anyBoolean(), any(SearchType.class))).thenReturn(userResponses);
        when(repository.save(any())).thenReturn(getEntity());
        CloudProfileResponse response = service.editCloudProfile(null,
            new CloudProfilePatch(getEntity().getCloudProfileData()), "test-cloud-profile",
            new BigInteger("157236105403847391232405464474353"));
        verify(repository, times(1)).save(any());
        Assertions.assertNotNull(response);
        Assertions.assertEquals("test-cloud-profile", response.getCloudProfileName());
        Assertions.assertEquals("157236105403847391232405464474353", response.getId());
        Assertions.assertEquals("157236105403847391232405464474353", response.getUserId());
    }

    private CloudProfileEntity getEntity() {
        CloudProfileEntity response = new CloudProfileEntity();
        Map<String, Object> cloudProfileData = new HashMap<>();
        cloudProfileData.putIfAbsent("email", "test@domain.com");
        response.setCloudProfileData(cloudProfileData);
        response.setCloudProfileName("test-cloud-profile");
        response.setId(new BigInteger("157236105403847391232405464474353"));
        response.setUserId(new BigInteger("157236105403847391232405464474353"));
        return response;
    }

    private CloudProfileRequest getCloudProfileRequest() {
        CloudProfileRequest cloudProfileRequest = new CloudProfileRequest();
        Map<String, Object> cloudProfileData = new HashMap<>();
        cloudProfileData.putIfAbsent("email", "test@domain.com");
        cloudProfileRequest.setCloudProfileData(cloudProfileData);
        cloudProfileRequest.setCloudProfileName("test-cloud-profile");
        cloudProfileRequest.setUserId(new BigInteger("157236105403847391232405464474353"));
        return cloudProfileRequest;
    }

    public Optional<List<CloudProfileEntity>> getCloudProfiles() {
        return Optional.of(List.of(getEntity()));
    }
}
