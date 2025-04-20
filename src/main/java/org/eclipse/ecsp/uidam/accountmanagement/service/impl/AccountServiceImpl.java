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

package org.eclipse.ecsp.uidam.accountmanagement.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.AccountFilterDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.CreateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.UpdateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.FilterAccountsApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.GetAccountApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.exception.AccountManagementException;
import org.eclipse.ecsp.uidam.accountmanagement.mapper.AccountMapper;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.accountmanagement.service.AccountService;
import org.eclipse.ecsp.uidam.accountmanagement.utilities.AccountSearchSpecification;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.exception.handler.ErrorProperty;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAccountRoleMappingRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.AccountFilterDto.AccountFilterDtoEnum.ACCOUNT_NAMES;
import static org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.AccountFilterDto.AccountFilterDtoEnum.IDS;
import static org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.AccountFilterDto.AccountFilterDtoEnum.PARENTIDS;
import static org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.AccountFilterDto.AccountFilterDtoEnum.STATUS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ALREADY_EXISTS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ALREADY_EXISTS_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_DOES_NOT_EXIST;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_DOES_NOT_EXIST_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ID;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_NAME;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_REGEX;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ROLE_VALIDATION_FAILURE;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ROLE_VALIDATION_FAILURE_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_DELETE_ASSOCIATED_ACCOUNT_CODE;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_DELETE_ASSOCIATED_ACCOUNT_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_DELETE_DEFAULT_ACCOUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_DELETE_DEFAULT_ACCOUNT_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_UPDATE_DEFAULT_ACCOUNT;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.CANNOT_UPDATE_DEFAULT_ACCOUNT_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_ACCOUNT_STATUS;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_ACCOUNT_STATUS_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ACCOUNT_NAME_PATTERN;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ACCOUNT_NAME_PATTERN_MSG;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ROLE;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.INVALID_INPUT_ROLE_MSG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UNKNOWN_DB_ERROR;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UNKNOWN_DB_ERROR_MSG;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * This class implements the AccountService interface and provides the
 * implementation for managing account apis.
 */
@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;
    private RolesService rolesService;
    private BigInteger userDefaultAccountId;

    private ApplicationProperties applicationProperties;

    private UserAccountRoleMappingRepository userAccountRoleMappingRepository;

    /**
     * AccountServiceImpl constructor with values from application.properties.
     *
     * @param accountRepository     accountRepository
     * @param rolesService          rolesService
     * @param applicationProperties applicationProperties
     */
    public AccountServiceImpl(AccountRepository accountRepository, RolesService rolesService,
            ApplicationProperties applicationProperties,
            UserAccountRoleMappingRepository userAccountRoleMappingRepository) {
        this.accountRepository = accountRepository;
        this.rolesService = rolesService;
        this.applicationProperties = applicationProperties;
        this.userAccountRoleMappingRepository = userAccountRoleMappingRepository;
    }

    /**
     * AccountServiceImpl post constructor to set default accountId.
     */
    @PostConstruct
    public void postConstruct() {
        Optional<AccountEntity> defaultAccount = accountRepository
                .findByAccountName(applicationProperties.getUserDefaultAccountName());
        if (defaultAccount.isPresent()) {
            AccountEntity accountEntity = defaultAccount.get();
            applicationProperties.setUserDefaultAccountId(accountEntity.getId());
            this.userDefaultAccountId = accountEntity.getId();
        }
    }

    /**
     * Creates an account based on the provided accountDto and loginUserId.
     *
     * @param accountDto     The account data transfer object.
     * @param loggedInUserId The ID of the logged-in user.
     * @return The response containing the created account details.
     * @throws AccountManagementException If an error occurs during account
     *                                    creation.
     */
    @Override
    public CreateAccountResponse createAccount(final CreateAccountDto accountDto, final BigInteger loggedInUserId)
            throws AccountManagementException {
        // 1. validate account pattern
        String accountName = accountDto.getAccountName();
        validateAccount(accountName);
        Set<String> accountRolesSet = accountDto.getRoles();
        Map<String, BigInteger> roleNameToId = null;
        if (!CollectionUtils.isEmpty(accountRolesSet)) {
            roleNameToId = validateRolesAndGetFromAuthManagementByName(accountRolesSet);
        }
        Optional<AccountEntity> foundAccount = accountRepository.findByAccountName(accountDto.getAccountName());
        if (foundAccount.isPresent()) {
            throwException(ACCOUNT_ALREADY_EXISTS_MSG, ACCOUNT_ALREADY_EXISTS, ACCOUNT_NAME, accountName, BAD_REQUEST);
        }
        AccountEntity accountEntity = AccountMapper.ACCOUNT_MAPPER.mapToAccount(accountDto, roleNameToId);
        accountEntity.setCreatedBy(String.valueOf(loggedInUserId));
        accountEntity.setStatus(AccountStatus.ACTIVE);
        AccountEntity savedAccount = accountRepository.save(accountEntity);
        CreateAccountResponse response = AccountMapper.ACCOUNT_MAPPER.mapToCreateAccountResponse(savedAccount);
        log.info("Account creation successful for account {} with Account ID {} completed by {}.", accountName,
                response.getId(), loggedInUserId);
        return response;
    }

    /**
     * Validates the account roles with the auth management service.
     *
     * @param accountRolesNameSet Set of role names to be validated
     * @return                    Map of role names to role ids
     * @throws AccountManagementException AccountManagementException
     */
    protected Map<String, BigInteger> validateRolesAndGetFromAuthManagementByName(Set<String> accountRolesNameSet)
            throws AccountManagementException {
        try {
            RoleListRepresentation accountRoleListDto = rolesService.filterRoles(accountRolesNameSet,
                    Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT), Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT),
                    false);
            // 2.Validate roles exist in the database
            Map<String, BigInteger> filteredRoles = Optional.ofNullable(accountRoleListDto.getRoles())
                    .orElse(Collections.emptySet()).stream().filter(ele -> accountRolesNameSet.contains(ele.getName()))
                    .collect(Collectors.toMap(RoleCreateResponse::getName, RoleCreateResponse::getId));
            if (filteredRoles.size() != accountRolesNameSet.size()) {
                String errorMessage = formatErrorMessage(INVALID_INPUT_ROLE_MSG, accountRolesNameSet);
                log.error(errorMessage);
                throw new AccountManagementException(INVALID_INPUT_ROLE, errorMessage, null, BAD_REQUEST);
            }
            return filteredRoles;
        } catch (EntityNotFoundException e) {
            String errorMessage = formatErrorMessage(INVALID_INPUT_ROLE_MSG, accountRolesNameSet);
            log.error(errorMessage);
            throw new AccountManagementException(INVALID_INPUT_ROLE, errorMessage, null, BAD_REQUEST);
        }
    }

    /**
     * Validates the account roles with the auth management service.
     *
     * @param roleIds  Set of role ids to be validated
     * @return         map of role ids against role names
     * @throws AccountManagementException When role id list given does not match with valid role ids
     */
    protected Map<String, BigInteger> validateRolesAndGetFromAuthManagementById(Set<BigInteger> roleIds)
            throws AccountManagementException {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            RoleListRepresentation accountRoleListDto = rolesService.getRoleById(roleIds);
            // 2.Validate roles exist in the database
            Map<String, BigInteger> filteredRoles = Optional.ofNullable(accountRoleListDto.getRoles())
                    .orElse(Collections.emptySet()).stream().filter(ele -> roleIds.contains(ele.getId()))
                    .collect(Collectors.toMap(RoleCreateResponse::getName, RoleCreateResponse::getId));
            if (filteredRoles.size() != roleIds.size()) {
                String errorMessage = formatErrorMessage(INVALID_INPUT_ROLE_MSG, roleIds);
                log.error(errorMessage);
                throw new AccountManagementException(INVALID_INPUT_ROLE, errorMessage, null, BAD_REQUEST);
            }
            return filteredRoles;
        } catch (EntityNotFoundException e) {
            String errorMessage = formatErrorMessage(INVALID_INPUT_ROLE_MSG, roleIds);
            log.error(errorMessage);
            throw new AccountManagementException(INVALID_INPUT_ROLE, errorMessage, null, BAD_REQUEST);
        }

    }

    /**
     * Validate account name against valid regex.
     *
     * @param accountName The account name to be validated.
     * @throws AccountManagementException If the account name pattern is invalid.
     */
    protected void validateAccount(String accountName) throws AccountManagementException {
        if (!accountName.matches(ACCOUNT_REGEX)) {
            throwException(INVALID_INPUT_ACCOUNT_NAME_PATTERN_MSG, INVALID_INPUT_ACCOUNT_NAME_PATTERN, ACCOUNT_NAME,
                    accountName, BAD_REQUEST);
        }
    }

    /**
     * Retrieves the account details based on the provided accountId.
     *
     * @param accountId The ID of the account to retrieve.
     * @return The response containing the account details.
     * @throws AccountManagementException If the account is not found.
     */
    @Override
    public GetAccountApiResponse getAccount(BigInteger accountId) throws AccountManagementException {
        Optional<AccountEntity> accountDetails = accountRepository.findByIdAndStatusNot(accountId,
                AccountStatus.DELETED);
        if (accountDetails.isEmpty()) {
            throwException(ACCOUNT_DOES_NOT_EXIST_MSG, ACCOUNT_DOES_NOT_EXIST, ACCOUNT_ID, accountId.toString(),
                    NOT_FOUND);
        }

        AccountEntity accountEntity = accountDetails.get();
        Map<String, BigInteger> roleNameToId = null;
        try {
            roleNameToId = validateRolesAndGetFromAuthManagementById(accountEntity.getDefaultRoles());
        } catch (AccountManagementException e) {
            throwException(ACCOUNT_ROLE_VALIDATION_FAILURE_MSG, ACCOUNT_ROLE_VALIDATION_FAILURE, ACCOUNT_ID,
                    accountId.toString(), INTERNAL_SERVER_ERROR);
        }
        GetAccountApiResponse response = AccountMapper.ACCOUNT_MAPPER.mapToGetAccountApiResponse(accountDetails.get(),
                roleNameToId);
        log.info("GET Account for ID {} successful.", String.valueOf(accountId));
        return response;
    }

    /**
     * Deletes the account based on the provided accountId.
     *
     * @param accountId      The ID of the account to delete.
     * @param loggedInUserId The ID of the logged-in user.
     * @throws AccountManagementException Any other invalid action
     */
    @Override
    public void deleteAccount(BigInteger loggedInUserId, BigInteger accountId) throws AccountManagementException {
        // 1. Check if this is default account
        if (accountId.equals(userDefaultAccountId)) {
            throwException(CANNOT_DELETE_DEFAULT_ACCOUNT_MSG, CANNOT_DELETE_DEFAULT_ACCOUNT, ACCOUNT_ID,
                    String.valueOf(accountId), BAD_REQUEST);
        }

        // 2. Check if account exists
        Optional<AccountEntity> savedAccount = accountRepository.findByIdAndStatusNot(accountId, AccountStatus.DELETED);
        if (savedAccount.isEmpty()) {
            throwException(ACCOUNT_DOES_NOT_EXIST_MSG, ACCOUNT_DOES_NOT_EXIST, ACCOUNT_ID,
                    String.valueOf(accountId), NOT_FOUND);
        }

        // 3. Check if users are associated with the account.
        boolean isAccountAssociated = userAccountRoleMappingRepository.existsByAccountId(accountId);
        if (isAccountAssociated) {
            throwException(CANNOT_DELETE_ASSOCIATED_ACCOUNT_MSG, CANNOT_DELETE_ASSOCIATED_ACCOUNT_CODE, ACCOUNT_ID,
                    String.valueOf(accountId), BAD_REQUEST);
        }

        AccountEntity account = savedAccount.get();
        account.setStatus(AccountStatus.DELETED);
        account.setUpdatedBy(String.valueOf(loggedInUserId));
        account.setUpdateDate(new Timestamp(System.currentTimeMillis()));
        try {
            accountRepository.save(account);
        } catch (Exception e) {
            throwException(UNKNOWN_DB_ERROR_MSG, UNKNOWN_DB_ERROR, ACCOUNT_ID,
                    String.valueOf(accountId), INTERNAL_SERVER_ERROR);
            log.error("Could not delete account : {}", accountId, e);
        }
        log.info("Account deletion for Account ID {} successfully completed by {}.", accountId, loggedInUserId);
    }

    /**
     * Searches/Filters accounts based on the provided search criteria.
     *
     * @param accountFilterDto json containing the search fields
     * @param sortBy           which search field to be used for sorting
     * @param sortOrder        Asc / Desc
     * @param ignoreCase       true / false
     * @param searchMode       Equal / prefix / suffix / contains
     * @return The response containing the list of all accounts matching the
     *         criteria
     */
    @Override
    public FilterAccountsApiResponse filterAccounts(AccountFilterDto accountFilterDto, String sortBy, String sortOrder,
            boolean ignoreCase, SearchType searchMode) {
        validateAccountNameForUnderscore(accountFilterDto);
        Specification<AccountEntity> specification = createFilterQuery(accountFilterDto, ignoreCase, searchMode);
        Sort sortByField = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        List<AccountEntity> accountEntities = accountRepository.findAll(specification, sortByField);
        FilterAccountsApiResponse filterResponse = new FilterAccountsApiResponse();
        if (accountEntities.isEmpty()) {
            filterResponse.setItems(Collections.emptyList());
            return filterResponse;
        }
        accountEntities = filterEntityByRoleId(accountFilterDto, accountEntities);
        Set<BigInteger> roleIds = accountEntities.stream().filter(entity -> entity.getDefaultRoles() != null)
                .map(AccountEntity::getDefaultRoles).flatMap(Set::stream).collect(Collectors.toSet());
        Map<String, BigInteger> roleNameToId = validateRolesAndGetFromAuthManagementById(roleIds);
        filterResponse.setItems(accountEntities.stream()
                .map(entity -> AccountMapper.ACCOUNT_MAPPER.mapToGetAccountApiResponse(entity, roleNameToId)).toList());
        return filterResponse;
    }

    /**
     * Validates the account name for the underscore to treat it as literal
     * character instead of special character.
     *
     * @param accountFilterDto request body from the filter API
     */
    private void validateAccountNameForUnderscore(AccountFilterDto accountFilterDto) {
        if (!CollectionUtils.isEmpty(accountFilterDto.getAccountNames())) {
            Set<String> accountNamesSet = new HashSet<>();
            accountFilterDto.getAccountNames().forEach(accountName -> {
                if (accountName.matches(ACCOUNT_REGEX)) {
                    accountNamesSet.add(accountName.replace("_", "\\_"));
                }
            });
            accountFilterDto.setAccountNames(accountNamesSet);
        }
    }

    /**
     * Searches accounts by given list of roleids if any.
     *
     * @param accountFilterDto Source Filter data.
     * @param accountEntities  list of account matching rest of the criteria than
     *                         roleids.
     * @return the filtered entities matching the roleids.
     */
    private List<AccountEntity> filterEntityByRoleId(AccountFilterDto accountFilterDto,
            List<AccountEntity> accountEntities) {
        if (accountFilterDto.getRoles() != null) {
            // Get roles based on the account filter condition.
            RoleListRepresentation roleListDto = null;
            try {
                roleListDto = rolesService.filterRoles(accountFilterDto.getRoles(),
                        Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT),
                        Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT), false);
            } catch (EntityNotFoundException e) {
                log.error("No role records for the input roles {} with exception", accountFilterDto.getRoles(), e);
                return Collections.emptyList();
            }
            final Set<BigInteger> roleIdsToSearch = Optional.ofNullable(roleListDto)
                    .map(RoleListRepresentation::getRoles).orElse(Collections.emptySet()).stream()
                    .map(RoleCreateResponse::getId).collect(Collectors.toSet());

            accountEntities = accountEntities.stream().filter(entity -> entity.getDefaultRoles() != null
                    && !Collections.disjoint(roleIdsToSearch, entity.getDefaultRoles())).toList();
        }
        return accountEntities;
    }

    /**
     * Creates the search specification for account entity.
     *
     * @param accountFilterDto json containing the search fields
     * @param ignoreCase       true / false
     * @param searchMode       Equal / prefix / suffix / contains
     * @return The specification generated using the accountfilterdto fields
     *         connected AND
     */
    private Specification<AccountEntity> createFilterQuery(AccountFilterDto accountFilterDto, Boolean ignoreCase,
            SearchType searchMode) {
        Map<String, Set<?>> filterMap = createFilterMap(accountFilterDto);
        List<Specification<AccountEntity>> specifications = filterMap.entrySet().stream()
                .filter(entry -> !CollectionUtils.isEmpty(entry.getValue())).map(entry -> {
                    String field = entry.getKey();
                    Set<?> searchByList = entry.getValue();
                    SearchCriteria searchCriteria = new SearchCriteria(field, searchByList, searchMode, ignoreCase);
                    searchCriteria.setValue(searchByList);
                    return (Specification<AccountEntity>) new AccountSearchSpecification(searchCriteria);
                }).toList();
        return specifications.stream().reduce(Specification::and).orElse(null);
    }

    /**
     * Fills the map using account filter dto fields.
     *
     * @param accountFilterDto json containing the search fields
     * @return The Map containing account entity fields mapped to search criteria
     */
    private Map<String, Set<?>> createFilterMap(AccountFilterDto accountFilterDto) {
        Map<String, Set<?>> filterMap = new HashMap<>();
        filterMap.put(IDS.getField(), accountFilterDto.getIds());
        filterMap.put(ACCOUNT_NAMES.getField(), accountFilterDto.getAccountNames());
        filterMap.put(PARENTIDS.getField(), accountFilterDto.getParentIds());
        filterMap.put(STATUS.getField(),
                CollectionUtils.isEmpty(accountFilterDto.getStatus()) ? Set.of(AccountStatus.ACTIVE)
                        : accountFilterDto.getStatus());
        return filterMap;
    }

    /**
     * Updates account for the given account id.
     *
     * @param id             The id of the account to updated.
     * @param accountDto     The contents to ve updated
     * @param loggedInUserId The ID of the logged-in user.
     * @throws AccountManagementException If any other error occurs during db update
     */
    @Override
    public void updateAccount(BigInteger id, UpdateAccountDto accountDto, BigInteger loggedInUserId)
            throws AccountManagementException {
        String accIdAsString = String.valueOf(id);
        if (id.equals(userDefaultAccountId)) {
            throwException(CANNOT_UPDATE_DEFAULT_ACCOUNT_MSG, CANNOT_UPDATE_DEFAULT_ACCOUNT, ACCOUNT_ID, accIdAsString,
                    BAD_REQUEST);
        }
        if (accountDto.getStatus() == AccountStatus.DELETED) {
            throwException(INVALID_ACCOUNT_STATUS_MSG, INVALID_ACCOUNT_STATUS, ACCOUNT_ID, accIdAsString, BAD_REQUEST);
        }

        Optional<AccountEntity> accountEntity = accountRepository.findByIdAndStatusNot(id, AccountStatus.DELETED);
        if (accountEntity.isEmpty()) {
            throwException(ACCOUNT_DOES_NOT_EXIST_MSG, ACCOUNT_DOES_NOT_EXIST, ACCOUNT_ID, accIdAsString, BAD_REQUEST);
        }
        AccountEntity account = accountEntity.get();

        Map<String, BigInteger> roleNameToId = getRoleNameAndIdMapping(accountDto, accIdAsString, account);

        AccountMapper.ACCOUNT_MAPPER.updateAccountEntity(accountDto, account, roleNameToId);
        account.setUpdateDate(new Timestamp(System.currentTimeMillis()));
        account.setUpdatedBy(String.valueOf(loggedInUserId));
        accountRepository.save(account);
        log.info("Account update successful for ID {} by {}.", id, loggedInUserId);
    }

    private Map<String, BigInteger> getRoleNameAndIdMapping(UpdateAccountDto accountDto, String accIdAsString,
            AccountEntity account) {
        // Map RoleId from DB to Role Name.
        Map<String, BigInteger> roleNameToId = new HashMap<>();
        try {
            roleNameToId = new HashMap<>(validateRolesAndGetFromAuthManagementById(account.getDefaultRoles()));
        } catch (AccountManagementException e) {
            throwException(ACCOUNT_ROLE_VALIDATION_FAILURE_MSG, ACCOUNT_ROLE_VALIDATION_FAILURE, ACCOUNT_ID,
                    accIdAsString, INTERNAL_SERVER_ERROR);
        }

        // Get RoleId from Dto to map role name.
        Set<String> accountRolesSet = accountDto.getRoles();
        if (!CollectionUtils.isEmpty(accountRolesSet)) {
            try {
                Map<String, BigInteger> roleNameToIdByName = validateRolesAndGetFromAuthManagementByName(
                        accountRolesSet);
                roleNameToId.putAll(roleNameToIdByName);
            } catch (AccountManagementException e) {
                throwException(ACCOUNT_ROLE_VALIDATION_FAILURE_MSG, ACCOUNT_ROLE_VALIDATION_FAILURE, ACCOUNT_ID,
                        accIdAsString, INTERNAL_SERVER_ERROR);
            }
        }
        return roleNameToId;
    }

    private void throwException(String msgPattern, String msgKey, String propertyKey, String propertyValue,
            HttpStatus httpStatus) {
        String errorMessage = formatErrorMessage(msgPattern, propertyValue);
        log.error(errorMessage);

        ErrorProperty property = new ErrorProperty();
        property.setKey(propertyKey);
        property.setValues(Arrays.asList(propertyValue));
        throw new AccountManagementException(msgKey, errorMessage, Arrays.asList(property), httpStatus);
    }

    private String formatErrorMessage(String msgPattern, Object... arguments) {
        MessageFormat msgFormat = new MessageFormat(msgPattern);
        StringBuffer errorMessage = new StringBuffer();
        msgFormat.format(arguments, errorMessage, new FieldPosition(0));
        return errorMessage.toString();
    }
}
