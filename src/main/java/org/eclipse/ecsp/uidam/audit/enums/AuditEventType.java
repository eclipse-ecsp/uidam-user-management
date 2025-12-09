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

package org.eclipse.ecsp.uidam.audit.enums;

/**
 * Audit Event Types - User Management Service events.
 * 
 * <p><strong>Design Note:</strong> This enum is part of the CALLER's code (not the common audit framework).
 * Each service defines its own event types. The audit framework accepts String event types via getType().</p>
 * 
 * <p>This enum contains user management, account management, and self-service events
 * relevant to the UIDAM User Management Service. Authentication/authorization events
 * are maintained separately in the Authorization Server.</p>
 * 
 * <p>Each enum constant contains:</p>
 * <ul>
 *   <li>Event Type - unique identifier stored in database</li>
 *   <li>Description - human-readable explanation of the event</li>
 * </ul>
 *
 * @version 1.0.0
 * @since 1.2.0
 */
public enum AuditEventType {
    
    // ========== User Management Events (Admin) ==========
    
    ADMIN_USER_CREATED("ADMIN_USER_CREATED", "Administrator created a new user"),
    ADMIN_USER_UPDATED("ADMIN_USER_UPDATED", "Administrator updated user details"),
    ADMIN_USER_DELETED("ADMIN_USER_DELETED", "Administrator deleted a user"),
    ADMIN_USER_STATUS_CHANGED("ADMIN_USER_STATUS_CHANGED", "Administrator changed user status"),
    ADMIN_USER_ROLE_CHANGED("ADMIN_USER_ROLE_CHANGED", "Administrator changed user roles"),
    ADMIN_USER_ACCOUNT_ASSOCIATED("ADMIN_USER_ACCOUNT_ASSOCIATED", "Administrator associated user with account"),
    
    // ========== Account Management Events ==========
    
    ADMIN_ACCOUNT_CREATED("ADMIN_ACCOUNT_CREATED", "Administrator created a new account"),
    ADMIN_ACCOUNT_UPDATED("ADMIN_ACCOUNT_UPDATED", "Administrator updated account details"),
    ADMIN_ACCOUNT_DELETED("ADMIN_ACCOUNT_DELETED", "Administrator deleted an account"),
    
    // ========== Self-Service Events ==========
    
    SELF_USER_REGISTERED("SELF_USER_REGISTERED", "User self-registered"),
    SELF_PASSWORD_RESET_REQUESTED("SELF_PASSWORD_RESET_REQUESTED", "User requested password reset"),
    SELF_PASSWORD_RESET_COMPLETED("SELF_PASSWORD_RESET_COMPLETED", "User completed password reset"),
    SELF_USER_UPDATED("SELF_USER_UPDATED", "User updated their own profile");
    
    private final String type;
    private final String description;
    
    /**
     * Constructor.
     *
     * @param type the event type identifier
     * @param description the human-readable description
     */
    AuditEventType(String type, String description) {
        this.type = type;
        this.description = description;
    }
    
    /**
     * Get the event type identifier.
     *
     * @return the event type string
     */
    public String getType() {
        return type;
    }
    
    /**
     * Get the human-readable description.
     *
     * @return the event description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this event is admin-related.
     *
     * @return true if admin event
     */
    public boolean isAdminEvent() {
        return type.startsWith("ADMIN_");
    }
    
    /**
     * Check if this event is self-service-related.
     *
     * @return true if self-service event
     */
    public boolean isSelfServiceEvent() {
        return type.startsWith("SELF_");
    }
    
    /**
     * Check if this event is user management-related.
     *
     * @return true if user management event
     */
    public boolean isUserManagement() {
        return type.contains("_USER_");
    }
    
    /**
     * Check if this event is account management-related.
     *
     * @return true if account management event
     */
    public boolean isAccountManagement() {
        return type.contains("_ACCOUNT_");
    }
    
    /**
     * Find enum by type string.
     *
     * @param type the event type to find
     * @return the matching enum or null if not found
     */
    public static AuditEventType fromType(String type) {
        if (type == null) {
            return null;
        }
        for (AuditEventType eventType : values()) {
            if (eventType.type.equals(type)) {
                return eventType;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return type;
    }
}
