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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID_UI;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STRING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TENANT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TENANT_ID_UI;

/**
 * Combined swagger parameter annotations to apply over a REST method.
 */
@Parameter(name = CORRELATION_ID, description = CORRELATION_ID_UI, schema = @Schema(
    type = STRING
), in = ParameterIn.HEADER)
@Parameter(name = TENANT_ID, description = TENANT_ID_UI, required = false, schema = @Schema(
    type = STRING
), in = ParameterIn.HEADER)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommonParameters {
}
