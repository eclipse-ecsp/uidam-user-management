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

package org.eclipse.ecsp.uidam.accountmanagement.constants;

/**
 * This class contains constants related to the Account Management API.
 */
public final class AccountApiConstants {

    private AccountApiConstants() {
    }

    public static final String ACCOUNT_V1_VERSION = "/v1";
    public static final Character ESCAPE_CHARACTER = '\\';
    public static final String ACCOUNT_RESOURCE_PATH = "/accounts";
    public static final String ACCOUNT_FILTER_PATH = "/filter";
    public static final String PATH_VARIABLE_ACCOUNT_ID = "/{account_id}";
    public static final String REQUEST_PAYLOAD = "Request payload";
    public static final String ACCOUNT_ID = "account_id";
    public static final String ACCOUNT_NAME = "account_name";
    public static final String SUMMARY_ADD_ACCOUNT = "Add a new account";
    public static final String SUMMARY_GET_ACCOUNT = "Get account details";
    public static final String SUMMARY_GET_ACCOUNT_DESC = "Gets the account identified by its ID";
    public static final String SUMMARY_CREATE_ACCOUNT = "Create a new account";
    public static final String SUMMARY_DELETE_ACCOUNT = "Delete an existing account";
    public static final String SUMMARY_DELETE_ACCOUNT_DESC = "Account must not be default and no users associated";
    public static final String SUMMARY_UPDATE_ACCOUNT = "Update an existing account";
    public static final String ACCOUNT_TAG = "account";
    public static final String ACCOUNT_ENTITY_TABLE_NAME = "account";
    public static final String ACCOUNT_REGEX = "[^\\\\!()*~<>'\\\",:;${}|+?%]{1,254}$";
    public static final String INVALID_INPUT_ROLE = "um.invalid.input.role";
    public static final String INVALID_INPUT_ROLE_MSG = "Invalid roles detected: {0}";
    public static final String INVALID_INPUT_ACCOUNT_NAME_PATTERN = "um.invalid.input.accountname.pattern";
    public static final String INVALID_INPUT_ACCOUNT_NAME_PATTERN_MSG = "Invalid account name pattern: {0}";
    public static final String ACCOUNT_ALREADY_EXISTS = "um.account.already.exists";
    public static final String ACCOUNT_ALREADY_EXISTS_MSG = "Account name already exists: {0}";
    public static final String ACCOUNT_DOES_NOT_EXIST = "um.account.does.not.exist";
    public static final String ACCOUNT_DOES_NOT_EXIST_MSG = "The account does not exist: {0}";
    public static final String ACCOUNT_ROLE_VALIDATION_FAILURE = "um.account.role.validation.failure";
    public static final String ACCOUNT_ROLE_VALIDATION_FAILURE_MSG = "An exception occurred while"
            + " retrieving roles for the account: {0}";
    public static final String CANNOT_DELETE_DEFAULT_ACCOUNT = "um.cannot.delete.default.account";
    public static final String CANNOT_DELETE_DEFAULT_ACCOUNT_MSG = "Not allowed to delete the default account: {0}";
    public static final String CANNOT_UPDATE_DEFAULT_ACCOUNT = "um.cannot.update.default.account";
    public static final String CANNOT_UPDATE_DEFAULT_ACCOUNT_MSG = "Not allowed to update the default account: {0}";
    public static final String INVALID_ACCOUNT_STATUS = "um.invalid.account.status";
    public static final String INVALID_ACCOUNT_STATUS_MSG = "The account status cannot be changed"
            + " to DELETED for account ID: {0}";
    public static final int MIN_ACCOUNTNAME_LENGTH = 1;
    public static final int MAX_ACCOUNTNAME_LENGTH = 254;
    public static final String SEARCH_MODE = "searchMode";
    public static final String SEARCH_MODE_DESCRIPTION = "Search type, CONTAINS, PREFIX, SUFFIX or EQUAL";
    public static final String SORT_BY_DEFAULT_FOR_FILTER_ACCOUNTS = "ACCOUNT_NAMES";
    public static final String IGNORE_CASE_DEFAULT_FOR_FILTER_ACCOUNTS = "false";
    public static final String SEARCH_MODE_DEFAULT_FOR_FILTER_ACCOUNTS = "EQUAL";
    public static final String INVALID_ELEMENT_LENGTH = "invalid.element.length";
    public static final String CANNOT_DELETE_ASSOCIATED_ACCOUNT_CODE = "um.account.cannot.delete.association";
    public static final String CANNOT_DELETE_ASSOCIATED_ACCOUNT_MSG = "Account is associated to user,"
            + " so not allowed to delete the account: {0}";
    public static final String INVALID_COUNT = "invalid.number.of.search.items";
    public static final String INVALID_NULL_ELEMENT = "invalid.null.element";
    public static final String INVALID_ROLE_PATTERN = "invalid.role.pattern";
    public static final int MAX_ROLE_LENGTH = 49;
    public static final String LIST_ACCOUNTS_BY_FILTER = "Retrieve accounts that match the defined criteria";
    public static final int MINIMUM_PATTERN_LENGTH = 1;

    /**
     * This class contains comments for the fields in the AccountApiConstants class.
     */
    public static class FieldComments {
        private FieldComments() {
        }

        public static final String ACCOUNT_NAME = "Account name. Maximum length is 254 chars";
        public static final String PARENT_ID = "Parent ID of the account";
        public static final String STATUS_FIELD_DESCRIPTION = "Account Status. Allowed values are: "
                + "[PENDING, ACTIVE, SUSPENDED, BLOCKED, DELETED]";

    }

}
