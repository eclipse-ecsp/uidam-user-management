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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.http.HttpStatus;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Custom exception created for UIDAM User Management Microservice rest apis.
 */
public class ApplicationRuntimeException extends RuntimeException implements Serializable {

    private final HttpStatus httpStatus;
    private final String key;
    private final String[] parameters;

    private static final int INITIAL_ODD_NUMBER = 17;
    private static final int MULTIPLIER_ODD_NUMBER = 37;

    /**
     * ApplicationRuntimeException parameterized constructor having key as exception key, status and params.
     *
     * @param key        exception description
     * @param httpStatus status to be returned on exception
     * @param values     params causing exception
     */
    public ApplicationRuntimeException(String key, HttpStatus httpStatus, String... values) {
        super("{ Error ='" + key + '\''
            + ", parameters=" + Arrays.stream(values).toList()
            + " }");
        this.key = key;
        this.httpStatus = httpStatus;
        this.parameters = values;
    }

    /**
     * ApplicationRuntimeException parameterized constructor having key and exception.
     *
     * @param key   exception description
     * @param cause exception
     */
    public ApplicationRuntimeException(String key, Throwable cause) {
        super(key, cause);
        this.httpStatus = null;
        this.key = key;
        this.parameters = null;
    }

    /**
     * ApplicationRuntimeException parameterized constructor having key as exception key and values.
     *
     * @param key    exception description
     * @param values params causing exception
     */
    public ApplicationRuntimeException(String key, String... values) {
        this(key, null, values);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getKey() {
        return key;
    }

    public String[] getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ApplicationRuntimeException that = (ApplicationRuntimeException) o;

        return new EqualsBuilder()
            .append(httpStatus, that.httpStatus)
            .append(key, that.key)
            .append(parameters, that.parameters)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(INITIAL_ODD_NUMBER, MULTIPLIER_ODD_NUMBER)
            .append(httpStatus)
            .append(key)
            .append(parameters)
            .toHashCode();
    }
}
