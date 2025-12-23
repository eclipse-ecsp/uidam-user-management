/*
 * Copyright (c) 2024 - 2025 Harman International
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

package org.eclipse.ecsp.uidam.audit.context;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AccountTargetContext.
 */
class AccountTargetContextTest {

    private static final int EXPECTED_MAP_SIZE_WITH_ALL_FIELDS = 4;
    private static final int EXPECTED_MAP_SIZE_WITH_MINIMAL_FIELDS = 2;

    @Test
    void toMapShouldIncludeAllFieldsWhenAllProvided() {
        // Given
        AccountTargetContext context = AccountTargetContext.builder()
                .accountId("account-123")
                .accountName("Test Account")
                .status("ACTIVE")
                .build();

        // When
        Map<String, Object> map = context.toMap();

        // Then
        assertThat(map).containsEntry("targetId", "account-123");
        assertThat(map).containsEntry("targetType", "ACCOUNT");
        assertThat(map).containsEntry("accountName", "Test Account");
        assertThat(map).containsEntry("status", "ACTIVE");
        assertThat(map).hasSize(EXPECTED_MAP_SIZE_WITH_ALL_FIELDS);
    }

    @Test
    void toMapShouldIncludeOnlyAccountIdWhenOtherFieldsNull() {
        // Given
        AccountTargetContext context = AccountTargetContext.builder()
                .accountId("account-456")
                .accountName(null)
                .status(null)
                .build();

        // When
        Map<String, Object> map = context.toMap();

        // Then
        assertThat(map).containsEntry("targetId", "account-456");
        assertThat(map).containsEntry("targetType", "ACCOUNT");
        assertThat(map).doesNotContainKey("accountName");
        assertThat(map).doesNotContainKey("status");
        assertThat(map).hasSize(EXPECTED_MAP_SIZE_WITH_MINIMAL_FIELDS);
    }

    @Test
    void toMapShouldHandleNullAccountId() {
        // Given
        AccountTargetContext context = AccountTargetContext.builder()
                .accountId(null)
                .accountName("My Account")
                .status("DELETED")
                .build();

        // When
        Map<String, Object> map = context.toMap();

        // Then
        assertThat(map).containsEntry("targetId", null);
        assertThat(map).containsEntry("targetType", "ACCOUNT");
        assertThat(map).containsEntry("accountName", "My Account");
        assertThat(map).containsEntry("status", "DELETED");
        assertThat(map).hasSize(EXPECTED_MAP_SIZE_WITH_ALL_FIELDS);
    }

    @Test
    void toMapShouldExcludeNullAccountNameAndStatus() {
        // Given
        AccountTargetContext context = AccountTargetContext.builder()
                .accountId("account-789")
                .build();

        // When
        Map<String, Object> map = context.toMap();

        // Then
        assertThat(map).containsEntry("targetId", "account-789");
        assertThat(map).containsEntry("targetType", "ACCOUNT");
        assertThat(map).doesNotContainKey("accountName");
        assertThat(map).doesNotContainKey("status");
        assertThat(map).hasSize(EXPECTED_MAP_SIZE_WITH_MINIMAL_FIELDS);
    }

    @Test
    void toMapShouldAlwaysIncludeTargetTypeAsAccount() {
        // Given
        AccountTargetContext context = AccountTargetContext.builder()
                .accountId("acc-001")
                .accountName("Demo")
                .status("SUSPENDED")
                .build();

        // When
        Map<String, Object> map = context.toMap();

        // Then
        assertThat(map).containsEntry("targetType", "ACCOUNT");
    }

    @Test
    void builderShouldCreateContextWithAllFields() {
        // When
        AccountTargetContext context = AccountTargetContext.builder()
                .accountId("account-999")
                .accountName("Builder Test Account")
                .status("PENDING")
                .build();

        // Then
        assertThat(context.getAccountId()).isEqualTo("account-999");
        assertThat(context.getAccountName()).isEqualTo("Builder Test Account");
        assertThat(context.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void toMapShouldHandleEmptyStrings() {
        // Given
        AccountTargetContext context = AccountTargetContext.builder()
                .accountId("")
                .accountName("")
                .status("")
                .build();

        // When
        Map<String, Object> map = context.toMap();

        // Then - empty strings are not null, so they should be included
        assertThat(map).containsEntry("targetId", "");
        assertThat(map).containsEntry("targetType", "ACCOUNT");
        assertThat(map).containsEntry("accountName", "");
        assertThat(map).containsEntry("status", "");
        assertThat(map).hasSize(EXPECTED_MAP_SIZE_WITH_ALL_FIELDS);
    }

    @Test
    void toMapShouldHandleSpecialCharactersInAccountName() {
        // Given
        AccountTargetContext context = AccountTargetContext.builder()
                .accountId("account-special")
                .accountName("O'Reilly & Sons <Test>")
                .status("ACTIVE")
                .build();

        // When
        Map<String, Object> map = context.toMap();

        // Then
        assertThat(map).containsEntry("accountName", "O'Reilly & Sons <Test>");
    }
}
