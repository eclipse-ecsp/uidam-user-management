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

package org.eclipse.ecsp.uidam.usermanagement.config;

import org.eclipse.ecsp.uidam.usermanagement.filter.SecurityHeadersFilter;
import org.eclipse.ecsp.uidam.usermanagement.filter.TenantResolutionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class FilterConfigTest {

    private static final int TENANT_FILTER_ORDER_OFFSET = 10;

    private FilterConfig filterConfig;

    @Mock
    private TenantResolutionFilter tenantResolutionFilter;

    @BeforeEach
    void setUp() {
        filterConfig = new FilterConfig();
    }

    @Test
    void tenantResolutionFilterRegistration_ReturnsBean() {
        FilterRegistrationBean<TenantResolutionFilter> reg =
            filterConfig.tenantResolutionFilterRegistration(tenantResolutionFilter);

        assertNotNull(reg);
        assertEquals(tenantResolutionFilter, reg.getFilter());
    }

    @Test
    void tenantResolutionFilterRegistration_HasCorrectOrder() {
        FilterRegistrationBean<TenantResolutionFilter> reg =
            filterConfig.tenantResolutionFilterRegistration(tenantResolutionFilter);

        int expectedOrder = Ordered.HIGHEST_PRECEDENCE + TENANT_FILTER_ORDER_OFFSET;
        assertEquals(expectedOrder, reg.getOrder());
    }

    @Test
    void tenantResolutionFilterRegistration_HasApiUrlPatterns() {
        FilterRegistrationBean<TenantResolutionFilter> reg =
            filterConfig.tenantResolutionFilterRegistration(tenantResolutionFilter);

        assertTrue(reg.getUrlPatterns().contains("/v1/*"));
        assertTrue(reg.getUrlPatterns().contains("/v2/*"));
    }

    @Test
    void securityHeadersFilterRegistration_ReturnsBean() {
        FilterRegistrationBean<SecurityHeadersFilter> reg =
            filterConfig.securityHeadersFilterRegistration();

        assertNotNull(reg);
        assertNotNull(reg.getFilter());
        assertTrue(reg.getFilter() instanceof SecurityHeadersFilter);
    }

    @Test
    void securityHeadersFilterRegistration_HasHighestPriority() {
        FilterRegistrationBean<SecurityHeadersFilter> reg =
            filterConfig.securityHeadersFilterRegistration();

        assertEquals(Ordered.HIGHEST_PRECEDENCE, reg.getOrder());
    }

    @Test
    void securityHeadersFilterRegistration_AppliesToAllEndpoints() {
        FilterRegistrationBean<SecurityHeadersFilter> reg =
            filterConfig.securityHeadersFilterRegistration();

        assertTrue(reg.getUrlPatterns().contains("/*"));
    }

    @Test
    void securityHeadersFilter_RunsBeforeTenantFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> secReg =
            filterConfig.securityHeadersFilterRegistration();
        FilterRegistrationBean<TenantResolutionFilter> tenantReg =
            filterConfig.tenantResolutionFilterRegistration(tenantResolutionFilter);

        assertTrue(secReg.getOrder() < tenantReg.getOrder());
    }
}
