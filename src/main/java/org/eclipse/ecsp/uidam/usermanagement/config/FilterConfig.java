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

import org.eclipse.ecsp.uidam.usermanagement.filter.TenantResolutionFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * Configuration for registering filters in the User Management service.
 * Ensures proper filter ordering and URL pattern matching for header-based tenant resolution.
 */
@Configuration
@Profile("!test")
public class FilterConfig {

    private static final int INTEGER_10 = 10;

    /**
     * Register the TenantResolutionFilter with proper ordering and URL patterns.
     * This filter must run early in the chain to establish tenant context before
     * any business logic or security processing occurs.
     *
     * @param tenantResolutionFilter the tenant resolution filter to register
     * @return FilterRegistrationBean configuration for the tenant filter
     */
    @Bean
    public FilterRegistrationBean<TenantResolutionFilter> tenantResolutionFilterRegistration(
            TenantResolutionFilter tenantResolutionFilter) {
        
        FilterRegistrationBean<TenantResolutionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(tenantResolutionFilter);
        
        // Set high priority to run early in filter chain
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + INTEGER_10);
        
        // Apply to all User Management API endpoints
        registration.addUrlPatterns(
            "/v1/*",           // All v1 API endpoints
            "/v2/*"            // All v2 API endpoints
        );
        
        // Filter name for debugging and monitoring
        registration.setName("TenantResolutionFilter");
        
        return registration;
    }
}
