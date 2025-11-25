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

import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.MultiTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service for managing tenant configurations in User Management.
 * Provides access to tenant-specific properties and configuration.
 * Follows the same method signatures as the Auth Server TenantConfigurationService.
 */
@Service
@Profile("!test")
public class TenantConfigurationService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConfigurationService.class);
    
    private final MultiTenantProperties multiTenantProperties;
    
    public TenantConfigurationService(MultiTenantProperties multiTenantProperties) {
        this.multiTenantProperties = multiTenantProperties;
    }
    
    /**
     * Get tenant properties for the current tenant from TenantContext.
     * This method follows the Auth Server pattern.
     *
     * @return UserManagementTenantProperties for the current tenant
     */
    public UserManagementTenantProperties getTenantProperties() {
        String currentTenant = TenantContext.getCurrentTenant();
        return getTenantProperties(currentTenant);
    }
    
    /**
     * Get tenant properties for a specific tenant ID.
     *
     * @param tenantId the tenant ID
     * @return UserManagementTenantProperties for the specified tenant, or null if not found
     */
    public UserManagementTenantProperties getTenantProperties(String tenantId) {
        UserManagementTenantProperties properties = multiTenantProperties.getTenantProperties(tenantId);
        if (properties == null) {
            LOGGER.warn("No tenant configuration found for tenant: {}", tenantId);
        }
        return properties;
    }
    
    /**
     * Check if a tenant exists in the configuration.
     *
     * @param tenantId the tenant ID to check
     * @return true if tenant exists, false otherwise
     */
    public boolean tenantExists(String tenantId) {
        boolean exists = multiTenantProperties.hasTenant(tenantId);
        LOGGER.debug("Tenant '{}' exists: {}", tenantId, exists);
        return exists;
    }
    
    /**
     * Get all configured tenant IDs.
     *
     * @return Set of all tenant IDs
     */
    public Set<String> getAllTenantIds() {
        return multiTenantProperties.getAllTenantIds();
    }
}
