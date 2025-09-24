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

package org.eclipse.ecsp.uidam.usermanagement.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.exception.TenantResolutionException;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Filter to resolve and set the tenant context from HTTP request for User Management service. This filter runs early in
 * the Spring Security filter chain and supports header-based tenant resolution only: 1. tenantId header (from API
 * Gateway)
 * Static resources and health endpoints are bypassed and served without tenant resolution. Simplified version for User
 * Management service - header-only tenant resolution.
 */
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Run early, but after basic security filters
public class TenantResolutionFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantResolutionFilter.class);
    
    private final TenantConfigurationService tenantConfigurationService;
    private final ObjectMapper objectMapper;
    
    // Static resource patterns to bypass tenant resolution - User Management specific
    private static final Pattern STATIC_RESOURCE_PATTERN =
        Pattern.compile("^/(actuator|css|js|images|fonts|favicon\\.ico|static|health|metrics|prometheus)(/.*)?$", 
                       Pattern.CASE_INSENSITIVE);

    private static final String TENANT_HEADER = "tenantId";
    private static final String TENANT_SESSION_KEY = "RESOLVED_TENANT_ID";

    @Value("${tenant.multitenant.enabled}")
    private boolean multiTenantEnabled;

    @Value("${tenant.default}")
    private String defaultTenant;

    /**
     * Constructor to inject dependencies.
     *
     * @param tenantConfigurationService the tenant configuration service
     * @param objectMapper object mapper for JSON responses
     */
    public TenantResolutionFilter(TenantConfigurationService tenantConfigurationService, ObjectMapper objectMapper) {
        this.tenantConfigurationService = tenantConfigurationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("Initializing TenantResolutionFilter for User Management");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestUri = httpRequest.getRequestURI();
        
        // Skip tenant resolution for static resources and health endpoints
        if (isStaticResource(requestUri)) {
            LOGGER.debug("Skipping tenant resolution for static resource: {}", requestUri);
            chain.doFilter(request, response);
            return;
        }
        
        String tenantId = null;
        
        LOGGER.debug("Processing tenant resolution for User Management request: {}", requestUri);

        try {
            // Try to resolve tenant from request (header or path)
            tenantId = resolveTenantFromRequest(httpRequest);
            if (StringUtils.hasText(tenantId)) {
                LOGGER.debug("Tenant resolved from request: {}", tenantId);
            } else {
                // Fallback: try to get tenant from session
                tenantId = getTenantFromSession(httpRequest);
                if (StringUtils.hasText(tenantId)) {
                    LOGGER.debug("Tenant resolved from session: {}", tenantId);
                } else if (multiTenantEnabled) {
                    // No tenant could be resolved - throw exception
                    LOGGER.error("No tenant could be resolved for User Management request: {}", requestUri);
                    throw TenantResolutionException.tenantNotFoundInRequest(requestUri);
                }
            }

            // Additional validation: if multitenant is disabled, set tenantId to default
            if (!StringUtils.hasText(tenantId) && !multiTenantEnabled) {
                tenantId = defaultTenant;
                LOGGER.debug("Multitenant disabled, setting tenantId to default: {}", tenantId);
            }

            // Validate that the resolved tenant actually exists in configuration
            if (!isValidConfiguredTenant(tenantId)) {
                LOGGER.error("Tenant '{}' is not configured in User Management system for request: {}",
                    tenantId, requestUri);
                throw TenantResolutionException.invalidTenant(tenantId, requestUri);
            }
            
            // Set tenant context
            TenantContext.setCurrentTenant(tenantId);
            MDC.put(TENANT_HEADER, tenantId);
            LOGGER.debug("Tenant '{}' validated and set in User Management context", tenantId);
            
            // Continue filter chain
            chain.doFilter(request, response);

        } catch (TenantResolutionException ex) {
            // Handle tenant resolution exceptions and return proper error response
            LOGGER.error("User Management tenant resolution failed: {}", ex.getMessage(), ex);
            handleTenantResolutionException(httpResponse, ex);
            
        } finally {
            // Always clear tenant context after request processing
            TenantContext.clear();
            LOGGER.debug("Tenant context cleared for User Management request: {}", requestUri);
            MDC.remove(TENANT_HEADER);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying TenantResolutionFilter for User Management");
    }

    /**
     * Check if the request URI is for a static resource that should bypass tenant resolution.
     *
     * @param requestUri The request URI to check
     * @return true if the URI matches static resource patterns
     */
    private boolean isStaticResource(String requestUri) {
        if (!StringUtils.hasText(requestUri)) {
            return false;
        }
        
        boolean isStatic = STATIC_RESOURCE_PATTERN.matcher(requestUri).matches();
        if (isStatic) {
            LOGGER.debug("Request URI '{}' identified as static resource", requestUri);
        }
        return isStatic;
    }

    /**
     * Get tenant ID from HTTP session.
     */
    private String getTenantFromSession(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            return (String) request.getSession(false).getAttribute(TENANT_SESSION_KEY);
        }
        return null;
    }

    /**
     * Store tenant ID in HTTP session.
     */
    private void storeTenantInSession(HttpServletRequest request, String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            request.getSession(true).setAttribute(TENANT_SESSION_KEY, tenantId);
            LOGGER.debug("Stored tenant '{}' in session for future User Management requests", tenantId);
        }
    }

    /**
     * Resolve tenant from request header or path parameter.
     * Enhanced version for User Management supporting both header and path-based tenant resolution.
     */
    private String resolveTenantFromRequest(HttpServletRequest request) {
        // Strategy 1: Check for tenant in URL path (for email verification links)
        String tenantFromPath = extractTenantFromPath(request.getRequestURI());
        if (StringUtils.hasText(tenantFromPath)) {
            LOGGER.debug("Tenant resolved from URL path: {}", tenantFromPath);
            storeTenantInSession(request, tenantFromPath);
            return tenantFromPath;
        }
        
        // Strategy 2: Check tenantId header (from API Gateway)
        String tenantId = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(tenantId)) {
            LOGGER.debug("Tenant resolved from header: {}", tenantId);
            storeTenantInSession(request, tenantId);
            return tenantId;
        }

        LOGGER.debug("No tenant found in header or path for User Management request");
        return null;
    }

    /**
     * Extract tenant ID from URL path for tenant-first URL patterns.
     * Supports patterns like: /{tenantId}/v1/emailVerification/{token}
     *
     * @param requestUri The request URI to analyze
     * @return The tenant ID if found in path, null otherwise
     */
    private String extractTenantFromPath(String requestUri) {
        if (!StringUtils.hasText(requestUri)) {
            return null;
        }
        
        // Pattern: /{tenantId}/v1/emailVerification/...
        Pattern tenantPattern = Pattern.compile("^/([^/]+)/v1/emailVerification(/.*)?$");
        java.util.regex.Matcher matcher = tenantPattern.matcher(requestUri);
        
        if (matcher.matches()) {
            String tenantId = matcher.group(1);
            LOGGER.debug("Extracted tenant '{}' from URL path: {}", tenantId, requestUri);
            return tenantId;
        }
        
        LOGGER.debug("No tenant pattern matched for URL: {}", requestUri);
        return null;
    }

    /**
     * Check if tenant exists in the configuration.
     */
    private boolean isValidConfiguredTenant(String tenantId) {
        try {
            return tenantConfigurationService.tenantExists(tenantId);
        } catch (Exception e) {
            LOGGER.error("Error checking tenant configuration for '{}': {}", tenantId, e.getMessage());
            return false;
        }
    }

    /**
     * Handle tenant resolution exceptions by sending appropriate error response.
     */
    private void handleTenantResolutionException(HttpServletResponse response, TenantResolutionException ex) 
            throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // Create error response matching User Management API format
        UserManagementErrorResponse errorResponse = new UserManagementErrorResponse(
            "TENANT_RESOLUTION_ERROR",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        
        LOGGER.debug("Sent tenant resolution error response: {}", jsonResponse);
    }

    /**
     * Error response model for tenant resolution failures.
     */
    private static class UserManagementErrorResponse {
        private final String error;
        private final String message;
        private final int status;
        private final long timestamp;

        public UserManagementErrorResponse(String error, String message, int status) {
            this.error = error;
            this.message = message;
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }

    }
}
