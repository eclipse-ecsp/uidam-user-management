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

package org.eclipse.ecsp.uidam.security.policy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.security.Security;
import org.eclipse.ecsp.uidam.security.policy.response.PasswordPolicyResponse;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID_UI;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STRING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.MANAGE_ACCOUNTS_SCOPE;

/**
 * Controller for managing password policies.
 */
@RestController
@RequestMapping(PasswordPolicyController.V1_PASSWORD_POLICIES)
@Validated
@Slf4j
@AllArgsConstructor
public class PasswordPolicyController {

    private static final String POLICY_KEY = "/{policyKey}";
    static final String V1_PASSWORD_POLICIES = "/v1/password-policies";
    private static final String DESC_GET_ALL_PASSWORD_POLICIES = "Fetches all available password policies";
    private static final String SUMMARY_GET_ALL_PASSWORD_POLICIES = "Get all password policies";
    private static final String DESC_GET_PASSWORD_POLICIES = "Fetches a specific password policy by its key";
    private static final String SUMMARY_GET_PASSWORD_POLICIES = "Get password policy by key";
    private static final String DESC_UPDATE_PASSWORD_POLICIES = "Updates password policies following JSON Patch format";
    private static final String SUMMARY_UPDATE_PASSWORD_POLICIES = "Update password policies";
    
    private final PasswordPolicyService passwordPolicyService;

    /**
     * Get all password policies.
     *
     * @return List of password policies.
     */
    @Operation(summary = SUMMARY_GET_ALL_PASSWORD_POLICIES, description = DESC_GET_ALL_PASSWORD_POLICIES,
            responses = {@ApiResponse(responseCode = "200", description = "Success", 
                  content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
                  schema = @Schema(implementation = PasswordPolicyResponse.class)))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { MANAGE_ACCOUNTS_SCOPE })
    @Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI, 
        schema = @Schema(type = STRING), in = ParameterIn.HEADER) 
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID, 
        schema = @Schema(type = STRING), in = ParameterIn.HEADER) 
    @GetMapping
    public ResponseEntity<PasswordPolicyResponse> getAllPasswordPolicies(
            @RequestHeader(value = LOGGED_IN_USER_ID, required = true) String loggedInUser) {
        log.info("Fetching all password policies. Logged in user: {}", loggedInUser);
        PasswordPolicyResponse response = PasswordPolicyResponse.fromEntities(passwordPolicyService.getAllPolicies());
        log.info("Fetched {} password policies.", response.getPasswordPolicies().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get password policy by key.
     *
     * @param policyKey The policy key.
     * @return Password policy details.
     */
    @Operation(summary = SUMMARY_GET_PASSWORD_POLICIES, description = DESC_GET_PASSWORD_POLICIES)
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { MANAGE_ACCOUNTS_SCOPE })
    @Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI, schema = @Schema(type = STRING), 
        in = ParameterIn.HEADER) 
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID, schema = @Schema(type = STRING), 
        in = ParameterIn.HEADER) 
    @GetMapping(POLICY_KEY)
    public ResponseEntity<PasswordPolicyResponse> getPolicyByKey(
            @Valid @PathVariable(value = "policyKey")
            @Parameter(description = "Policy key", required = true) String policyKey,
            @RequestHeader(value = LOGGED_IN_USER_ID, required = true) String loggedInUser) {
        log.info("Fetching password policy for key: {}. Logged in user: {}", policyKey, loggedInUser);
        PasswordPolicyResponse response = PasswordPolicyResponse
                .fromEntities(List.of(passwordPolicyService.getPolicyByKey(policyKey)));
        log.info("Fetched password policy for key: {}", policyKey);
        return ResponseEntity.ok(response);
    }

    /**
     * Update password policies.
     *
     * @param patchRequest List of patch operations.
     * @param loggedInUser The ID of the logged-in user (from header).
     * @return Updated password policies.
     */
    @Operation(summary = SUMMARY_UPDATE_PASSWORD_POLICIES, description = DESC_UPDATE_PASSWORD_POLICIES)
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { MANAGE_ACCOUNTS_SCOPE })
    @Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI, schema = @Schema(type = STRING), 
        in = ParameterIn.HEADER) 
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID, schema = @Schema(type = STRING), 
        in = ParameterIn.HEADER) 
    @PatchMapping
    public ResponseEntity<PasswordPolicyResponse> updatePasswordPolicies(@RequestBody JsonNode patchRequest,
            @RequestHeader(value = LOGGED_IN_USER_ID, required = true) String loggedInUser) {
        log.info("Updating password policies. Logged in user: {}. Patch request: {}", loggedInUser, patchRequest);
        PasswordPolicyResponse response = PasswordPolicyResponse
                .fromEntities(passwordPolicyService.updatePolicies(patchRequest, loggedInUser));
        log.info("Updated password policies successfully.");
        return ResponseEntity.ok(response);
    }
}