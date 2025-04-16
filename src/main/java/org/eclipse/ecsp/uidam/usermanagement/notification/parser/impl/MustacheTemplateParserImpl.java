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
import org.eclipse.ecsp.uidam.usermanagement.config.NotificationTemplateConfig;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateManagerException;
import org.eclipse.ecsp.uidam.usermanagement.exception.TemplateNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.TemplateParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;


/**
 * Implementation for {@link TemplateParser} uses Mustache template engine.
 * for more details please refer <a href="https://github.com/spullara/mustache.java/tree/main">Mustache Template Engine</a>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "template.engine", havingValue = "mustache", matchIfMissing = true)
public class MustacheTemplateParserImpl implements TemplateParser {

    private MustacheFactory mustacheFactory;
    private NotificationTemplateConfig templateConfig;
    private ResourceLoader resourceLoader;
    private String resourcePrefix;

    /**
     * Constructor to initialize mustache and validate config.
     *
     * @param templateConfig {@link NotificationTemplateConfig} obj
     */
    public MustacheTemplateParserImpl(NotificationTemplateConfig templateConfig, ResourceLoader resourceLoader) {
        this.templateConfig = templateConfig;
        Objects.requireNonNull(templateConfig.getResolver(), "template resolver should not be null");
        Objects.requireNonNull(templateConfig.getFormat(), "template format should not be null");
        this.resourceLoader = resourceLoader;
        MustacheResolver resolver = null;
        switch (templateConfig.getResolver()) {
            case CLASSPATH -> {
                String templatePrefix = templateConfig.getPrefix();
                this.resourcePrefix = "classpath:";
                if (StringUtils.isNotEmpty(templatePrefix) && templatePrefix.startsWith("/")) {
                    templatePrefix = templatePrefix.substring(1);
                }
                resolver = new ClasspathResolver(templatePrefix);
            }
            case FILE -> {
                resourcePrefix = "file:";
                resolver = new FileSystemResolver(Path.of(templateConfig.getPrefix()));
            }
            case URL ->
                resolver = new URIResolver();
            default -> log.info("no template resolver is configured..");
        }
        if (StringUtils.isNotEmpty(templateConfig.getPrefix())) {
            resourcePrefix += templateConfig.getPrefix();
        }
        mustacheFactory = new DefaultMustacheFactory(resolver);
    }

    @Override
    public String parseText(String template, Map<String, Object> placeholderValues) {
        try {
            log.debug("[mustache]: text processing started");
            StringWriter stringWriter = new StringWriter();
            Mustache mustache = this.mustacheFactory.compile(new StringReader(template), "string-template");
            mustache.execute(stringWriter, placeholderValues);
            log.debug("[mustache]: text processing completed");
            return stringWriter.toString();
        } catch (Exception e) {
            log.error("[thymeleaf]: Unknown error occurred while processing template {}", template, e);
            throw new TemplateManagerException(
                    String.format("An unknown error happened during template parsing , Template : %s", template), e);
        }
    }

    @Override
    public String parseTemplate(String template, Map<String, Object> placeholderValues) {
        try {
            log.debug("[mustache]: template processing started");
            if (null != templateConfig.getSuffix() && !template.contains(this.templateConfig.getSuffix())) {
                template = template + "." + this.templateConfig.getSuffix();
            }
            Mustache mustache = mustacheFactory.compile(template);
            StringWriter stringWriter = new StringWriter();
            mustache.execute(stringWriter, placeholderValues);
            log.debug("[mustache]: template processing completed");
            return stringWriter.toString();
        } catch (MustacheNotFoundException e) {
            log.error("[Mustache] Template {} not found", template);
            throw new TemplateNotFoundException(String.format("Template : %s not found", template), e);
        } catch (Exception e) {
            log.error("[thymeleaf]: Unknown error occurred while processing template {}", template, e);
            throw new TemplateManagerException(
                    String.format("An unknown error happened during template parsing , Template : %s", template), e);
        }
    }

    @Override
    public Resource getFile(String path) {
        path = null != resourcePrefix ? resourcePrefix + path : path;
        Resource resource = resourceLoader.getResource(path);
        if (resource.exists()) {
            return resource;
        }
        return null;
    }
}
