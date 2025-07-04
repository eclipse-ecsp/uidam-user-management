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

package org.eclipse.ecsp.uidam.usermanagement.template;

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.notification.parser.TemplateParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mustache template parser test.
 */
@SpringBootTest(classes = MustacheTemplateParserImplTest.AppConfig.class)
@TestPropertySource(properties = { "template.resolver=file", "template.prefix=./src/test/resources/templates/" },
        locations = "classpath:application-thymeleaf.properties")
@MockBean(AccountRepository.class)
class ThymeleafTemplateParserImplFailureTest {

    /**
     * template parser instance.
     */
    @Autowired
    private TemplateParser templateManager;
    
    @MockBean
    PasswordValidationService passwordValidationService;
    /**
     * password policy service.
     */
    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    /**
     * app test config.
     */
    @Configuration
    @ComponentScan("org.eclipse.ecsp")
    @EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
    public static class AppConfig {}

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void loadFileSuccess() {
        Resource resource = templateManager.getFile("mustache-sample.html");
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void loadFileNotFound() {
        Resource resource = templateManager.getFile("mustache-sample_1.html");
        assertNull(resource);
    }
}
