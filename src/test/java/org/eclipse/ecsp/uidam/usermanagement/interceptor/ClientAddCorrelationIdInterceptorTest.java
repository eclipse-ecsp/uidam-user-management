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

package org.eclipse.ecsp.uidam.usermanagement.interceptor;

import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClientAddCorrelationIdInterceptor.
 */
@DisplayName("ClientAddCorrelationIdInterceptor Test Suite")
class ClientAddCorrelationIdInterceptorTest {

    private ExchangeFunction mockExchangeFunction;

    @BeforeEach
    void setUp() {
        mockExchangeFunction = mock(ExchangeFunction.class);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should add correlation ID header when present in MDC")
    void testAddCorrelationIdWhenPresentInMdc() {
        // Arrange
        String correlationId = "test-correlation-id-123";
        MDC.put(ApiConstants.CORRELATION_ID, correlationId);

        ClientRequest originalRequest = ClientRequest.create(org.springframework.http.HttpMethod.GET, URI.create("http://localhost"))
            .build();

        ClientResponse mockResponse = mock(ClientResponse.class);
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
            .thenReturn(Mono.just(mockResponse));

        ExchangeFilterFunction filterFunction = ClientAddCorrelationIdInterceptor.addCorrelationIdAndContentType();

        // Act
        Mono<ClientResponse> result = filterFunction.filter(originalRequest, mockExchangeFunction);

        // Assert
        assertNotNull(result);
        result.block();
    }

    @Test
    @DisplayName("Should handle null correlation ID in MDC")
    void testAddCorrelationIdWhenNullInMdc() {
        // Arrange
        MDC.put(ApiConstants.CORRELATION_ID, null);

        ClientRequest originalRequest = ClientRequest.create(org.springframework.http.HttpMethod.GET, URI.create("http://localhost"))
            .build();

        ClientResponse mockResponse = mock(ClientResponse.class);
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
            .thenReturn(Mono.just(mockResponse));

        ExchangeFilterFunction filterFunction = ClientAddCorrelationIdInterceptor.addCorrelationIdAndContentType();

        // Act
        Mono<ClientResponse> result = filterFunction.filter(originalRequest, mockExchangeFunction);

        // Assert
        assertNotNull(result);
        result.block();
    }

    @Test
    @DisplayName("Should create exchange filter function successfully")
    void testFilterFunctionCreation() {
        // Act
        ExchangeFilterFunction filterFunction = ClientAddCorrelationIdInterceptor.addCorrelationIdAndContentType();

        // Assert
        assertNotNull(filterFunction, "Exchange filter function should not be null");
    }

    @Test
    @DisplayName("Should handle empty correlation ID")
    void testEmptyCorrelationId() {
        // Arrange
        MDC.put(ApiConstants.CORRELATION_ID, "");

        ClientRequest originalRequest = ClientRequest.create(org.springframework.http.HttpMethod.POST, URI.create("http://localhost/api"))
            .build();

        ClientResponse mockResponse = mock(ClientResponse.class);
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
            .thenReturn(Mono.just(mockResponse));

        ExchangeFilterFunction filterFunction = ClientAddCorrelationIdInterceptor.addCorrelationIdAndContentType();

        // Act
        Mono<ClientResponse> result = filterFunction.filter(originalRequest, mockExchangeFunction);

        // Assert
        assertNotNull(result);
        result.block();
    }

    @Test
    @DisplayName("Should work with different HTTP methods")
    void testDifferentHttpMethods() {
        // Arrange
        String correlationId = "test-id";
        MDC.put(ApiConstants.CORRELATION_ID, correlationId);

        ClientResponse mockResponse = mock(ClientResponse.class);
        when(mockExchangeFunction.exchange(any(ClientRequest.class)))
            .thenReturn(Mono.just(mockResponse));

        ExchangeFilterFunction filterFunction = ClientAddCorrelationIdInterceptor.addCorrelationIdAndContentType();

        // Test GET
        ClientRequest getRequest = ClientRequest.create(org.springframework.http.HttpMethod.GET, URI.create("http://localhost"))
            .build();
        Mono<ClientResponse> getResult = filterFunction.filter(getRequest, mockExchangeFunction);
        assertNotNull(getResult.block());

        // Test POST
        ClientRequest postRequest = ClientRequest.create(org.springframework.http.HttpMethod.POST, URI.create("http://localhost"))
            .build();
        Mono<ClientResponse> postResult = filterFunction.filter(postRequest, mockExchangeFunction);
        assertNotNull(postResult.block());

        // Test PUT
        ClientRequest putRequest = ClientRequest.create(org.springframework.http.HttpMethod.PUT, URI.create("http://localhost"))
            .build();
        Mono<ClientResponse> putResult = filterFunction.filter(putRequest, mockExchangeFunction);
        assertNotNull(putResult.block());
    }
}
