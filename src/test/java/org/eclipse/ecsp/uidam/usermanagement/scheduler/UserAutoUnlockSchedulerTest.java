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

import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserAutoUnlockScheduler.
 */
@ExtendWith(MockitoExtension.class)
class UserAutoUnlockSchedulerTest {

    private static final int DEFAULT_LOCK_PERIOD_MINUTES = 30;
    private static final int EXPIRED_LOCK_MINUTES = 35;
    private static final int CUSTOM_LOCK_PERIOD_MINUTES = 60;
    private static final int CUSTOM_EXPIRED_LOCK_MINUTES = 65;
    private static final int INVALID_LOCK_PERIOD = -1;
    private static final int EXPECTED_UNLOCK_COUNT = 1;
    private static final int EXPECTED_MULTI_TENANT_CALLS = 2;

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private UsersRepository userRepository;

    @Mock
    private UsersService usersService;

    @Mock
    private ConfigurableEnvironment environment;

    private UserAutoUnlockScheduler scheduler;

    private UserManagementTenantProperties tenantProperties;
    private UserEntity blockedUser;

    @BeforeEach
    void setUp() {
        // Manually create scheduler with mocked dependencies
        scheduler = new UserAutoUnlockScheduler(
            tenantConfigurationService,
            userRepository,
            usersService,
            environment
        );
        
        // Set up test properties
        ReflectionTestUtils.setField(scheduler, "multiTenantEnabled", false);
        ReflectionTestUtils.setField(scheduler, "defaultTenant", "ecsp");

        // Set up tenant properties
        tenantProperties = new UserManagementTenantProperties();
        tenantProperties.setTemporaryLockEnabled(true);
        tenantProperties.setTemporaryLockPeriodMinutes(DEFAULT_LOCK_PERIOD_MINUTES);

        // Set up blocked user
        blockedUser = new UserEntity();
        blockedUser.setId(BigInteger.ONE);
        blockedUser.setUserName("testuser");
        blockedUser.setStatus(UserStatus.BLOCKED);
        blockedUser.setUpdateDate(Timestamp.valueOf(LocalDateTime.now().minusMinutes(EXPIRED_LOCK_MINUTES)));
    }

    @Test
    void testUnlockExpiredBlockedUsers_SingleTenant_Success() {
        // Given
        when(tenantConfigurationService.getTenantProperties("ecsp")).thenReturn(tenantProperties);
        when(userRepository.findByStatusAndTemporaryLockTimestampBefore(
                eq(UserStatus.BLOCKED), any(Timestamp.class)))
            .thenReturn(Arrays.asList(blockedUser));
        when(usersService.processBlockedUsersForScheduledUnlock(any(), any())).thenReturn(EXPECTED_UNLOCK_COUNT);

        // When
        scheduler.unlockExpiredBlockedUsers();

        // Then
        verify(userRepository).findByStatusAndTemporaryLockTimestampBefore(
                eq(UserStatus.BLOCKED), any(Timestamp.class));
        verify(usersService).processBlockedUsersForScheduledUnlock(
            Arrays.asList(blockedUser), DEFAULT_LOCK_PERIOD_MINUTES);
    }

    @Test
    void testUnlockExpiredBlockedUsers_MultiTenant_Success() {
        // Given
        ReflectionTestUtils.setField(scheduler, "multiTenantEnabled", true);

        when(environment.getProperty("tenant.ids", "")).thenReturn("ecsp,sdp");
        when(tenantConfigurationService.getTenantProperties(anyString())).thenReturn(tenantProperties);
        when(userRepository.findByStatusAndTemporaryLockTimestampBefore(eq(UserStatus.BLOCKED), any(Timestamp.class)))
            .thenReturn(Arrays.asList(blockedUser));
        when(usersService.processBlockedUsersForScheduledUnlock(any(), any())).thenReturn(EXPECTED_UNLOCK_COUNT);

        // When
        scheduler.unlockExpiredBlockedUsers();

        // Then
        verify(environment).getProperty("tenant.ids", "");
        verify(userRepository, times(EXPECTED_MULTI_TENANT_CALLS))
            .findByStatusAndTemporaryLockTimestampBefore(eq(UserStatus.BLOCKED), any(Timestamp.class));
        verify(usersService, times(EXPECTED_MULTI_TENANT_CALLS))
            .processBlockedUsersForScheduledUnlock(any(), eq(DEFAULT_LOCK_PERIOD_MINUTES));
    }

    @Test
    void testUnlockExpiredBlockedUsers_TemporaryLockDisabled() {
        // Given
        tenantProperties.setTemporaryLockEnabled(false);
        when(tenantConfigurationService.getTenantProperties("ecsp")).thenReturn(tenantProperties);

        // When
        scheduler.unlockExpiredBlockedUsers();

        // Then
        verify(userRepository, never()).findByStatusAndTemporaryLockTimestampBefore(any(), any());
        verify(usersService, never()).processBlockedUsersForScheduledUnlock(any(), any());
    }

    @Test
    void testUnlockExpiredBlockedUsers_NoBlockedUsers() {
        // Given
        when(tenantConfigurationService.getTenantProperties("ecsp")).thenReturn(tenantProperties);
        when(userRepository.findByStatusAndTemporaryLockTimestampBefore(
                eq(UserStatus.BLOCKED), any(Timestamp.class)))
            .thenReturn(new ArrayList<>());

        // When
        scheduler.unlockExpiredBlockedUsers();

        // Then
        verify(userRepository).findByStatusAndTemporaryLockTimestampBefore(
                eq(UserStatus.BLOCKED), any(Timestamp.class));
        verify(usersService, never()).processBlockedUsersForScheduledUnlock(any(), any());
    }

    @Test
    void testUnlockExpiredBlockedUsers_TenantPropertiesNull() {
        // Given
        when(tenantConfigurationService.getTenantProperties("ecsp")).thenReturn(null);

        // When
        scheduler.unlockExpiredBlockedUsers();

        // Then
        verify(userRepository, never()).findByStatusAndTemporaryLockTimestampBefore(any(), any());
        verify(usersService, never()).processBlockedUsersForScheduledUnlock(any(), any());
    }

    @Test
    void testUnlockExpiredBlockedUsers_InvalidLockPeriod() {
        // Given
        tenantProperties.setTemporaryLockPeriodMinutes(INVALID_LOCK_PERIOD);
        when(tenantConfigurationService.getTenantProperties("ecsp")).thenReturn(tenantProperties);
        when(userRepository.findByStatusAndTemporaryLockTimestampBefore(
                eq(UserStatus.BLOCKED), any(Timestamp.class)))
            .thenReturn(Arrays.asList(blockedUser));
        when(usersService.processBlockedUsersForScheduledUnlock(any(), any())).thenReturn(EXPECTED_UNLOCK_COUNT);

        // When
        scheduler.unlockExpiredBlockedUsers();

        // Then - Should use default 30 minutes
        verify(userRepository).findByStatusAndTemporaryLockTimestampBefore(
                eq(UserStatus.BLOCKED), any(Timestamp.class));
        verify(usersService).processBlockedUsersForScheduledUnlock(any(), eq(DEFAULT_LOCK_PERIOD_MINUTES));
    }

    @Test
    void testProcessTenantsBlockedUsers_Success() {
        // Given
        when(tenantConfigurationService.getTenantProperties("ecsp")).thenReturn(tenantProperties);
        when(userRepository.findByStatusAndTemporaryLockTimestampBefore(eq(UserStatus.BLOCKED), any(Timestamp.class)))
            .thenReturn(Arrays.asList(blockedUser));
        when(usersService.processBlockedUsersForScheduledUnlock(any(), any())).thenReturn(EXPECTED_UNLOCK_COUNT);

        // When
        int unlockedCount = scheduler.processTenantsBlockedUsers("ecsp");

        // Then
        assert unlockedCount == EXPECTED_UNLOCK_COUNT;
        verify(usersService).processBlockedUsersForScheduledUnlock(
            Arrays.asList(blockedUser), DEFAULT_LOCK_PERIOD_MINUTES);
    }

    @Test
    void testProcessTenantsBlockedUsers_CustomLockPeriod() {
        // Given
        tenantProperties.setTemporaryLockPeriodMinutes(CUSTOM_LOCK_PERIOD_MINUTES);
        blockedUser.setUpdateDate(Timestamp.valueOf(LocalDateTime.now().minusMinutes(CUSTOM_EXPIRED_LOCK_MINUTES)));

        when(tenantConfigurationService.getTenantProperties("ecsp")).thenReturn(tenantProperties);
        when(userRepository.findByStatusAndTemporaryLockTimestampBefore(eq(UserStatus.BLOCKED), any(Timestamp.class)))
            .thenReturn(Arrays.asList(blockedUser));
        when(usersService.processBlockedUsersForScheduledUnlock(any(), any())).thenReturn(EXPECTED_UNLOCK_COUNT);

        // When
        int unlockedCount = scheduler.processTenantsBlockedUsers("ecsp");

        // Then
        assert unlockedCount == EXPECTED_UNLOCK_COUNT;
        verify(usersService).processBlockedUsersForScheduledUnlock(any(), eq(CUSTOM_LOCK_PERIOD_MINUTES));
    }
}
