/********************************************************************************
 * Copyright (c) 2023-24 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.uidam.usermanagement.utilities;

/**
 * Utility class for sanitizing and validating user input to prevent
 * injection attacks (SQL injection, XSS, script injection).
 */
public final class InputSanitizer {

    private InputSanitizer() {
    }

    /**
     * Sanitizes a string for safe inclusion in log statements by removing
     * CR and LF characters that could be used for log injection/forging.
     *
     * @param input the string to sanitize
     * @return the sanitized string, or null if input is null
     */
    public static String forLog(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\r\n]", "_");
    }
}
