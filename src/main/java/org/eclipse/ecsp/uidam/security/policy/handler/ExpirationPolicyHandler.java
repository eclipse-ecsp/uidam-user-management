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
import org.eclipse.ecsp.uidam.usermanagement.entity.PasswordHistoryEntity;
import org.eclipse.ecsp.uidam.usermanagement.repository.PasswordHistoryRepository;
import org.eclipse.ecsp.uidam.usermanagement.utilities.PasswordUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This class is used to check password history.
 *
 */
public class ExpirationPolicyHandler extends PasswordPolicyHandler {
    private static final int INT_90 = 90;

    private static final Log LOGGER = LogFactory.getLog(ExpirationPolicyHandler.class);

    private final int passwordHistoryCount;
    private final int passwordExpiryDays;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final String passwordEncoder;

    /**
     * Constructor to initialize PasswordHistoryHandler with rules, password history repository and password encoder.
     *
     * @param rules The rules for password history and expiry.
     * @param passwordHistoryRepository The repository to fetch password history.
     * @param passwordEncoder The password encoder.
     */
    public ExpirationPolicyHandler(Map<String, Object> rules, PasswordHistoryRepository passwordHistoryRepository,
            String passwordEncoder) {
        this.passwordHistoryCount = toInt(rules.get("passwordHistoryCount"), 0);
        this.passwordExpiryDays = toInt(rules.get("passwordExpiryDays"), INT_90);
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Handles password validation by checking password history and expiry.
     *
     * @param input The input containing username and password.
     * @return true if the password is valid, false otherwise.
     */
    protected boolean doHandle(PasswordValidationInput input) {
        if (isUsernamePresentAndPasswordNull(input)) {
            LOGGER.info("Validating password expiry for user: " + input.username());
            Date lastPasswordChangeDate = passwordHistoryRepository.findLastPasswordChangeDate(input.username());
            if (lastPasswordChangeDate != null) {
                LocalDate expiryDate = lastPasswordChangeDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        .plusDays(passwordExpiryDays);
                if (LocalDate.now().isAfter(expiryDate)) {
                    LOGGER.warn(ApiConstants.PASSWORD_EXPIRED);
                    setErrorMessage(ApiConstants.PASSWORD_EXPIRED);
                    return false;
                }
            }
            return true;
        } else {
            LOGGER.info("Validating password history and expiry for user: " + input.username());
            // Fetch password history with pagination
            Pageable pageable = PageRequest.of(0, passwordHistoryCount); // Fetch only 'passwordHistoryCount' records
            List<PasswordHistoryEntity> passwordHistoryEntityList = passwordHistoryRepository
                    .findPasswordHistoryByUserName(input.username(), pageable);
            // Check password history
            if (passwordHistoryEntityList != null && !passwordHistoryEntityList.isEmpty()) {
                List<String> userSalts = passwordHistoryEntityList.stream().map(PasswordHistoryEntity::getPasswordSalt)
                        .toList();
                List<String> userPasswords = passwordHistoryEntityList.stream()
                        .map(PasswordHistoryEntity::getUserPassword).toList();
                boolean isValid = PasswordUtils.isPasswordValid(passwordEncoder, input.password(), userSalts,
                        userPasswords);
                if (!isValid) {
                    LOGGER.warn(ApiConstants.OLD_PASSWORD_CANT_BE_REUSED);
                    setErrorMessage(ApiConstants.OLD_PASSWORD_CANT_BE_REUSED);
                    return false;
                }
            }
        }
        LOGGER.info("Password validation passed for user: " + input.username());
        return true;
    }

    /**
     * Determines if the username is present and the password is null.
     *
     * @param input The input containing username and password.
     * @return true if the username is present and the password is null, false otherwise.
     */
    private boolean isUsernamePresentAndPasswordNull(PasswordValidationInput input) {
        return input.username() != null && input.password() == null;
    }
}
