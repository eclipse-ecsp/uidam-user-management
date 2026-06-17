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
import org.eclipse.ecsp.uidam.usermanagement.utilities.PatchMap;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ForbiddenFieldsValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ForbiddenFieldsValidator Test Suite")
class ForbiddenFieldsValidatorTest {

    private ForbiddenFieldsValidator validator;

    @Mock
    private ForbiddenFields forbiddenFieldsAnnotation;

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
        validator = new ForbiddenFieldsValidator();
        
        // Setup mock chain for constraint violation building (lenient to avoid unused stubbing errors)
        // Chain: buildConstraintViolationWithTemplate -> addBeanNode -> inIterable -> atKey
        lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        lenient().when(violationBuilder.addBeanNode()).thenReturn(leafNodeCustomizable);
        lenient().when(leafNodeCustomizable.inIterable()).thenReturn(leafNodeContext);
        lenient().when(leafNodeContext.atKey(org.mockito.ArgumentMatchers.any()))
                .thenReturn(leafNodeDefined);
        lenient().when(leafNodeDefined.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("Should validate successfully when no forbidden fields are present")
    void testIsValidWithNoForbiddenFields() {
        // Arrange
        String[] forbiddenFields = {"id", "userId", "createdDate"};
        when(forbiddenFieldsAnnotation.forbiddenFields()).thenReturn(forbiddenFields);
        when(forbiddenFieldsAnnotation.message()).thenReturn("Field cannot be modified");
        
        validator.initialize(forbiddenFieldsAnnotation);
        
        PatchMap patchMap = new PatchMap();
        patchMap.put("username", "testuser");
        patchMap.put("email", "test@example.com");

        // Act
        boolean result = validator.isValid(patchMap, context);

        // Assert
        assertTrue(result);
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Should fail validation when single forbidden field is present")
    void testIsValidWithSingleForbiddenField() {
        // Arrange
        String[] forbiddenFields = {"id", "userId", "createdDate"};
        String message = "Field cannot be modified";
        when(forbiddenFieldsAnnotation.forbiddenFields()).thenReturn(forbiddenFields);
        when(forbiddenFieldsAnnotation.message()).thenReturn(message);
        
        validator.initialize(forbiddenFieldsAnnotation);
        
        PatchMap patchMap = new PatchMap();
        patchMap.put("username", "testuser");
        patchMap.put("id", "123");

        // Act
        boolean result = validator.isValid(patchMap, context);

        // Assert
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(message);
    }

    @Test
    @DisplayName("Should fail validation when multiple forbidden fields are present")
    void testIsValidWithMultipleForbiddenFields() {
        // Arrange
        String[] forbiddenFields = {"id", "userId", "createdDate", "updatedDate"};
        String message = "These fields cannot be modified";
        when(forbiddenFieldsAnnotation.forbiddenFields()).thenReturn(forbiddenFields);
        when(forbiddenFieldsAnnotation.message()).thenReturn(message);
        
        validator.initialize(forbiddenFieldsAnnotation);
        
        PatchMap patchMap = new PatchMap();
        patchMap.put("username", "testuser");
        patchMap.put("id", "123");
        patchMap.put("createdDate", "2024-01-01");
        patchMap.put("email", "test@example.com");

        // Act
        boolean result = validator.isValid(patchMap, context);

        // Assert
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(message);
    }

    @Test
    @DisplayName("Should validate successfully when PatchMap is empty")
    void testIsValidWithEmptyMap() {
        // Arrange
        String[] forbiddenFields = {"id", "userId"};
        when(forbiddenFieldsAnnotation.forbiddenFields()).thenReturn(forbiddenFields);
        when(forbiddenFieldsAnnotation.message()).thenReturn("Field cannot be modified");
        
        validator.initialize(forbiddenFieldsAnnotation);
        
        PatchMap patchMap = new PatchMap();

        // Act
        boolean result = validator.isValid(patchMap, context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should fail validation when all fields are forbidden")
    void testIsValidWithAllFieldsForbidden() {
        // Arrange
        String[] forbiddenFields = {"id", "userId", "email"};
        when(forbiddenFieldsAnnotation.forbiddenFields()).thenReturn(forbiddenFields);
        when(forbiddenFieldsAnnotation.message()).thenReturn("All fields are forbidden");
        
        validator.initialize(forbiddenFieldsAnnotation);
        
        PatchMap patchMap = new PatchMap();
        patchMap.put("id", "123");
        patchMap.put("userId", "456");
        patchMap.put("email", "test@example.com");

        // Act
        boolean result = validator.isValid(patchMap, context);

        // Assert
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Should validate successfully when no forbidden fields configured")
    void testIsValidWithNoForbiddenFieldsConfigured() {
        // Arrange
        String[] forbiddenFields = {};
        when(forbiddenFieldsAnnotation.forbiddenFields()).thenReturn(forbiddenFields);
        when(forbiddenFieldsAnnotation.message()).thenReturn("Field cannot be modified");
        
        validator.initialize(forbiddenFieldsAnnotation);
        
        PatchMap patchMap = new PatchMap();
        patchMap.put("username", "testuser");
        patchMap.put("email", "test@example.com");

        // Act
        boolean result = validator.isValid(patchMap, context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle case-sensitive field names correctly")
    void testIsValidCaseSensitive() {
        // Arrange
        String[] forbiddenFields = {"userId"};
        when(forbiddenFieldsAnnotation.forbiddenFields()).thenReturn(forbiddenFields);
        when(forbiddenFieldsAnnotation.message()).thenReturn("Field cannot be modified");
        
        validator.initialize(forbiddenFieldsAnnotation);
        
        PatchMap patchMap = new PatchMap();
        patchMap.put("UserId", "123"); // Different case
        patchMap.put("userid", "456"); // Different case

        // Act
        boolean result = validator.isValid(patchMap, context);

        // Assert
        assertTrue(result); // Should pass because field names are case-sensitive
    }

    @Test
    @DisplayName("Should create validator with default constructor")
    void testDefaultConstructor() {
        // Act
        ForbiddenFieldsValidator newValidator = new ForbiddenFieldsValidator();

        // Assert
        assertNotNull(newValidator);
    }

    @Test
    @DisplayName("Should initialize validator with annotation parameters")
    void testInitialize() {
        // Arrange
        String[] forbiddenFields = {"field1", "field2", "field3"};
        String message = "Custom error message";
        when(forbiddenFieldsAnnotation.forbiddenFields()).thenReturn(forbiddenFields);
        when(forbiddenFieldsAnnotation.message()).thenReturn(message);

        // Act
        validator.initialize(forbiddenFieldsAnnotation);
        
        PatchMap patchMap = new PatchMap();
        patchMap.put("field1", "value");
        boolean result = validator.isValid(patchMap, context);

        // Assert
        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate(message);
    }
}
