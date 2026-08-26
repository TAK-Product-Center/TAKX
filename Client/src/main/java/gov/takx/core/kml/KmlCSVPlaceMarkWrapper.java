package gov.takx.core.kml;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.ogc.kml.impl.KMLExportUtil;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.placemarkoverlay.CSVPlaceMark;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static gov.nasa.worldwind.ogc.kml.impl.KMLExportUtil.kmlBoolean;

/**
 * Wrapper class for exporting CSVPlaceMark objects.
 */
public class KmlCSVPlaceMarkWrapper extends AKmlWrapper
{
    private final CSVPlaceMark csvPlaceMark;

    public KmlCSVPlaceMarkWrapper(CSVPlaceMark csvPlaceMark, String kmlFolder)
    {
        super(kmlFolder);
        this.csvPlaceMark = csvPlaceMark;
    }

    /**
     * Export the CSVPlaceMark to kml.  If an image is specified, copy that to the KMZ file as well.
     *
     * @param output output writer
     * @throws IOException        exception in writing to the file
     * @throws XMLStreamException exception in writing the xml stream
     */
    @Override
    protected void exportAsKML(Object output) throws IOException, XMLStreamException
    {
        XMLStreamWriter xmlWriter = createStreamWriter(output);

        xmlWriter.writeStartElement("Placemark");
        xmlWriter.writeStartElement("name");
        xmlWriter.writeCharacters(csvPlaceMark.getLabelText());
        xmlWriter.writeEndElement();   // name

        xmlWriter.writeStartElement("visibility");
        xmlWriter.writeCharacters(kmlBoolean(csvPlaceMark.isVisible()));
        xmlWriter.writeEndElement();   // visibility

        String shortDescription = (String) csvPlaceMark.getValue(AVKey.SHORT_DESCRIPTION);
        if (shortDescription != null)
        {
            xmlWriter.writeStartElement("Snippet");
            xmlWriter.writeCharacters(shortDescription);
            xmlWriter.writeEndElement();        // snippet
        }

        String imageColumn = csvPlaceMark.getImageColumn();
        String imagePath = csvPlaceMark.getCSVMap().get(imageColumn);
        String rolloverText = (String) csvPlaceMark.getValue(AVKey.ROLLOVER_TEXT);

        xmlWriter.writeStartElement("description");
        if (!StringUtils.isEmpty(imagePath))
        {
            File imageFile = new File(imagePath);
            String imageName = imageFile.getName();
            String copyToPath = kmlFolder + File.separator + imageName;
            if (imageFile.isAbsolute())
            {
                FileUtils.copy(imageFile, new File(copyToPath));
            } else
            {
                FileUtils.copy(new File(csvPlaceMark.getDirectory(), imagePath), new File(copyToPath));
            }
            xmlWriter.writeCData(rolloverText + "<p><img src=\"" + imageName + "\"/>");
        } else
        {
            xmlWriter.writeCData(rolloverText);
        }
        xmlWriter.writeEndElement();        // description

        final PointPlacemarkAttributes normalAttributes = csvPlaceMark.getAttributes();

        // This is really the custom part where we include the image file in the kmz file.
        String imageName = new File(normalAttributes.getImageAddress()).getName();
        String copyToPath = kmlFolder + File.separator + imageName;
        FileUtils.copy(new File(normalAttributes.getImageAddress()), new File(copyToPath));

        writeStyle(xmlWriter, null, "1.5", imageName, "$[description]", false);

        // Write geometry
        xmlWriter.writeStartElement("Point");

        xmlWriter.writeStartElement("extrude");
        xmlWriter.writeCharacters(kmlBoolean(csvPlaceMark.isLineEnabled()));
        xmlWriter.writeEndElement();       // extrude

        final String altitudeMode = KMLExportUtil.kmlAltitudeMode(csvPlaceMark.getAltitudeMode());
        xmlWriter.writeStartElement("altitudeMode");
        xmlWriter.writeCharacters(altitudeMode);
        xmlWriter.writeEndElement();   // altitudeMode

        final String coordString = String.format(Locale.US, "%f,%f,%f",
                csvPlaceMark.getPosition().getLongitude().getDegrees(),
                csvPlaceMark.getPosition().getLatitude().getDegrees(),
                csvPlaceMark.getPosition().getElevation());
        xmlWriter.writeStartElement("coordinates");
        xmlWriter.writeCharacters(coordString);
        xmlWriter.writeEndElement();    // coordinates

        xmlWriter.writeEndElement(); // Point
        xmlWriter.writeEndElement(); // Placemark
        xmlWriter.flush();
    }
}
