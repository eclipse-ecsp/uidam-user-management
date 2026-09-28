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
import lombok.RequiredArgsConstructor;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ClientFilterDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.entity.ClientEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientRegistrationResponseCode;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientRegistrationResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientStatus;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.exception.ClientRegistrationException;
import org.eclipse.ecsp.uidam.usermanagement.repository.ClientRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.ClientRegistration;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ClientFilterResponse;
import org.eclipse.ecsp.uidam.usermanagement.utilities.AesEncryptionDecryption;
import org.eclipse.ecsp.uidam.usermanagement.utilities.ClientSearchSpecification;
import org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria;
import org.eclipse.ecsp.uidam.usermanagement.utilities.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import static org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ClientFilterDto.ClientFilterDtoEnum.CLIENT_IDS;
import static org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ClientFilterDto.ClientFilterDtoEnum.CLIENT_NAMES;
import static org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ClientFilterDto.ClientFilterDtoEnum.STATUS;

/**
 * Service class for client registration including all crud methods.
 */
@Service
@RequiredArgsConstructor
public class ClientRegistrationServiceImpl implements ClientRegistration {
    private static Logger logger = LoggerFactory.getLogger(ClientRegistrationServiceImpl.class);
    private static final String DEFAULT_TENANT_ID = "default_tenant";
    private static final String DEFAULT_CLIENT_AUTHENTICATION_METHODS = "client_secret_basic,client_secret_post";


    private final TenantConfigurationService tenantConfigurationService;

    private final ClientRepository clientRepository;

    private final AesEncryptionDecryption aesEncryptionDecryption;

    /**
     * This method is used to add new client in the database.
     *
     * @return RegisteredClientDetails
     **/
    @Override
    @Transactional
    public RegisteredClientDetails addRegisteredClient(RegisteredClientDetails request) {
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

        return toServiceProvider(client);
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
        String updater = StringUtils.hasText(client.get().getUpdatedBy())
                ? client.get().getUpdatedBy()
                : client.get().getCreatedBy();
        client.get().setUpdatedBy(updater);
        client.get().setUpdateDate(Instant.now());
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
     * Retrieve the clients matching the given filter criteria, page by page.
     *
     * @return paginated clients matching the criteria, without their client secret.
     **/
    @Override
    public ClientFilterResponse filterClients(ClientFilterDto clientFilterDto, Integer pageNumber, Integer pageSize,
            String sortBy, String sortOrder, boolean ignoreCase, SearchType searchType) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.fromString(sortOrder), sortBy));
        Page<ClientEntity> clients = clientRepository
                .findAll(createFilterQuery(clientFilterDto, ignoreCase, searchType), pageable);
        logger.debug("filtered {} clients out of {} matching the given criteria", clients.getNumberOfElements(),
                clients.getTotalElements());
        ClientFilterResponse response = new ClientFilterResponse();
        response.setItems(clients.getContent().stream().map(this::toClientSummary).toList());
        response.setPage(clients.getNumber());
        response.setPageSize(clients.getSize());
        response.setTotalItems(clients.getTotalElements());
        response.setTotalPages(clients.getTotalPages());
        return response;
    }

    /**
     * Method to build the search specification out of the client filter criteria.
     *
     * @param clientFilterDto client attribute names and values to filter on.
     * @param ignoreCase      flag for case-sensitive and case-insensitive search.
     * @param searchType      match values as prefix, suffix, contains or equal.
     * @return specification query for client, null when no criteria was provided.
     */
    private Specification<ClientEntity> createFilterQuery(ClientFilterDto clientFilterDto, boolean ignoreCase,
            SearchType searchType) {
        return createFilterMap(clientFilterDto).entrySet().stream()
                .filter(entry -> !CollectionUtils.isEmpty(entry.getValue()))
                .map(entry -> {
                    SearchCriteria searchCriteria = new SearchCriteria(entry.getKey(), searchType, ignoreCase);
                    searchCriteria.setStringValue(entry.getValue());
                    return searchCriteria;
                })
                .filter(searchCriteria -> !searchCriteria.getValue().isEmpty())
                .map(searchCriteria -> (Specification<ClientEntity>) new ClientSearchSpecification(searchCriteria))
                .reduce(Specification::and).orElse(null);
    }

    /**
     * Method to map the client filter criteria to the client entity attributes.
     *
     * @param clientFilterDto client attribute names and values to filter on.
     * @return map of client entity attributes and the values to search for.
     */
    private Map<String, Set<String>> createFilterMap(ClientFilterDto clientFilterDto) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        filterMap.put(CLIENT_IDS.getField(), clientFilterDto.getClientIds());
        filterMap.put(CLIENT_NAMES.getField(), clientFilterDto.getClientNames());
        if (!CollectionUtils.isEmpty(clientFilterDto.getStatuses())) {
            filterMap.put(STATUS.getField(),
                    clientFilterDto.getStatuses().stream().map(ClientStatus::getValue).collect(Collectors.toSet()));
        }
        return filterMap;
    }

    /**
     * Method to map clientEntity to RegisteredClientDetails, leaving out the client secret.
     *
     * @param client client entity input.
     * @return RegisteredClientDetails object without the client secret.
     */
    private RegisteredClientDetails toClientSummary(ClientEntity client) {
        RegisteredClientDetails registeredClientDetails = toServiceProvider(client, false);
        registeredClientDetails.setStatus(client.getStatus());
        return registeredClientDetails;
    }

    /**
     * This method is used to convert request to entity return Client.
     **/
    private ClientEntity toClient(RegisteredClientDetails request) {
        ClientEntity client = new ClientEntity();
        client.setClientId(request.getClientId());
        client.setSecret(aesEncryptionDecryption.encrypt(request.getClientSecret()));
        client.setClientName(request.getClientName());
        client.setAdditionalInformation(normalizeAdditionalInformation(request.getAdditionalInformation()));
        client.setAuthenticationMethods(Optional.ofNullable(request.getClientAuthenticationMethods()).isPresent()
                ? request.getClientAuthenticationMethods().stream().map(Object::toString)
                        .collect(Collectors.joining(","))
                : DEFAULT_CLIENT_AUTHENTICATION_METHODS);        
        if (request.getRedirectUris() != null) {
            client.setRedirectUrls(
                    request.getRedirectUris().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        if (request.getPostLogoutRedirectUris() != null) {
            client.setPostLogoutRedirectUris(request.getPostLogoutRedirectUris().stream().map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
        client.setGrantTypes(
                request.getAuthorizationGrantTypes().stream().map(Object::toString).collect(Collectors.joining(",")));
        client.setScopes(request.getScopes().stream().map(Object::toString).collect(Collectors.joining(",")));
        client.setRequiredAuthorizationConsent(request.isRequireAuthorizationConsent());
        client.setRefreshTokenValidity(Optional.ofNullable(request.getRefreshTokenValidity()).isPresent()
                ? request.getRefreshTokenValidity()
                : tenantConfigurationService.getTenantProperties().getClientRegistration().getRefreshTokenValidity());
        client.setAccessTokenValidity(Optional.ofNullable(request.getAccessTokenValidity()).isPresent()
                ? request.getAccessTokenValidity()
                : tenantConfigurationService.getTenantProperties().getClientRegistration().getAccessTokenValidity());
        client.setAuthorizationCodeValidity(Optional.ofNullable(request.getAuthorizationCodeValidity()).isPresent()
                ? request.getAuthorizationCodeValidity()
                : tenantConfigurationService.getTenantProperties().getClientRegistration()
                        .getAuthorizationCodeValidity());
        client.setCreatedBy(request.getCreatedBy());
        client.setUpdatedBy(request.getCreatedBy());
        client.setUpdateDate(Instant.now());
        client.setStatus(tenantConfigurationService.getTenantProperties().getClientRegistration().getDefaultStatus());
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
        return toServiceProvider(client, true);
    }

    /**
     * Method to map clientEntity to RegisteredClientDetails.
     *
     * @param client client entity input.
     * @param includeClientSecret whether the decrypted client secret must be exposed.
     * @return RegisteredClientDetails object.
     */
    private RegisteredClientDetails toServiceProvider(ClientEntity client, boolean includeClientSecret) {
        RegisteredClientDetails registeredClientDetails = new RegisteredClientDetails();
        registeredClientDetails.setClientId(client.getClientId());
        if (includeClientSecret) {
            registeredClientDetails.setClientSecret(aesEncryptionDecryption.decrypt(client.getSecret()));
        }
        registeredClientDetails.setClientName(client.getClientName());
        registeredClientDetails.setAccessTokenValidity((int) client.getAccessTokenValidity());
        registeredClientDetails.setAdditionalInformation(client.getAdditionalInformation());
        registeredClientDetails.setClientAuthenticationMethods(splitToList(client.getAuthenticationMethods()));
        if (Optional.ofNullable(client.getRedirectUrls()).isPresent()
                && !Optional.ofNullable(client.getRedirectUrls()).isEmpty()) {
            registeredClientDetails.setRedirectUris(Arrays.asList(client.getRedirectUrls().split(",")).stream()
                    .map(Object::toString).toList());
        }
        if (Optional.ofNullable(client.getPostLogoutRedirectUris()).isPresent()
                && !client.getPostLogoutRedirectUris().isEmpty()) {
            registeredClientDetails.setPostLogoutRedirectUris(Arrays
                    .asList(client.getPostLogoutRedirectUris().split(",")).stream().map(Object::toString).toList());
        }
        registeredClientDetails.setAuthorizationGrantTypes(splitToList(client.getGrantTypes()));
        registeredClientDetails.setScopes(Set.copyOf(splitToList(client.getScopes())));
        registeredClientDetails.setRequireAuthorizationConsent(client.isRequiredAuthorizationConsent());
        registeredClientDetails.setRefreshTokenValidity((int) client.getRefreshTokenValidity());
        registeredClientDetails.setAuthorizationCodeValidity((int) client.getAuthorizationCodeValidity());

        return registeredClientDetails;
    }

    /**
     * Method to split a comma separated column value into its elements.
     *
     * @param value comma separated column value, may be null.
     * @return list of elements, empty when the column holds no value.
     */
    private static List<String> splitToList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.asList(value.split(","));
    }

    /**
     * Method to normalize additionalInformation so blank input is never sent to the jsonb column,
     * since Postgres rejects an empty string as invalid JSON.
     *
     * @param additionalInformation raw value from the request.
     * @return the value unchanged, or null when blank.
     */
    private static String normalizeAdditionalInformation(String additionalInformation) {
        return StringUtils.hasText(additionalInformation) ? additionalInformation : null;
    }

    /**
     * Method to update client details.
     *
     * @param client client details present in db.
     * @param request input client data modification request.
     * @return clientEntity.
     */
    private ClientEntity toUpdateClient(ClientEntity client, RegisteredClientDetails request) {
        if (StringUtils.hasText(request.getClientSecret())) {
            client.setSecret(aesEncryptionDecryption.encrypt(request.getClientSecret()));
        }
        if (Optional.ofNullable(request.getClientName()).isPresent()) {
            client.setClientName(request.getClientName());
        }
        if (Optional.ofNullable(request.getAdditionalInformation()).isPresent()) {
            client.setAdditionalInformation(normalizeAdditionalInformation(request.getAdditionalInformation()));
        }
        if (Optional.ofNullable(request.getClientAuthenticationMethods()).isPresent()
                && !request.getClientAuthenticationMethods().isEmpty()) {
            client.setAuthenticationMethods(request.getClientAuthenticationMethods().stream().map(Object::toString)
                    .collect(Collectors.joining(",")));
        }        
        updateRedirectUris(client, request);
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
        String updater = StringUtils.hasText(request.getCreatedBy())
            ? request.getCreatedBy()
            : (StringUtils.hasText(client.getUpdatedBy()) ? client.getUpdatedBy() : client.getCreatedBy());
        client.setUpdatedBy(updater);
        client.setUpdateDate(Instant.now());
        return client;
    }

    /**
     * Method to update redirect uris.
     *
     * @param client  client entity present in db.
     * @param request input client data modification request.
     */
    private void updateRedirectUris(ClientEntity client, RegisteredClientDetails request) {
        if (request.getRedirectUris() != null && !request.getRedirectUris().isEmpty()) {
            client.setRedirectUrls(
                    request.getRedirectUris().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        if (Optional.ofNullable(request.getPostLogoutRedirectUris()).isPresent()
                && !request.getPostLogoutRedirectUris().isEmpty()) {
            client.setPostLogoutRedirectUris(request.getPostLogoutRedirectUris().stream().map(Object::toString)
                    .collect(Collectors.joining(",")));
        }
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
