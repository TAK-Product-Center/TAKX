package gov.takx.core.kml;

import gov.nasa.worldwind.Exportable;
import gov.nasa.worldwind.ogc.kml.KMLConstants;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.invoke.MethodHandles;

/**
 * Abstract class to extend with various KML wrapper classes
 */
public abstract class AKmlWrapper implements Exportable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected String kmlFolder;

    protected AKmlWrapper(String kmlFolder)
    {
        this.kmlFolder = kmlFolder;
    }

    @Override
    public String isExportFormatSupported(String mimeType)
    {
        return KMLConstants.KML_MIME_TYPE.equalsIgnoreCase(mimeType) ? FORMAT_SUPPORTED : FORMAT_NOT_SUPPORTED;
    }

    @Override
    public void export(@Nonnull String mimeType, @Nonnull Object output) throws IOException, UnsupportedOperationException
    {
        if (KMLConstants.KML_MIME_TYPE.equalsIgnoreCase(mimeType))
        {
            try
            {
                exportAsKML(output);
            } catch (XMLStreamException e)
            {
                logger.error("XMLStreamException", e);
            }
        } else
        {
            throw new UnsupportedOperationException("Unsupported export format");
        }
    }

    /**
     * Export to specified output.  Overriding classes should export as their model calls for.
     *
     * @param output Object that will receive the exported data. The type of this object depends on the export format.
     *               All formats should support {@code java.io.OutputStream}. Text based format (for example, XML
     *               formats) should also support {@code java.io.Writer}. Certain formats may also support other
     *               object types.
     * @throws IOException        if an exception occurs while exporting the data
     * @throws XMLStreamException if an exception occurs writing the XML
     */
    protected abstract void exportAsKML(Object output) throws IOException, XMLStreamException;

    /**
     * Create an {@link XMLStreamWriter} from the given {@code output} type.
     *
     * @param output Output, one of {@link XMLStreamWriter}, {@link Writer}, or {@link OutputStream}
     * @return XMLStreamWriter
     * @since 5.5.1
     */
    protected XMLStreamWriter createStreamWriter(@Nonnull Object output) throws XMLStreamException
    {
        if (output instanceof XMLStreamWriter xmlStreamWriter)
        {
            return xmlStreamWriter;
        } else if (output instanceof Writer writer)
        {
            return XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
        } else if (output instanceof OutputStream outputStream)
        {
            return XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        }

        logger.warn("Unknown output type: {}", output.getClass().getName());
        throw new IllegalArgumentException("Unknown output type");
    }

    /**
     * Write out an {@code ExtendedData} element with the given data {@code value}.
     *
     * @param xmlWriter Write to use
     * @param value data value
     * @since 5.5.1
     */
    protected void writeExtendedData(@Nonnull XMLStreamWriter xmlWriter, String value) throws XMLStreamException
    {
        xmlWriter.writeStartElement("ExtendedData");
        xmlWriter.writeStartElement("Data");
        xmlWriter.writeAttribute("name", "extendedData");
        xmlWriter.writeStartElement("value");
        xmlWriter.writeCData(value);
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();
    }

    /**
     * Write out a {@code Style} element, including {@code IconStyle} and {@code BalloonStyle}.
     *
     * @param xmlWriter Writer to use
     * @param styleId Style id, ignored if {@code null}
     * @param scale Icon scale (like "0.4" or "1.5")
     * @param href Icon href
     * @param balloonText text for BalloonStyle
     * @param includeLabelStyle Pass {@code true} to include a {@code LabelStyle} with a scale of "0"
     * @since 5.5.1
     */
    protected void writeStyle(@Nonnull XMLStreamWriter xmlWriter, @Nullable String styleId, @Nonnull String scale,
                              @Nonnull String href, @Nonnull String balloonText, boolean includeLabelStyle)
            throws XMLStreamException
    {
        xmlWriter.writeStartElement("Style");
        if (styleId != null) xmlWriter.writeAttribute("id", styleId);

        xmlWriter.writeStartElement("IconStyle");
        xmlWriter.writeStartElement("scale");
        xmlWriter.writeCharacters(scale);
        xmlWriter.writeEndElement();    // scale
        writeIconHref(xmlWriter, href);
        xmlWriter.writeEndElement();    // IconStyle

        if (includeLabelStyle)
        {
            xmlWriter.writeStartElement("LabelStyle");
            xmlWriter.writeStartElement("scale");
            xmlWriter.writeCharacters("0");
            xmlWriter.writeEndElement();
            xmlWriter.writeEndElement();
        }

        xmlWriter.writeStartElement("BalloonStyle");
        xmlWriter.writeStartElement("text");
        xmlWriter.writeCData(balloonText);
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement();
    }

    /**
     * Write out a {@code Point} geometry.
     *
     * @param xmlWriter Writer to use
     * @param location Location of point
     * @since 5.5.1
     */
    protected void writePoint(@Nonnull XMLStreamWriter xmlWriter, @Nonnull ILocation location) throws XMLStreamException
    {
        xmlWriter.writeStartElement("Point");
        xmlWriter.writeStartElement("coordinates");
        if (location.hasElevation())
        {
            xmlWriter.writeCharacters(location.getLongitude() + "," + location.getLatitude() + "," + location.getElevation());
        } else
        {
            xmlWriter.writeCharacters(location.getLongitude() + "," + location.getLatitude());
        }
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();
    }

    /**
     * Write out an {@code Icon} element with the given {@code href}.
     *
     * @param xmlWriter Writer to use
     * @param href icon's href
     * @since 5.5.1
     */
    protected void writeIconHref(@Nonnull XMLStreamWriter xmlWriter, @Nonnull String href) throws XMLStreamException
    {
        xmlWriter.writeStartElement("Icon");
        xmlWriter.writeStartElement("href");
        xmlWriter.writeCharacters(href);
        xmlWriter.writeEndElement(); // href
        xmlWriter.writeEndElement();  // Icon
    }
}
