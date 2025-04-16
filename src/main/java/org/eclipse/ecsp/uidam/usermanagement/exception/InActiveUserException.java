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

package org.eclipse.ecsp.uidam.usermanagement.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom Exception is thrown when crud operations are performed for non active users.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
@Getter
public class InActiveUserException extends Exception {
    private final String errorCode;
    private final String message;

    /**
     * Constructor for InActiveUserException.
     *
     * @param message Exception message
     * @param errorCode Error Code
     */
    public InActiveUserException(String message, String errorCode) {
        super(String.format("%s", message));
        this.errorCode = errorCode;
        this.message = message;
    }
}