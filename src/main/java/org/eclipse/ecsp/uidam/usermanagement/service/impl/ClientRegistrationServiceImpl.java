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

package org.eclipse.ecsp.uidam.usermanagement.service.impl;

import jakarta.transaction.Transactional;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.entity.ClientEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientRegistrationResponseCode;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientRegistrationResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ClientRegistrationException;
import org.eclipse.ecsp.uidam.usermanagement.repository.ClientRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.ClientRegistration;
import org.eclipse.ecsp.uidam.usermanagement.utilities.AesEncryptionDecryption;
import org.eclipse.ecsp.uidam.usermanagement.utilities.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for client registration including all crud methods.
 */
@Service
public class ClientRegistrationServiceImpl implements ClientRegistration {
    private static Logger logger = LoggerFactory.getLogger(ClientRegistrationServiceImpl.class);
    private static final String DEFAULT_TENANT_ID = "default_tenant";
    private static final String DEFAULT_CLIENT_AUTHENTICATION_METHODS = "client_secret_basic,client_secret_post";

    @Value("${client.registration.default.status:approved}")
    private String clientStatus;

    @Value("${client.refresh.token.validaity.default:3600}")
    private long refreshTokenValidity;

    @Value("${client.access.token.validaity.default:3600}")
    private long accessTokenValidity;

    @Value("${client.authorization.code.validaity.default:300}")
    private long authorizationCodeValidity;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    AesEncryptionDecryption aesEncryptionDecryption;

    /**
     * This method is used to add new client in the database.
     *
     * @return RegisteredClientDetails
     **/
    @Override
    @Transactional
    public Optional<RegisteredClientDetails> addRegisteredClient(RegisteredClientDetails request) {
        // validate request
        ValidationUtils.validateClientRegistrationRequest(request);
        logger.debug("checking if client already exist in system!");
        if (isClientExist(request.getClientId())) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_ALREADY_EXIST);
        }
        ClientEntity client = toClient(request);
        logger.debug("registering client in system with clientId {} !", request.getClientId());
        clientRepository.save(client);
        logger.info("client registered successfully with clientId {}!", request.getClientId());

        return Optional.of(toServiceProvider(client));
    }

    /**
     * This method is used to update existing client in the database.
     *
     * @return RegisteredClientDetails
     **/
    @Override
    @Transactional
    public Optional<RegisteredClientDetails> updateRegisteredClient(String clientId, RegisteredClientDetails request) {
        Optional<ClientEntity> client = clientRepository.findByClientId(clientId);
        if (!client.isPresent()) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_DOES_NOT_EXIST);
        }
        if (ClientStatus.DELETED.getValue().equalsIgnoreCase(client.get().getStatus())
                && !ClientStatus.APPROVED.getValue().equalsIgnoreCase(request.getStatus())) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_DOES_NOT_EXIST);
        }
        ClientEntity clientEntity = toUpdateClient(client.get(), request);
        clientRepository.save(clientEntity);
        logger.debug("updated client in system with clientId {} !", request.getClientId());
        return Optional.of(toServiceProvider(clientEntity));
    }

    /**
     * This method is used to get client details from the database.
     *
     * @return RegisteredClientDetails
     **/
    @Override
    public Optional<RegisteredClientDetails> getRegisteredClient(String clientId, String status) {
        if (!Optional.ofNullable(status).isPresent()) {
            status = ClientStatus.APPROVED.getValue();
        }
        ValidationUtils.validateStatus(status);
        Optional<ClientEntity> client = clientRepository.findByClientIdAndStatus(clientId, status.toLowerCase());
        if (!client.isPresent()) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_DOES_NOT_EXIST);
        }
        return Optional.of(toServiceProvider(client.get()));
    }

    /**
     * This method is used to soft delete of existing client in the database client
     * status would be DELETED for soft delete.
     *
     * @return String
     **/
    @Override
    @Transactional
    public Optional<String> deleteRegisteredClient(String clientId) {
        Optional<ClientEntity> client = clientRepository.findByClientId(clientId);
        if (!client.isPresent() || ClientStatus.DELETED.getValue().equalsIgnoreCase(client.get().getStatus())) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_DOES_NOT_EXIST);
        }
        client.get().setStatus(ClientStatus.DELETED.getValue());
        clientRepository.save(client.get());
        logger.debug("deleted client in system with clientId {} !", clientId);
        return Optional.of(ClientRegistrationResponseMessage.SP_REGISTRATION_DELETE_SUCCESS_200_MSG.getMessage());
    }

    /**
     * This method is used to check client exists in the database.
     *
     * @return true/false
     **/
    public boolean isClientExist(String clientId) {
        return clientRepository.existsByClientId(clientId);
    }

    /**
     * This method is used to convert request to entity return Client.
     **/
    private ClientEntity toClient(RegisteredClientDetails request) {
        ClientEntity client = new ClientEntity();
        client.setClientId(request.getClientId());
        client.setSecret(aesEncryptionDecryption.encrypt(request.getClientSecret()));
        client.setClientName(request.getClientName());
        client.setAdditionalInformation(request.getAdditionalInformation());
        client.setAuthenticationMethods(Optional.ofNullable(request.getClientAuthenticationMethods()).isPresent()
                ? request.getClientAuthenticationMethods().stream().map(Object::toString)
                        .collect(Collectors.joining(","))
                : DEFAULT_CLIENT_AUTHENTICATION_METHODS);
        if (request.getRedirectUris() != null) {
            client.setRedirectUrls(
                    request.getRedirectUris().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        client.setGrantTypes(
                request.getAuthorizationGrantTypes().stream().map(Object::toString).collect(Collectors.joining(",")));
        client.setScopes(request.getScopes().stream().map(Object::toString).collect(Collectors.joining(",")));
        client.setRequiredAuthorizationConsent(request.isRequireAuthorizationConsent());
        client.setRefreshTokenValidity(
                Optional.ofNullable(request.getRefreshTokenValidity()).isPresent() ? request.getRefreshTokenValidity()
                        : refreshTokenValidity);
        client.setAccessTokenValidity(
                Optional.ofNullable(request.getAccessTokenValidity()).isPresent() ? request.getAccessTokenValidity()
                        : accessTokenValidity);
        client.setAuthorizationCodeValidity(Optional.ofNullable(request.getAuthorizationCodeValidity()).isPresent()
                ? request.getAuthorizationCodeValidity()
                : authorizationCodeValidity);
        client.setCreatedBy(request.getCreatedBy());
        client.setStatus(clientStatus);
        // to be updated when multi-tenancy implemented
        client.setTenantId(DEFAULT_TENANT_ID);
        client.setApprovedBy(request.getCreatedBy());

        return client;
    }

    /**
     * Method to map clientEntity to RegisteredClientDetails.
     *
     * @param client client entity input.
     * @return RegisteredClientDetails object.
     */
    private RegisteredClientDetails toServiceProvider(ClientEntity client) {
        RegisteredClientDetails registeredClientDetails = new RegisteredClientDetails();
        registeredClientDetails.setClientId(client.getClientId());
        registeredClientDetails.setClientSecret(aesEncryptionDecryption.decrypt(client.getSecret()));
        registeredClientDetails.setClientName(client.getClientName());
        registeredClientDetails.setAccessTokenValidity((int) client.getAccessTokenValidity());
        registeredClientDetails.setAdditionalInformation(client.getAdditionalInformation());
        registeredClientDetails
                .setClientAuthenticationMethods(Arrays.asList(client.getAuthenticationMethods().split(",")).stream()
                        .map(Object::toString).toList());
        if (Optional.ofNullable(client.getRedirectUrls()).isPresent()
                && !Optional.ofNullable(client.getRedirectUrls()).isEmpty()) {
            registeredClientDetails.setRedirectUris(Arrays.asList(client.getRedirectUrls().split(",")).stream()
                    .map(Object::toString).toList());
        }
        registeredClientDetails.setAuthorizationGrantTypes(Arrays.asList(client.getGrantTypes().split(",")).stream()
                .map(Object::toString).toList());
        registeredClientDetails.setScopes(Arrays.asList(client.getScopes().split(",")).stream().map(Object::toString)
                .collect(Collectors.toSet()));
        registeredClientDetails.setRequireAuthorizationConsent(client.isRequiredAuthorizationConsent());
        registeredClientDetails.setRefreshTokenValidity((int) client.getRefreshTokenValidity());
        registeredClientDetails.setAuthorizationCodeValidity((int) client.getAuthorizationCodeValidity());

        return registeredClientDetails;
    }

    /**
     * Method to update client details.
     *
     * @param client client details present in db.
     * @param request input client data modification request.
     * @return clientEntity.
     */
    private ClientEntity toUpdateClient(ClientEntity client, RegisteredClientDetails request) {
        if (Optional.ofNullable(request.getClientSecret()).isPresent()) {
            client.setSecret(aesEncryptionDecryption.encrypt(request.getClientSecret()));
        }
        if (Optional.ofNullable(request.getClientName()).isPresent()) {
            client.setClientName(request.getClientName());
        }
        if (Optional.ofNullable(request.getAdditionalInformation()).isPresent()) {
            client.setAdditionalInformation(request.getAdditionalInformation());
        }
        if (Optional.ofNullable(request.getClientAuthenticationMethods()).isPresent()
                && !request.getClientAuthenticationMethods().isEmpty()) {
            client.setAuthenticationMethods(request.getClientAuthenticationMethods().stream().map(Object::toString)
                    .collect(Collectors.joining(",")));
        }

        if (request.getRedirectUris() != null && !request.getRedirectUris().isEmpty()) {
            client.setRedirectUrls(
                    request.getRedirectUris().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        if (Optional.ofNullable(request.getAuthorizationGrantTypes()).isPresent()
                && !request.getAuthorizationGrantTypes().isEmpty()) {
            ValidationUtils.validateUris(request);
            client.setGrantTypes(request.getAuthorizationGrantTypes().stream().map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        if (Optional.ofNullable(request.getScopes()).isPresent() && !request.getScopes().isEmpty()) {
            client.setScopes(request.getScopes().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        if (Optional.ofNullable(request.isRequireAuthorizationConsent()).isPresent()) {
            client.setRequiredAuthorizationConsent(request.isRequireAuthorizationConsent());
        }

        checkTokenValidaty(client, request);
        if (!ClientStatus.DELETED.getValue().equalsIgnoreCase(request.getStatus())
                && Optional.ofNullable(request.getStatus()).isPresent()) {
            ValidationUtils.validateStatus(request.getStatus());
            client.setStatus(request.getStatus());
        }
        client.setUpdatedBy(request.getCreatedBy());
        return client;
    }

    /**
     * Method to check token validity.
     *
     * @param client client data stored in db.
     * @param request input client request.
     */
    private void checkTokenValidaty(ClientEntity client, RegisteredClientDetails request) {
        if (Optional.ofNullable(request.getRefreshTokenValidity()).isPresent()) {
            client.setRefreshTokenValidity(request.getRefreshTokenValidity());
        }
        if (Optional.ofNullable(request.getAccessTokenValidity()).isPresent()) {
            client.setAccessTokenValidity(request.getAccessTokenValidity());
        }
        if (Optional.ofNullable(request.getAuthorizationCodeValidity()).isPresent()) {
            client.setAuthorizationCodeValidity(request.getAuthorizationCodeValidity());
        }
    }
}
