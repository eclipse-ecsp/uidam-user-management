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
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import javax.net.ssl.SSLException;
import java.time.Duration;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.BUILDER_NAME;

/**
 * Base bean definition class.
 */
public abstract class BaseApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseApplication.class);

    @Autowired
    TenantConfigurationService tenantConfigurationService;

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
