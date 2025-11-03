# Multi-Tenant Notification System Configuration Guide

## Overview

This guide helps developers configure the multi-tenant notification system in UIDAM User Management. The system supports flexible email notification delivery with pluggable components for template management, rendering, and delivery.

### Key Features
- 🏢 **Multi-tenant Support**: Separate configurations per tenant
- 🔌 **Pluggable Architecture**: Mix and match components via configuration
- 📧 **Flexible Delivery**: SMTP or API-based email sending
- 🎨 **Multiple Template Engines**: Thymeleaf or Mustache
- 🗂️ **Dynamic or Static Templates**: Local files or remote API

---

## Quick Start

### Minimal Configuration (Development)

```properties
# Tenant ID: ecsp
tenant.tenants.ecsp.notification.config.resolver=internal
tenant.tenants.ecsp.notification.template.engine=mustache
tenant.tenants.ecsp.notification.email.provider=internal
tenant.tenants.ecsp.notification.email.host=localhost
tenant.tenants.ecsp.notification.email.port=1025
```

This gives you: Local JSON config → Mustache templates → Local SMTP (e.g., MailHog)

---

## 🎛️ Core Configuration Options

These three properties determine the main behavior of the notification system:

### 1. Notification Config Resolver

**What it controls:** Where notification template configurations are loaded from

**Property:**
```properties
tenant.tenants.{tenantId}.notification.config.resolver=internal
```

**Options:**

| Value | Description | When to Use |
|-------|-------------|-------------|
| `internal` | Read from local JSON file | Development, static templates, no external dependencies |
| `ignite` | Fetch from external REST API | Production, dynamic templates, centralized management |

**Implementation Details:**
- **Factory:** `NotificationConfigResolverFactory`
- **Internal:** `InternalNotificationConfigResolver` reads from `classpath:/notification/uidam-notification-config.json`
- **Ignite:** `IgniteNotificationConfigResolver` calls REST API at configured URL

**Example Configuration:**

```properties
# Internal (default)
tenant.tenants.ecsp.notification.config.resolver=internal
tenant.tenants.ecsp.notification.config.path=classpath:/notification/uidam-notification-config.json

# External API
tenant.tenants.sdp.notification.config.resolver=ignite
tenant.tenants.sdp.notification.notification-api-url=http://notification-service:8080
```

---

### 2. Template Engine

**What it controls:** How email templates are parsed and rendered

**Property:**
```properties
tenant.tenants.{tenantId}.notification.template.engine=thymeleaf
```

**Options:**

| Value | Description | Best For |
|-------|-------------|----------|
| `thymeleaf` | Spring-native, feature-rich HTML templates | Complex templates with logic, conditionals, loops |
| `mustache` | Simple, logic-less templates | Lightweight, language-agnostic, minimal dependencies |

**Template Syntax Comparison:**

```html
<!-- Thymeleaf -->
<p th:text="${userName}">Welcome</p>
<div th:if="${isPremium}">
    <p>Premium features available</p>
</div>
<ul>
    <li th:each="item : ${items}" th:text="${item}">Item</li>
</ul>

<!-- Mustache -->
<p>{{userName}}</p>
{{#isPremium}}
<div>
    <p>Premium features available</p>
</div>
{{/isPremium}}
<ul>
    {{#items}}
    <li>{{.}}</li>
    {{/items}}
</ul>
```

**Implementation Details:**
- **Factory:** `TemplateParserFactory`
- **Thymeleaf:** `ThymeleafTemplateParserImpl` - Spring-integrated template engine
- **Mustache:** `MustacheTemplateParserImpl` - Standalone template engine

---

### 3. Email Provider

**What it controls:** How emails are sent

**Property:**
```properties
tenant.tenants.{tenantId}.notification.email.provider=internal
```

**Options:**

| Value | Description | When to Use |
|-------|-------------|-------------|
| `internal` | Direct SMTP sending via JavaMail | Full control, on-premise servers, Gmail/Office365 |
| `ignite` | Delegate to external notification API | Centralized service, analytics, rate limiting |

**Implementation Details:**
- **Factory:** `EmailNotificationProviderFactory`
- **Internal:** `InternalEmailNotificationProvider` uses Spring's `JavaMailSender`
- **Ignite:** `IgniteEmailNotificationProvider` POSTs to external API

**Example Configuration:**

```properties
# Internal SMTP (Gmail)
tenant.tenants.ecsp.notification.email.provider=internal
tenant.tenants.ecsp.notification.email.host=smtp.gmail.com
tenant.tenants.ecsp.notification.email.port=587
tenant.tenants.ecsp.notification.email.username=${GMAIL_USER}
tenant.tenants.ecsp.notification.email.password=${GMAIL_APP_PASSWORD}

# External API
tenant.tenants.sdp.notification.email.provider=ignite
tenant.tenants.sdp.notification.notification-api-url=http://notification-service:8080
```

---

## 📋 Supporting Configuration

### 4. Template Location

**Properties:**
```properties
tenant.tenants.{tenantId}.notification.template.resolver=CLASSPATH
tenant.tenants.{tenantId}.notification.template.prefix=/notification/
tenant.tenants.{tenantId}.notification.template.suffix=.html
```

**Template Resolver Options:**

| Value | Description | Example Path |
|-------|-------------|--------------|
| `CLASSPATH` | Resources in classpath | `src/main/resources/notification/welcome.html` |
| `FILE` | Filesystem paths | `/var/templates/welcome.html` |
| `URL` | HTTP URLs | `http://templates.example.com/welcome.html` |

**How Templates are Resolved:**

```
Template Name: "welcome-email"
Prefix: /notification/
Suffix: .html
→ Full Path: /notification/welcome-email.html
```

**Example for Different Formats:**

```properties
# HTML emails (default)
tenant.tenants.ecsp.notification.template.prefix=/notification/
tenant.tenants.ecsp.notification.template.suffix=.html
# Resolves: /notification/password-reset.html

# Plain text emails
tenant.tenants.ecsp.notification.template.prefix=/notification/text/
tenant.tenants.ecsp.notification.template.suffix=.txt
# Resolves: /notification/text/password-reset.txt
```

---

### 5. Template Format

**Property:**
```properties
tenant.tenants.{tenantId}.notification.template.format=HTML
```

**Options:**

| Value | Description | File Extension | Use Case |
|-------|-------------|----------------|----------|
| `HTML` | HTML templates | `.html` | Rich formatted emails with styling |
| `XML` | XML templates | `.xml` | Structured data templates |
| `TEXT` | Plain text | `.txt` | Simple text-only emails |

---

### 6. SMTP Configuration (Internal Provider Only)

**Basic SMTP Settings:**

```properties
tenant.tenants.{tenantId}.notification.email.host=smtp.gmail.com
tenant.tenants.{tenantId}.notification.email.port=587
tenant.tenants.{tenantId}.notification.email.username=${SMTP_USER}
tenant.tenants.{tenantId}.notification.email.password=${SMTP_PASS}
```

**Advanced SMTP Properties (JavaMail):**

```properties
# Authentication
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.auth=true

# TLS/SSL
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.starttls.enabled=true
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.starttls.required=true
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.ssl.enabled=false

# Debugging
tenant.tenants.{tenantId}.notification.email.properties.mail.debug=false
```

**Common SMTP Configurations:**

<details>
<summary><b>Gmail</b></summary>

```properties
tenant.tenants.ecsp.notification.email.host=smtp.gmail.com
tenant.tenants.ecsp.notification.email.port=587
tenant.tenants.ecsp.notification.email.username=your-email@gmail.com
tenant.tenants.ecsp.notification.email.password=${GMAIL_APP_PASSWORD}
tenant.tenants.ecsp.notification.email.properties.mail.smtp.auth=true
tenant.tenants.ecsp.notification.email.properties.mail.smtp.starttls.enabled=true
```

**Note:** Use [App Passwords](https://support.google.com/accounts/answer/185833), not your regular password.
</details>

<details>
<summary><b>Office 365</b></summary>

```properties
tenant.tenants.ecsp.notification.email.host=smtp.office365.com
tenant.tenants.ecsp.notification.email.port=587
tenant.tenants.ecsp.notification.email.username=your-email@company.com
tenant.tenants.ecsp.notification.email.password=${OFFICE365_PASSWORD}
tenant.tenants.ecsp.notification.email.properties.mail.smtp.auth=true
tenant.tenants.ecsp.notification.email.properties.mail.smtp.starttls.enabled=true
```
</details>

<details>
<summary><b>AWS SES</b></summary>

```properties
tenant.tenants.ecsp.notification.email.host=email-smtp.us-east-1.amazonaws.com
tenant.tenants.ecsp.notification.email.port=587
tenant.tenants.ecsp.notification.email.username=${AWS_SES_SMTP_USER}
tenant.tenants.ecsp.notification.email.password=${AWS_SES_SMTP_PASSWORD}
tenant.tenants.ecsp.notification.email.properties.mail.smtp.auth=true
tenant.tenants.ecsp.notification.email.properties.mail.smtp.starttls.enabled=true
```
</details>

<details>
<summary><b>MailHog (Development)</b></summary>

```properties
tenant.tenants.ecsp.notification.email.host=localhost
tenant.tenants.ecsp.notification.email.port=1025
tenant.tenants.ecsp.notification.email.username=
tenant.tenants.ecsp.notification.email.password=
```

**Run MailHog:**
```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
# Access UI: http://localhost:8025
```
</details>

---

### 7. Notification Config Path (Internal Resolver Only)

**Property:**
```properties
tenant.tenants.{tenantId}.notification.config.path=classpath:/notification/uidam-notification-config.json
```

**What it controls:** Location of the JSON file containing notification configurations

**Example Config File:**

```json
{
  "notifications": [
    {
      "id": "UIDAM_USER_VERIFY_ACCOUNT",
      "locale": "en_US",
      "email": {
        "from": "noreply@example.com",
        "subject": "Verify your account",
        "body": {
          "header": "Welcome!",
          "footer": "Thanks for joining us"
        },
        "referenceHtml": "email-verification"
      }
    },
    {
      "id": "UIDAM_USER_PASSWORD_RESET",
      "locale": "en_US",
      "email": {
        "from": "noreply@example.com",
        "subject": "Reset your password",
        "body": {
          "header": "Password Reset Request"
        },
        "referenceHtml": "password-reset"
      }
    }
  ]
}
```

---

## 🔄 Configuration Combinations

Choose the right combination for your use case:

| Scenario | Config Resolver | Template Engine | Email Provider | Best For |
|----------|----------------|-----------------|----------------|----------|
| **Local Development** | internal | mustache | internal | Quick setup, offline development |
| **Enterprise On-Premise** | internal | thymeleaf | internal | Full control, custom SMTP servers |
| **Cloud-Native SaaS** | ignite | thymeleaf | ignite | Scalable, centralized management |
| **Hybrid Setup** | internal | thymeleaf | ignite | Static templates + API analytics |
| **Simple Microservice** | internal | mustache | ignite | Lightweight + centralized sending |
| **Dynamic Templates** | ignite | mustache | internal | Remote templates + local SMTP |

---

## 📖 Complete Examples

### Example 1: Local Development Setup

**Scenario:** Developer working offline with MailHog for email testing

```properties
# File: src/main/resources/application-dev.properties

# ECSP Tenant - Development
tenant.tenants.ecsp.notification.config.resolver=internal
tenant.tenants.ecsp.notification.config.path=classpath:/notification/uidam-notification-config.json

tenant.tenants.ecsp.notification.template.engine=mustache
tenant.tenants.ecsp.notification.template.format=HTML
tenant.tenants.ecsp.notification.template.resolver=CLASSPATH
tenant.tenants.ecsp.notification.template.prefix=/notification/
tenant.tenants.ecsp.notification.template.suffix=.html

tenant.tenants.ecsp.notification.email.provider=internal
tenant.tenants.ecsp.notification.email.host=localhost
tenant.tenants.ecsp.notification.email.port=1025
```

**Setup:**
```bash
# Start MailHog
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog

# Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# View emails: http://localhost:8025
```

---

### Example 2: Production with Gmail

**Scenario:** Small business using Gmail for transactional emails

```properties
# File: src/main/resources/application-prod.properties

# ECSP Tenant - Production
tenant.tenants.ecsp.notification.config.resolver=internal
tenant.tenants.ecsp.notification.config.path=classpath:/notification/uidam-notification-config.json

tenant.tenants.ecsp.notification.template.engine=thymeleaf
tenant.tenants.ecsp.notification.template.format=HTML
tenant.tenants.ecsp.notification.template.resolver=CLASSPATH
tenant.tenants.ecsp.notification.template.prefix=/notification/
tenant.tenants.ecsp.notification.template.suffix=.html

tenant.tenants.ecsp.notification.email.provider=internal
tenant.tenants.ecsp.notification.email.host=smtp.gmail.com
tenant.tenants.ecsp.notification.email.port=587
tenant.tenants.ecsp.notification.email.username=${GMAIL_USER}
tenant.tenants.ecsp.notification.email.password=${GMAIL_APP_PASSWORD}
tenant.tenants.ecsp.notification.email.properties.mail.smtp.auth=true
tenant.tenants.ecsp.notification.email.properties.mail.smtp.starttls.enabled=true
```

**Environment Variables:**
```bash
export GMAIL_USER="noreply@yourcompany.com"
export GMAIL_APP_PASSWORD="xxxx-xxxx-xxxx-xxxx"  # Generate at google.com/apppasswords
```

---

### Example 3: Cloud-Native with External API

**Scenario:** Microservices architecture with centralized notification service

```properties
# File: src/main/resources/application-prod.properties

# SDP Tenant - Cloud Production
tenant.tenants.sdp.notification.config.resolver=ignite
tenant.tenants.sdp.notification.notification-api-url=https://notification-api.example.com

tenant.tenants.sdp.notification.template.engine=mustache
tenant.tenants.sdp.notification.template.format=HTML
tenant.tenants.sdp.notification.template.resolver=CLASSPATH
tenant.tenants.sdp.notification.template.prefix=/notification/
tenant.tenants.sdp.notification.template.suffix=.html

tenant.tenants.sdp.notification.email.provider=ignite
```

**Benefits:**
- ✅ Centralized template management
- ✅ Email delivery analytics
- ✅ Rate limiting and retry logic
- ✅ No SMTP configuration needed

---

### Example 4: Multi-Tenant Configuration

**Scenario:** SaaS application with different settings per customer

```properties
# File: src/main/resources/application.properties

# ECSP Tenant - Uses Gmail
tenant.tenants.ecsp.notification.config.resolver=internal
tenant.tenants.ecsp.notification.template.engine=thymeleaf
tenant.tenants.ecsp.notification.email.provider=internal
tenant.tenants.ecsp.notification.email.host=smtp.gmail.com
tenant.tenants.ecsp.notification.email.port=587
tenant.tenants.ecsp.notification.email.username=${ECSP_SMTP_USER}
tenant.tenants.ecsp.notification.email.password=${ECSP_SMTP_PASS}

# SDP Tenant - Uses Office 365
tenant.tenants.sdp.notification.config.resolver=internal
tenant.tenants.sdp.notification.template.engine=mustache
tenant.tenants.sdp.notification.email.provider=internal
tenant.tenants.sdp.notification.email.host=smtp.office365.com
tenant.tenants.sdp.notification.email.port=587
tenant.tenants.sdp.notification.email.username=${SDP_SMTP_USER}
tenant.tenants.sdp.notification.email.password=${SDP_SMTP_PASS}
```

---

## 🏗️ Architecture Overview

### Component Flow

```
┌─────────────────────┐
│  Notification       │
│  Request            │
└──────────┬──────────┘
           │
           ▼
┌──────────────────────────────────────┐
│  Step 1: Load Template Config        │
│  NotificationConfigResolverFactory   │
├──────────────┬───────────────────────┤
│  internal    │  ignite                │
│  ↓           │  ↓                     │
│  JSON file   │  REST API              │
└──────────────┴───────────────────────┘
           │
           ▼
┌──────────────────────────────────────┐
│  Step 2: Parse Template              │
│  TemplateParserFactory               │
├──────────────┬───────────────────────┤
│  thymeleaf   │  mustache              │
│  ↓           │  ↓                     │
│  Complex HTML│  Simple templates      │
└──────────────┴───────────────────────┘
           │
           ▼
┌──────────────────────────────────────┐
│  Step 3: Send Email                  │
│  EmailNotificationProviderFactory    │
├──────────────┬───────────────────────┤
│  internal    │  ignite                │
│  ↓           │  ↓                     │
│  SMTP        │  REST API              │
└──────────────┴───────────────────────┘
           │
           ▼
    📧 Email Delivered
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `NotificationConfigResolverFactory` | Creates config resolver based on `notification.config.resolver` |
| `TemplateParserFactory` | Creates template parser based on `notification.template.engine` |
| `EmailNotificationProviderFactory` | Creates email provider based on `notification.email.provider` |
| `TenantAwareJavaMailSenderFactory` | Manages per-tenant SMTP connections |
| `TenantContext` | ThreadLocal storage for current tenant ID |
| `TenantConfigurationService` | Loads tenant-specific properties |

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---
