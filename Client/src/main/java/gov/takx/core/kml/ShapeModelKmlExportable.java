package gov.takx.core.kml;

import gov.nasa.worldwind.Exportable;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.ogc.kml.KMLConstants;
import gov.nasa.worldwind.ogc.kml.impl.KMLExportUtil;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IArrow;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.IIrregularShape;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IPolyline;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IRingedShape;
import gov.takx.api.shapes.IRoute;
import gov.takx.api.shapes.IShape;
import gov.takx.platform.kml.KmzUtils;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static gov.nasa.worldwind.ogc.kml.impl.KMLExportUtil.kmlBoolean;

/**
 * An {@link Exportable} wrapper implementation for {@link IShape} objects.
 *
 * @since 5.5
 */
public class ShapeModelKmlExportable implements Exportable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IShape shape;

    public ShapeModelKmlExportable(IShape shape)
    {
        this.shape = shape;
    }

    @Override
    public String isExportFormatSupported(String mimeType)
    {
        if (KMLConstants.KML_MIME_TYPE.equalsIgnoreCase(mimeType))
            return Exportable.FORMAT_SUPPORTED;
        else
            return Exportable.FORMAT_NOT_SUPPORTED;
    }

    @Override
    public void export(String mimeType, Object output) throws IOException, UnsupportedOperationException
    {
        if (mimeType == null)
        {
            String message = "Format is null";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (output == null)
        {
            String message = "Output buffer is null";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (KMLConstants.KML_MIME_TYPE.equalsIgnoreCase(mimeType))
        {
            try
            {
                exportAsKML(output);
            } catch (XMLStreamException e)
            {
                logger.error("Error exporting", e);
                throw new IOException(e);
            }
        } else
        {
            String message = "Unsupported format " + mimeType;
            logger.warn(message);
            throw new UnsupportedOperationException(message);
        }
    }

    /**
     * Export the shape to KML as a {@code <Placemark>} element. The {@code output} object will receive the data. This
     * object must be one of: {@link Writer} {@link OutputStream} {@link XMLStreamWriter}
     *
     * @param output Object to receive the generated KML.
     *
     * @throws XMLStreamException If an exception occurs while writing the KML
     */
    public void exportAsKML(Object output) throws XMLStreamException
    {
        XMLStreamWriter xmlWriter = null;
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        boolean closeWriterWhenFinished = true;

        if (output instanceof XMLStreamWriter xmlStreamWriter)
        {
            xmlWriter = xmlStreamWriter;
            closeWriterWhenFinished = false;
        } else if (output instanceof Writer writer)
        {
            xmlWriter = factory.createXMLStreamWriter(writer);
        } else if (output instanceof OutputStream outputStream)
        {
            xmlWriter = factory.createXMLStreamWriter(outputStream);
        }

        if (xmlWriter == null)
        {
            String message = "Unsupported output object type";
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }

        xmlWriter.writeStartElement("Placemark");

        xmlWriter.writeStartElement("name");
        xmlWriter.writeCharacters(shape.getName());
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("visibility");
        xmlWriter.writeCharacters(KMLExportUtil.kmlBoolean(shape.isVisible()));
        xmlWriter.writeEndElement();

        String shortDescription = shape.getRemarks();
        if (shortDescription != null)
        {
            xmlWriter.writeStartElement("Snippet");
            xmlWriter.writeCharacters(shortDescription);
            xmlWriter.writeEndElement();
        }

        String description = shape.getDescription();
        if (description != null)
        {
            xmlWriter.writeStartElement("description");
            xmlWriter.writeCharacters(description);
            xmlWriter.writeEndElement();
        }

        // Write style map
        xmlWriter.writeStartElement("StyleMap");
        exportAttributesAsKML(xmlWriter, KMLConstants.NORMAL);
        xmlWriter.writeEndElement(); // StyleMap

        // Write geometry
        xmlWriter.writeStartElement("Polygon");

        final boolean canUseVerticalHeight = shape.canUseVerticalHeight();
        final boolean extrude = canUseVerticalHeight && shape.isVerticalHeightEnabled();

        if (canUseVerticalHeight)
        {
            xmlWriter.writeStartElement("extrude");
            xmlWriter.writeCharacters(KMLExportUtil.kmlBoolean(extrude));
            xmlWriter.writeEndElement();
        }

        xmlWriter.writeStartElement("altitudeMode");
        xmlWriter.writeCharacters(extrude ? "relativeToGround" : "clampToGround");
        xmlWriter.writeEndElement();

        // Outer boundary
        Iterable<? extends LatLon> outerBoundary;
        if (shape instanceof IIrregularShape irregularShape)
        {
            outerBoundary = irregularShape.getVertices().stream().map(WWUtils::locationToLatLon).toList();
        } else if (shape instanceof IRectangle rectangle)
        {
            outerBoundary = ShapeUtils.closeLocations(rectangle.getVertices()).stream().map(WWUtils::locationToLatLon).toList();
        } else if (shape instanceof IRingedShape ringedShape)
        {
            double width;
            double height;
            if (shape instanceof ICircle circle)
            {
                width = circle.getRadius();
                height = circle.getRadius();
            } else if (shape instanceof IEllipse ellipse)
            {
                width = ellipse.getSemiMinorAxis();
                height = ellipse.getSemiMinorAxis();
            } else
            {
                throw new IllegalArgumentException("Unsupported ringed shape type: " + ringedShape.getClass().getName());
            }
            List<ILocation> locations = ShapeUtils.getEllipseLocations(ringedShape.getLocation(), width, height, ringedShape.getOrientation());
            outerBoundary = locations.stream().map(WWUtils::locationToLatLon).toList();
        } else
        {
            throw new IllegalArgumentException("Unsupported shape type: " + shape.getClass().getName());
        }

        final Double altitude = extrude ? shape.getVerticalHeight() : null;

        xmlWriter.writeStartElement("outerBoundaryIs");
        KMLExportUtil.exportBoundaryAsLinearRing(xmlWriter, outerBoundary, altitude);
        xmlWriter.writeEndElement(); // outerBoundaryIs

        if (shape instanceof IPolygon polygon)
        {
            // Inner boundaries
            for (IPolygon hole : polygon.getHoles())
            {
                Iterable<? extends LatLon> boundary = hole.getVertices().stream().map(WWUtils::locationToLatLon).toList();

                xmlWriter.writeStartElement("innerBoundaryIs");
                KMLExportUtil.exportBoundaryAsLinearRing(xmlWriter, boundary, altitude);
                xmlWriter.writeEndElement(); // innerBoundaryIs
            }
        }

        xmlWriter.writeEndElement(); // Polygon
        xmlWriter.writeEndElement(); // Placemark

        xmlWriter.flush();
        if (closeWriterWhenFinished) xmlWriter.close();
    }

    /**
     * Export ShapeAttributes as KML Pair element in a StyleMap. This method assumes that the StyleMap tag has already
     * been written; it writes the Pair tag.
     *
     * @param xmlWriter  Writer to receive the Style element.
     * @param styleType  The type of style: normal or highlight. Value should match either {@link KMLConstants#NORMAL}
     *                   or {@link KMLConstants#HIGHLIGHT}
     *
     * @throws javax.xml.stream.XMLStreamException if exception occurs writing XML.
     */
    private void exportAttributesAsKML(XMLStreamWriter xmlWriter, String styleType) throws XMLStreamException
    {
        xmlWriter.writeStartElement("Pair");
        xmlWriter.writeStartElement("key");
        xmlWriter.writeCharacters(styleType);
        xmlWriter.writeEndElement();
        exportAttributesAsKML(xmlWriter);
        xmlWriter.writeEndElement(); // Pair
    }

    /**
     * Export the shape attributes to KML as a {@code <Style>} element. The {@code output} object will receive the
     * data. This object must be one of: {@link Writer} {@link OutputStream} {@link XMLStreamWriter}
     *
     * @param output Object to receive the generated KML.
     *
     * @throws XMLStreamException If an exception occurs while writing the KML
     */
    private void exportAttributesAsKML(Object output) throws XMLStreamException
    {
        XMLStreamWriter xmlWriter = null;
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        boolean closeWriterWhenFinished = true;

        if (output instanceof XMLStreamWriter xmlStreamWriter)
        {
            xmlWriter = xmlStreamWriter;
            closeWriterWhenFinished = false;
        } else if (output instanceof Writer writer)
        {
            xmlWriter = factory.createXMLStreamWriter(writer);
        } else if (output instanceof OutputStream outputStream)
        {
            xmlWriter = factory.createXMLStreamWriter(outputStream);
        }

        if (xmlWriter == null)
        {
            String message = "Unsupported output object type";
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }

        xmlWriter.writeStartElement("Style");

        // Line style
        xmlWriter.writeStartElement("LineStyle");

        final Color lineColor = shape.getLineColor();
        if (lineColor != null)
        {
            xmlWriter.writeStartElement("color");
            xmlWriter.writeCharacters(KmzUtils.formatKmlColor(lineColor));
            xmlWriter.writeEndElement();

            xmlWriter.writeStartElement("colorMode");
            xmlWriter.writeCharacters("normal");
            xmlWriter.writeEndElement();
        }

        final double lineWidth = shape.getLineWidth();
        xmlWriter.writeStartElement("width");
        xmlWriter.writeCharacters(Double.toString(lineWidth));
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement(); // LineStyle

        // Poly style
        xmlWriter.writeStartElement("PolyStyle");

        final Color fillColor = shape.getFillColor();
        if (fillColor != null)
        {
            xmlWriter.writeStartElement("color");
            xmlWriter.writeCharacters(KmzUtils.formatKmlColor(fillColor));
            xmlWriter.writeEndElement();

            xmlWriter.writeStartElement("colorMode");
            xmlWriter.writeCharacters("normal");
            xmlWriter.writeEndElement();
        }

        boolean isLine = shape instanceof IPolyline || shape instanceof IRoute || shape instanceof IArrow;
        boolean shouldFill = !isLine && (fillColor != null && fillColor.getAlpha() > 0);
        xmlWriter.writeStartElement("fill");
        xmlWriter.writeCharacters(kmlBoolean(shouldFill));
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("outline");
        xmlWriter.writeCharacters(kmlBoolean(lineColor != null && lineColor.getAlpha() > 0));
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement(); // PolyStyle
        xmlWriter.writeEndElement(); // Style

        xmlWriter.flush();
        if (closeWriterWhenFinished)
            xmlWriter.close();
    }

    /**
     * Determines if an {@link IShape} object is exportable using {@link ShapeModelKmlExportable}.
     * @param shape the shape
     * @return {@code true} if exportable, {@code false} if not
     */
    public static boolean isSupported(@Nullable IShape shape)
    {
        return (shape instanceof IIrregularShape ||
                shape instanceof IRectangle ||
                shape instanceof ICircle ||
                shape instanceof IEllipse);
    }
}
