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

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientRegistrationResponseCode;
import org.eclipse.ecsp.uidam.usermanagement.enums.ClientStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ClientRegistrationException;
import java.util.Arrays;
import java.util.Optional;

/**
 * This class validates client registration details.
 */
public class ValidationUtils {
    private static final String AUTHORIZATION_CODE = "authorization_code";

    private ValidationUtils() {

    }

    /**
     * This method is used to validate client registration request.
     *
     * @exception ClientRegistrationException if any validation fails then throw
     *                                        exception
     **/
    public static void validateClientRegistrationRequest(RegisteredClientDetails request) {
        if (!Optional.ofNullable(request.getClientId()).isPresent()) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_ID_MISSING);
        }
        if (!Optional.ofNullable(request.getClientName()).isPresent()) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_NAME_MISSING);
        }
        if (!Optional.ofNullable(request.getClientSecret()).isPresent()) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_SECRET_MISSING);
        }
        if (!Optional.ofNullable(request.getScopes()).isPresent()) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_CLIENT_SCOPE_MISSING);
        }
        validateUris(request);
    }

    /**
     * This method is used to validate client redirect uris.
     *
     * @exception ClientRegistrationException if uri is not correct throw this
     *                                        exception
     **/
    public static void validateUris(RegisteredClientDetails request) {
        if (Optional.ofNullable(request.getAuthorizationGrantTypes()).isPresent()
            && request.getAuthorizationGrantTypes().contains(AUTHORIZATION_CODE)
            && Optional.ofNullable(request.getRedirectUris()).isEmpty()) {
            throw new ClientRegistrationException(ClientRegistrationResponseCode.SP_REDIRECT_URI_MISSING);
        }
    }

    /**
     * This method is used to validate client status.
     *
     * @exception ClientRegistrationException if status is not valid then throw this
     *                                        exception
     **/
    public static void validateStatus(String status) {
        Arrays.stream(ClientStatus.values()).filter(val -> val.name().equals(status.toUpperCase())).findFirst()
                .orElseThrow(() -> new ClientRegistrationException(
                        ClientRegistrationResponseCode.SP_CLIENT_STATUS_NOT_SUPPORTED));
    }

}
