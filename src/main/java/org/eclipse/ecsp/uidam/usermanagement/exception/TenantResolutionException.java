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

package org.eclipse.ecsp.uidam.usermanagement.exception;

/**
 * Exception thrown when tenant resolution fails in User Management service.
 * This exception is used by TenantResolutionFilter to indicate various tenant-related issues.
 */
public class TenantResolutionException extends RuntimeException {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 5498140581584239115L;
    
    private final String tenantId;
    private final String requestUri;

    /**
     * Constructor for tenant resolution exception.
     *
     * @param message the error message
     * @param tenantId the tenant ID (if available)
     * @param requestUri the request URI where the error occurred
     */
    public TenantResolutionException(String message, String tenantId, String requestUri) {
        super(message);
        this.tenantId = tenantId;
        this.requestUri = requestUri;
    }

    /**
     * Constructor for tenant resolution exception with cause.
     *
     * @param message the error message
     * @param tenantId the tenant ID (if available)
     * @param requestUri the request URI where the error occurred
     * @param cause the underlying cause
     */
    public TenantResolutionException(String message, String tenantId, String requestUri, Throwable cause) {
        super(message, cause);
        this.tenantId = tenantId;
        this.requestUri = requestUri;
    }

    /**
     * Create exception for when no tenant could be found in the request.
     *
     * @param requestUri the request URI
     * @return TenantResolutionException
     */
    public static TenantResolutionException tenantNotFoundInRequest(String requestUri) {
        String message = String.format(
            "No tenant identifier found in request. Please provide tenant via 'tenantId' header, " 
                        + "path prefix (/{tenant}/v1/...), or 'tenant' parameter. Request URI: %s",
                requestUri);
        return new TenantResolutionException(message, null, requestUri);
    }

    /**
     * Create exception for when an invalid/unconfigured tenant is provided.
     *
     * @param tenantId the invalid tenant ID
     * @param requestUri the request URI
     * @return TenantResolutionException
     */
    public static TenantResolutionException invalidTenant(String tenantId, String requestUri) {
        String message = String.format(
            "Tenant '%s' is not configured in the User Management system. " 
                        + "Please check tenant configuration or contact system administrator. Request URI: %s",
                tenantId, requestUri);
        return new TenantResolutionException(message, tenantId, requestUri);
    }

    /**
     * Create exception for tenant configuration errors.
     *
     * @param tenantId the tenant ID
     * @param requestUri the request URI
     * @param cause the underlying cause
     * @return TenantResolutionException
     */
    public static TenantResolutionException configurationError(String tenantId, String requestUri, Throwable cause) {
        String message = String.format(
            "Error accessing configuration for tenant '%s'. Request URI: %s. Error: %s", 
            tenantId, requestUri, cause.getMessage()
        );
        return new TenantResolutionException(message, tenantId, requestUri, cause);
    }

    /**
     * Get the tenant ID associated with this exception.
     *
     * @return the tenant ID, or null if not available
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Get the request URI where the exception occurred.
     *
     * @return the request URI
     */
    public String getRequestUri() {
        return requestUri;
    }

    @Override
    public String toString() {
        return String.format("TenantResolutionException{tenantId='%s', requestUri='%s', message='%s'}", 
                           tenantId, requestUri, getMessage());
    }
}
