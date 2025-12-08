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
 * Account Target Context Implementation.
 * 
 * <p>Captures details about the account (target) being acted upon in UIDAM.</p>
 * 
 * <p><strong>PII Fields (auto-masked):</strong></p>
 * <ul>
 *   <li>accountName - Account display name</li>
 * </ul>
 *
 * @version 1.0.0
 * @since 1.2.0
 */
@Data
@Builder
public class AccountTargetContext implements TargetContext {
    
    /**
     * Account ID from UIDAM accounts table.
     * This will be stored in both target_id field (as VARCHAR) and in target_context JSON.
     */
    private String accountId;
    
    /**
     * Account name.
     * PII - will be masked in target_context JSON.
     */
    private String accountName;
    
    /**
     * Account status (e.g., ACTIVE, DELETED).
     */
    private String status;
    
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        // These fields are used to populate target_id and target_type in audit_log table
        map.put("targetId", accountId);
        map.put("targetType", "ACCOUNT");
        
        // Context details for JSONB target_context field
        if (accountName != null) {
            map.put("accountName", accountName);  // PII - will be masked
        }
        if (status != null) {
            map.put("status", status);
        }
        
        return map;
    }
}
