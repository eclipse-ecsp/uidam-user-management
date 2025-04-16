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

package org.eclipse.ecsp.uidam.accountmanagement.exception;

import org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.exception.handler.ErrorProperty;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserNotFoundExceptionTest {

    @Test
    void testConstructorWithKeyAndHttpStatusAndValues() {
        String key = "testKey";
        String message = "Account already exist";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        ErrorProperty property = new ErrorProperty();
        property.setKey(ACCOUNT_ID);
        property.setValues(Arrays.asList(key));

        AccountManagementException exception = new AccountManagementException(
                AccountApiConstants.ACCOUNT_DOES_NOT_EXIST, message, Arrays.asList(property), httpStatus);

        assertEquals(AccountApiConstants.ACCOUNT_DOES_NOT_EXIST, exception.getCode());
        assertEquals(ACCOUNT_ID, exception.getProperty().get(0).getKey());
        assertEquals(key, exception.getProperty().get(0).getValues().get(0));
        assertEquals(httpStatus, exception.getHttpStatus());
    }

}