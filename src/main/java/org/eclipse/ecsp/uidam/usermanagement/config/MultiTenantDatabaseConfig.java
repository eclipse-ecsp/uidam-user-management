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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Multi-tenant Database Configuration for User Management service. Creates and configures separate DataSources for each
 * tenant based on tenant-specific properties.
 * This configuration follows the exact pattern from UIDAM Authorization Server but adapted for User Management service
 * requirements.
 * Configuration sources (in priority order): 1. Environment variables: TENANT_TENANTS_{TENANT}_POSTGRES_* 2.
 * Application properties: tenant.tenants.{tenant}.postgres.* 3. Tenant property files:
 * classpath:tenant-{tenant}.properties
 * Each tenant requires: - postgres.jdbc.url: JDBC connection URL - postgres.username: Database username -
 * postgres.password: Database password - postgres.driver.class.name: JDBC driver (defaults to PostgreSQL)
 */
//@Configuration DOTO to be removed after testing
@Profile("!test")
public class MultiTenantDatabaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTenantDatabaseConfig.class);

    // Configuration property keys
    private static final String TENANT_DEFAULT = "${tenant.default}";
    private static final String TENANT_IDS = "${tenant.ids}";
    private static final String TENANT_PROPERTIES_PREFIX = "tenant.tenants.%s.%s";
    private static final String TENANT_PROPERTIES_FILE = "classpath:tenant-%s.properties";
    
    // Database property keys
    private static final String POSTGRES_DRIVER_CLASS_NAME = "postgres.driver.class.name";
    private static final String POSTGRES_USERNAME = "postgres.username";
    private static final String POSTGRES_PASSWORD = "postgres.password";
    private static final String POSTGRES_JDBC_URL = "postgres.jdbc.url";
    
    // Default values
    private static final String DEFAULT_POSTGRES_DRIVER = "org.postgresql.Driver";
    
    // Error messages
    private static final String ERROR_LOADING_TENANT_FILE = "Error loading tenant properties file for %s: %s";
    private static final String MISSING_DB_PROPS 
        = "Missing required database properties for tenant: %s. Required: [url, username, password]";
    private static final String CONFIGURE_TENANT_ERROR = "Problem configuring tenant datasource for %s: %s";
    private static final String NO_TENANT_DATASOURCES 
        = "No tenant datasources could be configured. Check tenant.ids property and tenant configurations.";

    @Value(TENANT_DEFAULT)
    private String defaultTenant;
    
    @Autowired
    private Environment env;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Value("#{'" + TENANT_IDS + "'.split(',')}")
    private List<String> tenantIds;

    // Cache for tenant properties to avoid repeated file I/O
    private final Map<String, Properties> tenantPropertiesCache = new HashMap<>();

    /**
     * Resolve property value from multiple sources in priority order.
     */
    private String resolveProperty(String tenantId, String propertyKey) {
        // Try application properties first
        String fullKey = String.format(TENANT_PROPERTIES_PREFIX, tenantId, propertyKey);
        String value = env.getProperty(fullKey);
        
        if (!StringUtils.hasText(value)) {
            // Try tenant properties file (cached)
            try {
                Properties props = tenantPropertiesCache.get(tenantId);
                if (props == null) {
                    Resource resource = resourceLoader.getResource(String.format(TENANT_PROPERTIES_FILE, tenantId));
                    if (resource.exists()) {
                        props = new Properties();
                        try (InputStream is = resource.getInputStream()) {
                            props.load(is);
                        }
                        tenantPropertiesCache.put(tenantId, props);
                        LOGGER.debug("Loaded properties file for tenant '{}': {}", tenantId, resource.getURI());
                    }
                }
                if (props != null) {
                    value = props.getProperty(fullKey);
                }
            } catch (IOException e) {
                LOGGER.warn(String.format(ERROR_LOADING_TENANT_FILE, tenantId, e.getMessage()));
            }
        }
        
        // Try environment variables (with underscores and uppercase)
        if (!StringUtils.hasText(value)) {
            String envKey = fullKey.replace('.', '_').toUpperCase();
            value = env.getProperty(envKey);
        }
        
        return value;
    }

    /**
     * Create and configure the primary DataSource with multi-tenant routing.
     * This DataSource routes database operations to tenant-specific databases based on TenantContext.
     *
     * @return Multi-tenant routing DataSource
     */
    @Bean("multiTenantDataSource")
    @Primary
    @ConfigurationProperties(prefix = "tenant")
    public DataSource dataSource() {
        LOGGER.info("Configuring multi-tenant DataSource for User Management");
        
        Map<Object, Object> resolvedDataSources = new HashMap<>();
        int configuredTenants = 0;

        for (String tenantId : tenantIds) {
            try {
                tenantId = tenantId.trim(); // Clean up any whitespace
                if (!StringUtils.hasText(tenantId)) {
                    LOGGER.warn("Skipping empty tenant ID in configuration");
                    continue;
                }

                LOGGER.debug("Configuring DataSource for tenant: {}", tenantId);
                
                DataSource tenantDataSource = createTenantDataSource(tenantId);
                resolvedDataSources.put(tenantId, tenantDataSource);
                configuredTenants++;
                
                LOGGER.info("Successfully configured DataSource for tenant '{}' in User Management", tenantId);

            } catch (Exception exp) {
                LOGGER.error(String.format(CONFIGURE_TENANT_ERROR, tenantId, exp.getMessage()), exp);
                throw new IllegalStateException(String.format(CONFIGURE_TENANT_ERROR, tenantId, exp.getMessage()), exp);
            }
        }

        if (resolvedDataSources.isEmpty()) {
            LOGGER.error(NO_TENANT_DATASOURCES);
            throw new IllegalStateException(NO_TENANT_DATASOURCES);
        }

        // Create and configure the routing DataSource
        MultiTenantDataSource multiTenantDataSource = new MultiTenantDataSource();
        multiTenantDataSource.setTargetDataSources(resolvedDataSources);
        
        // Set default DataSource (usually the first tenant or explicitly configured default)
        if (StringUtils.hasText(defaultTenant) && resolvedDataSources.containsKey(defaultTenant)) {
            multiTenantDataSource.setDefaultTargetDataSource(resolvedDataSources.get(defaultTenant));
            LOGGER.info("Set default DataSource to tenant '{}' for User Management", defaultTenant);
        } else if (!resolvedDataSources.isEmpty()) {
            Object firstTenant = resolvedDataSources.keySet().iterator().next();
            multiTenantDataSource.setDefaultTargetDataSource(resolvedDataSources.get(firstTenant));
            LOGGER.info("Set default DataSource to first configured tenant '{}' for User Management", firstTenant);
        }

        // Required for proper initialization
        multiTenantDataSource.afterPropertiesSet();

        LOGGER.info("Multi-tenant DataSource configuration completed for User Management. " 
                + "Configured {} tenant(s): {}", configuredTenants, resolvedDataSources.keySet());

        return multiTenantDataSource;
    }

    /**
     * Create a DataSource for a specific tenant using its configuration properties.
     */
    private DataSource createTenantDataSource(String tenantId) {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

        // Resolve required database properties
        String driverClassName = resolveProperty(tenantId, POSTGRES_DRIVER_CLASS_NAME);
        String username = resolveProperty(tenantId, POSTGRES_USERNAME);
        String password = resolveProperty(tenantId, POSTGRES_PASSWORD);
        String url = resolveProperty(tenantId, POSTGRES_JDBC_URL);

        // Validate required properties
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) 
                || !StringUtils.hasText(url)) {
            String missing = "";
            if (!StringUtils.hasText(username)) {
                missing += "username ";
            }
            if (!StringUtils.hasText(password)) {
                missing += "password ";
            }
            if (!StringUtils.hasText(url)) {
                missing += "url ";
            }

            throw new IllegalArgumentException(
                    String.format(MISSING_DB_PROPS + " Missing: %s", tenantId, missing.trim()));
        }

        // Use default driver if not specified
        if (!StringUtils.hasText(driverClassName)) {
            driverClassName = DEFAULT_POSTGRES_DRIVER;
            LOGGER.debug("Using default PostgreSQL driver for tenant '{}'", tenantId);
        }

        // Configure DataSource
        dataSourceBuilder.driverClassName(driverClassName);
        dataSourceBuilder.username(username);
        dataSourceBuilder.password(password);
        dataSourceBuilder.url(url);

        LOGGER.debug("Created DataSource for tenant '{}': driver={}, url={}, username={}", tenantId, driverClassName,
                url, username);

        return dataSourceBuilder.build();
    }
}
