# UIDAM User Management – Properties Reference

---

## Table of Contents

1. [application.properties](#1-applicationproperties)
2. [Tenant Default Properties](#2-tenant-default-properties)

---

## Tenant Property Naming Convention

| File | Property prefix used in file | Equivalent ENV-variable convention |
|------|-------------------------------|-------------------------------------|
| `tenant-default.properties` | `tenant.props.default.<property>` | `DEFAULT_<PROPERTY>` |
| `tenant-<TENANTID>.properties` | `tenants.profile.<TENANTID>.<property>` | `<TENANTID>_<PROPERTY>` |

**Tenant property key convention:**  
`tenants_profile_<TENANTID>_<property-key>` where dots (`.`) are replaced with underscores (`_`) and hyphens (`-`) are kept as-is.

---

## 1. `application.properties`

### 1.1 Application & Server

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `spring.application.name` | `spring_application_name` | `uidam-user-management` |
| `spring.application.version` | `spring_application_version` | `1.0` |
| `service.name` | `spring_application_servicename` | `uidam-user-management` |
| `server.port` | `server_port` | `8080` |
| `application.security.disabled` | `application_security_disabled` | `true` |
| `spring.main.allow-bean-definition-overriding` | `spring_main_allow-bean-definition-overriding` | `true` |

### 1.2 Database

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `postgres.jdbc.url` | `POSTGRES_DATASOURCE` | `jdbc:postgresql://localhost:5432/ChangeMe` |
| `postgres.username` | `POSTGRES_USERNAME` | `ChangeMe` |
| `postgres.password` | `POSTGRES_PASSWORD` | `ChangeMe` |
| `postgres.driver.class.name` | `postgres_driver_class_name` | `org.postgresql.Driver` |
| `postgres.pool.name` | `postgres_pool_name` | `hikariConnectionPool` |
| `postgres.data-source-properties.cachePrepStmts` | `postgres_data_source_properties_cachePrepStmts` | `true` |
| `postgres.data-source-properties.prepStmtCacheSize` | `postgres_data_source_properties_prepStmtCacheSize` | `250` |
| `postgres.data-source-properties.prepStmtCacheSqlLimit` | `postgres_data_source_properties_prepStmtCacheSqlLimit` | `2048` |
| `postgres.max.idle.time` | `postgres_max_idle_time` | `0` |
| `postgres.min.pool.size` | `postgres_min_pool_size` | `15` |
| `postgres.max.pool.size` | `postgres_max_pool_size` | `30` |
| `postgres.connection.timeout.ms` | `postgres_connection_timeout_ms` | `60000` |
| `postgres.expected99thPercentileMs` | `postgres_expected99thPercentileMs` | `60000` |
| `postgres.datasource.create.retry.count` | `postgres_create_retry_count` | `3` |
| `postgres.datasource.retry.delay.ms` | `postgres_retry_delay` | `30` |
| `postgresdb.metrics.enabled` | `postgresdb_metrics_enabled` | `false` |
| `postgresdb.metrics.executor.shutdown.buffer.ms` | `postgresdb_metrics_executor_shutdown_buffer_ms` | `2000` |
| `postgresdb.metrics.thread.freq.ms` | `postgresdb_metrics_thread_freq_ms` | `5000` |
| `postgresdb.metrics.thread.initial.delay.ms` | `postgresdb_metrics_thread_initial_delay_ms` | `2000` |

### 1.3 JPA / Hibernate

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `spring_jpa_hibernate_ddl-auto` | `none` |
| `spring.jpa.properties.hibernate.dialect` | `spring_jpa_properties_hibernate_dialect` | `org.hibernate.dialect.PostgreSQLDialect` |
| `spring.jpa.properties.hibernate.default_schema` | `UIDAM_DEFAULT_DB_SCHEMA` | `uidam` |
| `spring.jpa.properties.hibernate.format_sql` | `spring_jpa_properties_hibernate_format_sql` | `false` |
| `spring.jpa.show-sql` | `SHOW_SQL` | `false` |

### 1.4 Liquibase & Multi-tenancy

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `spring.liquibase.enabled` | `spring_liquibase_enabled` | `true` |
| `uidam.liquibase.change-log.path` | `UIDAM_LIQUIBASE_CHANGE_LOG_PATH` | `classpath:database.schema/master.xml` |
| `uidam.default.db.schema` | `UIDAM_DEFAULT_DB_SCHEMA` | `uidam` |
| `uidam.liquibase.db.credential.global` | `UIDAM_LIQUIBASE_DB_CREDENTIAL_GLOBAL` | `true` |
| `uidam.tenant.config.dbname.validation` | `UIDAM_TENANT_CONFIG_DBNAME_VALIDATION` | `CONTAINS` |
| `tenant.ids` | `TENANT_IDS` | `<TENANT_ID1>,<TENANT_ID2>,...` |
| `tenant.default` | `TENANT_DEFAULT` | `<TENANTID>` |
| `tenant.multitenant.enabled` | `TENANT_MULTITENANT_ENABLED` | `false` |
| `tenant.config.validation.enabled` | `TENANT_CONFIG_VALIDATION_ENABLED` | `true` |
| `multitenancy.enabled` | `multitenancy_enabled` | `true` |
| `spring.config.import` | `UIDAM_CONFIG_IMPORT` | `optional:classpath:tenant-default.properties,...` |

### 1.5 Security & Password

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `hash.algorithm` | `hash_algorithm` | `SHA-256` |
| `security.password.policy.check-interval` | `security_password_policy_check-interval` | `60s` |

### 1.6 Health & Actuators

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `management.endpoint.health.show-details` | `management_endpoint_health_show-details` | `always` |
| `management.endpoint.health.probes.enabled` | `management_endpoint_health_probes_enabled` | `true` |
| `management.health.livenessState.enabled` | `management_health_livenessState_enabled` | `true` |
| `management.health.readinessState.enabled` | `management_health_readinessState_enabled` | `true` |
| `management.health.db.enabled` | `management_health_db_enabled` | `false` |
| `management.endpoints.web.exposure.include` | `management_endpoints_web_exposure_include` | `health,info,prometheus,metrics,refresh` |
| `health.postgresdb.monitor.enabled` | `health_postgresdb_monitor_enabled` | `false` |
| `health.postgresdb.monitor.restart.on.failure` | `health_postgresdb_monitor_restart_on_failure` | `false` |

### 1.7 Metrics – Prometheus

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `metrics.prometheus.enabled` | `metrics_prometheus_enabled` | `false` |
| `prometheus.agent.port` | `prometheus_agent_port` | `9100` |
| `prometheus.agent.port.exposed` | `prometheus_agent_port_exposed` | `9100` |

### 1.8 Metrics – Datadog

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `management.datadog.metrics.export.enabled` | `metrics_datadog_enabled` | `false` |
| `management.datadog.metrics.export.api-key` | `metrics_datadog_apiKey` | `api-key` |
| `management.datadog.metrics.export.application-key` | `metrics_datadog_applicationKey` | `applicationKey` |
| `management.datadog.metrics.export.descriptions` | `metrics_datadog_descriptions` | `true` |
| `management.datadog.metrics.export.uri` | `metrics_datadog_uri` | `https://api.datadoghq.eu` |
| `management.datadog.metrics.export.step` | `metrics_datadog_step` | `30s` |
| `management.datadog.metrics.export.read-timeout` | `metrics_datadog_readTimeout` | `5s` |
| `management.datadog.metrics.export.connect-timeout` | `metrics_datadog_connectTimeout` | `5s` |
| `management.datadog.metrics.export.batch-size` | `metrics_datadog_batchSize` | `1000` |

### 1.9 Logging (Graylog)

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `APP_GRAYLOG_ENABLED` | `GRAYLOG_ENABLE` | `false` |
| `APP_GRAYLOG_HOST` | `GRAYLOG_HOST` | `graylog.default.svc.cluster.local` |
| `APP_GRAYLOG_PORT` | `GRAYLOG_PORT` | `12201` |
| `APP_NEVER_BLOCK_FOR_GRAYLOG` | `NEVER_BLOCK_FOR_GRAYLOG` | `false` |
| `APP_LOG_FOLDER` | `LOG_FOLDER` | `logs/` |
| `APP_LOG_LEVEL` | `LOG_LEVEL` | `ERROR` |
| `APP_SVC_LOG_LEVEL` | `SVC_LOG_LEVEL` | `ERROR` |
| `APP_SPRING_LOG_LEVEL` | `SPRING_LOG_LEVEL` | `ERROR` |
| `APP_PROP_LOAD_LOG_LEVEL` | `PROP_LOAD_LOG_LEVEL` | `INFO` |

### 1.10 Config Server

| Property Name | ENV Variable | Default Value |
|---|---|---|
| `spring.cloud.config.uri` | `CONFIG_SERVER_HOST` | `http://localhost:8888` |
| `spring.cloud.config.enabled` | `CONFIG_SERVER_ENABLED` | `false` |
| `spring.cloud.config.retry.max-attempts` | `CONFIG_SERVER_RETRY_MAX_ATTEMPTS` | `6` |
| `spring.cloud.config.retry.initial-interval` | `CONFIG_SERVER_RETRY_INITIAL_INTERVAL` | `1000` |
| `spring.cloud.config.retry.max-interval` | `CONFIG_SERVER_RETRY_MAX_INTERVAL` | `2000` |

---

## 2. Tenant Default Properties

> File: `tenant-default.properties`  
> Property prefix in file: `tenant.props.default.<property>`

### 2.1 Core Identity

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `tenant-id` | `tenant.props.default.tenant-id` | `TENANT_DEFAULT` | `default` | `tenants_profile_<TENANTID>_tenant-id` |
| `tenant-name` | `tenant.props.default.tenant-name` | `DEFAULT_TENANT_NAME` | `default` | `tenants_profile_<TENANTID>_tenant-name` |
| `account-name` | `tenant.props.default.account-name` | `DEFAULT_ACCOUNT_NAME` | `default` | `tenants_profile_<TENANTID>_account-name` |
| `account-type` | `tenant.props.default.account-type` | `DEFAULT_ACCOUNT_TYPE` | `Root` | `tenants_profile_<TENANTID>_account-type` |

### 2.2 User Management

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `password-encoder` | `tenant.props.default.password-encoder` | `DEFAULT_PASSWORD_ENCODER` | `SHA-256` | `tenants_profile_<TENANTID>_password-encoder` |
| `max-allowed-login-attempts` | `tenant.props.default.max-allowed-login-attempts` | `DEFAULT_MAX_ALLOWED_LOGIN_ATTEMPTS` | `3` | `tenants_profile_<TENANTID>_max-allowed-login-attempts` |
| `is-user-status-life-cycle-enabled` | `tenant.props.default.is-user-status-life-cycle-enabled` | `DEFAULT_IS_USER_STATUS_LIFE_CYCLE_ENABLED` | `false` | `tenants_profile_<TENANTID>_is-user-status-life-cycle-enabled` |
| `external-user-permitted-roles` | `tenant.props.default.external-user-permitted-roles` | `DEFAULT_EXTERNAL_USER_PERMITTED_ROLES` | `VEHICLE_OWNER` | `tenants_profile_<TENANTID>_external-user-permitted-roles` |
| `external-user-default-status` | `tenant.props.default.external-user-default-status` | `DEFAULT_EXTERNAL_USER_DEFAULT_STATUS` | *(empty)* | `tenants_profile_<TENANTID>_external-user-default-status` |
| `user-default-account-name` | `tenant.props.default.user-default-account-name` | `DEFAULT_USER_DEFAULT_ACCOUNT_NAME` | `userdefaultaccount` | `tenants_profile_<TENANTID>_user-default-account-name` |
| `additional-attr-check-enabled-for-sign-up` | `tenant.props.default.additional-attr-check-enabled-for-sign-up` | `DEFAULT_ADDITIONAL_ATTR_CHECK_ENABLED_FOR_SIGN_UP` | `false` | `tenants_profile_<TENANTID>_additional-attr-check-enabled-for-sign-up` |

### 2.3 Authentication

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `auth.admin-scope` | `tenant.props.default.auth.admin-scope` | `DEFAULT_AUTH_ADMIN_SCOPE` | `UIDAMSystem` | `tenants_profile_<TENANTID>_auth_admin-scope` |

### 2.4 Email Verification

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `is-email-verification-enabled` | `tenant.props.default.is-email-verification-enabled` | `DEFAULT_IS_EMAIL_VERIFICATION_ENABLED` | `false` | `tenants_profile_<TENANTID>_is-email-verification-enabled` |
| `email-verification-url` | `tenant.props.default.email-verification-url` | `DEFAULT_EMAIL_VERIFICATION_URL` | `https://localhost:8080/default/v1/emailVerification/%s` | `tenants_profile_<TENANTID>_email-verification-url` |
| `email-verification-notification-id` | `tenant.props.default.email-verification-notification-id` | `DEFAULT_EMAIL_VERIFICATION_NOTIFICATION_ID` | `UIDAM_USER_VERIFY_ACCOUNT` | `tenants_profile_<TENANTID>_email-verification-notification-id` |
| `email-verification-exp-days` | `tenant.props.default.email-verification-exp-days` | `DEFAULT_EMAIL_VERIFICATION_EXP_DAYS` | `7` | `tenants_profile_<TENANTID>_email-verification-exp-days` |
| `email-regex-pattern-exclude` | `tenant.props.default.email-regex-pattern-exclude` | `DEFAULT_EMAIL_REGEX_PATTERN_EXCLUDE` | *(empty)* | `tenants_profile_<TENANTID>_email-regex-pattern-exclude` |
| `auth-server-email-verification-response-url` | `tenant.props.default.auth-server-email-verification-response-url` | `DEFAULT_AUTH_SERVER_EMAIL_VERIFICATION_RESPONSE_URL` | `https://localhost:9443/default/emailVerification/verify` | `tenants_profile_<TENANTID>_auth-server-email-verification-response-url` |
| `email-logo-path` | `tenant.props.default.email-logo-path` | `DEFAULT_TENANT_EMAIL_LOGO_PATH` | `images/default-logo.svg` | `tenants_profile_<TENANTID>_email-logo-path` |
| `email-copyright` | `tenant.props.default.email-copyright` | `DEFAULT_TENANT_EMAIL_COPYRIGHT` | `Copyright © 2023 HARMAN International. All Rights Reserved.` | `tenants_profile_<TENANTID>_email-copyright` |

### 2.5 Password Recovery

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `recovery-secret-expires-in-minutes` | `tenant.props.default.recovery-secret-expires-in-minutes` | `DEFAULT_RECOVERY_SECRET_EXPIRES_IN_MINUTES` | `15` | `tenants_profile_<TENANTID>_recovery-secret-expires-in-minutes` |
| `password-recovery-notification-id` | `tenant.props.default.password-recovery-notification-id` | `DEFAULT_PASSWORD_RECOVERY_NOTIFICATION_ID` | `UIDAM_USER_PASSWORD_RECOVERY` | `tenants_profile_<TENANTID>_password-recovery-notification-id` |
| `auth-server-reset-response-url` | `tenant.props.default.auth-server-reset-response-url` | `DEFAULT_AUTH_SERVER_RESET_RESPONSE_URL` | `https://localhost:9443/default/recovery/reset/` | `tenants_profile_<TENANTID>_auth-server-reset-response-url` |
| `captcha-enforce-after-no-of-failures` | `tenant.props.default.captcha-enforce-after-no-of-failures` | `DEFAULT_CAPTCHA_ENFORCE_AFTER_NO_OF_FAILURES` | *(empty)* | `tenants_profile_<TENANTID>_captcha-enforce-after-no-of-failures` |

### 2.6 Database

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `jdbc-url` | `tenant.props.default.jdbc-url` | `DEFAULT_POSTGRES_DATASOURCE` | `jdbc:postgresql://localhost:5432/ChangeMe` | `tenants_profile_<TENANTID>_jdbc-url` |
| `user-name` | `tenant.props.default.user-name` | `DEFAULT_POSTGRES_USERNAME` | `ChangeMe` | `tenants_profile_<TENANTID>_user-name` |
| `password` | `tenant.props.default.password` | `DEFAULT_POSTGRES_PASSWORD` | `ChangeMe` | `tenants_profile_<TENANTID>_password` |
| `driver-class-name` | `tenant.props.default.driver-class-name` | *(fixed: `org.postgresql.Driver`)* | `org.postgresql.Driver` | `tenants_profile_<TENANTID>_driver-class-name` |
| `max-pool-size` | `tenant.props.default.max-pool-size` | `DEFAULT_POSTGRES_MAX_POOL_SIZE` | `30` | `tenants_profile_<TENANTID>_max-pool-size` |
| `max-idle-time` | `tenant.props.default.max-idle-time` | `DEFAULT_POSTGRES_MAX_IDLE_TIME` | `0` | `tenants_profile_<TENANTID>_max-idle-time` |
| `connection-timeout-ms` | `tenant.props.default.connection-timeout-ms` | `DEFAULT_POSTGRES_CONNECTION_TIMEOUT_MS` | `60000` | `tenants_profile_<TENANTID>_connection-timeout-ms` |
| `default-schema` | `tenant.props.default.default-schema` | `DEFAULT_POSTGRES_DEFAULT_SCHEMA` | `uidam` | `tenants_profile_<TENANTID>_default-schema` |
| `cache-prep-stmts` | `tenant.props.default.cache-prep-stmts` | `DEFAULT_POSTGRES_CACHE_PREP_STMTS` | `true` | `tenants_profile_<TENANTID>_cache-prep-stmts` |
| `prep-stmt-cache-size` | `tenant.props.default.prep-stmt-cache-size` | `DEFAULT_POSTGRES_PREP_STMT_CACHE_SIZE` | `250` | `tenants_profile_<TENANTID>_prep-stmt-cache-size` |
| `prep-stmt-cache-sql-limit` | `tenant.props.default.prep-stmt-cache-sql-limit` | `DEFAULT_POSTGRES_PREP_STMT_CACHE_SQL_LIMIT` | `2048` | `tenants_profile_<TENANTID>_prep-stmt-cache-sql-limit` |

### 2.7 Notification Configuration

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `notification.notification-api-url` | `tenant.props.default.notification.notification-api-url` | `DEFAULT_NOTIFICATION_API_URL` | `http://notification-api-int-svc:8080/v1/notifications/nonRegisteredUsers` | `tenants_profile_<TENANTID>_notification_notification-api-url` |
| `notification.notification-id` | `tenant.props.default.notification.notification-id` | `DEFAULT_NOTIFICATION_ID` | `uidamCustomEmailTemplate5` | `tenants_profile_<TENANTID>_notification_notification-id` |
| `notification.config.resolver` | `tenant.props.default.notification.config.resolver` | `DEFAULT_NOTIFICATION_CONFIG_RESOLVER` | `internal` | `tenants_profile_<TENANTID>_notification_config_resolver` |
| `notification.config.path` | `tenant.props.default.notification.config.path` | `DEFAULT_NOTIFICATION_CONFIG_PATH` | `classpath:/notification/uidam-notification-config.json` | `tenants_profile_<TENANTID>_notification_config_path` |
| `notification.template.engine` | `tenant.props.default.notification.template.engine` | `DEFAULT_NOTIFICATION_TEMPLATE_ENGINE` | `thymeleaf` | `tenants_profile_<TENANTID>_notification_template_engine` |
| `notification.template.format` | `tenant.props.default.notification.template.format` | `DEFAULT_NOTIFICATION_TEMPLATE_FORMAT` | `HTML` | `tenants_profile_<TENANTID>_notification_template_format` |
| `notification.template.resolver` | `tenant.props.default.notification.template.resolver` | `DEFAULT_NOTIFICATION_TEMPLATE_RESOLVER` | `CLASSPATH` | `tenants_profile_<TENANTID>_notification_template_resolver` |
| `notification.template.prefix` | `tenant.props.default.notification.template.prefix` | `DEFAULT_NOTIFICATION_TEMPLATE_PREFIX` | `/notification/` | `tenants_profile_<TENANTID>_notification_template_prefix` |
| `notification.template.suffix` | `tenant.props.default.notification.template.suffix` | `DEFAULT_NOTIFICATION_TEMPLATE_SUFFIX` | `.html` | `tenants_profile_<TENANTID>_notification_template_suffix` |

### 2.8 Email Provider

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `notification.email.provider` | `tenant.props.default.notification.email.provider` | `DEFAULT_NOTIFICATION_EMAIL_PROVIDER` | `internal` | `tenants_profile_<TENANTID>_notification_email_provider` |
| `notification.email.host` | `tenant.props.default.notification.email.host` | `DEFAULT_NOTIFICATION_EMAIL_PROVIDER_HOST` | `smtp.gmail.com` | `tenants_profile_<TENANTID>_notification_email_host` |
| `notification.email.port` | `tenant.props.default.notification.email.port` | `DEFAULT_NOTIFICATION_EMAIL_PROVIDER_PORT` | `587` | `tenants_profile_<TENANTID>_notification_email_port` |
| `notification.email.username` | `tenant.props.default.notification.email.username` | `DEFAULT_NOTIFICATION_EMAIL_PROVIDER_USERNAME` | `ChangeMe` | `tenants_profile_<TENANTID>_notification_email_username` |
| `notification.email.password` | `tenant.props.default.notification.email.password` | `DEFAULT_NOTIFICATION_EMAIL_PROVIDER_PASSWORD` | `ChangeMe` | `tenants_profile_<TENANTID>_notification_email_password` |
| `notification.email.properties.mail.smtp.auth` | `tenant.props.default.notification.email.properties.mail.smtp.auth` | `DEFAULT_NOTIFICATION_EMAIL_PROPERTIES_MAIL_SMTP_AUTH` | `true` | `tenants_profile_<TENANTID>_notification_email_properties_mail_smtp_auth` |
| `notification.email.properties.mail.smtp.starttls.enabled` | `tenant.props.default.notification.email.properties.mail.smtp.starttls.enabled` | `DEFAULT_NOTIFICATION_EMAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | `true` | `tenants_profile_<TENANTID>_notification_email_properties_mail_smtp_starttls_enabled` |
| `notification.email.properties.mail.smtp.starttls.required` | `tenant.props.default.notification.email.properties.mail.smtp.starttls.required` | `DEFAULT_NOTIFICATION_EMAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED` | `true` | `tenants_profile_<TENANTID>_notification_email_properties_mail_smtp_starttls_required` |
| `notification.email.properties.mail.smtp.ssl.enabled` | `tenant.props.default.notification.email.properties.mail.smtp.ssl.enabled` | `DEFAULT_NOTIFICATION_EMAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE` | `false` | `tenants_profile_<TENANTID>_notification_email_properties_mail_smtp_ssl_enabled` |
| `notification.email.properties.mail.debug` | `tenant.props.default.notification.email.properties.mail.debug` | `DEFAULT_NOTIFICATION_EMAIL_PROPERTIES_MAIL_DEBUG` | `false` | `tenants_profile_<TENANTID>_notification_email_properties_mail_debug` |

### 2.9 Auth Server Integration

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `auth-server.host-name` | `tenant.props.default.auth-server.host-name` | `DEFAULT_AUTHORIZATION_SERVER_URL` | `http://uidam-authorization-server:9443` | `tenants_profile_<TENANTID>_auth-server_host-name` |
| `auth-server.token-url` | `tenant.props.default.auth-server.token-url` | `DEFAULT_AUTH_SERVER_TOKEN_URL` | `/default/oauth2/token` | `tenants_profile_<TENANTID>_auth-server_token-url` |
| `auth-server.revoke-token-url` | `tenant.props.default.auth-server.revoke-token-url` | `DEFAULT_AUTH_SERVER_REVOKE_TOKEN_URL` | `/default/revoke/revokeByAdmin` | `tenants_profile_<TENANTID>_auth-server_revoke-token-url` |
| `auth-server.client-id` | `tenant.props.default.auth-server.client-id` | `DEFAULT_CLIENT_ID` | `token-mgmt` | `tenants_profile_<TENANTID>_auth-server_client-id` |
| `auth-server.client-secret` | `tenant.props.default.auth-server.client-secret` | `DEFAULT_CLIENT_SECRET` | `ChangeMe` | `tenants_profile_<TENANTID>_auth-server_client-secret` |

### 2.10 Client Registration

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `client-registration.default-status` | `tenant.props.default.client-registration.default-status` | `DEFAULT_CLIENT_REGISTRATION_DEFAULT_STATUS` | `approved` | `tenants_profile_<TENANTID>_client-registration_default-status` |
| `client-registration.secret-key` | `tenant.props.default.client-registration.secret-key` | `DEFAULT_CLIENT_REGISTRATION_SECRET_KEY` | `ChangeMe` | `tenants_profile_<TENANTID>_client-registration_secret-key` |
| `client-registration.secret-salt` | `tenant.props.default.client-registration.secret-salt` | `DEFAULT_CLIENT_REGISTRATION_SECRET_SALT` | `ChangeMe` | `tenants_profile_<TENANTID>_client-registration_secret-salt` |
| `client-registration.refresh-token-validity` | `tenant.props.default.client-registration.refresh-token-validity` | `DEFAULT_CLIENT_REFRESH_TOKEN_VALIDITY` | `3600` | `tenants_profile_<TENANTID>_client-registration_refresh-token-validity` |
| `client-registration.access-token-validity` | `tenant.props.default.client-registration.access-token-validity` | `DEFAULT_CLIENT_ACCESS_TOKEN_VALIDITY` | `3600` | `tenants_profile_<TENANTID>_client-registration_access-token-validity` |
| `client-registration.authorization-code-validity` | `tenant.props.default.client-registration.authorization-code-validity` | `DEFAULT_CLIENT_AUTHORIZATION_CODE_VALIDITY` | `300` | `tenants_profile_<TENANTID>_client-registration_authorization-code-validity` |
| `client-registration.hash-algorithm` | `tenant.props.default.client-registration.hash-algorithm` | `DEFAULT_CLIENT_HASH_ALGORITHM` | `SHA-256` | `tenants_profile_<TENANTID>_client-registration_hash-algorithm` |

### 2.11 Liquibase

| Property Name | Default Property | ENV Variable | Default Value | Tenant Property (`tenants_profile_<TENANTID>_<property>`) |
|---|---|---|---|---|
| `liquibase.change-log` | `tenant.props.default.liquibase.change-log` | `DEFAULT_LIQUIBASE_CHANGE_LOG` | `classpath:database.schema/master.xml` | `tenants_profile_<TENANTID>_liquibase_change-log` |
| `liquibase.default-schema` | `tenant.props.default.liquibase.default-schema` | `DEFAULT_LIQUIBASE_DEFAULT_SCHEMA` | `uidam` | `tenants_profile_<TENANTID>_liquibase_default-schema` |
| `liquibase.parameters.schema` | `tenant.props.default.liquibase.parameters.schema` | `DEFAULT_LIQUIBASE_PARAMETERS_SCHEMA` | `uidam` | `tenants_profile_<TENANTID>_liquibase_parameters_schema` |
| `liquibase.parameters.initial-data-client-secret` | `tenant.props.default.liquibase.parameters.initial-data-client-secret` | `DEFAULT_LIQUIBASE_INITIAL_DATA_CLIENT_SECRET` | `ChangeMe` | `tenants_profile_<TENANTID>_liquibase_parameters_initial-data-client-secret` |
| `liquibase.parameters.initial-data-user-salt` | `tenant.props.default.liquibase.parameters.initial-data-user-salt` | `DEFAULT_LIQUIBASE_INITIAL_DATA_USER_SALT` | `ChangeMe` | `tenants_profile_<TENANTID>_liquibase_parameters_initial-data-user-salt` |
| `liquibase.parameters.initial-data-user-pwd` | `tenant.props.default.liquibase.parameters.initial-data-user-pwd` | `DEFAULT_LIQUIBASE_INITIAL_DATA_USER_PWD` | `ChangeMe` | `tenants_profile_<TENANTID>_liquibase_parameters_initial-data-user-pwd` |


---

*End of document*
