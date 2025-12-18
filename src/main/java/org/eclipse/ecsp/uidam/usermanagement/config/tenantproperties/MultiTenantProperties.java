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

package org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Multi-tenant properties configuration for User Management service.
 * Uses prefix-based property binding with pattern: tenants.profile.{tenantId}.{property}
 * Follows the Auth Server pattern for consistent multi-tenant configuration.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "tenants")
@Validated
public class MultiTenantProperties {
    
    /**
     * Map of tenant-specific configurations for User Management.
     * Spring automatically binds tenants.profile.{tenantId}.* properties to this map.
     * Key: tenant ID (e.g., "ecsp", "sdp"), Value: tenant properties.
     */
    @Valid
    private Map<String, UserManagementTenantProperties> profile = new HashMap<>();
    
    /**
     * Get tenant properties for a specific tenant ID.
     *
     * @param tenantId the tenant ID
     * @return UserManagementTenantProperties for the specified tenant, or null if not found
     */
    public UserManagementTenantProperties getTenantProperties(String tenantId) {
        return profile.get(tenantId);
    }
    
    /**
     * Get all configured tenant IDs.
     *
     * @return Set of all tenant IDs
     */
    public Set<String> getAllTenantIds() {
        return profile.keySet();
    }
    
    /**
     * Check if a tenant exists in the configuration.
     *
     * @param tenantId the tenant ID to check
     * @return true if tenant exists, false otherwise
     */
    public boolean hasTenant(String tenantId) {
        return profile.containsKey(tenantId);
    }
}
