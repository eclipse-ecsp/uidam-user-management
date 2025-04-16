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

package org.eclipse.ecsp.uidam.usermanagement.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfilePatch;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfileRequest;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.response.dto.CloudProfileResponse;
import org.eclipse.ecsp.uidam.usermanagement.entity.CloudProfileEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.CloudProfilesRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.CloudProfileService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.CLOUD_PROFILE_STRING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.DELETED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERID_IS_NULL;
import static org.eclipse.ecsp.uidam.usermanagement.mapper.CloudProfileMapper.CLOUD_PROFILE_MAPPER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 *  CloudProfileService methods for cloud profile crud operations.
 */
@Service
@Slf4j
@AllArgsConstructor
public class CloudProfileServiceImpl implements CloudProfileService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private CloudProfilesRepository cloudProfilesRepository;

    private UsersService usersService;

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_CLOUD_PROFILE_PAGE_SIZE = 50;
    private static final int DEFAULT_USERS_PAGE_SIZE = 20;


    /**
     * Get Cloud Profile on the basis of cloudProfileBusinessKey.
     *
     * @param userId      user ID
     * @param profileName cloud profile name of the user
     * @return {@link CloudProfileResponse}  Returns requested cloudProfile details by the user.
     * @throws ResourceNotFoundException If there is an error in getting the cloudProfile or cloudProfile does not exist
     */
    @Override
    public List<CloudProfileResponse> getCloudProfile(BigInteger userId, String profileName)
        throws ResourceNotFoundException {
        final String cloudProfileBusinessKey = userId + "_" + profileName;
        logger.debug("Getting cloudProfile for cloudProfileBusinessKey: {}", cloudProfileBusinessKey);
        verifyCloudProfileUser(userId);
        Optional<List<CloudProfileEntity>> cloudProfileEntities = cloudProfilesRepository
            .findAllByCloudProfileBusinessKeyAndStatusIsNot(cloudProfileBusinessKey, DELETED);
        if (cloudProfileEntities.isEmpty() || cloudProfileEntities.get().isEmpty()) {
            throw new ResourceNotFoundException(CLOUD_PROFILE_STRING, "cloudProfileBusinessKey",
                cloudProfileBusinessKey);
        }
        logger.debug("Successfully got cloudProfile for cloudProfileBusinessKey: {}", cloudProfileBusinessKey);
        List<CloudProfileResponse> cloudProfileResponses = cloudProfileEntities.get().stream()
            .map(CLOUD_PROFILE_MAPPER::cloudProfileEntityToCloudProfileResponse)
            .toList();

        logger.debug("Number of CloudProfileResponses: {}", cloudProfileResponses.size());
        return cloudProfileResponses;
    }

    /**
     * Get all cloud profiles for a user as a map of profile name and hashcode.
     *
     * @param userId user ID of the user
     * @return A map of cloud profiles for the user. { Key = profile name, value = hashcode value}
     * @throws ResourceNotFoundException If there is an error in getting the cloudProfile or cloudProfile does not exist
     */
    @Override
    public Map<String, String> getCloudProfiles(BigInteger userId) throws ResourceNotFoundException {
        logger.debug("Getting cloud profiles for userId: {}", userId);
        verifyCloudProfileUser(userId);
        List<CloudProfileEntity> cloudProfiles = getCloudProfilesAsList(userId);
        logger.debug("Successfully got cloudProfiles for userId: {}", userId);

        return cloudProfiles.stream().map(CLOUD_PROFILE_MAPPER::cloudProfileEntityToCloudProfileResponse)
            .collect(Collectors.toMap(CloudProfileResponse::getCloudProfileName, cp -> String.valueOf(cp.hashCode())));
    }

    /**
     * Add a new cloud profile.
     *
     * @param cloudProfileRequest {@link CloudProfileRequest} containing cloud profile details
     * @return {@link CloudProfileResponse}  Returns added cloudProfile
     * @throws ResourceNotFoundException If there is an error in getting the cloudProfile or cloudProfile does not exist
     */
    @Override
    @Transactional
    public CloudProfileResponse addCloudProfile(CloudProfileRequest cloudProfileRequest)
        throws ResourceNotFoundException {

        // verify if the user exists.
        logger.info("Adding Cloud Profile with userId: {} and name: {}", cloudProfileRequest.getUserId(),
            cloudProfileRequest.getCloudProfileName());
        verifyCloudProfileUser(cloudProfileRequest.getUserId());

        // user exists, save cloud profile in the database
        return saveCloudProfileInDatabase(cloudProfileRequest);
    }

    /**
     * Updates an existing cloud profile.
     *
     * @param cloudProfileRequest {@link CloudProfileRequest} containing cloud profile details to be updated
     * @param id                  Id of the cloud profile to be updated
     * @return {@link CloudProfileResponse}  Returns added cloudProfile
     * @throws ResourceNotFoundException If there is an error in getting the cloudProfile or
     *                                   cloudProfile/user does not exist
     */
    @Override
    @Transactional
    public CloudProfileResponse updateCloudProfile(String etagFromRequest, CloudProfileRequest cloudProfileRequest,
                                                   String cloudProfileName, BigInteger id)
        throws ResourceNotFoundException {
        // Verify if the user exists in the database.
        logger.info("Updating Cloud Profile: {}", id);
        verifyCloudProfileUser(id);

        // update the CloudProfileRequest entity in the database since the user exists.
        return updateCloudProfileInDatabase(etagFromRequest, cloudProfileRequest, id, cloudProfileName);
    }

    /**
     * Partially updates an existing cloud profile.
     *
     * @param patch {@link CloudProfilePatch} containing cloud profile patch data
     * @param id    Id of the cloud profile to be updated
     * @return {@link CloudProfileResponse}  Returns added cloudProfile
     * @throws ResourceNotFoundException If there is an error in getting the cloudProfile or
     *                                   cloudProfile/user does not exist
     */
    @Override
    @Transactional
    public CloudProfileResponse editCloudProfile(String etagFromRequest, CloudProfilePatch patch,
                                                 String cloudProfileName, BigInteger id)
        throws ResourceNotFoundException {
        logger.info("Patching Cloud Profile: {}", id);
        verifyCloudProfileUser(id);
        final String cloudProfileBusinessKey = id + "_" + cloudProfileName;
        Optional<CloudProfileEntity> cloudProfileEntityOptional = cloudProfilesRepository
            .findByCloudProfileBusinessKeyAndStatusIsNot(cloudProfileBusinessKey, DELETED);
        if (cloudProfileEntityOptional.isPresent()) {
            CloudProfileEntity cloudProfileEntity = cloudProfileEntityOptional.get();
            verifyEtag(etagFromRequest, cloudProfileEntity);
            CLOUD_PROFILE_MAPPER.updateCloudProfileFromPatchDto(patch, cloudProfileEntity);
            CloudProfileEntity dbResponse = cloudProfilesRepository.save(cloudProfileEntity);
            logger.debug("Patched cloudProfile with id: {} with value: {}", id, dbResponse);
            return CLOUD_PROFILE_MAPPER.cloudProfileEntityToCloudProfileResponse(dbResponse);
        } else {
            logger.debug("Cloud Profile with business key: {} does not exists in the database.. creating a new one",
                cloudProfileBusinessKey);
            CloudProfileRequest cloudProfileRequest = new CloudProfileRequest();
            cloudProfileRequest.setUserId(id);
            cloudProfileRequest.setCloudProfileBusinessKey(cloudProfileBusinessKey);
            cloudProfileRequest.setCloudProfileName(cloudProfileName);
            cloudProfileRequest.setCloudProfileData(patch.getCloudProfileData());
            return saveCloudProfileInDatabase(cloudProfileRequest);
        }
    }

    /**
     * Delete a cloud profile by cloud profile id.
     *
     * @param id cloud profile id to be deleted
     * @return {@link CloudProfileResponse}  Returns added cloudProfile
     */
    @Override
    @Transactional
    public CloudProfileResponse deleteCloudProfile(String etagFromRequest, String cloudProfileName, BigInteger id)
        throws ResourceNotFoundException {
        logger.debug("Deleting Cloud Profile: {} for userId: {}", cloudProfileName, id);
        final String cloudProfileBusinessKey = id + "_" + cloudProfileName;
        Optional<CloudProfileEntity> cloudProfileEntityOptional = cloudProfilesRepository
            .findByCloudProfileBusinessKeyAndStatusIsNot(cloudProfileBusinessKey, DELETED);
        if (cloudProfileEntityOptional.isPresent()) {
            CloudProfileEntity cloudProfileEntity = cloudProfileEntityOptional.get();
            verifyEtag(etagFromRequest, cloudProfileEntity);
            cloudProfilesRepository.deleteCloudProfile(id);
            logger.info("Cloud Profile: {} deleted from the database", cloudProfileEntity.getId());
            cloudProfileEntity.setStatus(DELETED);
            return CLOUD_PROFILE_MAPPER.cloudProfileEntityToCloudProfileResponse(cloudProfileEntity);
        } else {
            throw new ResourceNotFoundException("CLOUD_PROFILE_STRING", "business key", cloudProfileBusinessKey);
        }
    }


    /**
     * Get 50 cloud profiles for a user as a list.
     *
     * @param userId user ID of the user
     * @return A list of cloud profiles {@link CloudProfileEntity} for the user
     */
    private List<CloudProfileEntity> getCloudProfilesAsList(BigInteger userId) {
        logger.debug("Getting cloudProfiles for userId: {}", userId);
        Pageable first50CloudProfiles = PageRequest.of(DEFAULT_PAGE_NUMBER, DEFAULT_CLOUD_PROFILE_PAGE_SIZE);
        Optional<List<CloudProfileEntity>> response = cloudProfilesRepository.findByUserIdIsAndStatusIsNot(userId,
            DELETED, first50CloudProfiles);
        if (response.isEmpty() || response.get().isEmpty()) {
            return new ArrayList<>();
        }
        return response.get();
    }

    /**
     * Method to get cloud profile entity from cloud profile request.
     *
     * @param request cloudProfileRequest object.
     * @return cloudProfileEntity object.
     */
    private CloudProfileEntity cloudProfileEntityFromCloudProfileRequest(CloudProfileRequest request) {
        BigInteger userId = request.getUserId();
        String profileName = request.getCloudProfileName();
        // Set business key for profile
        request.setCloudProfileBusinessKey(userId + "_" + profileName);
        return CLOUD_PROFILE_MAPPER.cloudProfileRequestToCloudProfileEntity(request);
    }

    /**
     * Method to verify if user exists.
     *
     * @param userId user identifier.
     * @throws ResourceNotFoundException if user is not found then exception is thrown.
     */
    private void verifyCloudProfileUser(BigInteger userId) throws ResourceNotFoundException {
        // Verify that the user exists
        logger.debug("Verifying if the user exists");
        if (Objects.isNull(userId)) {
            logger.error("UserId verification failed as it is null");
            throw new ApplicationRuntimeException(USERID_IS_NULL, BAD_REQUEST);
        }
        UsersGetFilterV1 usersGetFilter = new UsersGetFilterV1();
        usersGetFilter.setIds(Set.of(userId));
        List<UserResponseV1> user = usersService.getUsers(usersGetFilter, DEFAULT_PAGE_NUMBER, DEFAULT_USERS_PAGE_SIZE,
                null, "ASC", false, SearchType.CONTAINS).stream().map(UserResponseV1.class::cast).toList();
        
        logger.info("User: {} exists in the database with name: {}", userId, user.get(0).getUserName());
    }

    /**
     * Service method to update cloud profile data of user.
     *
     * @param etagFromRequest etagFromRequest.
     * @param cloudProfileRequest user cloud profile request.
     * @param userId user id of the input user.
     * @param cloudProfileName cloud profile name of the cloud profile for
     *                         which data needs to be updated.
     * @return cloudProfileResponse
     */
    private CloudProfileResponse updateCloudProfileInDatabase(String etagFromRequest,
                                                              CloudProfileRequest cloudProfileRequest,
                                                              BigInteger userId, String cloudProfileName) {
        final String cloudProfileBusinessKey = userId + "_" + cloudProfileName;
        Optional<CloudProfileEntity> cloudProfileEntityOptional = cloudProfilesRepository
            .findByCloudProfileBusinessKeyAndStatusIsNot(cloudProfileBusinessKey, DELETED);
        if (cloudProfileEntityOptional.isPresent()) {
            logger.info("Cloud Profile with business key: {} exists in the database", cloudProfileBusinessKey);
            CloudProfileEntity cloudProfileEntity = cloudProfileEntityOptional.get();
            verifyEtag(etagFromRequest, cloudProfileEntity);
            CLOUD_PROFILE_MAPPER.updateCloudProfileFromDto(cloudProfileRequest, cloudProfileEntity);
            cloudProfileEntity.setCloudProfileBusinessKey(cloudProfileRequest.getUserId() + "_"
                + cloudProfileRequest.getCloudProfileName());
            return CLOUD_PROFILE_MAPPER.cloudProfileEntityToCloudProfileResponse(
                cloudProfilesRepository.save(cloudProfileEntity));
        } else {
            logger.info("Cloud Profile with business key: {} does not exists in the database.. creating a new one",
                cloudProfileBusinessKey);
            return saveCloudProfileInDatabase(cloudProfileRequest);
        }
    }

    /**
     * Method to validate etag received with respect to cloud profile.
     *
     * @param etagFromRequest etagFromRequest
     * @param cloudProfileEntity cloudProfileEntity
     */
    private void verifyEtag(String etagFromRequest, CloudProfileEntity cloudProfileEntity) {
        String hash = String.valueOf(cloudProfileEntity.hashCode());
        if (!StringUtils.isEmpty(etagFromRequest) && !hash.equals(String.join("", etagFromRequest.split("\"")))) {
            throw new ApplicationRuntimeException("", HttpStatus.PRECONDITION_FAILED);
        }
    }

    /**
     * Method to save cloud profile data in db.
     *
     * @param cloudProfileRequest cloudProfileRequest.
     * @return CloudProfileResponse.
     */
    private CloudProfileResponse saveCloudProfileInDatabase(CloudProfileRequest cloudProfileRequest) {
        final String cloudProfileBusinessKey =
            cloudProfileRequest.getUserId() + "_" + cloudProfileRequest.getCloudProfileName();
        final Optional<List<CloudProfileEntity>> cloudProfileEntities =
            getCloudProfileForBusinessKey(cloudProfileBusinessKey);
        if (cloudProfileEntities.isEmpty() || cloudProfileEntities.get().isEmpty()) {
            CloudProfileEntity cloudProfileEntity = cloudProfileEntityFromCloudProfileRequest(cloudProfileRequest);
            logger.debug("Saving cloudProfile in the database for business key: {}",
                cloudProfileEntity.getCloudProfileBusinessKey());

            return CLOUD_PROFILE_MAPPER.cloudProfileEntityToCloudProfileResponse(
                cloudProfilesRepository.save(cloudProfileEntity));
        }
        throw new ApplicationRuntimeException(
            "Profile with name " + cloudProfileRequest.getCloudProfileName() + " already exists for the user: "
                + cloudProfileRequest.getUserId(), HttpStatus.CONFLICT, cloudProfileBusinessKey);
    }

    /**
     * Method to get cloud profile data for provided business key.
     *
     * @param cloudProfileBusinessKey cloudProfileBusinessKey.
     * @return List of CloudProfileEntity.
     */
    private Optional<List<CloudProfileEntity>> getCloudProfileForBusinessKey(String cloudProfileBusinessKey) {
        logger.debug("Getting cloudProfile for cloudProfileBusinessKey: {}", cloudProfileBusinessKey);
        return cloudProfilesRepository.findAllByCloudProfileBusinessKeyAndStatusIsNot(cloudProfileBusinessKey, DELETED);
    }
}
