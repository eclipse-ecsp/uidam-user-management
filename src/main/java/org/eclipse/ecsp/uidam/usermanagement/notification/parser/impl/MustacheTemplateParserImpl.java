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


import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheNotFoundException;
import com.github.mustachejava.MustacheResolver;
import com.github.mustachejava.resolver.ClasspathResolver;
import com.github.mustachejava.resolver.FileSystemResolver;
import com.github.mustachejava.resolver.URIResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.NotificationProperties.TemplateEngineProperties;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateManagerException;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.TemplateParser;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Implementation for {@link TemplateParser} uses Mustache template engine with tenant-specific configuration.
 * for more details please refer <a href="https://github.com/spullara/mustache.java/tree/main">Mustache Template Engine</a>
 */
@Slf4j
@Component
public class MustacheTemplateParserImpl implements TemplateParser {

    private final ResourceLoader resourceLoader;
    private final TenantConfigurationService tenantConfigurationService;
    
    // Cache of tenant-specific mustache factories: key = tenantId, value = MustacheFactory
    private final Map<String, MustacheFactory> tenantMustacheFactories = new ConcurrentHashMap<>();

    /**
     * Constructor to initialize mustache parser with tenant configuration service.
     *
     * @param tenantConfigurationService service to get tenant-specific configuration
     * @param resourceLoader resource loader for loading files
     */
    public MustacheTemplateParserImpl(TenantConfigurationService tenantConfigurationService, 
                                      ResourceLoader resourceLoader) {
        log.info("Initializing MustacheTemplateParser with tenant-specific configuration support");
        this.tenantConfigurationService = tenantConfigurationService;
        this.resourceLoader = resourceLoader;
        log.info("MustacheTemplateParser initialization completed");
    }

    /**
     * Get or create mustache factory for the current tenant.
     *
     * @param tenantId tenant identifier
     * @return mustache factory for the tenant
     */
    private MustacheFactory getOrCreateMustacheFactory(String tenantId) {
        return tenantMustacheFactories.computeIfAbsent(tenantId, tid -> {
            log.info("Creating Mustache factory for tenant '{}'", tid);
            
            // Get tenant-specific template configuration
            TemplateEngineProperties templateConfig = tenantConfigurationService
                    .getTenantProperties().getNotification().getTemplate();
            
            MustacheResolver resolver = null;
            String resolverType = templateConfig.getResolver();
            String prefix = templateConfig.getPrefix();
            
            if ("CLASSPATH".equalsIgnoreCase(resolverType)) {
                String templatePrefix = prefix;
                if (StringUtils.isNotEmpty(templatePrefix) && templatePrefix.startsWith("/")) {
                    templatePrefix = templatePrefix.substring(1);
                }
                resolver = new ClasspathResolver(templatePrefix);
                log.debug("Created ClasspathResolver for tenant '{}' with prefix: {}", tid, templatePrefix);
            } else if ("FILE".equalsIgnoreCase(resolverType)) {
                resolver = new FileSystemResolver(Path.of(prefix != null ? prefix : "."));
                log.debug("Created FileSystemResolver for tenant '{}' with prefix: {}", tid, prefix);
            } else if ("URL".equalsIgnoreCase(resolverType)) {
                resolver = new URIResolver();
                log.debug("Created URIResolver for tenant '{}'", tid);
            } else {
                log.warn("No template resolver configured for tenant '{}', using default", tid);
            }
            
            return new DefaultMustacheFactory(resolver);
        });
    }

    @Override
    public String parseText(String template, Map<String, Object> placeholderValues) {
        try {
            log.debug("[mustache]: text processing started");
            String tenantId = TenantContext.getCurrentTenant();
            MustacheFactory factory = getOrCreateMustacheFactory(tenantId);
            
            StringWriter stringWriter = new StringWriter();
            Mustache mustache = factory.compile(new StringReader(template), "string-template");
            mustache.execute(stringWriter, placeholderValues);
            log.debug("[mustache]: text processing completed");
            return stringWriter.toString();
        } catch (Exception e) {
            log.error("[mustache]: Unknown error occurred while processing template {}", template, e);
            throw new TemplateManagerException(
                    String.format("An unknown error happened during template parsing , Template : %s", template), e);
        }
    }

    @Override
    public String parseTemplate(String template, Map<String, Object> placeholderValues) {
        try {
            log.debug("[mustache]: template processing started");
            String tenantId = TenantContext.getCurrentTenant();
            MustacheFactory factory = getOrCreateMustacheFactory(tenantId);
            
            // Get tenant-specific template configuration
            TemplateEngineProperties templateConfig = tenantConfigurationService
                    .getTenantProperties().getNotification().getTemplate();
            
            // Add suffix if configured and not already present
            String templateName = template;
            String suffix = templateConfig.getSuffix();
            if (StringUtils.isNotEmpty(suffix) && !template.contains(suffix)) {
                templateName = template + suffix;
            }
            
            Mustache mustache = factory.compile(templateName);
            StringWriter stringWriter = new StringWriter();
            mustache.execute(stringWriter, placeholderValues);
            log.debug("[mustache]: template processing completed");
            return stringWriter.toString();
        } catch (MustacheNotFoundException e) {
            log.error("[Mustache] Template {} not found", template, e);
            throw new TemplateNotFoundException(String.format("Template : %s not found", template), e);
        } catch (Exception e) {
            log.error("[mustache]: Unknown error occurred while processing template {}", template, e);
            throw new TemplateManagerException(
                    String.format("An unknown error happened during template parsing , Template : %s", template), e);
        }
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
}
