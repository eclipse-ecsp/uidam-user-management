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
import java.util.regex.Pattern;

/**
 * Liquibase configuration for the tenants.
 */
@Configuration
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true")
public class LiquibaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseConfig.class);
    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    private static final String TOKEN_RUN_INTERVAL_DAYS = "token.run_interval_days";
    private static final String TOKEN_RETENTION_DAYS = "token.retention_days";
    private static final String TOKEN_ACTION = "token.action";
    private static final String TOKEN_S3_BUCKET_NAME = "token.s3_bucket_name";
    private static final String TOKEN_S3_REGION = "token.s3_region";
    private static final String TOKEN_DISK_MOUNT_PATH = "token.disk_mount_path";

    private static final String AUDIT_RUN_INTERVAL_DAYS = "audit.run_interval_days";
    private static final String AUDIT_RETENTION_DAYS = "audit.retention_days";
    private static final String AUDIT_ACTION = "audit.action";
    private static final String AUDIT_S3_BUCKET_NAME = "audit.s3_bucket_name";
    private static final String AUDIT_S3_REGION = "audit.s3_region";
    private static final String AUDIT_DISK_MOUNT_PATH = "audit.disk_mount_path";

    private static final String SOFT_DELETE_DATA_RUN_INTERVAL_DAYS = "soft_delete_data.run_interval_days";
    private static final String SOFT_DELETE_DATA_RETENTION_DAYS = "soft_delete_data.retention_days";
    private static final String SOFT_DELETE_DATA_ACTION = "soft_delete_data.action";
    private static final String SOFT_DELETE_DATA_S3_BUCKET_NAME = "soft_delete_data.s3_bucket_name";
    private static final String SOFT_DELETE_DATA_S3_REGION = "soft_delete_data.s3_region";
    private static final String SOFT_DELETE_DATA_DISK_MOUNT_PATH = "soft_delete_data.disk_mount_path";

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

    @Value("${uidam.default.db.schema:}")
    private String defaultUidamSchemaProperty;

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
    @DependsOn({ "multitenancySystemPropertyConfig", "tenantAwareDataSource" })
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

                // Get schema name for this tenant
                String defaultUidamSchema = getSchemaNameForTenant(tenantId);

                liquibase.setDataSource(tenantDataSource);
                liquibase.setChangeLog(liquibaseChangeLogPath);
                liquibase.setContexts(tenantId);
                liquibase.setDefaultSchema(defaultUidamSchema);

                // Get tenant-specific Liquibase parameters from tenant properties
                Map<String, String> liquibaseParams = getTenantSpecificLiquibaseParameters(tenantId);
                liquibase.setChangeLogParameters(liquibaseParams);

                // Validate schema name to prevent SQL injection
                validateSchemaName(defaultUidamSchema);


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
            DataSource tenantDs = abstractRoutingDataSource.getResolvedDataSources().get(tenantId);
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

        DriverManagerDataSource globalCredentialDataSource = new DriverManagerDataSource();
        globalCredentialDataSource.setDriverClassName(driverClassName);
        globalCredentialDataSource.setUrl(tenantJdbcUrl);
        globalCredentialDataSource.setUsername(globalDbUsername);
        globalCredentialDataSource.setPassword(globalDbPassword);

        return globalCredentialDataSource;
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
        DataSource tenantDs = abstractRoutingDataSource.getResolvedDataSources().get(tenantId);

        if (tenantDs != null) {
            // Try to extract JDBC URL from tenant datasource
            try (Connection connection = tenantDs.getConnection()) {
                String url = connection.getMetaData().getURL();
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
     * Gets the schema name for a tenant. 
     * First checks the uidam.default.db.schema property.
     * If empty or null, uses the tenant ID (lowercase) as the schema name.
     *
     * @param tenantId the tenant identifier
     * @return the schema name to use for this tenant
     */
    private String getSchemaNameForTenant(String tenantId) {
        if (defaultUidamSchemaProperty == null || defaultUidamSchemaProperty.trim().isEmpty()) {
            // Use tenant ID (lowercase) as schema name
            String schemaName = tenantId.toLowerCase();
            LOGGER.info("Property 'uidam.default.db.schema' is empty or null. "
                    + "Using tenant ID (lowercase) as schema: {}", schemaName);
            return schemaName;
        }

        LOGGER.info("Using schema from property 'uidam.default.db.schema': {}",
                defaultUidamSchemaProperty);
        return defaultUidamSchemaProperty;
    }

    private void validateSchemaName(String schemaName) {
        if (!SCHEMA_NAME_PATTERN.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName
                    + ". Schema name must contain only letters, numbers, underscores, hyphens, and dots.");
        }
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
        // Get schema name for this tenant
        String schemaName = getSchemaNameForTenant(tenantId);
        liquibaseParams.put("schema", schemaName);

        addInitialDataLiquibaseParameters(tenantId, liquibaseParams);

        // Data Archival & Retention Framework parameters (token / audit / soft-delete-data jobs)
        addArchivalLiquibaseParameters(tenantId, liquibaseParams);

        LOGGER.debug("Final Liquibase parameters for tenant {}: {}", tenantId, liquibaseParams);
        return liquibaseParams;
    }

    /**
     * Adds the initial-data Liquibase parameters (client secret, user salt/password), preferring
     * tenant-specific environment values, then falling back to the MultiTenantProperties bean.
     *
     * @param tenantId the tenant identifier
     * @param liquibaseParams the Liquibase parameter map to update
     */
    private void addInitialDataLiquibaseParameters(String tenantId, Map<String, String> liquibaseParams) {
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
            putAndLog(liquibaseParams, tenantId, "initial.data.client.secret", clientSecret);
            putAndLog(liquibaseParams, tenantId, "initial.data.user.salt", userSalt);
            putAndLog(liquibaseParams, tenantId, "initial.data.user.pwd", userPwd);
            LOGGER.info("Loaded Liquibase parameters from environment for tenant {}: {} parameters",
                    tenantId, liquibaseParams.size());
            return;
        }

        // Fallback to MultiTenantProperties bean (for backward compatibility)
        UserManagementTenantProperties tenant = multiTenantProperties.getTenantProperties(tenantId);
        if (tenant == null || tenant.getLiquibase() == null || tenant.getLiquibase().getParameters() == null) {
            LOGGER.warn("No tenant-specific Liquibase properties found for tenant: {}", tenantId);
            return;
        }

        UserManagementTenantProperties.LiquibaseProperties.ParametersProperties params = tenant.getLiquibase()
                .getParameters();
        liquibaseParams.put("tenant.id", tenantId);
        putAndLog(liquibaseParams, tenantId, "initial.data.client.secret", params.getInitialDataClientSecret());
        putAndLog(liquibaseParams, tenantId, "initial.data.user.salt", params.getInitialDataUserSalt());
        putAndLog(liquibaseParams, tenantId, "initial.data.user.pwd", params.getInitialDataUserPwd());
        LOGGER.info("Loaded Liquibase parameters from MultiTenantProperties bean for tenant {}", tenantId);
    }

    /**
     * Adds archival Liquibase parameters, preferring tenant-specific environment values, then the
     * default tenant template (needed because TenantDefaultPropertiesProcessor never generates
     * tenants.profile.default.* for the literal default tenant), then the MultiTenantProperties bean.
     *
     * @param tenantId the tenant identifier
     * @param liquibaseParams the Liquibase parameter map to update
     */
    private void addArchivalLiquibaseParameters(String tenantId, Map<String, String> liquibaseParams) {
        if (addArchivalLiquibaseParametersFromEnvironment(tenantId, liquibaseParams)) {
            return;
        }

        addArchivalLiquibaseParametersFromProperties(tenantId, liquibaseParams);
    }

    private boolean addArchivalLiquibaseParametersFromEnvironment(String tenantId,
            Map<String, String> liquibaseParams) {
        String tokenAction = getLiquibaseParameter(tenantId, "token-action");
        String tokenRunIntervalDays = getLiquibaseParameter(tenantId, "token-run-interval-days");
        String tokenRetentionDays = getLiquibaseParameter(tenantId, "token-retention-days");
        String tokenS3BucketName = getLiquibaseParameter(tenantId, "token-s3-bucket-name");
        String tokenS3Region = getLiquibaseParameter(tenantId, "token-s3-region");
        String tokenDiskMountPath = getLiquibaseParameter(tenantId, "token-disk-mount-path");

        String auditAction = getLiquibaseParameter(tenantId, "audit-action");
        String auditRunIntervalDays = getLiquibaseParameter(tenantId, "audit-run-interval-days");
        String auditRetentionDays = getLiquibaseParameter(tenantId, "audit-retention-days");
        String auditS3BucketName = getLiquibaseParameter(tenantId, "audit-s3-bucket-name");
        String auditS3Region = getLiquibaseParameter(tenantId, "audit-s3-region");
        String auditDiskMountPath = getLiquibaseParameter(tenantId, "audit-disk-mount-path");

        String softDeleteDataAction = getLiquibaseParameter(tenantId, "soft-delete-data-action");
        String softDeleteDataRunIntervalDays = getLiquibaseParameter(tenantId, "soft-delete-data-run-interval-days");
        String softDeleteDataRetentionDays = getLiquibaseParameter(tenantId, "soft-delete-data-retention-days");
        String softDeleteDataS3BucketName = getLiquibaseParameter(tenantId, "soft-delete-data-s3-bucket-name");
        String softDeleteDataS3Region = getLiquibaseParameter(tenantId, "soft-delete-data-s3-region");
        String softDeleteDataDiskMountPath = getLiquibaseParameter(tenantId, "soft-delete-data-disk-mount-path");

        if (noArchivalParameters(tokenAction, tokenRunIntervalDays, tokenRetentionDays,
            tokenS3BucketName, tokenS3Region, tokenDiskMountPath, auditAction,
            auditRunIntervalDays, auditRetentionDays, auditS3BucketName, auditS3Region, auditDiskMountPath,
            softDeleteDataAction, softDeleteDataRunIntervalDays, softDeleteDataRetentionDays,
            softDeleteDataS3BucketName, softDeleteDataS3Region, softDeleteDataDiskMountPath)) {
            return false;
        }

        putAndLog(liquibaseParams, tenantId, TOKEN_ACTION, tokenAction);
        putAndLog(liquibaseParams, tenantId, TOKEN_RUN_INTERVAL_DAYS, tokenRunIntervalDays);
        putAndLog(liquibaseParams, tenantId, TOKEN_RETENTION_DAYS, tokenRetentionDays);
        putAndLog(liquibaseParams, tenantId, TOKEN_S3_BUCKET_NAME, tokenS3BucketName);
        putAndLog(liquibaseParams, tenantId, TOKEN_S3_REGION, tokenS3Region);
        putAndLog(liquibaseParams, tenantId, TOKEN_DISK_MOUNT_PATH, tokenDiskMountPath);

        putAndLog(liquibaseParams, tenantId, AUDIT_ACTION, auditAction);
        putAndLog(liquibaseParams, tenantId, AUDIT_RUN_INTERVAL_DAYS, auditRunIntervalDays);
        putAndLog(liquibaseParams, tenantId, AUDIT_RETENTION_DAYS, auditRetentionDays);
        putAndLog(liquibaseParams, tenantId, AUDIT_S3_BUCKET_NAME, auditS3BucketName);
        putAndLog(liquibaseParams, tenantId, AUDIT_S3_REGION, auditS3Region);
        putAndLog(liquibaseParams, tenantId, AUDIT_DISK_MOUNT_PATH, auditDiskMountPath);

        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_ACTION, softDeleteDataAction);
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_RUN_INTERVAL_DAYS, softDeleteDataRunIntervalDays);
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_RETENTION_DAYS, softDeleteDataRetentionDays);
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_S3_BUCKET_NAME, softDeleteDataS3BucketName);
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_S3_REGION, softDeleteDataS3Region);
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_DISK_MOUNT_PATH, softDeleteDataDiskMountPath);

        LOGGER.info("Loaded archival Liquibase parameters from environment for tenant {}", tenantId);
        return true;
    }

    private boolean noArchivalParameters(String... values) {
        for (String value : values) {
            if (value != null) {
                return false;
            }
        }
        return true;
    }

    private void addArchivalLiquibaseParametersFromProperties(String tenantId,
            Map<String, String> liquibaseParams) {
        // Fallback to MultiTenantProperties bean (for backward compatibility)
        UserManagementTenantProperties tenant = multiTenantProperties.getTenantProperties(tenantId);
        if (tenant == null || tenant.getLiquibase() == null || tenant.getLiquibase().getParameters() == null) {
            LOGGER.warn("No tenant-specific archival Liquibase properties found for tenant: {}", tenantId);
            return;
        }

        UserManagementTenantProperties.LiquibaseProperties.ParametersProperties params = tenant.getLiquibase()
                .getParameters();
        putAndLog(liquibaseParams, tenantId, TOKEN_ACTION, params.getTokenAction());
        putAndLog(liquibaseParams, tenantId, TOKEN_RUN_INTERVAL_DAYS,
                params.getTokenRunIntervalDays());
        putAndLog(liquibaseParams, tenantId, TOKEN_RETENTION_DAYS, params.getTokenRetentionDays());
        putAndLog(liquibaseParams, tenantId, TOKEN_S3_BUCKET_NAME, params.getTokenS3BucketName());
        putAndLog(liquibaseParams, tenantId, TOKEN_S3_REGION, params.getTokenS3Region());
        putAndLog(liquibaseParams, tenantId, TOKEN_DISK_MOUNT_PATH, params.getTokenDiskMountPath());

        putAndLog(liquibaseParams, tenantId, AUDIT_ACTION, params.getAuditAction());
        putAndLog(liquibaseParams, tenantId, AUDIT_RUN_INTERVAL_DAYS, params.getAuditRunIntervalDays());
        putAndLog(liquibaseParams, tenantId, AUDIT_RETENTION_DAYS, params.getAuditRetentionDays());
        putAndLog(liquibaseParams, tenantId, AUDIT_S3_BUCKET_NAME, params.getAuditS3BucketName());
        putAndLog(liquibaseParams, tenantId, AUDIT_S3_REGION, params.getAuditS3Region());
        putAndLog(liquibaseParams, tenantId, AUDIT_DISK_MOUNT_PATH, params.getAuditDiskMountPath());

        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_ACTION, params.getSoftDeleteDataAction());
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_RUN_INTERVAL_DAYS,
                params.getSoftDeleteDataRunIntervalDays());
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_RETENTION_DAYS,
                params.getSoftDeleteDataRetentionDays());
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_S3_BUCKET_NAME, params.getSoftDeleteDataS3BucketName());
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_S3_REGION, params.getSoftDeleteDataS3Region());
        putAndLog(liquibaseParams, tenantId, SOFT_DELETE_DATA_DISK_MOUNT_PATH,
                params.getSoftDeleteDataDiskMountPath());

        LOGGER.info("Loaded archival Liquibase parameters from MultiTenantProperties bean for tenant {}", tenantId);
    }

    /**
     * Reads a tenant-scoped Liquibase parameter, falling back to the default tenant template when
     * the current tenant is the default tenant.
     *
     * @param tenantId the tenant identifier
     * @param propertySuffix the parameter name suffix, e.g. "token-s3-bucket-name"
     * @return the resolved value, or null if not configured anywhere
     */
    private String getLiquibaseParameter(String tenantId, String propertySuffix) {
        String value = getPropertyFromEnvironment("tenants.profile." + tenantId
                + ".liquibase.parameters." + propertySuffix);
        if (value != null) {
            return value;
        }

        if (defaultTenant != null && defaultTenant.equals(tenantId)) {
            return getPropertyFromEnvironment("tenant.props.default.liquibase.parameters." + propertySuffix);
        }

        return null;
    }

    private void putAndLog(Map<String, String> target, String tenantId, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.put(key, value);
            LOGGER.debug("Loaded Liquibase parameter {} for tenant {}", key, tenantId);
        }
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

            // Get schema name for this tenant
            String defaultUidamSchema = getSchemaNameForTenant(tenantId);

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
            validateSchemaName(defaultUidamSchema);

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
