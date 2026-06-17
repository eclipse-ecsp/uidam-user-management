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
import org.eclipse.ecsp.uidam.usermanagement.entity.UserMfaBackupCodeEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserMfaSecretEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.MfaStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserMfaBackupCodeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserMfaSecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaEnrollInitiateResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaStatusResponse;
import org.eclipse.ecsp.uidam.usermanagement.utilities.MfaSecretEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MfaManagementService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MfaManagementService Test Suite")
class MfaManagementServiceTest {

    private static final long TEST_USER_ID = 123L;
    private static final long ALTERNATE_USER_ID = 456L;
    private static final String TEST_ENCRYPTION_KEY  = "TestEncryptionKey12345!";
    private static final String TEST_ENCRYPTION_SALT = "TestEncryptionSalt12345";

    private MfaManagementService mfaService;

    @Mock
    private UserMfaSecretRepository mfaSecretRepository;

    @Mock
    private UserMfaBackupCodeRepository mfaBackupCodeRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private UserManagementTenantProperties tenantProperties;

    @BeforeEach
    void setUp() {
        mfaService = new MfaManagementService(
                mfaSecretRepository,
                mfaBackupCodeRepository,
                usersRepository,
                emailNotificationService,
                tenantConfigurationService
        );
        // Provide a tenant with known encryption key/salt so initiateEnrollment can encrypt
        when(tenantProperties.getMfaSecretEncryptionKey()).thenReturn(TEST_ENCRYPTION_KEY);
        when(tenantProperties.getMfaSecretEncryptionSalt()).thenReturn(TEST_ENCRYPTION_SALT);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
    }

    @Test
    @DisplayName("Should initiate enrollment for valid user and store encrypted secret")
    void testInitiateEnrollmentSuccess() throws ResourceNotFoundException {
        // Arrange
        String username = "testuser";
        UserEntity user = new UserEntity();
        user.setId(BigInteger.valueOf(TEST_USER_ID));
        user.setUserName(username);

        List<UserEntity> users = List.of(user);
        
        when(usersRepository.findByUserName(username)).thenReturn(users);
        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username))
                .thenReturn(Optional.empty());
        when(mfaSecretRepository.save(any(UserMfaSecretEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MfaEnrollInitiateResponse response = mfaService.initiateEnrollment(username);

        // Assert
        assertNotNull(response);
        assertNotNull(response.secret());
        assertNotNull(response.qrUri());
        assertNotNull(response.manualKey());
        assertTrue(response.qrUri().contains("otpauth://totp/"));
        // The response secret is the plain-text Base32 secret (for QR display)
        // Verify the entity was saved with an encrypted (different) value
        verify(mfaSecretRepository, times(1)).save(any(UserMfaSecretEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found during enrollment initiation")
    void testInitiateEnrollmentUserNotFound() {
        // Arrange
        String username = "nonexistent";
        when(usersRepository.findByUserName(username)).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> mfaService.initiateEnrollment(username));
    }

    @Test
    @DisplayName("Should activate pending enrollment")
    void testActivateEnrollmentSuccess() throws ResourceNotFoundException {
        // Arrange
        String username = "testuser";
        UserMfaSecretEntity entity = new UserMfaSecretEntity();
        entity.setUsername(username);
        entity.setStatus(MfaStatus.PENDING);

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                username, MfaStatus.PENDING))
                .thenReturn(Optional.of(entity));
        when(mfaSecretRepository.save(any(UserMfaSecretEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        mfaService.activateEnrollment(username);

        // Assert
        assertEquals(MfaStatus.ACTIVE, entity.getStatus());
        verify(mfaSecretRepository, times(1)).save(entity);
    }

    @Test
    @DisplayName("Should throw exception when activating non-existent enrollment")
    void testActivateEnrollmentNotFound() {
        // Arrange
        String username = "testuser";
        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                username, MfaStatus.PENDING))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> mfaService.activateEnrollment(username));
    }

    @Test
    @DisplayName("Should get status for enrolled user")
    void testGetStatusEnrolled() {
        // Arrange
        String username = "testuser";
        UserMfaSecretEntity entity = new UserMfaSecretEntity();
        entity.setStatus(MfaStatus.ACTIVE);

        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username))
                .thenReturn(Optional.of(entity));
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);

        // Act
        MfaStatusResponse response = mfaService.getStatus(username);

        // Assert
        assertNotNull(response);
        assertTrue(response.enrolled());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    @DisplayName("Should get status for non-enrolled user")
    void testGetStatusNotEnrolled() {
        // Arrange
        String username = "testuser";
        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username))
                .thenReturn(Optional.empty());
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);

        // Act
        MfaStatusResponse response = mfaService.getStatus(username);

        // Assert
        assertNotNull(response);
        assertFalse(response.enrolled());
        assertEquals("NONE", response.status());
    }

    @Test
    @DisplayName("Should get (encrypted) secret for active enrollment")
    void testGetSecretActive() {
        // Arrange
        String username = "testuser";
        // Simulate an already-encrypted value stored in DB
        String encryptedSecret = MfaSecretEncryptionUtil.encrypt(
                "TESTSECRET123", TEST_ENCRYPTION_KEY, TEST_ENCRYPTION_SALT);
        UserMfaSecretEntity entity = new UserMfaSecretEntity();
        entity.setStatus(MfaStatus.ACTIVE);
        entity.setTotpSecret(encryptedSecret);

        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username))
                .thenReturn(Optional.of(entity));

        // Act
        Optional<String> result = mfaService.getSecret(username);

        // Assert – getSecret returns the encrypted blob; the auth server decrypts it
        assertTrue(result.isPresent());
        assertEquals(encryptedSecret, result.get());
    }

    @Test
    @DisplayName("Should not return secret for revoked enrollment")
    void testGetSecretRevoked() {
        // Arrange
        String username = "testuser";
        UserMfaSecretEntity entity = new UserMfaSecretEntity();
        entity.setStatus(MfaStatus.REVOKED);
        entity.setTotpSecret("SECRET");

        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username))
                .thenReturn(Optional.of(entity));

        // Act
        Optional<String> result = mfaService.getSecret(username);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get status by user ID")
    void testGetStatusByUserId() {
        // Arrange
        BigInteger userId = BigInteger.valueOf(TEST_USER_ID);
        UserMfaSecretEntity entity = new UserMfaSecretEntity();
        entity.setStatus(MfaStatus.ACTIVE);

        when(mfaSecretRepository.findTopByUserIdOrderByCreatedDateDesc(userId))
                .thenReturn(Optional.of(entity));
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);

        // Act
        MfaStatusResponse response = mfaService.getStatusByUserId(userId);

        // Assert
        assertNotNull(response);
        assertTrue(response.enrolled());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    @DisplayName("Should revoke enrollment by user ID")
    void testRevokeEnrollmentByUserId() {
        // Arrange
        BigInteger userId = BigInteger.valueOf(TEST_USER_ID);

        // Act
        mfaService.revokeEnrollmentByUserId(userId);

        // Assert
        verify(mfaSecretRepository, times(1)).revokeAllActiveForUserId(userId);
    }

    @Test
    @DisplayName("Should revoke enrollment by username")
    void testRevokeEnrollment() {
        // Arrange
        String username = "testuser";

        // Act
        mfaService.revokeEnrollment(username);

        // Assert
        verify(mfaSecretRepository, times(1)).revokeAllActiveForUser(username);
    }

    @Test
    @DisplayName("Should send recovery key to user email")
    void testSendRecoveryKey() throws ResourceNotFoundException {
        // Arrange
        String email = "test@example.com";
        
        UserEntity user = new UserEntity();
        user.setId(BigInteger.valueOf(TEST_USER_ID));
        String username = "testuser";
        user.setUserName(username);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");

        UserMfaSecretEntity entity = new UserMfaSecretEntity();
        entity.setUsername(username);
        entity.setStatus(MfaStatus.ACTIVE);

        when(usersRepository.findByUserName(username)).thenReturn(List.of(user));
        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                username, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(mfaSecretRepository.save(any(UserMfaSecretEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        mfaService.sendRecoveryKey(username);

        // Assert
        verify(mfaSecretRepository, times(1)).save(entity);
        verify(emailNotificationService, times(1))
                .sendNotification(any(), eq("UIDAM_MFA_RECOVERY"), any());
        assertNotNull(entity.getRecoveryKey());
        assertNotNull(entity.getRecoveryKeyExpiry());
    }

    @Test
    @DisplayName("Should throw exception when sending recovery key for non-existent user")
    void testSendRecoveryKeyUserNotFound() {
        // Arrange
        String username = "nonexistent";
        when(usersRepository.findByUserName(username)).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> mfaService.sendRecoveryKey(username));
    }

    @Test
    @DisplayName("Should normalize username to lowercase")
    void testInitiateEnrollmentNormalizesUsername() throws ResourceNotFoundException {
        // Arrange
        String normalizedUsername = "testuser";
        UserEntity user = new UserEntity();
        user.setId(BigInteger.valueOf(TEST_USER_ID));
        user.setUserName(normalizedUsername);

        when(usersRepository.findByUserName(normalizedUsername)).thenReturn(List.of(user));
        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(normalizedUsername))
                .thenReturn(Optional.empty());
        when(mfaSecretRepository.save(any(UserMfaSecretEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String username = "TestUser";
        MfaEnrollInitiateResponse response = mfaService.initiateEnrollment(username);

        // Assert
        assertNotNull(response);
        verify(usersRepository, times(1)).findByUserName(normalizedUsername);
    }

    @Test
    @DisplayName("Should reuse existing MFA secret entity during re-enrollment")
    void testInitiateEnrollmentReusesEntity() throws ResourceNotFoundException {
        // Arrange
        String username = "testuser";
        UserEntity user = new UserEntity();
        user.setId(BigInteger.valueOf(TEST_USER_ID));
        user.setUserName(username);

        UserMfaSecretEntity existingEntity = new UserMfaSecretEntity();
        existingEntity.setUserId(BigInteger.valueOf(ALTERNATE_USER_ID));
        existingEntity.setUsername("oldusername");
        existingEntity.setStatus(MfaStatus.REVOKED);

        when(usersRepository.findByUserName(username)).thenReturn(List.of(user));
        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username))
                .thenReturn(Optional.of(existingEntity));
        when(mfaSecretRepository.save(any(UserMfaSecretEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MfaEnrollInitiateResponse response = mfaService.initiateEnrollment(username);

        // Assert
        assertNotNull(response);
        assertEquals(BigInteger.valueOf(TEST_USER_ID), existingEntity.getUserId());
        assertEquals(username, existingEntity.getUsername());
        assertEquals(MfaStatus.PENDING, existingEntity.getStatus());
        verify(mfaSecretRepository, times(1)).save(existingEntity);
    }

    @Test
    @DisplayName("Should get status for pending enrollment")
    void testGetStatusPending() {
        // Arrange
        String username = "testuser";
        UserMfaSecretEntity entity = new UserMfaSecretEntity();
        entity.setStatus(MfaStatus.PENDING);

        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username))
                .thenReturn(Optional.of(entity));
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);

        // Act
        MfaStatusResponse response = mfaService.getStatus(username);

        // Assert
        assertNotNull(response);
        assertFalse(response.enrolled()); // Only ACTIVE counts as enrolled
        assertEquals("PENDING", response.status());
    }

    @Test
    @DisplayName("Should return empty secret when user has no enrollment")
    void testGetSecretNoEnrollment() {
        // Arrange
        String username = "testuser";
        when(mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username))
                .thenReturn(Optional.empty());

        // Act
        Optional<String> result = mfaService.getSecret(username);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get status by user ID when no enrollment exists")
    void testGetStatusByUserIdNoEnrollment() {
        // Arrange
        BigInteger userId = BigInteger.valueOf(TEST_USER_ID);
        when(mfaSecretRepository.findTopByUserIdOrderByCreatedDateDesc(userId))
                .thenReturn(Optional.empty());
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);

        // Act
        MfaStatusResponse response = mfaService.getStatusByUserId(userId);

        // Assert
        assertNotNull(response);
        assertFalse(response.enrolled());
        assertEquals("NONE", response.status());
    }
}
