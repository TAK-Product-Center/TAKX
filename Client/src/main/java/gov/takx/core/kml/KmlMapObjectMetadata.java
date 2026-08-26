package gov.takx.core.kml;

import gov.takx.api.kml.IKmlConstants;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

/**
 * Metadata for a KML representation of a map object.  Reads the metadata from an XML string passed in the constructor.
 */
public class KmlMapObjectMetadata
{
    private String family;
    private String type;
    private String unitID;
    private String name;
    private long trackPoints;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Read and populate the metadata from the metadata XML string
     *
     * @param metadata XML Metadata
     */
    protected KmlMapObjectMetadata(String metadata)
    {
        if (IKmlConstants.UNKNOWN_FORMAT.equals(metadata)) return;

        try
        {
            restore(XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(metadata)));
        } catch (XMLStreamException e)
        {
            logger.error("Error restoring Map Object from KML", e);
        }
    }

    protected String getFamily()
    {
        return family;
    }

    protected String getUnitID()
    {
        return unitID;
    }

    protected String getType()
    {
        return type;
    }

    protected long getTrackPointCount()
    {
        return trackPoints;
    }

    protected boolean isValidMapObject()
    {
        return family != null && type != null && unitID != null;
    }

    protected String getName()
    {
        return name;
    }

    /**
     * Create a restorable state string (as XML) to be stored in the {@code ExtendedData} element of the Map Object Placemark.
     *
     * @param mapObject MapObject to process
     * @param messageCount Number of track points associated with the given Map Object
     * @return restorable XML string
     * @since 5.5.1
     */
    public static String makeRestorableString(@Nonnull IMapObject mapObject, long messageCount)
    {
        final var sw = new StringWriter(1024);
        try
        {
            var xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(sw);
            xsw.writeStartElement(IKmlConstants.MO_ROOT_ELEMENT);
            xsw.writeAttribute(IKmlConstants.MESSAGES, Long.toString(messageCount));
            xsw.writeAttribute(IKmlConstants.FAMILY, mapObject.getFamily());
            xsw.writeAttribute(IKmlConstants.TYPE, mapObject.getType());
            xsw.writeAttribute(IKmlConstants.UNITID, mapObject.getUnitID());
            xsw.writeAttribute(IKmlConstants.NAME, mapObject.getName());
            xsw.writeEndElement();
            xsw.writeEndDocument();
            xsw.flush();
            xsw.close();
            return sw.toString();
        } catch (XMLStreamException e)
        {
            logger.error("XML Exception writing map object restorable string", e);
        }
        return IKmlConstants.UNKNOWN_FORMAT;
    }

    /**
     * Restore and populate the metadata from the XML stream
     *
     * @param xsr XMLStreamReader
     * @throws XMLStreamException exception parsing the XML
     */
    private void restore(XMLStreamReader xsr) throws XMLStreamException
    {
        int eventType = xsr.next();

        if (eventType != XMLEvent.START_ELEMENT)
        {
            logger.error("Event type must be XMLEvent.START_ELEMENT ({}); got {}", XMLEvent.START_ELEMENT, eventType);
            return;
        }

        for (int idx = 0; idx < xsr.getAttributeCount(); idx++)
        {
            String name = xsr.getAttributeLocalName(idx);
            if (IKmlConstants.FAMILY.equals(name))
            {
                family = xsr.getAttributeValue(idx);
            } else if (IKmlConstants.TYPE.equals(name))
            {
                type = xsr.getAttributeValue(idx);
            } else if (IKmlConstants.UNITID.equals(name))
            {
                unitID = xsr.getAttributeValue(idx);
            } else if (IKmlConstants.MESSAGES.equals(name))
            {
                trackPoints = Long.parseLong(xsr.getAttributeValue(idx));
            } else if (IKmlConstants.NAME.equals(name))
            {
                this.name = xsr.getAttributeValue(idx);
            }
        }
    }
}
