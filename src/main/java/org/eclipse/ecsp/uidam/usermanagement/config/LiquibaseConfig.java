package org.eclipse.ecsp.uidam.usermanagement.config;

import liquibase.integration.spring.SpringLiquibase;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.MultiTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Liquibase configuration for the tenants.
 */ 
@Configuration
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true")
public class LiquibaseConfig  {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseConfig.class);

    private static final String TENANT_HEADER = "tenantId";

    private final DataSource dataSource;
    private final MultiTenantProperties multiTenantProperties;
    private final Environment environment;
    
    @Value("#{'${tenant.ids}'.split(',')}")
    private List<String> tenantIds;

    @Value("${tenant.multitenant.enabled}")
    private boolean multiTenantEnabled;

    @Value("${tenant.default}")
    private String defaultTenant;

    @Value("${uidam.liquibase.change-log.path}")
    private String liquibaseChangeLogPath;
    
    @Value("${uidam.default.db.schema}")
    private String defaultUidamSchema;
    
    @Value("${uidam.liquibase.db.credential.global:false}")
    private boolean useGlobalCredentials;
    
    @Value("${postgres.username}")
    private String globalDbUsername;
    
    @Value("${postgres.password}")
    private String globalDbPassword;
    
    @Value("${postgres.driver.class.name}")
    private String driverClassName;
    
    /**
     * Constructor for LiquibaseConfig.
     *
     * @param dataSource            the multi-tenant DataSource
     * @param multiTenantProperties the multi-tenant properties
     * @param environment           the Spring Environment to read fresh property values
     */
    public LiquibaseConfig(DataSource dataSource,
                          MultiTenantProperties multiTenantProperties,
                          Environment environment) {
        this.dataSource = dataSource;
        this.multiTenantProperties = multiTenantProperties;
        this.environment = environment;
        LOGGER.info("LiquibaseConfig initialized with DataSource of type: {}", dataSource.getClass().getName());
    }

    /**
     * Programmatically run Liquibase to run and create table schema and insert default data.
     * It runover all tenants and create schema if not exists.
     *
     * @return SpringLiquibase
     */
    @Bean
    @Primary
    @DependsOn({"multitenancySystemPropertyConfig", "tenantAwareDataSource"})
    @ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true")
    @SuppressWarnings("java:S2077") // SQL injection prevented by strict schema name validation
    // Bean creation will be skipped when spring.liquibase.enabled=false (e.g., in tests)
    public SpringLiquibase createSchemaForTenant() {
        SpringLiquibase liquibase = new SpringLiquibase();

        // If multi-tenant is disabled, run Liquibase for the default tenant only
        if (!multiTenantEnabled) {
            tenantIds = List.of(defaultTenant);
            LOGGER.info("Multi-tenant is disabled. Running Liquibase for the default tenant only: {}", 
                defaultTenant);
        } else {
            LOGGER.info("Multi-tenant is enabled. Running Liquibase for tenants: {}", tenantIds);
        }
        
        LOGGER.info("Liquibase using global credentials: {}", useGlobalCredentials);
        
        for (String tenantId : tenantIds) {
            TenantContext.setCurrentTenant(tenantId);
            MDC.put(TENANT_HEADER, tenantId);
            
            DataSource tenantDataSource = null;
            try {
                // Get tenant-specific datasource based on configuration
                tenantDataSource = getTenantDataSource(tenantId);
                
                liquibase.setDataSource(tenantDataSource);
                liquibase.setChangeLog(liquibaseChangeLogPath);
                liquibase.setContexts(tenantId);
                liquibase.setDefaultSchema(defaultUidamSchema);
                
                // Get tenant-specific Liquibase parameters from tenant properties
                Map<String, String> liquibaseParams = getTenantSpecificLiquibaseParameters(tenantId);
                liquibase.setChangeLogParameters(liquibaseParams);

                // Validate schema name to prevent SQL injection
                if (!defaultUidamSchema.matches("^[a-zA-Z0-9_.-]+$")) {
                    throw new IllegalArgumentException("Invalid schema name: " + defaultUidamSchema 
                        + ". Schema name must contain only letters, numbers, underscores, hyphens, and dots.");
                }

                try (Connection conn = tenantDataSource.getConnection()) {
                    // Create schema using safer approach with identifier validation
                    createSchemaIfNotExists(conn, defaultUidamSchema);

                    // Run Liquibase migration
                    LOGGER.info("Liquibase configuration Start run for tenant {}", tenantId);
                    liquibase.afterPropertiesSet();
                    LOGGER.info("Liquibase configuration Completed run for tenant {}", tenantId);
                } catch (SQLException e) {
                    LOGGER.error("SQL error during Liquibase initialization for tenant: {}. Error: {}", 
                            tenantId, e.getMessage(), e);
                    throw new LiquibaseInitializationException(
                            "SQL error during Liquibase initialization for tenant: " + tenantId, e);
                } catch (Exception e) {
                    LOGGER.error("Liquibase initialization failed for tenant: {}. Error: {}", 
                            tenantId, e.getMessage(), e);
                    throw new LiquibaseInitializationException(
                            "Liquibase initialization failed for tenant: " + tenantId, e);
                }
            } finally {
                // Clean up resources
                if (useGlobalCredentials && tenantDataSource != null) {
                    // For global credentials, we created a simple datasource that can be cleaned up
                    LOGGER.debug("Cleaning up global credential datasource for tenant: {}", tenantId);
                    // DriverManagerDataSource doesn't need explicit cleanup, it will be garbage collected
                }
                MDC.remove(TENANT_HEADER);
                TenantContext.clear();
            }
        }
        return null;
    }

    /**
     * Creates schema if it doesn't exist using safer SQL execution.
     *
     * @param connection the database connection
     * @param schemaName the validated schema name
     * @throws SQLException if schema creation fails
     */
    @SuppressWarnings("java:S2077") // SQL injection prevented by strict schema name validation
    private void createSchemaIfNotExists(Connection connection, String schemaName) throws SQLException {
        // Schema name is already validated with regex
        // Using Statement here is acceptable because:
        // 1. Schema name is strictly validated with regex [a-zA-Z0-9_.-]+
        // 2. Schema names cannot be parameterized in prepared statements for CREATE SCHEMA
        // 3. We're not accepting user input directly - it comes from validated configuration
        String sql = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
        
        try (Statement stmt = connection.createStatement()) {
            LOGGER.debug("Creating schema if not exists: {}", schemaName);
            stmt.execute(sql);
            LOGGER.info("Schema '{}' created or already exists", schemaName);
        }
    }

    /**
     * Gets the appropriate DataSource for the tenant based on configuration.
     * If useGlobalCredentials is true, creates a simple DataSource with global admin credentials
     * and tenant-specific JDBC URL. Otherwise, uses the routing datasource.
     *
     * @param tenantId the tenant identifier
     * @return DataSource for the tenant
     */
    private DataSource getTenantDataSource(String tenantId) {
        if (useGlobalCredentials) {
            LOGGER.info("Creating datasource with global credentials for tenant: {}", tenantId);
            return createGlobalCredentialDataSource(tenantId);
        } else {
            LOGGER.info("Using routing datasource for tenant: {}", tenantId);
            AbstractRoutingDataSource abstractRoutingDataSource = (AbstractRoutingDataSource) dataSource;
            DataSource tenantDs = (DataSource) abstractRoutingDataSource.getResolvedDataSources().get(tenantId);
            if (tenantDs == null) {
                throw new IllegalStateException("No datasource found for tenant: " + tenantId);
            }
            return tenantDs;
        }
    }

    /**
     * Creates a simple DataSource with global admin credentials and tenant-specific JDBC URL.
     * This DataSource uses global credentials from application.properties but connects to 
     * the tenant-specific database.
     *
     * @param tenantId the tenant identifier
     * @return DataSource configured with global credentials and tenant-specific URL
     */
    private DataSource createGlobalCredentialDataSource(String tenantId) {
        // Get tenant-specific JDBC URL from tenant properties or generate it
        String tenantJdbcUrl = getTenantJdbcUrl(tenantId);
        
        LOGGER.info("Creating global credential datasource for tenant {} with URL: {}", 
            tenantId, tenantJdbcUrl);
        
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(tenantJdbcUrl);
        dataSource.setUsername(globalDbUsername);
        dataSource.setPassword(globalDbPassword);
        
        return dataSource;
    }

    /**
     * Gets tenant-specific JDBC URL. First tries to get from tenant properties,
     * then falls back to generating from global postgres.jdbc.url by replacing database name.
     *
     * @param tenantId the tenant identifier
     * @return tenant-specific JDBC URL
     */
    private String getTenantJdbcUrl(String tenantId) {
        // Try to read from system environment with tenant-specific pattern
        String tenantUpperCase = tenantId.toUpperCase().replace("-", "_");
        String envVarName = tenantUpperCase + "_POSTGRES_DATASOURCE";
        String envValue = System.getenv(envVarName);
        
        if (envValue != null && !envValue.isEmpty() && !envValue.equals("ChangeMe")) {
            LOGGER.info("Using tenant-specific JDBC URL from environment variable {} for tenant: {}", 
                envVarName, tenantId);
            return envValue;
        }
        
        // Fall back to extracting URL from routing datasource
        AbstractRoutingDataSource abstractRoutingDataSource = (AbstractRoutingDataSource) dataSource;
        DataSource tenantDs = (DataSource) abstractRoutingDataSource.getResolvedDataSources().get(tenantId);
        
        if (tenantDs != null) {
            // Try to extract JDBC URL from tenant datasource
            try {
                String url = tenantDs.getConnection().getMetaData().getURL();
                LOGGER.info("Extracted JDBC URL from tenant datasource for tenant {}: {}", tenantId, url);
                return url;
            } catch (SQLException e) {
                LOGGER.warn("Could not extract JDBC URL from tenant datasource for tenant: {}. "
                    + "Error: {}", tenantId, e.getMessage());
            }
        }
        
        // Last resort: generate from tenant ID pattern
        LOGGER.warn("Could not determine tenant-specific JDBC URL, using default pattern for tenant: {}", 
            tenantId);
        return "jdbc:postgresql://localhost:5432/" + tenantId;
    }
    
   
    /**
     * Retrieve tenant-specific Liquibase parameters from tenant properties.
     * This method reads directly from the Environment to ensure fresh values
     * when tenants are added at runtime via actuator/refresh.
     *
     * @param tenantId the tenant identifier
     * @return a map of Liquibase parameters for the specified tenant
     */
    public Map<String, String> getTenantSpecificLiquibaseParameters(String tenantId) {
        Map<String, String> liquibaseParams = new HashMap<>();
        liquibaseParams.put("schema", defaultUidamSchema);

        // Do not set tenant.id parameter to maintain null TENANT_ID for users
        // This preserves the original behavior where user records have null TENANT_ID

        // Try to read tenant-specific Liquibase properties directly from Environment
        // This ensures we get fresh values after actuator/refresh
        String propertyPrefix = "tenants.profile." + tenantId + ".liquibase.parameters.";
        
        String clientSecret = getPropertyFromEnvironment(propertyPrefix + "initial-data-client-secret");
        String userSalt = getPropertyFromEnvironment(propertyPrefix + "initial-data-user-salt");
        String userPwd = getPropertyFromEnvironment(propertyPrefix + "initial-data-user-pwd");
        
        if (clientSecret != null || userSalt != null || userPwd != null) {
            // Found properties in environment, use them
            liquibaseParams.put("tenant.id", tenantId);
            
            if (clientSecret != null) {
                liquibaseParams.put("initial.data.client.secret", clientSecret);
                LOGGER.debug("Read initial.data.client.secret from environment for tenant: {}", tenantId);
            }
            if (userSalt != null) {
                liquibaseParams.put("initial.data.user.salt", userSalt);
                LOGGER.debug("Read initial.data.user.salt from environment for tenant: {}", tenantId);
            }
            if (userPwd != null) {
                liquibaseParams.put("initial.data.user.pwd", userPwd);
                LOGGER.debug("Read initial.data.user.pwd from environment for tenant: {}", tenantId);
            }
            
            LOGGER.info("Loaded Liquibase parameters from environment for tenant {}: {} parameters", 
                       tenantId, liquibaseParams.size());
        } else {
            // Fallback to MultiTenantProperties bean (for backward compatibility)
            UserManagementTenantProperties tenant = multiTenantProperties.getTenantProperties(tenantId);
            if (tenant != null && tenant.getLiquibase() != null) {
                UserManagementTenantProperties.LiquibaseProperties liquibaseProps = tenant.getLiquibase();
                liquibaseParams.put("tenant.id", tenantId);

                // Set tenant-specific initial data parameters from nested parameters
                if (liquibaseProps.getParameters() != null) {
                    UserManagementTenantProperties.LiquibaseProperties.ParametersProperties params = liquibaseProps
                            .getParameters();

                    if (params.getInitialDataClientSecret() != null) {
                        liquibaseParams.put("initial.data.client.secret", params.getInitialDataClientSecret());
                    }
                    if (params.getInitialDataUserSalt() != null) {
                        liquibaseParams.put("initial.data.user.salt", params.getInitialDataUserSalt());
                    }
                    if (params.getInitialDataUserPwd() != null) {
                        liquibaseParams.put("initial.data.user.pwd", params.getInitialDataUserPwd());
                    }
                }

                LOGGER.info("Loaded Liquibase parameters from MultiTenantProperties bean for tenant {}", tenantId);
            } else {
                LOGGER.warn("No tenant-specific Liquibase properties found for tenant: {}", tenantId);
            }
        }

        LOGGER.debug("Final Liquibase parameters for tenant {}: {}", tenantId, liquibaseParams);
        return liquibaseParams;
    }
    
    /**
     * Helper method to read property from Spring Environment.
     * Returns null if property is not found or is empty.
     *
     * @param propertyKey the property key to read
     * @return the property value or null
     */
    private String getPropertyFromEnvironment(String propertyKey) {
        try {
            String value = environment.getProperty(propertyKey);
            if (value == null || value.trim().isEmpty() || "ChangeMe".equals(value)) {
                return null;
            }
            return value;
        } catch (Exception e) {
            LOGGER.debug("Error reading property {} from environment: {}", propertyKey, e.getMessage());
            return null;
        }
    }

    /**
     * Initializes database schema for a dynamically added tenant.
     * This method is called when a new tenant is added via configuration refresh.
     * It creates the schema and runs Liquibase migrations for the new tenant.
     *
     * @param tenantId the tenant identifier
     */
    public void initializeTenantSchema(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        
        LOGGER.info("Initializing schema for dynamically added tenant: {}", tenantId);
        
        TenantContext.setCurrentTenant(tenantId);
        MDC.put(TENANT_HEADER, tenantId);
        
        DataSource tenantDataSource = null;
        try {
            // Get tenant-specific datasource
            tenantDataSource = getTenantDataSource(tenantId);
            
            // Create and configure Liquibase
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(tenantDataSource);
            liquibase.setChangeLog(liquibaseChangeLogPath);
            liquibase.setContexts(tenantId);
            liquibase.setDefaultSchema(defaultUidamSchema);
            
            // Get tenant-specific Liquibase parameters
            Map<String, String> liquibaseParams = getTenantSpecificLiquibaseParameters(tenantId);
            liquibase.setChangeLogParameters(liquibaseParams);
            
            // Validate schema name to prevent SQL injection
            if (!defaultUidamSchema.matches("^[a-zA-Z0-9_.-]+$")) {
                throw new IllegalArgumentException("Invalid schema name: " + defaultUidamSchema 
                    + ". Schema name must contain only letters, numbers, underscores, hyphens, and dots.");
            }
            
            try (Connection conn = tenantDataSource.getConnection()) {
                // Create schema if not exists
                createSchemaIfNotExists(conn, defaultUidamSchema);
                
                // Run Liquibase migration
                LOGGER.info("Running Liquibase migrations for dynamically added tenant: {}", tenantId);
                liquibase.afterPropertiesSet();
                LOGGER.info("Successfully initialized schema for tenant: {}", tenantId);
            } catch (SQLException e) {
                LOGGER.error("SQL error during schema initialization for tenant: {}. Error: {}", 
                        tenantId, e.getMessage(), e);
                throw new LiquibaseInitializationException(
                        "SQL error during schema initialization for tenant: " + tenantId, e);
            } catch (Exception e) {
                LOGGER.error("Schema initialization failed for tenant: {}. Error: {}", 
                        tenantId, e.getMessage(), e);
                throw new LiquibaseInitializationException(
                        "Schema initialization failed for tenant: " + tenantId, e);
            }
        } finally {
            // Clean up resources
            if (useGlobalCredentials && tenantDataSource != null) {
                LOGGER.debug("Cleaning up global credential datasource for tenant: {}", tenantId);
            }
            MDC.remove(TENANT_HEADER);
            TenantContext.clear();
        }
    }

    /**
     * Custom exception for Liquibase initialization failures.
     */
    public static class LiquibaseInitializationException extends RuntimeException {
        public LiquibaseInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
