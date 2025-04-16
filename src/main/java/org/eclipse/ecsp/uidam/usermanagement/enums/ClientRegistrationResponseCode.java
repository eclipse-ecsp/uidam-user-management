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

package org.eclipse.ecsp.uidam.usermanagement.enums;

/**
 * ENUM contains unique code, message and http status code for client
 * registration api.
 */
public enum ClientRegistrationResponseCode {

    SP_SUCCESS("sp-000", "SUCCESS", 200), SP_CREATED("sp-001", "CREATED", 201),
    SP_CLIENT_ALREADY_EXIST("sp-002", "client id already exist!", 400),
    SP_CLIENT_DOES_NOT_EXIST("sp-003", "client does not exists!", 404),
    SP_CLIENT_ID_MISSING("sp-004", "client id is missing in the request!", 400),
    SP_CLIENT_NAME_MISSING("sp-005", "client name is missing in the request!", 400),
    SP_CLIENT_SECRET_MISSING("sp-006", "client secret is missing in the request!", 400),
    SP_CLIENT_SCOPE_MISSING("sp-007", "client scopes are missing in the request!", 400),
    SP_REDIRECT_URI_MISSING("sp-008", "redirectUris cannot be empty", 400),
    SP_CLIENT_STATUS_NOT_SUPPORTED("sp-009", "status is not supported", 400);

    String code;
    String message;
    int statusCode;

    ClientRegistrationResponseCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return statusCode;
    }

}
