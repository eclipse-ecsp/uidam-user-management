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
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is used to validate password length.
 *
 */
public class ValidatePasswordLength extends AbstractPasswordValidationHandler {
    private static final Log LOGGER = LogFactory.getLog(ValidatePasswordLength.class);

    ApplicationProperties applicationProperties;
    
    Pattern pattern = null;

    /**
     * constructor and initialize password pattern.
     */
    public ValidatePasswordLength(ApplicationProperties applicationProperties) {
        super();
        this.applicationProperties = applicationProperties;
        pattern = Pattern.compile(applicationProperties.getPasswordRegexPattern());
    }

    @Override
    public boolean validate(Map<String, String> userDetails) throws Exception {
        String password = userDetails.get(ApiConstants.PASSWORD);
        if (password.length() < applicationProperties.getMinPasswordLength()) {
            LOGGER.warn("Password length is less than min allowed password length");
            userDetails.put(ApiConstants.ERROR_MESSAGE, ApiConstants.MIN_PASSWORD_LENGTH_VIOLATION);
            return false;
        }
        if (password.length() > applicationProperties.getMaxPasswordLength()) {
            LOGGER.warn("Password length is more than max allowed password length");
            userDetails.put(ApiConstants.ERROR_MESSAGE, ApiConstants.MAX_PASSWORD_LENGTH_VIOLATION);
            return false;
        }

        Matcher matcher = pattern.matcher(password);
        
        if (!matcher.matches()) {
            LOGGER.warn("Password should contain atleast One Number and One Uppercase and "
                   + "One Lowercase one One Special Character");
            userDetails.put(ApiConstants.ERROR_MESSAGE, ApiConstants.PASSWORD_PATTERN_VIOLATION);
            return false;
        }
        LOGGER.info("Password length is within allowed limit, go for next validation");
        return nextValidationHandler(userDetails);
    }

}
