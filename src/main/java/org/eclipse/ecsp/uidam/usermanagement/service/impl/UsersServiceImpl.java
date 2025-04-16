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

package org.eclipse.ecsp.uidam.usermanagement.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.common.utils.RoleManagementUtils;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.BaseResponseFromAuthorization;
import org.eclipse.ecsp.uidam.usermanagement.cache.CacheTokenService;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.dao.UserManagementDao;
import org.eclipse.ecsp.uidam.usermanagement.entity.CloudProfileEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAddressEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeValueEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEvents;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserRecoverySecret;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientStatus;
import org.eclipse.ecsp.uidam.usermanagement.enums.OperationPriority;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserEventStatus;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserEventType;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserRecoverySecretStatus;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ClientRegistrationException;
import org.eclipse.ecsp.uidam.usermanagement.exception.InActiveUserException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecoverySecretExpireException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserAccountRoleMappingException;
import org.eclipse.ecsp.uidam.usermanagement.exception.handler.ErrorProperty;
import org.eclipse.ecsp.uidam.usermanagement.mapper.UserMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.CloudProfilesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.EmailVerificationRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeValueRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserEventRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserRecoverySecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.AuthorizationServerClient;
import org.eclipse.ecsp.uidam.usermanagement.service.ClientRegistration;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailNotificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.AssociateAccountAndRolesDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.FederatedUserDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserChangeStatusRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoBase;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV1;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV2;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV2.UserAccountsAndRoles;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserEventsDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserMetaDataRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserUpdatePasswordDto;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersDeleteFilter;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV1;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterV2;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.AssociateAccountAndRolesResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.PasswordPolicyResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserDetailsResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserMetaDataResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV2;
import org.eclipse.ecsp.uidam.usermanagement.utilities.ObjectConverter;
import org.eclipse.ecsp.uidam.usermanagement.utilities.PasswordUtils;
import org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria;
import org.eclipse.ecsp.uidam.usermanagement.utilities.UserAccountRoleAssociationValidator;
import org.eclipse.ecsp.uidam.usermanagement.utilities.UserAttributeSpecification;
import org.eclipse.ecsp.uidam.usermanagement.utilities.UserManagementUtils;
import org.eclipse.ecsp.uidam.usermanagement.utilities.UserSpecification;
import org.eclipse.ecsp.uidam.usermanagement.validations.password.policy.CustomPasswordPatternPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.lang.Boolean.TRUE;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_DOES_NOT_EXIST;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCOUNT_AND_ROLE_ASSOCIATION_ERROR_MSG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCOUNT_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCOUNT_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCOUNT_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCOUNT_TYPE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ADD;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.AMPERSAND;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ASSOCIATION_REQ_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CAPTCHA_REQUIRED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CAPTCHA_REQUIRED_ATTRIBUTE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CloudProfileApiConstants.DELETED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.FIRSTNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_ACCOUNT_ID_AND_ROLES_ERROR_MESSGAE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_EXTERNAL_USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_INPUT_ROLE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_PAYLOAD_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LASTNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.MIN_CONSECUTIVE_LETTERS_LENGTH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.NO_ROLEID_FOR_FILTER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.OPERATION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ORIGINAL_USERNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PASSWORD;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PASS_REGEXP;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.REPLACE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ROLES_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ROLE_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SAVED_USER_ID_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SYSTEM;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UNDERSCORE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME2;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME_FOR_ERROR_MSG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME_REGEXP;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ACCOUNT_ROLE_ASSOCIATION_CODE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ACCOUNT_ROLE_MAPPING_TABLE_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ADDRESS_ENTITY_TABLE_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_DISASSOCIATE_ERROR_MSG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ENTITY_TABLE_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID_NOT_FOUND_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID_NOT_PRESENT_IN_THE_DB;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID_VARIABLE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.ACTION_FORBIDDEN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.ATTRIBUTE_METADATA_IS_MISSING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.FIELD_CANNOT_BE_MODIFIED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.FIELD_DATA_IS_INVALID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.FIELD_IS_UNIQUE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.FIELD_NOT_FOUND;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.IGNITE_SYSTEM_SCOPE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_API_VERSION;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_PASS_CANNOT_CONTAIN_USERNAME;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_USERNAME_CANNOT_START_WITH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.INVALID_INPUT_USERNAME_PATTERN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MANAGE_USERS_SCOPE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MISSING_MANDATORY_PARAMETERS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.USER_IS_BLOCKED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.USER_NOT_VERIFIED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.USER_ROLES_NOT_FOUND;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.ACCOUNTIDS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.ADDRESS1;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.ADDRESS2;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.CITIES;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.COUNTRIES;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.DEV_IDS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.EMAILS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.FIRST_NAMES;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.GENDER;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.IDS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.LAST_NAMES;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.LOCALES;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.PHONE_NUMBERS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.POSTAL_CODES;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.ROLEIDS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.STATES;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.STATUS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.TIMEZONE;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.USER_NAMES;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria.RootParam.USER_ACCOUNT_ROLE_ROOT;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria.RootParam.USER_ADDRESS_ROOT;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria.RootParam.USER_ROOT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

/**
 * Service class containing business logic for CRUD operations on user.
 */
@Service
@AllArgsConstructor
public class UsersServiceImpl implements UsersService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public CacheTokenService cacheTokenService;

    private static final Map<String, Class<?>> DATA_TYPE_MAP = Map.ofEntries(Map.entry("varchar", String.class),
        Map.entry("bool", Boolean.class), Map.entry("bit", Boolean.class), Map.entry("int8", Long.class),
        Map.entry("bigserial", Long.class), Map.entry("oid", Long.class), Map.entry("bytea", Byte.class),
        Map.entry("char", String.class), Map.entry("bpchar", String.class), Map.entry("numeric", BigDecimal.class),
        Map.entry("int4", Integer.class), Map.entry("serial", Integer.class), Map.entry("int2", Short.class),
        Map.entry("float4", Float.class), Map.entry("float8", Double.class), Map.entry("money", Double.class),
        Map.entry("name", String.class), Map.entry("text", String.class), Map.entry("date", Date.class),
        Map.entry("time", Time.class), Map.entry("timetz", Time.class), Map.entry("timestamp", Timestamp.class),
        Map.entry("_abc", List.class), Map.entry("uuid", UUID.class), Map.entry("json", String.class),
        Map.entry("jsonb", JsonNode.class));
    @Autowired
    private ApplicationProperties applicationProperties;
    private UsersRepository userRepository;
    private UserAttributeRepository userAttributeRepository;
    private UserAttributeValueRepository userAttributeValueRepository;
    private UserEventRepository userEventRepository;
    private UserRecoverySecretRepository userRecoverySecretRepository;
    private UserManagementDao userManagementDao;
    @Autowired
    private AuthorizationServerClient authorizationServerClient;
    @PersistenceContext
    private EntityManager entityManager;
    private EmailNotificationService emailNotificationService;
    @Autowired
    private RolesService rolesService;
    @Autowired
    private RolesRepository rolesRepository;
    @Setter(onMethod_ = { @Autowired })
    ClientRegistration clientRegistrationService;
    private CloudProfilesRepository cloudProfilesRepository;
    private EmailVerificationRepository emailVerificationRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersServiceImpl.class);

    @Autowired
    private AccountRepository accountRepository;

    //Adding these to support the switch case down the line
    private static final String VERSION_1 = "v1";
    private static final String VERSION_2 = "v2";

    // The below maps are added to avoid multiple db calls to get account and role
    // id using name
    private Map<String, BigInteger> accountNameToIdMapping = new HashMap<>();
    private Map<BigInteger, String> accountIdToNameMapping = new HashMap<>();
    private Map<BigInteger, String> roleIdToNameMapping = new HashMap<>();
    private Map<String, BigInteger> roleNameToIdMapping = new HashMap<>();

    /**
     * Method to create user profile in user management.
     *
     * @param userDto        input user details provided by user.
     * @param loggedInUserId user Id for the user creating a user profile.
     * @param isSelfAddUser boolean flag to identify if the call is made for self add user and avoid
     *                      user/client permission validation and further validateMissingMandatoryAttributes
     *                      as configured
     * @return userResponse.
     * @throws ResourceNotFoundException throws exception if user details not found
     *                                   for the logged-in user id.
     */
    @Transactional
    @Override
    public UserResponseBase addUser(UserDtoBase userDto, BigInteger loggedInUserId, boolean isSelfAddUser)
        throws ResourceNotFoundException {
        validateUserNameAndPassword(userDto.getUserName(), userDto.getPassword());

        if (!isSelfAddUser) {
            validateUserPermissions(userDto, loggedInUserId);
        }

        validateAccountAndRoles(userDto);
        UserEntity user = userRepository.findByUserNameIgnoreCaseAndStatusNot(userDto.getUserName(),
            UserStatus.DELETED);
        if (user != null) {
            throw new RecordAlreadyExistsException(ApiConstants.USER);
        }
        List<UserAttributeEntity> userAttributeEntities = new ArrayList<>();
        if (!isSelfAddUser || applicationProperties.isAdditionalAttrCheckEnabledForSignUp()) {
            userAttributeEntities = validateMissingMandatoryAttributes(userDto.getAdditionalAttributes());
        }
        userDto.setStatus(applicationProperties.getIsUserStatusLifeCycleEnabled().booleanValue() ? UserStatus.PENDING
            : UserStatus.ACTIVE);

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userDto);
        String passwordSalt = PasswordUtils.getSalt();
        String hashPassword = PasswordUtils.getSecurePassword(userDto.getPassword(), passwordSalt,
            applicationProperties.getPasswordEncoder());
        userEntity.setUserPassword(hashPassword);
        userEntity.setPasswordSalt(passwordSalt);
        userEntity.setAccountRoleMapping(mapToAccountsAndRoles(userDto, loggedInUserId));

        userEntity.getUserAddresses().forEach(userAddressEntity -> userAddressEntity.setUserEntity(userEntity));

        UserEntity savedUser = userRepository.save(userEntity);
        // UserAccountRoleMapping would not have got the new userId now.
        // Set it explicitly for each of them.
        BigInteger savedUserId = savedUser.getId();
        LOGGER.debug(SAVED_USER_ID_MESSAGE, savedUserId);
        savedUser.getAccountRoleMapping().forEach(urm -> {
            LOGGER.debug(ACCOUNT_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE, urm.getAccountId());
            LOGGER.debug(ROLE_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE, urm.getRoleId());
            urm.setUserId(savedUserId);
        });

        String version = (userDto instanceof UserDtoV1) ? VERSION_1 : VERSION_2;

        UserResponseBase userResponseBase = addRoleNamesAndMapToUserResponse(userEntity, version);

        if ((!isSelfAddUser || applicationProperties.isAdditionalAttrCheckEnabledForSignUp())
            && !ObjectUtils.isEmpty(userDto.getAdditionalAttributes())
            && isValidAdditionalAttributes(userDto.getAdditionalAttributes(), userAttributeEntities, true)) {
            userResponseBase
                .setAdditionalAttributes(persistAdditionalAttributes(userDto, savedUser).get(savedUser.getId()));
        }
        return userResponseBase;
    }

    /**
     * Method to validate the account names and roles.
     *
     * @param userDto The pojo containing user details
     */
    public void validateAccountAndRoles(UserDtoBase userDto) throws ApplicationRuntimeException {
        if (userDto instanceof UserDtoV1 userDtoV1) {
            validateRoles(userDtoV1.getRoles());
        } else { // validate account and roles
            UserDtoV2 userDtoV2 = (UserDtoV2) userDto;
            // Collect all account names and roles into distinct sets.
            Set<String> accountsSet = new HashSet<>();
            Set<String> rolesSet = new HashSet<>();
            userDtoV2.getAccounts().stream().forEach(account -> {
                accountsSet.add(account.getAccount());
                Set<String> subRoles = account.getRoles();
                subRoles.forEach(r -> rolesSet.add(r));
            });
            // Validate Accounts by checking if given number of accounts same as active
            // accounts in db,
            // searched by name and ACTIVE status
            List<Object[]> activeAccounts = accountRepository
                .findIdAndNameByStatusAndAccountNameIn(AccountStatus.ACTIVE, accountsSet);

            activeAccounts.stream().forEach(activeac -> {
                LOGGER.debug("Active account name: {}, id: {}", (String) activeac[1], (BigInteger) activeac[0]);
                accountNameToIdMapping.put((String) activeac[1], (BigInteger) activeac[0]);
                accountIdToNameMapping.put((BigInteger) activeac[0], (String) activeac[1]);
            });

            if (activeAccounts.size() != accountsSet.size()) {
                throw new ApplicationRuntimeException(ACCOUNT_DOES_NOT_EXIST, BAD_REQUEST);
            }
            validateRoles(rolesSet);
        }
    }

    /**
     * Validates if all the given roles exist in the database.
     *
     * @param roles Set of role names
     */
    private void validateRoles(Set<String> roles) {
        RoleListRepresentation roleListDto;
        try {
            roleListDto = rolesService.filterRoles(roles, Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT),
                Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT), false);
            if (!validateRoleExists(roleListDto, roles)) {
                throw new ApplicationRuntimeException(USER_ROLES_NOT_FOUND, BAD_REQUEST);
            }
        } catch (EntityNotFoundException e) {
            throw new ApplicationRuntimeException(USER_ROLES_NOT_FOUND, BAD_REQUEST);

        }
        roleIdToNameMapping = roleListDto.getRoles().stream()
            .collect(Collectors.toMap(RoleCreateResponse::getId, RoleCreateResponse::getName));
        roleNameToIdMapping = roleListDto.getRoles().stream()
            .collect(Collectors.toMap(RoleCreateResponse::getName, RoleCreateResponse::getId));
    }

    /**
     * map accounts in userDto to UserAccountRoleMappingEntity.
     *
     * @param userDto      userDto
     * @param loggedInUser loggedInUser
     * @return List list
     * @throws ResourceNotFoundException When default account does not exist for v1
     */
    private List<UserAccountRoleMappingEntity> mapToAccountsAndRoles(UserDtoBase userDto, BigInteger loggedInUser)
        throws ResourceNotFoundException {
        List<UserAccountRoleMappingEntity> uarMappinglist = new ArrayList<>();

        Set<UserAccountsAndRoles> accounts = new HashSet<>();
        if (userDto instanceof UserDtoV1 userDtoV1) {
            // Set default account
            UserAccountsAndRoles ac = new UserAccountsAndRoles();
            BigInteger defaultAccountId = applicationProperties.getUserDefaultAccountId();
            String defaultAcName = null;
            Optional<AccountEntity> defAccount = accountRepository.findById(defaultAccountId);
            if (defAccount.isPresent()) {
                defaultAcName = defAccount.get().getAccountName();
                ac.setAccount(defaultAcName);
                LOGGER.debug("mapToAccountsAndRoles: Roles given in userDtoV1: {}", userDtoV1.getRoles());
                ac.setRoles(userDtoV1.getRoles());
                accounts.add(ac);
                accountNameToIdMapping.put(defaultAcName, defaultAccountId);
                accountIdToNameMapping.put(defaultAccountId, defaultAcName);
            } else {
                throw new ResourceNotFoundException("Account", "DefaultAccount", defaultAccountId.toString());
            }
        } else {
            accounts = ((UserDtoV2) userDto).getAccounts();
        }

        for (UserAccountsAndRoles account : accounts) {
            Set<String> roles = account.getRoles();
            AccountEntity ac = new AccountEntity();
            ac.setAccountName(account.getAccount());
            BigInteger accountId = accountNameToIdMapping.get(account.getAccount());
            LOGGER.debug("accountId {} and roles {} to be added to mapping", accountId, roles);
            for (String r : roles) {
                UserAccountRoleMappingEntity urm = new UserAccountRoleMappingEntity();
                urm.setRoleId(roleNameToIdMapping.get(r));
                urm.setAccountId(accountId);
                urm.setCreatedBy(loggedInUser != null ? String.valueOf(loggedInUser) : SYSTEM);
                uarMappinglist.add(urm);
            }
        }
        return uarMappinglist;
    }

    /**
     * Method to validate input username and password.
     *
     * @param userName username of the user.
     * @param password password of the user.
     */
    private void validateUserNameAndPassword(String userName, String password) {
        final String invalidPrefix = "federated_";
        if (!userName.matches(USERNAME_REGEXP)) {
            LOGGER.error("Validate session, user name pattern is invalid: {}", userName);
            throw new ApplicationRuntimeException(INVALID_INPUT_USERNAME_PATTERN, BAD_REQUEST);
        }
        if (StringUtils.startsWithIgnoreCase(userName, invalidPrefix)) {
            throw new ApplicationRuntimeException(INVALID_INPUT_USERNAME_CANNOT_START_WITH, BAD_REQUEST, invalidPrefix);
        }
        if (StringUtils.containsIgnoreCase(password, userName)) {
            throw new ApplicationRuntimeException(INVALID_INPUT_PASS_CANNOT_CONTAIN_USERNAME, BAD_REQUEST, PASSWORD,
                USERNAME2);
        }
        LOGGER.debug("Password validation as per configured password policy started");
        CustomPasswordPatternPolicy customPasswordPatternPolicy = new CustomPasswordPatternPolicy(
            applicationProperties);

        if (!customPasswordPatternPolicy.enforce(password, userName)) {
            throw new ApplicationRuntimeException(customPasswordPatternPolicy.getErrorMessage(), BAD_REQUEST,
                ApiConstants.PASSWORD);
        }
    }

    /**
     * Method to save user additional attributes to database.
     *
     * @param userDto   userRequestDto received from user api.
     * @param savedUser userEntity persisted in db.
     * @return Saved additional attribute data map.
     */
    public Map<BigInteger, Map<String, Object>> persistAdditionalAttributes(UserDtoBase userDto, UserEntity savedUser) {
        List<UserAttributeEntity> userAttributeEntities = userAttributeRepository.findAll();
        Map<String, UserAttributeEntity> userAttributeEntityByNameMap = groupUserAttributeEntityByName(
            userAttributeEntities);
        List<UserAttributeValueEntity> userAttributeValueEntities = userDto.getAdditionalAttributes().keySet().stream()
            .map(additionalAttribute -> {
                UserAttributeValueEntity userAttributeEntity = new UserAttributeValueEntity();
                userAttributeEntity.setCreatedBy("system");
                userAttributeEntity.setUserId(savedUser.getId());
                userAttributeEntity.setAttributeId(
                    userAttributeEntityByNameMap.get(additionalAttribute.toLowerCase(Locale.ROOT)).getId());
                Object value = userDto.getAdditionalAttributes().get(additionalAttribute);
                userAttributeEntity.setValue(
                    parseAdditionalAttributeValue(userAttributeEntityByNameMap, additionalAttribute, value));
                return userAttributeEntity;
            }).toList();
        List<UserAttributeValueEntity> savedAttributeValueEntities = userAttributeValueRepository
            .saveAll(userAttributeValueEntities);
        return mapAttributeNameToValue(userAttributeEntities, savedAttributeValueEntities);
    }

    /**
     * Validate and parse value of Object data type to String for each user
     * attribute.
     *
     * @param userAttributeEntityByNameMap Attribute map with attribute metadata as
     *                                     value and key as attribute name.
     * @param additionalAttribute          attribute name.
     * @param value                        attribute value
     * @return String equivalent value for each attribute value.
     */
    public String parseAdditionalAttributeValue(Map<String, UserAttributeEntity> userAttributeEntityByNameMap,
                                                String additionalAttribute, Object value) {
        if (DATA_TYPE_MAP.get(userAttributeEntityByNameMap.get(additionalAttribute.toLowerCase(Locale.ROOT)).getTypes()
            .toLowerCase(Locale.ROOT)).equals(JsonNode.class)) {
            return ObjectConverter.jsonNodeObjectToString(value);
        } else if (DATA_TYPE_MAP.get(userAttributeEntityByNameMap.get(additionalAttribute.toLowerCase(Locale.ROOT))
            .getTypes().toLowerCase(Locale.ROOT)).equals(List.class)) {
            return String.join(",", new ArrayList<>((List) value));
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Method to map saved(i.e., into database) attribute values to attribute name
     * for each user.
     *
     * @param userAttributeEntities       Entity containing additional attribute
     *                                    metadata.
     * @param savedAttributeValueEntities List of attribute value entity.
     * @return map of userId with attribute name and value map as value.
     */
    public Map<BigInteger, Map<String, Object>> mapAttributeNameToValue(List<UserAttributeEntity> userAttributeEntities,
                                                                        List<UserAttributeValueEntity>
                                                                            savedAttributeValueEntities) {
        Map<BigInteger, UserAttributeEntity> userAttributeByIdMap = groupUserAttributeById(userAttributeEntities);
        return savedAttributeValueEntities.stream()
            .collect(Collectors.groupingBy(UserAttributeValueEntity::getUserId,
                Collectors.toMap(
                    userAttributeValueEntity -> userAttributeByIdMap
                        .get(userAttributeValueEntity.getAttributeId()).getName(),
                    userAttributeValueEntity -> parseAttributeValueToCorrectDataType(
                        userAttributeByIdMap.get(userAttributeValueEntity.getAttributeId()),
                        userAttributeValueEntity.getValue()))));
    }

    /**
     * Method to parse additional attribute value from string to defined data type
     * in userAttributeEntity attribute metadata.
     *
     * @param userAttributeEntity object containing attribute metadata.
     * @param value               value of attribute in string.
     * @return value of correct datatype as defined in attribute metadata.
     */
    public Object parseAttributeValueToCorrectDataType(UserAttributeEntity userAttributeEntity, String value) {
        if (userAttributeEntity.getTypes().equalsIgnoreCase("bit")) {
            return Integer.valueOf(value);
        }
        return ObjectConverter.convert(value, DATA_TYPE_MAP.get(userAttributeEntity.getTypes()));
    }

    /**
     * This method validates whether the additional attribute value confirms with
     * the additional attribute validations.
     *
     * @param additionalAttributes  additional attribute map with key as attribute
     *                              name and value as attribute value.
     * @param userAttributeEntities List of attribute metadata.
     * @param newUser               boolean value to find if user already exists or
     *                              if it is a new user.
     * @return return true of attribute is valid/ false if attribute is invalid.
     */
    public boolean isValidAdditionalAttributes(Map<String, Object> additionalAttributes,
                                               List<UserAttributeEntity> userAttributeEntities, boolean newUser) {
        if (newUser) {
            Set<String> badDtoAttributes = findBadDtoAttributes(userAttributeEntities, additionalAttributes.keySet());
            if (!ObjectUtils.isEmpty(badDtoAttributes)) {
                throw new ApplicationRuntimeException(FIELD_NOT_FOUND, BAD_REQUEST, String.valueOf(badDtoAttributes));
            }
        }
        Set<String> invalidAttributeValues = invalidAttributeValue(userAttributeEntities, additionalAttributes);
        if (!ObjectUtils.isEmpty(invalidAttributeValues)) {
            throw new ApplicationRuntimeException(FIELD_DATA_IS_INVALID, BAD_REQUEST,
                String.valueOf(invalidAttributeValues));
        }
        Set<String> duplicateAttributes = findDuplicateValueAttributes(userAttributeEntities, additionalAttributes);
        if (!ObjectUtils.isEmpty(duplicateAttributes)) {
            throw new ApplicationRuntimeException(FIELD_IS_UNIQUE, BAD_REQUEST, String.valueOf(duplicateAttributes));
        }
        return true;
    }

    /**
     * Method to find attribute value is duplicate.
     *
     * @param userAttributeEntities List of attribute metadata.
     * @param additionalAttributes  additional attribute map with key as attribute
     *                              name and value as attribute value.
     * @return set of attributes of which unique constraint failed.
     */
    public Set<String> findDuplicateValueAttributes(List<UserAttributeEntity> userAttributeEntities,
                                                    Map<String, Object> additionalAttributes) {
        List<UserAttributeEntity> uniqueAttributes = userAttributeEntities.stream()
            .filter(UserAttributeEntity::getIsUnique).toList();
        Map<String, UserAttributeEntity> userAttributeEntityByNameMap = groupUserAttributeEntityByName(
            uniqueAttributes);
        Map<String, Set<String>> uniqueAdditionalAttribute = additionalAttributes.entrySet().stream()
            .filter(entry -> userAttributeEntityByNameMap.containsKey(entry.getKey().toLowerCase(Locale.ROOT)))
            .collect(Collectors.toMap(Map.Entry::getKey,
                additionalAttribute -> Collections
                    .singleton(parseAdditionalAttributeValue(userAttributeEntityByNameMap,
                        additionalAttribute.getKey(), additionalAttribute.getValue()))));

        Specification<UserAttributeValueEntity> specification = createUserAttributeValueSpec(uniqueAttributes, false,
            null, uniqueAdditionalAttribute);
        List<UserAttributeValueEntity> userAttributeValueEntities = userAttributeValueRepository.findAll(specification);
        if (!ObjectUtils.isEmpty(userAttributeValueEntities)) {
            return uniqueAdditionalAttribute.keySet();
        }
        return Collections.emptySet();
    }

    /**
     * Method to collect all the attributes for which user provided invalid values.
     *
     * @param userAttributeEntities List of attributes schema.
     * @param additionalAttributes  map of attribute names as key and user provided
     *                              attribute values as value.
     * @return set of attribute names for which user provided incorrect values.
     */
    private Set<String> invalidAttributeValue(List<UserAttributeEntity> userAttributeEntities,
                                              Map<String, Object> additionalAttributes) {
        Map<String, UserAttributeEntity> userAttributeEntityMap = groupUserAttributeEntityByName(userAttributeEntities);
        return additionalAttributes.keySet().stream().filter(attribute -> {
            UserAttributeEntity userAttributeEntity = userAttributeEntityMap.get(attribute.toLowerCase(Locale.ROOT));
            String dataType = userAttributeEntity.getTypes().toLowerCase(Locale.ROOT);
            Class<?> entityDataTypeClass = DATA_TYPE_MAP.get(dataType);
            Object value = additionalAttributes.get(attribute);
            if (entityDataTypeClass.isInstance(value) && value instanceof String regex) {
                return !regex.matches(userAttributeEntity.getRegex());
            } else {
                return !Boolean.TRUE.equals(isObjectCastable(dataType, value));
            }
        }).collect(Collectors.toSet());
    }

    /**
     * Method to validate if object can be converted to provided data type.
     *
     * @param dataType String dataType present in java.
     * @param value    Object type value.
     * @return true if object can be converted to the provided data type else false.
     */
    private Boolean isObjectCastable(String dataType, Object value) {
        try {
            if (DATA_TYPE_MAP.get(dataType).equals(JsonNode.class)
                && ObjectConverter.jsonNodeObjectToString(value) != null) {
                return true;
            } else if (dataType.equalsIgnoreCase("bit")) {
                Boolean.valueOf(String.valueOf(value));
                return true;
            } else if (DATA_TYPE_MAP.get(dataType).equals(List.class)) {
                new ArrayList<>((List<?>) value);
                return true;
            } else {
                String data = String.valueOf(value);
                ObjectConverter.convert(data, DATA_TYPE_MAP.get(dataType));
                return true;
            }
        } catch (Exception exception) {
            LOGGER.error("Data casting failed for value: {} with exception {} ", value, exception.getMessage());
        }
        return false;
    }

    /**
     * Method to map attribute metadata with name as key.
     *
     * @param userAttributeEntities List attribute metadata.
     * @return map of attribute name as key and attribute metadata as value.
     */
    public Map<String, UserAttributeEntity> groupUserAttributeEntityByName(
        List<UserAttributeEntity> userAttributeEntities) {
        return userAttributeEntities.stream()
            .collect(Collectors.toMap(userAttributeEntity -> userAttributeEntity.getName().toLowerCase(Locale.ROOT),
                userAttributeEntity -> userAttributeEntity));
    }

    /**
     * Method to map attribute metadata with attributeId as key.
     *
     * @param userAttributeEntities List attribute metadata.
     * @return map of attribute id as key and attribute metadata as value.
     */
    public Map<BigInteger, UserAttributeEntity> groupUserAttributeById(
        List<UserAttributeEntity> userAttributeEntities) {
        return userAttributeEntities.stream()
            .collect(Collectors.toMap(UserAttributeEntity::getId, userAttributeEntity -> userAttributeEntity));
    }

    /**
     * Method to validate attributes provided as part of request dto.
     *
     * @param userAttributeEntities List attribute metadata.
     * @param receivedAttributes    Set of attribute names provided as part of
     *                              request.
     * @return return set of attribute names for those attributes having no metadata
     *         in db.
     */
    public Set<String> findBadDtoAttributes(List<UserAttributeEntity> userAttributeEntities,
                                            Set<String> receivedAttributes) {
        Set<String> savedAttributes = userAttributeEntities.stream().map(UserAttributeEntity::getName)
            .collect(Collectors.toSet());

        return receivedAttributes.stream().filter(receivedAttribute -> !savedAttributes.contains(receivedAttribute))
            .collect(Collectors.toSet());
    }

    /**
     * Method to validate if mandatory additional attributes are missing from
     * request.
     *
     * @param userAttributeEntities List attribute metadata.
     * @param receivedAttributes    Set of attribute names provided as part of
     *                              request.
     * @return return the names of mandatory attributes that must be included in the
     *         request.
     */
    public Set<String> findMissingDtoAttributes(List<UserAttributeEntity> userAttributeEntities,
                                                Set<String> receivedAttributes) {
        return userAttributeEntities.stream().filter(UserAttributeEntity::getMandatory)
            .filter(userAttributeEntity -> !receivedAttributes.contains(userAttributeEntity.getName()))
            .map(UserAttributeEntity::getName).collect(Collectors.toSet());
    }

    /**
     * Method to fetch user details with respect to given user id.
     *
     * @param userId userId for which user details needs to be fetched.
     * @return user details for the provided user id.
     * @throws ResourceNotFoundException if given user id is not present in system.
     */
    @Override
    public UserResponseBase getUser(BigInteger userId, String apiVersion) throws ResourceNotFoundException {
        UserEntity userEntity = userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED);
        if (Objects.isNull(userEntity)) {
            throw new ResourceNotFoundException(USER, USER_ID_VARIABLE, String.valueOf(userId));
        }
        UserResponseBase userResponse = addRoleNamesAndMapToUserResponse(userEntity, apiVersion);
        Map<BigInteger, Map<String, Object>> additionalAttributes = findAdditionalAttributeData(List.of(userId));
        if (!ObjectUtils.isEmpty(additionalAttributes)) {
            userResponse.setAdditionalAttributes(additionalAttributes.get(userId));
        }
        return userResponse;
    }

    /**
     * Method to fetch user details with respect to given username.
     *
     * @param userName userName for which user details needs to be fetched.
     * @return user details for the provided username.
     */
    @Override
    public UserDetailsResponse getUserByUserName(String userName)
        throws ResourceNotFoundException, InActiveUserException {
        UserEntity userEntity = userRepository.findByUserNameIgnoreCaseAndStatusNot(userName, UserStatus.DELETED);
        verifyUserStatus(userEntity, userName);
        Set<BigInteger> roleIds = userEntity.getAccountRoleMapping().stream()
            .map(UserAccountRoleMappingEntity::getRoleId).collect(Collectors.toSet());
        RoleListRepresentation roleListDto = rolesService.getRoleById(roleIds);
        UserDetailsResponse userDetailsResponse = UserMapper.USER_MAPPER.mapToUserDetailsResponse(userEntity);
        userDetailsResponse.setScopes(RoleManagementUtils.getScopesFromRoles(roleListDto));
        userDetailsResponse.setPasswordEncoder(applicationProperties.getPasswordEncoder());
        userDetailsResponse.setEmail(userEntity.getEmail());
        // todo: Data to be extracted from tenant management
        userDetailsResponse.setAccountId(applicationProperties.getAccountId());
        populateAdditionalAttributes(userDetailsResponse, userEntity);
        Map<String, Object> captcha = userDetailsResponse.getCaptcha();
        UserAttributeEntity userAttributeEntity = userAttributeRepository.findByName(CAPTCHA_REQUIRED_ATTRIBUTE);
        if (Objects.nonNull(userAttributeEntity)) {
            UserAttributeValueEntity userAttributeValueEntity = userAttributeValueRepository
                .findByUserIdAndAttributeId(userEntity.getId(), userAttributeEntity.getId());
            captcha.put(CAPTCHA_REQUIRED,
                Objects.nonNull(userAttributeValueEntity) ? Boolean.valueOf(userAttributeValueEntity.getValue())
                    : null);
        } else {
            captcha.put(CAPTCHA_REQUIRED, null);
        }
        captcha.put(CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES, applicationProperties.getCaptchaEnforceAfterNoOfFailures());
        int failedLoginAttempts = 0;
        int allowedLoginAttempts = Integer.parseInt(applicationProperties.getMaxAllowedLoginAttempts());
        List<UserEvents> userEventList = userEventRepository.findUserEventsByUserIdAndEventType(userEntity.getId(),
            UserEventType.LOGIN_ATTEMPT.getValue(), allowedLoginAttempts);
        for (UserEvents e : userEventList) {
            if (UserEventStatus.FAILURE.getValue().equals(e.getEventStatus())) {
                failedLoginAttempts++;
            } else {
                userDetailsResponse.setLastSuccessfulLoginTime(e.getEventGeneratedAt().toString());
                break;
            }
        }
        userDetailsResponse.setFailureLoginAttempts(failedLoginAttempts);
        return userDetailsResponse;
    }

    /**
     * Method to add additional details to userDetailsResponse from userEntity
     * object.
     *
     * @param userDetailsResponse user details to be returned as response.
     * @param userEntity          user details from database.
     */
    private void populateAdditionalAttributes(UserDetailsResponse userDetailsResponse, UserEntity userEntity) {
        HashMap<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(ACCOUNT_NAME, applicationProperties.getAccountName());
        additionalAttributes.put(ACCOUNT_TYPE, applicationProperties.getAccountType());
        additionalAttributes.put(ORIGINAL_USERNAME, userEntity.getUserName());
        additionalAttributes.put(FIRSTNAME, userEntity.getFirstName());
        additionalAttributes.put(LASTNAME, userEntity.getLastName());
        userDetailsResponse.setAdditionalAttributes(additionalAttributes);
    }

    /**
     * Method validate UserStatus.
     *
     * @param userEntity UserData fetched from database.
     * @param userName   userName of the user.
     * @throws ResourceNotFoundException exception to be thrown if user data not
     *                                   found.
     * @throws InActiveUserException     exception to be thrown if user status is
     *                                   not ACTIVE.
     */
    public void verifyUserStatus(UserEntity userEntity, String userName)
        throws ResourceNotFoundException, InActiveUserException {
        if (Objects.isNull(userEntity)) {
            throw new ResourceNotFoundException(USER, USERNAME_FOR_ERROR_MSG, userName);
        } else if (userEntity.getStatus().equals(UserStatus.PENDING)) {
            throw new InActiveUserException(USER_NOT_VERIFIED, "USER_NOT_VERIFIED");
        } else if (userEntity.getStatus().equals(UserStatus.BLOCKED)) {
            throw new InActiveUserException(USER_IS_BLOCKED, "USER_IS_BLOCKED");
        }

    }

    /**
     * Returns additional details of users other than generic information such as
     * firstname, lastname etc if present.
     *
     * @param userIds List of user ids.
     * @return map of userId as key and value as map of additional user details.
     */
    Map<BigInteger, Map<String, Object>> findAdditionalAttributeData(List<BigInteger> userIds) {
        Map<BigInteger, Map<String, Object>> additionalAttributes = null;
        List<UserAttributeValueEntity> userAttributeValueEntities = userAttributeValueRepository
            .findAllByUserIdIn(userIds);
        if (!CollectionUtils.isEmpty(userAttributeValueEntities)) {
            List<BigInteger> attributeIds = userAttributeValueEntities.stream()
                .map(UserAttributeValueEntity::getAttributeId).toList();
            List<UserAttributeEntity> userAttributeEntities = userAttributeRepository.findAllById(attributeIds);
            if (CollectionUtils.isEmpty(userAttributeEntities)) {
                throw new ApplicationRuntimeException(ATTRIBUTE_METADATA_IS_MISSING, BAD_REQUEST);
            }
            additionalAttributes = mapAttributeNameToValue(userAttributeEntities, userAttributeValueEntities);
        }
        return additionalAttributes;
    }

    /**
     * Method to update user details in db.
     *
     * @param userId         userId of the user.
     * @param jsonPatch      jsonPatch containing details to be updated for a user.
     * @param loggedInUserId userId of the user trying to update the details.
     * @return UserResponse with updated user details.
     * @throws ResourceNotFoundException throw exception if user not found.
     * @throws UserAccountRoleMappingException  Exception while updating account role mapping for version 2
     */
    @Transactional
    @Modifying
    @Override
    public UserResponseBase editUser(BigInteger userId, JsonPatch jsonPatch, BigInteger loggedInUserId,
                                   boolean isExternalUser, String apiVersion)
        throws ResourceNotFoundException, UserAccountRoleMappingException {
        UserResponseBase userResponse = null;
        UserEntity user = getUserEntity(userId);
        if (isExternalUser) {
            validateIsExternalUser(user.getIsExternalUser());
        }

        Set<String> userRoleNames = getUserRoleNames(user);
        try {
            List<UserAttributeEntity> userAttributeEntities = userAttributeRepository.findAll();
            List<BigInteger> attributeIds = new ArrayList<>();
            Map<String, Object> additionalAttributes = new HashMap<>();
            Map<String, UserAttributeEntity> userAttributeEntitiesMap = groupUserAttributeEntityByName(
                userAttributeEntities);
            JsonNode jsonNodeList = objectMapper.convertValue(jsonPatch, JsonNode.class);
            List<JsonNode> operations = new ArrayList<>();
            jsonNodeList.forEach(operations::add);
            Iterator<JsonNode> iterator = operations.iterator();
            List<JsonNode> addressOperations = new ArrayList<>();
            List<JsonNode> accountOperations = new ArrayList<>();
            Set<String> addressEnums = Set.of(CITIES.getField(), STATES.getField(), COUNTRIES.getField(),
                ADDRESS1.getField(), ADDRESS2.getField(), TIMEZONE.getField(), POSTAL_CODES.getField());
            while (iterator.hasNext()) {
                JsonNode operation = iterator.next();
                if (apiVersion.equals(VERSION_1)
                    && operation.has(PATH) && operation.get(PATH).asText().equals(ROLES_RESOURCE_PATH)) {
                    patchUserRoles(user, userRoleNames, operation, isExternalUser);
                    iterator.remove();
                } else if (apiVersion.equals(VERSION_2)
                    && operation.has(PATH) && operation.get(PATH).asText().contains(ACCOUNT_RESOURCE_PATH)) {
                    accountOperations.add(operation);
                    iterator.remove();
                }
                String key = operation.get(PATH).asText().substring(1);
                checkForbiddenField(key, isExternalUser);
                UserAttributeEntity userAttributeEntity = userAttributeEntitiesMap.get(key);
                if (userAttributeEntity != null) {
                    handleAttributes(userAttributeEntity, attributeIds, additionalAttributes, operation, key);
                    iterator.remove();
                }
                if (addressEnums.contains(key)) {
                    addressOperations.add(operation);
                    iterator.remove();
                }
            }
            List<UserAccountRoleMappingEntity> acRoleMaps = new ArrayList<>();
            if (apiVersion.equals(VERSION_2) && !accountOperations.isEmpty()) {
                //Validate the account operations are correct and can be applied to this user
                acRoleMaps = validateAndCreateAccountRoleMappings(loggedInUserId,
                    user, accountOperations, objectMapper);
            }

            UserEntity userEntity = applyPatchToUser(JsonPatch.fromJson(objectMapper.valueToTree(operations)), user);
            UserEntity savedUser = updateUserEntity(userRoleNames, userAttributeEntities, attributeIds,
                additionalAttributes, userAttributeEntitiesMap, objectMapper, addressOperations, userEntity,
                loggedInUserId, isExternalUser, acRoleMaps);
            userResponse = buildUserResponse(userId, userRoleNames, savedUser, apiVersion);
        } catch (JsonPatchException | IOException e) {
            throw new ApplicationRuntimeException(FIELD_DATA_IS_INVALID, BAD_REQUEST, String.valueOf(e.getMessage()));
        }
        return userResponse;
    }

    private void handleAttributes(UserAttributeEntity userAttributeEntity, List<BigInteger> attributeIds,
            Map<String, Object> additionalAttributes, JsonNode operation, String key) {
        if (Boolean.TRUE.equals(userAttributeEntity.getReadOnly())) {
            throw new ApplicationRuntimeException(ACTION_FORBIDDEN, BAD_REQUEST, key,
                FIELD_CANNOT_BE_MODIFIED);
        }
        attributeIds.add(userAttributeEntity.getId());
        JsonNode value = operation.get(VALUE);
        additionalAttributes.put(key, value.asText());
    }

    private Set<String> getUserRoleNames(UserEntity user) {
        Set<BigInteger> roleIds = user.getAccountRoleMapping().stream().map(UserAccountRoleMappingEntity::getRoleId)
                .collect(Collectors.toSet());
        RoleListRepresentation roleListDto = rolesService.getRoleById(roleIds);
        return RoleManagementUtils.getRoleNamesFromRolesDto(roleListDto);
    }

    /**
     * Validate and create the user account role mapping list.
     *
     * @param loggedInUserId loggedInUserId
     * @param user user
     * @param accountOperations accountOperations
     * @param objectMapper objectMapper
     * @return List of UserAccountRoleMappingEntity
     * @throws UserAccountRoleMappingException UserAccountRoleMappingException
     * @throws ResourceNotFoundException ResourceNotFoundException
     */
    public List<UserAccountRoleMappingEntity> validateAndCreateAccountRoleMappings(BigInteger loggedInUserId,
            UserEntity user, List<JsonNode> accountOperations,
            ObjectMapper objectMapper) throws UserAccountRoleMappingException, ResourceNotFoundException  {
        List<AssociateAccountAndRolesDto> associationPatchList  = new ArrayList<>();
        for (JsonNode op : accountOperations) {
            AssociateAccountAndRolesDto arm = objectMapper.convertValue(op, AssociateAccountAndRolesDto.class);
            LOGGER.debug("account op dto entry: {}", arm);
            associationPatchList.add(arm);
        }
        return patchAccountRoleMappings(loggedInUserId, user, associationPatchList);
    }

    private UserEntity updateUserEntity(Set<String> userRoleNames, List<UserAttributeEntity> userAttributeEntities,
                                        List<BigInteger> attributeIds, Map<String, Object> additionalAttributes,
                                        Map<String, UserAttributeEntity> userAttributeEntitiesMap,
                                        ObjectMapper objectMapper,
                                        List<JsonNode> addressOperations, UserEntity userEntity,
                                        BigInteger loggedInUserId, boolean isExternalUser,
                                        List<UserAccountRoleMappingEntity> acRoleMaps)
        throws JsonPatchException, IOException, ResourceNotFoundException {
        updateUserAddress(objectMapper, addressOperations, userEntity);
        if (isExternalUser && !userRoleNames.isEmpty()) {
            validateRolePermitted(userRoleNames);
        }
        updateRolesAndAdditionalAttributes(userRoleNames, userAttributeEntities, attributeIds, additionalAttributes,
            userAttributeEntitiesMap, userEntity, loggedInUserId);

        if (!acRoleMaps.isEmpty()) {
            userEntity.setAccountRoleMapping(acRoleMaps);
        }
        userEntity.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
        userEntity.setUpdatedBy(SYSTEM);
        return userRepository.save(userEntity);
    }

    private UserResponseBase buildUserResponse(BigInteger userId, Set<String> userRoleNames, UserEntity savedUser,
            String apiVersion) {
        LOGGER.debug("build user response for user: {}, userRoles: {}", userId, userRoleNames);
        UserResponseBase userResponse = addRoleNamesAndMapToUserResponse(savedUser, apiVersion);

        Map<BigInteger, Map<String, Object>> updatedAdditionalAttributes = findAdditionalAttributeData(List.of(userId));
        if (!ObjectUtils.isEmpty(updatedAdditionalAttributes)) {
            userResponse.setAdditionalAttributes(updatedAdditionalAttributes.get(userId));
        }
        return userResponse;
    }

    private void updateRolesAndAdditionalAttributes(Set<String> userRoleNames,
                                                    List<UserAttributeEntity> userAttributeEntities,
                                                    List<BigInteger> attributeIds,
                                                    Map<String, Object> additionalAttributes, Map<String,
        UserAttributeEntity> userAttributeEntitiesMap,
                                                    UserEntity userEntity, BigInteger loggedInUserId)
        throws ResourceNotFoundException {
        if (!CollectionUtils.isEmpty(additionalAttributes)
            && isValidAdditionalAttributes(additionalAttributes, userAttributeEntities, false)) {
            patchAdditionalAttribute(additionalAttributes, userEntity, userAttributeEntitiesMap, attributeIds);
        }
        if (!userRoleNames.isEmpty()) { //This will happen only for /v1 api
            LOGGER.debug("checking permission for user {} to perform operation", loggedInUserId);
            if (Boolean.FALSE.equals(isUserAllowedToPerformOperation(loggedInUserId, userRoleNames))) {
                LOGGER.error("User '{}' is not allowed to perform operations on '{}' roles!", loggedInUserId,
                    userRoleNames);
                throw new ApplicationRuntimeException(ACTION_FORBIDDEN, BAD_REQUEST);
            }

            RoleListRepresentation roleList = rolesService.filterRoles(userRoleNames,
                Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT), Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT),
                false);
            Set<BigInteger> roleIds = RoleManagementUtils.getRoleIdsFromRoleList(roleList);
            List<UserAccountRoleMappingEntity> newAccountRoleMappingList = userEntity.getAccountRoleMapping().stream()
                .filter(a -> roleIds.contains(a.getRoleId())).collect(Collectors.toList());
            for (BigInteger r : roleIds) {
                if (newAccountRoleMappingList.stream().noneMatch(a -> a.getRoleId().equals(r))) {
                    UserAccountRoleMappingEntity urm = new UserAccountRoleMappingEntity();
                    urm.setRoleId(r);
                    urm.setAccountId(applicationProperties.getUserDefaultAccountId());
                    urm.setUpdateBy(SYSTEM);
                    urm.setUserId(userEntity.getId());
                    newAccountRoleMappingList.add(urm);
                }
            }
            userEntity.setAccountRoleMapping(newAccountRoleMappingList);
        }
    }

    private void checkForbiddenField(String key, Boolean isExternalUser) {
        if (Boolean.TRUE.equals(isExternalUser)) {
            if (USERNAME.equals(key)) {
                throw new ApplicationRuntimeException(ACTION_FORBIDDEN, BAD_REQUEST, key, FIELD_CANNOT_BE_MODIFIED);
            }
        } else {
            if (USERNAME.equals(key) || PASSWORD.equals(key) || DEV_IDS.getField().equals(key)) {
                throw new ApplicationRuntimeException(ACTION_FORBIDDEN, BAD_REQUEST, key, FIELD_CANNOT_BE_MODIFIED);
            }
        }
    }

    private void updateUserAddress(ObjectMapper objectMapper, List<JsonNode> addressOperations, UserEntity userEntity)
        throws IllegalArgumentException, IOException, JsonPatchException {
        List<UserAddressEntity> userAddresses = userEntity.getUserAddresses();
        if (!addressOperations.isEmpty()) {
            UserAddressEntity userAddressEntity = applyPatchToUserAddress(
                JsonPatch.fromJson(objectMapper.valueToTree(addressOperations)),
                userAddresses.get(0) != null ? userAddresses.get(0) : new UserAddressEntity());
            userAddressEntity.setUserEntity(userEntity);
            userEntity.setUserAddresses(List.of(userAddressEntity));
        }
    }

    private void patchUserRoles(UserEntity user, Set<String> userRoleNames, JsonNode operation,
                                Boolean isExternalUser) {
        patchNewRolesToUser(userRoleNames, operation);
        replaceExistingUserRoles(user, userRoleNames, operation, isExternalUser);
    }

    private void replaceExistingUserRoles(UserEntity user, Set<String> userRoleNames, JsonNode operation,
                                          Boolean isExternalUser) {
        if (operation.get(OPERATION).asText().equals(REPLACE)) {
            JsonNode nodeValue = operation.get(VALUE);
            if (nodeValue.isArray() && !nodeValue.isEmpty()) {
                if (Boolean.FALSE.equals(isExternalUser)) {
                    revokeUserTokens(user.getUserName());
                }
                userRoleNames.clear();
                for (JsonNode value : nodeValue) {
                    userRoleNames.add(value.asText());
                }
            }
        }
    }

    private void patchNewRolesToUser(Set<String> userRoleNames, JsonNode operation) {
        if (operation.get(OPERATION).asText().equals(ADD)) {
            JsonNode nodeValue = operation.get(VALUE);
            if (nodeValue.isArray()) {
                for (JsonNode value : nodeValue) {
                    if (!userRoleNames.contains(value.asText())) {
                        userRoleNames.add(value.asText());
                    }
                }
            }
        }
    }

    private UserEntity applyPatchToUser(JsonPatch patch, UserEntity targetUser) throws JsonPatchException, IOException {
        return applyPatch(patch, targetUser);
    }

    private UserAddressEntity applyPatchToUserAddress(JsonPatch patch, UserAddressEntity targetUserAddress)
        throws IOException, JsonPatchException {
        return applyPatch(patch, targetUserAddress);
    }

    private <T> T applyPatch(JsonPatch patch, T target) throws IOException, JsonPatchException {
        JsonNode patched = patch.apply(objectMapper.convertValue(target, JsonNode.class));
        return (T) objectMapper.treeToValue(patched, target.getClass());
    }

    /**
     * Method to delete single user from uidam system.
     *
     * @param userId         userId of user.
     * @param isUserExternal flag to determine whether user is external or not.
     * @param loggedInUserId userId of the user trying to delete the details.
     * @return user details for the provided user id.
     * @throws ResourceNotFoundException throw exception is user details is not
     *                                   present in db.
     */
    @Override
    @Transactional
    @Modifying
    public UserResponseV1 deleteUser(BigInteger userId, Boolean isUserExternal, BigInteger loggedInUserId)
        throws ResourceNotFoundException {
        LOGGER.debug("deleteUser -> external user = {}", isUserExternal);
        if (isUserExternal != null && Boolean.TRUE.equals(isUserExternal)) {
            return deleteExternalUser(userId, loggedInUserId);
        }
        return deleteUsers(new UsersDeleteFilter(Set.of(userId))).get(0);
    }

    /**
     * Method delete external user by id.
     *
     * @param userId         userId
     * @param loggedInUserId userId of the user trying to delete the user.
     * @return deleted user details.
     * @throws ResourceNotFoundException   exception to be thrown if user is already
     *                                     deleted/not present in db.
     * @throws ApplicationRuntimeException exception to be thrown if user is not
     *                                     external.
     */
    public UserResponseV1 deleteExternalUser(BigInteger userId, BigInteger loggedInUserId)
        throws ResourceNotFoundException {
        UserEntity userEntity = userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED);

        if (Objects.isNull(userEntity)) {
            throw new ResourceNotFoundException(USER, USER_ID_VARIABLE, String.valueOf(userId));
        }
        validateIsExternalUser(userEntity.getIsExternalUser());
        userEntity.setStatus(UserStatus.DELETED);
        userEntity.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
        userEntity.setUpdatedBy(String.valueOf(loggedInUserId));
        UserEntity updatedEntity = userRepository.save(userEntity);
        UserResponseV1 userResponse = (UserResponseV1) addRoleNamesAndMapToUserResponse(updatedEntity, VERSION_1);
        Map<BigInteger, Map<String, Object>> additionalAttributes = findAdditionalAttributeData(
            Collections.singletonList(userResponse.getId()));
        if (!CollectionUtils.isEmpty(additionalAttributes) && additionalAttributes.containsKey(userResponse.getId())) {
            userResponse.setAdditionalAttributes(additionalAttributes.get(userResponse.getId()));
        }
        return userResponse;
    }

    /**
     * Method to delete set of users by id.
     *
     * @param usersDeleteFilter pojo containing set of userIds
     * @return List of user response.
     * @throws ResourceNotFoundException exception to be thrown if user is already
     *                                   deleted/not present in db.
     */
    @Override
    public List<UserResponseV1> deleteUsers(UsersDeleteFilter usersDeleteFilter) throws ResourceNotFoundException {
        LOGGER.debug("deleteUsers START");
        List<UserEntity> savedUserEntities = userRepository.findAllByIdInAndStatusNot(usersDeleteFilter.getIds(),
            UserStatus.DELETED);
        if (CollectionUtils.isEmpty(savedUserEntities)) {
            throw new ResourceNotFoundException(USERS, "userIds", String.valueOf(usersDeleteFilter.getIds()));
        }
        Set<BigInteger> savedUserIds = savedUserEntities.stream().map(UserEntity::getId).collect(Collectors.toSet());
        Set<BigInteger> invalidIds = usersDeleteFilter.getIds().stream().filter(id -> !savedUserIds.contains(id))
            .collect(Collectors.toSet());
        if (!invalidIds.isEmpty()) {
            throw new ResourceNotFoundException(USERS, "userIds", String.valueOf(invalidIds));
        }
        savedUserEntities.forEach(savedUserEntity -> {
            savedUserEntity.setStatus(UserStatus.DELETED);
            savedUserEntity.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
            savedUserEntity.setUpdatedBy(SYSTEM);
            revokeUserTokens(savedUserEntity.getUserName());
            LOGGER.debug("deleting email verification data for user id {}", savedUserEntity.getId());
            emailVerificationRepository.deleteEmailVerification(savedUserEntity.getId());
            LOGGER.debug("fetching cloud profiles data for user id {}", savedUserEntity.getId());
            Optional<List<CloudProfileEntity>> cloudProfileOptional = cloudProfilesRepository
                .findAllByUserIdAndStatusIsNot(savedUserEntity.getId(), DELETED);
            if (cloudProfileOptional.isPresent()) {
                List<CloudProfileEntity> cloudProfileEntities = cloudProfileOptional.get();
                LOGGER.debug("deleting cloud profiles data for user id {} and cloudprofiles {}",
                       savedUserEntity.getId(), cloudProfileEntities);
                cloudProfileEntities.forEach(cloudProfileEntity -> {
                    LOGGER.debug("deleting cloud profiles data for cloudprofile id {}", cloudProfileEntity.getId());
                    cloudProfilesRepository.deleteCloudProfile(cloudProfileEntity.getId());
                });
            }
        });
        List<UserEntity> updatedUserEntities = userRepository.saveAll(savedUserEntities);
        Map<BigInteger, Map<String, Object>> additionalAttributes = findAdditionalAttributeData(
            updatedUserEntities.stream().map(UserEntity::getId).toList());
        return updatedUserEntities.stream().map(updatedUserEntity -> {
            UserResponseV1 userResponse = (UserResponseV1) addRoleNamesAndMapToUserResponse(updatedUserEntity,
                VERSION_1);
            if (!CollectionUtils.isEmpty(additionalAttributes)
                && additionalAttributes.containsKey(updatedUserEntity.getId())) {
                userResponse.setAdditionalAttributes(additionalAttributes.get(updatedUserEntity.getId()));
            }
            LOGGER.info("deleteUsers END");
            return userResponse;
        }).toList();
    }

    /**
     * Method to map role names with respect to each roleId mapped with user and
     * return as response.
     *
     * @param userEntity user details from database
     * @return user details for the provided user.
     */
    private UserResponseBase addRoleNamesAndMapToUserResponse(UserEntity userEntity, String version) {
        switch (version) {
            case VERSION_1: {
                UserResponseV1 userRespV1 = UserMapper.USER_MAPPER.mapToUserResponseV1(userEntity);
                Set<BigInteger> roleIds = userEntity.getAccountRoleMapping().stream()
                    .map(UserAccountRoleMappingEntity::getRoleId).collect(Collectors.toSet());
                LOGGER.debug("RoleIds in addRoleNamesAndMapToUserResponse V1: {}", roleIds);
                RoleListRepresentation roleListRepresentation = rolesService.getRoleById(roleIds);
                Set<String> roles = RoleManagementUtils.getRoleNamesFromRolesDto(roleListRepresentation);
                userRespV1.setRoles(roles);
                return userRespV1;
            }
            case VERSION_2: {
                Map<BigInteger, Set<BigInteger>> acRoleMap = userEntity.getAccountRoleMapping().stream()
                    .collect(Collectors.groupingBy(UserAccountRoleMappingEntity::getAccountId,
                        Collectors.mapping(UserAccountRoleMappingEntity::getRoleId, Collectors.toSet())));

                // Now map this map to set of accounts
                Set<UserAccountsAndRoles> accounts = new HashSet<>();
                for (Map.Entry<BigInteger, Set<BigInteger>> entry : acRoleMap.entrySet()) {
                    BigInteger accountId = entry.getKey();
                    String accountName = accountIdToNameMapping.containsKey(accountId)
                        ? accountIdToNameMapping.get(accountId)
                        : accountRepository.findById(accountId).get().getAccountName();
                    Set<BigInteger> roleIds = entry.getValue();
                    Set<String> roles = new HashSet<>();
                    if (!roleIds.stream().allMatch(roleIdToNameMapping::containsKey)) {
                        RoleListRepresentation roleListRepresentation = rolesService.getRoleById(roleIds);
                        roleIdToNameMapping = roleListRepresentation.getRoles().stream()
                            .collect(Collectors.toMap(RoleCreateResponse::getId, RoleCreateResponse::getName));
                    }
                    roleIds.stream().forEach(r -> roles.add(roleIdToNameMapping.get(r)));
                    UserAccountsAndRoles ac = new UserDtoV2.UserAccountsAndRoles();
                    ac.setAccount(accountName);
                    ac.setRoles(roles);
                    accounts.add(ac);
                }
                UserResponseV2 userRespV2 = UserMapper.USER_MAPPER.mapToUserResponseV2(userEntity);
                userRespV2.setAccounts(accounts);
                return userRespV2;
            }
            default: {
                throw new ApplicationRuntimeException(INVALID_API_VERSION, BAD_REQUEST);
            }
        }
    }

    /**
     * Method get user data based on criteria and filter query provided by user.
     *
     * @param userGetFilter params and values by which user data needs to be
     *                      filtered.
     * @param pageNumber    pageNumber for which the set of records needs to be
     *                      viewed.
     * @param pageSize      total number of records which are present as part of a
     *                      page.
     * @param sortBy        user attribute by which records are to be sorted.
     * @param sortOrder     order in which records are to be sorted
     * @param ignoreCase    while fetching records we need to check if filter needs
     *                      to be applied data having case-sensitive records.
     * @param searchType    filtering data for each attribute based on the provided
     *                      search type whether attribute value starts with xvz
     *                      (i.e., PREFIX) etc.
     * @return List of user details matching provided filter criteria.
     * @throws ResourceNotFoundException throws exception if user details not found.
     */
    @Override
    public List<UserResponseBase> getUsers(UsersGetFilterBase userGetFilter, Integer pageNumber, Integer pageSize,
                                           String sortBy, String sortOrder, boolean ignoreCase, SearchType searchType)
        throws ResourceNotFoundException {
        Pageable pageable = sortBy != null
            ? PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.fromString(sortOrder), sortBy))
            : PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.fromString(sortOrder), IDS.getField()));
        if (!ObjectUtils.isEmpty(userGetFilter.getAdditionalAttributes())) {
            List<BigInteger> userIds = filterQueryByAdditionalAttributes(userGetFilter.getAdditionalAttributes(),
                ignoreCase, searchType);
            if (userIds.isEmpty()) {
                throw new ResourceNotFoundException(USER, "userGetFilter", userGetFilter.toString());
            }
            Set<BigInteger> userGetIds = userGetFilter.getIds();
            if (!ObjectUtils.isEmpty(userGetIds)) {
                userIds.addAll(userGetIds);
            }
            userGetFilter.setIds(new HashSet<>(userIds));
        }
        Specification<UserEntity> specification = createFilterQuery(userGetFilter, ignoreCase, searchType);
        List<UserEntity> userEntities = userRepository.findAll(specification, pageable).getContent();
        if (userEntities.isEmpty()) {
            return Collections.emptyList();
        }
        Map<BigInteger, Map<String, Object>> additionalAttributes = findAdditionalAttributeData(
            userEntities.stream().map(UserEntity::getId).toList());
        String version = (userGetFilter instanceof UsersGetFilterV1) ? VERSION_1 : VERSION_2;
        return userEntities.stream().map(userEntity -> {
            UserResponseBase userResponse = addRoleNamesAndMapToUserResponse(userEntity, version);
            if (!ObjectUtils.isEmpty(additionalAttributes)) {
                userResponse.setAdditionalAttributes(additionalAttributes.get(userResponse.getId()));
            }
            return userResponse;
        }).toList();
    }

    /**
     * Method to get user attributes meta data.
     *
     * @return list of user attributes metadata.
     */
    @Override
    public List<UserMetaDataResponse> getUserMetaData() {
        List<Field> fieldList = getUserFieldList();
        Map<String, String> colDataTypes = getColumnDataTypes();
        List<UserMetaDataResponse> userMetaDataResponses = fieldList.stream()
            .map(field -> mapToAttributeMetaData(field, colDataTypes)).filter(Objects::nonNull).toList();
        List<UserAttributeEntity> userAttributeEntities = userAttributeRepository.findAll();
        if (!ObjectUtils.isEmpty(userAttributeEntities)) {
            List<UserMetaDataResponse> additionalAttributeMetaData = userAttributeEntities.stream()
                .map(UserMapper.USER_MAPPER::mapToMetaDataResponse).toList();
            userMetaDataResponses = Stream.concat(userMetaDataResponses.stream(), additionalAttributeMetaData.stream())
                .toList();
        }
        return userMetaDataResponses.stream().filter(UserManagementUtils.distinctByKey(UserMetaDataResponse::getName))
            .toList();
    }

    /**
     * Method to get non-dynamic user fields.
     *
     * @return List of non-dynamic user fields.
     */
    private List<Field> getUserFieldList() {
        Field[] userFields = UserEntity.class.getDeclaredFields();
        Field[] addressFields = UserAddressEntity.class.getDeclaredFields();
        Field[] accountRoleMappingFields = UserAccountRoleMappingEntity.class.getDeclaredFields();
        Field[] userAndAddress = ArrayUtils.addAll(userFields, addressFields);
        return Arrays.asList(ArrayUtils.addAll(userAndAddress, accountRoleMappingFields));
    }

    /**
     * Method to get data type for each user attribute.
     *
     * @return Map of user attribute names and their respective data types.
     */
    private Map<String, String> getColumnDataTypes() {
        Map<String, String> columnDataTypes = new ConcurrentHashMap<>();
        columnDataTypes.putAll(userManagementDao.getColumnDataType(entityManager, USER_ENTITY_TABLE_NAME));
        columnDataTypes.putAll(userManagementDao.getColumnDataType(entityManager, USER_ADDRESS_ENTITY_TABLE_NAME));
        columnDataTypes
            .putAll(userManagementDao.getColumnDataType(entityManager, USER_ACCOUNT_ROLE_MAPPING_TABLE_NAME));
        return columnDataTypes;
    }

    /**
     * Method to update/created list of user attribute metadata.
     *
     * @param userMetaDataRequests list of user attribute metadata.
     * @return list of metadata responses.
     */
    @Override
    public List<UserMetaDataResponse> putUserMetaData(List<UserMetaDataRequest> userMetaDataRequests) {
        List<UserAttributeEntity> userAttributeEntities = userAttributeRepository.findAll();
        Map<String, UserAttributeEntity> userAttributeEntityByNameMap = groupUserAttributeEntityByName(
            userAttributeEntities);
        List<UserAttributeEntity> attributesToAddInDataBase = new ArrayList<>();
        userMetaDataRequests.forEach(userMetaData -> {
            UserAttributeEntity userAttributeEntity;
            userMetaData.setDynamicAttribute(true);
            if (userAttributeEntityByNameMap.containsKey(userMetaData.getName().toLowerCase(Locale.ROOT))) {
                userAttributeEntity = userAttributeEntityByNameMap.get(userMetaData.getName().toLowerCase(Locale.ROOT));
                UserMapper.USER_MAPPER.updateMetaDataEntity(userMetaData, userAttributeEntity);
                userAttributeEntity.setUpdatedBy(SYSTEM);
                userAttributeEntity.setUpdatedDate(Timestamp.from(Instant.now()));
                attributesToAddInDataBase.add(userAttributeEntity);
            } else {
                userAttributeEntity = UserMapper.USER_MAPPER.mapToMetaDataEntity(userMetaData);
                userAttributeEntity.setCreatedDate(Timestamp.from(Instant.now()));
                attributesToAddInDataBase.add(userAttributeEntity);
            }
        });
        List<UserAttributeEntity> savedAttributes = userAttributeRepository.saveAll(attributesToAddInDataBase);
        return savedAttributes.stream().map(UserMapper.USER_MAPPER::mapToMetaDataResponse).toList();
    }

    /**
     * Method to map and return user attribute with user attribute metadata as
     * response.
     *
     * @param field        user attribute.
     * @param colDataTypes user attribute data type.
     * @return user metadata response.
     */
    private UserMetaDataResponse mapToAttributeMetaData(Field field, Map<String, String> colDataTypes) {
        Column col = field.getAnnotation(Column.class);
        if (!Objects.isNull(col)) {
            String fieldName = StringUtils.isNotEmpty(col.name()) ? col.name() : field.getName();
            return UserMetaDataResponse.builder().name(fieldName).mandatory(!col.nullable()).unique(col.unique())
                .readOnly(!col.updatable()).searchable(Boolean.TRUE).dynamicAttribute(Boolean.FALSE)
                .type(colDataTypes.getOrDefault(fieldName, field.getType().getSimpleName())).regex(".*").build();
        } else {
            return null;
        }
    }

    /**
     * Method to create filter query for additional attributes of user.
     *
     * @param additionalAttributes map of attribute name as key and set of attribute
     *                             values as value.
     * @param ignoreCase           boolean true/false for
     *                             case-sensitive/case-insensitive search
     * @param searchType           define a search type example if attribute
     *                             contains/ends-with/starts-with.
     * @return List of UserIds matching the filter query.
     */
    public List<BigInteger> filterQueryByAdditionalAttributes(Map<String, Set<String>> additionalAttributes,
                                                              boolean ignoreCase, SearchType searchType) {
        List<UserAttributeEntity> userAttributeEntities = userAttributeRepository.findAll();
        Set<String> badDtoAttributes = findBadDtoAttributes(userAttributeEntities, additionalAttributes.keySet());
        if (!ObjectUtils.isEmpty(badDtoAttributes)) {
            throw new ApplicationRuntimeException(FIELD_NOT_FOUND, BAD_REQUEST, String.valueOf(badDtoAttributes));
        }
        Specification<UserAttributeValueEntity> specification = createUserAttributeValueSpec(userAttributeEntities,
            ignoreCase, searchType, additionalAttributes);

        List<UserAttributeValueEntity> userAttributeValueEntities = userAttributeValueRepository.findAll(specification);

        return userAttributeValueEntities.stream().map(UserAttributeValueEntity::getUserId).toList();
    }

    /**
     * Create user attributevalue specifications for filter query.
     *
     * @param userAttributeEntities List attribute metadata.
     * @param ignoreCase            boolean true/false for
     *                              case-sensitive/case-insensitive search
     * @param searchType            define a search type example if attribute
     *                              contains/ends-with/starts-with.
     * @param additionalAttributes  map of attribute name as key and set of
     *                              attribute values as value.
     * @return return set of specifications.
     */
    public Specification<UserAttributeValueEntity> createUserAttributeValueSpec(
        List<UserAttributeEntity> userAttributeEntities, boolean ignoreCase, SearchType searchType,
        Map<String, Set<String>> additionalAttributes) {
        Map<String, BigInteger> userAttributeNameIdMap = mapUserAttributeIdByName(userAttributeEntities);
        Map<BigInteger, Set<String>> filterMap = additionalAttributes.entrySet().stream().collect(Collectors.toMap(
            additionalAttribute -> userAttributeNameIdMap.get(additionalAttribute.getKey()), Map.Entry::getValue));
        List<UserAttributeSpecification> userAttributeSpecifications = filterMap.entrySet().stream()
            .filter(entry -> !CollectionUtils.isEmpty(entry.getValue())).map(entry -> {
                BigInteger field = entry.getKey();
                Set<String> searchByList = entry.getValue();
                SearchCriteria searchCriteria = new SearchCriteria(field, searchType, ignoreCase);
                searchCriteria.setStringValue(searchByList);
                if (CollectionUtils.isEmpty(searchCriteria.getValue())) {
                    return null;
                } else {
                    return new UserAttributeSpecification(searchCriteria);
                }
            }).filter(Objects::nonNull).toList();

        Specification<UserAttributeValueEntity> specification = null;
        for (int i = 0; i < userAttributeSpecifications.size(); i++) {
            if (i == 0) {
                specification = Specification.where(userAttributeSpecifications.get(0));
            } else {
                specification = specification.and(userAttributeSpecifications.get(i));
            }
        }
        return specification;
    }

    /**
     * Method to map user attribute name with attribute id.
     *
     * @param userAttributeEntities List of user attribute metdata.
     * @return map of userattribute name and id.
     */
    public Map<String, BigInteger> mapUserAttributeIdByName(List<UserAttributeEntity> userAttributeEntities) {
        return userAttributeEntities.stream()
            .collect(Collectors.toMap(UserAttributeEntity::getName, UserAttributeEntity::getId));
    }

    /**
     * Update additional attributes value data for specific users.
     *
     * @param additionalAttributes     map of user additional attribute name and
     *                                 values.
     * @param userEntity               user details object.
     * @param userAttributeEntitiesMap map of attribute names and attribute metadata
     *                                 entity.
     * @param attributeIds             List of attribute ids.
     */
    private void patchAdditionalAttribute(Map<String, Object> additionalAttributes, UserEntity userEntity,
                                          Map<String, UserAttributeEntity> userAttributeEntitiesMap,
                                          List<BigInteger> attributeIds) {
        List<UserAttributeValueEntity> finalAttributeValueEntities = new ArrayList<>();
        List<UserAttributeValueEntity> userAttributeValueEntities = userAttributeValueRepository
            .findAllByUserIdAndAttributeIdIn(userEntity.getId(), attributeIds);
        Map<BigInteger, UserAttributeValueEntity> userAttributeValueEntitiesMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(userAttributeValueEntities)) {
            userAttributeValueEntitiesMap = mapUserAttributeValueEntityByAttributeId(userAttributeValueEntities);
        }
        Map<BigInteger, UserAttributeValueEntity> finalUserAttributeValueEntitiesMap = userAttributeValueEntitiesMap;
        additionalAttributes.forEach((key, value) -> {
            if (!finalUserAttributeValueEntitiesMap.isEmpty()
                && finalUserAttributeValueEntitiesMap.containsKey(userAttributeEntitiesMap.get(key).getId())) {
                UserAttributeValueEntity userAttributeValueEntity = finalUserAttributeValueEntitiesMap
                    .get(userAttributeEntitiesMap.get(key).getId());
                userAttributeValueEntity.setValue(parseAdditionalAttributeValue(userAttributeEntitiesMap, key, value));
                finalAttributeValueEntities.add(userAttributeValueEntity);
            } else {
                UserAttributeValueEntity userAttributeValueEntity = new UserAttributeValueEntity();
                userAttributeValueEntity.setUserId(userEntity.getId());
                userAttributeValueEntity.setAttributeId(userAttributeEntitiesMap.get(key).getId());
                userAttributeValueEntity.setValue(parseAdditionalAttributeValue(userAttributeEntitiesMap, key, value));
                finalAttributeValueEntities.add(userAttributeValueEntity);
            }
        });
        if (!ObjectUtils.isEmpty(finalAttributeValueEntities)) {
            userAttributeValueRepository.saveAll(finalAttributeValueEntities);
        }
    }

    /**
     * Method to map fields to respective user entity.
     *
     * @param classType   attribute/field class Type.
     * @param key         attribute name
     * @param classObject class object.
     * @param data        attribute value.
     * @return if attribute data mapping to class is successful then return true
     *         else false.
     */
    public boolean mapUserFields(Class<?> classType, String key, Object classObject, Object data) {
        Field field = ReflectionUtils.findField(classType, key);
        if (Objects.isNull(field)) {
            return false;
        } else if (classObject instanceof UserEntity userEntity) {
            UserMapper.USER_MAPPER.updateUserEntityData(Map.of(key, String.valueOf(data)), userEntity);
        } else if (classObject instanceof UserAddressEntity userAddressEntity) {
            UserMapper.USER_MAPPER.updateUserAddressEntityData(Map.of(key, (String) data), userAddressEntity);
        }
        return true;
    }

    /**
     * Method to map attribute value entity with attributeId.
     *
     * @param userAttributeValueEntities List of attribute value entity.
     * @return map of attributeId as key and value as UserAttributeValueEntity.
     */
    public Map<BigInteger, UserAttributeValueEntity> mapUserAttributeValueEntityByAttributeId(
        List<UserAttributeValueEntity> userAttributeValueEntities) {
        return userAttributeValueEntities.stream().collect(Collectors.toMap(UserAttributeValueEntity::getAttributeId,
            userAttributeValueEntity -> userAttributeValueEntity));
    }

    /**
     * Method to validate user/client permissions.
     *
     * @param userDto        input user data.
     * @param loggedInUserId user id of user trying to modify user data.
     * @throws ResourceNotFoundException throw exception if user details not found.
     */
    public void validateUserPermissions(UserDtoBase userDto, BigInteger loggedInUserId)
        throws ResourceNotFoundException {
        if (TRUE == userDto.getIsExternalUser()) {
            return;
        }

        String client = userDto.getAud();
        if (loggedInUserId == null) {
            // if userId is not there then validation on client as grant type may be
            // client_credentials(not password)
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Validate session, user name is not present, authorization header is: {}", client);
            }
            if (!isClientAllowedToManageUsers(client)) {
                throw new ApplicationRuntimeException(ACTION_FORBIDDEN, BAD_REQUEST);
            }
            return;
        }

        // checking if username is there in session i.e. grant type is password
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Validate session, user name is present {}", loggedInUserId);
        }

        if (userDto instanceof UserDtoV2 userDtoV2) {
            Set<UserAccountsAndRoles> accounts = userDtoV2.getAccounts();
            for (UserAccountsAndRoles account : accounts) {
                if (Boolean.FALSE.equals(isUserAllowedToPerformOperation(loggedInUserId, account.getRoles()))) {
                    LOGGER.error("User '{}' is not allowed to perform operations on '{}' role!", loggedInUserId,
                        account.getRoles());
                    throw new ApplicationRuntimeException(ACTION_FORBIDDEN, BAD_REQUEST);
                }
            }
        }
    }

    /**
     * Method to validate user permissions. 1. Check if loggedInUser has a superset
     * of the userDto roles. If true, return valid. 2. a) If above is false, filter
     * out the valid role names of loggedInUser roles and userdto roles b) If there
     * are invalid roles in the super set, throw exception. Not valid 3. Now check
     * the scope of loggedInUser. a) If it is IGNITE_SYSTEM_SCOPE, return valid. b)
     * If it contains all userDto role scopes (derived) then return valid, else
     * invalid
     *
     * @param loggedInUserId user id of user trying to modify user data.
     * @param roles          Set of userDto role names.
     * @return true if user allowed to perform operation else false.
     * @throws ApplicationRuntimeException if any role mismatch exists.
     */
    private Boolean isUserAllowedToPerformOperation(BigInteger loggedInUserId, Set<String> roles)
        throws ResourceNotFoundException {
        LOGGER.debug("Checking if user with Id '{}' is allowed to perform operation on '{}' role", loggedInUserId,
            roles);
        UserResponseBase loggedInUser = getUser(loggedInUserId, VERSION_1);
        if (ObjectUtils.isEmpty(loggedInUser)) {
            return false;
        }

        Set<String> loggedInUserRoles = getUserRolesFromUserResponse(loggedInUser);
        if (loggedInUserRoles.equals(roles)) {
            return true;
        }

        Set<String> userRoles = new HashSet<>();
        userRoles.addAll(roles);
        userRoles.addAll(loggedInUserRoles);
        RoleListRepresentation roleListDto = rolesService.filterRoles(userRoles,
            Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT), Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT),
            false);

        LOGGER.debug("Response from auth management roles api : {}", roleListDto);

        if (ObjectUtils.isEmpty(roleListDto)) {
            throw new ApplicationRuntimeException("role", BAD_REQUEST);
        }
        Set<String> roleNames = roleListDto.getRoles().stream().map(RoleCreateResponse::getName)
            .collect(Collectors.toSet());
        if (!roleNames.containsAll(loggedInUserRoles) || !roleNames.containsAll(roles)) {
            throw new ApplicationRuntimeException("role", BAD_REQUEST);
        }

        Set<String> userScopes = RoleManagementUtils.getScopesFromRolesDto(roleListDto, loggedInUserRoles);
        if (userScopes.contains(IGNITE_SYSTEM_SCOPE)) {
            return true;
        }

        Set<String> roleScopes = RoleManagementUtils.getScopesFromRolesDto(roleListDto, roles);
        return userScopes.containsAll(roleScopes);
    }

    /**
     * Returns roles depending on version 1 response or version 2 response.
     *
     * @param userResponse userResponse
     * @return set of roles extracted from the response
     */
    public Set<String> getUserRolesFromUserResponse(UserResponseBase userResponse) {
        Set<String> roles = new HashSet<>();

        if (userResponse instanceof UserResponseV1 userRespV1) {
            roles = userRespV1.getRoles();
        } else if (userResponse instanceof UserResponseV2 userRespV2) {
            Set<UserAccountsAndRoles> accounts = userRespV2.getAccounts();
            for (UserAccountsAndRoles ac : accounts) {
                roles.addAll(ac.getRoles());
            }
        }
        return roles;
    }

    /**
     * Method to validate if provided roles exists in database.
     *
     * @param roleListDto RoleDetails list.
     * @param roles       set of role names.
     * @return true if roles are valid.
     */
    public boolean validateRoleExists(RoleListRepresentation roleListDto, Set<String> roles) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Check for user role {}", roles);
        }
        // Verify result
        if (roleListDto == null) {
            LOGGER.warn("auth management response  does not contains results, roles {} does not exists", roles);
            return false;
        }
        Set<String> savedRoles = roleListDto.getRoles().stream().map(RoleCreateResponse::getName)
            .collect(Collectors.toSet());
        Set<String> userRoles = new HashSet<>(roles);
        userRoles.removeAll(savedRoles);
        if (CollectionUtils.isEmpty(savedRoles) || !CollectionUtils.isEmpty(userRoles)) {
            LOGGER.warn("Provided roles {} do not exist in our db", userRoles);
            return false;
        }
        return true;
    }

    /**
     * Method to create query to fetch user details based on certain criteria.
     *
     * @param usersGetFilter Input user filter attribute name and value.
     * @param ignoreCase     Input flag true/false for case-sensitive and
     *                       case-insensitive search
     * @param searchType     define search type prefix, suffix to filter user data.
     * @return specification query for user.
     */
    private Specification<UserEntity> createFilterQuery(UsersGetFilterBase usersGetFilter, boolean ignoreCase,
                                                        SearchType searchType) {
        Map<String, Set<?>> filterMap = createFilterMap(usersGetFilter);
        List<UserSpecification> userSpecifications = filterMap.entrySet().stream()
            .filter(entry -> !StringUtils.isEmpty(entry.getKey()) && !CollectionUtils.isEmpty(entry.getValue()))
            .map(entry -> {
                String field = entry.getKey();
                Set<?> searchByList = entry.getValue();
                SearchCriteria searchCriteria = new SearchCriteria(field, searchByList, searchType, ignoreCase);
                searchCriteria.setValue(searchByList);
                if (searchCriteria.getValue().isEmpty()) {
                    return null;
                } else {
                    return new UserSpecification(searchCriteria);
                }
            }).filter(Objects::nonNull).toList();
        Specification<UserEntity> specification = Specification.where(UserSpecification.getExistingUserSpecification());
        for (int i = 0; i < userSpecifications.size(); i++) {
            if (i == 0) {
                specification = specification.and(userSpecifications.get(0));
            } else {
                specification = specification.and(userSpecifications.get(i));
            }
        }
        return specification;
    }

    /**
     * Method to create map of user filter attributes and values.
     *
     * @param usersGetFilter object to filter user based on criteria.
     * @return map of user attributes and values.
     */
    private Map<String, Set<?>> createFilterMap(UsersGetFilterBase usersGetFilter) {
        Map<String, Set<?>> filterMap = new HashMap<>();
        filterMap.put(USER_ROOT + "." + IDS.getField(), usersGetFilter.getIds());
        filterMap.put(USER_ROOT + "." + USER_NAMES.getField(), usersGetFilter.getUserNames());
        if (usersGetFilter.getRoles() != null) {
            try {
                RoleListRepresentation roleListDto = rolesService.filterRoles(usersGetFilter.getRoles(),
                    Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT),
                    Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT), false);
                filterMap.put(USER_ACCOUNT_ROLE_ROOT + "." + ROLEIDS.getField(),
                    RoleManagementUtils.getRoleIdsFromRoleList(roleListDto));
            } catch (EntityNotFoundException e) {
                filterMap.put(USER_ACCOUNT_ROLE_ROOT + "." + ROLEIDS.getField(), Set.of(NO_ROLEID_FOR_FILTER));
            }
        }
        if (usersGetFilter instanceof UsersGetFilterV2 usergetFilterv2
            && !CollectionUtils.isEmpty(usergetFilterv2.getAccountNames())) {
            List<AccountEntity> activeAccounts = accountRepository.findAllByStatusAndAccountNameIn(AccountStatus.ACTIVE,
                usergetFilterv2.getAccountNames());
            filterMap.put(USER_ACCOUNT_ROLE_ROOT + "." + ACCOUNTIDS.getField(),
                activeAccounts.stream().map(AccountEntity::getId).collect(Collectors.collectingAndThen(
                    Collectors.toSet(), set -> !set.isEmpty() ? set : Set.of(UUID.randomUUID()))));
        }
        if (CollectionUtils.isEmpty(usersGetFilter.getStatus())) {
            usersGetFilter.setStatus(Set.of(UserStatus.ACTIVE));
        }
        filterMap.put(USER_ROOT + "." + STATUS.getField(), usersGetFilter.getStatus());
        filterMap.put(USER_ROOT + "." + FIRST_NAMES.getField(), usersGetFilter.getFirstNames());
        filterMap.put(USER_ROOT + "." + LAST_NAMES.getField(), usersGetFilter.getLastNames());
        filterMap.put(USER_ADDRESS_ROOT + "." + COUNTRIES.getField(), usersGetFilter.getCountries());
        filterMap.put(USER_ADDRESS_ROOT + "." + STATES.getField(), usersGetFilter.getStates());
        filterMap.put(USER_ADDRESS_ROOT + "." + CITIES.getField(), usersGetFilter.getCities());
        filterMap.put(USER_ADDRESS_ROOT + "." + ADDRESS1.getField(), usersGetFilter.getAddress1());
        filterMap.put(USER_ADDRESS_ROOT + "." + ADDRESS2.getField(), usersGetFilter.getAddress2());
        filterMap.put(USER_ADDRESS_ROOT + "." + POSTAL_CODES.getField(), usersGetFilter.getPostalCodes());
        filterMap.put(USER_ADDRESS_ROOT + "." + TIMEZONE.getField(), Collections.emptySet());
        filterMap.put(USER_ROOT + "." + EMAILS.getField(), usersGetFilter.getEmails());
        filterMap.put(USER_ROOT + "." + PHONE_NUMBERS.getField(), usersGetFilter.getPhoneNumbers());
        filterMap.put(USER_ROOT + "." + GENDER.getField(), usersGetFilter.getGender());
        filterMap.put(USER_ROOT + "." + LOCALES.getField(), usersGetFilter.getLocales());
        filterMap.put(USER_ROOT + "." + DEV_IDS.getField(), usersGetFilter.getDevIds());
        return filterMap;
    }

    /**
     * Method to add user event (ex.. login).
     *
     * @param userEventsDto received as request during login.
     * @param userId        user unique id.
     */
    @Override
    public void addUserEvent(UserEventsDto userEventsDto, String userId) {
        int allowedLoginAttempts = Integer.parseInt(applicationProperties.getMaxAllowedLoginAttempts());
        if (Optional.ofNullable(userEventsDto).isPresent()
            && Optional.ofNullable(userEventsDto.getEventType()).isPresent()
            && Optional.ofNullable(userEventsDto.getEventMessage()).isPresent()
            && Optional.ofNullable(userEventsDto.getEventStatus()).isPresent()) {
            UserEvents userEvents = new UserEvents();
            userEvents.setUserId(new BigInteger(userId));
            userEvents.setEventType(userEventsDto.getEventType());
            userEvents.setEventStatus(userEventsDto.getEventStatus());
            userEvents.setEventMessage(userEventsDto.getEventMessage());
            LOGGER.debug("saving user event details for userId: {} and events: {} ", userId, userEvents);
            userEventRepository.save(userEvents);
            List<UserEvents> userEventList = userEventRepository.findUserEventsByUserIdAndEventType(
                new BigInteger(userId), UserEventType.LOGIN_ATTEMPT.getValue(), allowedLoginAttempts);
            LOGGER.debug("user event list: {}", userEventList);
            if (userEventList.size() >= allowedLoginAttempts) {
                List<UserEvents> failedLoginAttemptsList = userEventList.stream()
                    .filter(a -> a.getEventType().equals(UserEventType.LOGIN_ATTEMPT.getValue())
                        && a.getEventStatus().equals(UserEventStatus.FAILURE.getValue()))
                    .toList();
                LOGGER.debug("user event list for failed login attempt: {}", failedLoginAttemptsList);
                if (failedLoginAttemptsList.size() >= allowedLoginAttempts) {
                    Optional<UserEntity> userEntity = userRepository.findById(new BigInteger(userId));
                    if (userEntity.isPresent() && UserStatus.ACTIVE.equals(userEntity.get().getStatus())) {
                        UserEntity userEntityObject = userEntity.get();
                        userEntityObject.setStatus(UserStatus.BLOCKED);
                        userRepository.save(userEntityObject);
                        LOGGER.info("user status updated to BLOCKED for userId {}", userId);
                    }
                }
            }

        } else {
            LOGGER.error("events data missing for userId {}!", userId);
            throw new ApplicationRuntimeException("events", BAD_REQUEST);
        }
    }

    /**
     * Method to update user password.
     *
     * @param userUpdatePasswordDto update password dto object.
     * @throws ResourceNotFoundException throw exception if user details not found.
     */
    @Override
    public void updateUserPasswordUsingRecoverySecret(UserUpdatePasswordDto userUpdatePasswordDto)
        throws ResourceNotFoundException {
        UserRecoverySecret userRecoverySecret = userRecoverySecretRepository
            .findUserRecoverySecretDetailsByRecoverySecret(userUpdatePasswordDto.getSecret());
        LOGGER.debug("updating password for userId {} with recovery secret {}", userRecoverySecret.getUserId(),
            userRecoverySecret.getRecoverySecret());
        if (UserRecoverySecretStatus.GENERATED.name().equals(userRecoverySecret.getRecoverySecretStatus())) {
            long minutes = applicationProperties.getRecoverySecretExpiresInMinutes();
            if (userRecoverySecret.getSecretGeneratedAt().plus(minutes, ChronoUnit.MINUTES).isAfter(Instant.now())) {
                UserEntity userEntity = userRepository.findByIdAndStatusNot(userRecoverySecret.getUserId(),
                    UserStatus.DELETED);
                if (Objects.isNull(userEntity)) {
                    throw new ResourceNotFoundException(USER, USER_ID_VARIABLE,
                        String.valueOf(userRecoverySecret.getUserId()));
                }
                validateUserNameAndPassword(userEntity.getUserName(), userUpdatePasswordDto.getPassword());
                CustomPasswordPatternPolicy customPasswordPatternPolicy = new CustomPasswordPatternPolicy(
                    applicationProperties);
                if (customPasswordPatternPolicy.enforce(userUpdatePasswordDto.getPassword(), userEntity.getUserName(),
                        userEntity.getPwdChangedtime())) {
                    String passwordSalt = PasswordUtils.getSalt();
                    String hashPassword = PasswordUtils.getSecurePassword(userUpdatePasswordDto.getPassword(),
                            passwordSalt, applicationProperties.getPasswordEncoder());
                    userEntity.setUserPassword(hashPassword);
                    userEntity.setPasswordSalt(passwordSalt);
                    userEntity.setPwdChangedtime(Timestamp.from(Instant.now()));
                    userRepository.save(userEntity);
                    userRecoverySecret.setRecoverySecretStatus(UserRecoverySecretStatus.VERIFIED.name());
                    userRecoverySecretRepository.save(userRecoverySecret);
                    revokeUserTokens(userEntity.getUserName());
                } else {
                    throw new ApplicationRuntimeException(customPasswordPatternPolicy.getErrorMessage(), BAD_REQUEST,
                        ApiConstants.PASSWORD);
                }

            } else {
                LOGGER.info("recovery secret is expired for the userid {}", userRecoverySecret.getUserId());

                userRecoverySecret.setRecoverySecretStatus(UserRecoverySecretStatus.EXPIRED.name());
                userRecoverySecretRepository.save(userRecoverySecret);
                throw new RecoverySecretExpireException("recover secret is expired!");
            }
        } else if (UserRecoverySecretStatus.EXPIRED.name().equals(userRecoverySecret.getRecoverySecretStatus())
                || UserRecoverySecretStatus.VERIFIED.name().equals(userRecoverySecret.getRecoverySecretStatus())
                || UserRecoverySecretStatus.INVALIDATED.name().equals(userRecoverySecret.getRecoverySecretStatus())) {
            LOGGER.info("recovery secret is expired for the userid {}", userRecoverySecret.getUserId());
            throw new RecoverySecretExpireException("recover secret is expired!");
        }
    }

    /**
     * Method to send password recovery url to user via notification center.
     *
     * @param user                 Username of the user.
     * @throws ResourceNotFoundException throw exception when user details not
     *                                   found.
     * @throws MalformedURLException     throw exception if recovery url is
     *                                   incorrect.
     */
    @Override
    public void sendUserRecoveryNotification(String user, boolean isUserId)
            throws ResourceNotFoundException, MalformedURLException {
        UserEntity userEntity = null;
        if (!isUserId) {
            userEntity = userRepository.findByUserNameIgnoreCaseAndStatusNot(user, UserStatus.DELETED);
        } else {
            userEntity = userRepository.findByIdAndStatusNot(new BigInteger(user), UserStatus.DELETED);
        }
        if (Objects.isNull(userEntity)) {
            throw new ResourceNotFoundException(USER, USER_ID_VARIABLE, String.valueOf(user));
        }
        LOGGER.debug("generating recovery secret for userId {} ", userEntity.getId());
        //Invalidate the older recovery secrets.
        userRecoverySecretRepository.invalidateOldRecoverySecret(userEntity.getId());
        
        UserRecoverySecret userRecoverySecret = new UserRecoverySecret();
        String recoverySecret = UUID.randomUUID().toString();
        userRecoverySecret.setRecoverySecret(recoverySecret);
        userRecoverySecret.setUserId(userEntity.getId());
        userRecoverySecretRepository.save(userRecoverySecret);

        String params = ApiConstants.USER_PASSWORD_RECOVERY_SECRET + recoverySecret;
        String encodedParams = Base64.getEncoder().encodeToString(params.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> notificationData = new HashMap<>();
        URL changePasswordUrl = new URL(applicationProperties.getAuthServerResetResponseUrl() + encodedParams);
        notificationData.put("changePasswordUrl", changePasswordUrl.toString());
        String email = userEntity.getEmail();
        notificationData.put("email", email);
        Map<String, String> userDetailsMap = new HashMap<>();
        userDetailsMap.put(ApiConstants.EMAIL_ADDRESS, email);
        String name = "";
        if (StringUtils.isNotEmpty(userEntity.getFirstName())) {
            name = userEntity.getFirstName();
        }
        if (StringUtils.isNotEmpty(userEntity.getLastName())) {
            name = name + " " + userEntity.getLastName();
        }
        userDetailsMap.put(ApiConstants.EMAIL_TO_NAME, name.trim());

        if (StringUtils.isNotEmpty(name)) {
            notificationData.put(ApiConstants.EMAIL_TO_NAME, name.trim());
        }
        emailNotificationService.sendNotification(userDetailsMap,
                applicationProperties.getPasswordRecoveryNotificationId(), notificationData);
    }

    /**
     * Method to get client scope and validate it.
     *
     * @param client clientId
     * @return boolean true/false if client has ManageUsers Scope
     */
    public boolean isClientAllowedToManageUsers(String client) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Check for client {}, scope of {}", client, MANAGE_USERS_SCOPE);
        }

        Optional<RegisteredClientDetails> clientDetails = Optional.empty();
        try {
            clientDetails = clientRegistrationService.getRegisteredClient(client, ClientStatus.APPROVED.getValue());
        } catch (ClientRegistrationException ex) {
            LOGGER.warn("Client details are not present, client {} is not allowed to create users", client);
            return false;
        }
        Set<String> scopes = clientDetails.isPresent() && clientDetails.get() != null ? clientDetails.get().getScopes()
            : Set.of();
        if (scopes.isEmpty()) {
            LOGGER.warn("No scopes present for client, client {} is not allowed to create users", client);
            return false;
        }
        if (!scopes.contains(MANAGE_USERS_SCOPE)) {
            LOGGER.warn("Client {} tried to manage users without being mapped to {} scope", client, MANAGE_USERS_SCOPE);
            return false;
        }
        return true;
    }

    /**
     * Method contain map of user statuses.
     */
    private final Map<UserStatus, UserStatus> userApprovedStatusTransition = java.util.Map.of(UserStatus.PENDING,
        UserStatus.ACTIVE, UserStatus.REJECTED, UserStatus.REJECTED, UserStatus.ACTIVE, UserStatus.ACTIVE,
        UserStatus.DEACTIVATED, UserStatus.ACTIVE);

    /**
     * Method contain map of user statuses.
     */
    private final Map<UserStatus, UserStatus> userDeclineStatusTransition = java.util.Map.of(UserStatus.PENDING,
        UserStatus.REJECTED, UserStatus.REJECTED, UserStatus.REJECTED, UserStatus.ACTIVE, UserStatus.DEACTIVATED,
        UserStatus.DEACTIVATED, UserStatus.DEACTIVATED);

    /**
     * Method to decide new status of user.
     *
     * @param currentStatus current status of user
     * @param approved      boolean flag which will decide the new user status
     * @return UserStatus
     */
    private UserStatus decideNewStatus(UserStatus currentStatus, boolean approved) {
        return approved ? userApprovedStatusTransition.get(currentStatus)
            : userDeclineStatusTransition.get(currentStatus);
    }

    /**
     * Method to update user status.
     *
     * @param userChangeStatusRequest userChangeStatusRequest
     * @param loggedInUserId          The ID of the logged-in user
     * @return List of UserResponse
     */
    @Override
    public List<UserResponseV1> changeUserStatus(UserChangeStatusRequest userChangeStatusRequest,
                                                 BigInteger loggedInUserId) {
        if (Objects.isNull(userChangeStatusRequest) || CollectionUtils.isEmpty(userChangeStatusRequest.getIds())) {
            LOGGER.debug("Either userChangeStatusRequest is null or Ids are empty, userChangeStatusRequest :{}",
                userChangeStatusRequest);
            return Collections.emptyList();
        }

        List<UserEntity> users = userRepository.findAllByIdInAndStatusNot(userChangeStatusRequest.getIds(),
            UserStatus.DELETED);
        LOGGER.debug("List of user entity has {} users", users.size());
        List<UserResponseV1> userResponses = new ArrayList<>();

        if (!CollectionUtils.isEmpty(users)) {
            users.stream().forEach(user -> {
                UserStatus currentStatus = user.getStatus();
                UserStatus newStatus = decideNewStatus(currentStatus, userChangeStatusRequest.isApproved());
                LOGGER.debug("For user id :{}, status will update to {} from {}", user.getId(), newStatus,
                    currentStatus);

                if (BooleanUtils.isTrue(applicationProperties.getIsUserStatusLifeCycleEnabled())
                    && !userChangeStatusRequest.isApproved() && !currentStatus.equals(newStatus)) {
                    LOGGER.debug("Revoke user token process initiated for user :{}", user.getUserName());
                    revokeUserTokens(user.getUserName());
                }
                user.setStatus(newStatus);
                user.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
                user.setUpdatedBy(String.valueOf(loggedInUserId));
                Set<BigInteger> roleIds = user.getAccountRoleMapping().stream()
                    .map(UserAccountRoleMappingEntity::getRoleId).collect(Collectors.toSet());
                RoleListRepresentation roleListDto = rolesService.getRoleById(roleIds);
                UserEntity savedUser = userRepository.save(user);
                UserResponseV1 userResponse = (UserResponseV1) UserMapper.USER_MAPPER.mapToUserResponseV1(savedUser);
                Map<BigInteger, Map<String, Object>> additionalAttributes = findAdditionalAttributeData(
                    List.of(user.getId()));
                if (!ObjectUtils.isEmpty(additionalAttributes)) {
                    userResponse.setAdditionalAttributes(additionalAttributes.get(user.getId()));
                }
                userResponse.setRoles(RoleManagementUtils.getRoleNamesFromRolesDto(roleListDto));
                userResponses.add(userResponse);
            });
        }

        return userResponses;
    }

    /**
     * Method to revoke token for given username.
     *
     * @param username provided to revoke token
     */
    private void revokeUserTokens(String username) {
        LOGGER.debug("revokeUserTokens START");
        String requestBody = "username=" + username;

        if (!requestBody.matches(USERNAME_REGEXP)) {
            LOGGER.error("User Name format is invalid '{}'!", requestBody);
            throw new ApplicationRuntimeException(INVALID_INPUT_USERNAME_PATTERN, BAD_REQUEST);
        }

        String uidamAuthToken = cacheTokenService.getAccessToken();
        if (StringUtils.isNotEmpty(uidamAuthToken)) {
            BaseResponseFromAuthorization response = authorizationServerClient.revokeTokenByAdmin(uidamAuthToken,
                username);

            if (response.getHttpStatus().equals(OK)) {
                LOGGER.info("For username :{}, Revoke token process update :{}", username, response.getMessage());
            } else {
                LOGGER.error("Unable to revoke token for user :{}", username);
                throw new ApplicationRuntimeException("Token has not been revoked", response.getHttpStatus());
            }
        } else {
            LOGGER.error("UIDAM auth token is inappropriate for user :{}", username);
            throw new ApplicationRuntimeException("Invalid UIDAM auth token", BAD_REQUEST);
        }
        LOGGER.debug("revokeUserTokens END");
    }

    /**
     * Method to validate the external user's roles.
     *
     * @param roles external user's roles
     */
    private void validateRolePermitted(Set<String> roles) {
        List<String> permittedRoles = Arrays.stream(applicationProperties.getExternalUserPermittedRoles().split(","))
            .map(String::trim).toList();
        for (String role : roles) {
            if (!permittedRoles.contains(role)) {
                LOGGER.error("Role {} is not allowed for external user", role);
                throw new ApplicationRuntimeException(INVALID_INPUT_ROLE, BAD_REQUEST, ApiConstants.ROLES);
            }
        }
        validateRoles(roles);
    }

    /**
     * Method to validate isExternalUser field.
     *
     * @param isExternalUser True for external user
     */
    private void validateIsExternalUser(Boolean isExternalUser) {
        if (isExternalUser == null || Boolean.FALSE.equals(isExternalUser)) {
            LOGGER.error("isExternalUser field should be true for external user");
            throw new ApplicationRuntimeException(INVALID_EXTERNAL_USER, BAD_REQUEST);
        }
    }

    /**
     * Method to create external user profile in user management.
     *
     * @param externalUserDto input external user details provided by user.
     * @param loggedInUserId  user Id for the user creating a user profile.
     * @return userResponse.
     * @throws ResourceNotFoundException throws exception if user details not found
     *                                   for the logged-in user id.
     */
    @Transactional
    @Override
    public UserResponseBase addExternalUser(UserDtoBase externalUserDto, BigInteger loggedInUserId)
        throws ResourceNotFoundException {
        LOGGER.debug("Adding an external user :{}", externalUserDto.getUserName());

        validateIsExternalUser(externalUserDto.getIsExternalUser());
        // This api is implemented only for V1 version
        validateRolePermitted(((UserDtoV1) externalUserDto).getRoles());
        validateUserPermissions(externalUserDto, loggedInUserId);

        String externalUserStatus = (StringUtils.isEmpty(applicationProperties.getExternalUserDefaultStatus()))
            ? UserStatus.ACTIVE.name()
            : applicationProperties.getExternalUserDefaultStatus();
        externalUserDto.setStatus(UserStatus.valueOf(externalUserStatus));

        RoleListRepresentation roleListDto = rolesService.filterRoles(((UserDtoV1) externalUserDto).getRoles(),
            Integer.valueOf(ApiConstants.PAGE_NUMBER_DEFAULT), Integer.valueOf(ApiConstants.PAGE_SIZE_DEFAULT),
            false);
        if (!validateRoleExists(roleListDto, ((UserDtoV1) externalUserDto).getRoles())) {
            throw new ApplicationRuntimeException(USER_ROLES_NOT_FOUND, BAD_REQUEST);
        }
        final List<UserAttributeEntity> userAttributeEntities =
            validateMissingMandatoryAttributes(externalUserDto.getAdditionalAttributes());

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(externalUserDto);
        userEntity.setAccountRoleMapping(mapToAccountsAndRoles(externalUserDto, loggedInUserId));
        userEntity.getUserAddresses().forEach(userAddressEntity -> userAddressEntity.setUserEntity(userEntity));

        UserEntity savedUser = userRepository.save(userEntity);
        // UserAccountRoleMapping would not have got the new userId now.
        // Set it explicitly for each of them.
        BigInteger savedUserId = savedUser.getId();
        LOGGER.debug(SAVED_USER_ID_MESSAGE, savedUserId);
        savedUser.getAccountRoleMapping().forEach(urm -> {
            LOGGER.debug(ACCOUNT_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE, urm.getAccountId());
            LOGGER.debug(ROLE_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE, urm.getRoleId());
            urm.setUserId(savedUserId);
        });

        // Hardcoding VERSION_1 here because this api is only for v1
        UserResponseBase userResponseBase = addRoleNamesAndMapToUserResponse(userEntity, VERSION_1);
        if (!ObjectUtils.isEmpty(externalUserDto.getAdditionalAttributes()) && isValidAdditionalAttributes(
            externalUserDto.getAdditionalAttributes(), userAttributeEntities, true)) {
            userResponseBase.setAdditionalAttributes(
                persistAdditionalAttributes(externalUserDto, savedUser).get(savedUser.getId()));
        }
        LOGGER.debug("##Add external user end for user :{}", userResponseBase.getUserName());
        return userResponseBase;
    }

    /**
     * Method to associate user to account and roles.
     *
     * @param loggedInUserId     loggedInUserId
     * @param associationRequest request payload
     * @param userId             userId to which the account and roles are
     *                           associated
     * @return associateAccountAndRolesResponse
     * @throws UserAccountRoleMappingException throws exception for invalid payload
     *                                         and when associations fails
     * @throws ResourceNotFoundException ResourceNotFound for loggedInUserId
     */
    @Override
    @Transactional
    public AssociateAccountAndRolesResponse associateUserToAccountAndRoles(BigInteger loggedInUserId,
                                                                           List<AssociateAccountAndRolesDto>
                                                                               associationRequest, BigInteger userId)
        throws UserAccountRoleMappingException, ResourceNotFoundException {
        LOGGER.debug("Processing the UserAccountRoleAssociation request for the user: {}", userId);
        UserEntity userEntity = userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED);
        if (Objects.isNull(userEntity)) {
            LOGGER.error("User Id is not found {}", userId);
            throw new UserAccountRoleMappingException(USER_ID_NOT_PRESENT_IN_THE_DB,
                formatErrorMessage(USER_ID_NOT_FOUND_MESSAGE, userId),
                Arrays.asList(new ErrorProperty("user_id", List.of(userId.toString()))), BAD_REQUEST);
        }

        Map<String, BigInteger> roleNameAndIdMap = new HashMap<>();

        validateAllInputsForAssociation(loggedInUserId, associationRequest, userId, roleNameAndIdMap);

        List<UserAccountRoleMappingEntity> acRoleMaps = prepareAccountRoleMappings(loggedInUserId,
            associationRequest, userEntity, roleNameAndIdMap);
        userEntity.getAccountRoleMapping().clear();
        userEntity.getAccountRoleMapping().addAll(acRoleMaps);
        userRepository.save(userEntity);
        return createAssociateAccountAndRolesResponse(userEntity);
    }

    /**
     * This method validates the input and then returns the final list of user account role mappings
     * to be applied on to the user.
     *
     * @param loggedInUserId loggedInUserId
     * @param userEntity userEntity
     * @param associationRequest associationRequest
     * @return List of UserAccountRoleMappingEntity
     * @throws UserAccountRoleMappingException UserAccountRoleMappingException
     * @throws ResourceNotFoundException ResourceNotFoundException
     */
    protected List<UserAccountRoleMappingEntity> patchAccountRoleMappings(BigInteger loggedInUserId,
            UserEntity userEntity, List<AssociateAccountAndRolesDto> associationRequest)
            throws UserAccountRoleMappingException, ResourceNotFoundException {
        Map<String, BigInteger> roleNameAndIdMap = new HashMap<>();
        validateAllInputsForAssociation(loggedInUserId, associationRequest, userEntity.getId(), roleNameAndIdMap);
        return prepareAccountRoleMappings(loggedInUserId, associationRequest, userEntity, roleNameAndIdMap);
    }


    private void validateAllInputsForAssociation(
            BigInteger loggedInUserId,
            List<AssociateAccountAndRolesDto> associationRequest, BigInteger userId,
            Map<String, BigInteger> roleNameAndIdMap)
            throws UserAccountRoleMappingException, ResourceNotFoundException {
        Set<BigInteger> accountIdSet = new HashSet<>();
        Set<String> roleNameSet = new HashSet<>();

        UserAccountRoleAssociationValidator.validatePayloadForAccountIdAndName(associationRequest,
            userId, accountIdSet, roleNameSet);

        associationRequest.sort(Comparator.comparingInt(request -> request.getOperationPriority().getPriority()));

        List<ErrorProperty> errorPropertyList = new ArrayList<>();
        List<RolesEntity> rolesEntityList = rolesRepository.findByNameInAndIsDeleted(roleNameSet, false);
        UserAccountRoleAssociationValidator.validateRolesFromDb(roleNameSet,
            rolesEntityList, errorPropertyList, roleNameAndIdMap);

        List<AccountEntity> accountEntityList = accountRepository.findByAccountIdInAndStatusNot(accountIdSet,
                AccountStatus.DELETED);
        UserAccountRoleAssociationValidator
            .validateAccountIdFromDb(accountIdSet, accountEntityList, errorPropertyList);

        if (!errorPropertyList.isEmpty()) {
            LOGGER.error(ASSOCIATION_REQ_ERROR_MESSAGE, INVALID_ACCOUNT_ID_AND_ROLES_ERROR_MESSGAE, errorPropertyList);
            throw new UserAccountRoleMappingException(USER_ACCOUNT_ROLE_ASSOCIATION_CODE,
                INVALID_ACCOUNT_ID_AND_ROLES_ERROR_MESSGAE, errorPropertyList, BAD_REQUEST);
        }

        //All roles are valid. Now check if the logged in user is permitted to do this operation.
        if (!roleNameSet.isEmpty()) {
            LOGGER.debug("checking user {} permission to perform association", loggedInUserId);
            if (Boolean.FALSE.equals(isUserAllowedToPerformOperation(loggedInUserId, roleNameSet))) {
                LOGGER.error("User '{}' is not allowed to perform operations on '{}' roles!", loggedInUserId,
                        roleNameSet);

                throw new UserAccountRoleMappingException(ACTION_FORBIDDEN,
                        formatErrorMessage("LoggedInUser is not allowed to perform operations on roles!"),
                        Arrays.asList(new ErrorProperty("role_names", new ArrayList<>(roleNameSet))), BAD_REQUEST);
            }
        }
    }

    private List<UserAccountRoleMappingEntity> prepareAccountRoleMappings(BigInteger loggedInUserId,
                                                                                 List<AssociateAccountAndRolesDto>
                                                                                     associationRequest,
                                                                                 UserEntity userEntity,
                                                                                 Map<String, BigInteger> roleNameMap)
        throws UserAccountRoleMappingException {
        BigInteger userId = userEntity.getId();
        Map<BigInteger, List<BigInteger>> accountIdRoleIdMap = new HashMap<>();
        Map<String, UserAccountRoleMappingEntity> accountIdRoleIdEntityMap = UserAccountRoleAssociationValidator
            .mapAccountToEntityForUser(userEntity.getAccountRoleMapping(), accountIdRoleIdMap);

        List<UserAccountRoleMappingEntity> addUserRoleAssociationList = new ArrayList<>();
        Set<BigInteger> removeUserAssociationByAccountIdList = new HashSet<>();
        Set<BigInteger> removeUserAssociationFromIdList = new HashSet<>();
        List<ErrorProperty> errorPropertyList = new ArrayList<>();
        associationRequest.forEach(associateRolesDto -> {
            BigInteger payloadAccountId = associateRolesDto.getAccountId();
            String payloadValue = associateRolesDto.getValue();
            Integer payloadPriority = associateRolesDto.getOperationPriority().getPriority();
            List<BigInteger> roleIdsOfAccount = accountIdRoleIdMap.getOrDefault(payloadAccountId, new ArrayList<>());
            BigInteger roleIdOfPayload = roleNameMap.get(payloadValue);

            if (payloadPriority.equals(OperationPriority.ONE.getPriority())
                && roleIdsOfAccount.contains(roleIdOfPayload)) {
                removeUserAssociationFromIdList
                    .add(accountIdRoleIdEntityMap.get(payloadAccountId + AMPERSAND + roleIdOfPayload).getId());
            } else if (payloadPriority.equals(OperationPriority.TWO.getPriority())
                && accountIdRoleIdMap.containsKey(associateRolesDto.getAccountId())) {
                removeUserAssociationByAccountIdList.add(associateRolesDto.getAccountId());
            } else if (payloadPriority.equals(OperationPriority.THREE.getPriority())
                && !roleIdsOfAccount.contains(roleIdOfPayload)) {
                addUserRoleAssociationList.add(new UserAccountRoleMappingEntity(roleIdOfPayload, userId,
                    payloadAccountId, loggedInUserId.toString()));
            } else {
                errorPropertyList.add(new ErrorProperty("account_role_association",
                    List.of(MessageFormat.format(ACCOUNT_AND_ROLE_ASSOCIATION_ERROR_MSG, associateRolesDto.getOp(),
                        associateRolesDto.getPath(), associateRolesDto.getValue()))));
            }
        });
        if (errorPropertyList.isEmpty()) {
            List<UserAccountRoleMappingEntity> removedAccountRoleEntityList = userEntity.getAccountRoleMapping()
                .stream().filter(entity -> removeUserAssociationFromIdList.contains(entity.getId())
                    || removeUserAssociationByAccountIdList.contains(entity.getAccountId()))
                .toList();
            if (removedAccountRoleEntityList.size() == userEntity.getAccountRoleMapping().size()
                && addUserRoleAssociationList.isEmpty()) {
                errorPropertyList
                    .add(new ErrorProperty("account_role_association", List.of(USER_DISASSOCIATE_ERROR_MSG)));
                throw new UserAccountRoleMappingException(USER_ACCOUNT_ROLE_ASSOCIATION_CODE,
                    USER_DISASSOCIATE_ERROR_MSG, errorPropertyList, BAD_REQUEST);
            }
            List<UserAccountRoleMappingEntity> acRoleMaps = new ArrayList<>(userEntity.getAccountRoleMapping());
            removedAccountRoleEntityList.stream().forEach(map ->
                LOGGER.debug("Removed entry in account role map: {}", map));
            addUserRoleAssociationList.stream().forEach(map ->
                LOGGER.debug("Added entry in account role map: {}", map));
            acRoleMaps.removeAll(removedAccountRoleEntityList);
            acRoleMaps.addAll(addUserRoleAssociationList);
            return acRoleMaps;
        } else {
            LOGGER.error(ASSOCIATION_REQ_ERROR_MESSAGE, INVALID_PAYLOAD_ERROR_MESSAGE, errorPropertyList);
            throw new UserAccountRoleMappingException(USER_ACCOUNT_ROLE_ASSOCIATION_CODE, INVALID_PAYLOAD_ERROR_MESSAGE,
                errorPropertyList, BAD_REQUEST);
        }
    }

    private AssociateAccountAndRolesResponse createAssociateAccountAndRolesResponse(UserEntity userEntity) {
        Set<BigInteger> accountIdSet = new HashSet<>();
        Set<BigInteger> roleIdSet = new HashSet<>();
        AssociateAccountAndRolesResponse associateAccountAndRolesResponse = new AssociateAccountAndRolesResponse();
        if (!CollectionUtils.isEmpty(userEntity.getAccountRoleMapping())) {
            userEntity.getAccountRoleMapping().forEach(accountRoleMappingEntity -> {
                accountIdSet.add(accountRoleMappingEntity.getAccountId());
                roleIdSet.add(accountRoleMappingEntity.getRoleId());
            });
            List<AccountEntity> accountEntityList = accountRepository.findAllById(accountIdSet);
            List<RolesEntity> rolesEntityList = rolesRepository.findByIdIn(roleIdSet);
            Map<BigInteger, String> accountIdAndNameMap = accountEntityList.stream()
                .collect(Collectors.toMap(AccountEntity::getId, AccountEntity::getAccountName));
            Map<BigInteger, String> roleIdAndNameMap = rolesEntityList.stream()
                .collect(Collectors.toMap(RolesEntity::getId, RolesEntity::getName));
            Map<String, Set<String>> accountAndRoleMap = new HashMap<>();
            userEntity.getAccountRoleMapping()
                .forEach(accountRoleMappingEntity -> accountAndRoleMap
                    .computeIfAbsent(accountIdAndNameMap.get(accountRoleMappingEntity.getAccountId()),
                        val -> new HashSet<>())
                    .add(roleIdAndNameMap.get(accountRoleMappingEntity.getRoleId())));
            Set<UserAccountsAndRoles> userAccountsAndRolesSet = new HashSet<>();
            accountAndRoleMap.entrySet().forEach(entrySet -> userAccountsAndRolesSet
                .add(new UserAccountsAndRoles(entrySet.getKey(), entrySet.getValue())));
            associateAccountAndRolesResponse.setAccounts(userAccountsAndRolesSet);
        }
        return associateAccountAndRolesResponse;
    }

    private String formatErrorMessage(String msgPattern, Object... arguments) {
        MessageFormat msgFormat = new MessageFormat(msgPattern);
        StringBuffer errorMessage = new StringBuffer();
        msgFormat.format(arguments, errorMessage, new FieldPosition(0));
        return errorMessage.toString();
    }

    /**
     * Method to create federated user profile in user management.
     *
     * @param federatedUserDto input federated user details provided by user.
     * @param loggedInUserId user Id for the user creating a user profile.
     * @return UserResponseBase.
     * @throws ResourceNotFoundException throws exception if user details not
     *                                   found for the logged-in user id.
     */
    @Transactional
    @Override
    public UserResponseBase addFederatedUser(FederatedUserDto federatedUserDto, BigInteger loggedInUserId)
        throws ResourceNotFoundException {
        LOGGER.debug("Adding a federated user :{}", federatedUserDto.getUserName());

        validateIsExternalUser(federatedUserDto.getIsExternalUser());
        validateUserPermissions(federatedUserDto, loggedInUserId);

        String generatedUserName = federatedUserDto.getIdentityProviderName() + UNDERSCORE
            + federatedUserDto.getUserName();

        //1. First verify that no other user exists with the same username
        List<UserEntity> users = userRepository.findByUserName(generatedUserName);
        if (users != null && !users.isEmpty()) {
            LOGGER.warn("Already exists user with username={}", generatedUserName);
            throw new ApplicationRuntimeException(FIELD_IS_UNIQUE, BAD_REQUEST, USERNAME);
        }

        LOGGER.debug("Updating the generated username : {}", generatedUserName);
        federatedUserDto.setUserName(generatedUserName);
        federatedUserDto.setStatus(applicationProperties.getIsUserStatusLifeCycleEnabled().booleanValue()
            ? UserStatus.PENDING : UserStatus.ACTIVE);

        validateRoles(federatedUserDto.getRoles());

        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(federatedUserDto);
        userEntity.setAccountRoleMapping(mapToAccountsAndRoles(federatedUserDto, loggedInUserId));
        //Set the identity provider name because userdto object in mapToUser method cannot access this field
        userEntity.setIdentityProviderName(federatedUserDto.getIdentityProviderName());
        userEntity.getUserAddresses().forEach(userAddressEntity ->
            userAddressEntity.setUserEntity(userEntity));

        UserEntity savedUser = userRepository.save(userEntity);
        //UserAccountRoleMapping would not have got the new userId now.
        //Set it explicitly for each of them.
        BigInteger savedUserId = savedUser.getId();
        LOGGER.debug(SAVED_USER_ID_MESSAGE, savedUserId);
        savedUser.getAccountRoleMapping().forEach(urm -> {
            LOGGER.debug(ACCOUNT_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE, urm.getAccountId());
            LOGGER.debug(ROLE_ID_IN_ACCOUNT_ROLE_MAPPING_MESSAGE, urm.getRoleId());
            urm.setUserId(savedUserId);
        });

        //Hardcoding VERSION_1 here because this api is only for v1
        UserResponseBase userResponseBase = addRoleNamesAndMapToUserResponse(userEntity, VERSION_1);

        List<UserAttributeEntity> userAttributeEntities =
                validateMissingMandatoryAttributes(federatedUserDto.getAdditionalAttributes());

        if (!ObjectUtils.isEmpty(federatedUserDto.getAdditionalAttributes())
            && isValidAdditionalAttributes(federatedUserDto.getAdditionalAttributes(), userAttributeEntities, true)) {
            userResponseBase.setAdditionalAttributes(persistAdditionalAttributes(federatedUserDto, savedUser)
                .get(savedUser.getId()));
        }
        LOGGER.debug("Add federated user end for user :{}", userResponseBase.getUserName());
        return userResponseBase;
    }

    private List<UserAttributeEntity> validateMissingMandatoryAttributes(Map<String, Object> additionalAttributes) {
        List<UserAttributeEntity> userAttributeEntities = userAttributeRepository.findAll();
        Set<String> missingMandatoryAttributes = findMissingDtoAttributes(userAttributeEntities,
            additionalAttributes.keySet());
        if (!ObjectUtils.isEmpty(missingMandatoryAttributes)) {
            throw new ApplicationRuntimeException(MISSING_MANDATORY_PARAMETERS, BAD_REQUEST,
                String.valueOf(missingMandatoryAttributes));
        }
        //returning user Attribute entities for further use
        return  userAttributeEntities;
    }

    /**
     * Method to update external user details in db.
     *
     * @param userId         userId of the user.
     * @param jsonPatch      jsonPatch containing details to be updated for a user.
     * @param loggedInUserId userId of the user trying to update the details.
     * @return UserResponseV1 with updated user details.
     * @throws ResourceNotFoundException throw exception if user not found.
     * @throws UserAccountRoleMappingException When account role mapping has some problem
     */
    @Transactional
    @Modifying
    @Override
    public UserResponseBase editExternalUser(BigInteger userId, JsonPatch jsonPatch, BigInteger loggedInUserId)
        throws ResourceNotFoundException, UserAccountRoleMappingException {
        return editUser(userId, jsonPatch, loggedInUserId, true, VERSION_1);
    }

    private UserEntity getUserEntity(BigInteger userId) throws ResourceNotFoundException {
        UserEntity user = userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED);
        if (Objects.isNull(user)) {
            throw new ResourceNotFoundException(USER, USER_ID_VARIABLE, String.valueOf(userId));
        }
        return user;
    }

    /**
     * Fetches the password policy.
     * This method retrieves the password policy settings, such as minimum length,
     * maximum length, and requirements for uppercase, lowercase, digits, and special characters.
     *
     * @return PasswordPolicyResponse containing the password policy details.
     */
    public PasswordPolicyResponse getPasswordPolicy() {
        LOGGER.debug("Creating Password Policy Response");
        PasswordPolicyResponse passwordPolicy = new PasswordPolicyResponse();
        // Set the password policy details
        passwordPolicy.setMinLength(applicationProperties.getMinPasswordLength());
        passwordPolicy.setMaxLength(applicationProperties.getMaxPasswordLength());
        passwordPolicy.setMinConsecutiveLettersLength(MIN_CONSECUTIVE_LETTERS_LENGTH);
        passwordPolicy.setPasswordRegex(PASS_REGEXP);
        return passwordPolicy;
    }
}
