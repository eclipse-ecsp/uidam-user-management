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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastUpdateValidationPolicyHandlerTest {

    private static final int INT_120 = 120;
    private static final long LONG_1000L = 1000L;
    private static final int INT_30 = 30;
    private static final int INT_60 = 60;
    private LastUpdateValidationPolicyHandler handler;
    private Map<String, Object> rules;

    @BeforeEach
    void setUp() {
        rules = new HashMap<>();
        rules.put("passwordUpdateTimeIntervalSec", INT_60); // 60 seconds
        handler = new LastUpdateValidationPolicyHandler(rules);
    }

    @Test
    void testDoHandle_PasswordRecentlyUpdated() {
        // Mock input
        PasswordValidationService.PasswordValidationInput input = Mockito
                .mock(PasswordValidationService.PasswordValidationInput.class);
        Mockito.when(input.lastUpdateTime())
                .thenReturn(new Timestamp(System.currentTimeMillis() - INT_30 * LONG_1000L)); // 30 seconds ago
        Mockito.when(input.username()).thenReturn("testUser");

        // Test
        boolean result = handler.doHandle(input);

        // Verify
        assertFalse(result, "Password should not be allowed to change as it was updated recently.");
    }

    @Test
    void testDoHandle_PasswordUpdateAllowed() {
        // Mock input
        PasswordValidationService.PasswordValidationInput input = Mockito
                .mock(PasswordValidationService.PasswordValidationInput.class);
        Mockito.when(input.lastUpdateTime())
                .thenReturn(new Timestamp(System.currentTimeMillis() - INT_120 * LONG_1000L)); // 120 seconds ago
        Mockito.when(input.username()).thenReturn("testUser");

        // Test
        boolean result = handler.doHandle(input);

        // Verify
        assertTrue(result, "Password should be allowed to change as sufficient time has passed.");
    }

    @Test
    void testDoHandle_NoLastUpdateTime() {
        // Mock input
        PasswordValidationService.PasswordValidationInput input = Mockito
                .mock(PasswordValidationService.PasswordValidationInput.class);
        Mockito.when(input.lastUpdateTime()).thenReturn(null);
        Mockito.when(input.username()).thenReturn("testUser");

        // Test
        boolean result = handler.doHandle(input);

        // Verify
        assertTrue(result, "Password should be allowed to change if no last update time is provided.");
    }
}