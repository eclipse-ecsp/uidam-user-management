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

package org.eclipse.ecsp.uidam.usermanagement.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for MaskingPatternUtil.
 */
@DisplayName("MaskingPatternUtil Test Suite")
class MaskingPatternUtilTest {

    @Test
    @DisplayName("Should mask password in simple message")
    void testMaskPasswordsWithSinglePattern() {
        // Arrange
        String message = "Login attempt with password=secret123 successful";
        String patterns = "password=([^&\\s]+)";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);
        String mask = "****";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, mask);

        // Assert
        assertEquals("Login attempt with password=**** successful", result);
    }

    @Test
    @DisplayName("Should mask multiple password occurrences")
    void testMaskPasswordsWithMultipleOccurrences() {
        // Arrange
        String message = "password=first and password=second";
        String patterns = "password=([^&\\s]+)";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);
        String mask = "***";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, mask);

        // Assert
        assertEquals("password=*** and password=***", result);
    }

    @Test
    @DisplayName("Should mask multiple sensitive fields with different patterns")
    void testMaskPasswordsWithMultiplePatterns() {
        // Arrange
        String message = "password=secret123 and apiKey=key456 and token=token789";
        String patterns = "password=([^&\\s]+)|apiKey=([^&\\s]+)|token=([^&\\s]+)";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);
        String mask = "****";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, mask);

        // Assert
        assertTrue(result.contains("password=****"));
        assertTrue(result.contains("apiKey=****"));
        assertTrue(result.contains("token=****"));
    }

    @Test
    @DisplayName("Should handle null patterns gracefully")
    void testMaskPasswordsWithNullPatterns() {
        // Arrange
        String message = "password=secret123";
        String mask = "****";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, null, mask);

        // Assert
        assertEquals("password=secret123", result);
    }

    @Test
    @DisplayName("Should handle null mask gracefully")
    void testMaskPasswordsWithNullMask() {
        // Arrange
        String message = "password=secret123";
        String patterns = "password=([^&\\s]+)";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, null);

        // Assert
        assertEquals("password=secret123", result);
    }

    @Test
    @DisplayName("Should return message unchanged when no patterns match")
    void testMaskPasswordsWithNoMatches() {
        // Arrange
        String message = "This is a normal log message";
        String patterns = "password=([^&\\s]+)";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);
        String mask = "****";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, mask);

        // Assert
        assertEquals("This is a normal log message", result);
    }

    @Test
    @DisplayName("Should parse single pattern correctly")
    void testParseSinglePattern() {
        // Arrange
        String patternsProperty = "password=([^&\\s]+)";

        // Act
        List<Pattern> result = MaskingPatternUtil.parsePatterns(patternsProperty);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("password=([^&\\s]+)", result.get(0).pattern());
    }

    @Test
    @DisplayName("Should parse multiple patterns separated by pipe")
    void testParseMultiplePatterns() {
        // Arrange
        String patternsProperty = "password=([^&\\s]+)|apiKey=([^&\\s]+)|token=([^&\\s]+)";

        // Act
        List<Pattern> result = MaskingPatternUtil.parsePatterns(patternsProperty);

        // Assert
        assertNotNull(result);
        final int expectedPatternCount = 3;
        final int firstPatternIndex = 0;
        final int secondPatternIndex = 1;
        final int thirdPatternIndex = 2;
        assertEquals(expectedPatternCount, result.size());
        assertEquals("password=([^&\\s]+)", result.get(firstPatternIndex).pattern());
        assertEquals("apiKey=([^&\\s]+)", result.get(secondPatternIndex).pattern());
        assertEquals("token=([^&\\s]+)", result.get(thirdPatternIndex).pattern());
    }

    @Test
    @DisplayName("Should handle complex regex patterns")
    void testParseComplexPatterns() {
        // Arrange
        String patternsProperty = "\"password\"\\s*:\\s*\"([^\"]+)\"|api_key=([^&\\s]+)";

        // Act
        List<Pattern> result = MaskingPatternUtil.parsePatterns(patternsProperty);

        // Assert
        assertNotNull(result);
        final int expectedComplexPatternCount = 2;
        assertEquals(expectedComplexPatternCount, result.size());
    }

    @Test
    @DisplayName("Should mask password in JSON format")
    void testMaskPasswordInJson() {
        // Arrange
        String message = "{\"username\":\"user\",\"password\":\"secret123\"}";
        String patterns = "\"password\"\\s*:\\s*\"([^\"]+)\"";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);
        String mask = "****";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, mask);

        // Assert
        assertEquals("{\"username\":\"user\",\"password\":\"****\"}", result);
    }

    @Test
    @DisplayName("Should mask password in URL query parameters")
    void testMaskPasswordInUrlQueryParams() {
        // Arrange
        String message = "https://example.com/login?username=user&password=secret123&remember=true";
        String patterns = "password=([^&\\s]+)";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);
        String mask = "****";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, mask);

        // Assert
        assertEquals("https://example.com/login?username=user&password=****&remember=true", result);
    }

    @Test
    @DisplayName("Should handle empty message")
    void testMaskPasswordsWithEmptyMessage() {
        // Arrange
        String message = "";
        String patterns = "password=([^&\\s]+)";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);
        String mask = "****";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, mask);

        // Assert
        assertEquals("", result);
    }

    @Test
    @DisplayName("Should handle pattern with special characters in mask")
    void testMaskPasswordsWithSpecialCharacterMask() {
        // Arrange
        String message = "password=secret123";
        String patterns = "password=([^&\\s]+)";
        List<Pattern> compiledPatterns = MaskingPatternUtil.parsePatterns(patterns);
        String mask = "***MASKED***";

        // Act
        String result = MaskingPatternUtil.maskPasswords(message, compiledPatterns, mask);

        // Assert
        assertEquals("password=***MASKED***", result);
    }
}
