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
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for validating passwords against a set of policies. This service loads password policies from the database
 * and applies them to validate passwords.
 */
@Service
public class PasswordValidationService {

    private static final Log LOGGER = LogFactory.getLog(PasswordValidationService.class);
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final PasswordPolicyHandlerFactory handlerFactory;

    private List<PasswordPolicyHandler> handlers;
    private Timestamp lastUpdateDate;

    /**
     * Constructor for PasswordValidationService.
     *
     * @param passwordPolicyRepository The repository for password policies.
     * @param handlerFactory The factory for creating password policy handlers.
     */
    public PasswordValidationService(PasswordPolicyRepository passwordPolicyRepository,
            PasswordPolicyHandlerFactory handlerFactory) {
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.handlerFactory = handlerFactory;
        this.lastUpdateDate = null; // Initialize with null to ensure the first refresh happens
    }

    // Use a read-write lock to allow concurrent reads and exclusive writes
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Initializes the password validation service by loading the policies from the database.
     */
    @PostConstruct
    public void loadPolicies() {
        refreshPolicies();
    }

    /**
     * Refreshes the password policies from the database and sets up the handlers. This method is called during
     * application startup and can also be called manually to refresh policies.
     */
    public void refreshPolicies() {
        lock.writeLock().lock();
        LOGGER.debug("Refreshing password policies from the database.");
        try {
            Optional<List<PasswordPolicy>> policies = passwordPolicyRepository
                    .findAllByRequiredTrueOrderByPriorityAsc();
            if (policies.isEmpty()) {
                handlers = new LinkedList<>();
                return;
            }
            // Clear existing handlers
            handlers = new LinkedList<>();

            for (PasswordPolicy policy : policies.get()) {
                PasswordPolicyHandler handler = handlerFactory.createHandler(policy);
                handlers.add(handler);
            }

        } finally {
            // Always unlock the write lock in a finally block to ensure it gets released
            LOGGER.debug("Password policies refreshed successfully,with " + handlers.size() + " handlers loaded.");
            lock.writeLock().unlock();
        }
    }

    /**
     * Periodically checks for updates in the password policies.
     */
    @Scheduled(fixedDelayString = "${security.password.policy.check-interval:60s}")
    public void checkForPolicyUpdates() {
        Timestamp latestUpdateDate = passwordPolicyRepository.findLatestUpdateDate();
        if (latestUpdateDate != null && (lastUpdateDate == null || latestUpdateDate.after(lastUpdateDate))) {
            LOGGER.debug("Detected changes in password policies. Refreshing policies.");
            refreshPolicies();
            lastUpdateDate = latestUpdateDate; // Update the last known update date
        }
    }

    /**
     * Validates the password for a new user.
     *
     * @param password The password to validate.
     * @param username The username of the user.
     * @return ValidationResult indicating whether the password is valid and any error message.
     */
    public ValidationResult validatePassword(String password, String username) {
        // Password validation for new user.
        lock.readLock().lock();
        LOGGER.info("Validating password for new user: " + username);
        try {
            if (handlers.isEmpty()) {
                return new ValidationResult(true, null); // No policies to validate
            }
            return handlers.stream()
                    .filter(handler -> !(handler instanceof ExpirationPolicyHandler)
                            && !(handler instanceof LastUpdateValidationPolicyHandler))
                    .filter(handler -> !handler.handle(new PasswordValidationInput(username, password, null)))
                    .findFirst().map(handler -> new ValidationResult(false, handler.getErrorMessage()))
                    .orElse(new ValidationResult(true, null)); // All handlers passed

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Validates the password for an existing user, including checking the last update time.
     *
     * @param password The password to validate.
     * @param username The username of the user.
     * @param lastUpdateTime The last update time of the password.
     * @return ValidationResult indicating whether the password is valid and any error message.
     */
    public ValidationResult validatePassword(String password, String username, Timestamp lastUpdateTime) {
        // Password validation for existing user.(update password)
        lock.readLock().lock();
        LOGGER.info("Validating password for existing user: " + username);
        try {
            if (handlers.isEmpty()) {
                return new ValidationResult(true, null); // No policies to validate
            }
            return handlers.stream()
                    .filter(handler -> !handler.handle(new PasswordValidationInput(username, password, lastUpdateTime)))
                    .findFirst().map(handler -> new ValidationResult(false, handler.getErrorMessage()))
                    .orElse(new ValidationResult(true, null)); // All handlers passed

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Validates the password expiry for an existing user.
     *
     * @param username The username of the user.
     * @return ValidationResult indicating whether the password is valid and any error message.
     */
    public ValidationResult validateUserPasswordExpiry(String username) {
        lock.readLock().lock();
        LOGGER.info("Validating password expiry for user: " + username);
        try {
            if (handlers.isEmpty()) {
                return new ValidationResult(true, null); // No policies to validate
            }
            return handlers.stream().filter(ExpirationPolicyHandler.class::isInstance)
                    .filter(handler -> !handler.handle(new PasswordValidationInput(username, null, null))).findFirst()
                    .map(handler -> new ValidationResult(false, handler.getErrorMessage()))
                    .orElse(new ValidationResult(true, null)); // All handlers passed
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Represents the result of a password validation check.
     *
     * @param isValid Indicates whether the password is valid.
     * @param errorMessage Contains an error message if the password is invalid, null otherwise.
     */
    public record ValidationResult(boolean isValid, String errorMessage) {
    }
}
