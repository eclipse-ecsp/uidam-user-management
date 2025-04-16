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
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RoleIdRequest;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolePatch;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolesCreateRequestDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RolesFilterRequest;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.constants.LoggerMessages;
import org.eclipse.ecsp.uidam.usermanagement.service.RolesService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
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
 * This class is a rest controller for roles.
 */
@RestController
@RequestMapping(value = ApiConstants.API_VERSION
    + ApiConstants.ROLES_RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
@Validated
public class RolesController {
    private static Logger logger = LoggerFactory.getLogger(RolesController.class);

    @Autowired
    private RolesService rolesService;

    /**
     * Rest API to create a new role.
     *
     * @param userId   - user trying to create role
     * @param rolesDto - request pojo
     * @return response with http status code
     */
    @PostMapping
    @Operation(summary = "Create Role", description = "Create a new role", responses = {
        @ApiResponse(responseCode = "201", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = RoleListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<RoleListRepresentation> createRole(
        @RequestHeader(ApiConstants.LOGGED_IN_USER_ID) String userId,
        @RequestHeader("scope") String scopes,
        @RequestBody @Valid RolesCreateRequestDto rolesDto) {
        logger.info(LoggerMessages.CREATE_ROLE, rolesDto.getName(), rolesDto.getScopeNames(), userId);
        return new ResponseEntity<>(rolesService.createRole(rolesDto, userId, scopes), HttpStatus.CREATED);
    }

    /**
     * API to get role information by role name.
     *
     * @param name role name.
     * @return role details.
     */
    @GetMapping("/{name}")
    @Operation(summary = "Get Role By Name", description = "Get role information by role name", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = RoleListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<RoleListRepresentation> getRole(
        @PathVariable(name = "name", required = true) @NotBlank String name) {
        logger.info(LoggerMessages.GET_ROLE, name);
        return ResponseEntity.ok(rolesService.getRole(name.trim()));
    }

    /**
     * API to get role information by role id.
     *
     * @param roleIds List of role ids.
     * @return List of roles matching role ids.
     */
    @PostMapping(ApiConstants.ROLES_RESOURCE_BY_ID_PATH)
    @Operation(summary = "Get Role By Id", description = "Get role information by role id", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = RoleListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<RoleListRepresentation> getRoleById(@RequestBody @Valid RoleIdRequest roleIds) {
        logger.info(LoggerMessages.GET_ROLE_BY_ID, roleIds.getRoleId());
        return ResponseEntity.ok(rolesService.getRoleById(roleIds.getRoleId()));
    }

    /**
     * API to get multiple roles information by role names.
     *
     * @param pageNumber         page number on which records are required to be displayed.
     * @param pageSize           total number of records in a page.
     * @param rolesFilterRequest request containing role filter criteria.
     * @return List of role details.
     */
    @PostMapping(ApiConstants.ROLES_SCOPES_FILTER_PATH)
    @Operation(summary = "Filter Role", description = "Get multiple roles information by role names", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = RoleListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<RoleListRepresentation> filterRoles(
        @RequestParam(name = "page", required = false,
            defaultValue = ApiConstants.PAGE_NUMBER_DEFAULT) Integer pageNumber,
        @RequestParam(name = "pageSize", required = false,
            defaultValue = ApiConstants.PAGE_SIZE_DEFAULT) Integer pageSize,
        @Valid @RequestBody RolesFilterRequest rolesFilterRequest) {
        logger.info(LoggerMessages.FILTER_ROLES, rolesFilterRequest.getRoles());
        return ResponseEntity.ok(rolesService.filterRoles(rolesFilterRequest.getRoles(), pageNumber, pageSize, false));
    }

    /**
     * API to update existing role details.
     *
     * @param roleName  role name.
     * @param userId    user id.
     * @param rolePatch role information to be updated.
     * @param scopes    scopes.
     * @return List of updated role information.
     */
    @PatchMapping("/{name}")
    @Operation(summary = "Update Role", description = "Update existing role details", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = RoleListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<RoleListRepresentation> updateRole(@PathVariable("name") @NotBlank String roleName,
                                                             @RequestHeader(ApiConstants.LOGGED_IN_USER_ID)
                                                             String userId,
                                                             @RequestBody @Valid RolePatch rolePatch,
                                                             @RequestHeader("scope") String scopes) {
        logger.info(LoggerMessages.UPDATE_ROLES, roleName, userId);
        return ResponseEntity.ok(rolesService.updateRole(roleName.trim(), rolePatch, userId, scopes));
    }

    /**
     * API to delete an existing role.
     *
     * @param roleName The unique name of the role to be deleted.
     * @return A ResponseEntity containing a RoleListRepresentation object.
     *     This object represents the updated list of roles after the deletion.
     *     The HTTP status code in the ResponseEntity indicates the result of the
     *     deletion operation.
     */
    @DeleteMapping("/{name}")
    @Operation(summary = "Delete Role", description = "Deletes a single role using its unique name.", responses = {
        @ApiResponse(responseCode = "200", description = "Success",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = RoleListRepresentation.class))))})
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUserRolesAndPermissions"})
    public ResponseEntity<RoleListRepresentation> deleteRole(@PathVariable(name = "name") @NotBlank String roleName,
                                                             @RequestHeader(ApiConstants.LOGGED_IN_USER_ID)
                                                             String userId,
                                                             @RequestHeader(ApiConstants.SCOPE_HEADER) String scopes) {
        logger.info(LoggerMessages.DELETE_ROLES, roleName, userId);
        if (roleName.trim().isEmpty()) {
            logger.error(LoggerMessages.DELETE_ROLE_NAME_EMPTY);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(rolesService.deleteRole(roleName.trim(), userId, scopes));
    }

}
