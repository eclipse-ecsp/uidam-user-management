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

package org.eclipse.ecsp.uidam;

import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.config.BaseApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * UidamUserManagementApplication main microservice class.
 */
@SpringBootApplication(scanBasePackages = {"org.eclipse.ecsp"})
@EnableConfigurationProperties({ApplicationProperties.class})
@EnableJpaAuditing
public class UidamUserManagementApplication extends BaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(UidamUserManagementApplication.class, args);
    }

}