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

package org.eclipse.ecsp.uidam.usermanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;

/**
 * Web MVC configuration to fix springdoc-openapi api-docs response encoding under Spring Boot 4.x.
 *
 * <p>In Spring Boot 4.x / Spring Framework 7, when a controller method returns {@code byte[]}
 * with {@code produces = "application/json"} (as springdoc's {@code openapiJson} endpoint does),
 * Jackson's {@code MappingJackson2HttpMessageConverter} is selected over
 * {@code ByteArrayHttpMessageConverter} because the latter no longer matches
 * {@code application/json} by default. Jackson then base64-encodes the raw bytes instead of
 * writing them directly, producing an encoded response instead of plain JSON.
 *
 * <p>This configuration overrides {@code configureMessageConverters} using the Spring Framework 7
 * {@link HttpMessageConverters.ServerBuilder} API, inserting a {@link ByteArrayHttpMessageConverter}
 * that explicitly supports {@code application/json} at the front of the converter list,
 * ensuring the raw bytes are written directly to the response for all
 * {@code byte[]} + {@code application/json} responses.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(final HttpMessageConverters.ServerBuilder builder) {
        ByteArrayHttpMessageConverter byteArrayConverter = new ByteArrayHttpMessageConverter();
        byteArrayConverter.setSupportedMediaTypes(
                Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        builder.configureMessageConvertersList(converters -> converters.add(0, byteArrayConverter));
    }
}
