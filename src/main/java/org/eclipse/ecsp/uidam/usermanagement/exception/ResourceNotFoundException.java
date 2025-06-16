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
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.UNKNOWN;

/**
 * Exception to catch ResourceNotFound error.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
@Getter
public class ResourceNotFoundException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = -2260904370868111455L;
    
    private final String resourceName;
    private final String fieldName;
    private final String fieldValue;

    /**
     * Default construction for ResourceNotFoundException.
     */
    public ResourceNotFoundException() {
        super(String.format("%s not found for %s: %s", UNKNOWN + " resource", UNKNOWN + " field", UNKNOWN + " value"));
        this.resourceName = UNKNOWN + " resource";
        this.fieldName = UNKNOWN + " field";
        this.fieldValue = UNKNOWN + " value";
    }

    /**
     * Parameterized constructor for ResourceNotFoundException.
     *
     * @param resourceName resourceName
     * @param fieldName field of the resource
     * @param fieldValue field value of the resource.
     */
    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found for %s: %s", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldValue = fieldValue;
        this.fieldName = fieldName;
    }
}

