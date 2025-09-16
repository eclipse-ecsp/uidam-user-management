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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Custom AbstractRoutingDataSource Configuration for Multi-tenancy in User Management service. Routes database
 * connections based on the current tenant context.
 * This class extends Spring's AbstractRoutingDataSource to provide tenant-specific database routing. The tenant context
 * is determined by the TenantResolutionFilter and stored in TenantContext.
 * Database routing flow: 1. TenantResolutionFilter extracts tenant from request (header/path/parameter) 2.
 * TenantContext.setCurrentTenant(tenantId) stores tenant in thread-local 3. This router's determineCurrentLookupKey()
 * returns current tenant ID 4. Spring routes database operations to tenant-specific DataSource 5.
 * TenantResolutionFilter clears context after request completion
 */
public class MultiTenantDataSource extends AbstractRoutingDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTenantDataSource.class);

    /**
     * Determine the current lookup key for routing to the appropriate DataSource.
     * This method is called by Spring's AbstractRoutingDataSource for every database operation.
     *
     * @return the tenant ID from current thread context, used as DataSource lookup key
     */
    @Override
    protected String determineCurrentLookupKey() {
        String tenantId = TenantContext.getCurrentTenant();
        
        if (tenantId != null) {
            LOGGER.debug("Routing database operation to tenant: {}", tenantId);
        } else {
            LOGGER.warn("No tenant context found for database routing - using null key");
        }
        
        return tenantId;
    }

    /**
     * Override to provide custom DataSource resolution when lookup key is not found.
     * This is called when determineCurrentLookupKey() returns a key that doesn't exist
     * in the configured target DataSources map.
     *
     * @return DataSource for the current tenant context
     */
    @Override
    protected javax.sql.DataSource determineTargetDataSource() {
        try {
            return super.determineTargetDataSource();
        } catch (IllegalStateException e) {
            String tenantId = TenantContext.getCurrentTenant();
            LOGGER.error("Failed to determine target DataSource for tenant '{}': {}", tenantId, e.getMessage());
            throw new IllegalStateException(
                String.format("No DataSource configured for tenant '%s'. "
                            + "Please verify tenant configuration and database setup.", tenantId),
                    e);
        }
    }
}
