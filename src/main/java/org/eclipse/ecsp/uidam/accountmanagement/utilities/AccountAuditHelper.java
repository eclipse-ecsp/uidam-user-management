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

package org.eclipse.ecsp.uidam.accountmanagement.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.audit.enums.AuditEventResult;
import org.eclipse.ecsp.audit.logger.AuditLogger;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.audit.context.AccountTargetContext;
import org.eclipse.ecsp.uidam.audit.context.HttpRequestContext;
import org.eclipse.ecsp.uidam.audit.context.UserActorContext;
import org.eclipse.ecsp.uidam.audit.enums.AuditEventType;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for account audit logging operations.
 */
@Component
@Slf4j
public class AccountAuditHelper {

    private static final String COMPONENT_NAME = "UIDAM_USER_MANAGEMENT";

    private final AuditLogger auditLogger;
    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for AccountAuditHelper.
     *
     * @param auditLogger       the audit logger
     * @param usersRepository   the users repository
     * @param accountRepository the account repository
     */
    public AccountAuditHelper(AuditLogger auditLogger,
                             UsersRepository usersRepository,
                             AccountRepository accountRepository) {
        this.auditLogger = auditLogger;
        this.usersRepository = usersRepository;
        this.accountRepository = accountRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Log audit event for account creation.
     *
     * @param savedAccount   the created account entity
     * @param loggedInUserId the ID of the logged-in user
     */
    public void logAccountCreatedAudit(AccountEntity savedAccount, BigInteger loggedInUserId) {
        try {
            AuditEventType eventType = AuditEventType.ADMIN_ACCOUNT_CREATED;

            UserActorContext actorContext = buildAdminActorContext(loggedInUserId);
            AccountTargetContext targetContext = buildAccountTargetContext(savedAccount);
            HttpRequestContext requestContext = buildHttpRequestContext();
            String afterValue = buildAccountStateJson(savedAccount);

            auditLogger.logWithStateChange(
                eventType.getType(),
                COMPONENT_NAME,
                AuditEventResult.SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                null,
                afterValue,
                null
            );

            log.debug("Audit log created for account creation: accountId={}", savedAccount.getId());
        } catch (Exception e) {
            log.error("Failed to create audit log for account creation: accountId={}, error={}",
                savedAccount.getId(), e.getMessage(), e);
        }
    }

    /**
     * Log audit event for account update.
     *
     * @param savedAccount   the updated account entity
     * @param loggedInUserId the ID of the logged-in user
     * @param beforeValue    the JSON representation of account state before update
     */
    public void logAccountUpdatedAudit(AccountEntity savedAccount, BigInteger loggedInUserId,
                                      String beforeValue) {
        try {
            AuditEventType eventType = AuditEventType.ADMIN_ACCOUNT_UPDATED;

            UserActorContext actorContext = buildAdminActorContext(loggedInUserId);
            AccountTargetContext targetContext = buildAccountTargetContext(savedAccount);
            HttpRequestContext requestContext = buildHttpRequestContext();
            String afterValue = buildAccountStateJson(savedAccount);

            auditLogger.logWithStateChange(
                eventType.getType(),
                COMPONENT_NAME,
                AuditEventResult.SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                beforeValue,
                afterValue,
                null
            );

            log.debug("Audit log created for account update: accountId={}", savedAccount.getId());
        } catch (Exception e) {
            log.error("Failed to create audit log for account update: accountId={}, error={}",
                savedAccount.getId(), e.getMessage(), e);
        }
    }

    /**
     * Log audit event for account deletion.
     *
     * @param deletedAccount the deleted account entity
     * @param loggedInUserId the ID of the logged-in user
     * @param beforeValue    the JSON representation of account state before deletion
     */
    public void logAccountDeletedAudit(AccountEntity deletedAccount, BigInteger loggedInUserId,
                                      String beforeValue) {
        try {
            AuditEventType eventType = AuditEventType.ADMIN_ACCOUNT_DELETED;

            UserActorContext actorContext = buildAdminActorContext(loggedInUserId);
            AccountTargetContext targetContext = buildAccountTargetContext(deletedAccount);
            HttpRequestContext requestContext = buildHttpRequestContext();

            auditLogger.logWithStateChange(
                eventType.getType(),
                COMPONENT_NAME,
                AuditEventResult.SUCCESS,
                eventType.getDescription(),
                actorContext,
                targetContext,
                requestContext,
                null,
                beforeValue,
                null,
                null
            );

            log.debug("Audit log created for account deletion: accountId={}", deletedAccount.getId());
        } catch (Exception e) {
            log.error("Failed to create audit log for account deletion: accountId={}, error={}",
                deletedAccount.getId(), e.getMessage(), e);
        }
    }

    /**
     * Build UserActorContext for admin user using loggedInUserId.
     *
     * @param loggedInUserId the ID of the logged-in user
     * @return UserActorContext with user details
     */
    private UserActorContext buildAdminActorContext(BigInteger loggedInUserId) {

        if (loggedInUserId == null) {
            return UserActorContext.builder()
                .userId("SYSTEM")
                .username("SYSTEM")
                .build();
        }

        try {
            UserEntity adminUser = usersRepository.findById(loggedInUserId).orElse(null);
            if (adminUser != null) {
                return buildUserActorContext(adminUser);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch admin user details for audit: userId={}", loggedInUserId, e);
        }

        // Fallback: minimal context
        return UserActorContext.builder()
            .userId(loggedInUserId.toString())
            .username("UNKNOWN_ADMIN")
            .build();
    }

    /**
     * Build UserActorContext from UserEntity.
     *
     * @param user the user entity
     * @return UserActorContext with user details
     */
    private UserActorContext buildUserActorContext(UserEntity user) {
        return UserActorContext.builder()
            .userId(user.getId().toString())
            .username(user.getUserName())
            .build();
    }

    /**
     * Build AccountTargetContext from AccountEntity.
     *
     * @param account the account entity
     * @return AccountTargetContext with account details
     */
    private AccountTargetContext buildAccountTargetContext(AccountEntity account) {
        return AccountTargetContext.builder()
            .accountId(account.getId() != null ? account.getId().toString() : null)
            .accountName(account.getAccountName())
            .status(account.getStatus() != null ? account.getStatus().name() : null)
            .build();
    }

    /**
     * Build HttpRequestContext from current request.
     *
     * @return HttpRequestContext with request details
     */
    private HttpRequestContext buildHttpRequestContext() {
        try {
            RequestAttributes requestAttributes =
                RequestContextHolder.getRequestAttributes();

            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request =
                    ((ServletRequestAttributes) requestAttributes).getRequest();
                return HttpRequestContext.from(request);
            }
        } catch (Exception e) {
            log.debug("Failed to build request context: {}", e.getMessage());
        }

        return HttpRequestContext.builder().build();
    }

    /**
     * Build JSON representation of account state for before/after values.
     *
     * @param account the account entity
     * @return JSON string representation of account state
     */
    public String buildAccountStateJson(AccountEntity account) {
        try {
            Map<String, Object> stateMap = new LinkedHashMap<>();
            stateMap.put("id", account.getId() != null ? account.getId().toString() : null);
            stateMap.put("accountName", account.getAccountName());
            stateMap.put("status", account.getStatus() != null ? account.getStatus().name() : null);
            stateMap.put("parentId", account.getParentId() != null 
                ? account.getParentId().toString() : null);
            stateMap.put("defaultRoles", account.getDefaultRoles());
            stateMap.put("createdBy", account.getCreatedBy());
            stateMap.put("createDate", account.getCreateDate() != null 
                ? account.getCreateDate().toString() : null);
            stateMap.put("updatedBy", account.getUpdatedBy());
            stateMap.put("updateDate", account.getUpdateDate() != null 
                ? account.getUpdateDate().toString() : null);

            return objectMapper.writeValueAsString(stateMap);

        } catch (Exception e) {
            log.error("Failed to build account state JSON: {}", e.getMessage(), e);
            return null;
        }
    }
}
