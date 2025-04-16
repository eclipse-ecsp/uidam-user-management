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

package org.eclipse.ecsp.uidam.security.policy.exception;

import lombok.Getter;
import org.eclipse.ecsp.uidam.usermanagement.exception.handler.ErrorProperty;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Custom exception for password policy-related validation errors.
 */
public class PasswordPolicyException extends RuntimeException {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 7388597421194909519L;

    @Getter
    private final HttpStatus httpStatus;
    @Getter
    private final List<ErrorProperty> property;
    @Getter
    private final String code;

    /**
     * PasswordPolicyException.
     *
     * @param code Error code.
     * @param message error message.
     * @param property error fields.
     * @param httpStatus error code.
     */
    public PasswordPolicyException(String code, String message, List<ErrorProperty> property, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.property = property;
        this.httpStatus = httpStatus;

    }
}
