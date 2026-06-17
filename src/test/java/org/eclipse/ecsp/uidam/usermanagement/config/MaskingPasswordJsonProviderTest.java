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

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MaskingPasswordJsonProvider.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MaskingPasswordJsonProvider Test Suite")
class MaskingPasswordJsonProviderTest {

    private MaskingPasswordJsonProvider provider;

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private ILoggingEvent loggingEvent;

    private static final String FIELD_MESSAGE = "message";

    @BeforeEach
    void setUp() {
        provider = new MaskingPasswordJsonProvider();
    }

    @Test
    @DisplayName("Should mask password in log message")
    void testWriteToWithPasswordMasking() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)|pwd=([^&\\s]+)";
        String mask = "****";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "User login with password=secretpass123";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        String expectedMaskedMessage = "User login with password=****";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should mask multiple password patterns in log message")
    void testWriteToWithMultiplePasswordPatterns() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)|pwd=([^&\\s]+)|secret=([^&\\s]+)";
        String mask = "***MASKED***";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "Login attempt: password=pass123 and secret=abc456";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        String expectedMaskedMessage = "Login attempt: password=***MASKED*** and secret=***MASKED***";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should replace CRLF characters to prevent log injection")
    void testWriteToWithCrlfReplacement() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)";
        String mask = "****";
        String clrfMask = "[CRLF]";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "User login\r\nwith password=secret\nand newline";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        // CRLF replacement happens before password masking, so pattern matches beyond [CRLF]
        String expectedMaskedMessage = "User login[CRLF][CRLF]with password=**** newline";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should handle message with no sensitive data")
    void testWriteToWithNoSensitiveData() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)|secret=([^&\\s]+)";
        String mask = "****";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "User login successful for username testuser";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(formattedMessage));
    }

    @Test
    @DisplayName("Should handle empty log message")
    void testWriteToWithEmptyMessage() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)";
        String mask = "****";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(formattedMessage));
    }

    @Test
    @DisplayName("Should handle message with only CRLF characters")
    void testWriteToWithOnlyCrlf() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)";
        String mask = "****";
        String clrfMask = " ";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "\r\n\n\r";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        // 4 characters (\r\n\n\r) each replaced with space = 4 spaces
        String expectedMaskedMessage = "    ";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should mask password in URL query parameters")
    void testWriteToWithUrlQueryParameters() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)|apiKey=([^&\\s]+)";
        String mask = "[REDACTED]";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "API call: https://api.example.com/login?user=john&password=mypass&apiKey=123abc";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        String expectedMaskedMessage = "API call: https://api.example.com/login?user=john&password=[REDACTED]&apiKey=[REDACTED]";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should handle complex pattern with multiple groups")
    void testWriteToWithComplexPattern() throws IOException {
        // Arrange
        String patterns = "\"password\"\\s*:\\s*\"([^\"]+)\"|password=([^&\\s]+)";
        String mask = "****";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "JSON: {\"username\":\"john\", \"password\": \"secret123\"}";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        String expectedMaskedMessage = "JSON: {\"username\":\"john\", \"password\": \"****\"}";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should set patterns property correctly")
    void testSetPatternsProperty() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)|secret=([^&\\s]+)";

        // Act
        provider.setPatternsProperty(patterns);
        provider.setMask("****");
        provider.setClrfMask("");

        String formattedMessage = "Login with password=test123 and secret=abc";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert - patterns are applied during writeTo
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq("Login with password=**** and secret=****"));
    }

    @Test
    @DisplayName("Should set mask correctly")
    void testSetMask() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)";
        String mask = "[HIDDEN]";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "password=test123";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        String expectedMaskedMessage = "password=[HIDDEN]";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should set CRLF mask correctly")
    void testSetClrfMask() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)";
        String mask = "****";
        String clrfMask = "[NL]";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "Test\nMessage";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        String expectedMaskedMessage = "Test[NL]Message";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should mask same password appearing multiple times")
    void testWriteToWithRepeatedPasswords() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)";
        String mask = "***";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "First password=abc123 and second password=abc123";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        String expectedMaskedMessage = "First password=*** and second password=***";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }

    @Test
    @DisplayName("Should handle special characters in passwords")
    void testWriteToWithSpecialCharactersInPassword() throws IOException {
        // Arrange
        String patterns = "password=([^&\\s]+)";
        String mask = "[MASKED]";
        String clrfMask = "";

        provider.setPatternsProperty(patterns);
        provider.setMask(mask);
        provider.setClrfMask(clrfMask);

        String formattedMessage = "Login with password=P@ssw0rd!#$";
        when(loggingEvent.getFormattedMessage()).thenReturn(formattedMessage);

        // Act
        provider.writeTo(jsonGenerator, loggingEvent);

        // Assert
        String expectedMaskedMessage = "Login with password=[MASKED]";
        verify(jsonGenerator).writeStringField(eq(FIELD_MESSAGE), eq(expectedMaskedMessage));
    }
}
