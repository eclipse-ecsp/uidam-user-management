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
 * This class defines all log messages.
 */
public class LoggerMessages {
    private LoggerMessages() {
    }

    public static final String CREATE_SCOPE = "Create scope request, scope name: {} and requested by: {}";
    public static final String GET_SCOPE = "Get scope request, scope name: {}";
    public static final String UPDATE_SCOPE = "Update scope request, scope name: {} and requested by: {}";
    public static final String DELETE_SCOPE = "Delete scope request, scope name: {} and requested by: {}";
    public static final String FILTER_SCOPES = "Filter scopes request, scopes: {}";
    public static final String MANAGE_SCOPE_PERMISSION = "User: {} does not have permission to manage scope: {}";
    public static final String SCOPE_NOT_EXISTS = "Scope does not exist, scope name: {}";
    public static final String PREDEFINED_SCOPE = "Predefined Scope cannot be updated or deleted, scope name: {}";

    public static final String CREATE_ROLE = "Create role request, role name: {}, scopes: {}, and requested by: {}";
    public static final String GET_ROLE = "Get role request, role name: {}";
    public static final String GET_ROLE_BY_ID = "Get role by id request, role id: {}";
    public static final String FILTER_ROLES = "Filter roles request, roles: {}";
    public static final String UPDATE_ROLES = "Update role request, role name: {}, and requested by: {}";
    public static final String DELETE_ROLES = "Delete role request, role name: {}, and requested by: {}";

    public static final String DELETE_ROLE_NAME_EMPTY = "Provided role name is empty";
    public static final String MANAGE_ROLE_PERMISSION = "User does not have permission to manage this role, "
            + "username {}";
    public static final String ROLE_NOT_EXISTS = "Role does not exists, role name: {}";
    public static final String ROLE_NOT_EXISTS_ID = "Role does not exists, role id: {}";
    public static final String ROLE_EXISTS = "Role already exists with role name : {}";

    public static final String SCOPE_EXISTS = "Scope already exists with scope name : {}";
    public static final String USERMANAGEMETN_EMPTY_RESPONSE = "Could not get scope details for username: {}";
    public static final String USERMANAGEMENT_NOT_FOUND_ERROR = "Could not find user details userId: {}";
    public static final String DELETE_COUNT_ROLE_SCOPE_MAPPING = "delete count role_scope_mapping {} for role id:{}";

    public static final String ROLE_MAPPED_WITH_USER = "Cannot delete role as already mapped with a user. "
        + "role name: {}";
    public static final String ROLE_NOT_MAPPED_WITH_USER = "Role not mapped with any user. role name: {}";

    public static final String APPLICATION_ERROR_MESSAGE = "Application Error encountered:";
    public static final String CLIENT_REGISTRATION_ERROR_MESSAGE = "ClientRegisterationException encountered: ";
    public static final String VALIDATION_ERROR_MESSAGE = "Received validation exception: ";
    public static final String DATA_INTEGRITY_ERROR_MESSAGE = "Data integrity error encountered:";
    public static final String METHOD_ARGUMENT_ERROR_MESSAGE = "Method argument is invalid: errors:";
    public static final String RESOURCE_NOT_FOUND_ERROR_MESSAGE = "Resource not found error encountered:";
    public static final String INACTIVE_USER_ERROR_MESSAGE = "Inactive user exception encountered:";
    public static final String RECOVERY_SECRET_ERROR_MESSAGE = "Recovery secret expires exception encountered:";
    public static final String RECORD_ALREADY_EXISTS_ERROR_MESSAGE = "RecordAlreadyExistsException ";
    public static final String SCOPE_NOT_EXISTS_ERROR_MESSAGE = "ScopeNotExists ";
    public static final String ENTITY_NOT_EXISTS_ERROR_MESSAGE = "EntityNotFoundException ";
    public static final String PERMISSION_DENIED_ERROR_MESSAGE =  "PermissionDeniedException ";
    public static final String MISSING_REQUEST_HEADER_ERROR_MESSAGE =  "MissingRequestHeaderException ";
}
