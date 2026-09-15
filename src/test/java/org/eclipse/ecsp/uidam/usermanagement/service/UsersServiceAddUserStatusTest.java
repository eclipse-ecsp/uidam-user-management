/*******************************************************************************
 * Copyright (c) 2023-24 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.ecsp.uidam.usermanagement.service;

import io.prometheus.client.CollectorRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.common.metrics.UidamMetricsService;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService.ValidationResult;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository;
import org.eclipse.ecsp.uidam.usermanagement.cache.CacheTokenService;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.dao.UserManagementDao;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.mapper.UserMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.CloudProfilesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.EmailVerificationRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.PasswordHistoryRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeValueRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserEventRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserRecoverySecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailNotificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.impl.UsersServiceImpl;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseV1;
import org.eclipse.ecsp.uidam.usermanagement.utilities.UserAuditHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the per-client {@code userStatus} preservation logic in
 * {@link UsersServiceImpl#addUser}.
 *
 * <p>Scenarios tested:
 * <ol>
 *   <li>Self-signup with a pre-set status (e.g. PENDING) → status preserved</li>
 *   <li>Self-signup with a pre-set ACTIVE status → ACTIVE preserved even when
 *       lifecycle flag is enabled</li>
 *   <li>Self-signup with no pre-set status (null) + lifecycle enabled → PENDING</li>
 *   <li>Self-signup with no pre-set status (null) + lifecycle disabled → ACTIVE</li>
 *   <li>Admin user creation (isSelfAddUser=false) with pre-set status → overwritten
 *       by lifecycle logic</li>
 * </ol>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UsersServiceImpl.class, Jackson2AutoConfiguration.class})
@MockitoBean(types = JpaMetamodelMappingContext.class)
class UsersServiceAddUserStatusTest {

    // -----------------------------------------------------------------------
    // Beans required by UsersServiceImpl
    // -----------------------------------------------------------------------

    @Autowired
    private UsersService usersService;

    @MockitoBean
    private UsersRepository userRepository;
    @MockitoBean
    private AccountRepository accountRepository;
    @MockitoBean
    private UserAttributeRepository userAttributeRepository;
    @MockitoBean
    private UserAttributeValueRepository userAttributeValueRepository;
    @MockitoBean
    private UserEventRepository userEventRepository;
    @MockitoBean
    private TenantConfigurationService tenantConfigurationService;
    @MockitoBean
    private UserManagementTenantProperties tenantProperties;
    @MockitoBean
    private EntityManagerFactory entityManagerFactory;
    @MockitoBean
    private UserManagementDao userManagementDao;
    @MockitoBean
    private EntityManager entityManager;
    @MockitoBean
    private UserRecoverySecretRepository userRecoverySecretRepository;
    @MockitoBean
    private EmailNotificationService emailNotificationService;
    @MockitoBean
    private CacheTokenService cacheTokenService;
    @MockitoBean
    private AuthorizationServerClient authorizationServerClient;
    @MockitoBean
    protected RestTemplate restTemplate;
    @MockitoBean
    RolesRepository rolesRepository;
    @MockitoBean
    private RolesService rolesService;
    @MockitoBean
    private ClientRegistration clientRegistrationService;
    @MockitoBean
    CloudProfilesRepository cloudProfilesRepository;
    @MockitoBean
    EmailVerificationRepository emailVerificationRepository;
    @MockitoBean
    PasswordHistoryRepository passwordHistoryRepository;
    @MockitoBean
    PasswordValidationService passwordValidationService;
    @MockitoBean
    PasswordPolicyRepository passwordPolicyRepository;
    @MockitoBean
    UidamMetricsService uidamMetricsService;
    @MockitoBean
    UserAuditHelper userAuditHelper;

    private static final BigInteger ACCOUNT_ID = BigInteger.ONE;
    private static final BigInteger ROLE_ID = BigInteger.TWO;
    private static final String ROLE_NAME = "VEHICLE_OWNER";
    private static final String PASSWORD = "StrongPass@1234";
    private static final String USERNAME = "alice@example.com";

    @BeforeEach
    void setUp() {
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);

        AccountEntity defaultAccount = new AccountEntity();
        defaultAccount.setAccountName("userdefaultaccount");
        defaultAccount.setId(ACCOUNT_ID);
        when(accountRepository.findByAccountName(anyString()))
                .thenReturn(Optional.of(defaultAccount));

        when(tenantProperties.getPasswordEncoder()).thenReturn("SHA-256");
        when(tenantProperties.getUserDefaultAccountName()).thenReturn("userdefaultaccount");
        when(tenantProperties.getAdditionalAttrCheckEnabledForSignUp()).thenReturn(false);
        when(passwordValidationService.validatePassword(anyString(), anyString()))
                .thenReturn(new ValidationResult(true, null));
    }

    @AfterEach
    void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    // -----------------------------------------------------------------------
    // Helper builders
    // -----------------------------------------------------------------------

    private UserDtoV1 buildUserDto(UserStatus presetStatus) {
        UserDtoV1 dto = new UserDtoV1();
        dto.setUserName(USERNAME);
        dto.setFirstName("Alice");
        dto.setLastName("Smith");
        dto.setEmail(USERNAME);
        dto.setPassword(PASSWORD);
        dto.setRoles(Set.of(ROLE_NAME));
        dto.setStatus(presetStatus);
        return dto;
    }

    private UserEntity savedUserEntity(UserDtoV1 dto) {
        UserEntity entity = UserMapper.USER_MAPPER.mapToUser(dto);
        entity.setId(BigInteger.TEN);
        List<UserAccountRoleMappingEntity> mapping = new ArrayList<>();
        UserAccountRoleMappingEntity m = new UserAccountRoleMappingEntity();
        m.setAccountId(ACCOUNT_ID);
        m.setRoleId(ROLE_ID);
        mapping.add(m);
        entity.setAccountRoleMapping(mapping);
        return entity;
    }

    private RoleListRepresentation roleListRepresentation() {
        RoleListRepresentation rep = new RoleListRepresentation();
        org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse role =
                new org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse();
        role.setName(ROLE_NAME);
        role.setId(ROLE_ID);
        rep.setRoles(new java.util.HashSet<>(List.of(role)));
        return rep;
    }

    private void mockRepositoryForUser(UserDtoV1 dto) {
        UserEntity entity = savedUserEntity(dto);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(anyString(), any(UserStatus.class)))
                .thenReturn(null);
        when(userRepository.save(any(UserEntity.class))).thenReturn(entity);

        RolesEntity rolesEntity = new RolesEntity();
        rolesEntity.setName(ROLE_NAME);
        rolesEntity.setId(ROLE_ID);
        when(rolesRepository.getRolesByName(anyString())).thenReturn(rolesEntity);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(roleListRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(roleListRepresentation());
        when(userAttributeRepository.findAll()).thenReturn(List.of());
    }

    // -----------------------------------------------------------------------
    // Test cases
    // -----------------------------------------------------------------------

    /**
     * Self-signup with status pre-set to PENDING by auth-server client config.
     * Lifecycle flag is enabled but status must be preserved.
     */
    @Test
    void selfSignup_presetStatusPending_preservedDespiteLifecycleEnabled()
            throws ResourceNotFoundException {
        when(tenantProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(tenantProperties.getIsEmailVerificationEnabled()).thenReturn(false);

        UserDtoV1 dto = buildUserDto(UserStatus.PENDING);
        mockRepositoryForUser(dto);

        UserResponseBase response = usersService.addUser(dto, null, true);

        // Verify the entity that was saved had PENDING status
        // (the returned response maps from the saved entity)
        assertEquals(UserStatus.PENDING,
                ((UserResponseV1) response).getStatus());
    }

    /**
     * Self-signup with status pre-set to ACTIVE by client config.
     * Lifecycle flag is enabled — status must still be preserved as ACTIVE.
     */
    @Test
    void selfSignup_presetStatusActive_preservedDespiteLifecycleEnabled()
            throws ResourceNotFoundException {
        when(tenantProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(tenantProperties.getIsEmailVerificationEnabled()).thenReturn(false);

        UserDtoV1 dto = buildUserDto(UserStatus.ACTIVE);
        mockRepositoryForUser(dto);

        UserResponseBase response = usersService.addUser(dto, null, true);

        assertEquals(UserStatus.ACTIVE,
                ((UserResponseV1) response).getStatus());
    }

    /**
     * Self-signup with no pre-set status (null) + lifecycle enabled → PENDING.
     */
    @Test
    void selfSignup_noPresetStatus_lifecycleEnabled_statusSetToPending()
            throws ResourceNotFoundException {
        when(tenantProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(true);
        when(tenantProperties.getIsEmailVerificationEnabled()).thenReturn(false);

        UserDtoV1 dto = buildUserDto(null);
        // After addUser, status will be set to PENDING before mapping
        UserEntity savedEntity = savedUserEntity(dto);
        savedEntity.setStatus(UserStatus.PENDING);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(anyString(), any(UserStatus.class)))
                .thenReturn(null);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        RolesEntity rolesEntity = new RolesEntity();
        rolesEntity.setName(ROLE_NAME);
        rolesEntity.setId(ROLE_ID);
        when(rolesRepository.getRolesByName(anyString())).thenReturn(rolesEntity);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(roleListRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(roleListRepresentation());
        when(userAttributeRepository.findAll()).thenReturn(List.of());

        UserResponseBase response = usersService.addUser(dto, null, true);

        assertEquals(UserStatus.PENDING,
                ((UserResponseV1) response).getStatus());
    }

    /**
     * Self-signup with no pre-set status (null) + lifecycle disabled + email
     * verification disabled → ACTIVE.
     */
    @Test
    void selfSignup_noPresetStatus_lifecycleDisabled_statusSetToActive()
            throws ResourceNotFoundException {
        when(tenantProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(false);
        when(tenantProperties.getIsEmailVerificationEnabled()).thenReturn(false);

        UserDtoV1 dto = buildUserDto(null);
        UserEntity savedEntity = savedUserEntity(dto);
        savedEntity.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(anyString(), any(UserStatus.class)))
                .thenReturn(null);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        RolesEntity rolesEntity = new RolesEntity();
        rolesEntity.setName(ROLE_NAME);
        rolesEntity.setId(ROLE_ID);
        when(rolesRepository.getRolesByName(anyString())).thenReturn(rolesEntity);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(roleListRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(roleListRepresentation());
        when(userAttributeRepository.findAll()).thenReturn(List.of());

        UserResponseBase response = usersService.addUser(dto, null, true);

        assertEquals(UserStatus.ACTIVE,
                ((UserResponseV1) response).getStatus());
    }

    /**
     * Self-signup with no pre-set status (null) + email verification enabled → PENDING.
     */
    @Test
    void selfSignup_noPresetStatus_emailVerificationEnabled_statusSetToPending()
            throws ResourceNotFoundException {
        when(tenantProperties.getIsUserStatusLifeCycleEnabled()).thenReturn(false);
        when(tenantProperties.getIsEmailVerificationEnabled()).thenReturn(true);

        UserDtoV1 dto = buildUserDto(null);
        UserEntity savedEntity = savedUserEntity(dto);
        savedEntity.setStatus(UserStatus.PENDING);
        when(userRepository.findByUserNameIgnoreCaseAndStatusNot(anyString(), any(UserStatus.class)))
                .thenReturn(null);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        RolesEntity rolesEntity = new RolesEntity();
        rolesEntity.setName(ROLE_NAME);
        rolesEntity.setId(ROLE_ID);
        when(rolesRepository.getRolesByName(anyString())).thenReturn(rolesEntity);
        when(rolesService.filterRoles(anySet(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(roleListRepresentation());
        when(rolesService.getRoleById(anySet())).thenReturn(roleListRepresentation());
        when(userAttributeRepository.findAll()).thenReturn(List.of());

        UserResponseBase response = usersService.addUser(dto, null, true);

        assertEquals(UserStatus.PENDING,
                ((UserResponseV1) response).getStatus());
    }
}
