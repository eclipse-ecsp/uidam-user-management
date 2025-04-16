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

package org.eclipse.ecsp.uidam.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to hold the config value for template managers.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "template")
@ConfigurationPropertiesScan
public class NotificationTemplateConfig {
    /**
     * Resolver for template file lookup, default CLASSPATH.
     */
    private Resolver resolver = Resolver.CLASSPATH;
    /**
     * Template format, default HTML.
     */
    private Format format = Format.HTML;
    /**
     * path of the template file if it is classpath, file or url, default /templates.
     */
    private String prefix = "/templates";

    /**
     * path of the template file it is classpath, file or url, default .html.
     */
    private String suffix = ".html";
    /**
     * template encoding, default is UTF-8.
     */
    private String encoding = "UTF-8";

    /**
     * Resolver will indicate the location from which the template should be loaded.
     */
    public enum Resolver {
        CLASSPATH, FILE, URL
    }

    /**
     * Format will indicate the template format.
     */
    public enum Format {
        TEXT, HTML, XML
    }
}
