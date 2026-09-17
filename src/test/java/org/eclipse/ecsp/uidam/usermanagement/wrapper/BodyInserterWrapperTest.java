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

package org.eclipse.ecsp.uidam.usermanagement.wrapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BodyInserterWrapper}.
 *
 * <p>Verifies that the wrapper captures the body value, exposes it via
 * {@code getBody()}, and correctly delegates {@code insert()} to the
 * underlying {@link org.springframework.web.reactive.function.BodyInserters#fromValue} inserter.
 */
class BodyInserterWrapperTest {

    private static final Integer TEST_INTEGER = 42;
    private static final String TEST_STRING = "hello";

    // -----------------------------------------------------------------------
    // Constructor / getBody
    // -----------------------------------------------------------------------

    @Test
    void constructor_stringBody_getBodyReturnsValue() {
        BodyInserterWrapper<String> wrapper = new BodyInserterWrapper<>(TEST_STRING);
        assertEquals(TEST_STRING, wrapper.getBody());
    }

    @Test
    void constructor_integerBody_getBodyReturnsValue() {
        BodyInserterWrapper<Integer> wrapper = new BodyInserterWrapper<>(TEST_INTEGER);
        assertEquals(TEST_INTEGER, wrapper.getBody());
    }

    @Test
    void constructor_objectBody_getBodyReturnsValue() {
        Object payload = new Object();
        BodyInserterWrapper<Object> wrapper = new BodyInserterWrapper<>(payload);
        assertEquals(payload, wrapper.getBody());
    }

    // -----------------------------------------------------------------------
    // insert — delegate called and Mono returned
    // -----------------------------------------------------------------------

    @Test
    void insert_delegatesAndReturnsMono() {
        BodyInserterWrapper<String> wrapper = new BodyInserterWrapper<>("test-body");
        ReactiveHttpOutputMessage message = mock(ReactiveHttpOutputMessage.class);
        org.springframework.http.HttpHeaders headers =
                new org.springframework.http.HttpHeaders();
        when(message.getHeaders()).thenReturn(headers);
        when(message.writeWith(any())).thenReturn(Mono.empty());
        BodyInserter.Context context = mock(BodyInserter.Context.class);
        when(context.serverRequest()).thenReturn(java.util.Optional.empty());
        when(context.hints()).thenReturn(java.util.Collections.emptyMap());

        Mono<Void> result = wrapper.insert(message, context);

        // The delegate (BodyInserters.fromValue) always returns a non-null Mono
        assertNotNull(result);
    }
}
