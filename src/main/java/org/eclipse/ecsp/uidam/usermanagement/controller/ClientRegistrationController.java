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
import org.eclipse.ecsp.security.Security;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientRegistrationResponseCode;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientRegistrationResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.service.ClientRegistration;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Rest controller for client registration.
 */
@RestController
@RequestMapping(value = ApiConstants.API_VERSION + ApiConstants.CLIENT_RESOURCE_PATH, produces = APPLICATION_JSON_VALUE)
public class ClientRegistrationController {
    private static Logger logger = LoggerFactory.getLogger(ClientRegistrationController.class);

    @Autowired
    ClientRegistration clientRegistrationService;

    /**
     * Create client api.
     *
     * @param registeredClientDetails - client create input
     * @return response
     */
    @PostMapping
    @Operation(summary = "Create client", description = "register a new client", responses = {
        @ApiResponse(responseCode = "201", description = "Success", 
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
                array = @ArraySchema(schema = @Schema(implementation = BaseResponse.class)))) })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { "OAuth2ClientMgmt" })
    public ResponseEntity<BaseResponse> createClient(@RequestBody RegisteredClientDetails registeredClientDetails) {
        logger.info("#Creating client request client name: {}", registeredClientDetails.getClientName());
        Optional<RegisteredClientDetails> clientDetails = clientRegistrationService
                .addRegisteredClient(registeredClientDetails);

        return buildResponse(ClientRegistrationResponseCode.SP_CREATED.getCode(),
                ClientRegistrationResponseMessage.SP_REGISTRATION_SUCCESS_201_MSG.getMessage(), clientDetails.get(),
                HttpStatus.CREATED);

    }

    /**
     * Get client details.
     *
     * @param clientId - clientId
     * @param status   - client status
     * @return response with details
     */
    @GetMapping("/{clientId}")
    @Operation(summary = "Get ClientDetails", description = "Get registered client details", responses = {
        @ApiResponse(responseCode = "200", description = "Success", 
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
                array = @ArraySchema(schema = @Schema(implementation = BaseResponse.class)))) })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { "OAuth2ClientMgmt" })
    public ResponseEntity<BaseResponse> getClient(@PathVariable(value = "clientId") String clientId,
            @RequestParam(value = "status", required = false) String status) {
        logger.debug("#Get client request, client id: {}", clientId);
        Optional<RegisteredClientDetails> clientDetails = clientRegistrationService.getRegisteredClient(clientId,
                status);
        return buildResponse(ClientRegistrationResponseCode.SP_SUCCESS.getCode(),
                ClientRegistrationResponseMessage.SP_REGISTRATION_RETRIEVE_SUCCESS_200_MSG.getMessage(),
                clientDetails.get(), HttpStatus.OK);
    }

    /**
     * Update client details.
     *
     * @param clientId - clientId
     * @param request  - values need to be updated
     * @return response with other details
     */
    @PutMapping("/{clientId}")
    @Operation(summary = "update client", description = "update existing client", responses = {
        @ApiResponse(responseCode = "200", description = "Success", 
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
                array = @ArraySchema(schema = @Schema(implementation = BaseResponse.class)))) })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { "OAuth2ClientMgmt" })
    public ResponseEntity<BaseResponse> updateClient(@PathVariable(value = "clientId") String clientId,
            @RequestBody RegisteredClientDetails request) {
        logger.info("#Update client request, client id: {}", clientId);
        Optional<RegisteredClientDetails> clientDetails = clientRegistrationService.updateRegisteredClient(clientId,
                request);
        return buildResponse(ClientRegistrationResponseCode.SP_SUCCESS.getCode(),
                ClientRegistrationResponseMessage.SP_REGISTRATION_UPDATE_SUCCESS_200_MSG.getMessage(),
                clientDetails.get(), HttpStatus.OK);
    }

    /**
     * Delete client api.
     *
     * @param clientId - client Id
     * @return response with details
     */
    @DeleteMapping("/{clientId}")
    @Operation(summary = "delete client", description = "delete existing client", responses = {
        @ApiResponse(responseCode = "200", description = "Success", 
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
                array = @ArraySchema(schema = @Schema(implementation = BaseResponse.class)))) })
    @SecurityRequirement(name = "JwtAuthValidator", scopes = { "OAuth2ClientMgmt" })
    public ResponseEntity<BaseResponse> deleteClient(@PathVariable("clientId") String clientId) {
        logger.info("#Delete client request, client id: {}", clientId);
        Optional<String> response = clientRegistrationService.deleteRegisteredClient(clientId);
        return buildResponse(ClientRegistrationResponseCode.SP_SUCCESS.getCode(), response.get(), null, HttpStatus.OK);
    }

    /**
     * Method to build client registration response.
     *
     * @return ResponseEntity
     **/
    private ResponseEntity<BaseResponse> buildResponse(String code, String message, Object data,
            HttpStatus statuscode) {
        BaseResponse response = BaseResponse.builder().code(code).data(data).httpStatus(statuscode).message(message)
                .build();

        return new ResponseEntity<>(response, statuscode);
    }

}
