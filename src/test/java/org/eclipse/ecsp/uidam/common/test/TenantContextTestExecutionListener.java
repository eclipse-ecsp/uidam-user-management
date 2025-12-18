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

package org.eclipse.ecsp.uidam.common.test;

import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test execution listener that sets up tenant context before test execution.
 * This is needed for integration tests that load Spring context with multitenant datasources.
 */
public class TenantContextTestExecutionListener extends AbstractTestExecutionListener {

    private static final String DEFAULT_TEST_TENANT = "ecsp";
    private static final int DEFAULT_ORDER = 1000;

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
        // Set tenant context before Spring context is loaded
        TenantContext.setCurrentTenant(DEFAULT_TEST_TENANT);
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        // Ensure tenant context is set before each test method
        if (!TenantContext.hasTenant()) {
            TenantContext.setCurrentTenant(DEFAULT_TEST_TENANT);
        }
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        // Clear tenant context after each test method
        TenantContext.clear();
    }

    @Override
    public int getOrder() {
        // Execute before other listeners (like DependencyInjectionTestExecutionListener)
        return DEFAULT_ORDER;
    }
}
