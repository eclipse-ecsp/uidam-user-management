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

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;
import org.eclipse.ecsp.uidam.accountmanagement.exception.AccountManagementException;
import org.eclipse.ecsp.uidam.security.policy.exception.PasswordPolicyException;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ClientRegistrationException;
import org.eclipse.ecsp.uidam.usermanagement.exception.InActiveUserException;
import org.eclipse.ecsp.uidam.usermanagement.exception.PasswordValidationException;
import org.eclipse.ecsp.uidam.usermanagement.exception.PermissionDeniedException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecordAlreadyExistsException;
import org.eclipse.ecsp.uidam.usermanagement.exception.RecoverySecretExpireException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.ScopeNotExists;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserAccountRoleMappingException;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.BaseRepresentation;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.BaseResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ERROR;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ERROR_INVALID_DATA;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.INVALID_OBJECT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.APPLICATION_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.CLIENT_REGISTRATION_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.DATA_INTEGRITY_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.ENTITY_NOT_EXISTS_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.INACTIVE_USER_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.METHOD_ARGUMENT_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.MISSING_REQUEST_HEADER_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.PERMISSION_DENIED_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.RECORD_ALREADY_EXISTS_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.RECOVERY_SECRET_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.RESOURCE_NOT_FOUND_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.SCOPE_NOT_EXISTS_ERROR_MESSAGE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages.VALIDATION_ERROR_MESSAGE;

/**
 * Creating custom application exception handler.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        List<ObjectError> errorList = ex.getBindingResult().getAllErrors();

        if (ex.getBindingResult().hasFieldErrors()) {
            errorList.forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String message = error.getDefaultMessage();
                errors.put(fieldName, message);
            });
        } else {
            errorList.forEach(error -> {
                String message = error.getDefaultMessage();
                errors.put(INVALID_OBJECT, message);
            });
        }
        logger.error(METHOD_ARGUMENT_ERROR_MESSAGE, ex);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Exception handler method for generic exceptions.
     *
     * @param exception  exception thrown within microservice
     * @param webRequest webrequest
     * @return ErrorDetails object
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception exception, WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(ERROR, "INTERNAL_SERVER_ERROR", exception.getMessage());
        logger.error(APPLICATION_ERROR_MESSAGE, exception);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * AccountManagementException.
     *
     * @param webRequest request
     * @return ResponseEntity
     */
    @ExceptionHandler(AccountManagementException.class)
    public ResponseEntity<UnifiedErrorDetails> handleGlobalException(AccountManagementException exception,
                                                                     WebRequest webRequest) {
        UnifiedErrorDetails errorDetails = new UnifiedErrorDetails(ERROR, exception.getCode(), exception.getMessage(),
            exception.getProperty());
        logger.error(APPLICATION_ERROR_MESSAGE, exception);
        return new ResponseEntity<>(errorDetails, exception.getHttpStatus());
    }
    
    /**
     * PasswordPolicyException.
     *
     * @param webRequest request
     * @return ResponseEntity
     */
    @ExceptionHandler(PasswordPolicyException.class)
    public ResponseEntity<UnifiedErrorDetails> handleGlobalException(PasswordPolicyException exception,
                                                                     WebRequest webRequest) {
        UnifiedErrorDetails errorDetails = new UnifiedErrorDetails(ERROR, exception.getCode(), exception.getMessage(),
            exception.getProperty());
        logger.error(APPLICATION_ERROR_MESSAGE, exception);
        return new ResponseEntity<>(errorDetails, exception.getHttpStatus());
    }
    
    /**
     * PasswordValidationException.
     *
     * @param webRequest request
     * @return ResponseEntity
     */
    @ExceptionHandler(PasswordValidationException.class)
    public ResponseEntity<String> handleGlobalException(PasswordValidationException exception, WebRequest webRequest) {
        logger.error(APPLICATION_ERROR_MESSAGE, exception);
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Exception handler method for application runtime exception.
     *
     * @param applicationRuntimeException applicationRuntimeException
     * @param webRequest                  webRequest
     * @return ErrorDetails
     */
    @ExceptionHandler(ApplicationRuntimeException.class)
    public ResponseEntity<ErrorDetails> handleApplicationRuntimeException(
        ApplicationRuntimeException applicationRuntimeException, WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(ERROR, applicationRuntimeException.getHttpStatus().toString(),
            applicationRuntimeException.getMessage(), List.of(applicationRuntimeException.getParameters()));
        logger.error(APPLICATION_ERROR_MESSAGE, applicationRuntimeException);
        return new ResponseEntity<>(errorDetails, applicationRuntimeException.getHttpStatus());
    }

    /**
     * UserAccountRoleMappingException method to handle exception for user association to account and roles.
     *
     * @param exception applicationRuntimeException
     * @param webRequest request
     * @return ErrorDetails
     */
    @ExceptionHandler(UserAccountRoleMappingException.class)
    public ResponseEntity<UnifiedErrorDetails> handleGlobalExceptionForRoleAssociation(
            UserAccountRoleMappingException exception, WebRequest webRequest) {
        UnifiedErrorDetails errorDetails = new UnifiedErrorDetails(ERROR, exception.getCode(),
                exception.getMessage(), exception.getProperty());
        logger.error(APPLICATION_ERROR_MESSAGE, exception);
        return new ResponseEntity<>(errorDetails, exception.getHttpStatus());
    }

    /**
     * Exception handler method for DataIntegrityViolationException.
     *
     * @param exception  DataIntegrityViolationException
     * @param webRequest webRequest
     * @return ErrorDetails
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDataIntegrityViolationException(DataIntegrityViolationException exception,
                                                                              WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(ERROR, ERROR_INVALID_DATA,
            exception.getMostSpecificCause().getMessage());
        logger.error(DATA_INTEGRITY_ERROR_MESSAGE, exception);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    /**
     * Exception handler method for ResourceNotFoundException.
     *
     * @param exception  ResourceNotFoundException
     * @param webRequest webRequest
     * @return ErrorDetails
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException exception,
                                                                        WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(ERROR, exception.getResourceName().toUpperCase() + "_NOT_FOUND",
            exception.getMessage());
        logger.error(RESOURCE_NOT_FOUND_ERROR_MESSAGE, exception);
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    /**
     * Exception handler method for InActiveUserException.
     *
     * @param exception  InActiveUserException
     * @param webRequest webRequest
     * @return ErrorDetails
     */
    @ExceptionHandler(InActiveUserException.class)
    public ResponseEntity<ErrorDetails> handleInActiveUserException(InActiveUserException exception,
                                                                    WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(ERROR, exception.getErrorCode(), exception.getMessage());
        logger.error(INACTIVE_USER_ERROR_MESSAGE, exception);
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    /**
     * Exception handler method for expired Recovery Secret.
     *
     * @param exception  RecoverySecretExpireException
     * @param webRequest webRequest
     * @return String
     */
    @ExceptionHandler(RecoverySecretExpireException.class)
    public ResponseEntity<String> handleRecoverySecretExpiredException(RecoverySecretExpireException exception,
                                                                       WebRequest webRequest) {
        logger.error(RECOVERY_SECRET_ERROR_MESSAGE, exception);
        return new ResponseEntity<>("password recovery secret expires!", HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles RecordAlreadyExistsException, if record already exists in system.
     *
     * @param e - takes RecordAlreadyExistsException as input
     * @return response with https status code 409 conflict
     */
    @ExceptionHandler(RecordAlreadyExistsException.class)
    public ResponseEntity<BaseRepresentation> exceptionHandler(RecordAlreadyExistsException e) {
        logger.error(RECORD_ALREADY_EXISTS_ERROR_MESSAGE, e);
        ResponseMessage errorResponse = new ResponseMessage(LocalizationKey.FIELD_IS_UNIQUE, e.getMessage());
        BaseRepresentation baseRepresentation = new BaseRepresentation();
        baseRepresentation.addMessage(errorResponse);
        return new ResponseEntity<>(baseRepresentation, HttpStatus.CONFLICT);
    }

    /**
     * Handles ScopeNotExists custom exception.
     *
     * @param e - takes ScopeNotExists as input
     * @return response with http status code 400 bad request
     */
    @ExceptionHandler(ScopeNotExists.class)
    public ResponseEntity<BaseRepresentation> exceptionHandler(ScopeNotExists e) {
        logger.error(SCOPE_NOT_EXISTS_ERROR_MESSAGE, e);
        ResponseMessage errorResponse = new ResponseMessage(LocalizationKey.SCOPE_DOES_NOT_EXIST, e.getMessage());
        BaseRepresentation baseRepresentation = new BaseRepresentation();
        baseRepresentation.addMessage(errorResponse);
        return new ResponseEntity<>(baseRepresentation, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom EntityNotFoundException exception.
     *
     * @param e - takes EntityNotFoundException as input
     * @return response with http status code 404 not found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<BaseRepresentation> exceptionHandler(EntityNotFoundException e) {
        logger.error(ENTITY_NOT_EXISTS_ERROR_MESSAGE, e);
        ResponseMessage errorResponse = new ResponseMessage(e.getMessage());
        BaseRepresentation baseRepresentation = new BaseRepresentation();
        baseRepresentation.addMessage(errorResponse);
        return new ResponseEntity<>(baseRepresentation, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles PermissionDeniedException exception, when user does not have
     * permission to perform given task.
     *
     * @param e - takes PermissionDeniedException as input
     * @return response with http status code 403 forbidden
     */
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<BaseRepresentation> exceptionHandler(PermissionDeniedException e) {
        logger.error(PERMISSION_DENIED_ERROR_MESSAGE, e);
        ResponseMessage errorResponse = new ResponseMessage(LocalizationKey.PERMISSION_DENIED);
        BaseRepresentation baseRepresentation = new BaseRepresentation();
        baseRepresentation.addMessage(errorResponse);
        return new ResponseEntity<>(baseRepresentation, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles MissingRequestHeaderException, if any mandatory header field is
     * missing in request.
     *
     * @param e - takes MissingRequestHeaderException as input
     * @return bad request response with http status code 400
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<BaseRepresentation> exceptionHandler(MissingRequestHeaderException e) {
        logger.error(MISSING_REQUEST_HEADER_ERROR_MESSAGE, e);
        String missingHeadername = e.getHeaderName();

        List<ResponseMessage> responseMessages = new ArrayList<>();
        ResponseMessage rm = new ResponseMessage(LocalizationKey.MISSING_REQUEST_HEADER, missingHeadername);
        responseMessages.add(rm);

        BaseRepresentation baseRepresentation = new BaseRepresentation();
        baseRepresentation.setMessages(responseMessages);

        return new ResponseEntity<>(baseRepresentation, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exception when validation fails for any incoming request like missing
     * mandatory parameters in request.
     *
     * @param ex - takes ValidationException as input
     * @return - return bad request response
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<BaseRepresentation> exceptionHandler(ValidationException ex) {
        logger.error(VALIDATION_ERROR_MESSAGE, ex);

        BaseRepresentation baseRepresentation = new BaseRepresentation();
        if (ex instanceof ConstraintViolationException constraintViolationException) {
            Iterator<ConstraintViolation<?>> iterator = constraintViolationException.getConstraintViolations()
                .iterator();

            while (iterator.hasNext()) {
                ConstraintViolation<?> constraintViolation = iterator.next();
                PathImpl propertyPath = (PathImpl) constraintViolation.getPropertyPath();
                for (Path.Node node : propertyPath) {
                    if (node.getKey() instanceof ResponseMessage response) {
                        baseRepresentation.addMessage(response);
                    }
                }
                if (baseRepresentation.getMessages().isEmpty()) {
                    baseRepresentation.addMessage(new ResponseMessage(constraintViolation.getMessageTemplate(),
                        "field value [" + constraintViolation.getInvalidValue() + "]",
                        propertyPath.getLeafNode().getName()));
                }
            }
        } else {
            throw new ClassCastException();
        }

        return ResponseEntity.badRequest().body(baseRepresentation);

    }

    /**
     * Handles ClientRegistrationException custom exception.
     *
     * @param ex - takes ClientRegistrationException as input
     * @return response with appropriate http status code
     */
    @ExceptionHandler({ClientRegistrationException.class})
    public ResponseEntity<BaseResponse> handleSpRegistrationException(ClientRegistrationException ex) {
        logger.error(CLIENT_REGISTRATION_ERROR_MESSAGE, ex);

        BaseResponse response = BaseResponse.builder().code(ex.getId()).data(null).message(ex.getMessage())
            .httpStatus(HttpStatus.valueOf(ex.getStatusCode())).build();

        return new ResponseEntity<>(response, response.getHttpStatus());

    }

}
