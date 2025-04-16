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
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationTemplateConfig.Format;
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationTemplateConfig.Resolver;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateManagerException;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateProcessingException;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.TemplateParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * thymeleaf template engine to parse notification templates.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "template.engine", havingValue = "thymeleaf")
public class ThymeleafTemplateParserImpl implements TemplateParser {
    public static final String TEXT = "TEXT";
    public static final String CUSTOM = "CUSTOM";
    private ResourceLoader resourceLoader;
    protected Map<String, TemplateEngine> templateEngines = new HashMap<>();
    private String prefix;

    /**
     * Initialize thymeleaf template engines.
     *
     * @param templateConfig template configuration
     * @param resourceLoader resource load for loading files
     */
    public ThymeleafTemplateParserImpl(NotificationTemplateConfig templateConfig, ResourceLoader resourceLoader) {
        log.info("Initializing ThymeleafTemplateManager");
        Objects.requireNonNull(templateConfig.getResolver(), "template resolver should not be null");
        Objects.requireNonNull(templateConfig.getFormat(), "template format should not be null");
        this.resourceLoader = resourceLoader;
        TemplateEngine templateEngine = new TemplateEngine();
        StringTemplateResolver stringTemplateResolver = new StringTemplateResolver();
        stringTemplateResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(stringTemplateResolver);
        templateEngines.put(TEXT, templateEngine);

        AbstractConfigurableTemplateResolver templateResolver = getTemplateResolver(templateConfig);

        if (null != templateResolver) {
            TemplateEngine templateEngineWithResolver = new TemplateEngine();
            templateEngineWithResolver.setTemplateResolver(templateResolver);
            setSuffixAndPrefix(templateResolver, templateConfig);
            templateResolver.setCharacterEncoding(templateConfig.getEncoding());
            if (!templateConfig.getPrefix().isEmpty()) {
                templateResolver.setPrefix(templateConfig.getPrefix());
                if (prefix != null) {
                    prefix += templateConfig.getPrefix();
                }
            }

            templateEngines.put(CUSTOM, templateEngineWithResolver);
        }
        log.info("Initialization ThymeleafTemplateManager completed");
    }

    private AbstractConfigurableTemplateResolver getTemplateResolver(NotificationTemplateConfig templateConfig) {
        AbstractConfigurableTemplateResolver templateResolver = null;
        if (Resolver.FILE.equals(templateConfig.getResolver())) {
            templateResolver = new FileTemplateResolver();
            prefix = "file:";
        }

        if (Resolver.CLASSPATH.equals(templateConfig.getResolver())) {
            templateResolver = new ClassLoaderTemplateResolver();
            prefix = "classpath:";
        }

        if (Resolver.URL.equals(templateConfig.getResolver())) {
            templateResolver = new UrlTemplateResolver();
        }
        return templateResolver;
    }

    private void setSuffixAndPrefix(AbstractConfigurableTemplateResolver templateResolver,
                                    NotificationTemplateConfig templateConfig) {
        if (Format.HTML.equals(templateConfig.getFormat())) {
            templateResolver.setTemplateMode(TemplateMode.HTML);
            if (null != templateConfig.getSuffix()) {
                templateResolver.setSuffix("." + templateConfig.getSuffix());
            } else {
                templateResolver.setSuffix(".html");
            }
        } else if (Format.XML.equals(templateConfig.getFormat())) {
            templateResolver.setTemplateMode(TemplateMode.XML);
            if (null != templateConfig.getSuffix()) {
                templateResolver.setSuffix("." + templateConfig.getSuffix());
            } else {
                templateResolver.setSuffix(".html");
            }
        } else if (Format.TEXT.equals(templateConfig.getFormat())) {
            templateResolver.setTemplateMode(TemplateMode.TEXT);
            if (null != templateConfig.getSuffix()) {
                templateResolver.setSuffix("." + templateConfig.getSuffix());
            } else {
                templateResolver.setSuffix(".txt");
            }
        } else {
            throw new IllegalArgumentException("Template format "
                    + templateConfig.getFormat() + " is not support with resolver "
                    + templateConfig.getResolver());
        }
    }

    @Override
    public String parseText(String template, Map<String, Object> placeholderValues) {
        return parseTemplateUsingProvidedEngine(templateEngines.get(TEXT), template, placeholderValues).trim();
    }

    @Override
    public String parseTemplate(String template, Map<String, Object> placeholderValues) {
        return parseTemplateUsingProvidedEngine(templateEngines.get(CUSTOM), template, placeholderValues);
    }

    @Override
    public Resource getFile(String path) {
        path = null != prefix ? prefix + path : path;
        Resource resource = resourceLoader.getResource(path);
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
