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

import org.apache.http.entity.ContentType;
import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.AccessTokenDetails;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.GRANT_TYPE_KEY;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SCOPE_KEY;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UIDAM_AUTH_CLIENT_CREDENTIALS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UIDAM_AUTH_CLIENT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UIDAM_AUTH_CLIENT_SECRET;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UIDAM_AUTH_SCOPE_VALUE;
import static org.springframework.http.HttpMethod.POST;

/**
 * UIDAM auth token generator helps in fetching the token from authorization server MS of RevokeToken scope.
 */
@Service
@Lazy
public class UidamAuthTokenGenerator {

    @Autowired
    TenantConfigurationService tenantConfigurationService;

    @Autowired
    private WebClient webClient;

    public static final String CONTENT_TYPE = "Content-Type";

    private static final Logger LOGGER = LoggerFactory.getLogger(UidamAuthTokenGenerator.class);

    /**
     * Method to fetch token from authorization server MS of RevokeToken scope.
     *
     * @return String token which has RevokeToken scope
     */
    public AccessTokenDetails fetchUidamAuthToken() {
        LOGGER.info("fetchUidamAuthToken START");
        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.toString());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(GRANT_TYPE_KEY, UIDAM_AUTH_CLIENT_CREDENTIALS);
        map.add(SCOPE_KEY, UIDAM_AUTH_SCOPE_VALUE);
        map.add(UIDAM_AUTH_CLIENT_ID, tenantConfigurationService.getTenantProperties().getAuthServer().getClientId());
        map.add(UIDAM_AUTH_CLIENT_SECRET,
                tenantConfigurationService.getTenantProperties().getAuthServer().getClientSecret());

        try {
            AccessTokenDetails response = webClient.method(POST)
                .uri(tenantConfigurationService.getTenantProperties().getAuthServer().getTokenUrl())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .bodyToMono(AccessTokenDetails.class)
                .block();

            LOGGER.info("fetchUidamAuthToken END. ## Fetched uidam auth token successfully.");
            return response;
        } catch (WebClientResponseException ex) {
            LOGGER.error("Web client error while fetching token contains scope: {} from authorization-server, ex: {}",
                UIDAM_AUTH_SCOPE_VALUE, ex);
            throw new ApplicationRuntimeException("Exception while fetching UIDAM token");
        }
    }
}

