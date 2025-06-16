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

import com.github.fge.jsonpatch.JsonPatch;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.exception.InActiveUserException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecoverySecretExpireException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserAccountRoleMappingException;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.AssociateAccountAndRolesDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.FederatedUserDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserChangeStatusRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoBase;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserEventsDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserMetaDataRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserUpdatePasswordDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersDeleteFilter;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.AssociateAccountAndRolesResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.PasswordPolicyResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserDetailsResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserMetaDataResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;

/**
 * UsersService to be called for CRUD APIs.
 */
public interface UsersService {

    UserResponseBase addUser(UserDtoBase userDto, BigInteger userId, boolean isSelfAddUser)
            throws ResourceNotFoundException;

    UserResponseBase getUser(BigInteger userId, String apiVersion) throws ResourceNotFoundException;

    UserDetailsResponse getUserByUserName(String userName) throws ResourceNotFoundException, InActiveUserException;

    UserResponseBase editUser(BigInteger id, JsonPatch jsonPatch, BigInteger userId, boolean isExternalUser,
            String apiVersion) throws ResourceNotFoundException, UserAccountRoleMappingException;

    UserResponseV1 deleteUser(BigInteger id, Boolean isUserExternal, BigInteger loggedInUserId)
            throws ResourceNotFoundException;

    UserResponseV1 deleteExternalUser(BigInteger id, BigInteger loggedInUserId) throws ResourceNotFoundException;

    List<UserResponseV1> deleteUsers(UsersDeleteFilter usersDeleteFilter) throws ResourceNotFoundException;

    List<UserResponseBase> getUsers(UsersGetFilterBase userGetFilter, Integer pageNumber, Integer pageSize,
            String sortBy, String sortOrder, boolean ignoreCase, SearchType searchType)
            throws ResourceNotFoundException;

    List<UserMetaDataResponse> getUserMetaData();

    void addUserEvent(UserEventsDto userEventsDto, String userId);

    List<UserMetaDataResponse> putUserMetaData(List<UserMetaDataRequest> userMetaDataRequests);

    void updateUserPasswordUsingRecoverySecret(UserUpdatePasswordDto userUpdatePasswordDto)
            throws ResourceNotFoundException, RecoverySecretExpireException;

    void sendUserRecoveryNotification(String username,
            boolean isUserId)
            throws ResourceNotFoundException, MalformedURLException, UnsupportedEncodingException;

    List<UserResponseV1> changeUserStatus(UserChangeStatusRequest userChangeStatusRequest, BigInteger userId);

    UserResponseBase addExternalUser(UserDtoBase externalUserDto, BigInteger userId) throws ResourceNotFoundException;

    AssociateAccountAndRolesResponse associateUserToAccountAndRoles(BigInteger loggedInUserId,
            List<AssociateAccountAndRolesDto> associationRequest, BigInteger userId)
            throws UserAccountRoleMappingException, ResourceNotFoundException;

    UserResponseBase addFederatedUser(FederatedUserDto federatedUserDto, BigInteger userId)
            throws ResourceNotFoundException;

    boolean hasUserPermissionForScope(BigInteger loggedInUserId, Set<String> scopes);

    PasswordPolicyResponse getPasswordPolicy();

}
