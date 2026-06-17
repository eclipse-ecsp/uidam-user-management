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

package org.eclipse.ecsp.uidam.usermanagement.validations;

import jakarta.validation.ConstraintValidatorContext;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExternalUserPasswordValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalUserPasswordValidator Test Suite")
class ExternalUserPasswordValidatorTest {

    private ExternalUserPasswordValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext 
            leafNodeCustomizable;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeContextBuilder 
            leafNodeContext;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderDefinedContext 
            leafNodeDefined;

    @BeforeEach
    void setUp() {
        validator = new ExternalUserPasswordValidator();
        
        // Setup mock chain for constraint violation building (lenient to avoid unused stubbing errors)
        lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        lenient().when(violationBuilder.addBeanNode()).thenReturn(leafNodeCustomizable);
        lenient().when(leafNodeCustomizable.inIterable()).thenReturn(leafNodeContext);
        lenient().when(leafNodeContext.atKey(org.mockito.ArgumentMatchers.any()))
                .thenReturn(leafNodeDefined);
        lenient().when(leafNodeDefined.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("Should create validator instance")
    void testDefaultConstructor() {
        // Assert
        assertNotNull(new ExternalUserPasswordValidator());
    }

    @Test
    @DisplayName("Should initialize with constraint annotation")
    void testInitialize() {
        // Arrange
        ExternalUserPasswordValidation annotation = mock(ExternalUserPasswordValidation.class);
        when(annotation.message()).thenReturn("External users cannot have passwords");

        // Act
        validator.initialize(annotation);
        
        // Create a scenario to trigger validation
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(true);
        userDto.setPassword("somePassword");

        boolean result = validator.isValid(userDto, context);

        // Assert
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("External users cannot have passwords");
    }

    @Test
    @DisplayName("Should return false when external user has password")
    void testIsValidExternalUserWithPassword() {
        // Arrange
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(true);
        userDto.setPassword("somePassword");

        ExternalUserPasswordValidation annotation = mock(ExternalUserPasswordValidation.class);
        when(annotation.message()).thenReturn("External users cannot have password");
        validator.initialize(annotation);

        // Act
        boolean result = validator.isValid(userDto, context);

        // Assert
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("External users cannot have password");
    }

    @Test
    @DisplayName("Should return true when external user has no password")
    void testIsValidExternalUserWithoutPassword() {
        // Arrange
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(true);
        userDto.setPassword(null);

        // Act
        boolean result = validator.isValid(userDto, context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when internal user has password")
    void testIsValidInternalUserWithPassword() {
        // Arrange
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(false);
        userDto.setPassword("somePassword");

        // Act
        boolean result = validator.isValid(userDto, context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when internal user has no password")
    void testIsValidInternalUserWithoutPassword() {
        // Arrange
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(false);
        userDto.setPassword(null);

        // Act
        boolean result = validator.isValid(userDto, context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when isExternalUser is null")
    void testIsValidNullExternalUser() {
        // Arrange
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(null);
        userDto.setPassword("somePassword");

        // Act
        boolean result = validator.isValid(userDto, context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when external user is TRUE (Boolean) with password")
    void testIsValidExternalUserBooleanTrueWithPassword() {
        // Arrange
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(Boolean.TRUE);
        userDto.setPassword("password123");

        ExternalUserPasswordValidation annotation = mock(ExternalUserPasswordValidation.class);
        when(annotation.message()).thenReturn("Cannot set password for external user");
        validator.initialize(annotation);

        // Act
        boolean result = validator.isValid(userDto, context);

        // Assert
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Should return true when isExternalUser is FALSE (Boolean) with password")
    void testIsValidExternalUserBooleanFalseWithPassword() {
        // Arrange
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(Boolean.FALSE);
        userDto.setPassword("password123");

        // Act
        boolean result = validator.isValid(userDto, context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle empty password string as not null")
    void testIsValidExternalUserWithEmptyPassword() {
        // Arrange
        UserDtoBase userDto = new UserDtoBase();
        userDto.setIsExternalUser(true);
        userDto.setPassword("");

        ExternalUserPasswordValidation annotation = mock(ExternalUserPasswordValidation.class);
        when(annotation.message()).thenReturn("Password not allowed");
        validator.initialize(annotation);

        // Act
        boolean result = validator.isValid(userDto, context);

        // Assert
        assertFalse(result);
    }
}
