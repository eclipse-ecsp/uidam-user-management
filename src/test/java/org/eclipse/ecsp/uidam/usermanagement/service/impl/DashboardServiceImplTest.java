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

import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.usermanagement.dashboard.response.dto.DashboardStatsResponse;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEvents;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.ScopesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAccountRoleMappingRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserEventRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DashboardServiceImpl — verifies aggregated dashboard statistics
 * including user status counts via GROUP BY, account counts, and recent activity mapping.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final long ACTIVE_USERS = 85L;
    private static final long PENDING_USERS = 10L;
    private static final long BLOCKED_USERS = 5L;
    private static final long DELETED_USERS = 3L;
    private static final long EXTERNAL_USERS = 20L;
    private static final long FEDERATED_USERS = 10L;
    private static final long TOTAL_ACCOUNTS = 8L;
    private static final long ACTIVE_ACCOUNTS = 7L;
    private static final long PENDING_ACCOUNTS = 1L;
    private static final long TOTAL_ROLES = 15L;
    private static final long TOTAL_SCOPES = 45L;
    private static final long USER_ACCOUNT_MAPPINGS = 150L;
    private static final long EVENT_USER_ID_1 = 12345L;
    private static final long EVENT_USER_ID_2 = 67890L;
    private static final int EXPECTED_EVENTS_COUNT = 2;
    private static final Instant EVENT_TIMESTAMP_1 = Instant.parse("2026-05-18T10:30:00Z");
    private static final Instant EVENT_TIMESTAMP_2 = Instant.parse("2026-05-18T10:25:00Z");

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private ScopesRepository scopesRepository;

    @Mock
    private UserAccountRoleMappingRepository userAccountRoleMappingRepository;

    @Mock
    private UserEventRepository userEventRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        TenantContext.initialize(true);
        TenantContext.setCurrentTenant("test");
    }

    @Test
    void getDashboardStats_withData_returnsAggregatedCounts() {
        // Given — repositories return status group counts and related data
        when(usersRepository.countGroupByStatus()).thenReturn(List.of(
                new Object[]{UserStatus.ACTIVE, ACTIVE_USERS},
                new Object[]{UserStatus.PENDING, PENDING_USERS},
                new Object[]{UserStatus.BLOCKED, BLOCKED_USERS},
                new Object[]{UserStatus.DELETED, DELETED_USERS}
        ));
        when(usersRepository.countByIsExternalUserAndStatusNot(true, UserStatus.DELETED)).thenReturn(EXTERNAL_USERS);
        when(usersRepository.countByIdentityProviderNameIsNotNullAndStatusNot(UserStatus.DELETED))
                .thenReturn(FEDERATED_USERS);
        when(accountRepository.countByStatusNot(AccountStatus.DELETED)).thenReturn(TOTAL_ACCOUNTS);
        when(accountRepository.countByStatus(AccountStatus.ACTIVE)).thenReturn(ACTIVE_ACCOUNTS);
        when(accountRepository.countByStatus(AccountStatus.PENDING)).thenReturn(PENDING_ACCOUNTS);
        when(rolesRepository.countByIsDeleted(false)).thenReturn(TOTAL_ROLES);
        when(scopesRepository.count()).thenReturn(TOTAL_SCOPES);
        when(userAccountRoleMappingRepository.count()).thenReturn(USER_ACCOUNT_MAPPINGS);
        when(userEventRepository.findByOrderByEventGeneratedAtDesc(any())).thenReturn(buildMockEvents());

        // When
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Then — totalUsers excludes DELETED, counts match
        assertNotNull(result);
        long expectedTotalUsers = ACTIVE_USERS + PENDING_USERS + BLOCKED_USERS;
        assertEquals(expectedTotalUsers, result.getTotalUsers());
        assertEquals(ACTIVE_USERS, result.getActiveUsers());
        assertEquals(PENDING_USERS, result.getPendingUsers());
        assertEquals(BLOCKED_USERS, result.getBlockedUsers());
        assertEquals(TOTAL_ACCOUNTS, result.getTotalAccounts());
        assertEquals(ACTIVE_ACCOUNTS, result.getActiveAccounts());
        assertEquals(PENDING_ACCOUNTS, result.getPendingAccounts());
        assertEquals(TOTAL_ROLES, result.getTotalRoles());
        assertEquals(TOTAL_SCOPES, result.getTotalScopes());
        assertEquals(EXTERNAL_USERS, result.getExternalUsers());
        assertEquals(FEDERATED_USERS, result.getFederatedUsers());
        assertEquals(USER_ACCOUNT_MAPPINGS, result.getUserAccountMappings());
        assertEquals(EXPECTED_EVENTS_COUNT, result.getRecentActivity().size());
        assertEquals("LOGIN", result.getRecentActivity().get(0).getType());
    }

    @Test
    void getDashboardStats_withZeroUsers_returnsZeroCounts() {
        // Given — all repositories return empty/zero results
        when(usersRepository.countGroupByStatus()).thenReturn(Collections.emptyList());
        when(usersRepository.countByIsExternalUserAndStatusNot(true, UserStatus.DELETED)).thenReturn(0L);
        when(usersRepository.countByIdentityProviderNameIsNotNullAndStatusNot(UserStatus.DELETED)).thenReturn(0L);
        when(accountRepository.countByStatusNot(AccountStatus.DELETED)).thenReturn(0L);
        when(accountRepository.countByStatus(AccountStatus.ACTIVE)).thenReturn(0L);
        when(accountRepository.countByStatus(AccountStatus.PENDING)).thenReturn(0L);
        when(rolesRepository.countByIsDeleted(false)).thenReturn(0L);
        when(scopesRepository.count()).thenReturn(0L);
        when(userAccountRoleMappingRepository.count()).thenReturn(0L);
        when(userEventRepository.findByOrderByEventGeneratedAtDesc(any())).thenReturn(Collections.emptyList());

        // When
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getTotalUsers());
        assertEquals(0L, result.getActiveUsers());
        assertEquals(0L, result.getPendingUsers());
        assertEquals(0L, result.getBlockedUsers());
        assertEquals(0, result.getRecentActivity().size());
    }

    @Test
    void getDashboardStats_withNullUserIdInEvent_returnsSystemAsUser() {
        // Given — an event with null userId and null timestamp
        when(usersRepository.countGroupByStatus()).thenReturn(List.<Object[]>of(
                new Object[]{UserStatus.ACTIVE, 1L}
        ));
        when(usersRepository.countByIsExternalUserAndStatusNot(true, UserStatus.DELETED)).thenReturn(0L);
        when(usersRepository.countByIdentityProviderNameIsNotNullAndStatusNot(UserStatus.DELETED)).thenReturn(0L);
        when(accountRepository.countByStatusNot(AccountStatus.DELETED)).thenReturn(0L);
        when(accountRepository.countByStatus(AccountStatus.ACTIVE)).thenReturn(0L);
        when(accountRepository.countByStatus(AccountStatus.PENDING)).thenReturn(0L);
        when(rolesRepository.countByIsDeleted(false)).thenReturn(0L);
        when(scopesRepository.count()).thenReturn(0L);
        when(userAccountRoleMappingRepository.count()).thenReturn(0L);

        UserEvents event = new UserEvents();
        event.setId(BigInteger.ONE);
        event.setUserId(null);
        event.setEventType("SYSTEM_EVENT");
        event.setEventMessage("System maintenance");
        event.setEventGeneratedAt(null);
        when(userEventRepository.findByOrderByEventGeneratedAtDesc(any())).thenReturn(List.of(event));

        // When
        DashboardStatsResponse result = dashboardService.getDashboardStats();

        // Then — null userId mapped to "system", null timestamp mapped to empty string
        assertNotNull(result);
        assertEquals(1, result.getRecentActivity().size());
        assertEquals("system", result.getRecentActivity().get(0).getUser());
        assertEquals("", result.getRecentActivity().get(0).getTimestamp());
    }

    private List<UserEvents> buildMockEvents() {
        UserEvents event1 = new UserEvents();
        event1.setId(BigInteger.ONE);
        event1.setUserId(BigInteger.valueOf(EVENT_USER_ID_1));
        event1.setEventType("LOGIN");
        event1.setEventMessage("User logged in successfully");
        event1.setEventGeneratedAt(EVENT_TIMESTAMP_1);

        UserEvents event2 = new UserEvents();
        event2.setId(BigInteger.TWO);
        event2.setUserId(BigInteger.valueOf(EVENT_USER_ID_2));
        event2.setEventType("PASSWORD_CHANGE");
        event2.setEventMessage("User changed password");
        event2.setEventGeneratedAt(EVENT_TIMESTAMP_2);

        return List.of(event1, event2);
    }
}
