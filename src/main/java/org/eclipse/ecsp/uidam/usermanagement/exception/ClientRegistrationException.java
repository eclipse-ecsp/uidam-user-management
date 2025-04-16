
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

import org.eclipse.ecsp.uidam.usermanagement.enums.ClientRegistrationResponseCode;

/**
 * This class sets id and status code for any client registration exception in
 * response.
 */
public class ClientRegistrationException extends RuntimeException {

    private static final long serialVersionUID = -6657118131457296626L;

    private final String id;
    private final int statusCode;

    /**
     * Constructor to set ClientRegistrationException.
     *
     * @param sp enum with status code and message
     */
    public ClientRegistrationException(ClientRegistrationResponseCode sp) {
        super(sp.getMessage());
        this.id = sp.getCode();
        this.statusCode = sp.getHttpStatus();
    }

    public String getId() {
        return id;
    }

    public int getStatusCode() {
        return statusCode;
    }

}
