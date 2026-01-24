# Multitenancy Developer Guidelines - UIDAM User Management

## Table of Contents
- [Overview](#overview)
- [Multitenancy Architecture](#multitenancy-architecture)
- [Configuration Scenarios](#configuration-scenarios)
- [Property Structure](#property-structure)
- [Validation Rules](#validation-rules)
- [Implementation Guide](#implementation-guide)
- [Troubleshooting](#troubleshooting)

---

## Overview

The UIDAM User Management service supports flexible multitenancy configurations that enable a single application instance to serve multiple isolated tenants. Each tenant can have its own database, notification settings, and configuration while sharing the same application codebase.

### Key Features
- **Flexible tenant isolation** with separate databases/schema per tenant
- **Hierarchical property resolution** with default values and tenant-specific overrides
- **Dynamic tenant configuration** supporting both local files and Spring Cloud Config Server
- **Runtime tenant resolution** from HTTP request headers for API's and context path for UI.
- **Comprehensive validation** of tenant configurations

---

## Multitenancy Architecture

### Core Components

| Component | Purpose |
|-----------|---------|
| `TenantResolutionFilter` | Extracts and validates tenant ID from requests |
| `TenantContext` | ThreadLocal storage for current tenant ID |
| `MultiTenantDataSource` | Routes database operations to tenant-specific databases |
| `TenantDefaultPropertiesProcessor` | Auto-generates missing tenant properties from defaults |
| `TenantConfigurationService` | Loads and caches tenant-specific properties |
| `TenantDatabaseNameValidator` | Validates database names against tenant IDs |

### Request Flow
```
1. HTTP Request → TenantResolutionFilter
2. Extract tenantId from header
3. Validate tenant exists → TenantContext.setCurrentTenant(tenantId)
4. Request processing → MultiTenantDataSource routes to tenant DB
5. Response sent → TenantContext.clear()
```

---

## Configuration Scenarios

### Scenario 1: Default (Single Tenant Mode)
**Multitenancy disabled with default tenant**

#### Configuration
```properties
# application.properties
tenant.multitenant.enabled=false
tenant.default=ecsp

# Spring config
spring.cloud.config.enabled=false
spring.config.import=optional:classpath:tenant-default.properties
```

#### Behavior
- **Tenant Resolution**: If no `tenantId` header is provided, uses `tenant.default` value
- **Request Requirements**: `tenantId` header is optional; defaults to configured default tenant
- **Use Case**: Single-tenant deployments

#### Example Request
```bash
# Both requests work
curl -H "tenantId: ecsp" http://localhost:8080/v1/users
curl http://localhost:8080/v1/users  # Uses default tenant 'ecsp'
```

---

### Scenario 2: Multitenancy without Config Server
**Multiple tenants with local property files**

#### Configuration
```properties
# application.properties
tenant.multitenant.enabled=true
tenant.ids=ecsp,sdp,tenant3
tenant.default=ecsp

# Config Server disabled
spring.cloud.config.enabled=false

# Import all tenant property files
spring.config.import=optional:classpath:tenant-default.properties,\
                     optional:classpath:tenant-ecsp.properties,\
                     optional:classpath:tenant-sdp.properties,\
                     optional:classpath:tenant-tenant3.properties
```

#### Property File Structure
```
src/main/resources/
├── tenant-default.properties    # Template for all tenants
├── tenant-ecsp.properties       # ECSP tenant overrides
├── tenant-sdp.properties        # SDP tenant overrides
└── tenant-tenant3.properties    # Tenant3 overrides
```

#### Behavior
- **Tenant Resolution**: Requires valid `tenantId` header in every request or tenantId in context path in the UI Requests.
- **Request Requirements**: Requests **MUST** include `tenantId` header; requests without it will fail with 400 Bad Request
- **Property Resolution**: 
  1. Loads `tenant-default.properties` as base
  2. Overlays `tenant-{tenantId}.properties` specific values
  3. Auto-generates missing properties from default
- **Use Case**: Multi-tenant deployments with static tenant configuration

#### Example Request
```bash
# Success
curl -H "tenantId: ecsp" http://localhost:8080/v1/users

# Failure (400 Bad Request)
curl http://localhost:8080/v1/users  # Missing tenantId header
```

---

### Scenario 3: Multitenancy with Config Server
**Multiple tenants with Spring Cloud Config Server**

#### Configuration
```properties
# application.properties
tenant.multitenant.enabled=true
tenant.ids=ecsp,sdp,tenant3 // this can be configured in config server to override add tenants dynamically.
tenant.default=ecsp

# Config Server enabled
spring.cloud.config.enabled=true
spring.cloud.config.uri=http://config-server:8888

# Import local defaults + config server and tenant related properties can be configured in config server ex: here tenant3 properties are available which will be configured in config server.
spring.config.import=optional:classpath:tenant-default.properties,\
                     optional:classpath:tenant-ecsp.properties,\
                     optional:classpath:tenant-sdp.properties,\
                     optional:configserver:

# Use tenant IDs as Spring profiles
spring.profiles.active=${tenant.ids}

# Retry configuration
spring.cloud.config.retry.max-attempts=6
spring.cloud.config.retry.initial-interval=1000
spring.cloud.config.retry.max-interval=2000
```

#### Config Server Structure
```yaml
# Config server repository structure
config-repo/
├── application.yml                    # Global defaults
├── application-ecsp.yml              # ECSP tenant profile
├── application-sdp.yml               # SDP tenant profile
└── application-tenant3.yml           # Tenant3 profile
```

#### Behavior
- **Tenant Resolution**: Requires valid `tenantId` header in every request
- **Request Requirements**: Requests **MUST** include `tenantId` header
- **Property Resolution** (priority order):
  1. Config Server tenant profile (`application-{tenantId}.yml`)
  2. Local tenant file (`tenant-{tenantId}.properties`)
  3. Default properties (`tenant-default.properties`)
- **Dynamic Updates**: Supports configuration refresh via `/actuator/refresh` endpoint (this works only single replica for multi-replica `/actuator/busrefresh`).
- **Use Case**: Production multi-tenant deployments requiring centralized configuration management

#### Example Request
```bash
# Success
curl -H "tenantId: sdp" http://localhost:8080/v1/users

# Failure (400 Bad Request)
curl http://localhost:8080/v1/users  # Missing tenantId header
```

---

## Property Structure

### Property Naming Convention
All tenant properties use the prefix: `tenants.profile.<tenantId>.<property-name>`

### Complete Property Reference

#### 1. Database Configuration (MANDATORY)
```properties
# Database connection
tenants.profile.<tenantId>.jdbc-url=${TENANT_POSTGRES_DATASOURCE}
tenants.profile.<tenantId>.user-name=${TENANT_POSTGRES_USERNAME}
tenants.profile.<tenantId>.password=${TENANT_POSTGRES_PASSWORD}
tenants.profile.<tenantId>.driver-class-name=org.postgresql.Driver

# Connection pool settings
tenants.profile.<tenantId>.max-pool-size=30
tenants.profile.<tenantId>.max-idle-time=0
tenants.profile.<tenantId>.connection-timeout-ms=60000
tenants.profile.<tenantId>.default-schema=uidam
```

#### 2. Notification Email Configuration (Conditional)
**MANDATORY when `notification.email.provider=internal`**

```properties
# Email provider selection
tenants.profile.<tenantId>.notification.email.provider=internal  # or 'ignite'

# SMTP settings (required only for 'internal' provider)
tenants.profile.<tenantId>.notification.email.host=smtp.gmail.com
tenants.profile.<tenantId>.notification.email.port=587
tenants.profile.<tenantId>.notification.email.username=email@example.com
tenants.profile.<tenantId>.notification.email.password=password

# SMTP properties
tenants.profile.<tenantId>.notification.email.properties.mail.smtp.auth=true
tenants.profile.<tenantId>.notification.email.properties.mail.smtp.starttls.enabled=true
tenants.profile.<tenantId>.notification.email.properties.mail.smtp.starttls.required=true
```

#### 3. Notification Configuration (Provider-specific)
```properties
# For 'ignite' provider
tenants.profile.<tenantId>.notification.notification-api-url=http://notification-api:8080
tenants.profile.<tenantId>.notification.notification-id=notificationTemplate1

# Template configuration
tenants.profile.<tenantId>.notification.config.resolver=internal
tenants.profile.<tenantId>.notification.config.path=classpath:/notification/config.json
tenants.profile.<tenantId>.notification.template.engine=thymeleaf
tenants.profile.<tenantId>.notification.template.format=HTML
```

#### 4. Core Identity Properties (Optional)
```properties
tenants.profile.<tenantId>.tenant-id=<tenantId>
tenants.profile.<tenantId>.tenant-name=Tenant Name
tenants.profile.<tenantId>.account-name=account
tenants.profile.<tenantId>.account-type=Root
```

#### 5. User Management Properties (Optional)
```properties
tenants.profile.<tenantId>.password-encoder=SHA-256
tenants.profile.<tenantId>.max-allowed-login-attempts=3
tenants.profile.<tenantId>.is-user-status-life-cycle-enabled=false
tenants.profile.<tenantId>.user-default-account-name=userdefaultaccount
```

#### 6. Email Verification Properties (Optional)
```properties
tenants.profile.<tenantId>.is-email-verification-enabled=false
tenants.profile.<tenantId>.email-verification-url=https://app.com/{tenantId}/verify/%s
tenants.profile.<tenantId>.email-verification-notification-id=USER_VERIFY_ACCOUNT
tenants.profile.<tenantId>.email-verification-exp-days=7
```

#### 7. Authorization Server Integration (Optional)
```properties
tenants.profile.<tenantId>.authorization-server.host-name=http://auth-server:9443
tenants.profile.<tenantId>.auth-server.token-url=/{tenantId}/oauth2/token
tenants.profile.<tenantId>.auth-server.client-id=client-id
tenants.profile.<tenantId>.auth-server.client-secret=client-secret
```

### Example: Complete Tenant Configuration

````properties name=tenant-ecsp.properties
# ECSP Tenant Configuration

# === MANDATORY: Database Configuration ===
tenants.profile.ecsp.jdbc-url=jdbc:postgresql://db-server:5432/ecsp_uidam
tenants.profile.ecsp.user-name=ecsp_user
tenants.profile.ecsp.password=securePassword123
tenants.profile.ecsp.driver-class-name=org.postgresql.Driver
tenants.profile.ecsp.max-pool-size=30
tenants.profile.ecsp.default-schema=uidam

# === MANDATORY: Email Configuration (internal provider) ===
tenants.profile.ecsp.notification.email.provider=internal
tenants.profile.ecsp.notification.email.host=smtp.gmail.com
tenants.profile.ecsp.notification.email.port=587
tenants.profile.ecsp.notification.email.username=noreply@ecsp.com
tenants.profile.ecsp.notification.email.password=emailPassword456
tenants.profile.ecsp.notification.email.properties.mail.smtp.auth=true
tenants.profile.ecsp.notification.email.properties.mail.smtp.starttls.enabled=true

# === Optional: Notification Template ===
tenants.profile.ecsp.notification.config.resolver=internal
tenants.profile.ecsp.notification.template.engine=thymeleaf
tenants.profile.ecsp.notification.template.format=HTML

# === Optional: Identity ===
tenants.profile.ecsp.tenant-id=ecsp
tenants.profile.ecsp.tenant-name=ECSP
tenants.profile.ecsp.account-name=ecsp
