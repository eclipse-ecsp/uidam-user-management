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

package org.eclipse.ecsp.sql.postgress.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.interceptor.CorrelationIdInterceptor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.method.HandlerMethod;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
class CorrelationIdInterceptorTest {

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private HttpServletResponse responseMock;

    @Mock
    private HandlerMethod handlerMethodMock;

    @InjectMocks
    private CorrelationIdInterceptor correlationIdInterceptor;

    @Test
    void testPreHandleHandlerNotInstanceOfHandlerMethod() throws Exception {
        assertTrue(correlationIdInterceptor.preHandle(requestMock, responseMock, new Object()));
    }

    @Test
    void testPreHandleHandler() throws Exception {
        Class c = SomeDeclaringClass.class;
        Method testMethod = c.getMethod("someMethod");
        when(handlerMethodMock.getMethod()).thenReturn(testMethod);
        when(requestMock.getHeader(anyString())).thenReturn("someCorrelationId");
        assertTrue(correlationIdInterceptor.preHandle(requestMock, responseMock, handlerMethodMock));
    }


    @Test
    void testPreHandleHandlerCorrelationIdNotRequired() throws Exception {
        Class c = SomeDeclaringClassRequiredFalse.class;
        Method testMethod = c.getMethod("someMethod");
        when(handlerMethodMock.getMethod()).thenReturn(testMethod);
        when(requestMock.getHeader(anyString())).thenReturn("someCorrelationId");
        assertTrue(correlationIdInterceptor.preHandle(requestMock, responseMock, handlerMethodMock));
    }

    @Test
    void testPreHandleHandlerNoCorrelationIdAnnotation() throws Exception {
        Class c = SomeDeclaringClassNoCorrelationId.class;
        Method testMethod = c.getMethod("someMethod");
        when(handlerMethodMock.getMethod()).thenReturn(testMethod);
        when(requestMock.getHeader(anyString())).thenReturn("someCorrelationId");
        assertTrue(correlationIdInterceptor.preHandle(requestMock, responseMock, handlerMethodMock));
    }

    @Test
    void testAfterCompletion() throws Exception {
        correlationIdInterceptor.afterCompletion(requestMock, responseMock, handlerMethodMock, new Exception());
        assertNull(MDC.get(ApiConstants.CORRELATION_ID));
    }

    @Configuration
    static class SomeDeclaringClass {
        public void someMethod() {
            /*
             * Placeholder to test Correlation intercepter.
             */
        }
    }

    static class SomeDeclaringClassRequiredFalse {
        public void someMethod() {
            /*
             * Placeholder to test Correlation intercepter.
             */
        }
    }

    static class SomeDeclaringClassNoCorrelationId {
        public void someMethod() {
            /*
             * Placeholder to test Correlation intercepter.
             */
        }
    }
}
