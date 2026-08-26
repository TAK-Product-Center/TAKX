package gov.takx.dataservices.utils;

import gov.tak.api.util.AttributeSet;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter to handle persistence of {@link AttributeSet} objects as JSON strings.
 * @since 3.4
 */
@Converter(autoApply = true)
public class AttributeSetConverter implements AttributeConverter<AttributeSet, String>
{
    @Override
    public String convertToDatabaseColumn(AttributeSet attributeSet)
    {
        return attributeSet.toJson();
    }

    @Override
    public AttributeSet convertToEntityAttribute(String dbData)
    {
        return new AttributeSet(dbData);
    }
}
