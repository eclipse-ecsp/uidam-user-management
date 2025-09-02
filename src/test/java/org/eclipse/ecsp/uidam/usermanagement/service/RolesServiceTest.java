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

import io.prometheus.client.CollectorRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolePatch;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolesCreateRequestDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.AuthProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.entity.RoleScopeMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.ScopesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.PermissionDeniedException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ScopeNotExists;
import org.eclipse.ecsp.uidam.usermanagement.mapper.RoleMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ACCOUNT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_ID;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = RolesServiceTest.class)
@TestPropertySource("classpath:application-test.properties")
class RolesServiceTest {

    private static final int PAGE_SIZE = 20;
    @Mock
    private RolesRepository rolesRepository;
    @Mock
    private UsersRepository userRepository;
    @Mock
    private ScopesService scopesService;
    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @InjectMocks
    private RolesService rolesService;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
        setupTenantConfiguration();
    }

    private void setupTenantConfiguration() {
        UserManagementTenantProperties tenantProperties = new UserManagementTenantProperties();
        AuthProperties authProperties = new AuthProperties();
        authProperties.setAdminScope("UIDAMSystem");
        tenantProperties.setAuth(authProperties);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
    }

    @Test
    void createRoleScopeNotPresentTest() {
        when(scopesService.scopesExist(Mockito.anyList(), Mockito.anySet()))
            .thenReturn(Arrays.asList("dummyScope"));

        Set<String> scopes = new HashSet<>();
        scopes.add("dummyScope");
        RolesCreateRequestDto request = new RolesCreateRequestDto("dummyRole", "dummy description", scopes);

        ScopeNotExists exception = assertThrows(ScopeNotExists.class, () -> {
            rolesService.createRole(request, "DummyUser", "OAuth2ClientMgmt, ManageUserRolesAndPermissions");
        }, "");

        assertEquals("[dummyScope]", exception.getMessage());
    }

    @Test
    void createRoleUserNotAllowedTest() {
        Set<String> scopes = new HashSet<>();
        scopes.add("dummyScope");
        RolePatch rolePatch = new RolePatch();
        rolePatch.setScopeNames(scopes);

        String roleName = "VEHICLE_OWNER";
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(new RolesEntity());
        when(scopesService.scopesExist(Mockito.anyList(), Mockito.anySet())).thenReturn(null);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(LOGGED_IN_USER_ID_VALUE);
        userEntity.setAccountRoleMapping(createUserAccountRoleMappingEntity(new RolesEntity()));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(rolesRepository.findByIdIn(Mockito.anySet()))
            .thenReturn(prepareDummyRoleEntityList("ManageUserRolesAndPermissions", roleName));

        RolesCreateRequestDto request = new RolesCreateRequestDto("dummyRole",
            "dummy description", scopes);

        assertThrows(PermissionDeniedException.class, () -> {
            rolesService.createRole(request, String.valueOf(USER_ID_VALUE), "OAuth2ClientMgmt, "
                + "ManageUserRolesAndPermissions");
        }, "");

    }

    @Test
    void createRoleRecordAlreadyExistsTest() {
        // Given
        RolesCreateRequestDto request = new RolesCreateRequestDto("dummyRole", "dummy description",
            new HashSet<>());
        when(rolesRepository.existsByNameIgnoreCaseAndIsDeleted(any(String.class), any(Boolean.class)))
            .thenReturn(true);

        // When & Then
        assertThrows(RecordAlreadyExistsException.class, () -> {
            rolesService.createRole(request, "DummyUser", "ManageUserRolesAndPermissions");
        });
    }

    @Test
    void createRoleSuccess() {
        Set<String> scopes = new HashSet<>();
        scopes.add("ManageUserRolesAndPermissions");

        List<RoleScopeMappingEntity> list = new ArrayList<>();
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID, "dummyScope", "dummy scope description",
            true, false, list,
            "dummyUser", new Date(), null, null);
        RolesEntity roleCreateResult = new RolesEntity(ROLE_ID_1, "dummyRole", "dummy role description",
            list, "dummyUser",
            new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMapping = new RoleScopeMappingEntity(roleCreateResult, scopeEntity,
            "dummyUser",
            null);
        list.add(roleScopeMapping);

        when(scopesService.getScopesEntityList(scopes)).thenReturn(Arrays.asList(scopeEntity));
        when(scopesService.scopesExist(Mockito.anyList(), Mockito.anySet())).thenReturn(null);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(LOGGED_IN_USER_ID_VALUE);
        userEntity.setAccountRoleMapping(createUserAccountRoleMappingEntity(roleCreateResult));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(rolesRepository.findByIdIn(Mockito.anySet()))
            .thenReturn(prepareDummyRoleEntityList("ManageUserRolesAndPermissions", "dummyRole"));
        when(rolesRepository.save(any(RolesEntity.class))).thenReturn(roleCreateResult);

        RolesCreateRequestDto request = new RolesCreateRequestDto("dummyRole", "dummy description",
            scopes);

        RoleListRepresentation response = rolesService.createRole(request, "DummyUser",
            "OAuth2ClientMgmt, ManageUserRolesAndPermissions");
        assertEquals(LocalizationKey.SUCCESS_KEY, response.getMessages().get(0).getKey());
        assertEquals(1, response.getRoles().size());
        for (RoleCreateResponse obj : response.getRoles()) {
            assertEquals("dummyRole", obj.getName());
        }

    }

    @Test
    void getRoleRoleNotFoundTest() {
        String roleName = "DummyRole";
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(null);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            rolesService.getRole(roleName);
        }, "");

        assertEquals(LocalizationKey.GET_ENTITY_FAILURE, exception.getMessage());
    }

    @Test
    void getRoleTest() {
        String roleName = "DummyRole";
        RolesEntity roleEntity = new RolesEntity(ROLE_ID_2, roleName, "dummy role description",
            new ArrayList<>(),
            "dummy user", new Date(), null, null, false);
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(roleEntity);

        RoleCreateResponse role = RoleMapper.MAPPER.mapToRole(roleEntity);
        RoleListRepresentation response = rolesService.getRole(roleName);

        assertEquals(LocalizationKey.SUCCESS_KEY, response.getMessages().get(0).getKey());
        assertEquals(1, response.getRoles().size());
        for (RoleCreateResponse obj : response.getRoles()) {
            assertEquals(role.getName(), obj.getName());
        }
    }

    @Test
    void getRoleByIdRoleNotFoundTest() {
        Set<BigInteger> roleId = new HashSet<>();
        roleId.add(ROLE_ID_2);
        when(rolesRepository.findByIdIn(roleId)).thenReturn(null);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            rolesService.getRoleById(roleId);
        }, "");

        assertEquals(LocalizationKey.GET_ENTITY_FAILURE, exception.getMessage());
    }

    @Test
    void getRoleByIdTest() {
        Set<BigInteger> roleId = new HashSet<>();
        roleId.add(ROLE_ID_2);
        RolesEntity roleEntity = new RolesEntity(ROLE_ID_2, "DummyRole", "dummy role description",
            new ArrayList<>(),
            "dummy user", new Date(), null, null, false);
        when(rolesRepository.findByIdIn(roleId)).thenReturn(Arrays.asList(roleEntity));

        RoleCreateResponse role = RoleMapper.MAPPER.mapToRole(roleEntity);
        RoleListRepresentation response = rolesService.getRoleById(roleId);

        assertEquals(LocalizationKey.SUCCESS_KEY, response.getMessages().get(0).getKey());
        assertEquals(1, response.getRoles().size());
        for (RoleCreateResponse obj : response.getRoles()) {
            assertEquals(role.getName(), obj.getName());
        }
    }

    @Test
    void filterRolesTest() {
        Set<String> roleNames = new HashSet<>();
        roleNames.add("dummyRole1");
        roleNames.add("dummyRole2");

        RolesEntity rolesEntity1 = new RolesEntity(ROLE_ID_2, "dummyRole1", "dummy role description",
            new ArrayList<>(),
            "dummyUser1", new Date(), null, null, false);
        RolesEntity rolesEntity2 = new RolesEntity(ROLE_ID_2, "dummyRole2", "dummy role description",
            new ArrayList<>(),
            "dummyUser2", new Date(), null, null, false);

        List<RolesEntity> rolesEntities = new ArrayList<>();
        rolesEntities.add(rolesEntity1);
        rolesEntities.add(rolesEntity2);

        int pageNumber = 0;
        int pageSize = PAGE_SIZE;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        when(rolesRepository.findByNameInAndIsDeleted(roleNames, pageable, false)).thenReturn(rolesEntities);

        List<RoleCreateResponse> roleCreateResponses = RoleMapper.MAPPER.mapToRolesList(rolesEntities);
        RoleListRepresentation roleListRepresentation = rolesService.filterRoles(roleNames, pageNumber, pageSize,
                false);

        assertEquals(LocalizationKey.SUCCESS_KEY, roleListRepresentation.getMessages().get(0).getKey());
        assertEquals(roleCreateResponses.size(), roleListRepresentation.getRoles().size());
    }

    @Test
    void filterRolesRoleNotFoundTest() {
        Set<String> roleNames = new HashSet<>();
        roleNames.add("dummyRole1");
        roleNames.add("dummyRole2");

        int pageNumber = 0;
        int pageSize = PAGE_SIZE;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        when(rolesRepository.findByNameInAndIsDeleted(roleNames, pageable, false)).thenReturn(null);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            rolesService.filterRoles(roleNames, pageNumber, pageSize, false);
        }, "");

        assertEquals(LocalizationKey.GET_ENTITY_FAILURE, exception.getMessage());
    }

    @Test
    void updateRoleEntityNotFoundTest() {
        String roleName = "VEHICLE_OWNER";
        RolePatch rolePatch = new RolePatch();
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(null);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            rolesService.updateRole(roleName, rolePatch, "DummyUser", "ManageUserRolesAndPermissions");
        }, "");

        assertEquals(LocalizationKey.GET_ENTITY_FAILURE, exception.getMessage());
    }

    @Test
    void updateRoleScopeNotPresentTest() {
        String roleName = "VEHICLE_OWNER";
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(new RolesEntity());
        when(scopesService.scopesExist(Mockito.anyList(), Mockito.anySet())).thenReturn(Arrays.asList("dummyScope"));

        Set<String> scopes = new HashSet<>();
        scopes.add("dummyScope");
        RolePatch rolePatch = new RolePatch();
        rolePatch.setScopeNames(scopes);

        ScopeNotExists exception = assertThrows(ScopeNotExists.class, () -> {
            rolesService.updateRole(roleName, rolePatch, "DummyUser", "ManageUserRolesAndPermissions");
        }, "");

        assertEquals("[dummyScope]", exception.getMessage());
    }

    @Test
    void updateRoleUserNotAllowedTest() {
        Set<String> scopes = new HashSet<>();
        scopes.add("dummyScope");
        RolePatch rolePatch = new RolePatch();
        rolePatch.setScopeNames(scopes);

        String roleName = "VEHICLE_OWNER";
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(new RolesEntity());
        when(scopesService.scopesExist(Mockito.anyList(), Mockito.anySet())).thenReturn(null);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID_VALUE);
        userEntity.setAccountRoleMapping(createUserAccountRoleMappingEntity(new RolesEntity()));
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(rolesRepository.findByIdIn(Mockito.anySet()))
            .thenReturn(prepareDummyRoleEntityList("ManageUserRolesAndPermissions", roleName));

        assertThrows(PermissionDeniedException.class, () -> {
            rolesService.updateRole(roleName, rolePatch, String.valueOf(USER_ID_VALUE),
                "ManageUserRolesAndPermissions");
        }, "");
    }

    private List<RolesEntity> prepareDummyRoleEntityList(String scope, String role) {
        ArrayList<RoleScopeMappingEntity> list = new ArrayList<>();
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID, scope, "dummy scope description",
            true, false, list,
            "dummyUser", new Date(), null, null);
        RolesEntity roleEntity = new RolesEntity(ROLE_ID_2, role, "dummy role description", list,
            "dummyUser",
            new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMapping = new RoleScopeMappingEntity(roleEntity, scopeEntity,
            "dummyUser",
            null);
        list.add(roleScopeMapping);
        List<RolesEntity> roleEntityList = new ArrayList<>();
        roleEntityList.add(roleEntity);
        return roleEntityList;
    }

    @Test
    void updateRoleSuccess() {
        Set<String> scopes = new HashSet<>();
        scopes.add("ManageUserRolesAndPermissions");

        RolePatch rolePatch = new RolePatch();
        rolePatch.setDescription("updated role description");
        rolePatch.setScopeNames(scopes);
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID, "ManageUserRolesAndPermissions",
            "scope description", false, false, null,
            "dummyUser", new Date(), null, null);
        String roleName = "dummyRole";
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(
            new RolesEntity(ROLE_ID_2, roleName, "role description", null,
                "dummyuser", new Date(), null, null, false));

        when(scopesService.getScopesEntityList(rolePatch.getScopeNames())).thenReturn(Arrays.asList(scopeEntity));
        when(scopesService.scopesExist(Mockito.anyList(), Mockito.anySet())).thenReturn(new ArrayList<>());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID_VALUE);

        userEntity.setAccountRoleMapping(createUserAccountRoleMappingEntity());
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(rolesRepository.findByIdIn(Mockito.anySet()))
            .thenReturn(prepareDummyRoleEntityList("ManageUserRolesAndPermissions", roleName));
        when(rolesRepository.deleteRoleScopeMapping(ROLE_ID_2)).thenReturn(1);

        List<RoleScopeMappingEntity> list = new ArrayList<>();
        RolesEntity roleUpdateResult = new RolesEntity(ROLE_ID_2, roleName, "dummy role description", list,
            "dummyUser",
            new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMapping = new RoleScopeMappingEntity(roleUpdateResult, scopeEntity,
            "dummyUser",
            null);
        list.add(roleScopeMapping);
        when(rolesRepository.save(any(RolesEntity.class))).thenReturn(roleUpdateResult);

        RoleListRepresentation response = rolesService.updateRole(roleName, rolePatch, String.valueOf(USER_ID_VALUE),
            "ManageUserRolesAndPermissions");
        assertEquals(LocalizationKey.SUCCESS_KEY, response.getMessages().get(0).getKey());
        assertEquals(1, response.getRoles().size());
        for (RoleCreateResponse obj : response.getRoles()) {
            assertEquals("dummyRole", obj.getName());
        }
    }

    @Test
    void getUserTest() throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add("VEHICLE_OWNER");

        ArrayList<RoleScopeMappingEntity> list = new ArrayList<>();
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID, "ManageUsers", "dummy scope description",
            true, false, list,
            "dummyUser", new Date(), null, null);
        RolesEntity roleEntity = new RolesEntity(ROLE_ID_2, "VEHICLE_OWNER", "dummy role description",
            list, "dummyUser",
            new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMapping = new RoleScopeMappingEntity(roleEntity, scopeEntity,
            "dummyUser",
            null);
        list.add(roleScopeMapping);
        List<RolesEntity> roleEntityList = new ArrayList<>();
        roleEntityList.add(roleEntity);

        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID_VALUE);
        userEntity.setAccountRoleMapping(createUserAccountRoleMappingEntity(roleEntity));
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(rolesRepository.findByIdIn(Mockito.anySet())).thenReturn(roleEntityList);
        List<RolesEntity> response = rolesService.getUser(USER_ID_VALUE);

        assertEquals("VEHICLE_OWNER", response.get(0).getName());
    }

    @Test
    void getUserResourceNotFoundTest() throws Exception {
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            rolesService.getUser(USER_ID_VALUE);
        }, "");
        assertEquals("User not found for userId: 145911385530649019822702644100150", exception.getMessage());
    }

    @Test
    void isUserAllowedToPerformOperationNullResponseTest() {
        Set<String> scopes = new HashSet<>();
        scopes.add("ManageUsers");
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(null);

        assertFalse(rolesService.isUserAllowedToPerformOperation(String.valueOf(USER_ID_VALUE), scopes));
    }

    @Test
    void isUserAllowedToPerformOperationAllowedTest() throws Exception {
        Set<String> scopes = new HashSet<>();
        scopes.add("ManageUsers");

        ArrayList<RoleScopeMappingEntity> list = new ArrayList<>();
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID, "ManageUsers", "dummy scope description",
            true, false, list,
            "dummyUser", new Date(), null, null);
        RolesEntity roleEntity = new RolesEntity(ROLE_ID_2, "VEHICLE_OWNER", "dummy role description",
            list, "dummyUser",
            new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMapping = new RoleScopeMappingEntity(roleEntity, scopeEntity,
            "dummyUser",
            null);
        list.add(roleScopeMapping);
        List<RolesEntity> roleEntityList = new ArrayList<>();
        roleEntityList.add(roleEntity);

        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID_VALUE);

        userEntity.setAccountRoleMapping(createUserAccountRoleMappingEntity(roleEntity));
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(rolesRepository.findByIdIn(Mockito.anySet())).thenReturn(roleEntityList);
        assertTrue(rolesService.isUserAllowedToPerformOperation(String.valueOf(USER_ID_VALUE), scopes));
    }

    @Test
    void isUserAllowedToPerformOperationNotAllowedTest() {
        Set<String> scopes = new HashSet<>();
        scopes.add("DummyScope");

        Set<String> roles = new HashSet<>();
        roles.add("VEHICLE_OWNER");

        ArrayList<RoleScopeMappingEntity> list = new ArrayList<>();
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID, "ManageUsers", "dummy scope description",
            true, false, list,
            "dummyUser", new Date(), null, null);
        RolesEntity roleEntity = new RolesEntity(ROLE_ID_2, "VEHICLE_OWNER", "dummy role description",
            list, "dummyUser",
            new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMapping = new RoleScopeMappingEntity(roleEntity, scopeEntity,
            "dummyUser",
            null);
        list.add(roleScopeMapping);
        List<RolesEntity> roleEntityList = new ArrayList<>();
        roleEntityList.add(roleEntity);

        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID_VALUE);
        userEntity.setAccountRoleMapping(createUserAccountRoleMappingEntity(roleEntity));
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);
        when(rolesRepository.findByIdIn(Mockito.anySet())).thenReturn(roleEntityList);
        assertFalse(rolesService.isUserAllowedToPerformOperation(String.valueOf(USER_ID_VALUE), scopes));
    }

    List<UserAccountRoleMappingEntity> createUserAccountRoleMappingEntity(RolesEntity role) {
        UserAccountRoleMappingEntity uar =
                new UserAccountRoleMappingEntity(role.getId(), null, ACCOUNT_ID_VALUE, "Test Admin");

        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(uar);
        return l;
    }

    List<UserAccountRoleMappingEntity> createUserAccountRoleMappingEntity() {
        UserAccountRoleMappingEntity uar =
                new UserAccountRoleMappingEntity(ROLE_ID_2, null, ACCOUNT_ID_VALUE, "Test Admin");

        List<UserAccountRoleMappingEntity> l = new ArrayList<>();
        l.add(uar);
        return l;
    }

    @Test
    void deleteRoleSuccessTest() {
        String roleName = "testRole";
        String userScopes = "testScopes";
        RoleListRepresentation roleListRepresentation = new RoleListRepresentation();
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(createRolesEntity());
        RoleListRepresentation roleListRepresentation1 = rolesService.deleteRole(roleName,
            String.valueOf(USER_ID_VALUE), userScopes);
        assertEquals("success.key", roleListRepresentation1.getMessages().get(0).getKey());
    }

    @Test
    void deleteRoleNotFoundTest() {
        String roleName = "nonExistingRole";
        String userScopes = "testScopes";
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(null);
        assertThrows(EntityNotFoundException.class, () -> rolesService.deleteRole(roleName,
            String.valueOf(USER_ID_VALUE), userScopes));
    }

    @Test
    void deleteRolePermissionDeniedTest() {
        ArrayList<String> scopesList = new ArrayList<>();
        scopesList.add("testScope1");
        scopesList.add("testScope");
        RolesEntity rolesEntity = createRolesEntity();
        rolesEntity.setRoleScopeMapping(scopesList.stream().map(scope -> {
            RoleScopeMappingEntity roleScopeMappingEntity = new RoleScopeMappingEntity();
            ScopesEntity scopesEntity = new ScopesEntity();
            scopesEntity.setName(scope);
            roleScopeMappingEntity.setScope(scopesEntity);
            roleScopeMappingEntity.setRole(rolesEntity);
            return roleScopeMappingEntity;
        }).collect(Collectors.toList()));
        String roleName = "testRole";

        String userScopes = "testScopes";
        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(rolesEntity);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
            createUserEntity());

        assertThrows(PermissionDeniedException.class, () -> rolesService.deleteRole(roleName,
            String.valueOf(USER_ID_VALUE), userScopes));
    }

    @Test
    void deleteRoleWhenUserMappedWithRoleToBeDeletedTest() {

        String roleName = "testRole";
        String userScopes = "testScopes";

        when(rolesRepository.getRolesByNameAndIsDeleted(roleName, false)).thenReturn(createRolesEntity());
        when(rolesRepository.existsByNameIgnoreCaseAndIsDeleted(roleName, false)).thenReturn(true);
        when(userRepository.existsAccountRoleMappingForStatusNot(createRolesEntity().getId(), UserStatus.DELETED))
            .thenReturn(true);

        RoleListRepresentation roleListRepresentation1 = rolesService.deleteRole(roleName,
            String.valueOf(USER_ID_VALUE), userScopes);

        assertEquals(LocalizationKey.ROLE_DELETE_FAILED_MAPPED_WITH_USER,
            roleListRepresentation1.getMessages().get(0).getKey());
    }

    private RolesEntity createRolesEntity() {
        RolesEntity rolesEntity = new RolesEntity();
        rolesEntity.setId(ROLE_ID_2);
        rolesEntity.setName("testRole");
        rolesEntity.setDescription("test description");
        rolesEntity.setCreatedBy("testUser");
        rolesEntity.setCreateDate(new Date());
        rolesEntity.setDeleted(false);
        return rolesEntity;
    }

    private UserEntity createUserEntity() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID_VALUE);
        userEntity.setAccountRoleMapping(createUserAccountRoleMappingEntity());
        return userEntity;
    }

}
