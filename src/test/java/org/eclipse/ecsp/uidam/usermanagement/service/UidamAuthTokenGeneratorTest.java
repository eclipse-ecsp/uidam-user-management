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

package org.eclipse.ecsp.uidam.usermanagement.service;

import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.AccessTokenDetails;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.AuthServerProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * UIDAM auth token generator Test Class.
 */
class UidamAuthTokenGeneratorTest {

    @InjectMocks
    private UidamAuthTokenGenerator uidamAuthTokenGenerator;

    @Mock
    TenantConfigurationService tenantConfigurationService;

    @Mock
    UserManagementTenantProperties tenantProperties;

    @Mock
    AuthServerProperties authServerProperties;

    @Mock
    private WebClient webClientMock;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;

    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;

    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getAuthServer()).thenReturn(authServerProperties);
    }

    @Test
    void fetchSpringAuthToken() {
        when(authServerProperties.getTokenUrl()).thenReturn("/token");
        when(authServerProperties.getClientId()).thenReturn("dummyClient");
        when(authServerProperties.getClientSecret()).thenReturn("dummySecret");

        when(webClientMock.method(any())).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.header(any(), any())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.body(any())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        AccessTokenDetails accessTokenDetails = new AccessTokenDetails();
        accessTokenDetails.setAccessToken("dummyToken");
        accessTokenDetails.setScope("OAuth2ClientMgmt SelfManage RevokeToken");
        accessTokenDetails.setTokenType("Bearer");
        accessTokenDetails.setExpiresIn("3599");
        when(responseSpecMock.bodyToMono(ArgumentMatchers.<Class<AccessTokenDetails>>notNull()))
            .thenReturn(Mono.just(accessTokenDetails));
        assertEquals(accessTokenDetails, uidamAuthTokenGenerator.fetchUidamAuthToken());
    }
}
