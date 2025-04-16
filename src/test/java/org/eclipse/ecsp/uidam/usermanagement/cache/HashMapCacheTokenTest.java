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

package org.eclipse.ecsp.uidam.usermanagement.cache;

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.AccessTokenDetails;
import org.eclipse.ecsp.uidam.usermanagement.cache.impl.HashMapCacheTokenImpl;
import org.eclipse.ecsp.uidam.usermanagement.service.UidamAuthTokenGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

/**
 * This class tests the functionality of accessing and caching tokens under various conditions.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {HashMapCacheTokenImpl.class})
@MockBean(JpaMetamodelMappingContext.class)
class HashMapCacheTokenTest {

    @Autowired
    private CacheTokenService cacheTokenService;

    @MockBean
    UidamAuthTokenGenerator uidamAuthTokenGenerator;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    /**
     * Tests the retrieval and caching of an access token when the token generator returns null.
     * Verifies that the service returns null when no token is fetched.
     */
    @Test
    @Order(1)
    void getAccessTokenNullResponse() {
        doReturn(null).when(uidamAuthTokenGenerator).fetchUidamAuthToken();
        assertNull(cacheTokenService.getAccessToken());
    }

    /**
     * Tests the retrieval and caching of an access token with immediate expiration.
     * Verifies that a token is fetched and cached correctly.
     */
    @Test
    @Order(2)
    void getAccessTokenAndPutInCache() {
        AccessTokenDetails accessTokenDetails = new AccessTokenDetails();
        accessTokenDetails.setAccessToken("token");
        accessTokenDetails.setExpiresIn("0");
        doReturn(accessTokenDetails).when(uidamAuthTokenGenerator).fetchUidamAuthToken();
        String accessToken = cacheTokenService.getAccessToken();
        assertEquals("token", accessToken);
    }

    /**
     * Tests the retrieval of a new access token when the cached token has expired.
     * Ensures that a new token is fetched, cached, and returned when the existing token in the cache is expired.
     */
    @Test
    @Order(3)
    void getAccessTokenWhenExpired() {
        AccessTokenDetails accessTokenDetails = new AccessTokenDetails();
        accessTokenDetails.setAccessToken("new_token");
        accessTokenDetails.setExpiresIn("3599");
        doReturn(accessTokenDetails).when(uidamAuthTokenGenerator).fetchUidamAuthToken();
        String accessToken = cacheTokenService.getAccessToken();
        assertEquals("new_token", accessToken);
    }

    /**
     * Tests the retrieval of an access token from the cache.
     * Assumes the token is already cached and verifies that the cached token is returned.
     */
    @Test
    @Order(4)
    void getAccessTokenFromCache() {
        String accessToken = cacheTokenService.getAccessToken();
        assertEquals("new_token", accessToken);
    }

}
