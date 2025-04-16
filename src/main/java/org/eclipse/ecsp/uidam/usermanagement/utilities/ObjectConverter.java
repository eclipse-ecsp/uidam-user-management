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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.exception.ApplicationRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Class to map object data to another data type.
 */
@Slf4j
public final class ObjectConverter {
    private static final Map<String, Method> CONVERTERS_FROM_STRING = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // Preload converters.
        Method[] methods = ObjectConverter.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 1) {
                // Converter should accept 1 argument. This skips the convert() method.
                CONVERTERS_FROM_STRING.put(method.getParameterTypes()[0].getName() + "_"
                    + method.getReturnType().getName(), method);
            }
        }
    }

    private ObjectConverter() {
        // Utility class, hide the constructor.
    }

    // Action -------------------------------------------------------------------------------------

    /**
     * Convert the given object value to the given class.
     *
     * @param from The object value to be converted.
     * @param to   The type class which the given object should be converted to.
     * @return The converted object value.
     * @throws NullPointerException          If 'to' is null.
     * @throws UnsupportedOperationException If no suitable converter can be found.
     * @throws RuntimeException              If conversion failed somehow. This can be caused by at least
     *                                       an ExceptionInInitializerError, IllegalAccessException or
     *                                       InvocationTargetException.
     */
    public static <T> T convert(Object from, Class<T> to) {

        // Null is just null.
        if (from == null) {
            return null;
        }

        // Can we cast? Then just do it.
        if (to.isAssignableFrom(from.getClass())) {
            return to.cast(from);
        }

        Method converter;
        // Lookup the suitable converter.
        String converterId = from.getClass().getName() + "_" + to.getName();
        converter = CONVERTERS_FROM_STRING.get(converterId);
        if (converter == null) {
            throw new UnsupportedOperationException("Cannot convert from "
                + from.getClass().getName() + " to " + to.getName()
                + ". Requested converter does not exist.");
        }

        // Convert the value.
        try {
            return to.cast(converter.invoke(to, from));
        } catch (Exception e) {
            throw new ApplicationRuntimeException("Cannot convert from "
                + from.getClass().getName() + " to " + to.getName()
                + ". Conversion failed with " + e.getMessage());
        }
    }

    // Converters ---------------------------------------------------------------------------------

    /**
     * Converts String to Boolean.
     *
     * @param value The String to be converted.
     * @return The converted Boolean value.
     */
    public static Boolean stringToBoolean(String value) {
        return Boolean.valueOf(value);
    }

    /**
     * Converts String to Integer.
     *
     * @param value The String to be converted.
     * @return The converted Integer value.
     */
    public static Integer stringToInteger(String value) {
        return Integer.valueOf(value);
    }

    /**
     * Converts String to Long.
     *
     * @param value The Long to be converted.
     * @return The converted String value.
     */
    public static Long stringToLong(String value) {
        return Long.valueOf(value);
    }

    /**
     * Converts String to Byte.
     *
     * @param value The Byte to be converted.
     * @return The converted String value.
     */
    public static Byte stringToByte(String value) {
        return Byte.valueOf(value);
    }

    /**
     * Converts String to BigDecimal.
     *
     * @param value The BigDecimal to be converted.
     * @return The converted String value.
     */
    public static BigDecimal stringToBigDecimal(String value) {
        return new BigDecimal(value);
    }

    /**
     * Converts String to Short.
     *
     * @param value The Short to be converted.
     * @return The converted String value.
     */
    public static Short stringToShort(String value) {
        return Short.valueOf(value);
    }

    /**
     * Converts String to Double.
     *
     * @param value The Double to be converted.
     * @return The converted String value.
     */
    public static Double stringToDouble(String value) {
        return Double.valueOf(value);
    }

    /**
     * Converts String to Float.
     *
     * @param value The Float to be converted.
     * @return The converted String value.
     */
    public static Float stringToFloat(String value) {
        return Float.valueOf(value);
    }

    /**
     * Converts String to Date.
     *
     * @param value The Date to be converted.
     * @return The converted String value.
     */
    public static Date stringToDate(String value) {
        return Date.valueOf(value);
    }

    /**
     * Converts String to Time.
     *
     * @param value The Time to be converted.
     * @return The converted String value.
     */
    public static Time stringToTime(String value) {
        return Time.valueOf(value);
    }

    /**
     * Converts String to Timestamp.
     *
     * @param value The Timestamp to be converted.
     * @return The converted String value.
     */
    public static Timestamp stringToTimestamp(String value) {
        return Timestamp.valueOf(value);
    }

    /**
     * Converts String to UUID.
     *
     * @param value The UUID to be converted.
     * @return The converted String value.
     */
    public static UUID stringToUuid(String value) {
        return UUID.fromString(value);
    }

    /**
     * Converts String to List.
     *
     * @param value The List to be converted.
     * @return The converted String value.
     */
    public static List<String> stringToList(String value) {
        return Arrays.stream(value.split(",")).toList();
    }

    /**
     * Converts String to JsonNode.
     *
     * @param value The JsonNode to be converted.
     * @return The converted String value.
     */
    public static JsonNode stringToJsonNode(String value) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(value);
    }

    /**
     * Converts Map to String.
     *
     * @param value The String to be converted.
     * @return The converted Map value.
     */
    public static String jsonNodeObjectToString(Object value) {
        JsonNode jsonNode = null;
        try {
            if (value instanceof String data) {
                jsonNode = OBJECT_MAPPER.readTree(data);
            } else {
                jsonNode = OBJECT_MAPPER.convertValue(value, JsonNode.class);
            }
            return jsonNode.toString();
        } catch (JsonProcessingException exception) {
            LOGGER.error("Error in casting object value to JSONNode:", exception);
            return null;
        }
    }
}
