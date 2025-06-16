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

package org.eclipse.ecsp.uidam.security.policy.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService.PasswordValidationInput;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;

import java.sql.Timestamp;
import java.util.Map;

/**
 * This class is used to check password last update.
 *
 */
public class LastUpdateValidationPolicyHandler extends PasswordPolicyHandler {
    private static final int INT_60 = 60;

    private static final Log LOGGER = LogFactory.getLog(LastUpdateValidationPolicyHandler.class);

    private final long passwordUpdateTimeInterval;
    private static final long INTERVAL_UNIT = 1000L;

    public LastUpdateValidationPolicyHandler(Map<String, Object> rules) {
        this.passwordUpdateTimeInterval = toInt(rules.get("passwordUpdateTimeIntervalSec"), INT_60) * INTERVAL_UNIT;
    }

    /**
     * Validates the password update time interval.
     *
     * @param userDetails Map containing user details
     * @return true if validation passes, false otherwise
     * @throws Exception if an error occurs during validation
     */
    @Override
    protected boolean doHandle(PasswordValidationInput input) {
        if (input.lastUpdateTime() != null) {
            Timestamp lastUpTimestamp = input.lastUpdateTime();
            LOGGER.debug(
                    "For user " + input.username() + " last password update time is " + lastUpTimestamp.toString());
            long lastPasswordChangeTimeDiff = Math.abs(System.currentTimeMillis() - lastUpTimestamp.getTime());
            if (lastPasswordChangeTimeDiff < passwordUpdateTimeInterval) {
                LOGGER.warn("Password can't be changed as it was changed " + lastPasswordChangeTimeDiff / INTERVAL_UNIT
                        + "s ago");
                setErrorMessage(ApiConstants.PASSWORD_CANT_BE_CHANGED);
                return false;
            }
        }
        LOGGER.debug("Password was changed before " + passwordUpdateTimeInterval + "s, go for next validation");
        return true;

    }

}
