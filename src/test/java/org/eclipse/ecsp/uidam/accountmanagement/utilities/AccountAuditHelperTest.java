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

package org.eclipse.ecsp.uidam.accountmanagement.utilities;

import org.eclipse.ecsp.audit.logger.AuditLogger;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountAuditHelper.
 */
@ExtendWith(MockitoExtension.class)
class AccountAuditHelperTest {

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AccountAuditHelper accountAuditHelper;

    private AccountEntity testAccount;
    private BigInteger loggedInUserId;

    @BeforeEach
    void setUp() {
        loggedInUserId = new BigInteger("100");
        
        // Create test account with actual AccountEntity fields
        testAccount = new AccountEntity();
        testAccount.setId(new BigInteger("1"));
        testAccount.setAccountName("Test Account");
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setParentId(new BigInteger("999"));
        testAccount.setDefaultRoles(Set.of(new BigInteger("1"), new BigInteger("2")));
        testAccount.setCreatedBy("admin");
        testAccount.setCreateDate(Timestamp.from(Instant.now()));
        testAccount.setUpdatedBy("admin");
        testAccount.setUpdateDate(Timestamp.from(Instant.now()));
    }

    @Test
    void testLogAccountCreatedAudit_Success() {
        // When
        accountAuditHelper.logAccountCreatedAudit(testAccount, loggedInUserId);

        // Then - verify audit logger was called with logWithStateChange
        verify(auditLogger, times(1)).logWithStateChange(any(), any(), any(), any(), 
                                                        any(), any(), any(), any(), 
                                                        any(), any(), any());
    }

    @Test
    void testLogAccountCreatedAudit_WithMinimalAccount() {
        // Given - minimal account with only required fields
        AccountEntity minimalAccount = new AccountEntity();
        minimalAccount.setId(new BigInteger("2"));
        minimalAccount.setAccountName("Minimal Account");

        // When
        accountAuditHelper.logAccountCreatedAudit(minimalAccount, loggedInUserId);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(any(), any(), any(), any(), 
                                                        any(), any(), any(), any(), 
                                                        any(), any(), any());
    }

    @Test
    void testLogAccountCreatedAudit_WithNullLoggedInUserId() {
        // When
        accountAuditHelper.logAccountCreatedAudit(testAccount, null);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(any(), any(), any(), any(), 
                                                        any(), any(), any(), any(), 
                                                        any(), any(), any());
    }

    @Test
    void testLogAccountCreatedAudit_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Audit logger error"))
            .when(auditLogger).logWithStateChange(any(), any(), any(), any(), any(), 
                                                any(), any(), any(), any(), any(), any());

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> 
            accountAuditHelper.logAccountCreatedAudit(testAccount, loggedInUserId)
        );
    }

    @Test
    void testLogAccountUpdatedAudit_Success() {
        // Given
        String beforeValue = "{\"accountName\":\"Old Name\"}";

        // When
        accountAuditHelper.logAccountUpdatedAudit(testAccount, loggedInUserId, beforeValue);

        // Then
        verify(auditLogger, times(1))
            .logWithStateChange(any(), any(), any(), any(), any(), any(), any(), any(), 
                              eq(beforeValue), any(), any());
    }

    @Test
    void testLogAccountUpdatedAudit_WithNullBeforeValue() {
        // When
        accountAuditHelper.logAccountUpdatedAudit(testAccount, loggedInUserId, null);

        // Then
        verify(auditLogger, times(1))
            .logWithStateChange(any(), any(), any(), any(), any(), any(), any(), any(), 
                              eq(null), any(), any());
    }

    @Test
    void testLogAccountUpdatedAudit_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Audit logger error"))
            .when(auditLogger).logWithStateChange(any(), any(), any(), any(), any(), 
                                                any(), any(), any(), any(), any(), any());

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> 
            accountAuditHelper.logAccountUpdatedAudit(testAccount, loggedInUserId, "before")
        );
    }

    @Test
    void testLogAccountDeletedAudit_Success() {
        // Given
        String beforeValue = "{\"accountName\":\"Test Account\"}";

        // When
        accountAuditHelper.logAccountDeletedAudit(testAccount, loggedInUserId, beforeValue);

        // Then
        verify(auditLogger, times(1))
            .logWithStateChange(any(), any(), any(), any(), any(), any(), any(), any(), 
                              eq(beforeValue), any(), any());
    }

    @Test
    void testLogAccountDeletedAudit_WithNullLoggedInUserId() {
        // When
        accountAuditHelper.logAccountDeletedAudit(testAccount, null, "before");

        // Then
        verify(auditLogger, times(1))
            .logWithStateChange(any(), any(), any(), any(), any(), any(), any(), any(), 
                              any(), any(), any());
    }

    @Test
    void testLogAccountDeletedAudit_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Audit logger error"))
            .when(auditLogger).logWithStateChange(any(), any(), any(), any(), any(), 
                                                any(), any(), any(), any(), any(), any());

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> 
            accountAuditHelper.logAccountDeletedAudit(testAccount, loggedInUserId, "before")
        );
    }

    @Test
    void testBuildAccountStateJson_WithAllFields() {
        // When
        String json = accountAuditHelper.buildAccountStateJson(testAccount);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"id\":\"1\""));
        assertTrue(json.contains("\"accountName\":\"Test Account\""));
        assertTrue(json.contains("\"status\":\"ACTIVE\""));
        assertTrue(json.contains("\"parentId\":\"999\""));
        assertTrue(json.contains("\"defaultRoles\""));
        assertTrue(json.contains("\"createdBy\":\"admin\""));
    }

    @Test
    void testBuildAccountStateJson_WithNullFields() {
        // Given
        AccountEntity accountWithNulls = new AccountEntity();
        accountWithNulls.setAccountName("Null Fields Account");

        // When
        String json = accountAuditHelper.buildAccountStateJson(accountWithNulls);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"accountName\":\"Null Fields Account\""));
    }

    @Test
    void testBuildAccountStateJson_WithDifferentStatuses() {
        // Test with SUSPENDED status
        testAccount.setStatus(AccountStatus.SUSPENDED);
        String json = accountAuditHelper.buildAccountStateJson(testAccount);
        assertNotNull(json);
        assertTrue(json.contains("\"status\":\"SUSPENDED\""));

        // Test with BLOCKED status
        testAccount.setStatus(AccountStatus.BLOCKED);
        json = accountAuditHelper.buildAccountStateJson(testAccount);
        assertNotNull(json);
        assertTrue(json.contains("\"status\":\"BLOCKED\""));

        // Test with PENDING status
        testAccount.setStatus(AccountStatus.PENDING);
        json = accountAuditHelper.buildAccountStateJson(testAccount);
        assertNotNull(json);
        assertTrue(json.contains("\"status\":\"PENDING\""));

        // Test with DELETED status
        testAccount.setStatus(AccountStatus.DELETED);
        json = accountAuditHelper.buildAccountStateJson(testAccount);
        assertNotNull(json);
        assertTrue(json.contains("\"status\":\"DELETED\""));

        // Test with null status
        testAccount.setStatus(null);
        json = accountAuditHelper.buildAccountStateJson(testAccount);
        assertNotNull(json);
    }

    @Test
    void testBuildAccountStateJson_WithEmptyDefaultRoles() {
        // Given
        testAccount.setDefaultRoles(Set.of());

        // When
        String json = accountAuditHelper.buildAccountStateJson(testAccount);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"defaultRoles\":[]"));
    }

    @Test
    void testBuildAccountStateJson_WithNullDefaultRoles() {
        // Given
        testAccount.setDefaultRoles(null);

        // When
        String json = accountAuditHelper.buildAccountStateJson(testAccount);

        // Then
        assertNotNull(json);
    }
}
