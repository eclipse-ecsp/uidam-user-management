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

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.eclipse.ecsp.audit.logger.AuditLogger;
import org.eclipse.ecsp.uidam.audit.context.HttpRequestContext;
import org.eclipse.ecsp.uidam.audit.context.UserActorContext;
import org.eclipse.ecsp.uidam.audit.context.UserTargetContext;
import org.eclipse.ecsp.uidam.audit.enums.AuditEventType;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.ecsp.audit.enums.AuditEventResult.SUCCESS;

/**
 * Helper utility for user audit logging operations.
 * Centralizes audit logging logic for user management actions.
 */
@Component
@RequiredArgsConstructor
public class UserAuditHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuditHelper.class);
    
    private final AuditLogger auditLogger;
    private final UsersRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Log audit event for user creation.
     */
    public void logUserCreatedAudit(UserEntity savedUser, BigInteger loggedInUserId, 
                                    boolean isSelfAddUser, Map<BigInteger, String> accountIdToNameMapping,
                                    Map<BigInteger, String> roleIdToNameMapping) {
        try {
            AuditEventType eventType = isSelfAddUser 
                ? AuditEventType.SELF_USER_REGISTERED
                : AuditEventType.ADMIN_USER_CREATED;
            
            UserActorContext actorContext;
            if (isSelfAddUser) {
                actorContext = buildUserActorContext(savedUser, accountIdToNameMapping);
            } else {
                actorContext = buildAdminActorContext(loggedInUserId, accountIdToNameMapping);
            }
            
            UserTargetContext targetContext = buildUserTargetContext(savedUser, accountIdToNameMapping);
            HttpRequestContext requestContext = buildHttpRequestContext();
            String afterValue = buildUserStateJson(savedUser, accountIdToNameMapping, roleIdToNameMapping);
            
            auditLogger.logWithStateChange(
                eventType.getType(),
                ApiConstants.COMPONENT_NAME,
                SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                null,
                afterValue,
                null
            );
            
            LOGGER.debug("Audit log created for user creation: userId={}, eventType={}", 
                savedUser.getId(), eventType.getType());
                
        } catch (Exception e) {
            LOGGER.error("Failed to create audit log for user creation: userId={}, error={}", 
                savedUser.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Log audit event for user update.
     */
    public void logUserUpdatedAudit(UserEntity savedUser, BigInteger loggedInUserId, 
                                    String beforeValue, boolean isSelfUpdate,
                                    Map<BigInteger, String> accountIdToNameMapping,
                                    Map<BigInteger, String> roleIdToNameMapping) {
        try {
            AuditEventType eventType = isSelfUpdate
                ? AuditEventType.SELF_USER_UPDATED
                : AuditEventType.ADMIN_USER_UPDATED;
            
            UserActorContext actorContext = buildAdminActorContext(loggedInUserId, accountIdToNameMapping);
            UserTargetContext targetContext = buildUserTargetContext(savedUser, accountIdToNameMapping);
            HttpRequestContext requestContext = buildHttpRequestContext();
            String afterValue = buildUserStateJson(savedUser, accountIdToNameMapping, roleIdToNameMapping);
            
            auditLogger.logWithStateChange(
                eventType.getType(),
                ApiConstants.COMPONENT_NAME,
                SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                beforeValue,
                afterValue,
                null
            );
            
            LOGGER.debug("Audit log created for user update: userId={}, eventType={}", 
                savedUser.getId(), eventType.getType());
        } catch (Exception e) {
            LOGGER.error("Failed to create audit log for user update: userId={}, error={}", 
                savedUser.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Log audit event for user deletion.
     */
    public void logUserDeletedAudit(UserEntity deletedUser, BigInteger loggedInUserId,
                                    Map<BigInteger, String> accountIdToNameMapping,
                                    Map<BigInteger, String> roleIdToNameMapping) {
        try {
            AuditEventType eventType = AuditEventType.ADMIN_USER_DELETED;
            
            UserActorContext actorContext;
            if (loggedInUserId != null) {
                actorContext = buildAdminActorContext(loggedInUserId, accountIdToNameMapping);
            } else {
                actorContext = UserActorContext.builder()
                    .userId("SYSTEM")
                    .username("SYSTEM")
                    .build();
            }
            
            UserTargetContext targetContext = buildUserTargetContext(deletedUser, accountIdToNameMapping);
            HttpRequestContext requestContext = buildHttpRequestContext();
            String beforeValue = buildUserStateJson(deletedUser, accountIdToNameMapping, roleIdToNameMapping);
            
            auditLogger.logWithStateChange(
                eventType.getType(),
                ApiConstants.COMPONENT_NAME,
                SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                beforeValue,
                null,
                null
            );
            
            LOGGER.debug("Audit log created for user deletion: userId={}, eventType={}", 
                deletedUser.getId(), eventType.getType());
        } catch (Exception e) {
            LOGGER.error("Failed to create audit log for user deletion: userId={}, error={}", 
                deletedUser.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Log audit event for user status change.
     */
    public void logUserStatusChangedAudit(UserEntity savedUser, BigInteger loggedInUserId, 
                                          UserStatus oldStatus, UserStatus newStatus,
                                          Map<BigInteger, String> accountIdToNameMapping) {
        try {
            AuditEventType eventType = AuditEventType.ADMIN_USER_STATUS_CHANGED;
            
            UserActorContext actorContext = buildAdminActorContext(loggedInUserId, accountIdToNameMapping);
            UserTargetContext targetContext = buildUserTargetContext(savedUser, accountIdToNameMapping);
            HttpRequestContext requestContext = buildHttpRequestContext();
            
            String beforeValue = "{\"status\":\"" + oldStatus.name() + "\"}";
            String afterValue = "{\"status\":\"" + newStatus.name() + "\"}";
            
            auditLogger.logWithStateChange(
                eventType.getType(),
                ApiConstants.COMPONENT_NAME,
                SUCCESS,
                eventType.getDescription() + " from " + oldStatus.name() + " to " + newStatus.name(),
                actorContext,
                targetContext,
                requestContext,
                null,
                beforeValue,
                afterValue,
                null
            );
            
            LOGGER.debug("Audit log created for user status change: userId={}, {} -> {}", 
                savedUser.getId(), oldStatus, newStatus);
        } catch (Exception e) {
            LOGGER.error("Failed to create audit log for user status change: userId={}, error={}", 
                savedUser.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Log audit event for password reset completion (self-service).
     */
    public void logPasswordResetCompletedAudit(UserEntity user, Map<BigInteger, String> accountIdToNameMapping) {
        try {
            AuditEventType eventType = AuditEventType.SELF_PASSWORD_RESET_COMPLETED;
            
            UserActorContext actorContext = buildUserActorContext(user, accountIdToNameMapping);
            UserTargetContext targetContext = buildUserTargetContext(user, accountIdToNameMapping);
            HttpRequestContext requestContext = buildHttpRequestContext();
            
            String afterValue = "{\"passwordChanged\":true,\"pwdChangedtime\":\"" 
                + user.getPwdChangedtime().toString() + "\"}";
            
            auditLogger.logWithStateChange(
                eventType.getType(),
                ApiConstants.COMPONENT_NAME,
                SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                null,
                afterValue,
                null
            );
            
            LOGGER.debug("Audit log created for password reset: userId={}", user.getId());
        } catch (Exception e) {
            LOGGER.error("Failed to create audit log for password reset: userId={}, error={}", 
                user.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Log audit event for password reset request (self-service).
     */
    public void logPasswordResetRequestedAudit(UserEntity user, String recoverySecret,
                                               Map<BigInteger, String> accountIdToNameMapping) {
        try {
            AuditEventType eventType = AuditEventType.SELF_PASSWORD_RESET_REQUESTED;
            
            UserActorContext actorContext = buildUserActorContext(user, accountIdToNameMapping);
            UserTargetContext targetContext = buildUserTargetContext(user, accountIdToNameMapping);
            HttpRequestContext requestContext = buildHttpRequestContext();
            
            String afterValue = "{\"recoverySecretGenerated\":true,\"email\":\"" 
                + user.getEmail() + "\"}";
            
            auditLogger.logWithStateChange(
                eventType.getType(),
                ApiConstants.COMPONENT_NAME,
                SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                null,
                afterValue,
                null
            );
            
            LOGGER.debug("Audit log created for password reset request: userId={}", user.getId());
        } catch (Exception e) {
            LOGGER.error("Failed to create audit log for password reset request: userId={}, error={}", 
                user.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Log audit event for account-role association changes (admin operation).
     * This captures both V1 (roles only) and V2 (explicit account-role) changes.
     */
    public void logAccountRoleChangedAudit(UserEntity user, BigInteger loggedInUserId,
                                           String beforeValue, String afterValue,
                                           Map<BigInteger, String> accountIdToNameMapping) {
        try {
            AuditEventType eventType = AuditEventType.ADMIN_USER_ACCOUNT_ROLE_CHANGED;
            
            UserActorContext actorContext = buildAdminActorContext(loggedInUserId, accountIdToNameMapping);
            UserTargetContext targetContext = buildUserTargetContext(user, accountIdToNameMapping);
            HttpRequestContext requestContext = buildHttpRequestContext();
            
            auditLogger.logWithStateChange(
                eventType.getType(),
                ApiConstants.COMPONENT_NAME,
                SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                beforeValue,
                afterValue,
                null
            );
            
            LOGGER.debug("Audit log created for account-role change: userId={}, eventType={}", 
                user != null ? user.getId() : "null", eventType.getType());
        } catch (Exception e) {
            LOGGER.error("Failed to create audit log for account-role change: userId={}, error={}", 
                user != null ? user.getId() : "null", e.getMessage(), e);
        }
    }
    
    /**
     * Build JSON representation of account-role mappings for before/after comparison.
     */
    public String buildAccountRoleMappingsJson(UserEntity user, 
                                               Map<BigInteger, String> accountIdToNameMapping,
                                               Map<BigInteger, String> roleIdToNameMapping) {
        try {
            if (user == null || user.getAccountRoleMapping() == null || user.getAccountRoleMapping().isEmpty()) {
                return "{}";
            }
            
            List<Map<String, String>> mappings = new ArrayList<>();
            for (var mapping : user.getAccountRoleMapping()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("accountId", mapping.getAccountId() != null ? mapping.getAccountId().toString() : null);
                m.put("accountName", accountIdToNameMapping.get(mapping.getAccountId()));
                m.put("roleId", mapping.getRoleId() != null ? mapping.getRoleId().toString() : null);
                m.put("roleName", roleIdToNameMapping.get(mapping.getRoleId()));
                mappings.add(m);
            }
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("accountRoleMappings", mappings);
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            LOGGER.error("Failed to build account-role mappings JSON: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Build UserActorContext from UserEntity.
     */
    private UserActorContext buildUserActorContext(UserEntity user, Map<BigInteger, String> accountIdToNameMapping) {
        String accountId = null;
        String accountName = null;
        if (user.getAccountRoleMapping() != null && !user.getAccountRoleMapping().isEmpty()) {
            BigInteger accId = user.getAccountRoleMapping().get(0).getAccountId();
            accountId = accId != null ? accId.toString() : null;
            accountName = accountId != null ? accountIdToNameMapping.get(accId) : null;
        }
        
        return UserActorContext.builder()
            .userId(user.getId().toString())
            .username(user.getUserName())
            .accountId(accountId)
            .accountName(accountName)
            .build();
    }
    
    /**
     * Build UserTargetContext from UserEntity.
     */
    private UserTargetContext buildUserTargetContext(UserEntity user, Map<BigInteger, String> accountIdToNameMapping) {
        if (user == null) {
            return null;
        }
        String accountId = null;
        String accountName = null;
        if (user.getAccountRoleMapping() != null && !user.getAccountRoleMapping().isEmpty()) {
            BigInteger accId = user.getAccountRoleMapping().get(0).getAccountId();
            accountId = accId != null ? accId.toString() : null;
            accountName = accountId != null ? accountIdToNameMapping.get(accId) : null;
        }
        
        return UserTargetContext.builder()
            .userId(user.getId().toString())
            .username(user.getUserName())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .accountId(accountId)
            .accountName(accountName)
            .status(user.getStatus() != null ? user.getStatus().name() : null)
            .build();
    }
    
    /**
     * Build UserActorContext for admin user using loggedInUserId.
     * Only logs the user ID without querying the database.
     */
    private UserActorContext buildAdminActorContext(BigInteger loggedInUserId, 
                                                    Map<BigInteger, String> accountIdToNameMapping) {
        return UserActorContext.builder()
            .userId(loggedInUserId != null ? loggedInUserId.toString() : null)
            .build();
    }
    
    /**
     * Build HttpRequestContext from current request.
     */
    private HttpRequestContext buildHttpRequestContext() {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = 
                    ((ServletRequestAttributes) requestAttributes).getRequest();
                return HttpRequestContext.from(request);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to build request context: {}", e.getMessage());
        }
        
        return HttpRequestContext.builder().build();
    }
    
    /**
     * Build JSON representation of user state for before/after values.
     */
    public String buildUserStateJson(UserEntity user, Map<BigInteger, String> accountIdToNameMapping,
                                     Map<BigInteger, String> roleIdToNameMapping) {
        try {
            Map<String, Object> stateMap = new LinkedHashMap<>();
            stateMap.put("id", user.getId().toString());
            stateMap.put("userName", user.getUserName());
            stateMap.put("email", user.getEmail());
            stateMap.put("firstName", user.getFirstName());
            stateMap.put("lastName", user.getLastName());
            stateMap.put("status", user.getStatus() != null ? user.getStatus().name() : null);
            stateMap.put("enabled", user.getEnabled());
            stateMap.put("isExternalUser", user.getIsExternalUser());
            stateMap.put("accountNoLocked", user.getAccountNoLocked());
            stateMap.put("phoneNumber", user.getPhoneNumber());
            stateMap.put("gender", user.getGender() != null ? user.getGender().name() : null);
            stateMap.put("locale", user.getLocale());
            stateMap.put("identityProviderName", user.getIdentityProviderName());
            stateMap.put("createdBy", user.getCreatedBy());
            stateMap.put("createDate", user.getCreateDate() != null ? user.getCreateDate().toString() : null);
            stateMap.put("updatedBy", user.getUpdatedBy());
            stateMap.put("updateDate", user.getUpdateDate() != null ? user.getUpdateDate().toString() : null);
            
            if (user.getAccountRoleMapping() != null && !user.getAccountRoleMapping().isEmpty()) {
                List<Map<String, String>> mappings = new ArrayList<>();
                for (var mapping : user.getAccountRoleMapping()) {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("accountId", mapping.getAccountId() != null ? mapping.getAccountId().toString() : null);
                    m.put("accountName", accountIdToNameMapping.get(mapping.getAccountId()));
                    m.put("roleId", mapping.getRoleId() != null ? mapping.getRoleId().toString() : null);
                    m.put("roleName", roleIdToNameMapping.get(mapping.getRoleId()));
                    mappings.add(m);
                }
                stateMap.put("accountRoleMappings", mappings);
            }
            
            return objectMapper.writeValueAsString(stateMap);
            
        } catch (Exception e) {
            LOGGER.error("Failed to build user state JSON: {}", e.getMessage(), e);
            return null;
        }
    }
}
