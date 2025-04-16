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

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import static org.eclipse.ecsp.uidam.usermanagement.config.MaskingPatternUtil.maskPasswords;
import static org.eclipse.ecsp.uidam.usermanagement.config.MaskingPatternUtil.parsePatterns;

/**
 * Utility to mask ip data in logs.
 */
public class MaskingPasswordJsonProvider extends MessageJsonProvider {

    private static final String FIELD_MESSAGE = "message";

    private String patternsProperty;
    private String mask;
    private String clrfMask;
    private List<Pattern> patterns;

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        String formattedMessage = event.getFormattedMessage()
            .replaceAll("(\\r|\\n)", clrfMask); //replacing the line breaks to prevent CLRF log injection attack
        String maskPasswords = maskPasswords(formattedMessage, patterns, mask);
        JsonWritingUtils.writeStringField(generator, FIELD_MESSAGE, maskPasswords);
    }

    public void setPatternsProperty(final String patternsProperty) {
        this.patternsProperty = patternsProperty;
        patterns = parsePatterns(patternsProperty);
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    public void setClrfMask(String clrfMask) {
        this.clrfMask = clrfMask;
    }
}
