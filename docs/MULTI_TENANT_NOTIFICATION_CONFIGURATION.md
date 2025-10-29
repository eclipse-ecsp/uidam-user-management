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

## 💡 Developer Guide

### Getting Started (5 Minutes)

1. **Choose Your Setup**
   - Development? → Use `internal` + `mustache` + `localhost:1025`
   - Production? → Choose based on your infrastructure

2. **Set Required Properties**
   ```properties
   tenant.tenants.{yourTenant}.notification.config.resolver=internal
   tenant.tenants.{yourTenant}.notification.template.engine=mustache
   tenant.tenants.{yourTenant}.notification.email.provider=internal
   tenant.tenants.{yourTenant}.notification.email.host=localhost
   tenant.tenants.{yourTenant}.notification.email.port=1025
   ```

3. **Create Notification Config** (`src/main/resources/notification/uidam-notification-config.json`)
   ```json
   {
     "notifications": [{
       "id": "TEST_NOTIFICATION",
       "locale": "en_US",
       "email": {
         "from": "test@example.com",
         "subject": "Test Email",
         "body": {"header": "Hello"},
         "referenceHtml": "test-template"
       }
     }]
   }
   ```

4. **Create Template** (`src/main/resources/notification/test-template.html`)
   ```html
   <!DOCTYPE html>
   <html>
   <body>
     <h1>{{header}}</h1>
     <p>Hello {{userName}}!</p>
   </body>
   </html>
   ```

5. **Send Test Email**
   ```java
   NotificationManager.sendNotification("TEST_NOTIFICATION", userData);
   ```

### Adding a New Notification

1. Add config to `uidam-notification-config.json`:
   ```json
   {
     "id": "WELCOME_EMAIL",
     "locale": "en_US",
     "email": {
       "from": "welcome@example.com",
       "subject": "Welcome to {{companyName}}!",
       "body": {
         "header": "Welcome aboard!",
         "footer": "Get started today"
       },
       "referenceHtml": "welcome-email"
     }
   }
   ```

2. Create template `welcome-email.html`:
   ```html
   <!DOCTYPE html>
   <html>
   <body>
     <h1>{{header}}</h1>
     <p>Hi {{firstName}} {{lastName}},</p>
     <p>Thanks for joining {{companyName}}!</p>
     <footer>{{footer}}</footer>
   </body>
   </html>
   ```

3. Send it:
   ```java
   Map<String, Object> data = Map.of(
       "firstName", "John",
       "lastName", "Doe",
       "companyName", "Acme Corp"
   );
   notificationManager.sendNotification("WELCOME_EMAIL", data);
   ```

---

## 🔒 Security Best Practices

### 1. Never Hardcode Credentials

❌ **BAD:**
```properties
tenant.tenants.ecsp.notification.email.password=mypassword123
```

✅ **GOOD:**
```properties
tenant.tenants.ecsp.notification.email.password=${SMTP_PASSWORD}
```

```bash
# Set via environment
export SMTP_PASSWORD="your-secure-password"

# Or use Kubernetes secrets, AWS Secrets Manager, etc.
```

### 2. Use App-Specific Passwords

For Gmail/Office365:
- Don't use your regular account password
- Generate app-specific passwords
- Gmail: https://myaccount.google.com/apppasswords
- Office365: https://account.microsoft.com/security

### 3. Validate Template Input

```java
// Sanitize user input before passing to templates
String sanitized = HtmlUtils.htmlEscape(userInput);
templateData.put("userInput", sanitized);
```

### 4. Secure API Endpoints

When using `ignite` provider:
```java
// Configure authentication
@Bean
public RestTemplate notificationRestTemplate() {
    RestTemplate template = new RestTemplate();
    template.getInterceptors().add((request, body, execution) -> {
        request.getHeaders().set("Authorization", "Bearer " + apiToken);
        return execution.execute(request, body);
    });
    return template;
}
```

---

## 🧪 Testing

### Unit Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "tenant.tenants.test.notification.config.resolver=internal",
    "tenant.tenants.test.notification.template.engine=mustache",
    "tenant.tenants.test.notification.email.provider=internal",
    "tenant.tenants.test.notification.email.host=localhost",
    "tenant.tenants.test.notification.email.port=1025"
})
class NotificationTest {
    
    @Autowired
    private NotificationManager notificationManager;
    
    @Test
    void testSendEmail() {
        TenantContext.setCurrentTenant("test");
        
        Map<String, Object> data = Map.of("userName", "Test User");
        notificationManager.sendNotification("TEST_NOTIFICATION", data);
        
        // Verify email sent (check MailHog, or use GreenMail)
    }
}
```

### Integration Testing with GreenMail

```xml
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail-spring</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@ExtendWith(GreenMailExtension.class)
class EmailIntegrationTest {
    
    @Autowired
    private NotificationManager notificationManager;
    
    @Test
    void testEmailDelivery(GreenMail greenMail) {
        greenMail.setUser("test@example.com", "password");
        
        TenantContext.setCurrentTenant("test");
        notificationManager.sendNotification("TEST_NOTIFICATION", data);
        
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        assertEquals("Test Email", messages[0].getSubject());
    }
}
```

---

## 📊 Monitoring & Observability

### Metrics

Add Micrometer metrics to track email sending:

```java
@Component
public class EmailMetrics {
    private final Counter emailsSent;
    private final Timer emailSendDuration;
    
    public EmailMetrics(MeterRegistry registry) {
        this.emailsSent = Counter.builder("emails.sent")
            .tag("provider", "internal")
            .register(registry);
            
        this.emailSendDuration = Timer.builder("emails.send.duration")
            .register(registry);
    }
    
    public void recordEmailSent(String tenant, boolean success) {
        emailsSent.increment(Tags.of("tenant", tenant, "success", String.valueOf(success)));
    }
}
```

### Health Checks

```java
@Component
public class SmtpHealthIndicator implements HealthIndicator {
    
    @Autowired
    private TenantAwareJavaMailSenderFactory mailSenderFactory;
    
    @Override
    public Health health() {
        try {
            JavaMailSender sender = mailSenderFactory.getJavaMailSender("ecsp");
            sender.testConnection(); // If supported
            return Health.up().build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Logging

Enable debug logging for troubleshooting:

```properties
# Application logging
logging.level.org.eclipse.ecsp.uidam.usermanagement.notification=DEBUG

# JavaMail debugging
tenant.tenants.ecsp.notification.email.properties.mail.debug=true
```

---

## 🆘 Troubleshooting Guide

### Problem: Emails Not Sending

**Check:**
1. ✅ SMTP host and port are correct
2. ✅ Credentials are valid
3. ✅ Firewall allows outbound connection on SMTP port
4. ✅ TLS/SSL settings match server requirements

**Debug:**
```bash
# Test SMTP connectivity
telnet smtp.gmail.com 587

# Check application logs
tail -f logs/application.log | grep notification
```

### Problem: Templates Not Found

**Check:**
1. ✅ Template path: `{prefix}{templateName}{suffix}`
2. ✅ File exists at: `src/main/resources{prefix}{templateName}{suffix}`
3. ✅ File is included in build (check `target/classes/`)

**Debug:**
```java
// Add logging to see resolved path
log.debug("Looking for template: {}{}{}", prefix, templateName, suffix);
```

### Problem: Wrong Resolver/Parser Selected

**Check:**
1. ✅ `TenantContext.setCurrentTenant()` was called
2. ✅ Property defined for that specific tenant
3. ✅ Property value is exact (case-sensitive): `internal`, `thymeleaf`, etc.

**Debug:**
```bash
# View all loaded properties
curl http://localhost:8080/actuator/env | jq '.propertySources[].properties | select(.name | contains("notification"))'
```

### Problem: Authentication Failures (Gmail)

**Solution:**
- Use App Passwords, not your regular password
- Enable "Less secure app access" (not recommended)
- Check 2FA settings

**Gmail App Password:**
1. Go to https://myaccount.google.com/security
2. Enable 2-Step Verification
3. Generate App Password
4. Use that password in configuration

### Problem: API Calls Failing (Ignite Provider)

**Check:**
1. ✅ API URL is correct and accessible
2. ✅ Network connectivity (can you ping/curl it?)
3. ✅ Authentication headers are set
4. ✅ API returns expected JSON format

**Debug:**
```java
// Add RestTemplate logging
logging.level.org.springframework.web.client.RestTemplate=DEBUG
```

---

## 📚 Reference

### All Configuration Properties

<details>
<summary><b>Complete Property List</b></summary>

```properties
# Config Resolver
tenant.tenants.{tenantId}.notification.config.resolver=internal|ignite
tenant.tenants.{tenantId}.notification.config.path=classpath:/notification/uidam-notification-config.json
tenant.tenants.{tenantId}.notification.notification-api-url=http://notification-api

# Template Engine  
tenant.tenants.{tenantId}.notification.template.engine=thymeleaf|mustache
tenant.tenants.{tenantId}.notification.template.format=HTML|XML|TEXT
tenant.tenants.{tenantId}.notification.template.resolver=CLASSPATH|FILE|URL
tenant.tenants.{tenantId}.notification.template.prefix=/notification/
tenant.tenants.{tenantId}.notification.template.suffix=.html

# Email Provider
tenant.tenants.{tenantId}.notification.email.provider=internal|ignite

# SMTP (Internal Provider)
tenant.tenants.{tenantId}.notification.email.host=smtp.gmail.com
tenant.tenants.{tenantId}.notification.email.port=587
tenant.tenants.{tenantId}.notification.email.username=user@example.com
tenant.tenants.{tenantId}.notification.email.password=password

# SMTP Advanced
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.auth=true
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.starttls.enabled=true
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.starttls.required=true
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.ssl.enabled=false
tenant.tenants.{tenantId}.notification.email.properties.mail.debug=false
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.connectiontimeout=5000
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.timeout=5000
tenant.tenants.{tenantId}.notification.email.properties.mail.smtp.writetimeout=5000
```
</details>

### Supported Notification IDs

Built-in notification types:
- `UIDAM_USER_VERIFY_ACCOUNT` - Email verification
- `UIDAM_USER_PASSWORD_RESET` - Password reset
- `UIDAM_USER_WELCOME` - Welcome email
- `UIDAM_USER_ACCOUNT_LOCKED` - Account locked notification

### Template Variables

Common variables available in all templates:
- `userName` - User's display name
- `email` - User's email address
- `firstName` - User's first name
- `lastName` - User's last name
- `tenantId` - Current tenant ID
- `baseUrl` - Application base URL

### Related Documentation

- [Thymeleaf Tutorial](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)
- [Mustache Manual](https://mustache.github.io/mustache.5.html)
- [Spring Boot Mail](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [JavaMail API](https://javaee.github.io/javamail/)

### Architecture Diagrams

- `multi-tenant-notification-sequence.puml` - Complete notification flow
- `smtp-class-diagram.puml` - SMTP components structure
- `multi-tenant-complete-class-diagram.puml` - Full system architecture

---

## ❓ FAQ

**Q: Can I use different template engines for different tenants?**  
A: Yes! Each tenant can have its own `notification.template.engine` setting.

**Q: Can I mix internal and ignite providers?**  
A: Yes! For example, use `internal` config resolver with `ignite` email provider.

**Q: How do I add a new tenant?**  
A: Just add properties with the new tenant ID pattern: `tenant.tenants.{newTenantId}.*`

**Q: Can templates be hot-reloaded?**  
A: For Thymeleaf, set `spring.thymeleaf.cache=false` in development. For Mustache, templates are cached by default.

**Q: What if I need custom template variables?**  
A: Pass them in the data map when calling `sendNotification()`.

**Q: Can I send attachments?**  
A: Not directly supported currently. Consider extending `EmailNotificationProvider`.

**Q: How do I test emails without spamming?**  
A: Use MailHog, GreenMail, or similar SMTP mock servers for testing.

---

## 🤝 Contributing

Found an issue or want to improve this guide?

1. Check existing issues: https://github.com/eclipse-ecsp/uidam-user-management/issues
2. Open a new issue or PR
3. Follow the project's contribution guidelines

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---
