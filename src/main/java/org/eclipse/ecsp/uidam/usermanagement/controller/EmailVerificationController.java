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

package org.eclipse.ecsp.uidam.usermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailVerificationService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.EmailVerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.List;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.EMAIL_VERIFICATION_TAG;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_EMAIL_VERIFICATION_GET;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_RESEND_EMAIL_VERIFICATION_PUT;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_VALIDATE_EMAIL;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_EMAIL_VERIFICATION_GET;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SUMMARY_EMAIL_VERIFY;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TOKEN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID_VARIABLE;

/**
 * User Email Controller CRUD APIs to send, get and update email verification to unregistered user in UIDAM.
 */
@RestController
@RequestMapping(value = "/{tenantId}/v1/emailVerification")
@Validated
@AllArgsConstructor
public class EmailVerificationController {

    private EmailVerificationService emailVerificationService;
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationController.class);

    /**
     * API to return email verification details whether user email was verified by user id.
     *
     * @param userId userId.
     * @return List of emailVerification response.
     * @throws ResourceNotFoundException if user id does not exist in system.
     */
    @Operation(summary = SUMMARY_EMAIL_VERIFICATION_GET,
        description = "Get if user email was verified by user id.",
        tags = {EMAIL_VERIFICATION_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success")
        })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ViewUsers", "ManageUsers"})
    @GetMapping(PATH_EMAIL_VERIFICATION_GET)
    public ResponseEntity<List<EmailVerificationResponse>> getIsEmailVerified(
            @PathVariable("tenantId") @Parameter(description = "Tenant ID", required = true) @NotBlank String tenantId,
            @PathVariable(PATH_USER_ID) 
            @Parameter(description = USER_ID_VARIABLE, required = true) @NotBlank String userId)
            throws ResourceNotFoundException {
        LOGGER.info("isEmailVerified started for User with userId: {}", userId);
        return new ResponseEntity<>(emailVerificationService.getEmailVerificationByUserId(userId), HttpStatus.OK);
    }

    /**
     * API to verify email with unique token.
     *
     * @param emailVerificationToken unique token generated at time of email verification.
     * @param httpServletResponse redirect response to auth server success = true/false/error page.
     * @throws IOException ioexception.
     */
    @Operation(summary = SUMMARY_EMAIL_VERIFY,
        description = "API requires only a valid link (i.e. UUID) to validate an email.",
        tags = {EMAIL_VERIFICATION_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success")
        })
    @GetMapping(value = PATH_VALIDATE_EMAIL)
    public void verifyEmail(
            @PathVariable("tenantId") @Parameter(description = "Tenant ID", required = true) @NotBlank String tenantId,
            @PathVariable(TOKEN) String emailVerificationToken, 
            HttpServletResponse httpServletResponse)
            throws IOException {
        LOGGER.info("verifyEmail started for tenant {} with token: {}", tenantId, emailVerificationToken);
        emailVerificationService.verifyEmail(emailVerificationToken, httpServletResponse);
    }

    /**
     * API to send email verification to user via notification service.
     *
     * @param userId userId.
     * @throws ResourceNotFoundException if user id does not exist in system.
     */
    @PutMapping(value = PATH_RESEND_EMAIL_VERIFICATION_PUT)
    public void resendEmailVerification(
            @PathVariable("tenantId") @Parameter(description = "Tenant ID", required = true) @NotBlank String tenantId,
            @PathVariable(USER_ID_VARIABLE) String userId)
            throws ResourceNotFoundException {
        LOGGER.info("resendEmailVerification started for tenant {} and user with userId: {}", tenantId, userId);
        emailVerificationService.resendEmailVerification(userId);
    }

}
