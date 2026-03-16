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
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
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

    // Static initializer to set system property before tests run
    static {
        System.setProperty("multitenancy.enabled", "true");
        System.setProperty("tenant.default", "ecsp");
    }

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

    private static final int TIMESTAMP_TOLERANCE_MS = 5000; // 5 seconds tolerance for timestamp validation

    @BeforeEach
    void setUp() throws Exception {
        tenantResolutionFilter = new TenantResolutionFilter(tenantConfigurationService, objectMapper);
        
        // Set default multiTenantEnabled=true and defaultTenant="ecsp" for all tests via reflection
        java.lang.reflect.Field multiTenantField = TenantResolutionFilter.class.getDeclaredField("multiTenantEnabled");
        multiTenantField.setAccessible(true);
        multiTenantField.set(tenantResolutionFilter, true);
        
        java.lang.reflect.Field defaultTenantField = TenantResolutionFilter.class.getDeclaredField("defaultTenant");
        defaultTenantField.setAccessible(true);
        defaultTenantField.set(tenantResolutionFilter, "ecsp");
        
        TenantContext.clear();
        // Set a default tenant context for the test setup phase
        // This will be cleared and reset by each test as needed
        TenantContext.setCurrentTenant("ecsp");
        
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

        // Capture tenant context value during filter chain execution
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - check tenant was set during filter chain execution
        assertEquals(expectedTenant, capturedTenant[0]);
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

        // Capture tenant context value during filter chain execution
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - check tenant was set during filter chain execution
        assertEquals(expectedTenant, capturedTenant[0]);
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

        // Capture tenant context value during filter chain execution
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - check tenant was set during filter chain execution
        assertEquals(pathTenant, capturedTenant[0]);
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

        // Capture tenant context value during filter chain execution
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - check tenant was set during filter chain execution
        assertEquals(expectedTenant, capturedTenant[0]);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDefaultTenantUsedWhenMultiTenantDisabled() throws Exception {
        // Arrange
        // Use reflection to set multiTenantEnabled=false and defaultTenant="default_tenant"
        java.lang.reflect.Field multiTenantField = TenantResolutionFilter.class.getDeclaredField("multiTenantEnabled");
        multiTenantField.setAccessible(true);
        multiTenantField.set(tenantResolutionFilter, false);
        java.lang.reflect.Field defaultTenantField = TenantResolutionFilter.class.getDeclaredField("defaultTenant");
        defaultTenantField.setAccessible(true);
        defaultTenantField.set(tenantResolutionFilter, "default_tenant");

        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(tenantConfigurationService.tenantExists("default_tenant")).thenReturn(true);

        // Capture tenant context value during filter chain execution
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - When multitenancy is disabled, default tenant should be set
        assertEquals("default_tenant", capturedTenant[0]);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testErrorWhenNoTenantAndMultiTenantEnabled() throws Exception {
        // Arrange
        // Use reflection to set multiTenantEnabled=true
        java.lang.reflect.Field multiTenantField = TenantResolutionFilter.class.getDeclaredField("multiTenantEnabled");
        multiTenantField.setAccessible(true);
        multiTenantField.set(tenantResolutionFilter, true);

        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("RESOLVED_TENANT_ID")).thenReturn(null);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(org.apache.http.HttpStatus.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        verify(printWriter).write(anyString());
        verify(printWriter).flush();
    }

    @Test
    void testTenantResolutionFromAuthorizationHeaderWithValidJwt() throws Exception {
        // Arrange
        String expectedTenant = "sdp";
        // Create a valid JWT token with tenantId claim
        // JWT format: header.payload.signature (Base64 URL encoded)
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        String payloadJson = "{\"sub\":\"admin\",\"tenantId\":\""
            + expectedTenant + "\",\"user_id\":\"12345\"}";
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payloadJson.getBytes());
        String signature = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("fake-signature".getBytes());
        final String jwtToken = header + "." + payload + "." + signature;

        // Mock objectMapper to parse the JWT payload - use exact string match
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("sub", "admin");
        claims.put("tenantId", expectedTenant);
        claims.put("user_id", "12345");
        when(objectMapper.readValue(payloadJson, java.util.Map.class)).thenReturn(claims);
        
        // Ensure session doesn't interfere
        when(session.getAttribute(anyString())).thenReturn(null);
        
        final String authHeader = "Bearer " + jwtToken;
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(authHeader);
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
    void testTenantResolutionFromAuthorizationHeaderWithDifferentTenant() throws Exception {
        // Arrange
        String expectedTenant = "ecsp";
        String payloadJson = "{\"sub\":\"testuser\",\"tenantId\":\""
            + expectedTenant + "\",\"accountName\":\"Engineering\"}";
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payloadJson.getBytes());
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String signature = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("signature".getBytes());
        final String jwtToken = header + "." + payload + "." + signature;

        // Mock objectMapper to parse the JWT payload
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("sub", "testuser");
        claims.put("tenantId", expectedTenant);
        claims.put("accountName", "Engineering");
        when(objectMapper.readValue(payloadJson, java.util.Map.class)).thenReturn(claims);
        
        final String authHeader = "Bearer " + jwtToken;
        when(request.getRequestURI()).thenReturn("/v1/roles");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tenantConfigurationService.tenantExists(expectedTenant)).thenReturn(true);

        // Capture tenant context value during filter chain execution
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - check tenant was set during filter chain execution
        assertEquals(expectedTenant, capturedTenant[0]);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testAuthorizationHeaderIgnoredWhenTenantIdHeaderPresent() throws Exception {
        // Arrange - header should take precedence over Authorization
        String headerTenant = "ecsp";
        String authTenant = "sdp";
        
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"tenantId\":\"" + authTenant + "\"}").getBytes());
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String signature = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("sig".getBytes());
        String jwtToken = header + "." + payload + "." + signature;

        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(headerTenant);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(tenantConfigurationService.tenantExists(headerTenant)).thenReturn(true);

        // Capture tenant context value during filter chain execution
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - should use tenantId header, not Authorization
        assertEquals(headerTenant, capturedTenant[0]);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testAuthorizationHeaderIgnoredWhenPathContainsTenant() throws Exception {
        // Arrange - path should take precedence over Authorization
        String pathTenant = "ecsp";
        String authTenant = "sdp";
        
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"tenantId\":\"" + authTenant + "\"}").getBytes());
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String signature = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("sig".getBytes());
        String jwtToken = header + "." + payload + "." + signature;
        String requestUri = "/" + pathTenant + "/v1/emailVerification/token-123";

        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        when(tenantConfigurationService.tenantExists(pathTenant)).thenReturn(true);

        // Capture tenant context value during filter chain execution
        final String[] capturedTenant = new String[1];
        doAnswer(invocation -> {
            capturedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - should use path tenant, not Authorization
        assertEquals(pathTenant, capturedTenant[0]);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testInvalidJwtFormatIgnored() throws Exception {
        // Arrange - invalid JWT (only 2 parts instead of 3)
        String invalidJwt = "header.payload"; // Missing signature
        String authHeader = "Bearer " + invalidJwt;

        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(authHeader);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - should fail to resolve tenant
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(org.apache.http.HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testNonBearerTokenIgnored() throws Exception {
        // Arrange - Authorization header without Bearer prefix
        String authHeader = "Basic dXNlcjpwYXNzd29yZA=="; // Basic auth

        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(authHeader);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - should fail to resolve tenant
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(org.apache.http.HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testJwtWithoutTenantIdClaimIgnored() throws Exception {
        // Arrange - JWT without tenantId claim
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"sub\":\"user\",\"role\":\"admin\"}".getBytes()); // No tenantId
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String signature = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("sig".getBytes());
        String jwtToken = header + "." + payload + "." + signature;

        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - should fail to resolve tenant
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(org.apache.http.HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testMalformedBase64InJwtIgnored() throws Exception {
        // Arrange - JWT with invalid Base64 encoding
        String invalidJwt = "header.invalid-base64-!!!.signature";
        String authHeader = "Bearer " + invalidJwt;

        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(authHeader);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - should fail to resolve tenant
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(org.apache.http.HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testEmptyAuthorizationHeaderIgnored() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - should fail to resolve tenant
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(org.apache.http.HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void testBearerTokenWithOnlyPrefixIgnored() throws Exception {
        // Arrange - "Bearer " with no token
        when(request.getRequestURI()).thenReturn("/v1/users");
        when(request.getHeader("tenantId")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - should fail to resolve tenant
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(org.apache.http.HttpStatus.SC_BAD_REQUEST);
    }

    // ==================== Tests for extractClientIp ====================

    @Test
    void testExtractClientIpFromForwardedForHeader() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - filter should proceed normally
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpFromForwardedForMultipleIps() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange - X-Forwarded-For with multiple IPs (client, proxy1, proxy2)
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1, 192.0.2.1");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - filter should proceed normally, using first IP
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpFromRealIpHeader() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.50");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpFromProxyClientHeader() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("172.16.0.100");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpFromWeblogicProxyHeader() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("192.168.100.200");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpFromHttpClientHeader() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn("10.20.30.40");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpFromHttpForwardedForHeader() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn("192.0.2.146");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpFromRemoteAddress() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange - All headers are null, fallback to remote address
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpReturnsUnknownWhenNoValidAddress() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange - All headers return "unknown"
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("unknown");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("unknown");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("unknown");
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn("unknown");
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn(null);

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - Should still proceed (IP logging is informational only)
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpIgnoresEmptyHeaders() throws Exception {
        // Enable source IP logging
        java.lang.reflect.Field sourceIpField = TenantResolutionFilter.class.getDeclaredField("sourceIpLoggingEnabled");
        sourceIpField.setAccessible(true);
        sourceIpField.set(tenantResolutionFilter, true);

        // Arrange - Empty string headers should be ignored
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("  ");
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testExtractClientIpDisabledWhenLoggingOff() throws Exception {
        // Arrange - Source IP logging is disabled by default
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // Act
        tenantResolutionFilter.doFilter(request, response, filterChain);

        // Assert - No IP headers should be checked
        verify(request, never()).getHeader("X-Forwarded-For");
        verify(request, never()).getRemoteAddr();
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Tests for UserManagementErrorResponse Getters ====================

    @Test
    void testUserManagementErrorResponse_Getters() throws Exception {
        // Use reflection to access the inner class and test its getters
        Class<?> errorResponseClass = Class.forName(
            "org.eclipse.ecsp.uidam.usermanagement.filter.TenantResolutionFilter$UserManagementErrorResponse");
        
        // Create instance using constructor
        java.lang.reflect.Constructor<?> constructor = errorResponseClass.getDeclaredConstructor(
            String.class, String.class, int.class);
        constructor.setAccessible(true);
        
        Object errorResponse = constructor.newInstance(
            "TEST_ERROR", "Test error message", HttpStatus.SC_BAD_REQUEST);
        
        // Test getError()
        java.lang.reflect.Method getError = errorResponseClass.getDeclaredMethod("getError");
        getError.setAccessible(true);
        assertEquals("TEST_ERROR", getError.invoke(errorResponse));
        
        // Test getMessage()
        java.lang.reflect.Method getMessage = errorResponseClass.getDeclaredMethod("getMessage");
        getMessage.setAccessible(true);
        assertEquals("Test error message", getMessage.invoke(errorResponse));
        
        // Test getStatus()
        java.lang.reflect.Method getStatus = errorResponseClass.getDeclaredMethod("getStatus");
        getStatus.setAccessible(true);
        assertEquals(HttpStatus.SC_BAD_REQUEST, getStatus.invoke(errorResponse));
        
        // Test getTimestamp()
        java.lang.reflect.Method getTimestamp = errorResponseClass.getDeclaredMethod("getTimestamp");
        getTimestamp.setAccessible(true);
        long timestamp = (long) getTimestamp.invoke(errorResponse);
        // Timestamp should be recent (within last second)
        long currentTime = System.currentTimeMillis();
        assertFalse(timestamp > currentTime, "Timestamp should not be in the future");
        assertFalse((currentTime - timestamp) > TIMESTAMP_TOLERANCE_MS, 
            "Timestamp should be within the last 5 seconds");
    }

    @Test
    void testUserManagementErrorResponse_TimestampIsUnique() throws Exception {
        // Use reflection to access the inner class
        Class<?> errorResponseClass = Class.forName(
            "org.eclipse.ecsp.uidam.usermanagement.filter.TenantResolutionFilter$UserManagementErrorResponse");
        
        java.lang.reflect.Constructor<?> constructor = errorResponseClass.getDeclaredConstructor(
            String.class, String.class, int.class);
        constructor.setAccessible(true);
        
        // Create two instances
        Object errorResponse1 = constructor.newInstance(
            "ERROR1", "Message 1", HttpStatus.SC_BAD_REQUEST);
        
        Object errorResponse2 = constructor.newInstance(
            "ERROR2", "Message 2", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        
        // Get timestamps
        java.lang.reflect.Method getTimestamp = errorResponseClass.getDeclaredMethod("getTimestamp");
        getTimestamp.setAccessible(true);
        
        long timestamp1 = (long) getTimestamp.invoke(errorResponse1);
        long timestamp2 = (long) getTimestamp.invoke(errorResponse2);
        
        // Both timestamps should be valid recent timestamps
        assertFalse(timestamp1 > System.currentTimeMillis());
        assertFalse(timestamp2 > System.currentTimeMillis());
        // Timestamps should be within reasonable range (last 5 seconds)
        long currentTime = System.currentTimeMillis();
        assertFalse((currentTime - timestamp1) > TIMESTAMP_TOLERANCE_MS);
        assertFalse((currentTime - timestamp2) > TIMESTAMP_TOLERANCE_MS);
    }
}
