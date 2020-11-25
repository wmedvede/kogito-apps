package org.kie.kogito.taskassigning.process.service.client.impl.mp.queries;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ZonedDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {

    public ZonedDateTimeDeserializer() {
        super(ZonedDateTime.class);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        String rawValue = parser.getText();
        try {
            return ZonedDateTime.parse(rawValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IOException("Invalid ISO-8601 value : '" + rawValue + "'. because of : '" + e.getMessage() + "'", e);
        }
    }
}
