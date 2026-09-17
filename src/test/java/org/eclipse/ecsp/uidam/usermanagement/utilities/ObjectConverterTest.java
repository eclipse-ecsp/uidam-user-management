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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectConverterTest {

    private static final short EXPECTED_SHORT_VALUE = 32767;
    private static final double EXPECTED_DOUBLE_VALUE = 5.5d;
    private static final float EXPECTED_FLOAT_VALUE = 5.5f;

    @Test
    void convertObjectValueIsNull() {
        assertEquals(false, Optional.ofNullable(ObjectConverter.convert(null, Integer.class))
            .isPresent());
    }

    @Test
    void convertObjectValueIsSame() {
        Object objectToConvert = 1;
        Integer integer = ObjectConverter.convert(objectToConvert, Integer.class);
        assertEquals(1, integer);
    }

    @Test
    void convertObjectMethodNotFound() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> ObjectConverter.convert(1, LocalDate.class)
        );
    }

    @Test
    void convertObjectFailed() {
        assertThrows(
            RuntimeException.class,
            () -> ObjectConverter.convert("1", Date.class)
        );
    }

    @Test
    void convertStringToObject() {
        Object objectToConvert = String.valueOf(Boolean.TRUE);
        Boolean booleanData = ObjectConverter.convert(objectToConvert, Boolean.class);
        assertEquals(Boolean.TRUE, booleanData);
    }

    @Test
    void stringToBoolean() {
        String stringToConvert = String.valueOf(Boolean.TRUE);
        Boolean booleanData = ObjectConverter.stringToBoolean(stringToConvert);
        assertEquals(Boolean.TRUE, booleanData);
    }

    @Test
    void stringToInteger() {
        String stringToConvert = String.valueOf(1);
        Integer integer = ObjectConverter.stringToInteger(stringToConvert);
        assertEquals(1, integer);
    }

    @Test
    void stringToLong() {
        String stringToConvert = String.valueOf(1L);
        Long stringToLong = ObjectConverter.stringToLong(stringToConvert);
        assertEquals(1L, stringToLong);
    }

    @Test
    void stringToByte() {
        String stringToConvert = String.valueOf(1);
        Byte stringToByte = ObjectConverter.stringToByte(stringToConvert);
        assertEquals(Byte.valueOf((byte) 1), stringToByte);
    }

    @Test
    void stringToBigDecimal() {
        String stringToConvert = "124567890.0987654321";
        BigDecimal bd1 = new BigDecimal("124567890.0987654321");
        BigDecimal toBigDecimal = ObjectConverter.stringToBigDecimal(stringToConvert);
        assertEquals(bd1, toBigDecimal);
    }

    @Test
    void stringToShort() {
        String stringToConvert = "32767";
        Short stringToShort = ObjectConverter.stringToShort(stringToConvert);
        assertEquals(EXPECTED_SHORT_VALUE, stringToShort);
    }

    @Test
    void stringToDouble() {
        String stringToConvert = "5.5";
        Double stringToDouble = ObjectConverter.stringToDouble(stringToConvert);
        assertEquals(EXPECTED_DOUBLE_VALUE, stringToDouble);
    }

    @Test
    void stringToFloat() {
        String stringToConvert = "5.5";
        Float stringToFloat = ObjectConverter.stringToFloat(stringToConvert);
        assertEquals(EXPECTED_FLOAT_VALUE, stringToFloat);
    }

    @Test
    void stringToDate() {
        Date date = Date.valueOf(LocalDate.now());
        String stringToConvert = String.valueOf(date);
        Date toDate = ObjectConverter.stringToDate(stringToConvert);
        assertEquals(date, toDate);
    }

    @Test
    void stringToTime() {
        Time time = Time.valueOf(LocalTime.now());
        String stringToConvert = time.toString();
        Time stringToTime = ObjectConverter.stringToTime(stringToConvert);
        assertEquals(time, stringToTime);
    }

    @Test
    void stringToTime_shortFormat_hhmm_appendsSeconds() {
        // Browser <input type="time"> sends "HH:mm" without seconds
        Time result = ObjectConverter.stringToTime("14:30");
        assertEquals(Time.valueOf("14:30:00"), result);
    }

    @Test
    void stringToTimestamp() {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        String stringToConvert = String.valueOf(timestamp);
        Timestamp stringToTimestamp = ObjectConverter.stringToTimestamp(stringToConvert);
        assertEquals(timestamp, stringToTimestamp);
    }

    @Test
    void stringToTimestamp_isoFormatWithTandNoSeconds_convertsCorrectly() {
        // Browser <input type="datetime-local"> sends "yyyy-MM-ddTHH:mm"
        Timestamp result = ObjectConverter.stringToTimestamp("2026-07-22T01:31");
        assertEquals(Timestamp.valueOf("2026-07-22 01:31:00"), result);
    }

    @Test
    void stringToTimestamp_isoFormatWithTandSeconds_convertsCorrectly() {
        // Full ISO 8601 with seconds "yyyy-MM-ddTHH:mm:ss"
        Timestamp result = ObjectConverter.stringToTimestamp("2026-07-22T01:31:45");
        assertEquals(Timestamp.valueOf("2026-07-22 01:31:45"), result);
    }

    @Test
    void stringToTimestamp_null_returnsNull() {
        assertNull(ObjectConverter.stringToTimestamp(null));
    }

    @Test
    void stringToTimestampWithIsoFormat() {
        String stringToConvert = "2026-09-02T10:15:30+05:30";
        Timestamp timestamp = ObjectConverter.stringToTimestamp(stringToConvert);
        assertEquals(Timestamp.from(OffsetDateTime.parse(stringToConvert).toInstant()), timestamp);
    }

    @Test
    void stringToTimestampWithBlankValue() {
        assertEquals(null, ObjectConverter.stringToTimestamp(""));
    }

    @Test
    void stringToUuid() {
        UUID uuid = UUID.randomUUID();
        String stringToConvert = String.valueOf(uuid);
        UUID toUuid = ObjectConverter.stringToUuid(stringToConvert);
        assertEquals(uuid, toUuid);
    }

    @Test
    void stringToList() {
        String data = "abc,new";
        List list = ObjectConverter.stringToList(data);
        assertEquals(true, list.contains("abc"));
        assertEquals(true, list.contains("new"));
    }

    @Test
    void stringToJsonNode() throws JsonProcessingException {
        String json = "{\"key\": \"value\"}";
        JsonNode jsonNode = ObjectConverter.stringToJsonNode(json);
        assertEquals("value", jsonNode.get("key").asText());
    }

    @Test
    void jsonNodeObjectToStringInstanceIsString() {
        String json = "{\"key\": \"value\"}";
        String jsonData = ObjectConverter.jsonNodeObjectToString(json);
        assertEquals("{\"key\":\"value\"}", jsonData);
    }

    @Test
    void jsonNodeObjectToStringInstanceIsMap() {
        Map json = Map.of("key", "value");
        String jsonData = ObjectConverter.jsonNodeObjectToString(json);
        assertEquals("{\"key\":\"value\"}", jsonData);
    }

    @Test
    void jsonNodeObjectToStringFailed() {
        Optional optionalData = Optional.ofNullable(ObjectConverter
            .jsonNodeObjectToString("key:value"));
        assertEquals(false, optionalData.isPresent());
    }
}
