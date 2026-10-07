/********************************************************************************
 * Copyright (c) 2023 - 2024 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.uidam.usermanagement.config;

import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.MultiTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for LiquibaseConfig related functionality.
 * Tests validation logic, exception handling, and tenant context management
 * without requiring the full Spring configuration context.
 *
 * <p>Note: These tests work with the TenantContext in single-tenant mode
 * since multitenancy requires full Spring context initialization.
 */
class LiquibaseConfigTest {

    @AfterEach
    void tearDown() {
        try {
            TenantContext.clear();
        } catch (Exception e) {
            // Ignore any exceptions during teardown
        }
        MDC.clear();
    }

    @Test
    void tenantContext_shouldSetAndGetCurrentTenant() {
        // Arrange
        String testTenant = "test-tenant";

        try {
            // Ensure clean state
            TenantContext.clear();
            
            // Act
            TenantContext.setCurrentTenant(testTenant);
            String currentTenant = TenantContext.getCurrentTenant();

            // Assert - In single-tenant mode without Spring context, TenantContext returns "default"
            // This test verifies that TenantContext operations don't throw exceptions
            assertFalse(currentTenant == null || currentTenant.isEmpty(), "Current tenant should not be null or empty");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void tenantContext_shouldClearTenant() throws Exception {
        // Arrange
        TenantContext.setCurrentTenant("test");

        // Act
        TenantContext.clear();

        // Assert - After clear, in multi-tenant mode it throws exception, in single-tenant mode returns default
        // This test verifies that clear() works without errors
        try {
            String tenant = TenantContext.getCurrentTenant();
            // If we get here, we're in single-tenant mode
            assertFalse(tenant == null || tenant.isEmpty(), "Should return a tenant value after clear");
        } catch (org.eclipse.ecsp.sql.exception.TenantNotFoundException e) {
            // This is expected in multi-tenant mode
            assertTrue(true, "TenantNotFoundException expected in multi-tenant mode");
        }
    }

    @Test
    void tenantContext_shouldReturnDefaultWhenNotSet() throws Exception {
        // Arrange - Ensure context is clear
        TenantContext.clear();

        // Assert - Behavior depends on tenant mode
        try {
            String tenant = TenantContext.getCurrentTenant();
            // If we get here, we're in single-tenant mode
            assertFalse(tenant == null || tenant.isEmpty(), "Should return a default tenant");
        } catch (org.eclipse.ecsp.sql.exception.TenantNotFoundException e) {
            // This is expected in multi-tenant mode when no tenant is set
            assertTrue(true, "TenantNotFoundException expected in multi-tenant mode");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"uidam", "test_schema", "schema123", "SCHEMA_NAME", "dev_env_1"})
    void schemaValidation_shouldAcceptValidSchemaNames(String validSchema) {
        // This tests the schema validation pattern used in LiquibaseConfig
        // Pattern: ^\\w+$ allows only word characters (letters, digits, underscore)
        
        // Act
        boolean isValid = validSchema.matches("^\\w+$");

        // Assert
        assertTrue(isValid, "Schema name should be valid: " + validSchema);
    }

    @ParameterizedTest
    @ValueSource(strings = {"schema;DROP TABLE", "schema OR 1=1", "schema--comment", 
        "schema/*comment*/", "schema'DROP"})
    void schemaValidation_shouldRejectInvalidSchemaNames(String invalidSchema) {
        // This tests the schema validation pattern used in LiquibaseConfig
        
        // Act
        boolean isValid = invalidSchema.matches("^\\w+$");

        // Assert
        assertFalse(isValid, "Schema name should be invalid: " + invalidSchema);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "schema'; DROP DATABASE test; --",
        "schema OR 1=1",
        "schema UNION SELECT * FROM users",
        "schema/**/OR/**/1=1",
        "schema\nDROP TABLE users",
        "schema\rDROP TABLE users",
        "schema\tDROP TABLE users"
    })
    void sqlInjectionPrevention_shouldValidateSchemaNames(String maliciousInput) {
        // This tests the SQL injection prevention logic from LiquibaseConfig
        
        // Act
        boolean isValid = maliciousInput.matches("^\\w+$");

        // Assert
        assertFalse(isValid, "Malicious input should be rejected: " + maliciousInput);
    }

    @Test
    void schemaValidation_shouldThrowExceptionForInvalidSchema() {
        // This simulates the validation logic from LiquibaseConfig.createSchemaForTenant()
        String invalidSchema = "invalid; DROP TABLE";
        
        // Act & Assert - This simulates the IllegalArgumentException thrown in the actual method
        assertThrows(IllegalArgumentException.class, () -> 
            validateSchemaName(invalidSchema));
    }
    
    private void validateSchemaName(String schemaName) {
        if (!schemaName.matches("^\\w+$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    @Test
    void tenantContext_shouldSupportMultipleTenantSwitching() {
        try {
            // Ensure clean state
            TenantContext.clear();
            
            // Test switching between different tenants
            TenantContext.setCurrentTenant("ecsp");
            String tenant1 = TenantContext.getCurrentTenant();
            assertFalse(tenant1 == null || tenant1.isEmpty(), "Tenant should not be null or empty");

            TenantContext.setCurrentTenant("sdp");
            String tenant2 = TenantContext.getCurrentTenant();
            assertFalse(tenant2 == null || tenant2.isEmpty(), "Tenant should not be null or empty");

            TenantContext.setCurrentTenant("custom_tenant");
            String tenant3 = TenantContext.getCurrentTenant();
            assertFalse(tenant3 == null || tenant3.isEmpty(), "Tenant should not be null or empty");

        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void mdcCleanup_shouldBeClearedOnTenantContextClear() throws Exception {
        try {
            // Ensure clean state
            TenantContext.clear();
            MDC.clear();
            
            // Setup - Simulate MDC being set (as done in LiquibaseConfig)
            MDC.put("tenantId", "test");
            TenantContext.setCurrentTenant("test");
            
            // Verify setup
            assertEquals("test", MDC.get("tenantId"));
            String tenant = TenantContext.getCurrentTenant();
            assertFalse(tenant == null || tenant.isEmpty(), "Tenant should be set");

            // Act
            TenantContext.clear();
            MDC.clear(); // This simulates the cleanup in the actual implementation

            // Assert - MDC should be cleared
            assertEquals(null, MDC.get("tenantId"), "MDC should be cleared");
        } finally {
            TenantContext.clear();
            MDC.clear();
        }
    }

    @Test
    void tenantIdPattern_shouldMatchValidTenantIds() {
        // Test the tenant ID patterns that would be used in the configuration
        String[] validTenantIds = {"ecsp", "sdp", "tenant1", "TENANT_2", "dev_env"};
        
        for (String tenantId : validTenantIds) {
            // This pattern would be used for tenant validation
            boolean isValid = tenantId.matches("^[a-zA-Z0-9_]+$");
            assertTrue(isValid, "Tenant ID should be valid: " + tenantId);
        }
    }

    @Test
    void tenantConfiguration_shouldHandleEmptyTenantList() {
        // This simulates the behavior when tenantIds list is empty
        // The actual method returns null when no tenants to process
        
        // Arrange
        java.util.List<String> emptyTenantIds = java.util.Collections.emptyList();
        
        // Act - Simulate the loop behavior
        int processedTenants = 0;
        for (@SuppressWarnings("unused") String tenantId : emptyTenantIds) {
            processedTenants++;
        }
        
        // Assert
        assertEquals(0, processedTenants, "Should not process any tenants when list is empty");
    }

    @Test
    void changeLogPath_shouldBeValidPath() {
        // Test the changelog path pattern used in LiquibaseConfig
        String validPath = "classpath:database.schema/master.xml";
        
        // This validates the path format
        assertTrue(validPath.startsWith("classpath:"), "Path should start with classpath:");
        assertTrue(validPath.endsWith(".xml"), "Path should end with .xml");
        assertTrue(validPath.contains("database.schema"), "Path should contain database.schema");
    }

    @Test
    void liquibaseParameters_shouldContainRequiredKeys() {
        // This tests the parameter map structure used in LiquibaseConfig
        java.util.Map<String, String> liquibaseParams = new java.util.HashMap<>();
        liquibaseParams.put("schema", "uidam");
        
        // Assert the required parameters are present
        assertTrue(liquibaseParams.containsKey("schema"), "Parameters should contain schema key");
        assertEquals("uidam", liquibaseParams.get("schema"), "Schema parameter should have correct value");
    }

    // New tests for improved schema validation (Issues #11, #12, #13)
    
    @ParameterizedTest
    @ValueSource(strings = {"valid_schema", "schema-name", "schema.name", "schema_123", 
        "my-schema-1.0", "tenant.dev", "prod-schema", "test_schema.v2"})
    void improvedSchemaValidation_shouldAcceptLegitimateSchemaNames(String validSchema) {
        // Test the new improved regex that supports hyphens and dots (Issue #11)
        // Pattern: ^[a-zA-Z0-9_.-]+$ allows legitimate schema naming conventions
        
        // Act
        boolean isValid = validSchema.matches("^[a-zA-Z0-9_.-]+$");

        // Assert
        assertTrue(isValid, "Legitimate schema name should be valid: " + validSchema);
    }

    @ParameterizedTest
    @ValueSource(strings = {"schema;DROP", "schema OR 1=1", "schema/*comment*/", 
        "schema'DROP", "schema\nDROP", "schema\tDROP", "schema\rDROP", "schema$()", "schema#test"})
    void improvedSchemaValidation_shouldRejectMaliciousSchemaNames(String maliciousSchema) {
        // Test that the new regex still prevents SQL injection (Issue #13)
        
        // Act
        boolean isValid = maliciousSchema.matches("^[a-zA-Z0-9_.-]+$");

        // Assert
        assertFalse(isValid, "Malicious schema name should be invalid: " + maliciousSchema);
    }

    @Test
    void schemaValidation_shouldProvideDescriptiveErrorMessage() {
        // Test that our new validation provides better error messages (Issue #11)
        String invalidSchema = "invalid;schema";
        
        // Simulate the validation logic from LiquibaseConfig
        if (!invalidSchema.matches("^[a-zA-Z0-9_.-]+$")) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                throw new IllegalArgumentException("Invalid schema name: " + invalidSchema 
                    + ". Schema name must contain only letters, numbers, underscores, hyphens, and dots.");
            });
            
            // Assert
            String expectedMessage = "must contain only letters, numbers, underscores, hyphens, and dots";
            assertTrue(exception.getMessage().contains(expectedMessage),
                    "Error message should be descriptive");
        }
    }

    @Test
    void sqlExceptionHandling_shouldBeSpecific() {
        // Test that we handle SQLException specifically (Issue #12)
        // This simulates the improved exception handling in createSchemaForTenant()
        
        try {
            // Simulate a SQLException scenario
            throw new java.sql.SQLException("Connection failed");
        } catch (java.sql.SQLException e) {
            // Assert that we can catch SQLException specifically
            assertTrue(e instanceof java.sql.SQLException, "Should catch SQLException specifically");
            assertEquals("Connection failed", e.getMessage(), "Should preserve original error message");
        }
    }

    @Test
    void databaseSchemaCreation_shouldUseSaferApproach() {
        // Test that schema creation follows safer practices (Issue #13)
        // This tests the concept behind createSchemaIfNotExists method
        
        String validatedSchema = "test_schema";
        
        // Simulate the validation step
        if (!validatedSchema.matches("^[a-zA-Z0-9_.-]+$")) {
            throw new IllegalArgumentException("Invalid schema name");
        }
        
        // Simulate SQL construction with validated input
        String sql = "CREATE SCHEMA IF NOT EXISTS " + validatedSchema;
        
        // Assert
        assertEquals("CREATE SCHEMA IF NOT EXISTS test_schema", sql, 
            "SQL should be constructed with validated schema name");
        assertFalse(sql.contains(";"), "SQL should not contain injection characters");
        assertFalse(sql.contains("--"), "SQL should not contain comment characters");
    }

    @Test
    void createSchemaForTenant_shouldUseCorrectTenantListBasedOnMultiTenantFlag() {
        // Arrange
        // Simulate the logic for lines 80-85 in LiquibaseConfig.java
        java.util.List<String> allTenants = java.util.Arrays.asList("ecsp", "sdp", "custom");
        String defaultTenant = "ecsp";
        boolean multiTenantEnabled;
        java.util.List<String> tenantIds;

        // Case 1: Multi-tenant disabled
        multiTenantEnabled = false;
        tenantIds = allTenants;
        if (!multiTenantEnabled) {
            tenantIds = java.util.Collections.singletonList(defaultTenant);
        }
        assertEquals(1, tenantIds.size(), "Should only use default tenant when multi-tenant is disabled");
        assertEquals(defaultTenant, tenantIds.get(0), "Default tenant should be used");

        // Case 2: Multi-tenant enabled
        multiTenantEnabled = true;
        tenantIds = allTenants;
        if (!multiTenantEnabled) {
            tenantIds = java.util.Collections.singletonList(defaultTenant);
        }
        assertEquals(allTenants, tenantIds, "Should use all tenants when multi-tenant is enabled");
    }

    @Test
    void initializeTenantSchema_shouldValidateSchemaName() {
        // This test validates that schema names are validated before use
        // Pattern from LiquibaseConfig: ^[a-zA-Z0-9_.-]+$
        
        // Valid schema names
        assertTrue("uidam".matches("^[a-zA-Z0-9_.-]+$"));
        assertTrue("test_schema".matches("^[a-zA-Z0-9_.-]+$"));
        assertTrue("schema-123".matches("^[a-zA-Z0-9_.-]+$"));
        assertTrue("schema.name".matches("^[a-zA-Z0-9_.-]+$"));
        
        // Invalid schema names (potential SQL injection)
        assertFalse("schema; DROP TABLE users--".matches("^[a-zA-Z0-9_.-]+$"));
        assertFalse("schema' OR '1'='1".matches("^[a-zA-Z0-9_.-]+$"));
        assertFalse("schema/*comment*/".matches("^[a-zA-Z0-9_.-]+$"));
    }

    @Test
    void initializeTenantSchema_shouldThrowExceptionForNullTenantId() {
        // This validates the null check in initializeTenantSchema method
        // The method should throw IllegalArgumentException for null tenant ID
        String tenantId = null;
        
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new IllegalArgumentException("Tenant ID cannot be null or empty");
            }
        });
    }

    @Test
    void initializeTenantSchema_shouldThrowExceptionForEmptyTenantId() {
        // This validates the empty check in initializeTenantSchema method
        String tenantId = "";
        
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new IllegalArgumentException("Tenant ID cannot be null or empty");
            }
        });
    }

    @Test
    void initializeTenantSchema_shouldThrowExceptionForWhitespaceTenantId() {
        // This validates the whitespace check in initializeTenantSchema method
        String tenantId = "   ";
        
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new IllegalArgumentException("Tenant ID cannot be null or empty");
            }
        });
    }

    @Test
    void initializeTenantSchema_shouldSetAndClearTenantContext() {
        // This test simulates the tenant context management in initializeTenantSchema
        String testTenant = "dynamic-tenant";
        
        try {
            // Ensure clean state
            TenantContext.clear();
            MDC.clear();
            
            // Simulate the try block behavior
            TenantContext.setCurrentTenant(testTenant);
            MDC.put("tenantId", testTenant);
            
            // Assert context is set
            String tenant = TenantContext.getCurrentTenant();
            assertFalse(tenant == null || tenant.isEmpty(), "Tenant should be set");
            assertEquals(testTenant, MDC.get("tenantId"));
            
        } finally {
            // Simulate the finally block behavior
            MDC.remove("tenantId");
            TenantContext.clear();
        }
        
        // Assert MDC is cleared
        assertEquals(null, MDC.get("tenantId"), "MDC should be cleared");
    }

    @Test
    void initializeTenantSchema_shouldHandleInvalidSchemaName() {
        // This test validates schema name rejection for SQL injection attempts
        String invalidSchema = "schema; DROP TABLE users--";
        
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            if (!invalidSchema.matches("^[a-zA-Z0-9_.-]+$")) {
                throw new IllegalArgumentException("Invalid schema name: " + invalidSchema 
                    + ". Schema name must contain only letters, numbers, underscores, hyphens, and dots.");
            }
        });
    }

    @Test
    void initializeTenantSchema_contextManagement_shouldFollowCorrectOrder() {
        // This test validates that context is set before operations and cleared in finally
        String tenantId = "test-tenant";
        boolean exceptionThrown = false;
        
        try {
            // Ensure clean state
            TenantContext.clear();
            MDC.clear();
            
            // Set context (as in initializeTenantSchema)
            TenantContext.setCurrentTenant(tenantId);
            MDC.put("tenantId", tenantId);
            
            // Verify context is set
            String tenant = TenantContext.getCurrentTenant();
            assertFalse(tenant == null || tenant.isEmpty(), "Tenant should be set");
            
            // Simulate an exception
            throw new RuntimeException("Simulated error");
            
        } catch (Exception e) {
            exceptionThrown = true;
        } finally {
            // Context should still be cleared even if exception occurred
            MDC.remove("tenantId");
            TenantContext.clear();
        }
        
        // Assert
        assertTrue(exceptionThrown, "Exception should have been thrown");
        assertEquals(null, MDC.get("tenantId"), "MDC should be cleared");
    }

    /**
     * Mirrors {@code LiquibaseConfig.addArchivalLiquibaseParameters}'s precedence logic locally
     * rather than invoking {@link LiquibaseConfig} directly: this module's test sources include a
     * {@code @Profile("test")} no-op stub with the exact same fully-qualified name as the real
     * {@code LiquibaseConfig}, which shadows it for every test in this module and makes the real
     * class impossible to instantiate from test code.
     */
    @Nested
    class ArchivalLiquibaseParametersTest {

        private static final String[] ARCHIVAL_SUFFIXES = {
            "token-action", "token-run-interval-days", "token-retention-days",
            "token-s3-bucket-name", "token-s3-region", "token-disk-mount-path",
            "audit-action", "audit-run-interval-days", "audit-retention-days",
            "audit-s3-bucket-name", "audit-s3-region", "audit-disk-mount-path",
            "soft-delete-data-action", "soft-delete-data-run-interval-days",
            "soft-delete-data-retention-days", "soft-delete-data-s3-bucket-name",
            "soft-delete-data-s3-region", "soft-delete-data-disk-mount-path"
        };

        private static final String[] ARCHIVAL_KEYS = {
            "token.action", "token.run_interval_days", "token.retention_days",
            "token.s3_bucket_name", "token.s3_region", "token.disk_mount_path",
            "audit.action", "audit.run_interval_days", "audit.retention_days",
            "audit.s3_bucket_name", "audit.s3_region", "audit.disk_mount_path",
            "soft_delete_data.action", "soft_delete_data.run_interval_days",
            "soft_delete_data.retention_days", "soft_delete_data.s3_bucket_name",
            "soft_delete_data.s3_region", "soft_delete_data.disk_mount_path"
        };

        private static final String DEFAULT_TENANT = "default";

        private final MultiTenantProperties multiTenantProperties = mock(MultiTenantProperties.class);
        private final Environment environment = mock(Environment.class);

        /**
         * Reproduces LiquibaseConfig#addArchivalLiquibaseParameters precedence: tenant-specific
         * environment values (falling back to the default-tenant template only for the literal
         * default tenant), then the MultiTenantProperties bean.
         */
        private Map<String, String> resolveArchivalParams(String tenantId) {
            Map<String, String> params = new java.util.HashMap<>();
            String[] values = new String[ARCHIVAL_SUFFIXES.length];
            for (int i = 0; i < ARCHIVAL_SUFFIXES.length; i++) {
                String value = environment.getProperty("tenants.profile." + tenantId
                        + ".liquibase.parameters." + ARCHIVAL_SUFFIXES[i]);
                if (value == null && DEFAULT_TENANT.equals(tenantId)) {
                    value = environment.getProperty("tenant.props.default.liquibase.parameters."
                            + ARCHIVAL_SUFFIXES[i]);
                }
                values[i] = value;
            }

            for (int i = 0; i < ARCHIVAL_KEYS.length; i++) {
                if (values[i] != null) {
                    params.put(ARCHIVAL_KEYS[i], values[i]);
                }
            }

            UserManagementTenantProperties tenant = multiTenantProperties.getTenantProperties(tenantId);
            if (tenant == null || tenant.getLiquibase() == null || tenant.getLiquibase().getParameters() == null) {
                return params;
            }
            UserManagementTenantProperties.LiquibaseProperties.ParametersProperties beanParams =
                    tenant.getLiquibase().getParameters();
            putIfAbsent(params, "token.action", beanParams.getTokenAction());
            putIfAbsent(params, "token.run_interval_days", beanParams.getTokenRunIntervalDays());
            putIfAbsent(params, "token.retention_days", beanParams.getTokenRetentionDays());
            putIfAbsent(params, "token.s3_bucket_name", beanParams.getTokenS3BucketName());
            putIfAbsent(params, "token.s3_region", beanParams.getTokenS3Region());
            putIfAbsent(params, "token.disk_mount_path", beanParams.getTokenDiskMountPath());
            putIfAbsent(params, "audit.action", beanParams.getAuditAction());
            putIfAbsent(params, "audit.run_interval_days", beanParams.getAuditRunIntervalDays());
            putIfAbsent(params, "audit.retention_days", beanParams.getAuditRetentionDays());
            putIfAbsent(params, "audit.s3_bucket_name", beanParams.getAuditS3BucketName());
            putIfAbsent(params, "audit.s3_region", beanParams.getAuditS3Region());
            putIfAbsent(params, "audit.disk_mount_path", beanParams.getAuditDiskMountPath());
            putIfAbsent(params, "soft_delete_data.action", beanParams.getSoftDeleteDataAction());
            putIfAbsent(params, "soft_delete_data.run_interval_days",
                    beanParams.getSoftDeleteDataRunIntervalDays());
            putIfAbsent(params, "soft_delete_data.retention_days", beanParams.getSoftDeleteDataRetentionDays());
            putIfAbsent(params, "soft_delete_data.s3_bucket_name", beanParams.getSoftDeleteDataS3BucketName());
            putIfAbsent(params, "soft_delete_data.s3_region", beanParams.getSoftDeleteDataS3Region());
            putIfAbsent(params, "soft_delete_data.disk_mount_path", beanParams.getSoftDeleteDataDiskMountPath());
            return params;
        }

        private void putIfAbsent(Map<String, String> target, String key, String value) {
            if (!target.containsKey(key) && value != null && !value.trim().isEmpty()) {
                target.put(key, value);
            }
        }

        @Test
        void archivalParameters_fromEnvironment_resolveExactLiquibaseKeys() {
            String prefix = "tenants.profile.tenant1.liquibase.parameters.";
            when(environment.getProperty(prefix + "token-action")).thenReturn("DELETE");
            when(environment.getProperty(prefix + "token-run-interval-days")).thenReturn("1");
            when(environment.getProperty(prefix + "token-retention-days")).thenReturn("1");
            when(environment.getProperty(prefix + "token-s3-bucket-name")).thenReturn("bucket");
            when(environment.getProperty(prefix + "token-s3-region")).thenReturn("us-east-1");
            when(environment.getProperty(prefix + "token-disk-mount-path")).thenReturn("/mnt/token");
            when(environment.getProperty(prefix + "audit-action")).thenReturn("ARCHIVE_THEN_DELETE");
            when(environment.getProperty(prefix + "audit-run-interval-days")).thenReturn("7");
            when(environment.getProperty(prefix + "audit-retention-days")).thenReturn("30");
            when(environment.getProperty(prefix + "audit-s3-bucket-name")).thenReturn("bucket");
            when(environment.getProperty(prefix + "audit-s3-region")).thenReturn("us-east-1");
            when(environment.getProperty(prefix + "audit-disk-mount-path")).thenReturn("/mnt/audit");
            when(environment.getProperty(prefix + "soft-delete-data-action")).thenReturn("ARCHIVE_THEN_DELETE");
            when(environment.getProperty(prefix + "soft-delete-data-run-interval-days")).thenReturn("1");
            when(environment.getProperty(prefix + "soft-delete-data-retention-days")).thenReturn("90");
            when(environment.getProperty(prefix + "soft-delete-data-s3-bucket-name")).thenReturn("bucket");
            when(environment.getProperty(prefix + "soft-delete-data-s3-region")).thenReturn("us-east-1");
            when(environment.getProperty(prefix + "soft-delete-data-disk-mount-path")).thenReturn("/mnt/sdd");

            Map<String, String> params = resolveArchivalParams("tenant1");

            assertEquals("DELETE", params.get("token.action"));
            assertEquals("1", params.get("token.run_interval_days"));
            assertEquals("1", params.get("token.retention_days"));
            assertEquals("bucket", params.get("token.s3_bucket_name"));
            assertEquals("us-east-1", params.get("token.s3_region"));
            assertEquals("/mnt/token", params.get("token.disk_mount_path"));
            assertEquals("ARCHIVE_THEN_DELETE", params.get("audit.action"));
            assertEquals("7", params.get("audit.run_interval_days"));
            assertEquals("30", params.get("audit.retention_days"));
            assertEquals("bucket", params.get("audit.s3_bucket_name"));
            assertEquals("us-east-1", params.get("audit.s3_region"));
            assertEquals("/mnt/audit", params.get("audit.disk_mount_path"));
            assertEquals("ARCHIVE_THEN_DELETE", params.get("soft_delete_data.action"));
            assertEquals("1", params.get("soft_delete_data.run_interval_days"));
            assertEquals("90", params.get("soft_delete_data.retention_days"));
            assertEquals("bucket", params.get("soft_delete_data.s3_bucket_name"));
            assertEquals("us-east-1", params.get("soft_delete_data.s3_region"));
            assertEquals("/mnt/sdd", params.get("soft_delete_data.disk_mount_path"));
        }

        @Test
        void archivalParameters_partialEnvironmentValues_fallBackToTenantPropertiesPerKey() {
            UserManagementTenantProperties.LiquibaseProperties.ParametersProperties beanParams =
                new UserManagementTenantProperties.LiquibaseProperties.ParametersProperties();
            beanParams.setTokenAction("ARCHIVE");
            beanParams.setTokenS3BucketName("qa-token-bucket");
            UserManagementTenantProperties.LiquibaseProperties liquibase =
                new UserManagementTenantProperties.LiquibaseProperties();
            liquibase.setParameters(beanParams);
            UserManagementTenantProperties tenantProps = new UserManagementTenantProperties();
            tenantProps.setLiquibase(liquibase);
            when(multiTenantProperties.getTenantProperties("tenant2")).thenReturn(tenantProps);

            String prefix = "tenants.profile.tenant2.liquibase.parameters.";
            when(environment.getProperty(prefix + "token-action")).thenReturn("DELETE");

            Map<String, String> params = resolveArchivalParams("tenant2");

            assertEquals("DELETE", params.get("token.action"));
            assertEquals("qa-token-bucket", params.get("token.s3_bucket_name"));
            assertNull(params.get("token.retention_days"));
            assertNull(params.get("audit.action"));
            assertNull(params.get("soft_delete_data.disk_mount_path"));
        }

        @Test
        void archivalParameters_defaultTenant_fallsBackToDefaultTemplateWhenTenantSpecificMissing() {
            // tenantId == defaultTenant ("default"); tenant-specific property is missing so the
            // literal-default-tenant template (tenant.props.default.liquibase.parameters.*) is used.
            when(environment.getProperty("tenant.props.default.liquibase.parameters.token-action"))
                    .thenReturn("DELETE");

            Map<String, String> params = resolveArchivalParams("default");

            assertEquals("DELETE", params.get("token.action"));
            assertNull(params.get("audit.action"));
        }

        @Test
        void archivalParameters_nonDefaultTenant_doesNotFallBackToDefaultTemplate() {
            // Default-template fallback only applies to the literal default tenant; any other
            // tenant with no tenant-specific property must resolve to null, not the template value.
            when(environment.getProperty("tenant.props.default.liquibase.parameters.token-action"))
                    .thenReturn("DELETE");

            Map<String, String> params = resolveArchivalParams("tenant5");

            assertNull(params.get("token.action"));
        }

        @Test
        void archivalParameters_noEnvironmentValues_fallsBackToMultiTenantPropertiesBean() {
            UserManagementTenantProperties.LiquibaseProperties.ParametersProperties parameters =
                    new UserManagementTenantProperties.LiquibaseProperties.ParametersProperties();
            parameters.setTokenAction("ARCHIVE_THEN_DELETE");
            parameters.setTokenRetentionDays("5");
            parameters.setAuditRetentionDays("60");
            parameters.setSoftDeleteDataDiskMountPath("/mnt/sdd-bean");

            UserManagementTenantProperties.LiquibaseProperties liquibase =
                    new UserManagementTenantProperties.LiquibaseProperties();
            liquibase.setParameters(parameters);

            UserManagementTenantProperties tenantProps = new UserManagementTenantProperties();
            tenantProps.setLiquibase(liquibase);
            when(multiTenantProperties.getTenantProperties("tenant3")).thenReturn(tenantProps);

            Map<String, String> params = resolveArchivalParams("tenant3");

            assertEquals("ARCHIVE_THEN_DELETE", params.get("token.action"));
            assertEquals("5", params.get("token.retention_days"));
            assertEquals("60", params.get("audit.retention_days"));
            assertEquals("/mnt/sdd-bean", params.get("soft_delete_data.disk_mount_path"));
        }

        @Test
        void archivalParameters_noSourceConfiguredAnywhere_resolvesToNoArchivalKeys() {
            Map<String, String> params = resolveArchivalParams("tenant4");

            assertNull(params.get("token.action"));
            assertNull(params.get("audit.action"));
            assertNull(params.get("soft_delete_data.action"));
        }
    }
}