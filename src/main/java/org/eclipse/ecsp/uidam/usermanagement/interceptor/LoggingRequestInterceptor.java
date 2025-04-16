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

import org.eclipse.ecsp.uidam.usermanagement.wrapper.BodyInserterWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import reactor.core.publisher.Mono;
import java.util.stream.Collectors;

/**
 * Class to intercept webClient request and response and add result to logger.
 */
public class LoggingRequestInterceptor implements AsyncHandlerInterceptor {

    private LoggingRequestInterceptor() {

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingRequestInterceptor.class);
    private static final String LOG_REQ_PATTERN = "Request: request method: {}, request URI: {}, request headers: {}, "
            + "request body: {}, ";
    private static final String LOG_RES_PATTERN = "Response: response status code: {}, response headers: {}";

    /**
     * Interceptor to log request and response.
     *
     * @return ExchangeFilterFunction
     */
    public static ExchangeFilterFunction interceptWebClientHttpRequestAndResponse() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (LOGGER.isInfoEnabled()) {
                String headers = clientRequest.headers().keySet().stream()
                    .map(key -> key + "=" + clientRequest.headers().get(key).get(0))
                    .collect(Collectors.joining(", ", "{", "}"));
                Object body = null;
                if (clientRequest.body() instanceof BodyInserterWrapper<?> bodyInserterWrapper) {
                    body = bodyInserterWrapper.getBody();
                }
                LOGGER.info(LOG_REQ_PATTERN, clientRequest.method(), clientRequest.url(), headers, body);
            }
            return Mono.just(clientRequest);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_RES_PATTERN, clientResponse.statusCode(), clientResponse.headers().asHttpHeaders());
            }
            return Mono.just(clientResponse);
        }));
    }
}

