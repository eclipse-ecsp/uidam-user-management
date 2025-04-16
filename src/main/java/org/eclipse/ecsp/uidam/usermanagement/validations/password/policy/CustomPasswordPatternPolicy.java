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
import java.util.HashMap;
import java.util.Map;

/**
 * This class is implementation of custom password pattern policy enforcer interface.
 *
 */
public class CustomPasswordPatternPolicy extends AbstractPasswordPolicyEnforcer {

    private static final Log LOGGER = LogFactory.getLog(CustomPasswordPatternPolicy.class);
    private ValidationChain validationChain;
    private ApplicationProperties applicationProperties;
    private static final int MAX_ARGS_LENGTH = 3;

    public CustomPasswordPatternPolicy(ApplicationProperties applicationProperties) {
        validationChain = new ValidationChain();
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean enforce(Object... args) {
        LOGGER.debug("Starting password validation enforcer!");
        boolean status = true;
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put(ApiConstants.PASSWORD, args[0].toString());
        userDetails.put(ApiConstants.USERNAME, args[1].toString());
        if (args.length == MAX_ARGS_LENGTH) {
            userDetails.put(ApiConstants.LAST_PASSWORD_UPDATE_TIME, args[MAX_ARGS_LENGTH - 1].toString());
        }
        userDetails.put(ApiConstants.ERROR_MESSAGE, "");
        try {
            AbstractPasswordValidationHandler validatePasswordForLastUpdate = new ValidatePasswordForLastUpdate(
                    applicationProperties);
            validatePasswordForLastUpdate.linkNextValidationHandler(new ValidatePasswordLength(applicationProperties))
                    .linkNextValidationHandler(new ValidatePasswordAgainstUserName())
                    .linkNextValidationHandler(new CheckForCompromisedPassword());
            validationChain.setStartingPoint(validatePasswordForLastUpdate);
            LOGGER.debug("Start validation of password !");
            status = validationChain.startValidation(userDetails);
            errorMessage = userDetails.get(ApiConstants.ERROR_MESSAGE);
        } catch (Exception e) {
            LOGGER.error("There is some issue while validating the password ::",  e);
        }
        return status;

    }

}