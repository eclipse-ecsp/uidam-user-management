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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet filter that adds security headers to all HTTP responses.
 * 
 * <p>This filter implements:
 * <ul>
 *   <li><b>HSTS (HTTP Strict-Transport-Security)</b>: Forces browsers to only use HTTPS connections,
 *       preventing downgrade attacks and man-in-the-middle attacks.</li>
 *   <li><b>X-Frame-Options</b>: Prevents clickjacking by denying page embedding in iframes.</li>
 *   <li><b>Content-Security-Policy frame-ancestors</b>: Additional clickjacking protection via CSP.</li>
 *   <li><b>X-Content-Type-Options</b>: Prevents MIME-type sniffing attacks.</li>
 * </ul>
 * 
 * <p>HSTS Configuration:
 * <ul>
 *   <li>max-age=63072000 (2 years in seconds)</li>
 *   <li>includeSubDomains - applies to all subdomains</li>
 *   <li>preload - allows inclusion in browser HSTS preload lists</li>
 * </ul>
 */
public class SecurityHeadersFilter implements Filter {

    /**
     * HSTS max-age value: 2 years in seconds (63072000 = 2 * 365 * 24 * 3600).
     */
    private static final String HSTS_MAX_AGE = "63072000";

    /**
     * HSTS header value with max-age, includeSubDomains, and preload directives.
     */
    private static final String HSTS_HEADER_VALUE = "max-age=" + HSTS_MAX_AGE + "; includeSubDomains; preload";

    /**
     * X-Frame-Options header name.
     */
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";

    /**
     * Strict-Transport-Security header name.
     */
    private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

    /**
     * Content-Security-Policy header name.
     */
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    /**
     * X-Content-Type-Options header name.
     */
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization required
    }

    /**
     * Adds security headers to every HTTP response.
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param chain the filter chain
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (response instanceof HttpServletResponse httpResponse) {
            // HSTS: Force HTTPS connections for 2 years, including subdomains
            httpResponse.setHeader(STRICT_TRANSPORT_SECURITY, HSTS_HEADER_VALUE);
            
            // Clickjacking protection: Deny framing of pages
            httpResponse.setHeader(X_FRAME_OPTIONS, "DENY");
            
            // CSP frame-ancestors: Additional clickjacking protection
            httpResponse.setHeader(CONTENT_SECURITY_POLICY, "frame-ancestors 'none'");
            
            // Prevent MIME-type sniffing
            httpResponse.setHeader(X_CONTENT_TYPE_OPTIONS, "nosniff");
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup required
    }
}
