package gov.takx.api.messages;

import gov.takx.api.plugin.IMetadata;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class takes an {@link IStructure} and generates the equivalent XML definition.  The output of this operation,
 * when fed back to {@link DataStructureBuilder#buildFromXML(String, String, InputStream)} will generate a Structure
 * with an identical configuration and hash value.
 *
 * @since 2.0.0
 */
public class StructureToXml
{
    private static final String CHARSET = "UTF-8";
    private static final String SCHEMA_NAMESPACE_URI = "http://www.doe.gov/2010/RaptorStructureSchema";
    private static final String PREFIX = "ns1";

    // Indexed by field type
    private static final String[] FIELD_TYPE_NAMES = {"StringField", "BooleanField", "IntegerField", "FloatField",
            "ShortField", "LongField", "DoubleField", "DateTimeField", "BytesField",
            "StringArrayField", "BooleanArrayField", "IntArrayField", "FloatArrayField", "ShortArrayField",
            "LongArrayField", "DoubleArrayField", "DateArrayField",
    };

    /**
     * Generate the XML equivalent of the given Structure.
     *
     * @param structure The structure to process
     * @return XML form of said structure
     */
    public static String toXml(IStructure structure) throws XMLStreamException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        final XMLStreamWriter xw = XMLOutputFactory.newInstance().createXMLStreamWriter(out, CHARSET);

        xw.writeStartDocument("utf-8", "1.0");

        // <ns1:Structures xmlns:ns1="http://www.doe.gov/2010/RaptorStructureSchema">

        xw.setPrefix(PREFIX, SCHEMA_NAMESPACE_URI);
        xw.writeStartElement(SCHEMA_NAMESPACE_URI, "Structures");
        xw.writeNamespace(PREFIX, SCHEMA_NAMESPACE_URI);

        writeStructure(xw, structure);

        xw.writeEndElement();
        xw.writeEndDocument();
        xw.flush();
        xw.close();

        return out.toString();
    }

    /**
     * Write the Structure element and all sub-elements.
     *
     * @param xw        XML writer
     * @param structure structure to process
     */
    private static void writeStructure(XMLStreamWriter xw, IStructure structure) throws XMLStreamException
    {
        xw.writeStartElement("Structure");

        xw.writeStartElement("MessageType");
        xw.writeCharacters(structure.getMessageType());
        xw.writeEndElement();

        xw.writeStartElement("Version");
        xw.writeCharacters(structure.getVersion());
        xw.writeEndElement();

        if (!structure.isRenderable()) // Default is true
        {
            xw.writeStartElement("Renderable");
            xw.writeCharacters("false");
            xw.writeEndElement();
        }

        writeMetadata(xw, structure.getMetadata());

        writeIndexes(xw, structure.getIndexes());

        writeFields(xw, structure.getFields(), getIndexedFields(structure));

        xw.writeEndElement();
    }

    /**
     * Get the collection of fields that are indexed individually.
     *
     * @param structure Structure to process
     * @return Collection of field names that are indexed
     */
    private static Set<String> getIndexedFields(IStructure structure)
    {
        final Set<String> indexedFields = new HashSet<>();
        for (IIndexDefinition indexDefinition : structure.getIndexes())
        {
            // Any index that is non-unique and has only a single column becomes an "indexed" setting on a field
            final List<String> columnNames = indexDefinition.getColumnNames();

            if (!indexDefinition.isUnique() && columnNames.size() == 1)
            {
                indexedFields.add(columnNames.get(0));
            }
        }
        return indexedFields;
    }

    /**
     * Write the metadata sub-element.
     *
     * @param xw       XML writer
     * @param metadata Metadata to process
     */
    private static void writeMetadata(XMLStreamWriter xw, IMetadata metadata) throws XMLStreamException
    {
        final Set<String> keys = metadata.getMetadataKeys();

        if (keys.isEmpty()) return; // No metadata

        xw.writeStartElement("Metadata");

        for (String key : keys)
        {
            xw.writeStartElement("Key");
            xw.writeCharacters(key);
            xw.writeEndElement();
            xw.writeStartElement("Value");
            xw.writeCharacters(metadata.getMetadataValue(key));
            xw.writeEndElement();
        }

        xw.writeEndElement();
    }

    /**
     * Write the fields collection.
     *
     * @param xw            XML writer
     * @param fields        Collection of fields to process
     * @param indexedFields The set of fields that are indexed individually (need to have "indexed" set true)
     */
    private static void writeFields(XMLStreamWriter xw, Collection<IDataField> fields, Set<String> indexedFields) throws XMLStreamException
    {
        xw.writeStartElement("Fields");

        for (IDataField field : fields)
        {
            writeField(xw, field, indexedFields.contains(field.getFieldName()));
        }

        xw.writeEndElement();
    }

    /**
     * Write a single field.
     *
     * @param xw      XML writer
     * @param field   Field to process
     * @param indexed {@code true} if this field is indexed
     */
    private static void writeField(XMLStreamWriter xw, IDataField field, boolean indexed) throws XMLStreamException
    {
        final int fieldType = field.getFieldType();
        final Set<String> metadataKeys = field.getMetadataKeys();
        final Object defaultValue = field.getDefaultValue();

        // If the field has metadata or a default value, then write a normal element, otherwise write an "empty" element
        final boolean useEmptyElement = metadataKeys.isEmpty() && defaultValue == null;
        if (useEmptyElement)
        {
            xw.writeEmptyElement(FIELD_TYPE_NAMES[fieldType]);
        } else
        {
            xw.writeStartElement(FIELD_TYPE_NAMES[fieldType]);
        }

        // Add the name, position, shared, and indexed attributes

        xw.writeAttribute("name", field.getFieldName());
        xw.writeAttribute("position", String.valueOf(field.getOrdinalPosition()));

        if (indexed) xw.writeAttribute("indexed", "true"); // false is default

        // String fields and array types can have a specified length
        if (isStringOrArrayType(fieldType))
        {
            final Integer fieldLength = field.getFieldLength();

            if (fieldLength != null)
            {
                xw.writeAttribute("fieldLength", String.valueOf(fieldLength));
            }
        }

        writeMetadata(xw, field);

        writeDefaultValue(xw, fieldType, defaultValue);

        if (!useEmptyElement) xw.writeEndElement();
    }

    /**
     * Determine if a given field type is a string or an array type.
     *
     * @param type Type to test
     * @return {@code true} if type is String or array type
     */
    private static boolean isStringOrArrayType(int type)
    {
        switch (type)
        {
            case IDataType.STRING:
            case IDataType.BYTE_ARRAY:
            case IDataType.BOOLEAN_ARRAY:
            case IDataType.STRING_ARRAY:
            case IDataType.SHORT_ARRAY:
            case IDataType.INT_ARRAY:
            case IDataType.LONG_ARRAY:
            case IDataType.FLOAT_ARRAY:
            case IDataType.DOUBLE_ARRAY:
            case IDataType.DATE_ARRAY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Write out the default value for a field, if any.  Special handling of byte array default values by Bas64
     * encoding the bytes.
     *
     * @param xw           XML writer
     * @param fieldType    Type of field being processed
     * @param defaultValue Default value to write, may be {@code null}
     */
    private static void writeDefaultValue(XMLStreamWriter xw, int fieldType, Object defaultValue) throws XMLStreamException
    {
        if (defaultValue != null)
        {
            if (fieldType == IDataType.BYTE_ARRAY)
            {
                // Base64 encode the data
                defaultValue = Base64.getEncoder().encodeToString((byte[]) defaultValue);
            }

            xw.writeStartElement("DefaultValue");
            xw.writeCharacters(String.valueOf(defaultValue));
            xw.writeEndElement();
        }
    }

    /**
     * Write out the composite indices on this structure.  Only those that are unique or use multiple columns will
     * generate an index definition.  Others will be handled as "indexed" attributes on individual fields.
     *
     * @param xw               XML writer
     * @param indexDefinitions All index definitions from the structure.
     */
    private static void writeIndexes(XMLStreamWriter xw, List<? extends IIndexDefinition> indexDefinitions) throws XMLStreamException
    {
        if (indexDefinitions.isEmpty()) return;

        final List<IIndexDefinition> definitions = new ArrayList<>();

        for (IIndexDefinition indexDefinition : indexDefinitions)
        {
            // Only emit those that are unique or have more than one column
            final List<String> columnNames = indexDefinition.getColumnNames();

            if (indexDefinition.isUnique() || columnNames.size() > 1)
            {
                definitions.add(indexDefinition);
            }
        }

        if (definitions.isEmpty()) return; // None to process

        xw.writeStartElement("Indexes");

        for (IIndexDefinition definition : definitions)
        {
            xw.writeStartElement("Index");
            xw.writeAttribute("unique", String.valueOf(definition.isUnique()));

            for (String columnName : definition.getColumnNames())
            {
                xw.writeStartElement("field");
                xw.writeCharacters(columnName);
                xw.writeEndElement();
            }

            xw.writeEndElement();
        }
        xw.writeEndElement();
    }
}
