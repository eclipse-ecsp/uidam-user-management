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

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for TokenMaskingUtils.
 */
class TokenMaskingUtilsTest {

    private static final int TEST_PREFIX_LENGTH = 3;
    private static final int TEST_SUFFIX_LENGTH = 3;

    @Test
    void testMaskToken_WithNullToken_ReturnsNull() {
        assertEquals("null", TokenMaskingUtils.maskToken(null));
    }

    @Test
    void testMaskToken_WithEmptyToken_ReturnsNull() {
        assertEquals("null", TokenMaskingUtils.maskToken(""));
    }

    @Test
    void testMaskToken_WithShortToken_ReturnsMaskedPlaceholder() {
        assertEquals("****-****-****", TokenMaskingUtils.maskToken("short"));
        assertEquals("****-****-****", TokenMaskingUtils.maskToken("1234567890"));
    }

    @Test
    void testMaskToken_WithLongToken_ReturnsMaskedToken() {
        String longToken = "ab123456-7890-1234-5678-ef123456gh90";
        String expected = "ab123456****gh90";  // First 8 + last 4 characters
        assertEquals(expected, TokenMaskingUtils.maskToken(longToken));
    }

    @Test
    void testMaskToken_WithUuidToken_ReturnsMaskedToken() {
        String uuidToken = "123e4567-e89b-12d3-a456-426614174000";
        String expected = "123e4567****4000";
        assertEquals(expected, TokenMaskingUtils.maskToken(uuidToken));
    }

    @Test
    void testMaskSensitiveData_WithCustomLengths_ReturnsMaskedData() {
        String data = "sensitivedata123";
        String expected = "sen****123";
        assertEquals(expected, TokenMaskingUtils.maskSensitiveData(data, TEST_PREFIX_LENGTH, TEST_SUFFIX_LENGTH));
    }

    @Test
    void testMaskSensitiveData_WithNullData_ReturnsNull() {
        assertEquals("null", TokenMaskingUtils.maskSensitiveData(null, TEST_PREFIX_LENGTH, TEST_SUFFIX_LENGTH));
    }

    @Test
    void testMaskSensitiveData_WithShortData_ReturnsMaskedPlaceholder() {
        assertEquals("****-****-****", 
                TokenMaskingUtils.maskSensitiveData("short", TEST_PREFIX_LENGTH, TEST_SUFFIX_LENGTH));
    }

    @Test
    void testUtilityClassInstantiation_ThrowsException() throws Exception {
        Constructor<TokenMaskingUtils> constructor = TokenMaskingUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }
}
