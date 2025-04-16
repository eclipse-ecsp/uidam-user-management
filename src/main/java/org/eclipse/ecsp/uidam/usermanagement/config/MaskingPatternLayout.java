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

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import java.util.regex.Pattern;
import static org.eclipse.ecsp.uidam.usermanagement.config.MaskingPatternUtil.maskPasswords;
import static org.eclipse.ecsp.uidam.usermanagement.config.MaskingPatternUtil.parsePatterns;

/**
 * Utility to mask ip data in logs.
 */
public class MaskingPatternLayout extends PatternLayout {

    private String patternsProperty;
    private String mask;

    private List<Pattern> patterns;

    @Override
    public String doLayout(ILoggingEvent event) {
        String message = super.doLayout(event);
        return maskPasswords(message, patterns, mask);
    }

    public void setPatternsProperty(String patternsProperty) {
        this.patternsProperty = patternsProperty;
        patterns = parsePatterns(patternsProperty);
    }

    public void setMask(String mask) {
        this.mask = mask;
    }
}
