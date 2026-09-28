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
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserEventResponseDto;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserMetaDataResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
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

    List<UserMetaDataResponse> getSignupAttributes(Boolean dynamicAttribute);

    /**
     * Returns metadata for every additional attribute defined in the user_attributes table
     * (both dynamic and static-defined custom attributes).
     *
     * @return list of additional attribute metadata.
     */
    List<UserMetaDataResponse> getAllUserAttributes();

    UserEventResponseDto addUserEvent(UserEventsDto userEventsDto, String userId);

    List<UserMetaDataResponse> putUserMetaData(List<UserMetaDataRequest> userMetaDataRequests);

    /**
     * Deletes an additional attribute definition from the user_attributes table, along with
     * every stored value for that attribute in user_attribute_values.
     *
     * @param attributeName name of the attribute definition to delete.
     * @throws ResourceNotFoundException if no attribute definition exists with the given name.
     */
    void deleteUserAttribute(String attributeName) throws ResourceNotFoundException;

    /**
     * Returns a single user's additional attribute name/value pairs from user_attribute_values.
     *
     * @param userId user id.
     * @return map of attribute name to value.
     * @throws ResourceNotFoundException if the user does not exist.
     */
    Map<String, Object> getUserAttributeValues(BigInteger userId) throws ResourceNotFoundException;

    /**
     * Adds/updates a single user's additional attribute values in user_attribute_values.
     *
     * @param userId          user id.
     * @param attributeValues map of attribute name to value.
     * @return map of the user's attribute name to value after the update.
     * @throws ResourceNotFoundException if the user does not exist.
     */
    Map<String, Object> updateUserAttributeValues(BigInteger userId, Map<String, Object> attributeValues)
            throws ResourceNotFoundException;

    /**
     * Deletes a single user's stored value for one additional attribute from user_attribute_values.
     *
     * @param userId        user id.
     * @param attributeName name of the attribute value to delete.
     * @throws ResourceNotFoundException if the user, the attribute definition, or the stored value
     *      does not exist.
     */
    void deleteUserAttributeValue(BigInteger userId, String attributeName) throws ResourceNotFoundException;

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

    /**
     * Process and unlock blocked users for scheduled unlock.
     * This method is called by the scheduler to unlock users whose temporary lock period has expired.
     *
     * @param blockedUsers list of blocked users to process
     * @param temporaryLockPeriodMinutes the lock period in minutes
     * @return number of users successfully unlocked
     */
    int processBlockedUsersForScheduledUnlock(
            List<org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity> blockedUsers,
            Integer temporaryLockPeriodMinutes);

}
