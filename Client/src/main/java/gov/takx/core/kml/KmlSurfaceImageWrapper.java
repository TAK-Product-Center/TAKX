package gov.takx.core.kml;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.gx.GXConstants;
import gov.nasa.worldwind.render.SurfaceImage;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Wrapper for exporting {@link gov.nasa.worldwind.render.SurfaceImage}
 */
public class KmlSurfaceImageWrapper extends AKmlWrapper
{
    private static int surfaceImageNumber = 1;

    private final SurfaceImage surfaceImage;

    protected KmlSurfaceImageWrapper(SurfaceImage surfaceImage, String kmlFolder)
    {
        super(kmlFolder);
        this.surfaceImage = surfaceImage;
    }

    @Override
    protected void exportAsKML(Object output) throws IOException, XMLStreamException
    {
        XMLStreamWriter xmlWriter = createStreamWriter(output);

        xmlWriter.writeStartElement("GroundOverlay");

        // Get the buffered image and save it to the kmz file
        Object imageSource = surfaceImage.getImageSource();
        if (imageSource instanceof BufferedImage)
        {
            final StringBuilder imagePath = new StringBuilder();
            imagePath.append(kmlFolder).append(File.separator).append("SurfaceImage").append(surfaceImageNumber++).append(".png");
            File outputFile = new File(imagePath.toString());
            ImageIO.write((BufferedImage) imageSource, "png", outputFile);

            writeIconHref(xmlWriter, outputFile.getName());
        }

        xmlWriter.writeStartElement("altitudeMode");
        xmlWriter.writeCharacters("clampToGround");
        xmlWriter.writeEndElement();

        // If the corners of the image are aligned to a sector, we can export the position as a KML LatLonBox. If not,
        // we'll need to use a gx:LatLonQuad.
        if (Sector.isSector(surfaceImage.getCorners()))
        {
            exportKMLLatLonBox(xmlWriter);
        } else
        {
            exportKMLLatLonQuad(xmlWriter);
        }

        xmlWriter.writeEndElement(); // GroundOverlay
        xmlWriter.flush();
    }

    private void exportKMLLatLonBox(XMLStreamWriter xmlWriter) throws XMLStreamException
    {
        xmlWriter.writeStartElement("LatLonBox");
        xmlWriter.writeStartElement("north");
        xmlWriter.writeCharacters(Double.toString(surfaceImage.getSector().getMaxLatitude().getDegrees()));
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("south");
        xmlWriter.writeCharacters(Double.toString(surfaceImage.getSector().getMinLatitude().getDegrees()));
        xmlWriter.writeEndElement(); // south

        xmlWriter.writeStartElement("east");
        xmlWriter.writeCharacters(Double.toString(surfaceImage.getSector().getMinLongitude().getDegrees()));
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("west");
        xmlWriter.writeCharacters(Double.toString(surfaceImage.getSector().getMaxLongitude().getDegrees()));
        xmlWriter.writeEndElement(); // west
        xmlWriter.writeEndElement(); // LatLonBox
    }

    private void exportKMLLatLonQuad(XMLStreamWriter xmlWriter) throws XMLStreamException
    {
        xmlWriter.writeStartElement(GXConstants.GX_NAMESPACE, "LatLonQuad");
        xmlWriter.writeStartElement("coordinates");

        for (LatLon ll : surfaceImage.getCorners())
        {
            xmlWriter.writeCharacters(Double.toString(ll.getLongitude().getDegrees()));
            xmlWriter.writeCharacters(",");
            xmlWriter.writeCharacters(Double.toString(ll.getLatitude().getDegrees()));
            xmlWriter.writeCharacters(" ");
        }

        xmlWriter.writeEndElement(); // coordinates

        xmlWriter.writeEndElement(); // gx:LatLonQuad
    }
}
