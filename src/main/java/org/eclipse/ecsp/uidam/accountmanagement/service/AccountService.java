/********************************************************************************

 * Copyright © 2023-24 Harman International

 * Licensed under the Apache License, Version 2.0 (the "License");

 * you may not use this file except in compliance with the License.

 * You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0
      
 * Unless required by applicable law or agreed to in writing, software

 * distributed under the License is distributed on an "AS IS" BASIS,

 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

 * See the License for the specific language governing permissions and

 * limitations under the License.

 * SPDX-License-Identifier: Apache-2.0

 ********************************************************************************/

package org.eclipse.ecsp.uidam.accountmanagement.service;

import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.AccountFilterDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.CreateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.UpdateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.FilterAccountsApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.GetAccountApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.exception.AccountManagementException;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * The AccountService interface provides methods for managing user accounts.
 */
public interface AccountService {

    /**
     * Adds a new account with the given account details and user ID.
     *
     * @param accountDto The account details.
     * @param userId     The ID of the loggedin user.
     * @return The response containing account id.
     * @throws NoSuchAlgorithmException  If an error occurs while generating a
     *                                   unique identifier for the account.
     * @throws AccountManagementException If any other error occurs during creation.
     */
    CreateAccountResponse createAccount(CreateAccountDto accountDto, BigInteger userId)
            throws NoSuchAlgorithmException, AccountManagementException;

    /**
     * Retrieves the account details for the specified account ID.
     *
     * @param accountId The ID of the account.
     * @return The response containing the account details.
     * @throws AccountManagementException If the account with the given ID is not
     *                                   found.
     */
    GetAccountApiResponse getAccount(BigInteger accountId) throws AccountManagementException;

    /**
     * Deletes the account with the specified account ID.
     *
     * @param accountId The ID of the account.
     * @param loggedInUserId The ID of the user.
     * @throws AccountManagementException AccountManagement exception
     */

    void deleteAccount(BigInteger loggedInUserId, BigInteger accountId) throws AccountManagementException;

    /**
     * Searches/filters the accounts specified by the search criteria.
     *
     * @param accountFilterDto The account filter details
     * @param sortBy           to sort the accounts list based on the field
     * @param sortOrder        to sort the records in desc or ascending order
     * @param ignoreCase       to filter the records by case check
     * @param searchType       to filter the records based on type ie prefix, suffix, contains and equal
     * @return List of accounts matching the search criteria
     */
    FilterAccountsApiResponse filterAccounts(AccountFilterDto accountFilterDto, String sortBy,
                String sortOrder, boolean ignoreCase, SearchType searchType);


    /**
     * Updates the account with the specified account id.
     *
     * @param accountId  The ID of the account.
     * @param accountDto The account details to be updated in database.
     * @param userId     The ID of the user.
     * @throws AccountManagementException Internal errors during account update.
     */
    void updateAccount(BigInteger accountId, UpdateAccountDto accountDto, BigInteger userId)
            throws AccountManagementException;
}
