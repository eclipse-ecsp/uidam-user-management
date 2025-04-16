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
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/**
 * Http client interceptor to apply on WebClient, adding 'correlationId' header to the request before sending it.
 */
public class ClientAddCorrelationIdInterceptor {
    
    private ClientAddCorrelationIdInterceptor() {

    }


    /**
     * Interceptor to validate clientRequest is having correlationId in headers.
     *
     * @return ExchangeFilterFunction
     */
    public static ExchangeFilterFunction addCorrelationIdAndContentType() {
        return (clientRequest, next) -> {
            String correlationId = MDC.get(ApiConstants.CORRELATION_ID);
            ClientRequest modifiedClientRequest = ClientRequest.from(clientRequest)
                .header(ApiConstants.CORRELATION_ID, correlationId)
                .build();
            return next.exchange(modifiedClientRequest);
        };
    }
}
