package org.eclipse.ecsp.uidam.usermanagement.config;

import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Configuration for H2 database compatibility in tests.
 * This configuration overrides PostgreSQL-specific settings to work with H2.
 */
@Configuration
@Profile("test")
public class H2TestConfiguration {

    /**
     * Provides a mock TenantConfigurationService for tests.
     * This is needed because the real TenantConfigurationService is excluded from test profile.
     */
    @Bean
    public TenantConfigurationService tenantConfigurationService() {

        // Create mock tenant properties for tests
        UserManagementTenantProperties tenantProperties = new UserManagementTenantProperties();

        // Set essential properties needed by tests
        tenantProperties.setMaxAllowedLoginAttempts("5"); // Default value for tests

        // Create mock notification properties
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties
                .setNotificationApiUrl("http://test-notification-api:8080/v1/notifications/nonRegisteredUsers");
        notificationProperties.setNotificationId("TEST_NOTIFICATION_ID");

        tenantProperties.setNotification(notificationProperties);
        TenantConfigurationService mockService = mock(TenantConfigurationService.class);
        // Configure the mock to return the tenant properties
        when(mockService.getTenantProperties()).thenReturn(tenantProperties);

        return mockService;
    }

    /**
     * Customizes Hibernate properties for H2 compatibility.
     * Disables schema validation and sets up H2-specific configurations.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            // Disable physical naming strategy to avoid schema issues
            hibernateProperties.put("hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
            hibernateProperties.put("hibernate.implicit_naming_strategy",
                    "org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl");

            // Use H2 dialect explicitly
            hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");

            // Disable schema validation for tests
            hibernateProperties.put("hibernate.hbm2ddl.auto", "create-drop");

            // Set default schema to avoid uidam schema issues
            hibernateProperties.put("hibernate.default_schema", "");

            // Enable legacy identifier generation for compatibility
            hibernateProperties.put("hibernate.id.new_generator_mappings", "false");

            // Disable foreign key constraint creation for H2 compatibility
            hibernateProperties.put("hibernate.hbm2ddl.create_foreign_keys", "false");
        };
    }
}
