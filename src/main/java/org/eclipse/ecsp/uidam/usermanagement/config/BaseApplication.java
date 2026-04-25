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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.eclipse.ecsp.uidam.usermanagement.interceptor.ClientAddCorrelationIdInterceptor;
import org.eclipse.ecsp.uidam.usermanagement.interceptor.CorrelationIdInterceptor;
import org.eclipse.ecsp.uidam.usermanagement.interceptor.LoggingRequestInterceptor;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.BUILDER_NAME;

/**
 * Base bean definition class.
 */
public abstract class BaseApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseApplication.class);

    @Autowired
    TenantConfigurationService tenantConfigurationService;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${webclient.max.connections:50}")
    private int webClientMaxConnections;
    @Value("${webclient.max.idle.time:20}")
    private int webClientMaxIdleTime;
    @Value("${webclient.max.life.time:60}")
    private int webClientMaxLifeTime;
    @Value("${webclient.pending.acquire.timeout:60}")
    private int webClientPendingAcquireTimeout;
    @Value("${webclient.evict.in.background:120}")
    private int webClientEvictInBackground;
    @Value("${httpclient.connect.timeout.millis:1000}")
    private int httpClientConnectTimeoutMillis;
    @Value("${httpclient.read.timeout:10}")
    private int httpClientReadTimeout;
    @Value("${httpclient.write.timeout:10}")
    private int httpClientWriteTimeout;

    /**
     * Bean for enabling interceptors in WebMvcConfigurer.
     *
     * @return WebMvcConfigurer
     */
    @Bean
    public WebMvcConfigurer adapter() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new CorrelationIdInterceptor());
            }

            @Override
            @SuppressWarnings("deprecation") // MappingJackson2HttpMessageConverter is deprecated in Spring 7
            // but required as a bridge for com.github.fge.jsonpatch which depends on Jackson 2 types
            public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
                // 1. Insert ByteArrayHttpMessageConverter at index 0 with application/json support so that
                //    byte[] + application/json responses (e.g. springdoc /v3/api-docs) are written as raw
                //    JSON bytes and not base64-encoded by Jackson (Spring Framework 7 / Spring Boot 4.x).
                ByteArrayHttpMessageConverter byteArrayConverter = new ByteArrayHttpMessageConverter();
                byteArrayConverter.setSupportedMediaTypes(
                        Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

                // 2. Register a Jackson 2 MappingJackson2HttpMessageConverter for application/json-patch+json.
                //    com.github.fge.jsonpatch.JsonPatch uses com.fasterxml.jackson.databind.JsonNode (Jackson 2)
                //    which is incompatible with the Jackson 3 JacksonJsonHttpMessageConverter in Spring 7.
                //    Also configure WRITE_DATES_AS_TIMESTAMPS=false so Timestamps serialize as ISO-8601 with
                //    timezone offset (e.g. 2024-03-05T11:44:25.965+00:00) instead of the default 'Z' suffix.
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                MappingJackson2HttpMessageConverter jackson2Converter =
                        new MappingJackson2HttpMessageConverter(objectMapper);
                jackson2Converter.setSupportedMediaTypes(List.of(
                        MediaType.APPLICATION_JSON,
                        MediaType.valueOf("application/json-patch+json"),
                        MediaType.valueOf("application/merge-patch+json")));

                builder.configureMessageConvertersList(converters -> {
                    converters.add(0, byteArrayConverter);
                    converters.add(1, jackson2Converter);
                });
            }
        };
    }

    /**
     * ReactorResourceFactory bean definition.
     *
     * @return ReactorResourceFactory
     */
    @Bean
    public ReactorResourceFactory resourceFactory() {
        ReactorResourceFactory factory = new ReactorResourceFactory();
        factory.setUseGlobalResources(false);
        return factory;
    }

    /**
     * WebClient bean definition method.
     *
     * @return WebClient
     */
    @Bean
    @Lazy
    @DependsOn("adapter")
    public WebClient webClient() {
        ConnectionProvider provider =
            ConnectionProvider.builder(BUILDER_NAME)
                .maxConnections(webClientMaxConnections)
                .maxIdleTime(Duration.ofSeconds(webClientMaxIdleTime))
                .maxLifeTime(Duration.ofSeconds(webClientMaxLifeTime))
                .pendingAcquireTimeout(Duration.ofSeconds(webClientPendingAcquireTimeout))
                .evictInBackground(Duration.ofSeconds(webClientEvictInBackground))
                .build();

        ClientHttpConnector connector = null;
        try {
            SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

            HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, httpClientConnectTimeoutMillis)
                .doOnConnected(conn -> conn
                    .addHandlerLast(new ReadTimeoutHandler(httpClientReadTimeout))
                    .addHandlerLast(new WriteTimeoutHandler(httpClientWriteTimeout)))

                .secure(spec -> spec.sslContext(sslContext)
                );
            connector = new ReactorClientHttpConnector(httpClient);
        } catch (SSLException e) {
            LOGGER.error("Error encountered ", e);
        }
        //DOTO need revert previous changes
        // Don't set baseUrl here - it will be set per-request in the services
        // This avoids tenant context access during bean initialization
        return WebClient.builder()
            .filter(ClientAddCorrelationIdInterceptor.addCorrelationIdAndContentType())
            .filter(LoggingRequestInterceptor.interceptWebClientHttpRequestAndResponse())
            .clientConnector(connector)
           .build();
    }

}
