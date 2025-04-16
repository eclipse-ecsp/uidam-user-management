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

import com.fasterxml.jackson.annotation.JsonView;
import com.github.fge.jsonpatch.JsonPatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.enums.SortOrder;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserAccountRoleMappingException;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailVerificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV2;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoViews;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV2;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.List;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.DESCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.HTTP_OK;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.HTTP_SUCCESS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IGNORE_CASE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IGNORE_CASE_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_NUMBER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_NUMBER_DEFAULT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_NUMBER_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_SIZE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_SIZE_DEFAULT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PAGE_SIZE_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_FILTER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_VARIABLE_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SEARCH_TYPE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SEARCH_TYPE_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY_DEFAULT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STRING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUCCESS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_ADD_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_EDIT_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_GET_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_GET_USERS_BY_FILTER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERS_TAG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.VERSION_V2;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MANAGE_USERS_SCOPE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.VIEW_USERS_SCOPE;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV1.UserGetFilterEnum;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * User CRUD APIs controller for Version 2 of user management.
 */
@RestController
@RequestMapping(value = VERSION_V2 + USER_RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class UsersControllerV2 {

    private UsersService usersService;
    private EmailVerificationService emailVerificationService;
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersControllerV2.class);
    private static final String API_VERSION_2 = VERSION_V2.substring(1);

    /**
     * API to create a new user as per version 2.
     * It takes array of account name and roles combination for associating user to
     * that account and set of roles.
     *
     * @param userId  header parameter
     * @param userDto request body
     * @return UserResponse
     * @throws ResourceNotFoundException ResourceNotFoundException
     */
    @Operation(
        summary = SUMMARY_ADD_USER,
        description = "Creates a new user with mapping to account and roles.",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = HTTP_SUCCESS, description = SUCCESS,
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV2.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {MANAGE_USERS_SCOPE})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PostMapping
    @JsonView(UserDtoViews.UserDtoV2View.class)
    public ResponseEntity<UserResponseV2> addUser(
        @RequestHeader(value = LOGGED_IN_USER_ID, required = false) String userId,
        @Valid @RequestBody @Parameter(name = "Request payload", description = "Parameters that define a single user")
            @JsonView(UserDtoViews.UserDtoV2View.class) UserDtoV2 userDto) throws ResourceNotFoundException {
        LOGGER.info("Add User Resource Started, user: {}", userDto.getUserName());
        userDto.setIsExternalUser(Boolean.FALSE);
        UserResponseV2 userResponseV2 = (UserResponseV2) usersService.addUser(userDto, StringUtils.isNotEmpty(userId)
            ? new BigInteger(userId) : null, false);
        try {
            emailVerificationService.resendEmailVerification(userResponseV2);
        } catch (Exception e) {
            LOGGER.error("Failed to send email verification", e);
        }
        return new ResponseEntity<>(userResponseV2, HttpStatus.CREATED);
    }

    /**
     * API to retrieve users that match items in the defined list of parameters and values.
     *
     * @param pageNumber request param specifying pageNumber of the result to be displayed.
     * @param pageSize request param specifying pageSize of the result to be displayed.
     * @param sortBy order results in asc or desc order by a particular field.
     * @param sortOrder order results in asc or desc order.
     * @param ignoreCase make search case-sensitive/case-insensitive.
     * @param searchType define search type of field contains, starts or ends with a specific value.
     * @param userGetFilter define filter criteria for each field.
     * @return List of UserResponse.
     * @throws ResourceNotFoundException exception to be thrown if no such users found.
     */
    @Operation(summary = "POST /v2/users/filter - "
        + SUMMARY_GET_USERS_BY_FILTER,
        tags = { USERS_TAG }, operationId = "getUsers-v2",
        description = "'V2 filter - Retrieves users that match items in the defined list of parameters and values.' "
            + " List of parameters can contain dynamic attributes only if they were defined as 'searchable'.",
        responses = {
            @ApiResponse(responseCode = HTTP_SUCCESS, description = SUCCESS,
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = UserResponseV2.class))))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {VIEW_USERS_SCOPE, MANAGE_USERS_SCOPE})
    @PostMapping(path = PATH_FILTER)
    public ResponseEntity<List<UserResponseV2>> getUsers(
        @RequestParam(name = PAGE_NUMBER, required = false, defaultValue = PAGE_NUMBER_DEFAULT)
        @Parameter(description = PAGE_NUMBER_DESCRIPTION) Integer pageNumber,
        @RequestParam(name = PAGE_SIZE, required = false, defaultValue = PAGE_SIZE_DEFAULT)
        @Parameter(description = PAGE_SIZE_DESCRIPTION) Integer pageSize,
        @RequestParam(name = SORT_BY, required = false, defaultValue = SORT_BY_DEFAULT)
        @Parameter(description = SORT_BY_DESCRIPTION) UserGetFilterEnum sortBy,
        @RequestParam(name = SORT_ORDER, required = false, defaultValue = DESCENDING)
        @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {DESCENDING, ASCENDING}))
        SortOrder sortOrder,
        @RequestParam(name = IGNORE_CASE, required = false)
        @Parameter(description = IGNORE_CASE_DESCRIPTION, schema = @Schema(allowableValues = {"true", "false"}))
        boolean ignoreCase,
        @RequestParam(name = SEARCH_TYPE, required = false)
        @Parameter(description = SEARCH_TYPE_DESCRIPTION, schema = @Schema(allowableValues = {"PREFIX", "SUFFIX",
            "CONTAINS"})) SearchType searchType,
        @Valid @RequestBody
        @Parameter(name = "Request payload", description = "Parameters and values by which to filter. "
            + "To get all users, leave empty.")
        UsersGetFilterV2 userGetFilter) throws ResourceNotFoundException {
        LOGGER.info("Get users request received, request: {}", userGetFilter);
        return new ResponseEntity<>(usersService
            .getUsers(userGetFilter, pageNumber, pageSize, sortBy == null ? null : sortBy.getField(),
                sortOrder.sortOrderLowerCase(), ignoreCase, searchType)
            .stream().map(UserResponseV2.class::cast).toList(), HttpStatus.OK);
    }

    /*
     * API to get a single user identified by its ID as per version 2.
     *
     * @param id user id of the user.
     *
     * @return version 2 of user details fetched with respect to user id.
     *
     * @throws ResourceNotFoundException throw exception if user details not found.
     */
    @Operation(summary = SUMMARY_GET_USER,
        description = "Gets a single user identified by its ID as per version 2",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = HTTP_OK, description = SUCCESS,
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV2.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {VIEW_USERS_SCOPE, MANAGE_USERS_SCOPE})
    @GetMapping(value = PATH_VARIABLE_ID)
    public ResponseEntity<UserResponseV2> getUser(
        @Valid @PathVariable(value = ID) @Parameter(description = "User ID", required = true) BigInteger id)
        throws ResourceNotFoundException {
        LOGGER.info("Get V2 User Resource Started, user id: {}", id);
        return new ResponseEntity<>((UserResponseV2) usersService.getUser(id, API_VERSION_2), HttpStatus.OK);
    }

    /**
     * API to update user details by passing user id.
     *
     * @param userId - user id(user who is trying to update user details) from api-gateway/token
     * @param id - id of user whose details needs to be updated
     * @param jsonPatch - update request parameters
     * @return updated response
     * @throws ResourceNotFoundException - thrown when user not found in system
     * @throws UserAccountRoleMappingException something wrong with account role mapping
     */
    @Operation(summary = SUMMARY_EDIT_USER,
        description = "Updates a single user identified by its ID as per version 2",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = HTTP_OK, description = SUCCESS, content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(
                implementation = UserResponseV2.class)))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {MANAGE_USERS_SCOPE})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PatchMapping(path = PATH_VARIABLE_ID)
    public ResponseEntity<UserResponseV2> editUser(
        @RequestHeader(value = LOGGED_IN_USER_ID, required = false) String userId,
        @PathVariable(value = ID) @Parameter(description = USER_ID, required = true) BigInteger id,
        @RequestBody @Valid @Parameter(name = "Request payload", description = "Parameters that update a single user",
            content = @Content(
                schema = @Schema(
                    type = "object",
                    additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
                    implementation = JsonPatch.class
                )
            )) JsonPatch jsonPatch
    ) throws ResourceNotFoundException, UserAccountRoleMappingException {
        LOGGER.info("update user request received for user id: {}", id);
        return new ResponseEntity<>((UserResponseV2) usersService.editUser(id, jsonPatch, StringUtils.isNotEmpty(userId)
            ? new BigInteger(userId) : null, false, API_VERSION_2), HttpStatus.OK);
    }
}
