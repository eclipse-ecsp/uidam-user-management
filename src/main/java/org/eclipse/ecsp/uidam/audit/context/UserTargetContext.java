/*
 * Copyright (c) 2024 - 2025 Harman International
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
 */

package org.eclipse.ecsp.uidam.audit.context;

import lombok.Builder;
import lombok.Data;
import org.eclipse.ecsp.audit.context.TargetContext;

import java.util.HashMap;
import java.util.Map;

/**
 * User Target Context Implementation.
 * 
 * <p>Captures details about the user (target) being acted upon in UIDAM.</p>
 * 
 * <p><strong>PII Fields (auto-masked):</strong></p>
 * <ul>
 *   <li>username - User's login identifier (partially masked)</li>
 *   <li>email - User's email address (partially masked)</li>
 *   <li>firstName - User's first name (fully masked per SENSITIVE_PERSONAL_FIELDS)</li>
 *   <li>lastName - User's last name (fully masked per SENSITIVE_PERSONAL_FIELDS)</li>
 *   <li>accountName - Account display name (partially masked)</li>
 * </ul>
 *
 */
@Data
@Builder
public class UserTargetContext implements TargetContext {
    
    /**
     * User ID from UIDAM users table.
     * This will be stored in both target_id field (as VARCHAR) and in target_context JSON.
     */
    private String userId;
    
    /**
     * Username for the user (e.g., email or login ID).
     * PII - will be masked in target_context JSON.
     */
    private String username;
    
    /**
     * Email address of the user.
     * PII - will be masked in target_context JSON.
     */
    private String email;
    
    /**
     * First name of the user.
     * PII - will be fully masked (no visible characters) in target_context JSON.
     */
    private String firstName;
    
    /**
     * Last name of the user.
     * PII - will be fully masked (no visible characters) in target_context JSON.
     */
    private String lastName;
    
    /**
     * Account ID associated with the user.
     */
    private String accountId;
    
    /**
     * Account name for readability.
     * PII - will be masked in target_context JSON.
     */
    private String accountName;
    
    /**
     * User status (e.g., ACTIVE, PENDING, DEACTIVATED).
     */
    private String status;
    
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        // These fields are used to populate target_id and target_type in audit_log table
        map.put("targetId", userId);
        map.put("targetType", "USER");
        
        // Context details for JSONB target_context field
        if (username != null) {
            map.put("username", username);  // PII - will be masked
        }
        if (email != null) {
            map.put("email", email);  // PII - will be masked
        }
        if (firstName != null) {
            map.put("firstName", firstName);  // PII - will be masked
        }
        if (lastName != null) {
            map.put("lastName", lastName);  // PII - will be masked
        }
        if (accountId != null) {
            map.put("accountId", accountId);
        }
        if (accountName != null) {
            map.put("accountName", accountName);  // PII - will be masked
        }
        if (status != null) {
            map.put("status", status);
        }
        
        return map;
    }
}
