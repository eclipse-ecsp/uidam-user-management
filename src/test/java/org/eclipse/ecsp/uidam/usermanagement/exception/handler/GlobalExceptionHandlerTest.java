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

package org.eclipse.ecsp.uidam.usermanagement.exception.handler;

import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.InActiveUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ERROR_INVALID_DATA;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.USER_NOT_VERIFIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

class GlobalExceptionHandlerTest {
    private WebRequest request;

    @BeforeEach
    void setUp() {
        request = Mockito.mock(WebRequest.class);
    }

    @Test
    void handleGlobalException() {
        Exception exception = new Exception("global exception");
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        Mockito.when(request.getDescription(anyBoolean())).thenReturn("v1/users");
        ResponseEntity responseEntity = globalExceptionHandler.handleGlobalException(exception, request);
        assertEquals(INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        ErrorDetails errorDetails = (ErrorDetails) responseEntity.getBody();
        assertEquals("INTERNAL_SERVER_ERROR", errorDetails.getCode());
        assertEquals("global exception", errorDetails.getMessage());
    }

    @Test
    void handleApplicationRuntimeException() {
        ApplicationRuntimeException exception =
            new ApplicationRuntimeException("application exception", HttpStatus.BAD_REQUEST);
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        Mockito.when(request.getDescription(anyBoolean())).thenReturn("v1/users");
        ResponseEntity responseEntity = globalExceptionHandler.handleApplicationRuntimeException(exception, request);
        assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
        ErrorDetails errorDetails = (ErrorDetails) responseEntity.getBody();
        assertEquals("400 BAD_REQUEST", errorDetails.getCode());
        assertEquals("{ Error ='application exception', parameters=[] }", errorDetails.getMessage());
    }

    @Test
    void handleDataIntegrityViolationException() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("unique constraint exception");
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        Mockito.when(request.getDescription(anyBoolean())).thenReturn("v1/users");
        ResponseEntity responseEntity =
            globalExceptionHandler.handleDataIntegrityViolationException(exception, request);
        assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
        ErrorDetails errorDetails = (ErrorDetails) responseEntity.getBody();
        assertEquals(ERROR_INVALID_DATA, errorDetails.getCode());
        assertEquals("unique constraint exception", errorDetails.getMessage());
    }

    @Test
    void handleInActiveUserExceptionException() {
        InActiveUserException exception = new InActiveUserException(USER_NOT_VERIFIED, "USER_NOT_VERIFIED");
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        Mockito.when(request.getDescription(anyBoolean())).thenReturn("v1/users/johnd/userByUserName");
        ResponseEntity responseEntity = globalExceptionHandler.handleInActiveUserException(exception, request);
        assertEquals(FORBIDDEN, responseEntity.getStatusCode());
        ErrorDetails errorDetails = (ErrorDetails) responseEntity.getBody();
        assertEquals("USER_NOT_VERIFIED", errorDetails.getCode());
        assertEquals(USER_NOT_VERIFIED, errorDetails.getMessage());
    }
}
