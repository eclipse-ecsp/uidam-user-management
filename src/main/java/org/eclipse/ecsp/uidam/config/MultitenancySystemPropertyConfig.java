/*
 *
 *   ******************************************************************************
 *
 *    Copyright (c) 2023-24 Harman International
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *
 *    you may not use this file except in compliance with the License.
 *
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *    See the License for the specific language governing permissions and
 *
 *    limitations under the License.
 *
 *    SPDX-License-Identifier: Apache-2.0
 *
 *    *******************************************************************************
 *
 */

package org.eclipse.ecsp.uidam.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Environment post-processor to bridge Spring application properties to System properties
 * for multitenancy support.
 * This is necessary because the sql-dao library reads multitenancy.enabled from
 * System properties, while the application defines it in application.properties.
 * This class implements EnvironmentPostProcessor to ensure it runs very early in the
 * Spring Boot lifecycle, before any @Configuration classes are loaded, including
 * database configurations from the sql-dao library.
 * Note: This class must be registered in META-INF/spring.factories to be discovered
 * by Spring Boot.
 */
@Configuration
public class MultitenancySystemPropertyConfig implements EnvironmentPostProcessor {

    private static final String MULTITENANCY_ENABLED_PROPERTY = "multitenancy.enabled";
    private static final String DEFAULT_VALUE = "false";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Read the property from Spring Environment (application.properties, etc.)
        String multitenancyEnabled = environment.getProperty(MULTITENANCY_ENABLED_PROPERTY, DEFAULT_VALUE);
        
        // Set it as a System property so sql-dao library can read it
        System.setProperty(MULTITENANCY_ENABLED_PROPERTY, multitenancyEnabled);
        
        // Log the initialization (optional, but helpful for debugging)
        System.out.println("MultitenancySystemPropertyConfig: Set system property " 
            + MULTITENANCY_ENABLED_PROPERTY + "=" + multitenancyEnabled);
    }
}
