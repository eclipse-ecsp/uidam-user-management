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
import io.swagger.v3.oas.annotations.Hidden;
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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.enums.SortOrder;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.InActiveUserException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecoverySecretExpireException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserAccountRoleMappingException;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailVerificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.AssociateAccountAndRolesDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.ExternalUserDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.FederatedUserDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserChangeStatusRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV1;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoViews;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserEventsDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserMetaDataRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserUpdatePasswordDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersDeleteFilter;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.AssociateAccountAndRolesResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.PasswordPolicyResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.SelfAddUserResponseV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserDetailsResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserMetaDataResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCOUNT_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCOUNT_ROLE_ASSOCIATION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASSOCIATE_USERS_TO_ROLE_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.DESCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.END_USER_TAG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.EXTERNAL_USER;
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
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_BY_USERNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_CHANGE_STATUS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_FEDERATED_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_FILTER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_GET_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_USER_ATTRIBUTES;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_VARIABLE_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_VARIABLE_USERNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SEARCH_TYPE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SEARCH_TYPE_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY_DEFAULT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STRING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUCCESS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_ADD_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_ADD_FEDERATED_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_ADD_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_CHANGE_USER_STATUS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_CHANGE_USER_STATUS_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_DELETE_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_DELETE_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_DELETE_USERS_BY_FILTER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_EDIT_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_EDIT_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_GET_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_GET_SELF_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_GET_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_GET_USERS_BY_FILTER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_GET_USER_ATTRIBUTES;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_PUT_USER_ATTRIBUTES;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_SELF_DELETE_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_SELF_EDIT_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_SELF_RESET;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERS_SELF_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERS_TAG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_EVENTS_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_RECOVERY_SECRET_NOTIFICATION_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_RECOVERY_SET_PASSWORD_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_SELF_RECOVERY_FORGOT_PWD_API_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.VERSION_V1;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV1.UserGetFilterEnum;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * User CRUD APIs controller.
 */
@RestController
@RequestMapping(value = VERSION_V1 + USER_RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
public class UsersController {

    private UsersService usersService;
    private EmailVerificationService emailVerificationService;
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersController.class);
    private static final String API_VERSION_1 = VERSION_V1.substring(1);
    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * API to create a new user.
     *
     * @param userId  header param
     * @param userDto request body
     * @return UserResponse
     * @throws ResourceNotFoundException ResourceNotFoundException
     */
    @Operation(
        summary = SUMMARY_ADD_USER,
        description = "Creates a new user.",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "201", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PostMapping
    public ResponseEntity<UserResponseV1> addUser(
        @RequestHeader(value = LOGGED_IN_USER_ID, required = false) String userId,
        @Valid @RequestBody @Parameter(name = "Request payload", description = "Parameters that define a single user")
        @JsonView(UserDtoViews.UserDtoV1View.class) UserDtoV1 userDto) throws ResourceNotFoundException {
        LOGGER.info("Add User Resource Started, user: {}", userDto.getUserName());
        userDto.setIsExternalUser(Boolean.FALSE);
        UserResponseV1 userResponseV1 = (UserResponseV1) usersService.addUser(userDto, StringUtils.isNotEmpty(userId)
            ? new BigInteger(userId) : null, false);
        try {
            emailVerificationService.resendEmailVerification(userResponseV1);
        } catch (Exception e) {
            LOGGER.warn("failed to send email verification: {}", e.getMessage());
        }
        return new ResponseEntity<>(userResponseV1, HttpStatus.CREATED);
    }

    /**
     * Self User API to create a new user.
     *
     * @param userDto request body
     * @return UserResponse
     * @throws ResourceNotFoundException ResourceNotFoundException
     */
    @Hidden
    @PostMapping(value = USERS_SELF_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SelfAddUserResponseV1> selfAddUser(
        @Valid @RequestBody @Parameter(name = "Request payload", description = "Parameters that define a single user")
            @JsonView(UserDtoViews.UserDtoV1SelfView.class) UserDtoV1 userDto) throws ResourceNotFoundException {
        LOGGER.info("Self add User Resource Started, user: {}", userDto.getUserName());
        SelfAddUserResponseV1 selfAddUserResponseV1 = new SelfAddUserResponseV1();
        UserResponseV1 userResponseV1 = null;
        Boolean verificationEmailSent = null;
        try {
            userResponseV1 =  (UserResponseV1) usersService.addUser(userDto, null, true);
            BeanUtils.copyProperties(userResponseV1, selfAddUserResponseV1);
        } catch (RecordAlreadyExistsException e) {
            throw new RecordAlreadyExistsException(ApiConstants.USER);
        } catch (Exception e) {
            throw new ApplicationRuntimeException(
                    "Failed to create user '" + userDto.getUserName() + "': " + e.getMessage(), BAD_REQUEST);
        }
        try {
            verificationEmailSent = emailVerificationService.resendEmailVerification(userResponseV1);
        } catch (Exception e) {
            LOGGER.error("failed to send email verification: {}", e.getMessage());
        } finally {
            if (BooleanUtils.isTrue(verificationEmailSent)) {
                LOGGER.info("Verification email sent successfully.");
                selfAddUserResponseV1.setVerificationEmailSent(verificationEmailSent);
            } else {
                LOGGER.info("Either email verification not enabled or email not sent successfully");
                selfAddUserResponseV1.setVerificationEmailSent(false);
            }
        }
        return new ResponseEntity<>(selfAddUserResponseV1, HttpStatus.CREATED);
    }


    /**
     * API to get a single user identified by its ID.
     *
     * @param id user id of the user.
     * @return user details fetched with respect to user id.
     * @throws ResourceNotFoundException throw exception if user details not found.
     */
    @Operation(summary = SUMMARY_GET_USER,
        description = "Gets a single user identified by its ID",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ViewUsers", "ManageUsers"})
    @GetMapping(value = PATH_VARIABLE_ID)
    public ResponseEntity<UserResponseV1> getUser(
        @Valid @PathVariable(value = ID) @Parameter(description = "User ID", required = true) BigInteger id)
        throws ResourceNotFoundException {
        LOGGER.info("Get User Resource Started, user id: {}", id);
        return new ResponseEntity<>((UserResponseV1) usersService.getUser(id, API_VERSION_1), HttpStatus.OK);
    }

    /**
     * API to get external user identified by its ID.
     *
     * @param id user id of the user.
     * @return user details fetched with respect to user id.
     * @throws ResourceNotFoundException throw exception if user details not found.
     */
    @Operation(summary = SUMMARY_GET_EXTERNAL_USER,
        description = "Gets a single external user identified by its ID.",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ViewUsers", "ManageUsers"})
    @GetMapping(value = PATH_GET_EXTERNAL_USER)
    public ResponseEntity<UserResponseV1> getExternalUser(
        @Valid @PathVariable(value = ID) @Parameter(description = "User ID", required = true) BigInteger id)
        throws ResourceNotFoundException {
        LOGGER.info("Get External User Resource Started, user id: {}", id);
        return new ResponseEntity<>((UserResponseV1) usersService.getUser(id, API_VERSION_1), HttpStatus.OK);
    }

    /**
     * API to get user details by username.
     *
     * @param accountName - account name
     * @param userName - user name
     * @return user details
     * @throws ResourceNotFoundException - thrown when user is not found in system
     * @throws InActiveUserException - thrown when user status is pending or blocked
     */
    @GetMapping(value = PATH_VARIABLE_USERNAME + PATH_BY_USERNAME)
    public ResponseEntity<UserDetailsResponse> getUserByUserName(
        @RequestHeader(value = ACCOUNT_NAME, required = false) String accountName,
        @Valid @PathVariable(value = USERNAME) String userName)
        throws ResourceNotFoundException, InActiveUserException {
        LOGGER.info("Get User By UserName Resource Started, username: {} accountName received: {}",
            userName, accountName);
        return new ResponseEntity<>(usersService.getUserByUserName(userName), HttpStatus.OK);
    }

    /**
     * API to get user's entity attributes.
     *
     * @return List of user entity attribute metadata.
     */
    @Operation(summary = SUMMARY_GET_USER_ATTRIBUTES,
        description = "Get user's entity attributes.",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ViewUsers", "ManageUsers"})
    @GetMapping(value = PATH_USER_ATTRIBUTES)
    public ResponseEntity<List<UserMetaDataResponse>> getUserAttributes() {
        LOGGER.info("Get user attributes");
        return new ResponseEntity<>(usersService.getUserMetaData(), HttpStatus.OK);
    }

    /**
     * API to add/modify additional attributes to user entity.
     *
     * @param userMetaDataRequests List of user attribute meta data.
     * @return List of user attribute metadata.
     */
    @Operation(summary = SUMMARY_PUT_USER_ATTRIBUTES,
        description = "Add/Modify additional attributes to user entity.",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @PutMapping(value = PATH_USER_ATTRIBUTES, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserMetaDataResponse>> putUserAttributes(@Valid @RequestBody List<UserMetaDataRequest>
                                                                            userMetaDataRequests) {
        LOGGER.info("Put user attributes");
        return new ResponseEntity<>(usersService.putUserMetaData(userMetaDataRequests), HttpStatus.OK);
    }

    /**
     * API to update user details by passing user id.
     *
     * @param userId - user id(user who is trying to update user details) from api-gateway/token
     * @param id - id of user whose details needs to be updated
     * @param jsonPatch - update request parameters
     * @return updated response
     * @throws ResourceNotFoundException - thrown when user not found in system
     * @throws UserAccountRoleMappingException some problem with account role mapping
     */
    @Operation(summary = SUMMARY_EDIT_USER,
        description = "Updates a single user identified by its ID",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(
                implementation = UserResponseV1.class)))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PatchMapping(path = PATH_VARIABLE_ID)
    public ResponseEntity<UserResponseV1> editUser(
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
        return new ResponseEntity<>((UserResponseV1) usersService.editUser(id, jsonPatch, StringUtils.isNotEmpty(userId)
            ? new BigInteger(userId) : null, false, API_VERSION_1), HttpStatus.OK);
    }

    /**
     * API to delete single user by its id.
     *
     * @param id userId of the user.
     * @param isUserExternal header variable isUserExternal.
     * @return User details of the deleted user.
     * @throws ResourceNotFoundException Method throws exception if user details not found.
     */
    @Operation(summary = SUMMARY_DELETE_USER,
        description = "Deletes a single user identified by its ID",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = EXTERNAL_USER, description = EXTERNAL_USER, in = ParameterIn.HEADER)
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @DeleteMapping(value = PATH_VARIABLE_ID)
    public ResponseEntity<UserResponseV1> deleteUser(
        @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @PathVariable(value = ID) @Parameter(description = USER_ID, required = true) BigInteger id,
        @RequestHeader(value = EXTERNAL_USER, required = false) Boolean isUserExternal)
        throws ResourceNotFoundException {
        LOGGER.info("delete user request received for user id: {}", id);
        return new ResponseEntity<>(usersService.deleteUser(id, isUserExternal, new BigInteger(userId)),
            HttpStatus.OK);
    }

    /**
     * API to delete users based on filter.
     *
     * @return list of deleted user.
     * @throws ResourceNotFoundException throws exception if user details not found/deleted.
     */
    @Operation(summary = SUMMARY_DELETE_USERS_BY_FILTER,
        description = SUMMARY_DELETE_USERS_BY_FILTER,
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @DeleteMapping
    public ResponseEntity<List<UserResponseV1>> deleteUsersByFilter(
        @Valid @RequestBody UsersDeleteFilter userDeleteFilter)
        throws ResourceNotFoundException {
        LOGGER.info("Delete User by filter Started, user ids: {}", userDeleteFilter.getIds());
        return new ResponseEntity<>(usersService.deleteUsers(userDeleteFilter), HttpStatus.OK);
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
    @Operation(summary = SUMMARY_GET_USERS_BY_FILTER,
        tags = { USERS_TAG }, operationId = "getUsers-v1",
        description = "'Retrieves users that match items in the defined list of parameters and values.' "
            + " List of parameters can contain dynamic attributes only if they were defined as 'searchable'.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = UserResponseV1.class))))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ViewUsers", "ManageUsers"})
    @PostMapping(path = PATH_FILTER)
    public ResponseEntity<List<UserResponseV1>> getUsers(
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
        Boolean ignoreCase,
        @RequestParam(name = SEARCH_TYPE, required = false)
        @Parameter(description = SEARCH_TYPE_DESCRIPTION, schema = @Schema(allowableValues = {"PREFIX", "SUFFIX",
            "CONTAINS"})) SearchType searchType,
        @Valid 
        @RequestBody
        @Parameter(name = "Request payload", description = "Parameters and values by which to filter. "
            + "To get all users, leave empty.")
        UsersGetFilterV1 userGetFilter) throws ResourceNotFoundException {
        LOGGER.info("Get users request received, request: {}", userGetFilter);
        return new ResponseEntity<>(usersService
            .getUsers(userGetFilter, pageNumber, pageSize, sortBy == null ? null : sortBy.getField(),
                sortOrder.sortOrderLowerCase(), ignoreCase == null ? false : ignoreCase.booleanValue(), searchType)
            .stream().map(UserResponseV1.class::cast).toList(), HttpStatus.OK);
    }

    /**
     * Api to get user details by user id.
     *
     * @param userId user unique identifier.
     * @return UserResponse
     * @throws ResourceNotFoundException throws exception if user details not found.
     */
    @Operation(summary = SUMMARY_GET_SELF_USER,
        description = SUMMARY_GET_SELF_USER,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @GetMapping(value = USERS_SELF_PATH)
    public ResponseEntity<UserResponseV1> getSelfUser(
        @Valid @RequestHeader(value = LOGGED_IN_USER_ID, required = false) BigInteger userId)
        throws ResourceNotFoundException {
        LOGGER.info("Get Self User Resource Started, user id: {}", userId);
        return new ResponseEntity<>((UserResponseV1) usersService.getUser(userId, API_VERSION_1), HttpStatus.OK);
    }

    /**
     * API to update logged-in user details.
     *
     * @param userId user unique identifier.
     * @param jsonPatch request object containing user details to be updated.
     * @return updated user details.
     * @throws ResourceNotFoundException throws exception if user details not found.
     * @throws UserAccountRoleMappingException something wrong with account role mapping
     */
    @Operation(summary = SUMMARY_SELF_EDIT_USER,
        description = SUMMARY_SELF_EDIT_USER,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PatchMapping(path = USERS_SELF_PATH)
    public ResponseEntity<UserResponseV1> editSelfUser(
        @RequestHeader(value = LOGGED_IN_USER_ID, required = false) BigInteger userId,
        @RequestBody @Valid @Parameter(name = "Request payload", description = "Parameters that update a single user",
            content = @Content(
                schema = @Schema(
                    type = "object",
                    additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
                    implementation = JsonPatch.class
                )
            )) JsonPatch jsonPatch
    ) throws ResourceNotFoundException, UserAccountRoleMappingException {
        LOGGER.info("Update Self User Started, user id: {}", userId);
        return new ResponseEntity<>((UserResponseV1) usersService.editUser(userId, jsonPatch, userId, false,
            API_VERSION_1), HttpStatus.OK);
    }

    /**
     * API to delete single user based userId.
     *
     * @param userId input userId.
     * @param isUserExternal flag to determine is user is external.
     * @return details of deleted user.
     * @throws ResourceNotFoundException throws exception if user details not found/deleted.
     */
    @Operation(summary = SUMMARY_SELF_DELETE_USER,
        description = SUMMARY_SELF_DELETE_USER,
        tags = {END_USER_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"SelfManage"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @Parameter(name = EXTERNAL_USER, description = EXTERNAL_USER, in = ParameterIn.HEADER)
    @DeleteMapping(value = USERS_SELF_PATH)
    public ResponseEntity<UserResponseV1> deleteSelfUser(
        @RequestHeader(value = LOGGED_IN_USER_ID, required = false) BigInteger userId,
        @RequestHeader(value = EXTERNAL_USER, required = false) Boolean isUserExternal)
        throws ResourceNotFoundException {
        LOGGER.info("Delete Self User Resource Started, user id: {}", userId);
        return new ResponseEntity<>(usersService.deleteUser(userId, isUserExternal, userId), HttpStatus.OK);
    }

    /**
     * API to add user event details for the user into uidam database,
     * event can be log in attempt event or other customized event.
     *
     * @param userId userId of the user.
     * @param userEventsDto userEvents received during login attempts.
     * @return 201 if userEvent added to db.
     */
    @PostMapping(value = USER_EVENTS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addUserEvent(@PathVariable(ID) String userId,
                                               @RequestBody UserEventsDto userEventsDto) {
        LOGGER.info("Add user event Started, user id: {}", userId);
        usersService.addUserEvent(userEventsDto, userId);
        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    /**
     * API to send notification to user with recovery secret details for the user to
     * registered email.
     *
     * @return recover secret details
     * @throws ResourceNotFoundException exception
     * @throws IOException exception
     */
    @PostMapping(value = USER_RECOVERY_SECRET_NOTIFICATION_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendUserResetPasswordNotification(@PathVariable(USERNAME) String username,
            @RequestHeader(value = ACCOUNT_NAME, required = false) String accountName)
            throws ResourceNotFoundException, IOException {
        LOGGER.info("Send user reset password Started, username: {}, account name: {}", username, accountName);
        usersService.sendUserRecoveryNotification(username, false);
        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);

    }
    
    
    /**
     * Self API initiated by user for notification with recovery secret details for the user to
     * registered email.
     *
     * @param userId of the user.
     * @return recover secret details
     * @throws ResourceNotFoundException when user not found.
     * @throws IOException exception. 
     */
    @Operation(summary = SUMMARY_SELF_RESET, 
            description = SUMMARY_SELF_RESET, tags = {END_USER_TAG }, 
            responses = {@ApiResponse(responseCode = "200", description = "Success", 
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
                     schema = @Schema(implementation = String.class))) }) 
    @SecurityRequirement(name = "JwtAuthValidator", 
            scopes = {"SelfManage" }) 
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID, 
        schema = @Schema(type = STRING), in = ParameterIn.HEADER, required = false)
    @PostMapping(value = USER_SELF_RECOVERY_FORGOT_PWD_API_PATH)
    public ResponseEntity<String> sendUserResetPasswordNotification(
            @RequestHeader(value = LOGGED_IN_USER_ID) String userId) throws ResourceNotFoundException, IOException {
        LOGGER.info("Send user reset password Started, user ID: {}", userId);
        usersService.sendUserRecoveryNotification(userId, true);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    /**
     * API to update user password for the user into uidam database, with recovery secret
     * validation.
     *
     * @return String
     * @throws ResourceNotFoundException     when resource not exist
     * @throws RecoverySecretExpireException when secret expired
     */
    @PostMapping(value = USER_RECOVERY_SET_PASSWORD_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateUserPasswordUsingRecoverySecret(
        @Valid @RequestBody @Parameter(name = "Request payload", description = "recovery secret and password")
        UserUpdatePasswordDto userUpdatePasswordDto)
        throws RecoverySecretExpireException, ResourceNotFoundException {
        LOGGER.info("update user password using recovery secret started");
        usersService.updateUserPasswordUsingRecoverySecret(userUpdatePasswordDto);
        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);

    }

    /**
     * Update user status into uidam database.
     *
     * @param userChangeStatusRequest contains list of ids and approved flag
     * @param userId The ID of the logged-in user.
     * @return List of UserResponse.
     */
    @Operation(
        summary = SUMMARY_CHANGE_USER_STATUS,
        description = SUMMARY_CHANGE_USER_STATUS_DESCRIPTION,
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PatchMapping(path = PATH_CHANGE_STATUS)
    public ResponseEntity<List<UserResponseV1>> changeUserStatus(
        @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @Valid @RequestBody @Parameter(name = "Request payload", description = "Parameters that update users status")
        UserChangeStatusRequest userChangeStatusRequest) {
        LOGGER.info("Change user status started by userId :{} for userChangeStatusRequest :{}",
            userId, userChangeStatusRequest);
        return new ResponseEntity<>(usersService.changeUserStatus(userChangeStatusRequest,
            new BigInteger(userId)), HttpStatus.OK);
    }

    /**
     * API to create a new external user.
     *
     * @param userId  header param
     * @param externalUserDto requestbody
     * @return UserResponse
     * @throws ResourceNotFoundException ResourceNotFoundException
     */
    @Operation(
        summary = SUMMARY_ADD_EXTERNAL_USER,
        description = "Creates a new external user.",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PostMapping(path = PATH_EXTERNAL_USER)
    public ResponseEntity<UserResponseV1> addExternalUser(
        @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @Valid @RequestBody @Parameter(name = "Request payload",
            description = "Parameters that define a single external user")
              @JsonView(UserDtoViews.UserDtoV1ExternalView.class) ExternalUserDto externalUserDto) 
                throws ResourceNotFoundException {
        LOGGER.info("Add External User Resource Started, user: {}", externalUserDto.getUserName());
        UserResponseV1 userResponse = (UserResponseV1) usersService.addExternalUser(externalUserDto,
            new BigInteger(userId));
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    /**
     * API to delete single external user by its id.
     *
     * @param id userId of the external user.
     * @param userId header variable.
     * @return User details of the deleted user.
     * @throws ResourceNotFoundException Method throws exception if user details not found.
     */
    @Operation(summary = SUMMARY_DELETE_EXTERNAL_USER,
        description = "Deletes a single external user identified by its ID",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @DeleteMapping(value = PATH_EXTERNAL_USER + PATH_VARIABLE_ID)
    public ResponseEntity<UserResponseV1> deleteExternalUser(
        @RequestHeader(value = LOGGED_IN_USER_ID) String userId,
        @PathVariable(value = ID) @Parameter(description = USER_ID, required = true) BigInteger id)
        throws ResourceNotFoundException {
        LOGGER.info("delete external user request received for user id: {}", id);
        return new ResponseEntity<>(usersService.deleteExternalUser(id, new BigInteger(userId)), HttpStatus.OK);
    }

    /**
     * API to associate/dissociate accounts and roles to a specified user.
     *
     * @param loggedInUserId logged in userId
     * @param userId userId on whom association/dissociation of account and roles is performed
     * @param associationRequest request payload for association/dissociation
     * @throws UserAccountRoleMappingException when associations fails.
     * @throws ResourceNotFoundException  When loggedInUserId not found
     */
    @Operation(summary = ACCOUNT_ROLE_ASSOCIATION, description = ACCOUNT_ROLE_ASSOCIATION, tags = {
        USERS_TAG }, responses = { @ApiResponse(responseCode = HTTP_SUCCESS, description = SUCCESS) })
    @SecurityRequirement(name = "JwtAuthValidator",
        scopes = {"ManageUsers", "ManageUserRolesAndPermissions"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PatchMapping(path = ASSOCIATE_USERS_TO_ROLE_PATH)
    public ResponseEntity<AssociateAccountAndRolesResponse> associateAccountAndRoles(
        @RequestHeader(value = LOGGED_IN_USER_ID,
            required = false) BigInteger loggedInUserId,
        @Valid @PathVariable(value = PATH_USER_ID) @Parameter(description = "User ID",
            required = true) BigInteger userId,
        @RequestBody @Valid @Parameter(name = "Request payload",
            description = "Operations that associates/dissociates a user with given account and roles")
        List<AssociateAccountAndRolesDto> associationRequest)
        throws UserAccountRoleMappingException, ResourceNotFoundException {
        LOGGER.info("Association or dissociation of account and roles for the user with user id: {}", userId);
        return new ResponseEntity<>(usersService
            .associateUserToAccountAndRoles(loggedInUserId, associationRequest, userId), HttpStatus.OK);
    }

    /**
     * API to create a federated user.
     *
     * @param federatedUserDto request body for federated user.
     * @param userId  header param. logged in userId.
     * @return UserResponseV1
     * @throws ResourceNotFoundException when resource not exist
     */
    @Operation(
        summary = SUMMARY_ADD_FEDERATED_USER,
        description = "Creates a federated user.",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseV1.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { "ManageUsers" })
    @Parameter(name = LOGGED_IN_USER_ID, 
        description = LOGGED_IN_USER_ID, schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PostMapping(path = PATH_FEDERATED_USER)
    public ResponseEntity<UserResponseV1> addFederatedUser(
            @RequestHeader(value = LOGGED_IN_USER_ID, required = false) String userId,
            @RequestBody @Parameter(name = "Request payload", 
            description = "Parameters that define a federated user") @Valid 
            @JsonView(UserDtoViews.UserDtoV1FederatedView.class) FederatedUserDto federatedUserDto)
            throws ResourceNotFoundException {
        LOGGER.info("Add Federated User Resource Started, user: {}", federatedUserDto.getUserName());
        UserResponseV1 userResponse = (UserResponseV1) usersService.addFederatedUser(federatedUserDto,
                StringUtils.isNotEmpty(userId) ? new BigInteger(userId) : null);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    /**
     * API to update an external user details by passing user id.
     *
     * @param userId - user id(user who is trying to update user details) from api-gateway/token
     * @param id - id of user whose details needs to be updated
     * @param jsonPatch - update request parameters
     * @return updated response
     * @throws ResourceNotFoundException - thrown when user not found in system
     * @throws UserAccountRoleMappingException something wrong with account role mapping
     */
    @Operation(summary = SUMMARY_EDIT_EXTERNAL_USER,
        description = "Updates a single external user identified by its ID",
        tags = {USERS_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(
                implementation = UserResponseV1.class)))
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PatchMapping(path = PATH_EXTERNAL_USER + PATH_VARIABLE_ID)
    public ResponseEntity<UserResponseV1> editExternalUser(
        @RequestHeader(value = LOGGED_IN_USER_ID, required = false) String userId,
        @PathVariable(value = ID) @Parameter(description = USER_ID, required = true) BigInteger id,
        @RequestBody @Valid @Parameter(name = "Request payload",
            description = "Parameters that update a single external user",
            content = @Content(
                schema = @Schema(
                    type = "object",
                    additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
                    implementation = JsonPatch.class
                )
            )) JsonPatch jsonPatch
    ) throws ResourceNotFoundException, UserAccountRoleMappingException {
        LOGGER.info("update external user request received for user id: {}", id);
        return new ResponseEntity<>((UserResponseV1) usersService.editUser(id, jsonPatch,
                StringUtils.isNotEmpty(userId) ? new BigInteger(userId) : null, true, "v1"), HttpStatus.OK);
    }

    /**
     * API to retrieve the password policy. This may be further used over UI pages.
     *
     * @return ResponseEntity containing the PasswordPolicyResponse.
     */
    @Hidden
    @GetMapping(value = "/password-policy")
    public ResponseEntity<PasswordPolicyResponse> getPasswordPolicy() {
        LOGGER.info("Get Password Policy request received");
        PasswordPolicyResponse passwordPolicy = usersService.getPasswordPolicy();
        return new ResponseEntity<>(passwordPolicy, HttpStatus.OK);
    }    
}
