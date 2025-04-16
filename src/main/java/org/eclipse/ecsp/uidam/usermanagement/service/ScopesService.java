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

import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopeDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopePatch;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages;
import org.eclipse.ecsp.uidam.usermanagement.entity.ScopesEntity;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.mapper.ScopeMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.ScopesRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ScopeListRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static java.util.Arrays.asList;

/**
 * Service class for scope apis crud operations.
 */
@Service
public class ScopesService {
    private static Logger logger = LoggerFactory.getLogger(ScopesService.class);
    @Autowired
    private ScopesRepository scopesRepository;

    public boolean validatePermissions(String scopes) {
        List<String> adminScopes = asList(scopes.split(ApiConstants.COMMA));
        return adminScopes.contains(ApiConstants.MANAGEMENT_SCOPE);
    }

    /**
     * service method to create a new scope.
     *
     * @param scopeDto - all pojo fields for create scope
     * @param userId   - id of user who is trying to create this scope.
     * @return response message
     */
    public ScopeListRepresentation addScope(ScopeDto scopeDto, String userId) {
        // convert dto to entity
        ScopesEntity scopeEntity = ScopeMapper.MAPPER.mapToScopeEntity(scopeDto);
        scopeEntity.setCreatedBy(userId);
        ScopesEntity createResult = null;
        try {
            createResult = scopesRepository.save(scopeEntity);
        } catch (DataIntegrityViolationException ex) {
            logger.error(LoggerMessages.SCOPE_EXISTS, scopeDto.getName());
            throw new RecordAlreadyExistsException(ApiConstants.NAME);
        }
        // convert entity result to pojo
        Scope scope = ScopeMapper.MAPPER.mapToScope(createResult);
        return prepareResponse(Arrays.asList(scope), LocalizationKey.SUCCESS_KEY);
    }

    private ScopeListRepresentation prepareResponse(List<Scope> scopeDtos, String message) {
        Set<Scope> scopeSet = null;
        if (scopeDtos != null && !scopeDtos.isEmpty()) {
            scopeSet = new HashSet<>();
            for (Scope scopeDto : scopeDtos) {
                scopeSet.add(scopeDto);
            }
        }

        List<ResponseMessage> responseMsgList = new ArrayList<>();
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setKey(message);
        responseMsgList.add(responseMessage);

        ScopeListRepresentation response = new ScopeListRepresentation();
        response.setScopes(scopeSet);
        response.setMessages(responseMsgList);
        return response;
    }

    /**
     * service method to get scope.
     *
     * @param scopeName - scope name to get
     * @return response with scope details
     */
    public ScopeListRepresentation getScope(String scopeName) {
        ScopesEntity scopeEntity = scopesRepository.getScopesByName(scopeName);
        if (scopeEntity == null) {
            logger.error(LoggerMessages.SCOPE_NOT_EXISTS, scopeName);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }

        Scope scope = ScopeMapper.MAPPER.mapToScope(scopeEntity);
        return prepareResponse(Arrays.asList(scope), LocalizationKey.SUCCESS_KEY);
    }

    /**
     * method checks if given scope exists in system or not.
     *
     * @param scopeEntityList  - list of scope entity
     * @param searchScopeNames - set of scope names needs to be searched
     * @return list of scope names which does not exists
     */
    public List<String> scopesExist(List<ScopesEntity> scopeEntityList, Set<String> searchScopeNames) {
        if (scopeEntityList != null && !scopeEntityList.isEmpty()) {
            List<String> scopeNameList = ScopeMapper.MAPPER.mapToScope(scopeEntityList).stream().map(Scope::getName)
                    .toList();
            return searchScopeNames.stream().filter(name -> !scopeNameList.contains(name)).toList();
        } else {
            return new ArrayList<>(searchScopeNames);
        }
    }

    public List<ScopesEntity> getScopesEntityList(Set<String> scopeNames) {
        return scopesRepository.findByNameIn(scopeNames, null);
    }

    /**
     * service method to update a scope.
     *
     * @param scopeName  - scope which needs to be updated
     * @param scopePatch - update request fields
     * @param userId     - id of requester
     * @return response with http status code
     * @throws EntityNotFoundException - if scope is not present in system
     */
    public ScopeListRepresentation updateScope(String scopeName, ScopePatch scopePatch, String userId)
            throws EntityNotFoundException {
        ScopesEntity scopeEntity = scopesRepository.getScopesByName(scopeName);
        if (scopeEntity == null) {
            logger.error(LoggerMessages.SCOPE_NOT_EXISTS, scopeName);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }
        if (scopeEntity.isPredefined()) {
            logger.error(LoggerMessages.PREDEFINED_SCOPE, scopeName);
            return ScopeListRepresentation.error(LocalizationKey.SCOPE_UPDATE_FAILED);
        }

        if (StringUtils.isNotBlank(scopePatch.getDescription())) {
            scopeEntity.setDescription(scopePatch.getDescription());
        }
        if (scopePatch.getAdministrative() != null) {
            scopeEntity.setAdministrative(scopePatch.getAdministrative());
        }

        scopeEntity.setUpdateBy(userId);
        ScopesEntity updateResult = scopesRepository.save(scopeEntity);

        Scope scope = ScopeMapper.MAPPER.mapToScope(updateResult);
        return prepareResponse(Arrays.asList(scope), LocalizationKey.SUCCESS_KEY);
    }

    /**
     * service method to delete a scope.
     *
     * @param scopeName - scope to be deleted
     * @return response with http status code
     * @throws EntityNotFoundException - if scope is not present in system
     */
    public ScopeListRepresentation deleteScope(String scopeName) throws EntityNotFoundException {
        ScopesEntity scopeEntity = scopesRepository.getScopesByName(scopeName);
        if (scopeEntity == null) {
            logger.error(LoggerMessages.SCOPE_NOT_EXISTS, scopeName);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }
        if (scopeEntity.isPredefined()) {
            logger.error(LoggerMessages.PREDEFINED_SCOPE, scopeName);
            return ScopeListRepresentation.error(LocalizationKey.SCOPE_DELETE_FAILED);
        }

        if (!scopeEntity.getMapping().isEmpty()) {
            return ScopeListRepresentation.error(LocalizationKey.SCOPE_DELETE_FAILED_MAPPED_WITH_ROLE);
        }

        scopesRepository.delete(scopeEntity);
        return prepareResponse(null, LocalizationKey.SUCCESS_KEY);
    }

    /**
     * service method to get multiple scopes.
     *
     * @param scopeNames - set of scope names
     * @param pageNumber - page number
     * @param pageSize   - number of records per page
     * @return all scope records which matches filter
     */
    public ScopeListRepresentation filterScopes(Set<String> scopeNames, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<ScopesEntity> scopesEntities = scopesRepository.findByNameIn(scopeNames, pageable);
        if (scopesEntities == null || scopesEntities.isEmpty()) {
            logger.error(LoggerMessages.SCOPE_NOT_EXISTS, scopeNames);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }

        List<Scope> scopes = ScopeMapper.MAPPER.mapToScope(scopesEntities);
        return prepareResponse(scopes, LocalizationKey.SUCCESS_KEY);
    }

}
