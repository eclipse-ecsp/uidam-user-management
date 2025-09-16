package org.eclipse.ecsp.uidam.usermanagement.config;

import liquibase.integration.spring.SpringLiquibase;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.MultiTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
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
    
    @Value("#{'${tenant.ids}'.split(',')}")
    private List<String> tenantIds;
    
    @Value("${uidam.liquibase.change-log.path}")
    private String liquibaseChangeLogPath;
    
    @Value("${uidam.default.db.schema}")
    private String defaultUidamSchema;
    
    /**
     * Constructor for LiquibaseConfig.
     *
     * @param dataSource            the multi-tenant DataSource
     * @param multiTenantProperties the multi-tenant properties
     */
    public LiquibaseConfig(@Qualifier("multiTenantDataSource") DataSource dataSource,
                          MultiTenantProperties multiTenantProperties) {
        this.dataSource = dataSource;
        this.multiTenantProperties = multiTenantProperties;
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
    @ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true")
    @DependsOn("multiTenantDataSource")
    // Bean creation will be skipped when spring.liquibase.enabled=false (e.g., in tests)
    public SpringLiquibase createSchemaForTenant() {
        SpringLiquibase liquibase = new SpringLiquibase();
        for (String tenantId : tenantIds) {
            TenantContext.setCurrentTenant(tenantId);
            MDC.put(TENANT_HEADER, tenantId);
            AbstractRoutingDataSource abstractRoutingDataSource = (AbstractRoutingDataSource) dataSource;
            liquibase.setDataSource(abstractRoutingDataSource.getResolvedDataSources().get(tenantId));
            liquibase.setChangeLog(liquibaseChangeLogPath);
            liquibase.setContexts(tenantId);
            liquibase.setDefaultSchema(defaultUidamSchema);
            
            // Get tenant-specific Liquibase parameters from tenant properties
            Map<String, String> liquibaseParams = getTenantSpecificLiquibaseParameters(tenantId);
            liquibase.setChangeLogParameters(liquibaseParams);

            try {
                // Create schema if it doesn't exist
                dataSource.getConnection().createStatement()
                        .execute("CREATE SCHEMA IF NOT EXISTS " + defaultUidamSchema);

                // Run Liquibase migration
                LOGGER.info("Liquibase configuration Start run for tenant {}", tenantId);
                liquibase.afterPropertiesSet();
                LOGGER.info("Liquibase configuration Completed run for tenant {}", tenantId);
                MDC.remove(TENANT_HEADER);
                TenantContext.clear();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Liquibase : " + tenantId, e);
            } finally {
                MDC.remove(TENANT_HEADER);
                TenantContext.clear();
            }
        }
        return null;
    }
    
   
    /**
     * Retrieve tenant-specific Liquibase parameters from tenant properties.
     *
     * @param tenantId the tenant identifier
     * @return a map of Liquibase parameters for the specified tenant
     */
    private Map<String, String> getTenantSpecificLiquibaseParameters(String tenantId) {
        Map<String, String> liquibaseParams = new HashMap<>();
        liquibaseParams.put("schema", defaultUidamSchema);

        // Do not set tenant.id parameter to maintain null TENANT_ID for users
        // This preserves the original behavior where user records have null TENANT_ID

        // Get tenant-specific properties
        UserManagementTenantProperties tenant = multiTenantProperties.getTenantProperties(tenantId);
        if (tenant != null && tenant.getLiquibase() != null) {
            UserManagementTenantProperties.LiquibaseProperties liquibaseProps = tenant.getLiquibase();
            liquibaseParams.put("tenant.id", tenantId); // Optional: Include tenant.id if needed

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

            LOGGER.debug("Liquibase parameters for tenant {}: {}", tenantId, liquibaseParams);
            LOGGER.info("Liquibase parameters for tenant {}: {}", tenantId, liquibaseParams);
        } else {

            LOGGER.info("No tenant-specific Liquibase properties found for tenant: {}, using defaults", tenantId);
            LOGGER.debug("Default Liquibase parameters for tenant {}: {}", tenantId, liquibaseParams);
        }

        return liquibaseParams;
    }
}
