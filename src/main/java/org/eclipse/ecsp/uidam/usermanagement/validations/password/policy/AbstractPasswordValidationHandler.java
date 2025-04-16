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
import java.util.Map;



/**
 * This class is implementing password validation handler.
 *
 */
public abstract class AbstractPasswordValidationHandler implements PasswordValidationHandler {
    private static final Log LOGGER = LogFactory.getLog(AbstractPasswordValidationHandler.class);
    private AbstractPasswordValidationHandler handler;

    public AbstractPasswordValidationHandler linkNextValidationHandler(AbstractPasswordValidationHandler handler) {
        this.handler = handler;
        return handler;
    }

    protected boolean nextValidationHandler(Map<String, String> userDetails) throws Exception {
        if (handler == null) {
            LOGGER.info("No next validation handler, allow password creation");
            return true;
        }
        return handler.validate(userDetails);
    }
}