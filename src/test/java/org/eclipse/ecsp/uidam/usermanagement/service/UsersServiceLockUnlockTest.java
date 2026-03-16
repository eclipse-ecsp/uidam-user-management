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

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.common.metrics.MetricInfo;
import org.eclipse.ecsp.uidam.common.metrics.UidamMetricsService;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAddressEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeValueEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEvents;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserEventStatus;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.InActiveUserException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserEventRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.impl.UsersServiceImpl;
import org.eclipse.ecsp.uidam.usermanagement.utilities.UserAuditHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for User Lock/Unlock functionality in UsersServiceImpl.
 * Tests coverage for: lockUserAccount, checkAndUnlockIfEligible, verifyUserStatus,
 * unlockBlockedUser, processBlockedUsersForScheduledUnlock, mapUserFields,
 * mapUserAttributeValueEntityByAttributeId, calculateRemainingLockDuration
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UsersServiceImpl.class, JacksonAutoConfiguration.class})
@MockBean(JpaMetamodelMappingContext.class)
class UsersServiceLockUnlockTest {

    @Autowired
    private UsersServiceImpl usersService;

    @MockBean
    private UsersRepository userRepository;

    @MockBean
    private UserEventRepository userEventRepository;

    @MockBean
    private TenantConfigurationService tenantConfigurationService;

    @MockBean
    private UserManagementTenantProperties tenantProperties;

    @MockBean
    private UidamMetricsService uidamMetricsService;

    @MockBean
    private UserAuditHelper userAuditHelper;

    @MockBean
    private EmailNotificationService emailNotificationService;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.cache.CacheTokenService cacheTokenService;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeRepository userAttributeRepository;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeValueRepository userAttributeValueRepository;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.repository.UserRecoverySecretRepository userRecoverySecretRepository;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.dao.UserManagementDao userManagementDao;

    @MockBean
    private AuthorizationServerClient authorizationServerClient;

    @MockBean
    private jakarta.persistence.EntityManager entityManager;

    @MockBean
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @MockBean
    private RolesService rolesService;

    @MockBean
    private org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository passwordPolicyRepository;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository rolesRepository;

    @MockBean
    private ClientRegistration clientRegistrationService;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.repository.CloudProfilesRepository cloudProfilesRepository;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.repository.EmailVerificationRepository emailVerificationRepository;

    @MockBean
    private org.eclipse.ecsp.uidam.usermanagement.repository.PasswordHistoryRepository passwordHistoryRepository;

    @MockBean
    private org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService passwordValidationService;

    @MockBean
    private org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository accountRepository;

    @MockBean
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private UsersServiceImpl usersServiceSpy;

    // Test constants
    private static final int ALLOWED_LOGIN_ATTEMPTS = 3;
    private static final int TEMPORARY_LOCK_MAX_ATTEMPTS = 5;
    private static final int TEMPORARY_LOCK_PERIOD_MINUTES = 60;
    private static final int EXPONENTIAL_FACTOR = 2;
    private static final int LOCK_EVENTS_COUNT = 15;
    private static final int LOCK_DURATION_30_MINUTES = 30;
    private static final int LOCK_DURATION_10_MINUTES = 10;
    private static final int MIN_REMAINING_TIME = 29;
    private static final int BLOCKED_USERS_COUNT = 3;
    private static final int ATTRIBUTE_MAP_SIZE = 2;

    @BeforeEach
    @AfterEach
    void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @BeforeEach
    void setup() {
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        usersServiceSpy = Mockito.spy(usersService);
    }

    private UserEntity createTestUser(BigInteger userId, String userName, UserStatus status) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUserName(userName);
        user.setStatus(status);
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    // ==================== Tests for lockUserAccount ====================

    @Test
    void testLockUserAccount_TemporaryLockEnabled_FirstAttempt() throws Exception {
        // Given
        final UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.ACTIVE);

        when(tenantProperties.getTemporaryLockMaxAttempts()).thenReturn(TEMPORARY_LOCK_MAX_ATTEMPTS);
        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(true);
        when(tenantProperties.getTemporaryLockPeriodMinutes()).thenReturn(TEMPORARY_LOCK_PERIOD_MINUTES);
        when(tenantProperties.getTemporaryLockExponentialFactor()).thenReturn(EXPONENTIAL_FACTOR);
        when(userEventRepository.findUserEventsByUserIdAndEventType(any(), any(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("lockUserAccount", 
            UserEntity.class, int.class);
        method.setAccessible(true);

        // When
        long lockDuration = (long) method.invoke(usersServiceSpy, user, ALLOWED_LOGIN_ATTEMPTS);

        // Then
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertEquals(TEMPORARY_LOCK_PERIOD_MINUTES, lockDuration); // First lock = 60 minutes
        assertNotNull(user.getTemporaryLockTimestamp());
        verify(userRepository, times(1)).save(user);
        verify(uidamMetricsService, times(1)).incrementCounter(any(MetricInfo.class));
    }

    @Test
    void testLockUserAccount_TemporaryLockDisabled_PermanentBlock() throws Exception {
        // Given
        final UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.ACTIVE);

        when(tenantProperties.getTemporaryLockMaxAttempts()).thenReturn(TEMPORARY_LOCK_MAX_ATTEMPTS);
        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("lockUserAccount", 
            UserEntity.class, int.class);
        method.setAccessible(true);

        // When
        long lockDuration = (long) method.invoke(usersServiceSpy, user, ALLOWED_LOGIN_ATTEMPTS);

        // Then
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertEquals(0, lockDuration); // Permanent block
        assertNull(user.getTemporaryLockTimestamp());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testLockUserAccount_MaxAttemptsExceeded_Deactivated() throws Exception {
        // Given
        final UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.ACTIVE);

        // Mock that user has been locked 5 times already
        final List<UserEvents> events = new ArrayList<>();
        for (int i = 0; i < LOCK_EVENTS_COUNT; i++) { // 5 blocks * 3 attempts each
            UserEvents event = new UserEvents();
            event.setEventStatus(UserEventStatus.FAILURE.getValue());
            events.add(event);
        }

        when(tenantProperties.getTemporaryLockMaxAttempts()).thenReturn(TEMPORARY_LOCK_MAX_ATTEMPTS);
        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(true);
        when(userEventRepository.findUserEventsByUserIdAndEventType(any(), any(), anyInt()))
            .thenReturn(events);
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("lockUserAccount", 
            UserEntity.class, int.class);
        method.setAccessible(true);

        // When
        long lockDuration = (long) method.invoke(usersServiceSpy, user, ALLOWED_LOGIN_ATTEMPTS);

        // Then
        assertEquals(UserStatus.DEACTIVATED, user.getStatus());
        assertEquals(0, lockDuration); // Permanent deactivation
        assertNull(user.getTemporaryLockTimestamp());
        verify(userRepository, times(1)).save(user);
    }

    // ==================== Tests for calculateRemainingLockDuration ====================

    @Test
    void testCalculateRemainingLockDuration_WithTimeRemaining() throws Exception {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_30_MINUTES);
        user.setTemporaryLockTimestamp(Timestamp.valueOf(lockUntil));

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("calculateRemainingLockDuration", 
            UserEntity.class);
        method.setAccessible(true);

        // When
        long remaining = (long) method.invoke(usersServiceSpy, user);

        // Then - Allow 1 minute tolerance
        assertTrue(remaining >= MIN_REMAINING_TIME && remaining <= LOCK_DURATION_30_MINUTES);
    }

    @Test
    void testCalculateRemainingLockDuration_ExpiredLock() throws Exception {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        LocalDateTime lockUntil = LocalDateTime.now().minusMinutes(LOCK_DURATION_10_MINUTES);
        user.setTemporaryLockTimestamp(Timestamp.valueOf(lockUntil));

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("calculateRemainingLockDuration", 
            UserEntity.class);
        method.setAccessible(true);

        // When
        long remaining = (long) method.invoke(usersServiceSpy, user);

        // Then
        assertEquals(0, remaining);
    }

    // ==================== Tests for verifyUserStatus ====================

    @Test
    void testVerifyUserStatus_NullUser_ThrowsException() {
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            usersService.verifyUserStatus(null, "testuser"));
    }

    @Test
    void testVerifyUserStatus_PendingUser_ThrowsException() {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.PENDING);

        // When & Then
        InActiveUserException exception = assertThrows(InActiveUserException.class, () -> 
            usersService.verifyUserStatus(user, "testuser"));
        
        assertEquals("USER_NOT_VERIFIED", exception.getErrorCode());
    }

    @Test
    void testVerifyUserStatus_BlockedUser_TemporaryLock_ThrowsException() {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_30_MINUTES);
        user.setTemporaryLockTimestamp(Timestamp.valueOf(lockUntil));

        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(true);

        // When & Then
        InActiveUserException exception = assertThrows(InActiveUserException.class, () -> 
            usersService.verifyUserStatus(user, "testuser"));
        
        assertEquals("USER_TEMPORARILY_BLOCKED", exception.getErrorCode());
        assertTrue(exception.getMinutesLeftToUnlock() >= MIN_REMAINING_TIME);
    }

    @Test
    void testVerifyUserStatus_BlockedUser_PermanentBlock_ThrowsException() {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        user.setTemporaryLockTimestamp(null); // No timestamp = permanent block

        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(false);

        // When & Then
        InActiveUserException exception = assertThrows(InActiveUserException.class, () -> 
            usersService.verifyUserStatus(user, "testuser"));
        
        assertEquals("USER_IS_BLOCKED", exception.getErrorCode());
    }

    @Test
    void testVerifyUserStatus_BlockedUser_AutoUnlock_Success() throws Exception {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        LocalDateTime lockUntil = LocalDateTime.now().minusMinutes(LOCK_DURATION_10_MINUTES); // Expired
        user.setTemporaryLockTimestamp(Timestamp.valueOf(lockUntil));

        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(true);
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        // When
        usersService.verifyUserStatus(user, "testuser");

        // Then - No exception should be thrown
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    void testVerifyUserStatus_ActiveUser_Success() {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.ACTIVE);

        // When & Then - No exception should be thrown
        try {
            usersService.verifyUserStatus(user, "testuser");
        } catch (Exception e) {
            throw new AssertionError("Should not throw exception for active user", e);
        }
    }

    @Test
    void testVerifyUserStatus_InactiveUser_ThrowsException() {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.DEACTIVATED);

        // When & Then
        InActiveUserException exception = assertThrows(InActiveUserException.class, () -> 
            usersService.verifyUserStatus(user, "testuser"));
        
        assertEquals("USER_NOT_ACTIVE", exception.getErrorCode());
    }

    // ==================== Tests for checkAndUnlockIfEligible ====================

    @Test
    void testCheckAndUnlockIfEligible_TemporaryLockDisabled_ReturnsFalse() throws Exception {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        LocalDateTime lockTime = LocalDateTime.now().plusMinutes(LOCK_DURATION_30_MINUTES);
        user.setTemporaryLockTimestamp(Timestamp.valueOf(lockTime));

        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(false);

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("checkAndUnlockIfEligible", 
            UserEntity.class);
        method.setAccessible(true);

        // When
        boolean result = (boolean) method.invoke(usersServiceSpy, user);

        // Then
        assertFalse(result);
        assertEquals(UserStatus.BLOCKED, user.getStatus()); // Still blocked
    }

    @Test
    void testCheckAndUnlockIfEligible_NoTimestamp_ReturnsFalse() throws Exception {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        user.setTemporaryLockTimestamp(null);

        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(true);

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("checkAndUnlockIfEligible", 
            UserEntity.class);
        method.setAccessible(true);

        // When
        boolean result = (boolean) method.invoke(usersServiceSpy, user);

        // Then
        assertFalse(result);
    }

    @Test
    void testCheckAndUnlockIfEligible_LockExpired_ReturnsTrue() throws Exception {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        LocalDateTime lockUntil = LocalDateTime.now().minusMinutes(LOCK_DURATION_10_MINUTES);
        user.setTemporaryLockTimestamp(Timestamp.valueOf(lockUntil));

        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(true);
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("checkAndUnlockIfEligible", 
            UserEntity.class);
        method.setAccessible(true);

        // When
        boolean result = (boolean) method.invoke(usersServiceSpy, user);

        // Then
        assertTrue(result);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testCheckAndUnlockIfEligible_LockNotExpired_ReturnsFalse() throws Exception {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_30_MINUTES);
        user.setTemporaryLockTimestamp(Timestamp.valueOf(lockUntil));

        when(tenantProperties.getTemporaryLockEnabled()).thenReturn(true);

        // Use reflection to call private method
        Method method = UsersServiceImpl.class.getDeclaredMethod("checkAndUnlockIfEligible", 
            UserEntity.class);
        method.setAccessible(true);

        // When
        boolean result = (boolean) method.invoke(usersServiceSpy, user);

        // Then
        assertFalse(result);
        assertEquals(UserStatus.BLOCKED, user.getStatus()); // Still blocked
    }

    // ==================== Tests for unlockBlockedUser ====================

    @Test
    void testUnlockBlockedUser_Success() {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.BLOCKED);
        LocalDateTime unlockTime = LocalDateTime.now();
        UserStatus previousStatus = UserStatus.BLOCKED;

        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        // When
        usersService.unlockBlockedUser(user, previousStatus, unlockTime);

        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("SYSTEM", user.getUpdatedBy());
        assertNull(user.getTemporaryLockTimestamp());
        verify(userRepository, times(1)).save(user);
        verify(uidamMetricsService, times(1)).incrementCounter(any(MetricInfo.class));
        verify(userAuditHelper, times(1)).logUserStatusChangedAudit(
            any(UserEntity.class), 
            eq(null), 
            eq(UserStatus.BLOCKED), 
            eq(UserStatus.ACTIVE), 
            any()
        );
    }

    // ==================== Tests for processBlockedUsersForScheduledUnlock ====================

    @Test
    void testProcessBlockedUsersForScheduledUnlock_Success() {
        // Given
        List<UserEntity> blockedUsers = new ArrayList<>();
        for (int i = 1; i <= BLOCKED_USERS_COUNT; i++) {
            UserEntity user = createTestUser(BigInteger.valueOf(i), "user" + i, UserStatus.BLOCKED);
            LocalDateTime lockTime = LocalDateTime.now().minusMinutes(LOCK_DURATION_10_MINUTES);
            user.setTemporaryLockTimestamp(Timestamp.valueOf(lockTime));
            blockedUsers.add(user);
        }

        when(userRepository.save(any(UserEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int unlockedCount = usersService.processBlockedUsersForScheduledUnlock(
            blockedUsers, TEMPORARY_LOCK_PERIOD_MINUTES);

        // Then
        assertEquals(BLOCKED_USERS_COUNT, unlockedCount);
        verify(userRepository, times(BLOCKED_USERS_COUNT)).save(any(UserEntity.class));
        verify(uidamMetricsService, times(BLOCKED_USERS_COUNT))
            .incrementCounter(any(MetricInfo.class));
    }

    @Test
    void testProcessBlockedUsersForScheduledUnlock_EmptyList() {
        // Given
        List<UserEntity> blockedUsers = new ArrayList<>();

        // When
        int unlockedCount = usersService.processBlockedUsersForScheduledUnlock(
            blockedUsers, TEMPORARY_LOCK_PERIOD_MINUTES);

        // Then
        assertEquals(0, unlockedCount);
        verify(userRepository, times(0)).save(any(UserEntity.class));
    }

    @Test
    void testProcessBlockedUsersForScheduledUnlock_PartialFailure() {
        // Given
        List<UserEntity> blockedUsers = new ArrayList<>();
        for (int i = 1; i <= BLOCKED_USERS_COUNT; i++) {
            UserEntity user = createTestUser(BigInteger.valueOf(i), "user" + i, UserStatus.BLOCKED);
            blockedUsers.add(user);
        }

        // Mock first save to fail, others to succeed
        when(userRepository.save(any(UserEntity.class)))
            .thenThrow(new RuntimeException("DB Error"))
            .thenAnswer(invocation -> invocation.getArgument(0))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int unlockedCount = usersService.processBlockedUsersForScheduledUnlock(
            blockedUsers, TEMPORARY_LOCK_PERIOD_MINUTES);

        // Then
        assertEquals(EXPONENTIAL_FACTOR, unlockedCount); // 1 failed, 2 succeeded
        verify(userRepository, times(BLOCKED_USERS_COUNT)).save(any(UserEntity.class));
    }

    // ==================== Tests for mapUserFields ====================

    @Test
    void testMapUserFields_UserEntity_Success() {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.ACTIVE);
        
        // When
        boolean result = usersService.mapUserFields(UserEntity.class, "firstName", user, "UpdatedName");

        // Then
        assertTrue(result);
        assertEquals("UpdatedName", user.getFirstName());
    }

    @Test
    void testMapUserFields_InvalidField_ReturnsFalse() {
        // Given
        UserEntity user = createTestUser(BigInteger.ONE, "testuser", UserStatus.ACTIVE);
        
        // When
        boolean result = usersService.mapUserFields(UserEntity.class, "nonExistentField", user, "value");

        // Then
        assertFalse(result);
    }

    @Test
    void testMapUserFields_UserAddressEntity_Success() {
        // Given
        UserAddressEntity address = new UserAddressEntity();
        
        // When
        boolean result = usersService.mapUserFields(
            UserAddressEntity.class, 
            "city", 
            address, 
            "NewCity"
        );

        // Then
        assertTrue(result);
        assertEquals("NewCity", address.getCity());
    }

    // ==================== Tests for mapUserAttributeValueEntityByAttributeId ====================

    @Test
    void testMapUserAttributeValueEntityByAttributeId_Success() {
        // Given
        final List<UserAttributeValueEntity> entities = new ArrayList<>();
        
        UserAttributeValueEntity entity1 = new UserAttributeValueEntity();
        entity1.setAttributeId(BigInteger.ONE);
        entity1.setValue("value1");
        
        UserAttributeValueEntity entity2 = new UserAttributeValueEntity();
        entity2.setAttributeId(BigInteger.TWO);
        entity2.setValue("value2");
        
        entities.add(entity1);
        entities.add(entity2);

        // When
        Map<BigInteger, UserAttributeValueEntity> result = 
            usersService.mapUserAttributeValueEntityByAttributeId(entities);

        // Then
        assertEquals(ATTRIBUTE_MAP_SIZE, result.size());
        assertEquals("value1", result.get(BigInteger.ONE).getValue());
        assertEquals("value2", result.get(BigInteger.TWO).getValue());
    }

    @Test
    void testMapUserAttributeValueEntityByAttributeId_EmptyList() {
        // Given
        List<UserAttributeValueEntity> entities = new ArrayList<>();

        // When
        Map<BigInteger, UserAttributeValueEntity> result = 
            usersService.mapUserAttributeValueEntityByAttributeId(entities);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testMapUserAttributeValueEntityByAttributeId_DuplicateKey_ThrowsException() {
        // Given
        final List<UserAttributeValueEntity> entities = new ArrayList<>();
        
        UserAttributeValueEntity entity1 = new UserAttributeValueEntity();
        entity1.setAttributeId(BigInteger.ONE);
        entity1.setValue("value1");
        
        UserAttributeValueEntity entity2 = new UserAttributeValueEntity();
        entity2.setAttributeId(BigInteger.ONE); // Same ID
        entity2.setValue("value2");
        
        entities.add(entity1);
        entities.add(entity2);

        // When & Then - Should throw IllegalStateException for duplicate keys
        assertThrows(IllegalStateException.class, () -> 
            usersService.mapUserAttributeValueEntityByAttributeId(entities));
    }
}
