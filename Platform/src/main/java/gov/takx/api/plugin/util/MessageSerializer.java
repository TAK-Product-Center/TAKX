package gov.takx.api.plugin.util;

import gov.takx.api.messages.DataFieldEncoding;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IDataType;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IPrePersistRaptorMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.commons.date.DateTimeParser;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class to serialize {@link IRaptorDataMessage}s to XML, and read them back.
 *
 */
public final class MessageSerializer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Date formatter used to format dates in XML serialization.
     */
    private static final DateTimeParser DATE_FORMATTER =
            new DateTimeParser(DateTimeParser.ISO_8601_DATE_FORMAT, ZoneId.of("UTC"));

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private MessageSerializer()
    {
    }

    /**
     * Read a list of messages back from an XML serialization.
     *
     * @param xml     XML serialization. May not be {@code null}.
     * @param factory Data structure factory to use for generation of messages
     * @return List of messages.
     */
    public static List<IPrePersistRaptorDataMessage> messagesFromXml(String xml, IDataStructureFactory factory)
    {
        if (xml == null) throw new IllegalArgumentException("Parameter 'xml' may not be null");

        List<IPrePersistRaptorDataMessage> list = new ArrayList<>();

        try
        {
            Document document = SafeXmlDocumentUtils.parseXML(xml);

            XPath xpath = XPathFactory.newInstance().newXPath();

            NodeList messages = (NodeList) xpath.evaluate("/dataMessages/dataMessage", document, XPathConstants.NODESET);
            if (messages != null)
            {
                for (int i = 0; i < messages.getLength(); i++)
                {
                    list.add(fromRdmNode(messages.item(i), xpath, factory));
                }
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return list;
    }

    /**
     * Read a message from an XML string.
     *
     * @param xml     XML representation of a RaptorDataMessage. May not be {@code null}.
     * @param factory Data structure factory to use for generation of message
     * @return reified {@link IRaptorDataMessage}
     */
    public static IPrePersistRaptorDataMessage fromXml(String xml, IDataStructureFactory factory)
    {
        return fromXml(xml, factory::getDataStructure);
    }

    /**
     * Read a message from an XML string.
     *
     * @param xml XML representation of a RaptorDataMessage. May not be {@code null}.
     * @param rds RaptorDataStructure to use for generation of message
     * @return reified {@link IRaptorDataMessage}
     */
    public static IPrePersistRaptorDataMessage fromXml(String xml, IRaptorDataStructure rds)
    {
        return fromXml(xml, s -> rds);
    }

    /**
     * Read a message from an XML string.
     *
     * @param xml             XML representation of a RaptorDataMessage. May not be {@code null}.
     * @param structureFinder Function to determine the data structure to use to create the RDM
     * @return reified {@link IRaptorDataMessage}
     * @since 2.22
     */
    private static IPrePersistRaptorDataMessage fromXml(String xml, Function<String, IRaptorDataStructure> structureFinder)
    {
        if (xml == null) throw new IllegalArgumentException("Parameter 'xml' may not be null");
        try
        {
            Document document = SafeXmlDocumentUtils.parseXML(xml);

            XPath xpath = XPathFactory.newInstance().newXPath();

            Node messageNode = (Node) xpath.evaluate("/dataMessage", document, XPathConstants.NODE);
            String messageType = (String) xpath.evaluate("messageType", messageNode, XPathConstants.STRING);

            return messageNode != null ? fromRdmNode(messageNode, xpath, structureFinder.apply(messageType)) : null;
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Construct and RDM from the given XML node.
     *
     * @param messageNode XML node to process
     * @param xpath       xpath processor
     * @param factory     Data structure factory to use for generation of messages
     * @return newly constructed RDM
     * @throws XPathExpressionException if an XPath expression error occurs
     */
    private static IPrePersistRaptorDataMessage fromRdmNode(@Nonnull Node messageNode, @Nonnull XPath xpath,
                                                            @Nonnull IDataStructureFactory factory)
            throws XPathExpressionException
    {
        String messageType = (String) xpath.evaluate("messageType", messageNode, XPathConstants.STRING);

        IRaptorDataStructure rds = factory.getDataStructure(messageType);
        return fromRdmNode(messageNode, xpath, rds);
    }

    /**
     * Construct an RDM from the given XML node and rds.
     *
     * @param messageNode XML node to process
     * @param xpath       xpath processor
     * @param rds         RaptorDataStructure used to construct the message.
     * @return newly constructed RDM
     * @throws XPathExpressionException if an XPath expression error occurs
     */
    private static IPrePersistRaptorDataMessage fromRdmNode(@Nonnull Node messageNode, @Nonnull XPath xpath,
                                                            @Nonnull IRaptorDataStructure rds)
            throws XPathExpressionException
    {
        String mapObjectType = (String) xpath.evaluate("mapObjectType", messageNode, XPathConstants.STRING);
        String mapObjectId = (String) xpath.evaluate("mapObjectId", messageNode, XPathConstants.STRING);

        IPrePersistRaptorDataMessage msg = rds.createRaptorDataMessage(mapObjectType, mapObjectId);

        processStringField(messageNode, xpath, "uuid", uuid -> msg.setUUID(UUID.fromString(uuid)));
        processStringField(messageNode, xpath, "sourceName", msg::setSourceName);

        String sourceType = (String) xpath.evaluate("sourceType", messageNode, XPathConstants.STRING);
        if (!sourceType.isEmpty())
        {
            try
            {
                CommServiceType commType = CommServiceType.valueOf(sourceType);
                msg.setSourceType(commType);
            } catch (IllegalArgumentException e)
            {
                logger.warn("Unrecognized source type encountered: {}", sourceType, e);
            }
        }

        String lat = (String) xpath.evaluate("lat", messageNode, XPathConstants.STRING);
        String lng = (String) xpath.evaluate("lng", messageNode, XPathConstants.STRING);
        String elevation = (String) xpath.evaluate("elevation", messageNode, XPathConstants.STRING);
        if (!lat.isEmpty() && !lng.isEmpty())
        {
            if (!elevation.isEmpty())
            {
                msg.setLocation(Double.parseDouble(lat), Double.parseDouble(lng), Double.parseDouble(elevation));
            } else
            {
                msg.setLocation(Double.parseDouble(lat), Double.parseDouble(lng));
            }
        }

        String pitch = (String) xpath.evaluate("pitch", messageNode, XPathConstants.STRING);
        String heading = (String) xpath.evaluate("heading", messageNode, XPathConstants.STRING);
        String roll = (String) xpath.evaluate("roll", messageNode, XPathConstants.STRING);
        if (!pitch.isEmpty() && !heading.isEmpty() && !roll.isEmpty())
        {
            msg.setOrientation(Double.parseDouble(heading), Double.parseDouble(pitch), Double.parseDouble(roll));
        }

        processStringField(messageNode, xpath, "speed", speed -> msg.setSpeedInMps(Double.parseDouble(speed)));
        processStringField(messageNode, xpath, "time", time -> msg.setTime(Long.parseLong(time)));
        processStringField(messageNode, xpath, "motion", motion -> msg.setHasMotion(Boolean.parseBoolean(motion)));
        processStringField(messageNode, xpath, "isImported", isImported -> msg.setImported(Boolean.parseBoolean(isImported)));

        NodeList fields = (NodeList) xpath.evaluate("field", messageNode, XPathConstants.NODESET);
        setFields(msg, fields);

        return msg;
    }

    /**
     * Sets the structure fields in a data message.
     *
     * @param msg    The message whose fields need to be set
     * @param fields The values of the fields
     * @since 2.35
     */
    private static void setFields(IPrePersistRaptorMessage msg, NodeList fields)
    {
        if (fields != null)
        {
            for (int i = 0; i < fields.getLength(); i++)
            {
                final Node node = fields.item(i);
                final NamedNodeMap attributes = node.getAttributes();
                final String name = attributes.getNamedItem("name").getNodeValue();
                final String type = attributes.getNamedItem("type").getNodeValue();
                final String value = node.getTextContent();

                try
                {
                    final int fieldType = IDataType.getDataTypeForName(type);
                    final Object fieldValue = DataFieldEncoding.decodeFieldValue(name, fieldType, value, DATE_FORMATTER);
                    msg.setField(name, fieldValue);
                } catch (IllegalArgumentException e)
                {
                    // Unknown field type name or unparseable value, report it and skip it
                    final Exception exceptionToReport = logger.isDebugEnabled() ? e : null;
                    logger.error("Unable to process XML node '{}' of type '{}' with, value '{}': {}", name, type, value, e, exceptionToReport);
                }
            }
        }
    }

    /**
     * Process a String element from the XML document.
     *
     * @param node         Message node
     * @param xpath        XPath instance
     * @param expression   Expression to locate field element
     * @param fieldHandler Handler for field value, only called if the value is non-empty
     * @throws XPathExpressionException If the expression is invalid
     * @since 2.22
     */
    private static void processStringField(Node node, XPath xpath, String expression, Consumer<String> fieldHandler) throws XPathExpressionException
    {
        final String value = (String) xpath.evaluate(expression, node, XPathConstants.STRING);
        if (!value.isEmpty()) fieldHandler.accept(value);
    }

    /**
     * Serialize a data message to an XML string.
     *
     * @param rdm Message to serialize. May not be {@code null}.
     * @return XML representation of the message.
     */
    public static String toXml(@Nonnull IRaptorDataMessage rdm)
    {
        StringWriter out = new StringWriter();
        try
        {
            toXml(rdm, out);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    /**
     * Serialize a list of messages to XML, and write the serialization to a Writer.
     *
     * @param messageList Messages to serialize. May not be {@code null}, and may not contain {@code null} elements.
     * @param out         Writer to receive the output. May not be {@code null}.
     * @throws IOException If anything goes wrong writing output.
     */
    public static void toXml(@Nonnull List<IRaptorDataMessage> messageList, @Nonnull Writer out) throws IOException
    {
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        try
        {
            XMLStreamWriter xtw = xof.createXMLStreamWriter(out);

            xtw.writeStartElement("dataMessages");
            for (IRaptorDataMessage msg : messageList)
            {
                xtw.writeStartElement("dataMessage");
                writeRdmElements(msg, xtw);
                xtw.writeEndElement();
            }
            xtw.writeEndElement();

            xtw.writeEndDocument();
            xtw.flush();
            xtw.close();
        } catch (XMLStreamException e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Serialize a message to XML, and write the serialization to a Writer.
     *
     * @param msg Message to serialize. May not be {@code null}.
     * @param out Writer to receive the output. May not be {@code null}.
     * @throws IOException If anything goes wrong writing output.
     */
    public static void toXml(IRaptorDataMessage msg, Writer out) throws IOException
    {
        if (msg == null) throw new IllegalArgumentException("Parameter 'msg' may not be null");
        if (out == null) throw new IllegalArgumentException("Parameter 'out' may not be null");

        XMLOutputFactory xof = XMLOutputFactory.newInstance();

        try
        {
            XMLStreamWriter xtw = xof.createXMLStreamWriter(out);

            xtw.writeStartElement("dataMessage");
            writeRdmElements(msg, xtw);
            xtw.writeEndElement();

            xtw.writeEndDocument();
            xtw.flush();
            xtw.close();
        } catch (XMLStreamException e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Writes the RDM fields to the provided XML writer
     *
     * @param msg The RDM that is being written
     * @param xtw The XML writer that receives the output
     * @throws XMLStreamException Thrown if an error occurs while writing the data
     * @since 2.35
     */
    private static void writeRdmElements(IRaptorDataMessage msg, XMLStreamWriter xtw) throws XMLStreamException
    {
        writeElement("receiveTime", msg.getReceiveTime(), xtw);
        writeElement("isImported", String.valueOf(msg.isImported()), xtw);

        if (msg.hasLocation())
        {
            writeElement("lat", msg.getLatitude(), xtw);
            writeElement("lng", msg.getLongitude(), xtw);
        }

        if (msg.hasSourceName()) writeElement("sourceName", msg.getSourceName(), xtw);
        if (msg.hasElevation()) writeElement("elevation", msg.getElevation(), xtw);
        if (msg.hasHeading()) writeElement("heading", msg.getHeading(), xtw);
        if (msg.has3dOrientation())
        {
            writeElement("pitch", msg.getPitch(), xtw);
            writeElement("roll", msg.getRoll(), xtw);
        }

        if (msg.hasSpeed()) writeElement("speed", msg.getSpeedInMps(), xtw);
        if (msg.hasTime()) writeElement("time", msg.getRelevantTime(), xtw);
        if (msg.isMotionSet()) writeElement("motion", String.valueOf(msg.hasMotion()), xtw);

        writeElement("messageType", msg.getMessageType(), xtw);
        writeElement("messageVersion", msg.getVersion(), xtw);
        writeElement("mapObjectFamily", msg.getFamily(), xtw);
        writeElement("mapObjectType", msg.getType(), xtw);
        writeElement("mapObjectId", msg.getUnitID(), xtw);
        writeElement("mapObjectKey", msg.getUniqueKey(), xtw);
        writeElement("uuid", msg.getUUID().toString(), xtw);

        for (IDataField curField : msg.getFields())
        {
            final String fieldName = curField.getFieldName();
            final int fieldType = curField.getFieldType();
            final Object fieldValue = msg.getField(fieldName);

            xtw.writeStartElement("field");
            xtw.writeAttribute("name", fieldName);
            xtw.writeAttribute("type", IDataType.getDataTypeName(fieldType));
            xtw.writeCharacters(DataFieldEncoding.encodeFieldValue(fieldName, fieldType, fieldValue, DATE_FORMATTER));
            xtw.writeEndElement();
        }
    }

    /**
     * Performance optimization to avoid the intermediate allocation of a Double object.
     *
     * @since 3.0
     */
    private static void writeElement(String elementName, double value, XMLStreamWriter xtw) throws XMLStreamException
    {
        writeElement(elementName, String.valueOf(value), xtw);
    }

    /**
     * Performance optimization to avoid the intermediate allocation of a Long object.
     *
     * @since 3.0
     */
    private static void writeElement(String elementName, long value, XMLStreamWriter xtw) throws XMLStreamException
    {
        writeElement(elementName, String.valueOf(value), xtw);
    }

    /**
     * Write an element with the character content of the given string.  Nulls are written as an empty string.
     *
     * @param elementName Name of element to write
     * @param value       Value to write
     * @param xtw         The XML stream writer to receive the output
     * @since 3.0
     */
    private static void writeElement(String elementName, String value, XMLStreamWriter xtw) throws XMLStreamException
    {
        xtw.writeStartElement(elementName);
        xtw.writeCharacters(value != null ? value : "");
        xtw.writeEndElement();
    }
}
