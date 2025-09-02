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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.NotificationConstants;
import org.eclipse.ecsp.uidam.usermanagement.interceptor.ClientAddCorrelationIdInterceptor;
import org.eclipse.ecsp.uidam.usermanagement.interceptor.CorrelationIdInterceptor;
import org.eclipse.ecsp.uidam.usermanagement.interceptor.LoggingRequestInterceptor;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.Properties;
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
        
        // Get tenant properties with null safety for test environments
        UserManagementTenantProperties tenantProperties = tenantConfigurationService.getTenantProperties();
        String baseUrl = "http://localhost:8080"; // Default for tests
        
        if (tenantProperties != null && tenantProperties.getAuthServer() != null) {
            baseUrl = tenantProperties.getAuthServer().getHostName();
            LOGGER.debug("Authorization Server service URL: {}", baseUrl);
        } else {
            LOGGER.warn("TenantProperties or AuthServer is null, using default URL: {}", baseUrl);
        }
        
        return WebClient.builder().baseUrl(baseUrl)
            .filter(ClientAddCorrelationIdInterceptor.addCorrelationIdAndContentType())
            .filter(LoggingRequestInterceptor.interceptWebClientHttpRequestAndResponse())
            .clientConnector(connector)
           .build();
    }

    /**
     * Create spring mail {@link JavaMailSender} instance if provider is internal.
     *
     * @param env contains all the enviroment configuration
     *
     * @return instance of {@link JavaMailSender}
     */
    @Bean
    @ConditionalOnProperty(name = NotificationConstants.NOTIFICATION_EMAIL_PROVIDER, havingValue = "internal")
    public JavaMailSender javaMailSender(ConfigurableEnvironment env) {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(env.getProperty(NotificationConstants.NOTIFICATION_EMAIL_PROVIDER_HOST));
        javaMailSender.setPort(env.getProperty(NotificationConstants.NOTIFICATION_EMAIL_PROVIDER_PORT, Integer.class));
        javaMailSender.setUsername(env.getProperty(NotificationConstants.NOTIFICATION_EMAIL_PROVIDER_USERNAME));
        javaMailSender.setPassword(env.getProperty(NotificationConstants.NOTIFICATION_EMAIL_PROVIDER_PASSWORD));
        Properties props = javaMailSender.getJavaMailProperties();
        for (PropertySource<?> propertySource : env.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource source) {
                for (String key : source.getPropertyNames()) {
                    if (key.startsWith(NotificationConstants.NOTIFICATION_EMAIL_PROVIDER_PROPERTIES_PREFIX)) {
                        String updatedKey = StringUtils.removeStart(
                                key, NotificationConstants.NOTIFICATION_EMAIL_PROVIDER_PROPERTIES_PREFIX);
                        String value = env.resolvePlaceholders((String) source.getProperty(key));
                        LOGGER.info("setting mail property with key: {}, value: {}", updatedKey, value);
                        props.put(updatedKey, value);
                    }
                }
            }
        }
        return javaMailSender;
    }

}
