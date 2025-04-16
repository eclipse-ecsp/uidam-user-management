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

package org.eclipse.ecsp.uidam.usermanagement.cache.impl;

import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.AccessTokenDetails;
import org.eclipse.ecsp.uidam.usermanagement.cache.CacheTokenService;
import org.eclipse.ecsp.uidam.usermanagement.cache.TokenCacheDetails;
import org.eclipse.ecsp.uidam.usermanagement.service.UidamAuthTokenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.ACCESS_TOKEN;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SEC_60;

/**
 * Implementation of the CacheTokenService interface using a HashMap.
 * This class provides methods to retrieve and store access tokens in a cache.
 */
@Service
public class HashMapCacheTokenImpl implements CacheTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashMapCacheTokenImpl.class);

    private final Map<String, TokenCacheDetails> tokenMap = new ConcurrentHashMap<>();
    private final Object cacheLock = new Object();

    @Autowired
    @Lazy
    UidamAuthTokenGenerator uidamAuthTokenGenerator;

    /**
     * Retrieves the access token from the cache. If the token is not present or is expired, a new token is fetched and
     * stored in the cache.
     *
     * @return the access token as a String
     */
    @Override
    public String getAccessToken() {
        LOGGER.debug("Getting Access Token from cache");
        if (tokenMap.containsKey(ACCESS_TOKEN)) {
            TokenCacheDetails tokenCacheDetails = tokenMap.get(ACCESS_TOKEN);
            if (isTokenValid(tokenCacheDetails.getExpiresAt())) {
                return tokenCacheDetails.getAccessToken();
            } else {
                tokenMap.clear();
            }
        }
        return putAccessToken();
    }

    /**
     * Fetches a new access token and stores it in the cache.
     *
     * @return the new access token as a String, or null if the token could not be fetched
     */
    private String putAccessToken() {
        LOGGER.info("Waiting for lock to generate new Access Token");
        synchronized (cacheLock) {
            if (tokenMap.containsKey(ACCESS_TOKEN)) {
                TokenCacheDetails tokenCacheDetails = tokenMap.get(ACCESS_TOKEN);
                if (isTokenValid(tokenCacheDetails.getExpiresAt())) {
                    return tokenCacheDetails.getAccessToken();
                } else {
                    LOGGER.info("Access Token has expired, generating new Access Token");
                }
            } else {
                LOGGER.info("Access Token not present in cache, generating new Access Token");
            }
            AccessTokenDetails accessTokenDetails = uidamAuthTokenGenerator.fetchUidamAuthToken();
            if (accessTokenDetails == null) {
                LOGGER.error("Failed to fetch Access Token");
                return null;
            }
            TokenCacheDetails tokenCacheDetails = new TokenCacheDetails();
            tokenCacheDetails.setAccessToken(accessTokenDetails.getAccessToken());
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(Long.parseLong(accessTokenDetails.getExpiresIn())).minusSeconds(SEC_60);
            tokenCacheDetails.setExpiresAt(expiresAt);
            LOGGER.debug("Putting Access Token in cache with expiration time: {}", expiresAt);
            tokenMap.put(ACCESS_TOKEN, tokenCacheDetails);
            return tokenCacheDetails.getAccessToken();
        }
    }

    /**
     * Checks if the token is still valid based on its expiration time.
     *
     * @param expiresAt the expiration time of the token
     * @return true if the token is valid, false otherwise
     */
    private boolean isTokenValid(Instant expiresAt) {
        Instant now = Instant.now();
        return now.isBefore(expiresAt);
    }

}
