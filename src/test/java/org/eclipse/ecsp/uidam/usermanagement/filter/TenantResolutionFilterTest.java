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
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.http.HttpStatus;
import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.ActiveProfiles;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ActiveProfiles("test")
class TenantResolutionFilterTest {

    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpSession session;

    @Mock  
    private PrintWriter printWriter;

    private TenantResolutionFilter tenantResolutionFilter;

    @BeforeEach
    void setUp() throws Exception {
        tenantResolutionFilter = new TenantResolutionFilter(tenantConfigurationService, objectMapper);
        TenantContext.clear();
        // Mock session for all tests - handle both getSession(false) and getSession(true)
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        // Mock response writer for error handling
        when(response.getWriter()).thenReturn(printWriter);
        // Mock objectMapper to return a JSON string for error responses
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"test\"}");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        // Clear any session interactions for next test
        org.mockito.Mockito.clearInvocations(request, response, session, filterChain);
    }

    @Test
    void testTenantResolutionFromHeader() throws Exception {
        // Arrange
        String expectedTenant = "ecsp";
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(expectedTenant);
        when(tenantConfigurationService.tenantExists(expectedTenant)).thenReturn(true);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(expectedTenant, TenantContext.getCurrentTenant());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testTenantResolutionFromPath() throws Exception {
        // Arrange
        String expectedTenant = "ecsp";
        String requestUri = "/" + expectedTenant + "/v1/emailVerification/12345-67890";
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getHeader("tenantId")).thenReturn(null);
        when(tenantConfigurationService.tenantExists(expectedTenant)).thenReturn(true);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(expectedTenant, TenantContext.getCurrentTenant());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testTenantResolutionFromPathDifferentTenant() throws Exception {
        // Arrange
        String expectedTenant = "sdp";
        String requestUri = "/" + expectedTenant + "/v1/emailVerification/token-123";
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getHeader("tenantId")).thenReturn(null);
        when(tenantConfigurationService.tenantExists(expectedTenant)).thenReturn(true);

        // Create a holder to capture tenant context from inside the filter chain
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(expectedTenant, capturedTenant[0]);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testTenantResolutionPathTakesPrecedenceOverHeader() throws Exception {
        // Arrange
        String pathTenant = "ecsp";
        String headerTenant = "sdp";
        String requestUri = "/" + pathTenant + "/v1/emailVerification/abc-def";
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getHeader("tenantId")).thenReturn(headerTenant);
        when(tenantConfigurationService.tenantExists(pathTenant)).thenReturn(true);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(pathTenant, TenantContext.getCurrentTenant());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testNoTenantResolutionForNonEmailVerificationPath() throws Exception {
        // Arrange
        String requestUri = "/some/other/path";
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getHeader("tenantId")).thenReturn(null);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - filter should handle the error internally and NOT call the filter chain
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpStatus.SC_BAD_REQUEST); // Bad Request
        verify(response).setContentType("application/json");
        verify(printWriter).write(anyString()); // Error response written
        verify(printWriter).flush();
    }

    @Test
    void testStaticResourceBypass() throws Exception {
        // Arrange
        String requestUri = "/actuator/health";
        when(request.getRequestURI()).thenReturn(requestUri);
        
        // Clear any previous tenant context
        TenantContext.clear();
        
        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - verify that no tenant was set during static resource processing
        assertFalse(TenantContext.hasTenant(), "No tenant should be set for static resources");
        verify(filterChain).doFilter(request, response);
        
        // Verify that no tenant configuration service was called (static resources bypass everything)
        verify(tenantConfigurationService, org.mockito.Mockito.never()).tenantExists(org.mockito.Mockito.anyString());
    }

    @Test
    void testTenantResolutionFromPathWithSubpaths() throws Exception {
        // Arrange
        String expectedTenant = "ecsp";
        String requestUri = "/" + expectedTenant + "/v1/emailVerification/users/12345/resend";
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getHeader("tenantId")).thenReturn(null);
        when(tenantConfigurationService.tenantExists(expectedTenant)).thenReturn(true);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        assertEquals(expectedTenant, TenantContext.getCurrentTenant());
        verify(filterChain).doFilter(request, response);
    }
}
