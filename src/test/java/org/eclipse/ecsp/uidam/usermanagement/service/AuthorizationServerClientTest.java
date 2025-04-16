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

import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.BaseResponseFromAuthorization;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
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
 * Test class for AuthorizationServerService.
 */
class AuthorizationServerClientTest {

    @Mock
    private WebClient webClientMock;

    @Mock
    ApplicationProperties applicationProperties;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;

    @InjectMocks
    private AuthorizationServerClient authorizationServerClient;

    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;

    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void revokeTokenByAdmin() {
        when(applicationProperties.getAuthServerRevokeTokenUrl()).thenReturn("/mockRevokeUri");
        when(webClientMock.method(any())).thenReturn(requestBodyUriSpecMock);
        when(requestBodySpecMock.contentType(any())).thenReturn(requestBodySpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.headers(any())).thenReturn(requestBodySpecMock);
        
        when(requestBodySpecMock.body(any())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        BaseResponseFromAuthorization baseResponseFromAuthorization = new BaseResponseFromAuthorization();
        when(responseSpecMock.bodyToMono(ArgumentMatchers.<Class<BaseResponseFromAuthorization>>notNull()))
            .thenReturn(Mono.just(baseResponseFromAuthorization));
        assertEquals(baseResponseFromAuthorization,
            authorizationServerClient.revokeTokenByAdmin("dummyToken", "john"));

    }

}
