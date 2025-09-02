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

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.BaseResponseFromAuthorization;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USERNAME2;
import static org.springframework.http.HttpMethod.POST;

/**
 * Class to make uidam authorization server microservice api calls.
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthorizationServerClient {
    @Autowired
    TenantConfigurationService tenantConfigurationService;

    @Autowired
    private WebClient webClient;

    private static final String BEARER = "Bearer ";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationServerClient.class);

    /**
     * Method to revoke token.
     *
     * @param authorization Authorization token with RevokeToken scope
     * @param username used for revoke token
     * @return BaseResponseFromAuthorization response from Authorization server
     */
    public BaseResponseFromAuthorization revokeTokenByAdmin(String authorization, String username) {
        LOGGER.debug("revokeTokenByAdmin START");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Revoking token for username: {}", username);
        }

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(USERNAME2, username);
        String token = BEARER + authorization;
        try {
            return webClient.method(POST)
                .uri(tenantConfigurationService.getTenantProperties().getAuthServer().getRevokeTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(httpHeaders -> httpHeaders.add(HttpHeaders.AUTHORIZATION, token))
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .bodyToMono(BaseResponseFromAuthorization.class)
                .block();

        } catch (WebClientResponseException ex) {
            LOGGER.error("Web client error while revoking token from authorization-server for username: {}, ex: {}",
                username, ex);
            throw new ApplicationRuntimeException("Exception while revoking token");
        } finally {
            LOGGER.debug("revokeTokenByAdmin END");
        }
    }
}
