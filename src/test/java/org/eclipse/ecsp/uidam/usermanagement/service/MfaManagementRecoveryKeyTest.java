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
 */

package org.eclipse.ecsp.uidam.usermanagement.service;

import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserMfaSecretEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.MfaStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserMfaBackupCodeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserMfaSecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the security-sensitive MFA recovery-key generation, storage,
 * verification and enrollment revocation logic in {@link MfaManagementService}.
 *
 * <p>These tests cover account-takeover-risk flows:
 * <ul>
 *   <li>Recovery key generation with time-bounded expiry</li>
 *   <li>Hash-based verification (plain text never stored)</li>
 *   <li>Enrollment revocation on successful key verification</li>
 *   <li>Defensive input normalization (trim, upper-case, null safety)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MfaManagementRecoveryKeyTest {

    private static final String USERNAME = "Alice";
    private static final String NORMALIZED_USERNAME = "alice";
    private static final BigInteger USER_ID = BigInteger.valueOf(123);
    private static final String USER_EMAIL = "alice@example.com";
    private static final String USER_FIRST_NAME = "Alice";
    private static final String USER_LAST_NAME = "Smith";
    private static final int RECOVERY_KEY_EXPIRY_MINUTES = 10;
    private static final int RECOVERY_KEY_LENGTH = 6;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int EXPIRY_TEST_TOLERANCE_SECONDS = 5;
    private static final int FIVE_MINUTES_FUTURE = 5;
    private static final int FIVE_MINUTES_PAST = -5;

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

    @InjectMocks
    private MfaManagementService service;

    private UserEntity user;
    private UserMfaSecretEntity mfaEntity;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();

        user = new UserEntity();
        user.setId(USER_ID);
        user.setUserName(NORMALIZED_USERNAME);
        user.setEmail(USER_EMAIL);
        user.setFirstName(USER_FIRST_NAME);
        user.setLastName(USER_LAST_NAME);

        mfaEntity = new UserMfaSecretEntity();
        mfaEntity.setId(BigInteger.valueOf(1));
        mfaEntity.setUserId(USER_ID);
        mfaEntity.setUsername(NORMALIZED_USERNAME);
        mfaEntity.setStatus(MfaStatus.ACTIVE);
        mfaEntity.setTotpSecret("JBSWY3DPEBLW64TMMQ======");
    }

    // ── sendRecoveryKey ───────────────────────────────────────────────────────

    @Test
    @DisplayName("sendRecoveryKey generates a 6-character key and sends email with plain text")
    void sendRecoveryKeySuccess() throws ResourceNotFoundException {
        when(usersRepository.findByUserName(NORMALIZED_USERNAME))
                .thenReturn(Collections.singletonList(user));
        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        service.sendRecoveryKey(USERNAME);

        // Verify recovery key was stored (as hash, not plain text)
        ArgumentCaptor<UserMfaSecretEntity> captor = ArgumentCaptor.forClass(UserMfaSecretEntity.class);
        verify(mfaSecretRepository).save(captor.capture());
        UserMfaSecretEntity saved = captor.getValue();

        assertNotNull(saved.getRecoveryKey());
        assertNotNull(saved.getRecoveryKeyExpiry());
        // The stored recovery key should be a BCrypt hash, not plain text
        // We can't directly verify the plain text, but we can verify it was set
        assertTrue(saved.getRecoveryKey().startsWith("$2a$") || saved.getRecoveryKey().startsWith("$2b$"));

        // Verify email notification was sent with the user's contact info
        ArgumentCaptor<Map<String, String>> userDetailsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailNotificationService).sendNotification(
                userDetailsCaptor.capture(),
                anyString(),
                dataCaptor.capture());

        Map<String, String> userDetails = userDetailsCaptor.getValue();
        assertEquals(USER_EMAIL, userDetails.get("emailAddress"));
        assertEquals(USER_FIRST_NAME + " " + USER_LAST_NAME, userDetails.get("name"));

        Map<String, Object> notificationData = dataCaptor.getValue();
        assertEquals(USER_FIRST_NAME + " " + USER_LAST_NAME, notificationData.get("name"));
        // Recovery code should be 6 characters
        String recoveryCode = (String) notificationData.get("recoveryCode");
        assertEquals(RECOVERY_KEY_LENGTH, recoveryCode.length());
        assertTrue(recoveryCode.matches("[A-Z0-9]{6}"));
    }

    @Test
    @DisplayName("sendRecoveryKey sets expiry to 10 minutes from current time")
    void sendRecoveryKeyExpiryTenMinutes() throws ResourceNotFoundException {
        when(usersRepository.findByUserName(NORMALIZED_USERNAME))
                .thenReturn(Collections.singletonList(user));
        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        Instant beforeCall = Instant.now();
        service.sendRecoveryKey(USERNAME);
        Instant afterCall = Instant.now();

        ArgumentCaptor<UserMfaSecretEntity> captor = ArgumentCaptor.forClass(UserMfaSecretEntity.class);
        verify(mfaSecretRepository).save(captor.capture());
        UserMfaSecretEntity saved = captor.getValue();

        Timestamp expiry = saved.getRecoveryKeyExpiry();
        Instant expiryInstant = expiry.toInstant();

        // Expiry should be ~10 minutes (600 seconds) from now, with some tolerance for execution time
        long secondsDifference = ChronoUnit.SECONDS.between(beforeCall, expiryInstant);
        // Allow 5-second tolerance for test execution time
        long minSeconds = RECOVERY_KEY_EXPIRY_MINUTES * SECONDS_PER_MINUTE - EXPIRY_TEST_TOLERANCE_SECONDS;
        long maxSeconds = RECOVERY_KEY_EXPIRY_MINUTES * SECONDS_PER_MINUTE + EXPIRY_TEST_TOLERANCE_SECONDS;
        assertTrue(secondsDifference >= minSeconds,
                "Recovery key expiry should be ~10 minutes from now, got " + secondsDifference + " seconds");
        assertTrue(secondsDifference <= maxSeconds,
                "Recovery key expiry should be ~10 minutes from now, got " + secondsDifference + " seconds");
    }

    @Test
    @DisplayName("sendRecoveryKey throws when user not found")
    void sendRecoveryKeyUserNotFound() {
        when(usersRepository.findByUserName(NORMALIZED_USERNAME))
                .thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> service.sendRecoveryKey(USERNAME));
        verify(mfaSecretRepository, never()).save(any());
        verify(emailNotificationService, never()).sendNotification(any(), anyString(), any());
    }

    @Test
    @DisplayName("sendRecoveryKey throws when no active MFA enrollment found")
    void sendRecoveryKeyNoActiveMfaEnrollment() {
        when(usersRepository.findByUserName(NORMALIZED_USERNAME))
                .thenReturn(Collections.singletonList(user));
        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.sendRecoveryKey(USERNAME));
        verify(mfaSecretRepository, never()).save(any());
        verify(emailNotificationService, never()).sendNotification(any(), anyString(), any());
    }

    // ── verifyRecoveryKeyAndRevoke ────────────────────────────────────────────

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke accepts valid non-expired key and revokes enrollment")
    void verifyRecoveryKeySuccess() throws ResourceNotFoundException {
        // Create a valid recovery key
        String plainKey = "ABC123";
        String encodedKey = encoder.encode(plainKey);
        mfaEntity.setRecoveryKey(encodedKey);
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_FUTURE, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, plainKey);

        assertTrue(result, "Recovery key verification should succeed");
        verify(mfaSecretRepository).revokeAllActiveForUser(NORMALIZED_USERNAME);
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke is case-insensitive")
    void verifyRecoveryKeySuccessCaseInsensitive() throws ResourceNotFoundException {
        String plainKey = "xyz789";
        String encodedKey = encoder.encode(plainKey.toUpperCase());
        mfaEntity.setRecoveryKey(encodedKey);
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_FUTURE, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        // User enters lowercase key, should match because of upper-case normalization
        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, "xyz789");

        assertTrue(result, "Recovery key verification should be case-insensitive");
        verify(mfaSecretRepository).revokeAllActiveForUser(NORMALIZED_USERNAME);
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke trims whitespace from input")
    void verifyRecoveryKeySuccessTrimWhitespace() throws ResourceNotFoundException {
        String plainKey = "DEF456";
        String encodedKey = encoder.encode(plainKey);
        mfaEntity.setRecoveryKey(encodedKey);
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_FUTURE, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        // User enters key with leading/trailing whitespace (common in copy/paste)
        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, "  def456  ");

        assertTrue(result, "Recovery key verification should trim whitespace");
        verify(mfaSecretRepository).revokeAllActiveForUser(NORMALIZED_USERNAME);
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke rejects expired key")
    void verifyRecoveryKeyExpired() throws ResourceNotFoundException {
        String plainKey = "GHI789";
        String encodedKey = encoder.encode(plainKey);
        mfaEntity.setRecoveryKey(encodedKey);
        // Expiry is 5 minutes in the past
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_PAST, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, plainKey);

        assertFalse(result, "Recovery key verification should fail for expired key");
        verify(mfaSecretRepository, never()).revokeAllActiveForUser(anyString());
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke rejects mismatched key")
    void verifyRecoveryKeyMismatch() throws ResourceNotFoundException {
        final String storedKey = "JKL012";
        final String enteredKey = "JKL999";
        String encodedKey = encoder.encode(storedKey);
        mfaEntity.setRecoveryKey(encodedKey);
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_FUTURE, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, enteredKey);

        assertFalse(result, "Recovery key verification should fail for mismatched key");
        verify(mfaSecretRepository, never()).revokeAllActiveForUser(anyString());
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke handles null key input gracefully (defensive)")
    void verifyRecoveryKeyNullInput() throws ResourceNotFoundException {
        String encodedKey = encoder.encode("MNO345");
        mfaEntity.setRecoveryKey(encodedKey);
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_FUTURE, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        // Null input should not cause NullPointerException, but should fail verification
        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, null);

        assertFalse(result, "Recovery key verification should fail for null input (treated as empty string)");
        verify(mfaSecretRepository, never()).revokeAllActiveForUser(anyString());
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke handles blank key input gracefully (defensive)")
    void verifyRecoveryKeyBlankInput() throws ResourceNotFoundException {
        String encodedKey = encoder.encode("PQR678");
        mfaEntity.setRecoveryKey(encodedKey);
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_FUTURE, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        // Blank input should fail verification
        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, "   ");

        assertFalse(result, "Recovery key verification should fail for blank input");
        verify(mfaSecretRepository, never()).revokeAllActiveForUser(anyString());
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke rejects when no recovery key was ever issued")
    void verifyRecoveryKeyNeverIssued() throws ResourceNotFoundException {
        // MFA entity has no recovery key set
        mfaEntity.setRecoveryKey(null);
        mfaEntity.setRecoveryKeyExpiry(null);

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, "STU901");

        assertFalse(result, "Recovery key verification should fail when no key was issued");
        verify(mfaSecretRepository, never()).revokeAllActiveForUser(anyString());
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke rejects when recovery key is null but expiry is set (inconsistent state)")
    void verifyRecoveryKeyNullKeyButSetExpiry() throws ResourceNotFoundException {
        // Edge case: key is null but expiry was set (shouldn't happen, but defensive check)
        mfaEntity.setRecoveryKey(null);
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_FUTURE, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, "VWX234");

        assertFalse(result, "Recovery key verification should fail when key is null");
        verify(mfaSecretRepository, never()).revokeAllActiveForUser(anyString());
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke rejects when expiry is null but key is set (inconsistent state)")
    void verifyRecoveryKeySetKeyButNullExpiry() throws ResourceNotFoundException {
        // Edge case: key is set but expiry is null (shouldn't happen, but defensive check)
        String encodedKey = encoder.encode("YZA567");
        mfaEntity.setRecoveryKey(encodedKey);
        mfaEntity.setRecoveryKeyExpiry(null);

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        boolean result = service.verifyRecoveryKeyAndRevoke(USERNAME, "YZA567");

        assertFalse(result, "Recovery key verification should fail when expiry is null");
        verify(mfaSecretRepository, never()).revokeAllActiveForUser(anyString());
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke throws when no active MFA enrollment found")
    void verifyRecoveryKeyNoActiveMfaEnrollment() {
        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.verifyRecoveryKeyAndRevoke(USERNAME, "BCD890"));
        verify(mfaSecretRepository, never()).revokeAllActiveForUser(anyString());
    }

    @Test
    @DisplayName("verifyRecoveryKeyAndRevoke normalizes username before lookup")
    void verifyRecoveryKeyNormalizesUsername() throws ResourceNotFoundException {
        String plainKey = "EFG123";
        String encodedKey = encoder.encode(plainKey);
        mfaEntity.setRecoveryKey(encodedKey);
        mfaEntity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(FIVE_MINUTES_FUTURE, ChronoUnit.MINUTES)));

        when(mfaSecretRepository.findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE))
                .thenReturn(Optional.of(mfaEntity));

        // Input username in mixed case; should normalize to lowercase for lookup
        boolean result = service.verifyRecoveryKeyAndRevoke("ALICE", plainKey);

        assertTrue(result, "Recovery key verification should normalize username");
        verify(mfaSecretRepository).findTopByUsernameAndStatusOrderByCreatedDateDesc(
                NORMALIZED_USERNAME, MfaStatus.ACTIVE);
    }
}
