package gov.takx.dataservices.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import gov.tak.api.video.ConnectionEntry;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Converter to handle persistence of {@link gov.tak.api.video.ConnectionEntry} objects as JSON strings.
 * @since 5.3
 */
@Converter(autoApply = true)
public class ConnectionEntryConverter implements AttributeConverter<ConnectionEntry, String>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ObjectMapper MAPPER = new ObjectMapper().addMixIn(ConnectionEntry.class, IConnectionEntryMixin.class);
    private static final ObjectReader READER = MAPPER.readerFor(ConnectionEntry.class);
    private static final ObjectWriter WRITER = MAPPER.writerFor(ConnectionEntry.class);

    @Override
    public String convertToDatabaseColumn(ConnectionEntry entry)
    {
        try
        {
            return WRITER.writeValueAsString(entry);
        } catch (JsonProcessingException e)
        {
            logger.error("Failed to create JSON for {}", entry, e);
            return null;
        }
    }

    @Override
    public ConnectionEntry convertToEntityAttribute(String json)
    {
        try
        {
            return READER.readValue(json);
        } catch (JsonProcessingException e)
        {
            logger.error("Failed to create ConnectionEntry from {}", json, e);
            return null;
        }
    }

    /**
     * Mixin to prevent handling the {@link ConnectionEntry#isRemote()} method as a property.
     */
    interface IConnectionEntryMixin
    {
        @JsonIgnore
        boolean isRemote();
    }
}
