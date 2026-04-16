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

package org.eclipse.ecsp.uidam.usermanagement.scheduler;

import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Scheduler service for automatic unlocking of temporarily blocked users.
 * This service runs periodically to check and unlock users whose temporary lock period has expired.
 * Delegates actual unlock operations to UsersService to avoid code duplication.
 */
@Component
@ConditionalOnProperty(value = "temporary.lock.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class UserAutoUnlockScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAutoUnlockScheduler.class);
    private static final int DEFAULT_TEMPORARY_LOCK_PERIOD_MINUTES = 30;

    private final TenantConfigurationService tenantConfigurationService;
    private final UsersRepository userRepository;
    private final UsersService usersService;
    private final ConfigurableEnvironment environment;
    
    @Value("${tenant.multitenant.enabled:false}")
    private boolean multiTenantEnabled;

    @Value("${tenant.default:ecsp}")
    private String defaultTenant;

    /**
     * Constructor for dependency injection.
     */
    public UserAutoUnlockScheduler(TenantConfigurationService tenantConfigurationService,
                                  UsersRepository userRepository,
                                  UsersService usersService,
                                  ConfigurableEnvironment environment) {
        this.tenantConfigurationService = tenantConfigurationService;
        this.userRepository = userRepository;
        this.usersService = usersService;
        this.environment = environment;
    }

    /**
     * Scheduled method to unlock blocked users whose temporary lock period has expired.
     * Runs every 5 minutes by default (configurable via cron expression).
     * Processes all tenants if multi-tenancy is enabled, otherwise only default tenant.
     */
    @Scheduled(cron = "${temporary.lock.scheduler.cron:0 */5 * * * *}")
    public void unlockExpiredBlockedUsers() {
        LOGGER.info("Starting scheduled task: Unlock expired blocked users");
        long startTime = System.currentTimeMillis();
        int totalUnlockedUsers = 0;

        try {
            if (multiTenantEnabled) {
                String tenantIdsProperty = environment.getProperty("tenant.ids", "");
                Set<String> tenantIds = tenantIdsProperty.isEmpty() 
                    ? Set.of() 
                    : Set.of(tenantIdsProperty.split(","));
                LOGGER.debug("Multi-tenancy enabled. Processing {} tenants", tenantIds.size());

                for (String tenantId : tenantIds) {
                    try {
                        TenantContext.setCurrentTenant(tenantId);
                        int unlockedCount = processTenantsBlockedUsers(tenantId);
                        totalUnlockedUsers += unlockedCount;
                    } catch (Exception e) {
                        LOGGER.error("Error processing tenant: {}. Continuing with next tenant.", tenantId, e);
                    } finally {
                        TenantContext.clear();
                    }
                }
            } else {
                LOGGER.debug("Single-tenant mode. Processing default tenant: {}", defaultTenant);
                TenantContext.setCurrentTenant(defaultTenant);
                try {
                    totalUnlockedUsers = processTenantsBlockedUsers(defaultTenant);
                } finally {
                    TenantContext.clear();
                }
            }

            long executionTime = System.currentTimeMillis() - startTime;
            LOGGER.info("Completed scheduled task: Unlocked {} users in {} ms",
                totalUnlockedUsers, executionTime);

        } catch (Exception e) {
            LOGGER.error("Error in scheduled task: Unlock expired blocked users", e);
        }
    }

    /**
     * Process blocked users for a specific tenant.
     *
     * @param tenantId the tenant ID to process
     * @return number of users unlocked
     */
    protected int processTenantsBlockedUsers(String tenantId) {
        LOGGER.debug("Processing blocked users for tenant: {}", tenantId);

        UserManagementTenantProperties tenantProperties =
            tenantConfigurationService.getTenantProperties(tenantId);

        if (tenantProperties == null) {
            LOGGER.warn("Tenant properties not found for tenant: {}. Skipping.", tenantId);
            return 0;
        }

        // Check if temporary lock feature is enabled for this tenant
        Boolean temporaryLockEnabled = tenantProperties.getTemporaryLockEnabled();
        if (temporaryLockEnabled == null || !temporaryLockEnabled) {
            LOGGER.debug("Temporary lock feature is disabled for tenant: {}. Skipping.", tenantId);
            return 0;
        }

        Integer temporaryLockPeriodMinutes = tenantProperties.getTemporaryLockPeriodMinutes();
        if (temporaryLockPeriodMinutes == null || temporaryLockPeriodMinutes <= 0) {
            LOGGER.warn("Invalid temporary lock period for tenant: {}. Using default: {} minutes",
                tenantId, DEFAULT_TEMPORARY_LOCK_PERIOD_MINUTES);
            temporaryLockPeriodMinutes = DEFAULT_TEMPORARY_LOCK_PERIOD_MINUTES;
        }

        LOGGER.debug("Temporary lock enabled for tenant: {} with period: {} minutes",
            tenantId, temporaryLockPeriodMinutes);

        // Get current timestamp for comparison
        Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());

        LOGGER.debug("Finding blocked users with temporary lock expired before: {}", currentTimestamp);

        // Find all blocked users whose temporary_lock_timestamp is not null and <= current time
        List<UserEntity> blockedUsers = userRepository.findByStatusAndTemporaryLockTimestampBefore(
            UserStatus.BLOCKED, currentTimestamp);

        if (blockedUsers.isEmpty()) {
            LOGGER.debug("No blocked users found for auto-unlock in tenant: {}", tenantId);
            return 0;
        }

        LOGGER.info("Found {} blocked users eligible for auto-unlock in tenant: {}",
            blockedUsers.size(), tenantId);

        // Delegate to UsersService for actual unlock operations
        int unlockedCount = usersService.processBlockedUsersForScheduledUnlock(
            blockedUsers, temporaryLockPeriodMinutes);

        LOGGER.info("Successfully unlocked {} users in tenant: {}", unlockedCount, tenantId);
        return unlockedCount;
    }
}
