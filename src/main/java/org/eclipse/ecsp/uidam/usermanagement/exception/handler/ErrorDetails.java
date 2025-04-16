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

package org.eclipse.ecsp.uidam.usermanagement.exception.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;



/**
 * Custom Error Details pojo to be returned in reponse if status is other than 200.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ErrorDetails {
    private String status;
    private String code;
    private String message;
    private Integer quantity;
    private String requestProperty;
    private List<String> parameters = new ArrayList<>();

    /**
     * Error details constructor.
     *
     * @param status HttpStatus
     * @param code ErrorCode
     * @param message ErrorMessage
     */
    public ErrorDetails(String status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /**
     * Error details constructor.
     *
     * @param status HttpStatus
     * @param code ErrorCode
     * @param message ErrorMessage
     * @param parameters Error params
     */
    public ErrorDetails(String status, String code, String message, List<String> parameters) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ErrorDetails{");
        stringBuilder.append("status='" + status + '\'');
        stringBuilder.append(", code='" + code + '\'');
        stringBuilder.append(", message='" + message + '\'');
        if (!parameters.isEmpty()) {
            stringBuilder.append(", parameters=" + parameters);
        }
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
