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

import lombok.AllArgsConstructor;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.usermanagement.dashboard.response.dto.DashboardStatsResponse;
import org.eclipse.ecsp.uidam.usermanagement.dashboard.response.dto.DashboardStatsResponse.RecentActivityItem;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEvents;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.ScopesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAccountRoleMappingRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserEventRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Implementation of DashboardService providing aggregated statistics.
 */
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final int RECENT_ACTIVITY_LIMIT = 10;
    private static final int IDX_TOTAL = 0;
    private static final int IDX_ACTIVE = 1;
    private static final int IDX_PENDING = 2;

    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;
    private final RolesRepository rolesRepository;
    private final ScopesRepository scopesRepository;
    private final UserAccountRoleMappingRepository userAccountRoleMappingRepository;
    private final UserEventRepository userEventRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        LOGGER.debug("Fetching dashboard statistics");

        // Capture tenant context from request thread for propagation to async tasks
        String currentTenant = TenantContext.getCurrentTenant();

        // Parallel execution of independent query groups
        CompletableFuture<Map<UserStatus, Long>> userStatusCountsFuture =
                CompletableFuture.supplyAsync(() -> withTenant(currentTenant, this::fetchUserStatusCounts));
        CompletableFuture<long[]> accountCountsFuture =
                CompletableFuture.supplyAsync(() -> withTenant(currentTenant, this::fetchAccountCounts));
        CompletableFuture<long[]> otherCountsFuture =
                CompletableFuture.supplyAsync(() -> withTenant(currentTenant, this::fetchOtherCounts));
        CompletableFuture<List<RecentActivityItem>> recentActivityFuture =
                CompletableFuture.supplyAsync(() -> withTenant(currentTenant, this::fetchRecentActivity));

        // Await all results
        Map<UserStatus, Long> statusCounts = userStatusCountsFuture.join();
        long[] accountCounts = accountCountsFuture.join();
        long[] otherCounts = otherCountsFuture.join();
        List<RecentActivityItem> recentActivity = recentActivityFuture.join();

        long activeUsers = statusCounts.getOrDefault(UserStatus.ACTIVE, 0L);
        long pendingUsers = statusCounts.getOrDefault(UserStatus.PENDING, 0L);
        long blockedUsers = statusCounts.getOrDefault(UserStatus.BLOCKED, 0L);
        long totalUsers = statusCounts.entrySet().stream()
                .filter(e -> e.getKey() != UserStatus.DELETED)
                .mapToLong(Map.Entry::getValue)
                .sum();

        long externalUsers = usersRepository.countByIsExternalUserAndStatusNot(true, UserStatus.DELETED);
        long federatedUsers = usersRepository.countByIdentityProviderNameIsNotNullAndStatusNot(UserStatus.DELETED);

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .pendingUsers(pendingUsers)
                .blockedUsers(blockedUsers)
                .totalAccounts(accountCounts[IDX_TOTAL])
                .activeAccounts(accountCounts[IDX_ACTIVE])
                .pendingAccounts(accountCounts[IDX_PENDING])
                .totalRoles(otherCounts[IDX_TOTAL])
                .totalScopes(otherCounts[IDX_ACTIVE])
                .externalUsers(externalUsers)
                .federatedUsers(federatedUsers)
                .userAccountMappings(otherCounts[IDX_PENDING])
                .recentActivity(recentActivity)
                .build();
    }

    private Map<UserStatus, Long> fetchUserStatusCounts() {
        List<Object[]> rows = usersRepository.countGroupByStatus();
        Map<UserStatus, Long> counts = new EnumMap<>(UserStatus.class);
        for (Object[] row : rows) {
            UserStatus status = (UserStatus) row[0];
            Long count = (Long) row[1];
            counts.put(status, count);
        }
        return counts;
    }

    private long[] fetchAccountCounts() {
        long totalAccounts = accountRepository.countByStatusNot(AccountStatus.DELETED);
        long activeAccounts = accountRepository.countByStatus(AccountStatus.ACTIVE);
        long pendingAccounts = accountRepository.countByStatus(AccountStatus.PENDING);
        return new long[]{totalAccounts, activeAccounts, pendingAccounts};
    }

    private long[] fetchOtherCounts() {
        long totalRoles = rolesRepository.countByIsDeleted(false);
        long totalScopes = scopesRepository.count();
        long userAccountMappings = userAccountRoleMappingRepository.count();
        return new long[]{totalRoles, totalScopes, userAccountMappings};
    }

    private List<RecentActivityItem> fetchRecentActivity() {
        List<UserEvents> events = userEventRepository
                .findByOrderByEventGeneratedAtDesc(PageRequest.of(0, RECENT_ACTIVITY_LIMIT));
        return events.stream()
                .map(event -> RecentActivityItem.builder()
                        .id(event.getId().toString())
                        .type(event.getEventType())
                        .description(event.getEventMessage())
                        .user(event.getUserId() != null ? event.getUserId().toString() : "system")
                        .timestamp(event.getEventGeneratedAt() != null
                                ? event.getEventGeneratedAt().toString() : "")
                        .build())
                .toList();
    }

    private <T> T withTenant(String tenantId, Supplier<T> supplier) {
        TenantContext.setCurrentTenant(tenantId);
        try {
            return supplier.get();
        } finally {
            TenantContext.clear();
        }
    }
}
