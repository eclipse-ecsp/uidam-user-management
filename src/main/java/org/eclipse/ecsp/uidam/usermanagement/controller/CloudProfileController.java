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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfilePatch;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfileRequest;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.response.dto.CloudProfileResponse;
import org.eclipse.ecsp.uidam.usermanagement.config.CommonParameters;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.CloudProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.CLOUD_PROFILE_API_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.CLOUD_PROFILE_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.PATH_CLOUD_PROFILE_MAP;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.PATH_VARIABLE_CLOUD_PROFILE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.PROFILE_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.SUMMARY_DELETE_CLOUD_PROFILE_FOR_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.SUMMARY_EDIT_CLOUD_PROFILE_FOR_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.SUMMARY_GET_CLOUD_PROFILE_BY_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.SUMMARY_GET_CLOUD_PROFILE_ETAG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.SUMMARY_GET_CLOUD_PROFILE_FOR_BUSINESS_KEY;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.SUMMARY_UPDATE_CLOUD_PROFILE_FOR_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.END_USER_TAG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ETAG_HEADER_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IF_NONE_MATCH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TENANT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.VERSION_V1;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 *  Controller for Cloud Profile APIs.
 */
@RestController
@RequestMapping(value = VERSION_V1 + USER_RESOURCE_PATH + CLOUD_PROFILE_API_PATH, 
    produces = APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class CloudProfileController {
    private CloudProfileService cloudProfileService;
    private static final int INITIAL_ODD_NUMBER = 17;
    private static final int MULTIPLIER_ODD_NUMBER = 37;
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProfileController.class);

    /**
     * API to get my user cloud profile data.
     *
     * @param etagFromRequest request header input for if-none-match.
     * @param tenantId request header input for tenantId.
     * @param userId request header input for logged in user userId.
     * @param profileName path variable input for cloudProfileName.
     * @return list of cloud profiles associated with user with specific cloudProfileName.
     * @throws ResourceNotFoundException exception to thrown if user does not exist.
     */
    @Operation(
        summary = SUMMARY_GET_CLOUD_PROFILE_FOR_BUSINESS_KEY,
        description = """
            Get my user cloud profile data <br /><br />
            Allowed scopes: SelfManage <br />
            ETag behavior differs slightly from RFC specifications: <br />
            - No support for list, only 1 ETag at a time <br />
            - No support for '*' value. Either a specific value needs to be used or none at all
            """,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = CloudProfileResponse.class)))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND. USER OR CLOUD PROFILE"
                + " WITH BUSINESS KEY DOES NOT EXIST"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")
        }
    )
    @CommonParameters
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @GetMapping(value = PATH_CLOUD_PROFILE_MAP, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CloudProfileResponse>> getCloudProfileMap(
        @RequestHeader(value = IF_NONE_MATCH, required = false) String etagFromRequest,
        @RequestHeader(value = TENANT_ID, required = false) String tenantId,
        @Valid @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @Valid @PathVariable(value = CLOUD_PROFILE_NAME) @Parameter(description = PROFILE_NAME, required = true)
        String profileName)
        throws ResourceNotFoundException {
        LOGGER.info("Getting cloud profile for userId: {} and profile: {}, tenantID: {}", userId, profileName,
            tenantId);
        List<CloudProfileResponse> cloudProfiles = cloudProfileService.getCloudProfile(new BigInteger(userId),
            profileName);
        LOGGER.info("Obtained Cloud Profile from database");
        HttpHeaders responseHeaders = new HttpHeaders();
        addEtag(responseHeaders, cloudProfiles);
        if (ObjectUtils.isEmpty(cloudProfiles)) {
            LOGGER.warn("Failed to retrieve a cloud profile with name: {}", profileName);
            return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
        } else if (StringUtils.isNotEmpty(etagFromRequest) && responseHeaders.getFirst(ETAG_HEADER_NAME) != null
            && etagFromRequest.equals(String.join("", responseHeaders.getFirst(ETAG_HEADER_NAME).split("\"")))) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        } else {
            return new ResponseEntity<>(cloudProfiles, responseHeaders, HttpStatus.OK);
        }
    }

    /**
     * API to get user cloud profile etag in response body.
     *
     * @param etagFromRequest request header input for if-none-match.
     * @param tenantId request header input for tenantId.
     * @param userId request header input for logged in user userId.
     * @param profileName path variable input for cloudProfileName.
     * @return map of ("map", etag) associated with user with specific cloudProfileName.
     * @throws ResourceNotFoundException exception to thrown if user does not exist.
     */
    @Operation(
        summary = SUMMARY_GET_CLOUD_PROFILE_ETAG,
        description = """
            Get my user cloud profile etag in response body <br /><br />
            Allowed scopes: SelfManage <br />
            ETag behavior differs slightly from RFC specifications: <br />
            - No support for list, only 1 ETag at a time <br />
            - No support for '*' value. Either a specific value needs to be used or none at all
            """,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = CloudProfileResponse.class)))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND. "
                + "USER OR CLOUD PROFILE WITH BUSINESS KEY DOES NOT EXIST"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")
        }
    )
    @CommonParameters
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @GetMapping(value = PATH_VARIABLE_CLOUD_PROFILE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getCloudProfile(
        @RequestHeader(value = IF_NONE_MATCH, required = false) String etagFromRequest,
        @RequestHeader(value = TENANT_ID, required = false) String tenantId,
        @Valid @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @Valid @PathVariable(value = CLOUD_PROFILE_NAME) @Parameter(description = PROFILE_NAME, required = true)
        String profileName)
        throws ResourceNotFoundException {
        LOGGER.info("Getting cloud profile for userId: {} and profile: {}, tenantID: {}", userId, profileName,
            tenantId);
        List<CloudProfileResponse> cloudProfiles = cloudProfileService.getCloudProfile(new BigInteger(userId),
            profileName);
        LOGGER.info("Obtained Cloud Profile from database");
        HttpHeaders responseHeaders = new HttpHeaders();
        addEtag(responseHeaders, cloudProfiles);
        if (ObjectUtils.isEmpty(cloudProfiles)) {
            LOGGER.warn("Failed to retrieve a cloud profile with name: {}", profileName);
            return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
        } else if (StringUtils.isNotEmpty(etagFromRequest)
            && etagFromRequest.equals(String.join("",
            responseHeaders.getFirst(ETAG_HEADER_NAME).split("\"")))) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        } else {
            String response = String.join("", responseHeaders.getFirst(ETAG_HEADER_NAME).split("\""));
            return new ResponseEntity<>(Map.of("map", response), responseHeaders, HttpStatus.OK);
        }
    }

    /**
     * API to get user cloud profile names and etag.
     *
     * @param etagFromRequest request header input for if-none-match.
     * @param tenantId request header input for tenantId.
     * @param userId request header input for logged in user userId.
     * @return map of (cloudProfileName, etag) associated with user with specific cloudProfileName.
     * @throws ResourceNotFoundException exception to thrown if user does not exist.
     */
    @Operation(
        summary = SUMMARY_GET_CLOUD_PROFILE_BY_USER_ID,
        description = """
            Get my user cloud profile names and etags <br /><br />
            Allowed scopes: SelfManage <br />
            ETag behavior differs slightly from RFC specifications: <br />
            - No support for list, only 1 ETag at a time <br />
            - No support for '*' value. Either a specific value needs to be used or none at all
            """,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND. USER DOES NOT EXIST"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")
        }
    )
    @CommonParameters
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getCloudProfiles(
        @RequestHeader(value = IF_NONE_MATCH, required = false) String etagFromRequest,
        @RequestHeader(value = TENANT_ID, required = false) String tenantId,
        HttpServletResponse httpServletResponse,
        @Valid @RequestHeader(value = LOGGED_IN_USER_ID) String userId)
        throws ResourceNotFoundException {
        LOGGER.info("Getting user cloud profiles. Tenant ID: {}", tenantId);
        Map<String, String> cloudProfiles = cloudProfileService.getCloudProfiles(new BigInteger(userId));
        addEtagToMap(httpServletResponse, cloudProfiles);
        if (StringUtils.isNotEmpty(etagFromRequest)
            && etagFromRequest.equals(String.join("",
            httpServletResponse.getHeader(ETAG_HEADER_NAME).split("\"")))) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
        LOGGER.debug("Returning CloudProfiles: {}", cloudProfiles);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(ETAG_HEADER_NAME, StringUtils.isNotEmpty(httpServletResponse.getHeader(ETAG_HEADER_NAME))
            ? httpServletResponse.getHeader(ETAG_HEADER_NAME).replace("\"", "") :
            null);
        return new ResponseEntity<>(cloudProfiles, httpHeaders, HttpStatus.OK);
    }

    /**
     * API to create a new profile for user or overwrites existing profile if already exists.
     *
     * @param etagFromRequest request header input for if-none-match.
     * @param tenantId request header input for tenantId.
     * @param userId request header input for logged in user userId.
     * @param cloudProfileName path variable input for cloudProfileName.
     * @return CloudProfileResponse associated with user with specific cloudProfileName.
     * @throws ResourceNotFoundException exception to thrown if user does not exist.
     */
    @Operation(summary = SUMMARY_UPDATE_CLOUD_PROFILE_FOR_ID,
        description = """
            This API creates a new profile for user or overwrites existing profile if already exists <br /><br />
            Allowed scopes: SelfManage <br />
            ETag behavior differs slightly from RFC specifications: <br />
            - No support for list, only 1 ETag at a time <br />
            - No support for '*' value. Either a specific value needs to be used or none at all
            """,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CloudProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND. USER DOES NOT EXIST"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR"),
            @ApiResponse(responseCode = "409", description = "CONFLICT - CLOUD PROFILE WITH "
                + "BUSINESS KEY ALREADY EXISTS")
        })
    @CommonParameters
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @PutMapping(value = PATH_CLOUD_PROFILE_MAP, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<CloudProfileResponse> updateCloudProfile(
        @RequestHeader(value = IF_NONE_MATCH, required = false) String etagFromRequest,
        @RequestHeader(value = TENANT_ID, required = false) String tenantId,
        @Valid @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @Valid @PathVariable(value = CLOUD_PROFILE_NAME) @Parameter(description = ID, required = true)
        String cloudProfileName,
        @Valid @RequestBody @Parameter(name = "Request payload", description = "Parameters that define a cloud profile")
        CloudProfileRequest cloudProfileRequest,
        HttpServletResponse response) throws ResourceNotFoundException {
        LOGGER.info("Updating cloud profile. Tenant ID: {}", tenantId);
        CloudProfileResponse cloudProfileResponse =
            cloudProfileService.updateCloudProfile(etagFromRequest, cloudProfileRequest,
                cloudProfileName, new BigInteger(userId));
        addEtag(response, cloudProfileResponse);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(ETAG_HEADER_NAME, StringUtils.isNotEmpty(response.getHeader(ETAG_HEADER_NAME))
            ? response.getHeader(ETAG_HEADER_NAME).replace("\"", "")
            : null);
        return new ResponseEntity<>(cloudProfileResponse, httpHeaders, HttpStatus.OK);
    }

    /**
     * API to create a new profile for user.
     *
     * @param cloudProfileRequest cloud profile to be created.
     * @return CloudProfileResponse associated with user with specific cloudProfileName.
     * @throws ResourceNotFoundException exception to thrown if user does not exist.
     */
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public CloudProfileResponse addCloudProfile(
        @Valid @RequestBody CloudProfileRequest cloudProfileRequest, HttpServletResponse response)
        throws ResourceNotFoundException {
        CloudProfileResponse cloudProfileResponse = cloudProfileService.addCloudProfile(cloudProfileRequest);
        addEtag(response, cloudProfileResponse);
        return cloudProfileResponse;
    }

    /**
     * API to create a new profile for user or updates specific fields in existing profile if already exists.
     *
     * @param etagFromRequest request header input for if-none-match.
     * @param tenantId request header input for tenantId.
     * @param userId request header input for logged in user userId.
     * @param cloudProfileName path variable input for cloudProfileName.
     * @param cloudProfilePatch patch request pojo for cloud profile.
     * @return CloudProfileResponse associated with user with specific cloudProfileName.
     * @throws ResourceNotFoundException exception to thrown if user does not exist.
     */
    @Operation(summary = SUMMARY_EDIT_CLOUD_PROFILE_FOR_ID,
        description = """
            This API creates a new profile for user or updates specific fields in existing profile
            if already exists. <br /><br />
            Allowed scopes: SelfManage <br />
            ETag behavior differs slightly from RFC specifications: <br />
            - No support for list, only 1 ETag at a time <br />
            - No support for '*' value. Either a specific value needs to be used or none at all
            """,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CloudProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND - CLOUD PROFILE ID DOES NOT EXIST"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")
        })
    @CommonParameters
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @PatchMapping(value = PATH_CLOUD_PROFILE_MAP, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<CloudProfileResponse> editCloudProfile(
        @RequestHeader(value = IF_NONE_MATCH, required = false) String etagFromRequest,
        @RequestHeader(value = TENANT_ID, required = false) String tenantId,
        @Valid @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @Valid @PathVariable(value = CLOUD_PROFILE_NAME) @Parameter(description = ID, required = true)
        String cloudProfileName,
        @Valid @RequestBody @Parameter(name = "Request payload", description = "Parameters that define a cloud profile")
        CloudProfilePatch cloudProfilePatch,
        HttpServletResponse response) throws ResourceNotFoundException {
        LOGGER.info("Patch cloud profile. Tenant ID: {}", tenantId);
        CloudProfileResponse cloudProfileResponse =
            cloudProfileService.editCloudProfile(etagFromRequest, cloudProfilePatch, cloudProfileName,
                new BigInteger(userId));
        addEtag(response, cloudProfileResponse);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(ETAG_HEADER_NAME, StringUtils.isNotEmpty(response.getHeader(ETAG_HEADER_NAME))
            ? response.getHeader(ETAG_HEADER_NAME).replace("\"", "") :
            null);
        return new ResponseEntity<>(cloudProfileResponse, httpHeaders, HttpStatus.OK);
    }

    /**
     * Delete a specific profile for the user.
     *
     * @param etagFromRequest request header input for if-none-match.
     * @param tenantId request header input for tenantId.
     * @param userId request header input for logged in user userId.
     * @param cloudProfileName path variable input for cloudProfileName.
     * @return CloudProfileResponse associated with user with specific cloudProfileName.
     * @throws ResourceNotFoundException exception to thrown if user does not exist.
     */
    @Operation(summary = SUMMARY_DELETE_CLOUD_PROFILE_FOR_ID,
        description = """
            Delete a specific profile for the user. <br /><br />
            Allowed scopes: SelfManage <br />
            ETag behavior differs slightly from RFC specifications: <br />
            - No support for list, only 1 ETag at a time <br />
            - No support for '*' value. Either a specific value needs to be used or none at all
            """,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CloudProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND - CLOUD PROFILE ID DOES NOT EXIST"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")
        })
    @CommonParameters
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @DeleteMapping(value = PATH_CLOUD_PROFILE_MAP)
    public ResponseEntity<CloudProfileResponse> deleteCloudProfile(
        @RequestHeader(value = IF_NONE_MATCH, required = false) String etagFromRequest,
        @RequestHeader(value = TENANT_ID, required = false) String tenantId,
        @Valid @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @Valid @PathVariable(value = CLOUD_PROFILE_NAME) @Parameter(description = ID, required = true)
        String cloudProfileName,
        HttpServletResponse response) throws ResourceNotFoundException {
        LOGGER.info("Delete cloud profile. Tenant ID: {}", tenantId);
        CloudProfileResponse cloudProfileResponse =
            cloudProfileService.deleteCloudProfile(etagFromRequest, cloudProfileName, new BigInteger(userId));
        addEtag(response, cloudProfileResponse);
        return new ResponseEntity<>(cloudProfileResponse, HttpStatus.OK);
    }

    private void addEtag(HttpServletResponse response, CloudProfileResponse cloudProfile) {
        String hash = String.valueOf(cloudProfile.hashCode());
        response.addHeader(ETAG_HEADER_NAME, "\"" + hash + "\"");
    }

    private void addEtag(HttpHeaders response, List<CloudProfileResponse> cloudProfiles) {
        if (!cloudProfiles.isEmpty()) {
            String hash = String.valueOf(cloudProfiles.get(0).hashCode());
            response.set(ETAG_HEADER_NAME, hash);
        }
    }

    private void addEtagToMap(HttpServletResponse response, Map<String, String> map) {
        LOGGER.debug("Adding eTag header");
        HashCodeBuilder builder = new HashCodeBuilder(INITIAL_ODD_NUMBER, MULTIPLIER_ODD_NUMBER);
        map.forEach((name, etag) -> builder.append(name).append(etag));
        String hash = String.valueOf(builder.toHashCode());
        response.addHeader(ETAG_HEADER_NAME, hash);
        LOGGER.debug("eTag header added with value: {}", hash);
    }
}
