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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopeDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopePatch;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ScopesFilterRequest;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey;
import org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages;
import org.eclipse.ecsp.uidam.usermanagement.service.ScopesService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ScopeListRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Rest controller class for scope crud.
 */
@RestController
@RequestMapping(value = ApiConstants.API_VERSION
    + ApiConstants.SCOPE_RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
@Validated
public class ScopesController {

    private static Logger logger = LoggerFactory.getLogger(ScopesController.class);
    @Autowired
    private ScopesService scopesService;

    /**
     * API to create new scope.
     *
     * @param userId   - user id of requester
     * @param scope    - scopes assigned to requester
     * @param scopeDto - requested details
     * @return response with http status code
     */
    @PostMapping
    @Operation(summary = "Create New Scope", description = "Create a new scope", responses = {
        @ApiResponse(responseCode = "201", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = ScopeListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<ScopeListRepresentation> createScope(
        @RequestHeader(ApiConstants.LOGGED_IN_USER_ID) String userId,
        @RequestHeader("scope") String scope, @RequestBody @Valid ScopeDto scopeDto) {
        logger.info(LoggerMessages.CREATE_SCOPE, scopeDto.getName(), userId);
        if (!scopesService.validatePermissions(scope)) {
            logger.error(LoggerMessages.MANAGE_SCOPE_PERMISSION, userId, scope);
            return new ResponseEntity<>(ScopeListRepresentation.error(LocalizationKey.PERMISSION_DENIED),
                HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(scopesService.addScope(scopeDto, userId), HttpStatus.CREATED);
    }

    /**
     * API to get scope details by scope name.
     *
     * @param scopeName scopeName.
     * @return scope details.
     */
    @GetMapping("/{name}")
    @Operation(summary = "Get Scope", description = "Get scope details", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = ScopeListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<ScopeListRepresentation> getScope(@PathVariable("name") @NotBlank String scopeName) {
        logger.info(LoggerMessages.GET_SCOPE, scopeName);
        return ResponseEntity.ok(scopesService.getScope(scopeName));
    }

    /**
     * API to update scope.
     *
     * @param scopeName  - scope name to be updated
     * @param userId     - id of requester
     * @param scope      - scopes assigned to requester
     * @param scopePatch - update details
     * @return response with http status code
     */
    @PatchMapping("/{name}")
    @Operation(summary = "Update Scope", description = "Update scope details", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = ScopeListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<ScopeListRepresentation> updateScope(@PathVariable("name") @NotBlank String scopeName,
                                                               @RequestHeader(ApiConstants.LOGGED_IN_USER_ID)
                                                               String userId, @RequestHeader("scope") String scope,
                                                               @RequestBody @Valid ScopePatch scopePatch) {
        logger.info(LoggerMessages.UPDATE_SCOPE, scopeName, userId);
        if (!scopesService.validatePermissions(scope)) {
            logger.error(LoggerMessages.MANAGE_SCOPE_PERMISSION, userId, scope);
            return new ResponseEntity<>(ScopeListRepresentation.error(LocalizationKey.PERMISSION_DENIED),
                HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(scopesService.updateScope(scopeName, scopePatch, userId));
    }

    /**
     * API to delete scope.
     *
     * @param scopeName - scope to be deleted
     * @param userId    - id of requester
     * @param scope     - scopes assigned to requester
     * @return response with http status code
     */
    @DeleteMapping("/{name}")
    @Operation(summary = "Delete Scope", description = "Delete scope", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = ScopeListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<ScopeListRepresentation> deleteScope(@PathVariable("name") @NotBlank String scopeName,
                                                               @RequestHeader(ApiConstants.LOGGED_IN_USER_ID)
                                                               String userId, @RequestHeader("scope") String scope) {
        logger.info(LoggerMessages.DELETE_SCOPE, scopeName, userId);
        if (!scopesService.validatePermissions(scope)) {
            logger.error(LoggerMessages.MANAGE_SCOPE_PERMISSION, userId, scope);
            return new ResponseEntity<>(ScopeListRepresentation.error(LocalizationKey.PERMISSION_DENIED),
                HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(scopesService.deleteScope(scopeName));
    }

    /**
     * API to filter scope details.
     *
     * @param pageNumber          page number on which records are required to be displayed.
     * @param pageSize            total number of records in a page.
     * @param scopesFilterRequest request containing scope filter criteria.
     * @return List of scope details.
     */
    @PostMapping(ApiConstants.ROLES_SCOPES_FILTER_PATH)
    @Operation(summary = "Filter Scope", description = "Filter scope details", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = ScopeListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<ScopeListRepresentation> filterScopes(
        @RequestParam(name = "page", required = false,
            defaultValue = ApiConstants.PAGE_NUMBER_DEFAULT) Integer pageNumber,
        @RequestParam(name = "pageSize", required = false,
            defaultValue = ApiConstants.PAGE_SIZE_DEFAULT) Integer pageSize,
        @Valid @RequestBody ScopesFilterRequest scopesFilterRequest) {
        logger.info(LoggerMessages.FILTER_SCOPES, scopesFilterRequest.getScopes());
        return ResponseEntity.ok(scopesService.filterScopes(scopesFilterRequest.getScopes(), pageNumber, pageSize));
    }

}
