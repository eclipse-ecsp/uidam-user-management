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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for MaskingPatternLayout.
 */
@DisplayName("MaskingPatternLayout Test Suite")
class MaskingPatternLayoutTest {

    private MaskingPatternLayout layout;
    private LoggerContext loggerContext;

    @BeforeEach
    void setUp() {
        layout = new MaskingPatternLayout();
        loggerContext = new LoggerContext();
        layout.setContext(loggerContext);
        layout.setPattern("%msg%n");
        layout.start();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDoLayoutTestCases")
    @DisplayName("Should apply masking patterns correctly")
    void testDoLayout(String description, String patterns, String mask,
            String inputMessage, String[] expectedContents) {
        layout.setPatternsProperty(patterns);
        layout.setMask(mask);

        Logger logger = loggerContext.getLogger("test");
        ILoggingEvent event = new LoggingEvent("test", logger, Level.INFO, inputMessage, null, null);

        String result = layout.doLayout(event);

        assertNotNull(result);
        for (String expected : expectedContents) {
            assertTrue(result.contains(expected), "Expected result to contain: " + expected);
        }
    }

    static Stream<Arguments> provideDoLayoutTestCases() {
        return Stream.of(
            Arguments.of("masks password in log message",
                    "password=([^&\\s]+)", "****",
                    "Login attempt with password=secret123",
                    new String[]{"password=****"}),
            Arguments.of("masks multiple sensitive fields",
                    "password=([^&\\s]+)|apiKey=([^&\\s]+)", "***MASKED***",
                    "Request with password=secret123 and apiKey=key456",
                    new String[]{"password=***MASKED***", "apiKey=***MASKED***"}),
            Arguments.of("does not modify message without sensitive data",
                    "password=([^&\\s]+)", "****",
                    "Normal log message without sensitive data",
                    new String[]{"Normal log message without sensitive data"})
        );
    }

    @Test
    @DisplayName("Should handle empty message")
    void testDoLayoutWithEmptyMessage() {
        // Arrange
        String patterns = "password=([^&\\s]+)";
        String mask = "****";
        
        layout.setPatternsProperty(patterns);
        layout.setMask(mask);

        Logger logger = loggerContext.getLogger("test");
        ILoggingEvent event = new LoggingEvent(
                "test",
                logger,
                Level.INFO,
                "",
                null,
                null
        );

        // Act
        String result = layout.doLayout(event);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should set patterns property correctly")
    void testSetPatternsProperty() {
        // Arrange
        String patterns = "password=([^&\\s]+)|token=([^&\\s]+)";

        // Act
        layout.setPatternsProperty(patterns);

        // Assert - verify no exception is thrown and layout can be used
        Logger logger = loggerContext.getLogger("test");
        ILoggingEvent event = new LoggingEvent(
                "test",
                logger,
                Level.INFO,
                "test message",
                null,
                null
        );
        
        String result = layout.doLayout(event);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should set mask property correctly")
    void testSetMask() {
        // Arrange
        String mask = "***REDACTED***";

        // Act
        layout.setMask(mask);

        // Assert - verify no exception is thrown
        assertNotNull(layout);
    }

    @Test
    @DisplayName("Should mask password in JSON log message")
    void testDoLayoutWithJsonMessage() {
        // Arrange
        String patterns = "\"password\"\\s*:\\s*\"([^\"]+)\"";
        String mask = "****";
        
        layout.setPatternsProperty(patterns);
        layout.setMask(mask);

        Logger logger = loggerContext.getLogger("test");
        ILoggingEvent event = new LoggingEvent(
                "test",
                logger,
                Level.INFO,
                "{\"username\":\"user\",\"password\":\"secret123\"}",
                null,
                null
        );

        // Act
        String result = layout.doLayout(event);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("\"password\":\"****\""));
    }

    @Test
    @DisplayName("Should work with pattern layout formatting")
    void testDoLayoutWithCustomPattern() {
        // Arrange
        layout.setPattern("%level - %msg%n");
        String patterns = "password=([^&\\s]+)";
        String mask = "****";
        
        layout.setPatternsProperty(patterns);
        layout.setMask(mask);
        layout.start(); // Must call start() after changing pattern

        Logger logger = loggerContext.getLogger("test");
        ILoggingEvent event = new LoggingEvent(
                "test",
                logger,
                Level.WARN,
                "Failed login with password=wrong123",
                null,
                null
        );

        // Act
        String result = layout.doLayout(event);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("WARN"));
        assertTrue(result.contains("password=****"));
    }
}
