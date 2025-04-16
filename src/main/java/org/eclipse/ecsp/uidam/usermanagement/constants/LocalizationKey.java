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

package org.eclipse.ecsp.uidam.usermanagement.constants;

/**
 * Error keys to be sent in message segment of errorDetails response to endUser.
 */
public interface LocalizationKey {

    /**
     * The following parameter {0} is missing.
     */
    String MISSING_MANDATORY_PARAMETERS = "missing.mandatory.parameters";
    String INVALID_INPUT_PASSWORD_PATTERN = "invalid.input.password.pattern";
    String USER_PASSWORD_UPDATE_FAILED = "user.password.update.failed";
    String USER_NOT_VERIFIED = "user.not.verified";
    String USER_NOT_FOUND = "user.not.found";
    String ATTRIBUTE_ALREADY_EXIST = "attribute.already.exist";
    String USER_IS_BLOCKED = "user.is.blocked";

    /**
     * Body cannot be empty.
     */
    String EMPTY_BODY = "empty.body";

    /**
     * The following set {0} should contain elements between {1} and {2}.
     */
    String INVALID_ELEMENT_LENGTH = "invalid.element.length";

    /**
     * The following parameter {0} contains invalid element.
     */
    String INVALID_NULL_ELEMENT = "invalid.null.element";

    /**
     * The following parameter {0} must be between {1} and {2}.
     */
    String INVALID_LENGTH = "invalid.length";
    String INVALID_ROLE_PATTERN = "invalid.role.pattern";

    String INVALID_EMAIL = "invalid.email";
    String INVALID_PHONE_NUMBER = "invalid.phone.number";

    /**
     * EXCEPTION KEYS.
     */
    String INTERNAL_ERROR = "internal.error";
    String INVALID_API_VERSION = "um.invalid.api.version";
    String INVALID_INPUT_PASS_PATTERN = "invalid.input.password.pattern";
    String INVALID_INPUT_EMAIL_PATTERN = "invalid.input.email.pattern";
    String INVALID_INPUT_USERNAME_PATTERN = "invalid.input.username.pattern";
    String INVALID_INPUT_FIELD_NAME_PATTERN = "invalid.input.field.name.pattern";
    String INVALID_INPUT_USERID_PATTERN = "invalid.input.userid.pattern";
    String INVALID_INPUT_ROLE_PATTERN = "invalid.input.role.pattern";
    String INVALID_INPUT_ACCESS_TOKEN_PATTERN = "invalid.input.access.token.pattern";
    String INVALID_INPUT_PASS_CANNOT_CONTAIN_USERNAME =
        "invalid.input.password.cannot.contain.username";
    String INVALID_INPUT_USERNAME_CANNOT_START_WITH = "invalid.input.username.cannot.start.with";
    /**
     * Failed to associate device {0} to user.
     */
    String ASSOCIATE_DEVICE_FAILURE = "associate.device.failure";

    /**
     * Failed to disassociate device from user.
     */
    String DISASSOCIATE_DEVICE_FAILURE = "disassociate.device.failure";

    /**
     * User does not have email addresses.
     */
    String EMAIL_NOT_FOUND = "email.not.found";

    String ADD_USER_TO_IDENTITY_SERVER_FAILED = "add.user.to.identity.server.fails";
    String IGNITE_SYSTEM_SCOPE = "UIDAMSystem";
    String MANAGE_USERS_SCOPE = "ManageUsers";
    String MANAGE_ACCOUNTS_SCOPE = "ManageAccounts";
    String VIEW_ACCOUNTS_SCOPE = "ViewAccounts";
    String VIEW_USERS_SCOPE = "ViewUsers";
    String FIELD_IS_UNIQUE = "field.is.unique";
    String FIELD_DATA_IS_INVALID = "field.data.is.invalid";
    String ATTRIBUTE_METADATA_IS_MISSING = "attribute.metadata.is.missing";
    String FIELD_NOT_FOUND = "field.not.found";
    String ACTION_FORBIDDEN = "action.forbidden";
    String DELETE_OPERATION_FAILED = "delete.operation.failed";
    String USER_ROLES_NOT_FOUND = "user.roles.not.found";
    String FIELD_CANNOT_BE_MODIFIED = "field.cannot.be.modified";
    String MISSING_CORRELATION_ID = "missing.correlation.id";

    //auth-management
    String PARSING_FAILURE = "parsing.failure";
    String MISSING_REQUEST_HEADER = "missing.request.header";
    String BAD_REQUEST = "bad.request";
    String RESOURCE_NOT_FOUND = "resource.not.found";
    String BAD_GATEWAY = "bad.gateway";

    String AUTHENTICATION_FAILED = "authentication.failed";
    String PERMISSION_DENIED = "User does not have permission to perform this operation";
    String SCOPE_UPDATE_FAILED = "Cannot update predefined scope.";
    String SCOPE_DELETE_FAILED = "Cannot delete predefined scope.";
    String SCOPE_DELETE_FAILED_MAPPED_WITH_ROLE = "Cannot delete a scope assigned "
        + "to an existing role.";
    String ROLE_DELETE_FAILED_MAPPED_WITH_USER = "Cannot delete a role assigned "
        + "to a user.";
    String SUCCESS_KEY = "success.key";

    String INVALID_FIELD = "invalid.field";
    String GET_ENTITY_FAILURE = "get.entity.failure";
    String EDIT_ENTITY_FAILURE = "edit.entity.failure";
    String SCOPE_DOES_NOT_EXIST = "Scope does not exist";
    String DELETE_ENTITY_FAILURE = "delete.entity.failure";
    String ROLE_DOES_NOT_EXIST = "Role does not exist";
}
