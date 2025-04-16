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
 * ENUM containing successful response messages for client registration process.
 */
public enum ClientRegistrationResponseMessage {

    SP_REGISTRATION_SUCCESS_201_MSG("Client registered successfully!!"),
    SP_REGISTRATION_UPDATE_SUCCESS_200_MSG("Client updated successfully!!"),
    SP_REGISTRATION_RETRIEVE_SUCCESS_200_MSG("Client details retrieved successfully."),
    SP_REGISTRATION_DELETE_SUCCESS_200_MSG("Client deleted successfully!!");

    private String message;

    ClientRegistrationResponseMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
