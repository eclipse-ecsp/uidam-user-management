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

package org.eclipse.ecsp.uidam.usermanagement.notification.parser.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties.TemplateEngineProperties;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateManagerException;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateProcessingException;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.TemplateParser;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresolver.UrlTemplateResolver;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * thymeleaf template engine to parse notification templates with tenant-specific configuration.
 */
@Slf4j
@Component
public class ThymeleafTemplateParserImpl implements TemplateParser {
    public static final String TEXT = "TEXT";
    public static final String CUSTOM = "CUSTOM";
    
    private final ResourceLoader resourceLoader;
    private final TenantConfigurationService tenantConfigurationService;
    private final TemplateEngine textTemplateEngine;
    
    // Cache of tenant-specific template engines: key = tenantId, value = Map<engineType, TemplateEngine>
    private final Map<String, Map<String, TemplateEngine>> tenantTemplateEngines = new ConcurrentHashMap<>();

    /**
     * Initialize thymeleaf template parser with tenant configuration service.
     *
     * @param tenantConfigurationService service to get tenant-specific configuration
     * @param resourceLoader resource loader for loading files
     */
    public ThymeleafTemplateParserImpl(TenantConfigurationService tenantConfigurationService, 
                                       ResourceLoader resourceLoader) {
        log.info("Initializing ThymeleafTemplateParser with tenant-specific configuration support");
        this.tenantConfigurationService = tenantConfigurationService;
        this.resourceLoader = resourceLoader;
        
        // Initialize the text template engine (shared across all tenants for inline text parsing)
        this.textTemplateEngine = new TemplateEngine();
        StringTemplateResolver stringTemplateResolver = new StringTemplateResolver();
        stringTemplateResolver.setTemplateMode(TemplateMode.HTML);
        this.textTemplateEngine.setTemplateResolver(stringTemplateResolver);
        
        log.info("ThymeleafTemplateParser initialization completed");
    }

    /**
     * Get or create template engines for the current tenant.
     *
     * @param tenantId tenant identifier
     * @return map of template engines for the tenant
     */
    private Map<String, TemplateEngine> getOrCreateTemplateEngines(String tenantId) {
        return tenantTemplateEngines.computeIfAbsent(tenantId, tid -> {
            log.info("Creating Thymeleaf template engines for tenant '{}'", tid);
            Map<String, TemplateEngine> engines = new ConcurrentHashMap<>();
            
            // Get tenant-specific template configuration
            TemplateEngineProperties templateConfig = tenantConfigurationService
                    .getTenantProperties().getNotification().getTemplate();
            
            // TEXT engine is shared
            engines.put(TEXT, textTemplateEngine);
            
            // Create custom template engine with tenant-specific configuration
            AbstractConfigurableTemplateResolver templateResolver = getTemplateResolver(templateConfig);
            if (null != templateResolver) {
                TemplateEngine customEngine = new TemplateEngine();
                customEngine.setTemplateResolver(templateResolver);
                setSuffixAndPrefix(templateResolver, templateConfig);
                
                // Set character encoding (always UTF-8)
                templateResolver.setCharacterEncoding("UTF-8");
                
                // Set prefix
                String prefix = templateConfig.getPrefix();
                if (prefix != null && !prefix.isEmpty()) {
                    templateResolver.setPrefix(prefix);
                }
                
                engines.put(CUSTOM, customEngine);
                log.debug("Created custom Thymeleaf engine for tenant '{}' with resolver: {}, format: {}, prefix: {}", 
                         tid, templateConfig.getResolver(), templateConfig.getFormat(), prefix);
            }
            
            return engines;
        });
    }

    private AbstractConfigurableTemplateResolver getTemplateResolver(TemplateEngineProperties templateConfig) {
        AbstractConfigurableTemplateResolver templateResolver = null;
        String resolver = templateConfig.getResolver();
        
        if ("FILE".equalsIgnoreCase(resolver)) {
            templateResolver = new FileTemplateResolver();
        } else if ("CLASSPATH".equalsIgnoreCase(resolver)) {
            templateResolver = new ClassLoaderTemplateResolver();
        } else if ("URL".equalsIgnoreCase(resolver)) {
            templateResolver = new UrlTemplateResolver();
        }
        
        return templateResolver;
    }

    private void setSuffixAndPrefix(AbstractConfigurableTemplateResolver templateResolver,
                                    TemplateEngineProperties templateConfig) {
        String format = templateConfig.getFormat();
        String suffix = templateConfig.getSuffix();
        
        if ("HTML".equalsIgnoreCase(format)) {
            templateResolver.setTemplateMode(TemplateMode.HTML);
            templateResolver.setSuffix((suffix != null && !suffix.isEmpty()) ? suffix : ".html");
        } else if ("XML".equalsIgnoreCase(format)) {
            templateResolver.setTemplateMode(TemplateMode.XML);
            templateResolver.setSuffix((suffix != null && !suffix.isEmpty()) ? suffix : ".xml");
        } else if ("TEXT".equalsIgnoreCase(format)) {
            templateResolver.setTemplateMode(TemplateMode.TEXT);
            templateResolver.setSuffix((suffix != null && !suffix.isEmpty()) ? suffix : ".txt");
        } else {
            throw new IllegalArgumentException("Template format " + format 
                    + " is not supported with resolver " + templateConfig.getResolver());
        }
    }

    @Override
    public String parseText(String template, Map<String, Object> placeholderValues) {
        // Text parsing uses the shared text template engine
        return parseTemplateUsingProvidedEngine(textTemplateEngine, template, placeholderValues).trim();
    }

    @Override
    public String parseTemplate(String template, Map<String, Object> placeholderValues) {
        // Get tenant-specific template engines
        String tenantId = TenantContext.getCurrentTenant();
        Map<String, TemplateEngine> engines = getOrCreateTemplateEngines(tenantId);
        TemplateEngine customEngine = engines.get(CUSTOM);
        
        if (customEngine == null) {
            throw new TemplateManagerException("Custom template engine not configured for tenant: " + tenantId, null);
        }
        
        return parseTemplateUsingProvidedEngine(customEngine, template, placeholderValues);
    }

    @Override
    public Resource getFile(String path) {
        TemplateEngineProperties templateConfig = tenantConfigurationService
                .getTenantProperties().getNotification().getTemplate();
        
        String resolver = templateConfig.getResolver();
        String prefix = templateConfig.getPrefix();
        
        // Build resource path
        String resourcePath = path;
        if ("FILE".equalsIgnoreCase(resolver)) {
            resourcePath = "file:" + (prefix != null ? prefix : "") + path;
        } else if ("CLASSPATH".equalsIgnoreCase(resolver)) {
            resourcePath = "classpath:" + (prefix != null ? prefix : "") + path;
        }
        
        Resource resource = resourceLoader.getResource(resourcePath);
        if (resource.exists()) {
            return resource;
        }
        return null;
    }

    private String parseTemplateUsingProvidedEngine(TemplateEngine templateEngine,
                                                    String template,
                                                    Map<String, Object> placeholderValues) {
        String processedTemplate = "";
        try {
            log.debug("[thymeleaf]: template processing started");
            Context context = new Context();
            context.setVariables(placeholderValues);
            processedTemplate = templateEngine.process(template, context);
            log.debug("[thymeleaf]: template processing completed");
        } catch (TemplateInputException e) {
            log.error("[thymeleaf]: Error occurred while processing template , {}", template, e);
            if (e.getCause() instanceof FileNotFoundException) {
                log.error("[thymeleaf]: template {}, not found", template, e);
                throw new TemplateNotFoundException(String.format("Template : %s not found", template), e);
            }
            throw new TemplateManagerException(
                    String.format("An error happened during template parsing , Template : %s", template), e);
        } catch (org.thymeleaf.exceptions.TemplateProcessingException e) {
            log.error("[thymeleaf]: Error occurred while processing template {}", template, e);
            throw new TemplateProcessingException(
                    String.format("An error happened during template parsing, Template : %s", template), e);
        } catch (Exception e) {
            log.error("[thymeleaf]: Unknown error occurred while processing template {}", template, e);
            throw new TemplateManagerException(
                    String.format("An unknown error happened during template parsing , Template : %s", template), e);
        }
        return processedTemplate;
    }
}
