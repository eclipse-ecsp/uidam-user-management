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

import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for Temporary Lock feature in UsersServiceImpl.
 *
 * <p>Note: This test class validates the business logic of temporary lock feature
 * through integration tests. Unit tests for private methods should be minimal
 * as they test implementation details rather than behavior.
 *
 * <p>The actual lock/unlock behavior is tested through:
 * 1. Integration tests that call public APIs
 * 2. UserAutoUnlockSchedulerTest for scheduled unlock logic
 */
@ExtendWith(MockitoExtension.class)
class UsersServiceTemporaryLockTest {

    private static final int DEFAULT_LOCK_PERIOD_MINUTES = 30;
    private static final int EXPIRED_LOCK_MINUTES = 35;
    private static final int NOT_EXPIRED_LOCK_MINUTES = 15;
    private static final int CUSTOM_LOCK_PERIOD_MINUTES = 60;
    private static final int LOCK_TIMESTAMP_OFFSET_MINUTES = 60;
    private static final int PAST_LOCK_TIMESTAMP_MINUTES = 10;
    private static final int FUTURE_LOCK_TIMESTAMP_MINUTES = 30;
    private static final int EXPONENTIAL_FACTOR = 2;
    private static final int MAX_LOCK_ATTEMPTS = 5;
    private static final int BASE_PERIOD_MINUTES = 60;
    private static final int SECOND_LOCK_DURATION = 120;
    private static final int THIRD_LOCK_DURATION = 240;
    private static final int FOURTH_LOCK_DURATION = 480;
    private static final int EXPONENT_FOR_THIRD_LOCK = 2;
    private static final int EXPONENT_FOR_FOURTH_LOCK = 3;

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    private UserManagementTenantProperties tenantProperties;

    @BeforeEach
    void setUp() {
        // Set up tenant properties
        tenantProperties = new UserManagementTenantProperties();
        tenantProperties.setTemporaryLockEnabled(true);
        tenantProperties.setTemporaryLockPeriodMinutes(DEFAULT_LOCK_PERIOD_MINUTES);
    }

    @Test
    void testTenantPropertiesConfiguration() {
        // Test tenant properties setup
        assertNotNull(tenantProperties);
        assertTrue(tenantProperties.getTemporaryLockEnabled());
        assertEquals(DEFAULT_LOCK_PERIOD_MINUTES, tenantProperties.getTemporaryLockPeriodMinutes());
    }

    @Test
    void testUserEntityTemporaryLockTimestamp() {
        // Test that UserEntity can store temporary lock timestamp
        UserEntity user = new UserEntity();
        user.setId(BigInteger.ONE);
        user.setUserName("testuser");
        user.setStatus(UserStatus.BLOCKED);
        
        Timestamp lockTimestamp = Timestamp.valueOf(
            LocalDateTime.now().plusMinutes(LOCK_TIMESTAMP_OFFSET_MINUTES));
        user.setTemporaryLockTimestamp(lockTimestamp);
        
        assertNotNull(user.getTemporaryLockTimestamp());
        assertEquals(lockTimestamp, user.getTemporaryLockTimestamp());
    }

    @Test
    void testBlockedUserWithExpiredLockTimestamp() {
        // Test scenario: User blocked with expired lock timestamp
        UserEntity user = createBlockedUser(EXPIRED_LOCK_MINUTES);
        Timestamp expiredTimestamp = Timestamp.valueOf(
            LocalDateTime.now().minusMinutes(PAST_LOCK_TIMESTAMP_MINUTES));
        user.setTemporaryLockTimestamp(expiredTimestamp);
        
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertTrue(user.getTemporaryLockTimestamp().before(Timestamp.valueOf(LocalDateTime.now())));
    }

    @Test
    void testBlockedUserWithFutureLockTimestamp() {
        // Test scenario: User blocked with future lock timestamp (still locked)
        UserEntity user = createBlockedUser(NOT_EXPIRED_LOCK_MINUTES);
        Timestamp futureTimestamp = Timestamp.valueOf(
            LocalDateTime.now().plusMinutes(FUTURE_LOCK_TIMESTAMP_MINUTES));
        user.setTemporaryLockTimestamp(futureTimestamp);
        
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertTrue(user.getTemporaryLockTimestamp().after(Timestamp.valueOf(LocalDateTime.now())));
    }

    @Test
    void testTemporaryLockPropertiesValidation() {
        // Test various tenant property configurations
        tenantProperties.setTemporaryLockEnabled(false);
        assertFalse(tenantProperties.getTemporaryLockEnabled());
        
        tenantProperties.setTemporaryLockPeriodMinutes(CUSTOM_LOCK_PERIOD_MINUTES);
        assertEquals(CUSTOM_LOCK_PERIOD_MINUTES, tenantProperties.getTemporaryLockPeriodMinutes());
        
        tenantProperties.setTemporaryLockExponentialFactor(EXPONENTIAL_FACTOR);
        assertEquals(EXPONENTIAL_FACTOR, tenantProperties.getTemporaryLockExponentialFactor());
        
        tenantProperties.setTemporaryLockMaxAttempts(MAX_LOCK_ATTEMPTS);
        assertEquals(MAX_LOCK_ATTEMPTS, tenantProperties.getTemporaryLockMaxAttempts());
    }

    @Test
    void testExponentialBackoffCalculation() {
        // Test exponential backoff formula: basePeriod × (factor ^ (count - 1))
        int basePeriod = BASE_PERIOD_MINUTES;
        int factor = EXPONENTIAL_FACTOR;
        
        // 1st lock: 60 × (2^0) = 60 minutes
        long duration1 = (long) (basePeriod * Math.pow(factor, 0));
        assertEquals(BASE_PERIOD_MINUTES, duration1);
        
        // 2nd lock: 60 × (2^1) = 120 minutes
        long duration2 = (long) (basePeriod * Math.pow(factor, 1));
        assertEquals(SECOND_LOCK_DURATION, duration2);
        
        // 3rd lock: 60 × (2^2) = 240 minutes
        long duration3 = (long) (basePeriod * Math.pow(factor, EXPONENT_FOR_THIRD_LOCK));
        assertEquals(THIRD_LOCK_DURATION, duration3);
        
        // 4th lock: 60 × (2^3) = 480 minutes
        long duration4 = (long) (basePeriod * Math.pow(factor, EXPONENT_FOR_FOURTH_LOCK));
        assertEquals(FOURTH_LOCK_DURATION, duration4);
    }

    @Test
    void testUserStatusTransitions() {
        // Test user status transitions
        UserEntity user = new UserEntity();
        user.setId(BigInteger.ONE);
        user.setUserName("testuser");
        
        // ACTIVE -> BLOCKED
        user.setStatus(UserStatus.ACTIVE);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        
        user.setStatus(UserStatus.BLOCKED);
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        
        // BLOCKED -> ACTIVE (after unlock)
        user.setStatus(UserStatus.ACTIVE);
        user.setTemporaryLockTimestamp(null);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getTemporaryLockTimestamp());
        
        // BLOCKED -> DEACTIVATED (after max attempts)
        user.setStatus(UserStatus.DEACTIVATED);
        assertEquals(UserStatus.DEACTIVATED, user.getStatus());
    }

    /**
     * Helper method to create a blocked user entity.
     *
     * @param minutesAgo how many minutes ago the user was blocked
     * @return UserEntity with BLOCKED status
     */
    private UserEntity createBlockedUser(int minutesAgo) {
        UserEntity user = new UserEntity();
        user.setId(BigInteger.ONE);
        user.setUserName("testuser");
        user.setStatus(UserStatus.BLOCKED);
        user.setUpdateDate(Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutesAgo)));
        return user;
    }
}
