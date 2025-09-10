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

package org.eclipse.ecsp.uidam.usermanagement.utilities;

/**
 * Utility class for masking sensitive data in logs.
 * Provides methods to safely log user-controlled data while maintaining traceability.
 */
public final class TokenMaskingUtils {

    private static final int TOKEN_MIN_LENGTH_FOR_MASKING = 12;
    private static final int TOKEN_PREFIX_LENGTH = 8;
    private static final int TOKEN_SUFFIX_LENGTH = 4;
    private static final String MASKED_PLACEHOLDER = "****-****-****";
    private static final String MASK_SEPARATOR = "****";
    private static final String NULL_REPRESENTATION = "null";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TokenMaskingUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Masks sensitive token data for logging purposes.
     * Shows first 8 characters and last 4 characters of the token for tracking while hiding sensitive parts.
     * This method helps prevent logging of user-controlled data while maintaining debugging capability.
     *
     * @param token the token to mask
     * @return masked token string safe for logging
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return NULL_REPRESENTATION;
        }
        if (token.length() <= TOKEN_MIN_LENGTH_FOR_MASKING) {
            return MASKED_PLACEHOLDER;
        }
        return token.substring(0, TOKEN_PREFIX_LENGTH) + MASK_SEPARATOR 
                + token.substring(token.length() - TOKEN_SUFFIX_LENGTH);
    }

    /**
     * Masks sensitive data for logging purposes with custom prefix and suffix lengths.
     *
     * @param data the sensitive data to mask
     * @param prefixLength number of characters to show at the beginning
     * @param suffixLength number of characters to show at the end
     * @return masked data string safe for logging
     */
    public static String maskSensitiveData(String data, int prefixLength, int suffixLength) {
        if (data == null || data.isEmpty()) {
            return NULL_REPRESENTATION;
        }
        if (data.length() <= prefixLength + suffixLength) {
            return MASKED_PLACEHOLDER;
        }
        return data.substring(0, prefixLength) + MASK_SEPARATOR 
                + data.substring(data.length() - suffixLength);
    }
}
