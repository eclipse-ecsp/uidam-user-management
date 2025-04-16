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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;

import java.sql.Timestamp;
import java.util.Map;

/**
 * This class is used to check password last update.
 *
 */
public class ValidatePasswordForLastUpdate extends AbstractPasswordValidationHandler {
    private static final Log LOGGER = LogFactory.getLog(ValidatePasswordForLastUpdate.class);

    private static final long INTERVAL_UNIT = 1000L;

    ApplicationProperties applicationProperties;

    public ValidatePasswordForLastUpdate(ApplicationProperties applicationProperties) {
        super();
        this.applicationProperties = applicationProperties;
    }

    @Override
    public boolean validate(Map<String, String> userDetails) throws Exception {
        long passwordUpdateTimeInterval = applicationProperties.getPasswordUpdateTimeInterval();
        String lastUpdateTime = userDetails.get(ApiConstants.LAST_PASSWORD_UPDATE_TIME);
     
        if (StringUtils.isNotEmpty(lastUpdateTime)) {
            Timestamp lastUpTimestamp = Timestamp.valueOf(lastUpdateTime);
            LOGGER.info("current time ::" + System.currentTimeMillis() + " password last update time::" + lastUpdateTime
                    + "milli " + lastUpTimestamp.getTime());
            long lastPasswordChangeTimeDiff = Math.abs(System.currentTimeMillis() - lastUpTimestamp.getTime());
            if (lastPasswordChangeTimeDiff < passwordUpdateTimeInterval * INTERVAL_UNIT) {
                LOGGER.warn("Password can't be changed as it was changed " + lastPasswordChangeTimeDiff / INTERVAL_UNIT
                        + "s ago");
                userDetails.put(ApiConstants.ERROR_MESSAGE, ApiConstants.PASSWORD_CANT_BE_CHANGED);
                return false;
            }
        }
        LOGGER.info("Password was changed before " + passwordUpdateTimeInterval + "s, go for next validation");
        return nextValidationHandler(userDetails);
    }

}