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

package org.eclipse.ecsp.uidam.common.test;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import java.io.IOException;

/**
 * Test configuration for tenant context management.
 * Ensures tenant context is properly set for all test requests.
 */
@TestConfiguration
public class TestTenantConfiguration {

    /**
     * A filter that runs early in the filter chain to ensure tenant context is set from the header.
     * This filter is specifically for tests and runs before the actual TenantResolutionFilter.
     */
    @Bean
    @Primary
    @Order(Integer.MIN_VALUE) // Run this filter first
    public Filter testTenantContextFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String tenantId = httpRequest.getHeader("tenantId");
                
                // If tenant header is present, set it in the context
                if (tenantId != null && !tenantId.isEmpty()) {
                    TenantContext.setCurrentTenant(tenantId);
                }
                
                try {
                    chain.doFilter(request, response);
                } finally {
                    // Don't clear here - let the actual TenantResolutionFilter handle cleanup
                }
            }
        };
    }
}
