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

package org.eclipse.ecsp.uidam.accountmanagement.controller;

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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.AccountFilterDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.CreateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.UpdateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.FilterAccountsApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.GetAccountApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.exception.AccountManagementException;
import org.eclipse.ecsp.uidam.accountmanagement.service.AccountService;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.enums.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_FILTER_PATH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ID;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_TAG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_V1_VERSION;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.IGNORE_CASE_DEFAULT_FOR_FILTER_ACCOUNTS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.LIST_ACCOUNTS_BY_FILTER;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.PATH_VARIABLE_ACCOUNT_ID;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.REQUEST_PAYLOAD;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SEARCH_MODE;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SEARCH_MODE_DEFAULT_FOR_FILTER_ACCOUNTS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SEARCH_MODE_DESCRIPTION;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SORT_BY_DEFAULT_FOR_FILTER_ACCOUNTS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SUMMARY_CREATE_ACCOUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SUMMARY_DELETE_ACCOUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SUMMARY_DELETE_ACCOUNT_DESC;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SUMMARY_GET_ACCOUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SUMMARY_GET_ACCOUNT_DESC;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.SUMMARY_UPDATE_ACCOUNT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID_UI;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.DESCENDING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.HTTP_OK;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.HTTP_SUCCESS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IGNORE_CASE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.IGNORE_CASE_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_BY_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SORT_ORDER_DESCRIPTION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STRING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUCCESS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MANAGE_ACCOUNTS_SCOPE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.VIEW_ACCOUNTS_SCOPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * This class represents the controller for managing accounts.
 */
@RestController
@RequestMapping(value = ACCOUNT_V1_VERSION
        + ACCOUNT_RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
@Validated
@Slf4j
@AllArgsConstructor
public class AccountController {

    private AccountService accountsService;

    /**
     * Account create request api to create a new account.
     *
     * @param userId The ID of the logged-in user.
     * @param accountDto The json containing the details of account to be created
     * @return Response with the created account id
     * @throws AccountManagementException if any other account error happens
     */
    @Operation(summary = SUMMARY_CREATE_ACCOUNT, description = "Creates a new Account.", tags = {
        ACCOUNT_TAG }, responses = {
            @ApiResponse(responseCode = HTTP_SUCCESS, description = SUCCESS, content =
              @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema =
              @Schema(implementation = CreateAccountResponse.class))) })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { MANAGE_ACCOUNTS_SCOPE })
    @Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI, schema = @Schema(type = STRING),
        in = ParameterIn.HEADER)
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(
            @RequestHeader(value = LOGGED_IN_USER_ID, required = true) String userId,
            @Valid @RequestBody @Parameter(name = "Request payload",
               description = "Parameters that define a single account") CreateAccountDto accountDto)
            throws NoSuchAlgorithmException, AccountManagementException {
        log.info("Create Account initiated for account {}", accountDto.getAccountName());
        return new ResponseEntity<>(accountsService.createAccount(accountDto, new BigInteger(userId)),
                HttpStatus.CREATED);
    }

    /**
     * Account delete request api to delete an account.
     *
     * @param userId The ID of the logged-in user.
     * @param accountId The account id to be deleted
     * @return Response with http status
     * @throws AccountManagementException if any other account error happens
     */
    @SuppressWarnings("rawtypes")
    @Operation(summary = SUMMARY_DELETE_ACCOUNT, description = SUMMARY_DELETE_ACCOUNT_DESC, tags = {
        ACCOUNT_TAG }, responses = { @ApiResponse(responseCode = HTTP_OK, description = SUCCESS) })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { MANAGE_ACCOUNTS_SCOPE })
    @Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI, schema = @Schema(type = STRING),
        in = ParameterIn.HEADER)
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @DeleteMapping(value = PATH_VARIABLE_ACCOUNT_ID)
    public ResponseEntity deleteAccount(
            @RequestHeader(value = LOGGED_IN_USER_ID, required = true) BigInteger userId,
            @Valid @PathVariable(value = ACCOUNT_ID) BigInteger accountId)
                 throws AccountManagementException {
        log.info("Delete Account initiated for id {}.", accountId);
        accountsService.deleteAccount(userId, accountId);
        return new ResponseEntity(HttpStatus.OK);

    }

    /**
     * Account Get request api to get an account information.
     *
     * @param accountId The account id of the account.
     * @return Response with requested account details.
     * @throws AccountManagementException If the logged in user is not found in db.
     */
    @Operation(summary = SUMMARY_GET_ACCOUNT, description = SUMMARY_GET_ACCOUNT_DESC, tags = {
        ACCOUNT_TAG }, responses = {
            @ApiResponse(responseCode = HTTP_OK, description = SUCCESS, content =
                @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema =
                  @Schema(implementation = GetAccountApiResponse.class))) })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { VIEW_ACCOUNTS_SCOPE,
        MANAGE_ACCOUNTS_SCOPE })
    @Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI, schema = @Schema(type = STRING),
        in = ParameterIn.HEADER)
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @GetMapping(value = PATH_VARIABLE_ACCOUNT_ID)
    public ResponseEntity<GetAccountApiResponse> getAccount(@Valid @PathVariable(value = ACCOUNT_ID)
            @Parameter(description = "Account ID", required = true) BigInteger accountId)
            throws AccountManagementException {
        log.info("Get Account Resource Started for account id {}", accountId);
        return new ResponseEntity<>(accountsService.getAccount(accountId), HttpStatus.OK);
    }

    /**
     * Account filter api to search and retrieve list of accounts based on account id,
     *      parentId, roles or status.
     *
     * @param userId The ID of the logged-in user.
     * @param searchMode EQUAL/PREFIX/SUFFIX/CONTAINS
     * @param sortOrder ASC / DESC
     * @param ignoreCase True or False
     * @param sortBy Json request body containing zero or more of account id, parent id, roles, status
     * @return Response with the list of accounts matching the criteria
     */
    @Operation(summary = LIST_ACCOUNTS_BY_FILTER,
        tags = { ACCOUNT_TAG },
        description = "Retrieves Accounts that match items in the defined list of parameters and values.",
        responses = {
            @ApiResponse(responseCode = HTTP_OK, description = SUCCESS,
               content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = GetAccountApiResponse.class))))
        })
    @SecurityRequirement(name = "JwtAuthValidator",
        scopes = { VIEW_ACCOUNTS_SCOPE, MANAGE_ACCOUNTS_SCOPE })
    @Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI, schema = @Schema(type = STRING),
        in = ParameterIn.HEADER)
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PostMapping(value = ACCOUNT_FILTER_PATH)
    public ResponseEntity<FilterAccountsApiResponse> filterAccounts(
        @RequestParam(name = SORT_BY, required = false, defaultValue = SORT_BY_DEFAULT_FOR_FILTER_ACCOUNTS)
            @Parameter(description = SORT_BY_DESCRIPTION) AccountFilterDto.AccountFilterDtoEnum sortBy,
        @RequestParam(name = SORT_ORDER, required = false, defaultValue = DESCENDING)
            @Parameter(description = SORT_ORDER_DESCRIPTION,
            schema = @Schema(allowableValues = {DESCENDING, ASCENDING})) SortOrder sortOrder,
        @RequestParam(name = IGNORE_CASE, required = false, defaultValue = IGNORE_CASE_DEFAULT_FOR_FILTER_ACCOUNTS)
            @Parameter(description = IGNORE_CASE_DESCRIPTION, schema =
            @Schema(allowableValues = {"true", "false"})) boolean ignoreCase,
        @RequestParam(name = SEARCH_MODE, required = false, defaultValue = SEARCH_MODE_DEFAULT_FOR_FILTER_ACCOUNTS)
            @Parameter(description = SEARCH_MODE_DESCRIPTION, schema =
            @Schema(allowableValues = {"PREFIX", "SUFFIX", "CONTAINS", "EQUAL"})) SearchType searchMode,
        @Valid @RequestBody @Parameter(name = "Request payload",
            description = "Parameters and values by which to filter. Leave empty to get all active accounts.")
            AccountFilterDto accountFilterDto,
        @RequestHeader(value = LOGGED_IN_USER_ID, required = true) String userId
    ) {
        return new ResponseEntity<>(accountsService.filterAccounts(accountFilterDto,
            sortBy.getField(), sortOrder.sortOrderLowerCase(),
            ignoreCase, searchMode), HttpStatus.OK);
    }

    /**
     * Account update request to update account roles, parentId or status for a given accountid.
     *
     * @param userId The ID of the logged-in user.
     * @param id The id of the account to be updated
     * @param accountDto The fields to be updated
     * @return http status code
     * @throws AccountManagementException If the account update failed.
     */
    @SuppressWarnings("rawtypes")
    @Operation(summary = SUMMARY_UPDATE_ACCOUNT, description = SUMMARY_UPDATE_ACCOUNT,
        tags = {ACCOUNT_TAG }, responses = {
          @ApiResponse(responseCode = HTTP_OK, description = SUCCESS) })
    @SecurityRequirement(name = "JwtAuthValidator",
             scopes = { MANAGE_ACCOUNTS_SCOPE })
    @Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI,
             schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
             schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
        schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @PostMapping(value = PATH_VARIABLE_ACCOUNT_ID, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateAccount(
            @RequestHeader(value = LOGGED_IN_USER_ID, required = true) String userId,
            @PathVariable(value = ACCOUNT_ID) @Parameter(description = "Account id", required = true) BigInteger id,
            @Valid @RequestBody @Parameter(name = REQUEST_PAYLOAD,
            description = "Parameters that updates an account") UpdateAccountDto accountDto)
            throws AccountManagementException {
        log.info("Update Account initiated for account id {}", id);
        accountsService.updateAccount(id, accountDto, new BigInteger(userId));
        return new ResponseEntity(HttpStatus.OK);
    }

}
