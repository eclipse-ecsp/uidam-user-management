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
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopeDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopePatch;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.entity.RoleScopeMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.ScopesEntity;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.mapper.ScopeMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.ScopesRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ScopeListRepresentation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ScopesServiceTest.class)
@TestPropertySource("classpath:application-test.properties")
class ScopesServiceTest {
    private static final int PAGE_SIZE = 20;
    private static final Long TWO = 2L;

    private static final BigInteger SCOPE_ID_1 = new BigInteger("145911385530649019822702644100150");
    private static final BigInteger SCOPE_ID_2 = new BigInteger("145911385590649019822702644100150");
    private static final BigInteger SCOPE_ID_3 = new BigInteger("145911385690649019822702644100150");
    @Mock
    private ScopesRepository scopesRepository;
    @InjectMocks
    ScopesService scopeService;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void testValidatePermissionSuccess() {
        String userScopes = ApiConstants.MANAGEMENT_SCOPE + ApiConstants.COMMA + "DummyScope";
        assertTrue(scopeService.validatePermissions(userScopes));
    }

    @Test
    void testValidatePermissionFail() {
        String userScopes = "DummyScope";
        assertFalse(scopeService.validatePermissions(userScopes));
    }

    @Test
    void testAddScopeSuccess() {
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_1, "DummyScope", "dummy create scope", true, false,
                new ArrayList<>(), "dummyuser", new Date(), null, null);
        when(scopesRepository.save(Mockito.any(ScopesEntity.class))).thenReturn(scopeEntity);
        ScopeListRepresentation response = scopeService.addScope(new ScopeDto("DummyScope", "test scope", false),
                "DummyUser");
        Scope scope = ScopeMapper.MAPPER.mapToScope(scopeEntity);

        assertEquals(LocalizationKey.SUCCESS_KEY, response.getMessages().get(0).getKey());
        assertTrue(response.getScopes().contains(scope));
    }

    @Test
    void testAddScopeUniqueConstraintViolation() {
        ScopeDto scopeDto = new ScopeDto("DummyScope", "dummy create scope", true);
        ScopesEntity scopeEntity = ScopeMapper.MAPPER.mapToScopeEntity(scopeDto);
        scopeEntity.setCreatedBy("DummyUser");
        when(scopesRepository.save(scopeEntity)).thenThrow(DataIntegrityViolationException.class);

        RecordAlreadyExistsException exception = Assertions.assertThrows(RecordAlreadyExistsException.class, () -> {
            scopeService.addScope(scopeDto, "DummyUser");
        }, "");

        assertEquals(ApiConstants.NAME, exception.getMessage());
    }

    @Test
    void testGetScopeSuccess() {
        String scopeName = "ManageUsers";
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_1, "DummyScope", "dummy create scope", true, false,
                new ArrayList<>(), "dummyuser", new Date(), null, null);
        when(scopesRepository.getScopesByName(scopeName)).thenReturn(scopeEntity);

        Scope scope = ScopeMapper.MAPPER.mapToScope(scopeEntity);
        ScopeListRepresentation response = scopeService.getScope(scopeName);
        assertEquals(LocalizationKey.SUCCESS_KEY, response.getMessages().get(0).getKey());
        assertEquals(1, response.getScopes().size());
        assertTrue(response.getScopes().contains(scope));
    }

    @Test
    void testGetScopeRecordNotFound() {
        String scopeName = "ManageUsers";
        when(scopesRepository.getScopesByName(scopeName)).thenReturn(null);

        EntityNotFoundException exception = Assertions.assertThrows(EntityNotFoundException.class, () -> {
            scopeService.getScope(scopeName);
        }, "");

        assertEquals(LocalizationKey.GET_ENTITY_FAILURE, exception.getMessage());
    }

    @Test
    void testScopesExistsNullInput() {
        Set<String> searchScopes = new HashSet<>();
        searchScopes.add("DummyScope");
        List<String> scopeList = scopeService.scopesExist(null, searchScopes);
        assertEquals(1, scopeList.size());
        assertTrue(scopeList.get(0).contains(searchScopes.iterator().next()));
    }

    @Test
    void testScopesExistsOneScopeDoesNotExists() {
        Set<String> searchScopes = new HashSet<>();
        searchScopes.add("DummyScope");
        searchScopes.add("NotExistScope");
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_2, "DummyScope", "dummy create scope", true, false,
                new ArrayList<>(), "dummyuser", new Date(), null, null);
        List<ScopesEntity> scopeEntityList = new ArrayList<>();
        scopeEntityList.add(scopeEntity);

        List<String> scopeList = scopeService.scopesExist(scopeEntityList, searchScopes);
        assertEquals(1, scopeList.size());
        assertEquals("NotExistScope", scopeList.get(0));
    }

    @Test
    void testScopesExistsAllScopesExists() {
        Set<String> searchScopes = new HashSet<>();
        searchScopes.add("DummyScope");
        searchScopes.add("New Scope");
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_3, "DummyScope", "dummy create scope", true, false,
                new ArrayList<>(), "dummyuser", new Date(), null, null);
        ScopesEntity scopeEntityNew = new ScopesEntity(SCOPE_ID_2, "New Scope", "dummy create scope", true, false,
                new ArrayList<>(), "dummyuser", new Date(), null, null);
        List<ScopesEntity> scopeEntityList = new ArrayList<>();
        scopeEntityList.add(scopeEntity);
        scopeEntityList.add(scopeEntityNew);

        List<String> scopeList = scopeService.scopesExist(scopeEntityList, searchScopes);
        assertEquals(0, scopeList.size());
    }

    @Test
    void testGetScopesEntityListScopesNotPresent() {
        Set<String> searchScopes = new HashSet<>();
        searchScopes.add("DummyScope");

        when(scopesRepository.findByNameIn(searchScopes, null)).thenReturn(null);
        List<ScopesEntity> scopesEntityList = scopeService.getScopesEntityList(searchScopes);

        assertNull(scopesEntityList);
    }

    @Test
    void testGetScopesEntityListScopesPresent() {
        Set<String> searchScopes = new HashSet<>();
        searchScopes.add("DummyScope");
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_1, "DummyScope", "dummy scope description", true, false,
                new ArrayList<>(), "dummyuser", new Date(), null, null);
        List<ScopesEntity> scopeEntityList = new ArrayList<>();
        scopeEntityList.add(scopeEntity);

        when(scopesRepository.findByNameIn(searchScopes, null)).thenReturn(scopeEntityList);
        List<ScopesEntity> scopesEntityList = scopeService.getScopesEntityList(searchScopes);

        assertEquals(1, scopesEntityList.size());
        assertEquals("DummyScope", scopesEntityList.get(0).getName());
    }

    @Test
    void updateScopeEntityNotFoundTest() {
        String scopeName = "ManageUsers";
        ScopePatch scopePatch = new ScopePatch();
        when(scopesRepository.getScopesByName(scopeName)).thenReturn(null);

        EntityNotFoundException exception = Assertions.assertThrows(EntityNotFoundException.class, () -> {
            scopeService.updateScope(scopeName, scopePatch, "DummyUser");
        }, "");

        assertEquals(LocalizationKey.GET_ENTITY_FAILURE, exception.getMessage());
    }

    @Test
    void updateScopePredefinedTest() {
        String scopeName = "ManageUsers";

        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_3, scopeName, "description",
            false, true, null, "dummyUser",
                new Date(), null, null);
        when(scopesRepository.getScopesByName(scopeName)).thenReturn(scopeEntity);

        ScopeListRepresentation response = scopeService.updateScope(scopeName, new ScopePatch(), "DummyUser");
        assertEquals(LocalizationKey.SCOPE_UPDATE_FAILED, response.getMessages().get(0).getKey());
        assertNull(response.getScopes());
    }

    @Test
    void updateScopeSuccessTest() {
        String scopeName = "ManageUsers";
        ScopePatch scopePatch = new ScopePatch();
        scopePatch.setDescription("changed description");
        scopePatch.setAdministrative(true);

        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_2, scopeName, "description",
            false, false, null, "dummyUser",
                new Date(), null, null);
        when(scopesRepository.getScopesByName(scopeName)).thenReturn(scopeEntity);

        ScopesEntity scopeEntityResponse = new ScopesEntity(SCOPE_ID_1, scopeName, "changed description",
            true, false, null,
                "dummyUser", new Date(), "dummyUser", new Date());
        when(scopesRepository.save(Mockito.any(ScopesEntity.class))).thenReturn(scopeEntityResponse);

        ScopeListRepresentation response = scopeService.updateScope(scopeName, scopePatch, "DummyUser");
        assertEquals(LocalizationKey.SUCCESS_KEY, response.getMessages().get(0).getKey());
        assertEquals(1, response.getScopes().size());
        assertEquals("changed description", response.getScopes().iterator().next().getDescription());
        assertEquals("ManageUsers", response.getScopes().iterator().next().getName());
    }

    @Test
    void deleteScopeEntityNotFoundTest() {
        String scopeName = "ManageUsers";
        when(scopesRepository.getScopesByName(scopeName)).thenReturn(null);

        EntityNotFoundException exception = Assertions.assertThrows(EntityNotFoundException.class, () -> {
            scopeService.deleteScope(scopeName);
        }, "");

        assertEquals(LocalizationKey.GET_ENTITY_FAILURE, exception.getMessage());
    }

    @Test
    void deleteScopePredefinedTest() {
        String scopeName = "ManageUsers";

        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_1, scopeName, "description",
            false, true, null, "dummyUser",
                new Date(), null, null);
        when(scopesRepository.getScopesByName(scopeName)).thenReturn(scopeEntity);

        ScopeListRepresentation response = scopeService.deleteScope(scopeName);
        assertEquals(LocalizationKey.SCOPE_DELETE_FAILED, response.getMessages().get(0).getKey());
        assertNull(response.getScopes());
    }

    @Test
    void deleteScopeRoleMappedTest() {
        String scopeName = "ManageUsers";

        List<RoleScopeMappingEntity> list = new ArrayList<>();
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_1, scopeName, "description",
            false, false, list, "dummyUser",
                new Date(), null, null);
        RolesEntity roleCreateResult = new RolesEntity(SCOPE_ID_1, "dummyRole", "dummy role description",
            list, "dummyUser",
                new Date(), null, null, false);
        RoleScopeMappingEntity roleScopeMapping = new RoleScopeMappingEntity(roleCreateResult, scopeEntity,
            "dummyUser", null);
        list.add(roleScopeMapping);

        when(scopesRepository.getScopesByName(scopeName)).thenReturn(scopeEntity);

        ScopeListRepresentation response = scopeService.deleteScope(scopeName);
        assertEquals(LocalizationKey.SCOPE_DELETE_FAILED_MAPPED_WITH_ROLE, response.getMessages().get(0).getKey());
        assertNull(response.getScopes());
    }

    @Test
    void deleteScopeSuccessTest() {
        String scopeName = "ManageUsers";

        List<RoleScopeMappingEntity> list = new ArrayList<>();
        ScopesEntity scopeEntity = new ScopesEntity(SCOPE_ID_1, scopeName, "description",
            false, false, list, "dummyUser",
                new Date(), null, null);
        when(scopesRepository.getScopesByName(scopeName)).thenReturn(scopeEntity);

        ScopeListRepresentation response = scopeService.deleteScope(scopeName);
        assertEquals(LocalizationKey.SUCCESS_KEY, response.getMessages().get(0).getKey());
        assertNull(response.getScopes());
    }

    @Test
    void filterScopesTest() {
        Set<String> scopeNames = new HashSet<>();
        scopeNames.add("dummyScope1");
        scopeNames.add("dummyScope2");

        ScopesEntity scopesEntity1 = new ScopesEntity(SCOPE_ID_1, "dummyScope1",
            "dummy scope description", false, false,
                new ArrayList<>(), "dummyUser1", new Date(), null, null);
        ScopesEntity scopesEntity2 = new ScopesEntity(SCOPE_ID_1, "dummyScope2",
            "dummy scope description", false, false,
                new ArrayList<>(), "dummyUser2", new Date(), null, null);

        List<ScopesEntity> scopesEntities = new ArrayList<>();
        scopesEntities.add(scopesEntity1);
        scopesEntities.add(scopesEntity2);

        int pageNumber = 0;
        int pageSize = PAGE_SIZE;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        when(scopesRepository.findByNameIn(scopeNames, pageable)).thenReturn(scopesEntities);

        List<Scope> scopes = ScopeMapper.MAPPER.mapToScope(scopesEntities);
        ScopeListRepresentation scopeListRepresentation = scopeService.filterScopes(scopeNames, pageNumber, pageSize);

        assertEquals(LocalizationKey.SUCCESS_KEY, scopeListRepresentation.getMessages().get(0).getKey());
        assertEquals(scopes.size(), scopeListRepresentation.getScopes().size());
    }

    @Test
    void filterScopesScopeNotFoundTest() {
        Set<String> scopeNames = new HashSet<>();
        scopeNames.add("dummyScope1");
        scopeNames.add("dummyScope2");

        int pageNumber = 0;
        int pageSize = PAGE_SIZE;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        when(scopesRepository.findByNameIn(scopeNames, pageable)).thenReturn(null);

        EntityNotFoundException exception = Assertions.assertThrows(EntityNotFoundException.class, () -> {
            scopeService.filterScopes(scopeNames, pageNumber, pageSize);
        }, "");

        assertEquals(LocalizationKey.GET_ENTITY_FAILURE, exception.getMessage());
    }

}
