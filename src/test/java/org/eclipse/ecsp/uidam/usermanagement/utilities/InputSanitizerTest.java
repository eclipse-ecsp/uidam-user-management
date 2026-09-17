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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link InputSanitizer}.
 *
 * <p>Covers the {@code forLog()} method which strips CR/LF characters to prevent
 * log injection attacks.
 */
class InputSanitizerTest {

    // -----------------------------------------------------------------------
    // forLog — null input
    // -----------------------------------------------------------------------

    @Test
    void forLog_nullInput_returnsNull() {
        assertNull(InputSanitizer.forLog(null));
    }

    // -----------------------------------------------------------------------
    // forLog — safe input unchanged
    // -----------------------------------------------------------------------

    @Test
    void forLog_normalInput_returnsSameValue() {
        String input = "some normal log message";
        assertEquals(input, InputSanitizer.forLog(input));
    }

    @Test
    void forLog_emptyString_returnsEmptyString() {
        assertEquals("", InputSanitizer.forLog(""));
    }

    @Test
    void forLog_inputWithSpacesAndDigits_unchanged() {
        String input = "user123 logged in at 10:00";
        assertEquals(input, InputSanitizer.forLog(input));
    }

    // -----------------------------------------------------------------------
    // forLog — CR/LF replaced with underscore
    // -----------------------------------------------------------------------

    @Test
    void forLog_inputWithLineFeed_replacedWithUnderscore() {
        assertEquals("line1_line2", InputSanitizer.forLog("line1\nline2"));
    }

    @Test
    void forLog_inputWithCarriageReturn_replacedWithUnderscore() {
        assertEquals("line1_line2", InputSanitizer.forLog("line1\rline2"));
    }

    @Test
    void forLog_inputWithCrLf_bothReplacedWithUnderscore() {
        assertEquals("line1__line2", InputSanitizer.forLog("line1\r\nline2"));
    }

    @Test
    void forLog_multipleNewlines_allReplaced() {
        assertEquals("a_b_c_d", InputSanitizer.forLog("a\nb\rc\nd"));
    }

    @Test
    void forLog_onlyNewline_replacedWithUnderscore() {
        assertEquals("_", InputSanitizer.forLog("\n"));
    }

    @Test
    void forLog_newlineAtStart_replaced() {
        assertEquals("_hello", InputSanitizer.forLog("\nhello"));
    }

    @Test
    void forLog_newlineAtEnd_replaced() {
        assertEquals("hello_", InputSanitizer.forLog("hello\n"));
    }

    // -----------------------------------------------------------------------
    // forLog — log-injection attempt patterns stripped
    // -----------------------------------------------------------------------

    @Test
    void forLog_injectionAttemptWithNewline_newlineReplaced() {
        String injected = "INFO: ok\nERROR: injected";
        assertEquals("INFO: ok_ERROR: injected", InputSanitizer.forLog(injected));
    }
}
