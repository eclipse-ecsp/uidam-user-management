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
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolePatch;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolesCreateRequestDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages;
import org.eclipse.ecsp.uidam.usermanagement.entity.RoleScopeMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.ScopesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.PermissionDeniedException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ScopeNotExists;
import org.eclipse.ecsp.uidam.usermanagement.mapper.RoleMapper;
import org.eclipse.ecsp.uidam.usermanagement.mapper.ScopeMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID_VARIABLE;

/**
 * service class for role apis.
 */
@Service
public class RolesService {
    private static Logger logger = LoggerFactory.getLogger(RolesService.class);

    @Autowired
    RolesRepository rolesRepository;
    @Autowired
    ScopesService scopesService;

    @Autowired
    private UsersRepository userRepository;

    @Value("${uidam.auth.admin.scope:UIDAMSystem}")
    private String adminUser;

    /**
     * method for create role.
     *
     * @param rolesDto - request details
     * @param userId   - id of requester
     * @return RoleListRepresentation - response with http status code
     */
    public RoleListRepresentation createRole(RolesCreateRequestDto rolesDto, String userId, String userScopes) {
        List<ScopesEntity> scopesEntityList = scopesService.getScopesEntityList(rolesDto.getScopeNames());

        List<String> scopesList = scopesService.scopesExist(scopesEntityList, rolesDto.getScopeNames());
        if (scopesList != null && !scopesList.isEmpty()) {
            logger.error(LoggerMessages.SCOPE_NOT_EXISTS, scopesList);
            throw new ScopeNotExists(scopesList.toString());
        }

        Set<String> userScopeSet = new HashSet<>();
        for (String userScope : userScopes.split(",")) {
            userScopeSet.add(userScope.trim());
        }
        boolean isAllowed = userScopeSet.contains(adminUser) || userScopeSet.containsAll(rolesDto.getScopeNames());
        if (!isAllowed) {
            boolean isUserAllowed = isUserAllowedToPerformOperation(userId, userScopeSet);
            if (!isUserAllowed) {
                logger.error(LoggerMessages.MANAGE_ROLE_PERMISSION, userId);
                throw new PermissionDeniedException();
            }
        }

        RolesEntity roleEntity = new RolesEntity();
        roleEntity.setName(rolesDto.getName().trim());
        roleEntity.setDescription(rolesDto.getDescription().trim());
        roleEntity.setCreatedBy(userId);

        ArrayList<RoleScopeMappingEntity> list = new ArrayList<>();
        for (ScopesEntity scope : scopesEntityList) {
            list.add(new RoleScopeMappingEntity(roleEntity, scope, userId, null));
        }

        roleEntity.setRoleScopeMapping(list);
        RolesEntity roleCreateResult = null;
        boolean isRoleExists = rolesRepository.existsByNameIgnoreCaseAndIsDeleted(roleEntity.getName(), false);
        if (isRoleExists) {
            logger.error(LoggerMessages.ROLE_EXISTS, roleEntity.getName());
            throw new RecordAlreadyExistsException(ApiConstants.NAME);
        } else {
            roleCreateResult = rolesRepository.save(roleEntity);
        }
        // convert entity result to pojo
        RoleCreateResponse role = RoleMapper.MAPPER.mapToRole(roleCreateResult);

        return prepareResponse(Arrays.asList(role), LocalizationKey.SUCCESS_KEY);
    }

    /**
     * get role method.
     *
     * @param name - role to be fetched
     * @return RoleListRepresentation
     * @throws EntityNotFoundException exception thrown when role does not exists in
     *                                 system
     */
    public RoleListRepresentation getRole(String name) throws EntityNotFoundException {
        RolesEntity roleEntity = rolesRepository.getRolesByNameAndIsDeleted(name, false);

        if (roleEntity == null) {
            logger.error(LoggerMessages.ROLE_NOT_EXISTS, name);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }
        RoleCreateResponse role = RoleMapper.MAPPER.mapToRole(roleEntity);
        return prepareResponse(Arrays.asList(role), LocalizationKey.SUCCESS_KEY);
    }

    /**
     * method to get roles by passing role id.
     *
     * @param roleIds - set of role ids
     * @return - details of fetched roles
     * @throws EntityNotFoundException thrown when role not found in system
     */
    public RoleListRepresentation getRoleById(Set<BigInteger> roleIds) throws EntityNotFoundException {

        List<RolesEntity> roleEntity = rolesRepository.findByIdIn(roleIds);
        if (roleEntity == null || roleEntity.isEmpty()) {
            logger.error(LoggerMessages.ROLE_NOT_EXISTS_ID, roleIds);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }
        List<RoleCreateResponse> role = RoleMapper.MAPPER.mapToRolesList(roleEntity);
        return prepareResponse(role, LocalizationKey.SUCCESS_KEY);
    }

    private RoleListRepresentation prepareResponse(List<RoleCreateResponse> roleDtos, String message) {
        Set<RoleCreateResponse> roleSet = roleDtos.stream()
            .filter(Objects::nonNull)
            .map(role -> {
                List<ScopesEntity> scopeNames = role.getRoleScopeMapping().stream().map(scope -> scope.getScope())
                    .toList();
                List<Scope> scopes = ScopeMapper.MAPPER.mapToScope(scopeNames);
                role.setScopes(scopes);
                return role;
            })
            .collect(Collectors.toSet());

        List<ResponseMessage> responseMsgList = Collections.singletonList(new ResponseMessage(message));

        RoleListRepresentation response = new RoleListRepresentation();
        response.setRoles(roleSet);
        response.setMessages(responseMsgList);
        return response;
    }

    /**
     * get role filter for multiple roles and with pagination.
     *
     * @param roleNames  - set of role names to be fetched.
     * @param pageNumber - page number
     * @param pageSize   - records per page
     * @param deleted    - flag based fetch, if role is deleted or not.
     * @return role details
     */
    public RoleListRepresentation filterRoles(Set<String> roleNames, Integer pageNumber, Integer pageSize,
            boolean deleted) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        List<RolesEntity> rolesEntities = rolesRepository.findByNameInAndIsDeleted(roleNames, pageable, deleted);
        if (rolesEntities == null || rolesEntities.isEmpty()) {
            logger.error(LoggerMessages.ROLE_NOT_EXISTS, roleNames);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }
        List<RoleCreateResponse> roleCreateResponses = RoleMapper.MAPPER.mapToRolesList(rolesEntities);
        return prepareResponse(roleCreateResponses, LocalizationKey.SUCCESS_KEY);
    }

    /**
     * update role api.
     *
     * @param roleName  - role to be updated
     * @param rolePatch - update details
     * @param userId    - id of requester
     * @return response with http status code
     * @throws EntityNotFoundException thrown when role not found in system
     */
    @Transactional
    public RoleListRepresentation updateRole(String roleName, RolePatch rolePatch, String userId, String userScopes)
        throws EntityNotFoundException {
        RolesEntity rolesEntity = rolesRepository.getRolesByNameAndIsDeleted(roleName, false);
        if (rolesEntity == null) {
            logger.error(LoggerMessages.ROLE_NOT_EXISTS, roleName);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }
        List<ScopesEntity> scopesEntityList = scopesService.getScopesEntityList(rolePatch.getScopeNames());

        List<String> scopesList = scopesService.scopesExist(scopesEntityList, rolePatch.getScopeNames());
        if (scopesList != null && !scopesList.isEmpty()) {
            logger.error(LoggerMessages.SCOPE_NOT_EXISTS, scopesList.toString());
            throw new ScopeNotExists(scopesList.toString());
        }
        Set<String> userScopeSet = new HashSet<>();
        for (String userScope : userScopes.split(",")) {
            userScopeSet.add(userScope.trim());
        }
        boolean isAllowed = userScopeSet.contains(adminUser) || userScopeSet.containsAll(rolePatch.getScopeNames());
        if (!isAllowed) {
            boolean isUserAllowed = isUserAllowedToPerformOperation(userId, rolePatch.getScopeNames());
            if (!isUserAllowed) {
                logger.error(LoggerMessages.MANAGE_ROLE_PERMISSION, userId);
                throw new PermissionDeniedException();
            }
        }

        if (StringUtils.isNotBlank(rolePatch.getDescription())) {
            rolesEntity.setDescription(rolePatch.getDescription());
        }

        int rowCount = rolesRepository.deleteRoleScopeMapping(rolesEntity.getId());

        logger.debug(LoggerMessages.DELETE_COUNT_ROLE_SCOPE_MAPPING, rowCount, rolesEntity.getId());
        ArrayList<RoleScopeMappingEntity> list = new ArrayList<>();
        for (ScopesEntity scopes : scopesEntityList) {
            list.add(new RoleScopeMappingEntity(rolesEntity, scopes, userId, null));
        }

        rolesEntity.setRoleScopeMapping(list);
        rolesEntity.setUpdatedBy(userId);
        RolesEntity updateResult = rolesRepository.save(rolesEntity);

        RoleCreateResponse role = RoleMapper.MAPPER.mapToRole(updateResult);
        return prepareResponse(Arrays.asList(role), LocalizationKey.SUCCESS_KEY);
    }

    /**
     * This method is used to delete a role by its name.
     *
     * @param roleName   The name of the role to be deleted.
     * @param userId     The ID of the user who is performing the delete operation.
     * @param userScopes The scopes of the user who is performing the delete operation.
     * @return RoleListRepresentation The representation of the role list after the deletion.
     * @throws EntityNotFoundException If the role to be deleted does not exist.
     */
    @Transactional
    public RoleListRepresentation deleteRole(String roleName, String userId, String userScopes)
        throws EntityNotFoundException {
        logger.debug("## deleteRole for roleName {} - START", roleName);
        RolesEntity rolesEntity = rolesRepository.getRolesByNameAndIsDeleted(roleName, false);
        if (rolesEntity == null || rolesEntity.getId() == null) {
            logger.error(LoggerMessages.ROLE_NOT_EXISTS, roleName);
            throw new EntityNotFoundException(LocalizationKey.GET_ENTITY_FAILURE);
        }
        logger.debug("Checking if user {} is allowed to delete the role", userId);
        boolean isAllowed = isUserAllowed(userId, userScopes, rolesEntity);
        if (!isAllowed) {
            logger.error(LoggerMessages.MANAGE_ROLE_PERMISSION, userId);
            throw new PermissionDeniedException();
        }
        logger.debug("User is allowed to delete the role");

        logger.debug("Checking if role is mapped with any user");
        boolean isRoleMappedWithAnyUser = userRepository.existsAccountRoleMappingForStatusNot(rolesEntity.getId(),
            UserStatus.DELETED);
        if (isRoleMappedWithAnyUser) {
            logger.error(LoggerMessages.ROLE_MAPPED_WITH_USER, roleName);
            return RoleListRepresentation.error(LocalizationKey.ROLE_DELETE_FAILED_MAPPED_WITH_USER);
        }
        logger.info(LoggerMessages.ROLE_NOT_MAPPED_WITH_USER, roleName);
        rolesEntity.setDeleted(true);
        rolesEntity.setUpdatedBy(userId);
        RolesEntity updateResult = rolesRepository.save(rolesEntity);

        RoleCreateResponse role = RoleMapper.MAPPER.mapToRole(updateResult);
        logger.debug("## deleteRole - END");
        return prepareResponse(Arrays.asList(role), LocalizationKey.SUCCESS_KEY);
    }

    /**
     * This method checks if a user is allowed to perform a certain operation based on their scopes and the
     * scopes of the role.
     *
     * @param userId      The ID of the user whose permissions are being checked.
     * @param userScopes  The scopes of the user whose permissions are being checked.
     * @param rolesEntity The entity of the role whose scopes are being compared with the user's scopes.
     * @return A boolean value indicating whether the user is allowed to perform the operation. Returns true if the
     *     user is allowed, false otherwise.
     */

    private boolean isUserAllowed(String userId, String userScopes, RolesEntity rolesEntity) {
        logger.debug("## isUserAllowed - START");
        Set<String> userScopeSet = new HashSet<>();
        for (String userScope : userScopes.split(",")) {
            userScopeSet.add(userScope.trim());
        }
        Set<String> roleScopeSet = rolesEntity.getRoleScopeMapping().stream()
            .map(RoleScopeMappingEntity::getScope)
            .map(ScopesEntity::getName)
            .collect(Collectors.toSet());

        if (!userScopeSet.contains(adminUser) && !userScopeSet.containsAll(roleScopeSet)
            && !isUserAllowedToPerformOperation(userId, roleScopeSet)) {
            return false;
        }
        logger.debug("## isUserAllowed - END");
        return true;
    }

    /**
     * This method checks if a user is allowed to perform a certain operation based on their scopes.
     *
     * @param userId     The ID of the user whose permissions are being checked.
     * @param scopeNames The scopes of the operation that the user wants to perform.
     * @return A boolean value indicating whether the user is allowed to perform the operation. Returns true if the
     *     user is allowed, false otherwise.
     */
    public boolean isUserAllowedToPerformOperation(String userId, Set<String> scopeNames) {
        List<RolesEntity> userResponse = null;
        try {
            userResponse = getUser(new BigInteger(userId));
        } catch (ResourceNotFoundException ex) {
            logger.error(LoggerMessages.USERMANAGEMENT_NOT_FOUND_ERROR, userId, ex);
            return false;
        }

        Set<String> userScopes = new HashSet<>();
        for (RolesEntity resp : userResponse) {
            userScopes.addAll(resp.getRoleScopeMapping().stream().map(scope -> scope.getScope().getName())
                .collect(Collectors.toSet()));
        }
        return userScopes.contains(adminUser) || userScopes.containsAll(scopeNames);
    }

    /**
     * Method to get user details from user table.
     *
     * @param userId - UUID coming from api-gateway
     * @return List of roles
     * @throws ResourceNotFoundException - If user not found in system then throw exception
     */
    public List<RolesEntity> getUser(BigInteger userId) throws ResourceNotFoundException {
        UserEntity userEntity = userRepository.findByIdAndStatusNot(userId, UserStatus.DELETED);
        if (Objects.isNull(userEntity)) {
            throw new ResourceNotFoundException(USER, USER_ID_VARIABLE, String.valueOf(userId));
        }
        Set<BigInteger> roleIds = new HashSet<>();
        userEntity.getAccountRoleMapping().forEach(uarEntity -> roleIds.add(uarEntity.getRoleId()));
        return rolesRepository.findByIdIn(roleIds);
    }
}
