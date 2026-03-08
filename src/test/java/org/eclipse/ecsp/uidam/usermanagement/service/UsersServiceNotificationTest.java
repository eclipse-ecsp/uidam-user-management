/*
 * Copyright (c) 2023 - 2026 Harman International
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
import org.eclipse.ecsp.uidam.usermanagement.service.impl.UsersServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for User Lock Notification functionality in UsersServiceImpl.
 * 
 * <p>Tests cover:
 * - Lock notification with different lock types (permanent, temporary, deactivated)
 * - Unlock notification
 * - Notification enabled/disabled scenarios
 * - Email validation
 * - Tenant override scenarios
 * - Exception handling
 * 
 * <p>Note: These tests use reflection to test private methods as they contain critical
 * business logic for notification handling. In a production scenario, consider making
 * these methods package-private for testability or testing through public APIs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Users Service - Lock/Unlock Notification Tests")
class UsersServiceNotificationTest {

    private static final BigInteger USER_ID = BigInteger.valueOf(123);
    private static final String USER_EMAIL = "test.user@example.com";
    private static final String USER_FIRST_NAME = "Test";
    private static final String USER_LAST_NAME = "User";
    private static final String FULL_NAME = "Test User";
    private static final long TEMPORARY_LOCK_DURATION = 60L;
    private static final String NOTIFICATION_ID_LOCKED = "UIDAM_USER_ACCOUNT_LOCKED";
    private static final String NOTIFICATION_ID_TEMPORARY = "UIDAM_USER_ACCOUNT_LOCKED_TEMPORARY";
    private static final String NOTIFICATION_ID_DEACTIVATED = "UIDAM_USER_ACCOUNT_DEACTIVATED";
    private static final String NOTIFICATION_ID_UNLOCKED = "UIDAM_USER_ACCOUNT_UNLOCKED";

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    private UsersServiceImpl usersService;
    private UserManagementTenantProperties tenantProperties;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        tenantProperties = new UserManagementTenantProperties();
        tenantProperties.setUserLockNotificationEnabled(true);
        
        // Create a partial mock - only mock the methods we need
        usersService = Mockito.mock(UsersServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        
        // Set up mock user entity
        userEntity = new UserEntity();
        userEntity.setId(USER_ID);
        userEntity.setEmail(USER_EMAIL);
        userEntity.setFirstName(USER_FIRST_NAME);
        userEntity.setLastName(USER_LAST_NAME);
        userEntity.setStatus(UserStatus.BLOCKED);

        // Inject mocked dependencies via reflection
        ReflectionTestUtils.setField(usersService, "emailNotificationService", emailNotificationService);
        ReflectionTestUtils.setField(usersService, "tenantConfigurationService", tenantConfigurationService);
        
        // Mock tenantConfigurationService to return our test properties
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
    }

    @Test
    @DisplayName("Should send permanent lock notification with correct notification ID")
    void testSendPermanentLockNotification() throws Exception {
        // Given
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> userDetailsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> notificationIdCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> notificationDataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            userDetailsCaptor.capture(),
            notificationIdCaptor.capture(),
            notificationDataCaptor.capture()
        );

        assertEquals(NOTIFICATION_ID_LOCKED, notificationIdCaptor.getValue());
        assertEquals(USER_EMAIL, userDetailsCaptor.getValue().get("emailAddress"));
        assertEquals(FULL_NAME, userDetailsCaptor.getValue().get("name"));
    }

    @Test
    @DisplayName("Should send temporary lock notification with lock duration")
    void testSendTemporaryLockNotification() throws Exception {
        // Given
        boolean isLocked = true;
        long lockDuration = TEMPORARY_LOCK_DURATION;
        boolean isTemporaryLock = true;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        ArgumentCaptor<String> notificationIdCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> notificationDataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            anyMap(),
            notificationIdCaptor.capture(),
            notificationDataCaptor.capture()
        );

        assertEquals(NOTIFICATION_ID_TEMPORARY, notificationIdCaptor.getValue());
        assertTrue(notificationDataCaptor.getValue().containsKey("lockDuration"));
        assertEquals(TEMPORARY_LOCK_DURATION, notificationDataCaptor.getValue().get("lockDuration"));
    }

    @Test
    @DisplayName("Should send deactivated notification when max lock attempts exceeded")
    void testSendDeactivatedNotification() throws Exception {
        // Given
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = true;
        userEntity.setStatus(UserStatus.DEACTIVATED);

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        ArgumentCaptor<String> notificationIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailNotificationService, times(1)).sendNotification(
            anyMap(),
            notificationIdCaptor.capture(),
            anyMap()
        );

        assertEquals(NOTIFICATION_ID_DEACTIVATED, notificationIdCaptor.getValue());
    }

    @Test
    @DisplayName("Should send unlock notification with correct notification ID")
    void testSendUnlockNotification() throws Exception {
        // Given
        boolean isLocked = false;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;
        userEntity.setStatus(UserStatus.ACTIVE);

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        ArgumentCaptor<String> notificationIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailNotificationService, times(1)).sendNotification(
            anyMap(),
            notificationIdCaptor.capture(),
            anyMap()
        );

        assertEquals(NOTIFICATION_ID_UNLOCKED, notificationIdCaptor.getValue());
    }

    @Test
    @DisplayName("Should not send notification when notification is disabled")
    void testNotificationDisabled() throws Exception {
        // Given
        tenantProperties.setUserLockNotificationEnabled(false);
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        verify(emailNotificationService, never()).sendNotification(anyMap(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should not send notification when notification enabled is null")
    void testNotificationEnabledNull() throws Exception {
        // Given
        tenantProperties.setUserLockNotificationEnabled(null);
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        verify(emailNotificationService, never()).sendNotification(anyMap(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should not send notification when user email is null")
    void testUserEmailNull() throws Exception {
        // Given
        userEntity.setEmail(null);
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        verify(emailNotificationService, never()).sendNotification(anyMap(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should not send notification when user email is empty")
    void testUserEmailEmpty() throws Exception {
        // Given
        userEntity.setEmail("");
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        verify(emailNotificationService, never()).sendNotification(anyMap(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should handle user with only first name")
    void testUserWithOnlyFirstName() throws Exception {
        // Given
        userEntity.setLastName(null);
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> userDetailsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            userDetailsCaptor.capture(),
            anyString(),
            anyMap()
        );

        assertEquals(USER_FIRST_NAME, userDetailsCaptor.getValue().get("name"));
    }

    @Test
    @DisplayName("Should handle user with only last name")
    void testUserWithOnlyLastName() throws Exception {
        // Given
        userEntity.setFirstName(null);
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> userDetailsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            userDetailsCaptor.capture(),
            anyString(),
            anyMap()
        );

        assertTrue(userDetailsCaptor.getValue().get("name").contains(USER_LAST_NAME));
    }

    @Test
    @DisplayName("Should handle user with no first or last name")
    void testUserWithNoName() throws Exception {
        // Given
        userEntity.setFirstName(null);
        userEntity.setLastName(null);
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> userDetailsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            userDetailsCaptor.capture(),
            anyString(),
            anyMap()
        );

        assertNotNull(userDetailsCaptor.getValue().get("name"));
    }

    @Test
    @DisplayName("Should not throw exception when email service fails")
    void testEmailServiceException() throws Exception {
        // Given
        doThrow(new RuntimeException("Email service unavailable"))
            .when(emailNotificationService).sendNotification(anyMap(), anyString(), anyMap());
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When & Then - should not throw exception
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Verify email service was called
        verify(emailNotificationService, times(1)).sendNotification(anyMap(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should include email in notification data")
    void testEmailInNotificationData() throws Exception {
        // Given
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> notificationDataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            anyMap(),
            anyString(),
            notificationDataCaptor.capture()
        );

        assertEquals(USER_EMAIL, notificationDataCaptor.getValue().get("email"));
    }

    @Test
    @DisplayName("Should include user name in notification data when available")
    void testUserNameInNotificationData() throws Exception {
        // Given
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> notificationDataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            anyMap(),
            anyString(),
            notificationDataCaptor.capture()
        );

        assertTrue(notificationDataCaptor.getValue().containsKey("name"));
        assertEquals(FULL_NAME, notificationDataCaptor.getValue().get("name"));
    }

    @Test
    @DisplayName("Should not include lock duration for permanent lock")
    void testNoLockDurationForPermanentLock() throws Exception {
        // Given
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = false;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> notificationDataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            anyMap(),
            anyString(),
            notificationDataCaptor.capture()
        );

        assertFalse(notificationDataCaptor.getValue().containsKey("lockDuration"));
    }

    @Test
    @DisplayName("Should not include lock duration for deactivated account")
    void testNoLockDurationForDeactivatedAccount() throws Exception {
        // Given
        boolean isLocked = true;
        long lockDuration = 0L;
        boolean isTemporaryLock = false;
        boolean isDeactivated = true;

        // When
        ReflectionTestUtils.invokeMethod(usersService, "sendUserLockNotification",
            userEntity, isLocked, lockDuration, isTemporaryLock, isDeactivated);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> notificationDataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(emailNotificationService, times(1)).sendNotification(
            anyMap(),
            anyString(),
            notificationDataCaptor.capture()
        );

        assertFalse(notificationDataCaptor.getValue().containsKey("lockDuration"));
    }
}
