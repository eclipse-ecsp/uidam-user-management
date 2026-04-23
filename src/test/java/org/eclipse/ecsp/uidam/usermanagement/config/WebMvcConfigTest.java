/*
 *
 *   ******************************************************************************
 *
 *    Copyright (c) 2023-24 Harman International
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *
 *    you may not use this file except in compliance with the License.
 *
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *    See the License for the specific language governing permissions and
 *
 *    limitations under the License.
 *
 *    SPDX-License-Identifier: Apache-2.0
 *
 *    *******************************************************************************
 *
 */

package org.eclipse.ecsp.uidam.usermanagement.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.StringHttpMessageConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebMvcConfig}.
 *
 * <p>Verifies that {@code configureMessageConverters} inserts a {@link ByteArrayHttpMessageConverter}
 * supporting {@code application/json} at position 0 of the converter list, which is required to
 * prevent Jackson from base64-encoding the raw bytes returned by springdoc's api-docs endpoint
 * under Spring Boot 4.x / Spring Framework 7.
 */
@ExtendWith(MockitoExtension.class)
class WebMvcConfigTest {

    private static final int EXPECTED_CONVERTER_COUNT = 2;

    private WebMvcConfig webMvcConfig;

    @BeforeEach
    void setUp() {
        webMvcConfig = new WebMvcConfig();
    }

    /**
     * Creates a mock ServerBuilder that captures and immediately applies the configureMessageConvertersList consumer.
     */
    @SuppressWarnings("unchecked")
    private HttpMessageConverters.ServerBuilder mockBuilder(List<HttpMessageConverter<?>> convertersList) {
        HttpMessageConverters.ServerBuilder builder = mock(HttpMessageConverters.ServerBuilder.class);
        when(builder.configureMessageConvertersList(any())).thenAnswer(invocation -> {
            Consumer<List<HttpMessageConverter<?>>> consumer = invocation.getArgument(0);
            consumer.accept(convertersList);
            return builder;
        });
        return builder;
    }

    @Test
    void configureMessageConverters_insertsAtPositionZero() {
        List<HttpMessageConverter<?>> convertersList = new ArrayList<>();
        convertersList.add(new StringHttpMessageConverter());
        HttpMessageConverters.ServerBuilder builder = mockBuilder(convertersList);

        webMvcConfig.configureMessageConverters(builder);

        assertInstanceOf(ByteArrayHttpMessageConverter.class, convertersList.get(0),
                "First converter must be ByteArrayHttpMessageConverter");
    }

    @Test
    void configureMessageConverters_byteArrayConverterSupportsApplicationJson() {
        List<HttpMessageConverter<?>> convertersList = new ArrayList<>();
        HttpMessageConverters.ServerBuilder builder = mockBuilder(convertersList);

        webMvcConfig.configureMessageConverters(builder);

        ByteArrayHttpMessageConverter converter = (ByteArrayHttpMessageConverter) convertersList.get(0);
        assertTrue(converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON),
                "ByteArrayHttpMessageConverter must support application/json");
    }

    @Test
    void configureMessageConverters_byteArrayConverterSupportsApplicationOctetStream() {
        List<HttpMessageConverter<?>> convertersList = new ArrayList<>();
        HttpMessageConverters.ServerBuilder builder = mockBuilder(convertersList);

        webMvcConfig.configureMessageConverters(builder);

        ByteArrayHttpMessageConverter converter = (ByteArrayHttpMessageConverter) convertersList.get(0);
        assertTrue(converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_OCTET_STREAM),
                "ByteArrayHttpMessageConverter must support application/octet-stream");
    }

    @Test
    void configureMessageConverters_byteArrayConverterSupportsAllMediaTypes() {
        List<HttpMessageConverter<?>> convertersList = new ArrayList<>();
        HttpMessageConverters.ServerBuilder builder = mockBuilder(convertersList);

        webMvcConfig.configureMessageConverters(builder);

        ByteArrayHttpMessageConverter converter = (ByteArrayHttpMessageConverter) convertersList.get(0);
        assertTrue(converter.getSupportedMediaTypes().contains(MediaType.ALL),
                "ByteArrayHttpMessageConverter must support */*");
    }

    @Test
    void configureMessageConverters_existingConvertersArePreserved() {
        List<HttpMessageConverter<?>> convertersList = new ArrayList<>();
        StringHttpMessageConverter existing = new StringHttpMessageConverter();
        convertersList.add(existing);
        HttpMessageConverters.ServerBuilder builder = mockBuilder(convertersList);

        webMvcConfig.configureMessageConverters(builder);

        assertEquals(EXPECTED_CONVERTER_COUNT, convertersList.size(), "Existing converter must be preserved");
        assertInstanceOf(StringHttpMessageConverter.class, convertersList.get(1),
                "Original converter must remain at index 1");
    }

    @Test
    void configureMessageConverters_worksOnEmptyList() {
        List<HttpMessageConverter<?>> convertersList = new ArrayList<>();
        HttpMessageConverters.ServerBuilder builder = mockBuilder(convertersList);

        webMvcConfig.configureMessageConverters(builder);

        assertEquals(1, convertersList.size());
        assertInstanceOf(ByteArrayHttpMessageConverter.class, convertersList.get(0));
    }

    @Test
    void configureMessageConverters_byteArrayConverterCanWriteApplicationJson() {
        List<HttpMessageConverter<?>> convertersList = new ArrayList<>();
        HttpMessageConverters.ServerBuilder builder = mockBuilder(convertersList);

        webMvcConfig.configureMessageConverters(builder);

        ByteArrayHttpMessageConverter converter = (ByteArrayHttpMessageConverter) convertersList.get(0);
        assertTrue(converter.canWrite(byte[].class, MediaType.APPLICATION_JSON),
                "ByteArrayHttpMessageConverter must be able to write byte[] as application/json");
    }

    @Test
    void configureMessageConverters_delegatesToBuilder() {
        List<HttpMessageConverter<?>> convertersList = new ArrayList<>();
        HttpMessageConverters.ServerBuilder builder = mockBuilder(convertersList);

        webMvcConfig.configureMessageConverters(builder);

        verify(builder).configureMessageConvertersList(any());
    }
}
