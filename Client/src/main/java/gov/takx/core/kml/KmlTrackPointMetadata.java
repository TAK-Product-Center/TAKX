package gov.takx.core.kml;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.kml.IKmlConstants;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.commons.date.DateFormatSet;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Class for reading and populating metadata for a specific track point from metadata in the KML ExtendedData tag.
 */
public class KmlTrackPointMetadata
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DateFormatSet.ISO_8601_DATE_FORMAT_Z);

    private ILocation location;
    private String unitID;
    private double speed;
    private double heading;
    private boolean motion;
    private long time;

    /**
     * Restore the track point metadata from an XML metadata string
     *
     * @param metadata XML metadata
     */
    protected KmlTrackPointMetadata(String metadata)
    {
        if (IKmlConstants.UNKNOWN_FORMAT.equals(metadata)) return;

        try
        {
            restore(XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(metadata)));
        } catch (XMLStreamException e)
        {
            logger.error("Error restoring Track Point from KML", e);
        }
    }

    protected void setLocation(Coordinate coordinate)
    {
        location = new GeoPoint(coordinate.getLatitude(), coordinate.getLongitude(), coordinate.getAltitude());
    }

    protected void setTime(String timeString)
    {
        try
        {
            time = dateFormatter.parse(timeString).getTime();
        } catch (ParseException e)
        {
            logger.warn("Error parsing time string {}", timeString);
        }
    }

    protected double getHeading()
    {
        return heading;
    }

    protected ILocation getLocation()
    {
        return location;
    }

    protected boolean isMotion()
    {
        return motion;
    }

    protected double getSpeed()
    {
        return speed;
    }

    protected long getTime()
    {
        return time;
    }

    protected String getUnitID()
    {
        return unitID;
    }

    /**
     * Create a restorable string so that on re-import of the kml, rdms can be created for the correct
     * map object.  Note that family, type and map object version will be the same for all track points of a
     * map object and can go in the map object folder extended data.
     *
     * @param msg RDM to extract data from
     * @return restorable rdm string
     * @since 5.5.1
     */
    public static String makeRestorableString(@Nonnull IRaptorDataMessage msg)
    {
        final var sw = new StringWriter(1024);
        try
        {
            XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(sw);
            xsw.writeStartElement(IKmlConstants.RDM_ROOT_ELEMENT);
            xsw.writeAttribute(IKmlConstants.UNITID, msg.getUnitID());
            xsw.writeAttribute(IKmlConstants.SPEED, Double.toString(msg.getSpeedInMps()));
            xsw.writeAttribute(IKmlConstants.HEADING, Double.toString(msg.getHeading()));
            xsw.writeAttribute(IKmlConstants.PITCH, Double.toString(msg.getPitch()));
            xsw.writeAttribute(IKmlConstants.ROLL, Double.toString(msg.getRoll()));
            xsw.writeAttribute(IKmlConstants.MOTION, msg.isMotionSet() ? Boolean.toString(msg.hasMotion()) : "");
            xsw.writeEndElement();
            xsw.writeEndDocument();
            xsw.flush();
            xsw.close();
            return sw.toString();
        } catch (XMLStreamException e)
        {
            logger.error("XML Exception writing rdm restorable string", e);
        }
        return IKmlConstants.UNKNOWN_FORMAT;
    }

    /**
     * Restore the track point metadata using an XMLStreamReader to parse data from the KML ExtendedData tag.
     *
     * @param xsr XMLStreamReader
     * @throws XMLStreamException exception parsing XML
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
            if (IKmlConstants.UNITID.equals(name))
            {
                unitID = xsr.getAttributeValue(idx);
            } else if (IKmlConstants.SPEED.equals(name))
            {
                speed = Double.parseDouble(xsr.getAttributeValue(idx));
            } else if (IKmlConstants.HEADING.equals(name))
            {
                heading = Double.parseDouble(xsr.getAttributeValue(idx));
            } else if (IKmlConstants.MOTION.equals(name))
            {
                motion = Boolean.parseBoolean(xsr.getAttributeValue(idx));
            }
        }
    }
}
