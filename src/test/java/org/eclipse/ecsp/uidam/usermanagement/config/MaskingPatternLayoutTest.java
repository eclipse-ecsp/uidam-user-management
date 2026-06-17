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

    @ParameterizedTest
    @DisplayName("Should correctly layout log message based on masking pattern")
    @MethodSource("provideDoLayoutInputs")
    void testDoLayout(String patterns, String mask, String inputMessage, String expectedSubstring) {
        // Arrange
        layout.setPatternsProperty(patterns);
        layout.setMask(mask);

        Logger logger = loggerContext.getLogger("test");
        ILoggingEvent event = new LoggingEvent(
                "test",
                logger,
                Level.INFO,
                inputMessage,
                null,
                null
        );

        // Act
        String result = layout.doLayout(event);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(expectedSubstring));
    }

    static Stream<Arguments> provideDoLayoutInputs() {
        return Stream.of(
            Arguments.of("password=([^&\\s]+)", "****",
                    "Login attempt with password=secret123", "password=****"),
            Arguments.of("password=([^&\\s]+)", "****",
                    "Normal log message without sensitive data",
                    "Normal log message without sensitive data"),
            Arguments.of("\"password\"\\s*:\\s*\"([^\"]+)\"", "****",
                    "{\"username\":\"user\",\"password\":\"secret123\"}",
                    "\"password\":\"****\"")
        );
    }

    @Test
    @DisplayName("Should mask multiple sensitive fields")
    void testDoLayoutWithMultiplePatterns() {
        // Arrange
        String patterns = "password=([^&\\s]+)|apiKey=([^&\\s]+)";
        String mask = "***MASKED***";
        
        layout.setPatternsProperty(patterns);
        layout.setMask(mask);

        Logger logger = loggerContext.getLogger("test");
        ILoggingEvent event = new LoggingEvent(
                "test",
                logger,
                Level.INFO,
                "Request with password=secret123 and apiKey=key456",
                null,
                null
        );

        // Act
        String result = layout.doLayout(event);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("password=***MASKED***"));
        assertTrue(result.contains("apiKey=***MASKED***"));
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
