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

package org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Database configuration properties for each tenant.
 * Each tenant has its own separate database for complete isolation.
 */
@Getter
@Setter
public class DatabaseProperties {
    
    private String url;
    private String username;
    private String password;
    private String driverClassName = "org.postgresql.Driver";
    private Integer maxPoolSize = 30;
    private Integer maxIdleTime = 0;
    private Integer connectionTimeoutMs = 60000;
    private String defaultSchema = "uidam";
    private String poolName = "hikariConnectionPool";
    private Integer expected99thPercentileMs = 60000;
    private Integer retryCount = 3;
    private Integer retryDelayMs = 30;
    
    // Data source properties for connection optimization
    private Boolean cachePrepStmts = true;
    private Integer prepStmtCacheSize = 250;
    private Integer prepStmtCacheSqlLimit = 2048;
}
