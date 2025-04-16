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

package org.eclipse.ecsp.uidam.usermanagement.validations.password.policy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;

import java.util.Map;

/**
 * This class is used to validate password against userName.
 *
 */
public class ValidatePasswordAgainstUserName extends AbstractPasswordValidationHandler {
    private static final Log LOGGER = LogFactory.getLog(ValidatePasswordAgainstUserName.class);
    private  static final int PASSWORD_SUBSTRING_LENGTH = 3;



    @Override
    public boolean validate(Map<String, String> userDetails) throws Exception {
        String userName = userDetails.get(ApiConstants.USERNAME);
        String password = userDetails.get(ApiConstants.PASSWORD);
        if (userName != null && password != null) {
            for (int i = 0; i < password.length() - ApiConstants.MIN_CONSECUTIVE_LETTERS_LENGTH; i++) {
                if (userName.contains(password.substring(i, i + PASSWORD_SUBSTRING_LENGTH))) {
                    LOGGER.warn("Password must not contains more than two consecutive letters of username !");
                    userDetails.put(ApiConstants.ERROR_MESSAGE, ApiConstants.PASSWORD_CONTAINS_USER_NAME);
                    return false;
                }

            }
        } else {
            LOGGER.warn("Username or password must not be null!");
            userDetails.put(ApiConstants.ERROR_MESSAGE, ApiConstants.USER_OR_PASSWORD_NOT_NULL);
            return false;
        }
        LOGGER.info("Password does not contain more than two consecutive letter of username,"
                + " go for next validator");
        return nextValidationHandler(userDetails);
    }
}
