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

package org.eclipse.ecsp.uidam.security.policy.handler;

import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationInput;
import org.eclipse.ecsp.uidam.usermanagement.entity.PasswordHistoryEntity;
import org.eclipse.ecsp.uidam.usermanagement.repository.PasswordHistoryRepository;
import org.eclipse.ecsp.uidam.usermanagement.utilities.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ExpirationPolicyHandler class.
 */
class ExpirationPolicyHandlerTest {

    private static final int INT_5 = 5;
    private static final int INT_90 = 90;
    private static final int INT_91 = 91;
    private static final int INT_30 = 30;
    private PasswordHistoryRepository passwordHistoryRepository;
    private ExpirationPolicyHandler expirationPolicyHandler;
    private Map<String, Object> rules;
    private static final String PASSWORD_ENCODER = "mockEncoder";

    @BeforeEach
    void setUp() {
        passwordHistoryRepository = mock(PasswordHistoryRepository.class);
        rules = new HashMap<>();
        rules.put("passwordHistoryCount", INT_5);
        rules.put("passwordExpiryDays", INT_90);
        expirationPolicyHandler = new ExpirationPolicyHandler(rules, passwordHistoryRepository, PASSWORD_ENCODER);
    }

    @Test
    void testDoHandle_PasswordExpired() {
        PasswordValidationInput input = mock(PasswordValidationInput.class);
        when(input.username()).thenReturn("testUser");
        when(input.password()).thenReturn(null);

        Date lastPasswordChangeDate = Date
                .from(LocalDate.now().minusDays(INT_91).atStartOfDay(ZoneId.systemDefault()).toInstant());
        when(passwordHistoryRepository.findLastPasswordChangeDate("testUser")).thenReturn(lastPasswordChangeDate);

        boolean result = expirationPolicyHandler.doHandle(input);

        assertFalse(result);
        verify(passwordHistoryRepository, times(1)).findLastPasswordChangeDate("testUser");
    }

    @Test
    void testDoHandle_PasswordNotExpired() {
        PasswordValidationInput input = mock(PasswordValidationInput.class);
        when(input.username()).thenReturn("testUser");
        when(input.password()).thenReturn(null);

        Date lastPasswordChangeDate = Date
                .from(LocalDate.now().minusDays(INT_30).atStartOfDay(ZoneId.systemDefault()).toInstant());
        when(passwordHistoryRepository.findLastPasswordChangeDate("testUser")).thenReturn(lastPasswordChangeDate);

        boolean result = expirationPolicyHandler.doHandle(input);

        assertTrue(result);
        verify(passwordHistoryRepository, times(1)).findLastPasswordChangeDate("testUser");
    }

    @Test
    void testDoHandle_PasswordHistoryValidationFails() {
        PasswordValidationInput input = mock(PasswordValidationInput.class);

        when(input.password()).thenReturn("newPassword");
        when(input.username()).thenReturn("testUser");
        when(input.password()).thenReturn("newPassword");
        PasswordHistoryEntity pwdHistory1 = new PasswordHistoryEntity();
        pwdHistory1.setPasswordSalt("salt1");
        pwdHistory1.setUserPassword("hashedPassword1");
        PasswordHistoryEntity pwdHistory2 = new PasswordHistoryEntity();
        pwdHistory2.setPasswordSalt("salt2");
        pwdHistory2.setUserPassword("hashedPassword2");
        List<PasswordHistoryEntity> passwordHistoryEntities = List.of(pwdHistory1, pwdHistory2);
        when(passwordHistoryRepository.findPasswordHistoryByUserName(eq("testUser"), any()))
                .thenReturn(passwordHistoryEntities);

        try (MockedStatic<PasswordUtils> pwdUtil = mockStatic(PasswordUtils.class)) {
            pwdUtil.when(
                    () -> PasswordUtils.isPasswordValid(eq(PASSWORD_ENCODER), eq("newPassword"), anyList(), anyList()))
                    .thenReturn(false);

            boolean result = expirationPolicyHandler.doHandle(input);

            assertFalse(result);
        }
        verify(passwordHistoryRepository, times(1)).findPasswordHistoryByUserName(eq("testUser"), any());
    }

    @Test
    void testDoHandle_PasswordHistoryValidationPasses() {
        PasswordValidationInput input = mock(PasswordValidationInput.class);
        when(input.username()).thenReturn("testUser");
        when(input.password()).thenReturn("newPassword");

        PasswordHistoryEntity pwdHistory1 = new PasswordHistoryEntity();
        pwdHistory1.setPasswordSalt("salt1");
        pwdHistory1.setUserPassword("hashedPassword1");
        PasswordHistoryEntity pwdHistory2 = new PasswordHistoryEntity();
        pwdHistory2.setPasswordSalt("salt2");

        List<PasswordHistoryEntity> passwordHistoryEntities = List.of(pwdHistory1, pwdHistory2);
        when(passwordHistoryRepository.findPasswordHistoryByUserName(eq("testUser"), any()))
                .thenReturn(passwordHistoryEntities);

        when(passwordHistoryRepository.findPasswordHistoryByUserName(eq("testUser"), any()))
                .thenReturn(passwordHistoryEntities);

        try (MockedStatic<PasswordUtils> pwdUtil = mockStatic(PasswordUtils.class)) {
            pwdUtil.when(
                    () -> PasswordUtils.isPasswordValid(eq(PASSWORD_ENCODER), eq("newPassword"), anyList(), anyList()))
                    .thenReturn(true);

            boolean result = expirationPolicyHandler.doHandle(input);

            assertTrue(result);
        }
        verify(passwordHistoryRepository, times(1)).findPasswordHistoryByUserName(eq("testUser"), any());
    }

    @Test
    void testDoHandle_NoPasswordHistory() {
        PasswordValidationInput input = mock(PasswordValidationInput.class);
        when(input.username()).thenReturn("testUser");
        when(input.password()).thenReturn("newPassword");

        when(passwordHistoryRepository.findPasswordHistoryByUserName(eq("testUser"), any()))
                .thenReturn(Collections.emptyList());

        boolean result = expirationPolicyHandler.doHandle(input);

        assertTrue(result);
        verify(passwordHistoryRepository, times(1)).findPasswordHistoryByUserName(eq("testUser"), any());
    }
}