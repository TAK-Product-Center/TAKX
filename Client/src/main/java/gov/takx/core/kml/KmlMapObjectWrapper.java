package gov.takx.core.kml;

import gov.tak.api.maps.PointMapItem;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.device.IKmlMetadataProvider;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.commons.date.DateFormatSet;
import gov.takx.commons.file.FileUtils;
import gov.takx.platform.kml.KmzUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.tracks.TrackShape;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nullable;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Random;

/**
 * Wrapper class to export Map Objects to KML
 */
public class KmlMapObjectWrapper extends AKmlWrapper
{
    private static final SimpleDateFormat KML_DATETIME_FORMAT = new SimpleDateFormat(DateFormatSet.ISO_8601_DATE_FORMAT_Z);
    private static final SimpleDateFormat KML_NAME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final IMapObject mapObject;
    private final IMapObjectController controller;
    private final IQueryService queryService;
    private final IQueryConstraints queryConstraints;
    private final String mapObjectName;
    private final CoordinateSystem coordinateSystem = UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class);

    /**
     * Determines if a Map Object is exportable as KML using this wrapper.
     *
     * @param mapObject the Map Object to test
     * @return {@code true} if exportable, {@code false} if not
     * @since 5.5.1
     */
    public static boolean isSupported(@Nullable IMapObject mapObject)
    {
        return mapObject instanceof PointMapItem;
    }

    public KmlMapObjectWrapper(IMapObjectController controller, String kmlFolder)
    {
        super(kmlFolder);
        this.controller = controller;
        mapObject = controller.getMapObject();
        mapObjectName = mapObject.getName();
        queryService = controller.getQueryService();
        queryConstraints = queryService.createEmptyConstraints();
        queryConstraints.setMaxMessageCount(-1);
    }

    @Override
    public String isExportFormatSupported(String format)
    {
        return mapObject instanceof PointMapItem ? super.isExportFormatSupported(format) : FORMAT_NOT_SUPPORTED;
    }

    public IMapObject getMapObject()
    {
        return mapObject;
    }

    /**
     * Export map object state and description to kml using the specified output
     *
     * @param output output writer
     * @throws IOException        exception in writing to the file
     * @throws XMLStreamException exception in writing the xml stream
     */
    @Override
    protected void exportAsKML(Object output) throws IOException, XMLStreamException
    {
        XMLStreamWriter xmlWriter = createStreamWriter(output);

        xmlWriter.writeStartElement("Folder");
        xmlWriter.writeStartElement("name");
        xmlWriter.writeCharacters(mapObjectName);
        xmlWriter.writeEndElement();

        exportIcon(xmlWriter);

        if (mapObject instanceof IIconMapObject) exportTracks(xmlWriter);

        if (mapObject instanceof IFileMapObject fileMapObject)
        {
            File file = fileMapObject.getVfsFile().getFile();
            File copyFile = new File(kmlFolder, file.getName());
            FileUtils.copy(file, copyFile);
        }

        xmlWriter.writeEndElement();
        xmlWriter.flush();
    }

    /**
     * Export the Map Object icon image to KML
     *
     * @param xmlWriter output writer
     * @throws IOException        exception thrown on io
     * @throws XMLStreamException xml stream exception
     */
    private void exportIcon(XMLStreamWriter xmlWriter) throws IOException, XMLStreamException
    {
        xmlWriter.writeStartElement("Placemark");

        xmlWriter.writeStartElement("name");
        xmlWriter.writeCharacters(mapObjectName);
        xmlWriter.writeEndElement();

        if (mapObject instanceof IFileMapObject fileMapObject)
        {
            xmlWriter.writeStartElement("description");
            StringBuilder sb = new StringBuilder();
            if (mapObject.getDescription() != null)
            {
                sb.append(mapObject.getDescription());
            }
            if (fileMapObject.isPreviewImageAvailable())
            {
                sb.append("<p><img src=\"").append(fileMapObject.getVfsFile().getFile().getName()).append("\"/>");
            }
            xmlWriter.writeCData(sb.toString());
            xmlWriter.writeEndElement();
        } else // IconMapObject
        {
            // put into cdata in case the user enters illegal xml chars in the description
            xmlWriter.writeStartElement("description");
            var pluginController = controller.getPluginController();
            if (pluginController instanceof IKmlMetadataProvider provider)
            {
                xmlWriter.writeCData(provider.getKmlDescription());
            } else
            {
                xmlWriter.writeCData(getDescriptionString(mapObject));
            }
            xmlWriter.writeEndElement();
        }

        writeExtendedData(xmlWriter, KmlMapObjectMetadata.makeRestorableString(mapObject, queryService.count(queryConstraints)));

        writeStyle(xmlWriter, null, "1.0", KmzUtils.getMapObjectImageKey(mapObject), "$[name]<p>$[description]", false);

        writePoint(xmlWriter, mapObject.getLocation());

        // write the icon to a png file
        File outputFile = new File(kmlFolder, KmzUtils.getMapObjectImageKey(mapObject));
        ImageIO.write(mapObject.getIconImage(), "png", outputFile);
    }

    /**
     * Export the tracks for the map object as track points plus connected lines.  Custom TSDs are not yet
     * supported.
     *
     * @param xmlWriter output writer
     * @throws IOException        exception thrown on io
     * @throws XMLStreamException xml stream exception
     */
    private void exportTracks(XMLStreamWriter xmlWriter) throws IOException, XMLStreamException
    {
        // We have to use a unique track shape filename every time because GE will cache data from a KMZ file that
        // it opens, and if you overwrite an existing KMZ file, it will ignore the new version of the
        // png that is inside the updated KMZ file.  Go figure.
        Random generator = new Random();
        String trackShapeFilename = mapObject.getType() + generator.nextLong() + ".png";  // just needs to be unique
        String styleName = mapObject.getType() + generator.nextLong() + "style";          // again... unique style name

        // write the style for the placemarks
        writeStyle(xmlWriter, styleName, "0.4", trackShapeFilename, mapObjectName + "<p>$[description]", true);

        xmlWriter.writeStartElement("Folder");
        xmlWriter.writeStartElement("name");
        xmlWriter.writeCharacters(mapObjectName + " tracks");
        xmlWriter.writeEndElement();

        var lineString = new StringBuilder();
        for (var msg : queryService.query(queryConstraints))
        {
            if (!msg.isManuallyGeneratedLocation() && msg.hasLocation())
            {
                xmlWriter.writeStartElement("Placemark");

                final String name = KML_NAME_FORMAT.format(msg.getRelevantTime());
                xmlWriter.writeStartElement("name");
                xmlWriter.writeCharacters(name);
                xmlWriter.writeEndElement();

                writeExtendedData(xmlWriter, KmlTrackPointMetadata.makeRestorableString(msg));

                xmlWriter.writeStartElement("description");
                xmlWriter.writeCharacters(name + " (GMT)");
                xmlWriter.writeEndElement();

                xmlWriter.writeStartElement("styleUrl");
                xmlWriter.writeCharacters("#" + styleName);
                xmlWriter.writeEndElement();

                xmlWriter.writeStartElement("TimeStamp");
                xmlWriter.writeStartElement("when");
                xmlWriter.writeCharacters(KML_DATETIME_FORMAT.format(msg.getRelevantTime()));
                xmlWriter.writeEndElement();
                xmlWriter.writeEndElement();

                final var msgLocation = msg.getLocation();
                writePoint(xmlWriter, msgLocation);

                lineString.append(msgLocation.getLongitude()).append(',').append(msgLocation.getLatitude());
                if (msgLocation.hasElevation()) lineString.append(',').append(msgLocation.getElevation());
                lineString.append(' ');
            }
        }
        xmlWriter.writeEndElement();    // Folder

        xmlWriter.writeStartElement("Placemark");

        xmlWriter.writeStartElement("name");
        xmlWriter.writeCharacters(mapObject.getName() + " path");
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("Style");
        xmlWriter.writeStartElement("LineStyle");
        xmlWriter.writeStartElement("color");

        final ITrackStyleDefinition tsd = controller.getTrackStyleDefinition();

        Color color = tsd != null ? tsd.getTrackShapeConstantColor() : null;
        if (color == null) color = mapObject.getDefaultTrackColor();
        xmlWriter.writeCharacters(KmzUtils.formatKmlColor(color));
        xmlWriter.writeEndElement();
        xmlWriter.writeStartElement("width");
        xmlWriter.writeCharacters("3");
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("LineString");

        xmlWriter.writeStartElement("tessellate");
        xmlWriter.writeCharacters("1");
        xmlWriter.writeEndElement();

        // TODO: Use correct altitude mode...
        xmlWriter.writeStartElement("altitudeMode");
        xmlWriter.writeCharacters("clampToGround");
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("coordinates");
        xmlWriter.writeCharacters(lineString.toString());
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement();    // LineString
        xmlWriter.writeEndElement();    // Placemark

        // write the track shape to a png file
        TrackShape shape = tsd != null ? tsd.getTrackShape() : TrackShape.Sphere;
        BufferedImage imageSource = createTrackPointImage(color, shape);
        File outputFile = new File(kmlFolder, trackShapeFilename);
        ImageIO.write(imageSource, "png", outputFile);
    }

    /**
     * Get Description string for a map object (icon) for use in balloon text.
     *
     * @param mo map object to get description string for
     * @return description string
     */
    private String getDescriptionString(IMapObject mo)
    {
        StringBuilder sb = new StringBuilder(256);
        ILocation location = mo.getLocation();
        long lastTime = mo.getMostRecentCommunicationTime();

        try
        {
            String locationText = CoordinateSystem.getLocationString(new RaptorLocation(location.getLatitude(), location.getLongitude()), coordinateSystem);
            String elevationText = AltitudeUnits.METERS.format(location.getElevation(), 1);
            locationText = locationText.replace("°", "");  // WW KML parser can't parse these...

            sb.append(KML_NAME_FORMAT.format(lastTime)).append(" (GMT)")
                    .append("<br>" + "Elevation: ").append(elevationText)
                    .append("<br>").append(locationText);
        } catch (Exception e)
        {
            sb.append(KML_NAME_FORMAT.format(lastTime)).append(" (GMT)")
                    .append("<br>").append("Elevation: ").append(location.getElevation())
                    .append("<br>").append("Lat: ").append(location.getLatitude())
                    .append("<br>").append("Lon: ").append(location.getLongitude());
        }
        return sb.toString();
    }

    /**
     * Create a track point object.  Basically either a rectangle for cubes or a sphere for all others.
     *
     * @param color track point color
     * @param shape track point shape
     * @return a buffered image for the track point.
     */
    private BufferedImage createTrackPointImage(Color color, TrackShape shape)
    {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        if (shape == TrackShape.Cube)
        {
            g2d.fillRect(0, 0, 32, 32);
        } else
        {
            g2d.fillOval(0, 0, 32, 32);
        }
        g2d.dispose();
        return image;
    }
}
