package org.eclipse.ecsp.uidam.usermanagement.utilities;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Serializer for converting {@link BigInteger} values to {@link String}.
 * This class extends {@link JsonSerializer} to provide custom serialization logic
 * for {@link BigInteger} fields, converting them into their string representation.
 */
public class BigIntegerToStringSerializer extends JsonSerializer<BigInteger> {

    /**
     * Serializes a {@link BigInteger} value into its {@link String} representation.
     * If the value is {@code null}, a null value is written to the JSON output.
     *
     * @param value The {@link BigInteger} value to serialize.
     * @param gen The {@link JsonGenerator} used to write the value as a string.
     * @param serializers The serializer provider that can be used to get serializers
     *                    for serializing the value's properties.
     * @throws IOException If an error occurs during writing to the JSON output.
     */
    @Override
    public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            gen.writeString(value.toString());
        } else {
            gen.writeNull();
        }
    }
}
