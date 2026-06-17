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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void doFilter_SetsStrictTransportSecurityHeader() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertEquals(
            "max-age=63072000; includeSubDomains; preload",
            response.getHeader("Strict-Transport-Security")
        );
    }

    @Test
    void doFilter_SetsXframeOptionsDeny() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertEquals("DENY", response.getHeader("X-Frame-Options"));
    }

    @Test
    void doFilter_SetsContentSecurityPolicy() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertEquals(
            "frame-ancestors 'none'",
            response.getHeader("Content-Security-Policy")
        );
    }

    @Test
    void doFilter_SetsXcontentTypeOptionsNosniff() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    }

    @Test
    void doFilter_ContinuesFilterChain() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_NonHttpResponse_ContinuesWithoutHeaders() throws Exception {
        ServletRequest servletRequest = mock(ServletRequest.class);
        ServletResponse servletResponse = mock(ServletResponse.class);

        filter.doFilter(servletRequest, servletResponse, filterChain);

        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    void init_DoesNotThrow() {
        assertDoesNotThrow(() -> filter.init(null));
    }

    @Test
    void destroy_DoesNotThrow() {
        assertDoesNotThrow(() -> filter.destroy());
    }
}
