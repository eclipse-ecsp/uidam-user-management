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

package org.eclipse.ecsp.uidam.usermanagement.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *  Mask IP data in logs.
 */
public class MaskingPatternUtil {

    private MaskingPatternUtil() {

    }

    /**
     * Method to mask data.
     *
     * @param message input log
     * @param patterns pattern to check if mask required
     * @param mask mask string
     * @return masked message
     */
    public static String maskPasswords(String message, Collection<Pattern> patterns, String mask) {
        if (patterns != null && mask != null) {
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(message);
                while (matcher.find()) {
                    message = message.replace(matcher.group(1), mask);
                }
            }
        }
        return message;
    }

    /**
     * Method to parse pattern.
     *
     * @param patternsProperty input patterns.
     * @return List of patterns.
     */
    public static List<Pattern> parsePatterns(final String patternsProperty) {
        String[] patterns = patternsProperty.split("\\|");
        return Arrays.stream(patterns).map(Pattern::compile).toList();
    }

}
