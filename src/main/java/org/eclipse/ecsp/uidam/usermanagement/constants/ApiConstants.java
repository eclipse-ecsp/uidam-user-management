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
 * File comprising API Constants.
 */
public final class ApiConstants {

    private ApiConstants() {

    }

    public static final String COMPONENT_NAME = "UIDAM_USER_MANAGEMENT";
    public static final String SAVED_USER_ID_MESSAGE = "savedUserId: {}";
    public static final String ACCOUNT_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE = "accountId in accountRoleMapping: {}";
    public static final String ROLE_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE = "roleId in accountRoleMapping: {}";
    public static final long NO_ROLEID_FOR_FILTER = -1L;

    public static final String VERSION_V1 = "/v1";
    public static final String VERSION_V2 = "/v2";
    public static final String USER_ID_NOT_PRESENT_IN_THE_DB = "um.user.not.found";
    public static final String ACCOUNT_AND_ROLE_ASSOCIATION_ERROR_MSG = "Invalid Payload for"
            + " Operation:{0} path:{1} value:{2}";
    public static final String USER_ID_NOT_FOUND_MESSAGE = "User Id is not found {0}";
    public static final String INVALID_OP_PATTERN  = "um.invalid.input.op.pattern";
    public static final String INVALID_PATH_PATTERN  = "um.invalid.input.path.pattern";
    public static final String OP_PATTERN_FOR_ROLE_ASSOCIATION = "^remove|add|ADD|REMOVE$";
    public static final String ASSOCIATION_REQ_ERROR_MESSAGE = "Application Error encountered:"
            + " message {} and error properties are {} ";
    public static final String USER_ACCOUNT_ROLE_ASSOCIATION_CODE = "um.user.account.role.association";
    public static final String INVALID_PAYLOAD_ERROR_MESSAGE = "Invalid"
            + " payload for user account role association";

    public static final String INVALID_ACCOUNT_ID_AND_ROLES_ERROR_MESSGAE = "Invalid"
            + " Account ids or role names in the payload";

    public static final String USER_DISASSOCIATE_ERROR_MSG = "User should be"
            + " associated to atleast one account";
    public static final String OP_FIELD_DESCRIPTION = "Incorrect Op key. Allowed values are: [ADD|REMOVE]";
    public static final String INVALID_PAYLOAD_FOR_ACCOUNT_ROLE_ASSOCIATION = "Invalid payload"
            + " for user {0} and the payload is {1}";
    public static final String ACCOUNT_ROLE_ASSOCIATION = "Associate Users to Accounts with Roles";
    public static final String AMPERSAND  = "&";
    public static final String ADD_ROLE_PATH_REGEX = "^/account/.*/roleName$";
    public static final String REMOVE_ROLE_PATH_REGEX = "^/account/.*/.*$";
    //Allowed paths examples are /account/12345  or /account/12345/vehicle_admin or /account/12345/roleName
    public static final String ASSOCIATE_USER_ACCOUNT_ROLE_REGEX =
            "^/account/\\d+(/[A-Za-z0-9_-]+){0,1}$";
    public static final String ASSOCIATE_ACCOUNT_PATH_REGEX = "^/account/\\d+$";
    public static final String REMOVE_OPERATION = "remove";
    public static final String ADD_OPERATION = "add";
    public static final String USER_RESOURCE_PATH = "/users";
    public static final String PASS_LIST = "\n\r ** &#124;#$%&'()*+,-./:;<=>?@[]^_`{}~ **";
    public static final String USERNAME_FIELD_DESCRIPTION = "Username. Maximum length = 254 chars";
    public static final String PASS_FIELD_DESCRIPTION =
        "\n\r User password. Password length should be between 6 to 80 "
            + "chars and must use at least three of the four available character types: "
            + "lowercase letters, uppercase letters, "
            + "numbers, and any character from the following list: \n" + PASS_LIST;
    public static final String ROLE_FIELD_DESCRIPTION =
        "Role. Allowed values are: [VEHICLE_OWNER, OEM_ADMIN, BUSINESS_ADMIN] or one of the defined custom roles";
    public static final String ROLE_REGEXP = "^[A-Za-z0-9_-]+";
    public static final String SUMMARY_ADD_USER = "Add a new user";
    public static final String SUMMARY_ADD_EXTERNAL_USER = "Add a new external user";

    public static final String DESCRIPTION_ADD_SELF_USER = """
        Creating end user have the following restrictions:

        Role field always get the value: 'VEHICLE_OWNER'

        Can not add devIds field
        """;
    public static final String IDENTITY_PROVIDER_NAME_FIELD_DESCRIPTION = "Identity Provider Name. The name of the "
        + "provider that identified this user";
    public static final String SUMMARY_ADD_FEDERATED_USER = "Add a federated user";
    public static final String PATH_FEDERATED_USER = "/federated";
    public static final String UNDERSCORE = "_";
    public static final String NULL_AND_EMPTY_USER_PASSWORD = "Password should not be null or empty.";
    public static final String INVALID_INPUT_PASSWORD = "Password shouldn't be provided.";
    public static final String INVALID_EXTERNAL_USER = "User should be external user";
    public static final String INVALID_INPUT_ROLE = "invalid.input.role";
    public static final String SUMMARY_CHANGE_USER_STATUS = "Change user status";
    public static final String SUMMARY_CHANGE_USER_STATUS_DESCRIPTION = "Change status for users.";
    public static final String USER_IDS_DESCRIPTION = "List of user IDs.";
    public static final String CORRELATION_ID = "correlationId";
    public static final String TENANT_ID = "X-tenant-id";
    public static final String IF_NONE_MATCH = "If-None-Match";
    public static final String CORRELATION_ID_UI = "Correlation ID";
    public static final String TENANT_ID_UI = "Tenant ID";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";
    public static final String PATH = "path";
    public static final String USERS_TAG = "users";
    public static final String EMAIL_VERIFICATION_TAG = "email";
    public static final String END_USER_TAG = "end user";
    public static final String ERROR_INVALID_DATA = "INVALID_DATA";
    public static final String INVALID_OBJECT = "invalid.object";
    public static final String RESULTS = "results";
    public static final String USER = "User";
    public static final String USERS = "Users";
    public static final String STATUS_FIELD_DESCRIPTION =
        "Status. Allowed values are: [PENDING, REJECTED, ACTIVE, DEACTIVATED]";
    public static final String IS_EXTERNAL_USER_DESCRIPTION =
        "User is External or not. Allowed values are: [true, false],for external user it should be true";
    public static final String CLIENT_ID_DESCRIPTION = "Client_Id/ AUD,mandatory "
            + "when user created with client credentials flow";
    public static final String SUMMARY_GET_USER = "Get a user";

    public static final String SUMMARY_GET_EXTERNAL_USER = "Get external user";
    public static final String SUMMARY_GET_USER_ATTRIBUTES = "Get user attributes";
    public static final String SUMMARY_PUT_USER_ATTRIBUTES = "Add/Modify additional attributes to user";
    public static final String SUMMARY_GET_SELF_USER = "Get my user data";

    public static final String SUMMARY_ADD_SELF_USER = "Create end user.";
    public static final String USERS_SELF_PATH = "/self";
    public static final String SUMMARY_EDIT_USER = "Update a user";
    public static final String SUMMARY_EDIT_EXTERNAL_USER = "Update an external user";
    public static final String SUMMARY_SELF_EDIT_USER = "Update logged in user.";
    public static final String SUMMARY_DELETE_USER = "Delete a user";
    public static final String SUMMARY_SELF_DELETE_USER = "Delete my own user";
    public static final String SUMMARY_SELF_RESET = "Reset password by the self user";
    public static final String SUMMARY_DELETE_EXTERNAL_USER = "Delete an external user";
    public static final String PATH_EXTERNAL_USER = "/external";
    public static final String SUMMARY_CHANGE_USER_PASS = "Change user password";
    public static final String PATH_PASS = "/password";
    public static final String USER_ID = "User ID";
    public static final String PATH_VARIABLE_ID = "/{id}";
    public static final String PATH_GET_EXTERNAL_USER = PATH_EXTERNAL_USER + PATH_VARIABLE_ID;
    public static final String PATH_CHANGE_STATUS = "/status";
    public static final String BUILDER_NAME = "custom";
    public static final String PATH_VARIABLE_USERNAME = "/{userName}";
    public static final String PATH_USER_ATTRIBUTES = "/attributes";
    public static final String CAPTCHA_REQUIRED = "required";
    public static final String CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES = "enforceAfterNoOfFailures";
    public static final String PATH_BY_USERNAME = "/byUserName";
    public static final String PATH_USER_ID = "userId";
    public static final String ASSOCIATE_USERS_TO_ROLE_PATH = "/{" + PATH_USER_ID + "}/accountRoleMapping";
    public static final String PATH_VARIABLE_TENANT_ID = "/{tenantId}";
    public static final String ID = "id";
    public static final String USERNAME = "userName";
    public static final String USERNAME2 = "username";

    public static final String USERNAME_FOR_ERROR_MSG = "Username";
    public static final String PASSWORD = "password";
    public static final String IS_EXTERNAL_USER = "is_external_user";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String PASSWORD_CONTAINS_USER_NAME = "Password contains Username";
    public static final String WEAK_PASSWORD = "Password is weak, try using some strong password";
    public static final String USER_OR_PASSWORD_NOT_NULL = "Username or Password is null";
    public static final String PASSWORD_CANT_BE_CHANGED = "Password can't be changed as it was changed recently";
    public static final String OLD_PASSWORD_CANT_BE_REUSED = "Old password cannot be reused";
    public static final String PASSWORD_EXPIRED = "Password expired, please reset password";
    public static final String LAST_PASSWORD_UPDATE_TIME = "lastPasswordUpdateTime";
    public static final String MIN_PASSWORD_LENGTH_VIOLATION = "Password length is less than"
            + " allowed minimum password length";
    public static final String MAX_PASSWORD_LENGTH_VIOLATION = "Password length is more than"
            + " allowed maximum password length";
    public static final String PASSWORD_PATTERN_VIOLATION = "Password should contain One UpperCase"
            + " and One Lowercase one One Special Character";
    public  static final int MIN_CONSECUTIVE_LETTERS_LENGTH = 2;
    public static final String SUMMARY_GET_USERS_BY_FILTER = "Retrieve users that match defined criteria";
    public static final String SUMMARY_DELETE_USERS_BY_FILTER = "Delete users that match defined criteria";
    public static final String SUMMARY_EMAIL_VERIFICATION_GET = "Get if user email was verified by user id";
    public static final String SUMMARY_EMAIL_VERIFY = "Verify email address via UUID";
    public static final String PATH_FILTER = "/filter";
    public static final String DESCENDING = "DESC";
    public static final String ASCENDING = "ASC";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String SEARCHABLE = "searchableOnly";
    public static final String PAGE_FROM_ENTITIES = "from";
    public static final String PAGE_SIZE_ENTITIES = "size";
    public static final String PAGE_NUMBER_DESCRIPTION = "Page number to retrieve; the first page is 0";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_SIZE_DESCRIPTION = "Number of items to display per page";
    public static final String SORT_BY = "sortBy";
    public static final String SORT_BY_DESCRIPTION = "Parameters to sort by";
    public static final String SORT_ORDER = "sortOrder";
    public static final String IGNORE_CASE = "ignoreCase";
    public static final String SEARCH_TYPE = "searchType";
    public static final String SORT_ORDER_DESCRIPTION =
        "Sort in ascending (ASC) or descending (DESC) order; default: descending";
    public static final String IGNORE_CASE_DESCRIPTION = "Ignore case, true or false";
    public static final String SEARCH_TYPE_DESCRIPTION = "Search type, CONTAINS, PREFIX or SUFFIX";
    public static final String PAGE_NUMBER_DEFAULT = "0";
    public static final String PAGE_SIZE_DEFAULT = "20";
    public static final String SORT_BY_DEFAULT = "";
    public static final String USERNAME_REGEXP = "[^\\\\!()*~<>'\\\",:;\\s${}|+?%]{1,254}$";
    public static final String FIELD_NAME_REGEX = "^[^_][a-z,A-Z,0-9_]{1,79}$";
    public static final String ETAG_HEADER_NAME = "etag";
    public static final String LOGGED_IN_USER_ID = "user-id";
    public static final String SCOPE_HEADER = "scope";
    public static final String ACCOUNT_NAME = "accountName";
    public static final String ACCOUNT_TYPE = "accountType";
    public static final String ORIGINAL_USERNAME = "original_username";
    public static final String USER_ENTITY_TABLE_NAME = "user";
    public static final String USER_ADDRESS_ENTITY_TABLE_NAME = "user_address";
    public static final String EMAIL_VERIFICATION_TABLE_NAME = "user_verification";
    public static final String USER_ACCOUNT_ROLE_MAPPING_TABLE_NAME = "user_account_role_mapping";
    public static final String USER_PASSWORD_HISTORY_TABLE_NAME = "user_password_history";

    public static final String PATH_IS_EMAIL_VERIFIED = "isEmailVerified";

    // New tenant-first email verification path
    public static final String PATH_EMAIL_VERIFICATION_TENANT = "/{tenantId}" + VERSION_V1 + "/emailVerification";
    public static final String PATH_EMAIL_VERIFICATION_GET = "/{" + PATH_USER_ID + "}/" + PATH_IS_EMAIL_VERIFIED;

    public static final String USERID_IS_NULL = "UserID is null";
    public static final String CAPTCHA_REQUIRED_ATTRIBUTE = "captchaRequired";
    public static final String FIRSTNAME = "firstName";
    public static final String ERROR = "error";
    public static final String LASTNAME = "lastName";
    public static final String SCOPES = "scopes";
    public static final String USER_ID_VARIABLE = "userId";
    public static final String TENANT_ID_VARIABLE = "tenantId";
    public static final String TOKEN = "token";
    public static final String UPDATE_TIME = "updateTime";
    public static final String IS_VERIFIED = "isVerified";
    public static final String EXTERNAL_USER = "external_user";
    public static final String SYSTEM = "system";
    public static final String SLASH = "/";
    public static final String UNKNOWN = "Unknown";
    public static final String HTTP_OK = "200";
    public static final String HTTP_SUCCESS = "201";
    public static final String SUCCESS = "Success";
    public static final String UNKNOWN_DB_ERROR = "unknown.database.error";
    public static final String UNKNOWN_DB_ERROR_MSG = "Unknown database error";
    public static final String USER_EVENTS_PATH = "/{id}/events";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String EMAIL_BODY = "body";
    public static final String EMAIL_FOOTER = "footer";
    public static final String FOOTER_MESSAGE = "Thanks";
    public static final String UIDAM = "uidam";
    public static final String REQUEST_ID = "requestId";
    public static final String USER_RECOVERY_SECRET_NOTIFICATION_PATH = "/{userName}/recovery/forgotpassword";
    public static final String USER_RECOVERY_SET_PASSWORD_PATH = "/recovery/set-password";
    public static final String USER_PASSWORD_RECOVERY_SECRET = "secret=";
    public static final String USER_PASSWORD_RECOVERY_REDIRECT_URL = "redirect_url=";
    public static final String USER_PASSWORD_RECOVERY_EMAIL_CONTENT_PART1 = "We received a request to reset "
            + "the password for the ";
    public static final String USER_PASSWORD_RECOVERY_EMAIL_CONTENT_PART2 = " account that is associated with"
            + " this email address."
            + " If you made this request, please click the button below to securely reset your password. <br />";
    public static final String USER_PASSWORD_RECOVERY_EMAIL_HREF = "Reset Password";
    public static final String USER_PASSWORD_RECOVERY_EMAIL_LINK_NOT_WORK = "<br /> If clicking the button"
            + " doesn't seem to work,"
            + " you can copy and paste the following link into your browser.<br />";
    public static final String USER_PASSWORD_RECOVERY_EMAIL_NOT_REQUESTED = "<br /> If you did not request "
            + "to have your password reset,"
            + " disregard this email and no changes to your account will be made.";

    public static final String SUMMARY_VERIFY_USER = "Verify user's email";

    public static final String PATH_VALIDATE_EMAIL = "/{" + TOKEN + "}";
    public static final String PATH_RESEND_EMAIL_VERIFY = "resendEmailVerification";
    public static final String PATH_RESEND_EMAIL_VERIFICATION_PUT = "/{" + USER_ID_VARIABLE + "}/"
        + PATH_RESEND_EMAIL_VERIFY;
    public static final String EMAIL_SUBJECT = "subject";
    public static final String USER_PASSWORD_RECOVERY_EMAIL_SUBJECT = "UIDAM notification";
    public static final String EMAIL_SALUTATION = "salutation";
    public static final String GREETINGS = "Greetings";
    public static final String EMAIL_TO_NAME = "name";

    public static final String EMAIL_VERIFY_ERROR = "?success=error";
    public static final String EMAIL_VERIFY_SUCCESS = "?success=true";
    public static final String EMAIL_VERIFY_FAILED = "?success=false";

    //auth-management constants
    public static final String API_VERSION = "/v1";
    public static final String SCOPE_RESOURCE_PATH = "/scopes";
    public static final String MANAGEMENT_SCOPE = "ManageUserRolesAndPermissions";

    public static final String ROLES_RESOURCE_PATH = "/roles";
    public static final String ACCOUNT_RESOURCE_PATH = "/account";
    public static final String ROLES_RESOURCE_BY_ID_PATH = "/rolesById";
    public static final String ROLES_SCOPES_FILTER_PATH = "/filter";

    public static final String CLIENT_RESOURCE_PATH = "/oauth2/client";

    public static final String SPACE = " ";
    public static final String COMMA = ",";
    public static final String ROLES = "roles";

    public static final String ENTITY_SCOPE = "scopes";

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String SCOPE_NAMES = "scopeNames";

    public static final String USER_MANAGEMENT_FILTER_API_PATH = "/v1/users/filter";
    public static final String USER_SELF_RECOVERY_FORGOT_PWD_API_PATH = "/self/recovery/resetpassword";
    public static final String OPERATION = "op";
    public static final String ADD = "add";
    public static final String REPLACE = "replace";
    public static final String VALUE = "value";

    //UIDAM Auth
    public static final String UIDAM_AUTH_CLIENT_CREDENTIALS = "client_credentials";
    public static final String UIDAM_AUTH_CLIENT_ID = "client_id";
    public static final String UIDAM_AUTH_CLIENT_SECRET = "client_secret";
    public static final String UIDAM_AUTH_SCOPE_VALUE = "RevokeToken";
    public static final String GRANT_TYPE_KEY = "grant_type";
    public static final String SCOPE_KEY = "scope";

    //Users V2 fields
    public static final String ACCOUNTS_FIELD_DESCRIPTION = "Associated Accounts";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final long SEC_60 = 60;

    /**
     * Cloud Profile API Constants.
     */
    public static class CloudProfileApiConstants {
        private CloudProfileApiConstants() {

        }

        public static final String CLOUD_PROFILE_STRING = "CloudProfile";
        public static final String CLOUD_PROFILE_ENTITY_NAME = "/cloud-profile";
        public static final String CLOUD_PROFILE_NAME = "cloudProfileName";
        public static final String PROFILE_NAME = "Profile name";
        public static final String SUMMARY_GET_CLOUD_PROFILE_FOR_BUSINESS_KEY = "Get my user cloud setting";
        public static final String SUMMARY_GET_CLOUD_PROFILE_ETAG = "Get my user cloud profile (setting) etag";
        public static final String SUMMARY_GET_CLOUD_PROFILE_BY_USER_ID =
            "Get my user cloud profile (setting) names and etags";
        public static final String SUMMARY_UPDATE_CLOUD_PROFILE_FOR_ID =
            "Create or update a new cloud profile for user";
        public static final String SUMMARY_EDIT_CLOUD_PROFILE_FOR_ID = "Create or update a new cloud profile for user";
        public static final String SUMMARY_DELETE_CLOUD_PROFILE_FOR_ID = "Delete a cloud profile for user";
        public static final String CLOUD_PROFILE_API_PATH = USERS_SELF_PATH + "/settings";
        public static final String PATH_CLOUD_PROFILE_MAP = "/{" + CLOUD_PROFILE_NAME + "}/map";
        public static final String PATH_VARIABLE_CLOUD_PROFILE = "/{" + CLOUD_PROFILE_NAME + "}";
        public static final String DELETED = "DELETED";
        public static final String ACTIVE = "ACTIVE";

    }
}
