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

import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserMfaBackupCodeEntity;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserMfaBackupCodeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserMfaSecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodeVerifyResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodesResponse;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the MFA backup-code generation, storage and verification logic in
 * {@link MfaManagementService}.
 */
@ExtendWith(MockitoExtension.class)
class MfaManagementBackupCodeTest {

    private static final String USERNAME = "John";
    private static final String NORMALIZED_USERNAME = "john";
    private static final BigInteger USER_ID = BigInteger.valueOf(42);
    private static final BigInteger BACKUP_CODE_ENTITY_ID = BigInteger.valueOf(100);
    private static final int CUSTOM_CODE_COUNT = 5;
    private static final int DEFAULT_CODE_COUNT = 8;
    private static final int TWO_CODES = 2;
    private static final int REMAINING_THREE = 3;
    private static final int REMAINING_TWO = 2;
    private static final int REMAINING_ONE = 1;
    private static final int REMAINING_ZERO = 0;
    private static final int REMAINING_FOUR = 4;

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

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(USER_ID);
        user.setUserName(NORMALIZED_USERNAME);
    }

    // ── isBackupCodesEnabled ──────────────────────────────────────────────────

    @Test
    @DisplayName("isBackupCodesEnabled returns tenant value when configured")
    void backupCodesEnabledHonoursTenantProperty() {
        UserManagementTenantProperties props = new UserManagementTenantProperties();
        props.setMfaBackupCodesEnabled(false);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(props);

        assertFalse(service.isBackupCodesEnabled());
    }

    @Test
    @DisplayName("isBackupCodesEnabled defaults to true when property absent")
    void backupCodesEnabledDefaultsTrue() {
        when(tenantConfigurationService.getTenantProperties())
                .thenReturn(new UserManagementTenantProperties());

        assertTrue(service.isBackupCodesEnabled());
    }

    @Test
    @DisplayName("isBackupCodesEnabled defaults to true when tenant lookup fails")
    void backupCodesEnabledDefaultsTrueOnError() {
        when(tenantConfigurationService.getTenantProperties())
                .thenThrow(new RuntimeException("no tenant"));

        assertTrue(service.isBackupCodesEnabled());
    }

    // ── generateBackupCodes ───────────────────────────────────────────────────

    @Test
    @DisplayName("generateBackupCodes produces the configured number of unique codes and stores hashes")
    void generateBackupCodesUsesTenantCount() throws ResourceNotFoundException {
        UserManagementTenantProperties props = new UserManagementTenantProperties();
        props.setMfaBackupCodesCount(CUSTOM_CODE_COUNT);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(props);
        when(usersRepository.findByUserName(NORMALIZED_USERNAME))
                .thenReturn(Collections.singletonList(user));

        MfaBackupCodesResponse response = service.generateBackupCodes(USERNAME);

        assertEquals(CUSTOM_CODE_COUNT, response.count());
        assertEquals(CUSTOM_CODE_COUNT, response.codes().size());
        // codes must be unique and 8-char alphanumeric
        assertEquals(CUSTOM_CODE_COUNT, response.codes().stream().distinct().count());
        response.codes().forEach(code -> assertTrue(code.matches("[A-Z0-9]{8}")));

        // old codes cleared, then 5 fresh codes persisted
        verify(mfaBackupCodeRepository).deleteAllForUser(NORMALIZED_USERNAME);
        verify(mfaBackupCodeRepository, times(CUSTOM_CODE_COUNT)).save(any(UserMfaBackupCodeEntity.class));
    }

    @Test
    @DisplayName("generateBackupCodes falls back to default count when tenant count missing")
    void generateBackupCodesDefaultsCount() throws ResourceNotFoundException {
        when(tenantConfigurationService.getTenantProperties())
                .thenReturn(new UserManagementTenantProperties());
        when(usersRepository.findByUserName(NORMALIZED_USERNAME))
                .thenReturn(Collections.singletonList(user));

        MfaBackupCodesResponse response = service.generateBackupCodes(USERNAME);

        assertEquals(DEFAULT_CODE_COUNT, response.count());
        assertEquals(DEFAULT_CODE_COUNT, response.codes().size());
    }

    @Test
    @DisplayName("generateBackupCodes persists only hashes, never plain text")
    void generateBackupCodesPersistsHashes() throws ResourceNotFoundException {
        UserManagementTenantProperties props = new UserManagementTenantProperties();
        props.setMfaBackupCodesCount(TWO_CODES);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(props);
        when(usersRepository.findByUserName(NORMALIZED_USERNAME))
                .thenReturn(Collections.singletonList(user));

        MfaBackupCodesResponse response = service.generateBackupCodes(USERNAME);

        ArgumentCaptor<UserMfaBackupCodeEntity> captor =
                ArgumentCaptor.forClass(UserMfaBackupCodeEntity.class);
        verify(mfaBackupCodeRepository, times(TWO_CODES)).save(captor.capture());

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        List<UserMfaBackupCodeEntity> saved = captor.getAllValues();
        for (int i = 0; i < saved.size(); i++) {
            UserMfaBackupCodeEntity entity = saved.get(i);
            assertFalse(entity.isUsed());
            assertEquals(user.getId(), entity.getUserId());
            assertEquals(NORMALIZED_USERNAME, entity.getUsername());
            // the stored hash is not the plain text but matches it
            assertFalse(response.codes().contains(entity.getCodeHash()));
            assertTrue(encoder.matches(response.codes().get(i), entity.getCodeHash()));
        }
    }

    @Test
    @DisplayName("generateBackupCodes throws when user does not exist")
    void generateBackupCodesUnknownUser() {
        when(usersRepository.findByUserName(NORMALIZED_USERNAME))
                .thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> service.generateBackupCodes(USERNAME));
        verify(mfaBackupCodeRepository, never()).save(any());
    }

    // ── verifyBackupCode ──────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyBackupCode consumes a matching code and reports remaining count")
    void verifyBackupCodeSuccess() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String plain = "ABCD2345";
        UserMfaBackupCodeEntity entity = backupEntity(encoder.encode(plain));
        when(mfaBackupCodeRepository.findByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn(new ArrayList<>(List.of(entity)));
        when(mfaBackupCodeRepository.consumeBackupCodeAtomically(eq(BACKUP_CODE_ENTITY_ID)))
                .thenReturn(1); // Successfully consumed (1 row affected)
        when(mfaBackupCodeRepository.countByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn((long) REMAINING_THREE);

        MfaBackupCodeVerifyResponse result = service.verifyBackupCode(USERNAME, plain);

        assertTrue(result.valid());
        assertEquals(REMAINING_THREE, result.remainingBackupCodes());
        assertFalse(result.lowBackupCodesWarning());
        verify(mfaBackupCodeRepository).consumeBackupCodeAtomically(eq(BACKUP_CODE_ENTITY_ID));
    }

    @Test
    @DisplayName("verifyBackupCode is case-insensitive and trims whitespace")
    void verifyBackupCodeCaseInsensitive() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String stored = "WXYZ6789";
        UserMfaBackupCodeEntity entity = backupEntity(encoder.encode(stored));
        when(mfaBackupCodeRepository.findByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn(new ArrayList<>(List.of(entity)));
        when(mfaBackupCodeRepository.consumeBackupCodeAtomically(eq(BACKUP_CODE_ENTITY_ID)))
                .thenReturn(1); // Successfully consumed (1 row affected)
        when(mfaBackupCodeRepository.countByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn((long) REMAINING_ZERO);

        MfaBackupCodeVerifyResponse result = service.verifyBackupCode(USERNAME, "  wxyz6789  ");

        assertTrue(result.valid());
        assertTrue(result.lowBackupCodesWarning());
        verify(mfaBackupCodeRepository).consumeBackupCodeAtomically(eq(BACKUP_CODE_ENTITY_ID));
    }

    @Test
    @DisplayName("verifyBackupCode returns invalid for non-matching code without consuming")
    void verifyBackupCodeFailure() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UserMfaBackupCodeEntity entity = backupEntity(encoder.encode("ABCD2345"));
        when(mfaBackupCodeRepository.findByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn(new ArrayList<>(List.of(entity)));
        when(mfaBackupCodeRepository.countByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn((long) REMAINING_ONE);

        MfaBackupCodeVerifyResponse result = service.verifyBackupCode(USERNAME, "ZZZZ9999");

        assertFalse(result.valid());
        assertEquals(REMAINING_ONE, result.remainingBackupCodes());
        assertTrue(result.lowBackupCodesWarning());
        assertFalse(entity.isUsed());
        verify(mfaBackupCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyBackupCode handles null input gracefully")
    void verifyBackupCodeNullInput() {
        when(mfaBackupCodeRepository.findByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn(Collections.emptyList());
        when(mfaBackupCodeRepository.countByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn((long) REMAINING_ZERO);

        MfaBackupCodeVerifyResponse result = service.verifyBackupCode(USERNAME, null);

        assertFalse(result.valid());
        assertEquals(REMAINING_ZERO, result.remainingBackupCodes());
    }

    @Test
    @DisplayName("verifyBackupCode detects race condition when another transaction consumes code first")
    void verifyBackupCodeRaceCondition() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String plain = "RACE0001";
        UserMfaBackupCodeEntity entity = backupEntity(encoder.encode(plain));
        when(mfaBackupCodeRepository.findByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn(new ArrayList<>(List.of(entity)));
        // Simulate race condition: another transaction consumed this code before us
        when(mfaBackupCodeRepository.consumeBackupCodeAtomically(eq(BACKUP_CODE_ENTITY_ID)))
                .thenReturn(0); // No rows affected (code already consumed)
        when(mfaBackupCodeRepository.countByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn((long) REMAINING_TWO);

        MfaBackupCodeVerifyResponse result = service.verifyBackupCode(USERNAME, plain);

        assertFalse(result.valid(), "Race condition should return invalid");
        assertEquals(REMAINING_TWO, result.remainingBackupCodes());
        assertTrue(result.lowBackupCodesWarning(), "Low codes warning when 2 remaining (at threshold)");
        // Verify atomic consumption was attempted
        verify(mfaBackupCodeRepository).consumeBackupCodeAtomically(eq(BACKUP_CODE_ENTITY_ID));
        // Verify remaining count was checked after race condition detected
        verify(mfaBackupCodeRepository).countByUsernameAndUsedFalse(NORMALIZED_USERNAME);
    }

    // ── getRemainingBackupCodeCount ───────────────────────────────────────────

    @Test
    @DisplayName("getRemainingBackupCodeCount delegates to repository with normalized username")
    void remainingBackupCodeCount() {
        when(mfaBackupCodeRepository.countByUsernameAndUsedFalse(NORMALIZED_USERNAME))
                .thenReturn((long) REMAINING_FOUR);

        assertEquals(REMAINING_FOUR, service.getRemainingBackupCodeCount(USERNAME));
        verify(mfaBackupCodeRepository).countByUsernameAndUsedFalse(NORMALIZED_USERNAME);
    }

    private UserMfaBackupCodeEntity backupEntity(String hash) {
        UserMfaBackupCodeEntity entity = new UserMfaBackupCodeEntity();
        entity.setId(BACKUP_CODE_ENTITY_ID);
        entity.setUserId(user.getId());
        entity.setUsername(NORMALIZED_USERNAME);
        entity.setCodeHash(hash);
        entity.setUsed(false);
        return entity;
    }
}
