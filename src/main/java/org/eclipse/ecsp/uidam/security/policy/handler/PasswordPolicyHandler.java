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

import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationInput;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract class representing a handler in a chain of responsibility pattern for password policy validation. Each
 * handler in the chain is responsible for validating a specific aspect of the password policy.
 */
public abstract class PasswordPolicyHandler {

    /**
     * The next handler in the chain of responsibility.
     */
    protected PasswordPolicyHandler next;

    /**
     * Error message to be used when a password policy violation occurs.
     */
    private String errorMessage;

    /**
     * Retrieves the error message associated with this handler.
     *
     * @return the error message.
     */
    protected String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for this handler.
     *
     * @param errorMessage the error message to set.
     */
    protected void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Sets the next handler in the chain of responsibility.
     *
     * @param nextHandler the next handler to set.
     */
    public void setNextHandler(PasswordPolicyHandler nextHandler) {
        this.next = nextHandler;
    }

    /**
     * Handles the password validation by invoking the current handler's logic and passing the input to the next handler
     * in the chain if applicable.
     *
     * @param input the password validation input.
     * @return true if the password passes all validations in the chain.
     */
    public boolean handle(PasswordValidationInput input) {
        boolean result = doHandle(input);
        if (!result) {
            return false;
        }
        if (next != null) {
            return next.handle(input);
        }
        return result;

    }

    /**
     * Abstract method to be implemented by subclasses to define specific password validation logic.
     *
     * @param input the password validation input.
     * @return true if the password passes the validation, false otherwise.
     */
    protected abstract boolean doHandle(PasswordValidationInput input);

    /**
     * Converts an object to an integer. If the object is null or cannot be converted, the default value is returned.
     *
     * @param value the object to convert.
     * @param defaultVal the default value to return if conversion fails.
     * @return the integer value or the default value.
     */
    protected int toInt(Object value, int defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    /**
     * Converts a string into a set of characters.
     *
     * @param input the string to convert.
     * @return a set of characters contained in the string.
     */
    protected Set<Character> toCharSet(String input) {
        Set<Character> result = new HashSet<>();
        if (input != null) {
            for (char c : input.toCharArray()) {
                result.add(c);
            }
        }
        return result;
    }

    /**
     * Converts an object to a string. If the object is null, the default value is returned.
     *
     * @param value the object to convert.
     * @param defaultValue the default value to return if the object is null.
     * @return the string representation of the object or the default value.
     */
    protected String toString(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }
}
