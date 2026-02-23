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

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.ClientRegistrationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.ClientEntity;
import org.eclipse.ecsp.uidam.usermanagement.repository.ClientRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.utilities.AesEncryptionDecryption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for client registration.
 */
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.TestExecutionListeners(
    listeners = org.eclipse.ecsp.uidam.common.test.TenantContextTestExecutionListener.class,
    mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@org.springframework.context.annotation.Import(org.eclipse.ecsp.uidam.common.test.TestTenantConfiguration.class)
class ClientRegistrationTest {

    private static final int INTEGER_300 = 300;

    private static final int INTEGER_3600 = 3600;

    @MockBean
    ClientRepository clientRepository;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    AccountRepository accountRepository;
    
    @MockBean
    PasswordValidationService passwordValidationService;

    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    @MockBean
    AesEncryptionDecryption aesEncryptionDecryption;
    
    @MockBean
    TenantConfigurationService tenantConfigurationService;
    
    private static final Long ONE_HUNDRED = 100L;
    private static final int ONE_HUNDRED_INT = 100;
    private static final Long TWO_THOUSAND = 2000L;
    private static final int TWO_THOUSAN_INT = 2000;

    @BeforeEach
    public void setup() {
        CollectorRegistry.defaultRegistry.clear();
        // Configure WebTestClient with default tenantId header for all requests
        webTestClient = webTestClient.mutate()
                .defaultHeader("tenantId", "ecsp")
                .build();
        setupTenantConfiguration();
    }

    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }
    
    private void setupTenantConfiguration() {
        // Set up mock tenant configuration
        ClientRegistrationProperties clientRegistrationProperties = new ClientRegistrationProperties();
        clientRegistrationProperties.setDefaultStatus("approved");
        clientRegistrationProperties.setRefreshTokenValidity(INTEGER_3600);
        clientRegistrationProperties.setAccessTokenValidity(INTEGER_3600);
        clientRegistrationProperties.setAuthorizationCodeValidity(INTEGER_300);
        UserManagementTenantProperties tenantProperties = new UserManagementTenantProperties();
        tenantProperties.setClientRegistration(clientRegistrationProperties);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
    }

    @Test
    void testAddClient() {
        when(clientRepository.existsByClientId("testClient")).thenReturn(false);
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());

        webTestClient.post()
            .uri("/v1/oauth2/client")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED);

    }

    @Test
    void testAddClientCorrelationIdMissing() {
        when(clientRepository.existsByClientId("testClient")).thenReturn(false);
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());

        webTestClient.post()
            .uri("/v1/oauth2/client")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
            })
            .bodyValue(rc)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void testAddClientWithDefaultTokenSettings() {
        when(clientRepository.existsByClientId("testClient")).thenReturn(false);
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        webTestClient.post()
            .uri("/v1/oauth2/client")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void testAddClient_clientAlreadyExist() {
        when(clientRepository.existsByClientId("testClient")).thenReturn(true);
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        webTestClient.post()
            .uri("/v1/oauth2/client")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testAddClient_RedirectUriMissing() {
        when(clientRepository.existsByClientId("testClient")).thenReturn(false);
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.getAuthorizationGrantTypes().add("authorization_code");
        rc.setRedirectUris(null);
        webTestClient.post()
            .uri("/v1/oauth2/client")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testAddClient_clientIdMissing() {
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setClientId(null);

        webTestClient.post().uri("/v1/oauth2/client").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testAddClient_clientNameMissing() {
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setClientName(null);

        webTestClient.post().uri("/v1/oauth2/client").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testAddClient_clientSecretMissing() {
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setClientSecret(null);

        webTestClient.post().uri("/v1/oauth2/client").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testAddClient_clientScopesMissing() {
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setScopes(null);

        webTestClient.post().uri("/v1/oauth2/client").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUpdateClient() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(getClient()));
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        webTestClient.put()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testUpdateClientCorrelationIdMissing() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(getClient()));
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        webTestClient.put()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testUpdateClient_redirectUriMissing() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(getClient()));
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.getAuthorizationGrantTypes().add("authorization_code");
        rc.setRedirectUris(null);
        webTestClient.put()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUpdateClient_ClientNotFound() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.empty());
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        webTestClient.put()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateClient_ClientDeleted() {
        ClientEntity client = getClient();
        client.setStatus("deleted");
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.empty());
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setStatus(null);
        webTestClient.put().uri("/v1/oauth2/client/testClient").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateClient_ClientReactivated() {
        ClientEntity client = getClient();
        client.setStatus("deleted");
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(client));
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setStatus("approved");
        webTestClient.put().uri("/v1/oauth2/client/testClient").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testUpdateClient_ClientReactivatedFailed() {
        ClientEntity client = getClient();
        client.setStatus("deleted");
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(client));
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setStatus("approve");
        webTestClient.put().uri("/v1/oauth2/client/testClient").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateClient_ClientStatus() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(getClient()));
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        webTestClient.put()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testUpdateClient_ClientStatusNotSupported() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(getClient()));
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setStatus("test");
        webTestClient.put()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUpdateClient_ClientMissingRedirectUrls() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(getClient()));
        RegisteredClientDetails rc = getRegisteredClientDetails(new RegisteredClientDetails());
        rc.setRedirectUris(null);
        rc.getAuthorizationGrantTypes().add("authorization_code");
        webTestClient.put()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Content-Type", "application/json");
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .bodyValue(rc).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testGetClient() {
        when(clientRepository.findByClientIdAndStatus(anyString(), anyString())).thenReturn(Optional.of(getClient()));

        webTestClient.get()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetClientCorrelationIdMissing() {
        when(clientRepository.findByClientIdAndStatus("testClient", "approved")).thenReturn(Optional.of(getClient()));

        webTestClient.get()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Accept", "application/json");
            })
            .exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetClient_statusDeleted() {
        ClientEntity client = getClient();
        client.setStatus("deleted");
        when(clientRepository.findByClientIdAndStatus("testClient", null)).thenReturn(Optional.of(client));
        webTestClient.get().uri("/v1/oauth2/client/testClient").headers(http -> {
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetClientWithStatus() {
        when(clientRepository.findByClientIdAndStatus("testClient", "approved")).thenReturn(Optional.of(getClient()));
        webTestClient.get()
            .uri("/v1/oauth2/client/testClient?status=approved")
            .headers(http -> {
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetClient_ClientNotFound() {
        webTestClient.get().uri("/v1/oauth2/client/testClient").headers(http -> {
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testDeleteClient() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(getClient()));
        webTestClient.delete()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testDeleteClientCorrelationIdMissing() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(getClient()));
        webTestClient.delete()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Accept", "application/json");
            })
            .exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testDeleteClient_clientNotFound() {
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.empty());
        webTestClient.delete()
            .uri("/v1/oauth2/client/testClient")
            .headers(http -> {
                http.add("Accept", "application/json");
                http.add(ApiConstants.CORRELATION_ID, "12345");
            })
            .exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testDeleteClient_clientAlreadyDeleted() {
        ClientEntity client = getClient();
        client.setStatus("deleted");
        when(clientRepository.findByClientId("testClient")).thenReturn(Optional.of(client));
        webTestClient.delete().uri("/v1/oauth2/client/testClient").headers(http -> {
            http.add("Accept", "application/json");
            http.add(ApiConstants.CORRELATION_ID, "12345");
        }).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ClientEntity getClient() {
        ClientEntity c = new ClientEntity();
        c.setId(new BigInteger("1"));
        c.setClientId("testClient");
        c.setClientName("name");
        c.setRedirectUrls("http://login.com/test");
        c.setSecret("5umr13KbAzChYkW4Q2hhbmdlTWXc+7vnicRT8go5zanqW6AKypl+twCa9q0=");
        c.setGrantTypes("client_credentials");
        c.setCreatedBy("test");
        c.setClientSecretExpireAt(Instant.now());
        c.setCreateDate(Instant.now());
        c.setAuthorizationCodeValidity(ONE_HUNDRED);
        c.setScopes("SelfManage,UIDAMSystem");
        c.setAccessTokenValidity(TWO_THOUSAND);
        c.setAdditionalInformation("abc");
        c.setApprovedDate(Instant.now());
        c.setAuthenticationMethods("client_secret_basic");
        c.setUpdatedBy("test");
        c.setApprovedBy("test");
        c.setStatus("approved");
        return c;
    }

    private RegisteredClientDetails getRegisteredClientDetails(RegisteredClientDetails registeredClientDetails) {
        RegisteredClientDetails rc = new RegisteredClientDetails();
        rc.setClientId("testClient");
        rc.setClientName("name");
        List<String> redirectUrls = new ArrayList<>();
        redirectUrls.add("http://login.com/test");
        rc.setRedirectUris(redirectUrls);
        rc.setClientSecret("secret23");
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        rc.setAuthorizationGrantTypes(grantTypes);
        rc.setCreatedBy("test");
        rc.setAuthorizationCodeValidity(ONE_HUNDRED_INT);
        rc.setRefreshTokenValidity(ONE_HUNDRED_INT);
        Set<String> scopes = new HashSet<>();
        scopes.add("SelfManage");
        rc.setScopes(scopes);
        rc.setAccessTokenValidity(TWO_THOUSAN_INT);
        rc.setAdditionalInformation("abc");
        List<String> authMethods = new ArrayList<>();
        authMethods.add("client_secret_basic");
        rc.setClientAuthenticationMethods(authMethods);
        rc.setAdditionalInformation("abc");
        rc.setRequireAuthorizationConsent(false);
        rc.setStatus("approved");
        return rc;
    }
}
