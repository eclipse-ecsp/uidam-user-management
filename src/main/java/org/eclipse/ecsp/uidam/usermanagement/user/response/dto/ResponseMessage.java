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

package org.eclipse.ecsp.uidam.usermanagement.user.response.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response message to be received from auth management microservice.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResponseMessage {

    @Schema(description = "key, a string which indicate the result type", example = "success.key")
    private String key;


    @Schema(description = "parameters, data relevant to the message")
    private List<Object> parameters = new ArrayList<>();

    private static final int INITIAL_ODD_NUMBER = 17;
    private static final int MULTIPLIER_ODD_NUMBER = 37;

    public ResponseMessage() {

    }

    public ResponseMessage(String key) {
        this.key = key;
    }

    public ResponseMessage(String key, Object... parameters) {
        this.key = key;
        Collections.addAll(this.parameters, parameters);
    }

    public ResponseMessage(String key, List<Object> parameters) {
        this.key = key;
        this.parameters = parameters;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResponseMessage that = (ResponseMessage) o;

        return new EqualsBuilder()
            .append(key, that.key)
            .append(parameters, that.parameters)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(INITIAL_ODD_NUMBER, MULTIPLIER_ODD_NUMBER)
            .append(key)
            .append(parameters)
            .toHashCode();
    }

    @Override
    public String toString() {
        return "ResponseMessage{"
            + "key='" + key + '\''
            + ", parameters=" + parameters
            + '}';
    }
}
