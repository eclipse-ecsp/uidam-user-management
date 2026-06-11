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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for UserActorContext.
 */
@DisplayName("UserActorContext Test Suite")
class UserActorContextTest {

    private static final int FAILED_ATTEMPTS_THREE = 3;
    private static final int FAILED_ATTEMPTS_FIVE = 5;
    private static final int FAILED_ATTEMPTS_SEVEN = 7;
    private static final int FAILED_ATTEMPTS_TEN = 10;
    private static final int FAILED_ATTEMPTS_NEGATIVE = -1;
    private static final int MAP_SIZE_TWO = 2;

    @Test
    @DisplayName("Should create UserActorContext with all fields using builder")
    void testBuilderWithAllFields() {
        UserActorContext context = UserActorContext.builder()
                .userId("user123")
                .username("john.doe@example.com")
                .accountId("account456")
                .accountName("Test Account")
                .failedAttempts(FAILED_ATTEMPTS_THREE)
                .build();

        assertNotNull(context);
        assertEquals("user123", context.getUserId());
        assertEquals("john.doe@example.com", context.getUsername());
        assertEquals("account456", context.getAccountId());
        assertEquals("Test Account", context.getAccountName());
        assertEquals(FAILED_ATTEMPTS_THREE, context.getFailedAttempts());
    }

    @Test
    @DisplayName("Should create UserActorContext with minimal fields")
    void testBuilderWithMinimalFields() {
        UserActorContext context = UserActorContext.builder()
                .userId("user789")
                .build();

        assertNotNull(context);
        assertEquals("user789", context.getUserId());
        assertNull(context.getUsername());
        assertNull(context.getAccountId());
        assertNull(context.getAccountName());
        assertNull(context.getFailedAttempts());
    }

    @Test
    @DisplayName("Should convert to map with all fields")
    void testToMapWithAllFields() {
        UserActorContext context = UserActorContext.builder()
                .userId("user123")
                .username("jane.smith@example.com")
                .accountId("account789")
                .accountName("Production Account")
                .failedAttempts(FAILED_ATTEMPTS_FIVE)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertEquals("user123", map.get("actorId"));
        assertEquals("USER", map.get("actorType"));
        assertEquals("jane.smith@example.com", map.get("username"));
        assertEquals("account789", map.get("accountId"));
        assertEquals("Production Account", map.get("accountName"));
        assertEquals(FAILED_ATTEMPTS_FIVE, map.get("failedAttempts"));
    }

    @Test
    @DisplayName("Should convert to map with only required fields")
    void testToMapWithRequiredFieldsOnly() {
        UserActorContext context = UserActorContext.builder()
                .userId("user999")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertEquals("user999", map.get("actorId"));
        assertEquals("USER", map.get("actorType"));
        assertEquals(MAP_SIZE_TWO, map.size()); // Only actorId and actorType
    }

    @Test
    @DisplayName("Should include username in map when provided")
    void testToMapWithUsername() {
        UserActorContext context = UserActorContext.builder()
                .userId("user111")
                .username("test.user@example.com")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("username"));
        assertEquals("test.user@example.com", map.get("username"));
    }

    @Test
    @DisplayName("Should include accountId in map when provided")
    void testToMapWithAccountId() {
        UserActorContext context = UserActorContext.builder()
                .userId("user222")
                .accountId("account999")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("accountId"));
        assertEquals("account999", map.get("accountId"));
    }

    @Test
    @DisplayName("Should include accountName in map when provided")
    void testToMapWithAccountName() {
        UserActorContext context = UserActorContext.builder()
                .userId("user333")
                .accountName("Dev Account")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("accountName"));
        assertEquals("Dev Account", map.get("accountName"));
    }

    @Test
    @DisplayName("Should include failedAttempts in map when provided")
    void testToMapWithFailedAttempts() {
        UserActorContext context = UserActorContext.builder()
                .userId("user444")
                .failedAttempts(FAILED_ATTEMPTS_TEN)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("failedAttempts"));
        assertEquals(FAILED_ATTEMPTS_TEN, map.get("failedAttempts"));
    }

    @Test
    @DisplayName("Should not include null username in map")
    void testToMapExcludesNullUsername() {
        UserActorContext context = UserActorContext.builder()
                .userId("user555")
                .username(null)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(!map.containsKey("username"));
    }

    @Test
    @DisplayName("Should not include null accountId in map")
    void testToMapExcludesNullAccountId() {
        UserActorContext context = UserActorContext.builder()
                .userId("user666")
                .accountId(null)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(!map.containsKey("accountId"));
    }

    @Test
    @DisplayName("Should not include null accountName in map")
    void testToMapExcludesNullAccountName() {
        UserActorContext context = UserActorContext.builder()
                .userId("user777")
                .accountName(null)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(!map.containsKey("accountName"));
    }

    @Test
    @DisplayName("Should not include null failedAttempts in map")
    void testToMapExcludesNullFailedAttempts() {
        UserActorContext context = UserActorContext.builder()
                .userId("user888")
                .failedAttempts(null)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(!map.containsKey("failedAttempts"));
    }

    @Test
    @DisplayName("Should handle zero failed attempts")
    void testToMapWithZeroFailedAttempts() {
        UserActorContext context = UserActorContext.builder()
                .userId("user999")
                .failedAttempts(0)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("failedAttempts"));
        assertEquals(0, map.get("failedAttempts"));
    }

    @Test
    @DisplayName("Should handle empty username string")
    void testToMapWithEmptyUsername() {
        UserActorContext context = UserActorContext.builder()
                .userId("user1000")
                .username("")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("username"));
        assertEquals("", map.get("username"));
    }

    @Test
    @DisplayName("Should handle empty accountName string")
    void testToMapWithEmptyAccountName() {
        UserActorContext context = UserActorContext.builder()
                .userId("user1001")
                .accountName("")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("accountName"));
        assertEquals("", map.get("accountName"));
    }

    @Test
    @DisplayName("Should set actorType to USER")
    void testActorTypeAlwaysUser() {
        UserActorContext context = UserActorContext.builder()
                .userId("user1002")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertEquals("USER", map.get("actorType"));
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void testToMapWithSpecialCharactersInUsername() {
        UserActorContext context = UserActorContext.builder()
                .userId("user1003")
                .username("test+user@example.com")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertEquals("test+user@example.com", map.get("username"));
    }

    @Test
    @DisplayName("Should handle special characters in accountName")
    void testToMapWithSpecialCharactersInAccountName() {
        UserActorContext context = UserActorContext.builder()
                .userId("user1004")
                .accountName("Account & Co.")
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertEquals("Account & Co.", map.get("accountName"));
    }

    @Test
    @DisplayName("Should handle negative failed attempts")
    void testToMapWithNegativeFailedAttempts() {
        UserActorContext context = UserActorContext.builder()
                .userId("user1005")
                .failedAttempts(FAILED_ATTEMPTS_NEGATIVE)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("failedAttempts"));
        assertEquals(FAILED_ATTEMPTS_NEGATIVE, map.get("failedAttempts"));
    }

    @Test
    @DisplayName("Should handle very large failed attempts")
    void testToMapWithLargeFailedAttempts() {
        UserActorContext context = UserActorContext.builder()
                .userId("user1006")
                .failedAttempts(Integer.MAX_VALUE)
                .build();

        Map<String, Object> map = context.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("failedAttempts"));
        assertEquals(Integer.MAX_VALUE, map.get("failedAttempts"));
    }

    @Test
    @DisplayName("Should allow setting all fields via setters")
    void testSettersAndGetters() {
        UserActorContext context = UserActorContext.builder().build();
        context.setUserId("user2000");
        context.setUsername("setter.test@example.com");
        context.setAccountId("account2000");
        context.setAccountName("Setter Test Account");
        context.setFailedAttempts(FAILED_ATTEMPTS_SEVEN);

        assertEquals("user2000", context.getUserId());
        assertEquals("setter.test@example.com", context.getUsername());
        assertEquals("account2000", context.getAccountId());
        assertEquals("Setter Test Account", context.getAccountName());
        assertEquals(FAILED_ATTEMPTS_SEVEN, context.getFailedAttempts());
    }

    @Test
    @DisplayName("Should support equals and hashCode for Lombok Data")
    void testEqualsAndHashCode() {
        UserActorContext context1 = UserActorContext.builder()
                .userId("user3000")
                .username("equal.test@example.com")
                .build();

        UserActorContext context2 = UserActorContext.builder()
                .userId("user3000")
                .username("equal.test@example.com")
                .build();

        assertEquals(context1, context2);
        assertEquals(context1.hashCode(), context2.hashCode());
    }

    @Test
    @DisplayName("Should support toString for Lombok Data")
    void testToString() {
        UserActorContext context = UserActorContext.builder()
                .userId("user4000")
                .username("string.test@example.com")
                .build();

        String toString = context.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("user4000"));
        assertTrue(toString.contains("string.test@example.com"));
    }
}
