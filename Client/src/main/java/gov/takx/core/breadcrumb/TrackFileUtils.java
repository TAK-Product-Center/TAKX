package gov.takx.core.breadcrumb;

import au.com.bytecode.opencsv.CSVWriter;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Container;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.gx.SimpleArrayData;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.commons.xml.kml.DefaultNamespaceKml;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Provides utility functions for converting {@link ITrack tracks} to various file types.
 * 
 * @since 5.4
 */
@ApplicationScoped
public class TrackFileUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NEW_LINE = "\n";
    private static final String HEADER = "UID, CallSign, TimeStamp (unix epoch), Latitude (dec deg), Longitude (dec deg), HAE Altitude (m), "
            + "Circular Error (m), Linear Error (m), Bearing, Speed (m/s), Pt Source, Alt Source, AsText(Point_Geom)";
    private static final String ATAK_KML_TRACK_EXTENDED_SCHEMA = "trackschema";
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    private final BreadcrumbManager breadcrumbManager;

    @Inject
    TrackFileUtils(BreadcrumbManager breadcrumbManager)
    {
        this.breadcrumbManager = breadcrumbManager;
    }

    /**
     * @param track The track to export
     * @param exportFile The file where data should be written
     * @throws IOException thrown if there is an error writing to the provided file
     */
    public boolean exportTrackCSV(@Nonnull ITrack track, @Nonnull File exportFile) throws IOException
    {
        try (OutputStream os = IOProviderFactory.getOutputStream(exportFile))
        {
            final StringWriter writer = new StringWriter();
            final CSVWriter csv = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);

            Instant previousBreadcrumbTime = null;

            for (IBreadcrumb breadcrumb : breadcrumbManager.getTrackBreadcrumbs(track))
            {
                final var geoPointMetadata = breadcrumb.getGeoPointMetaData();
                final var geoPoint = geoPointMetadata.get();

                if (!geoPoint.isValid()) continue;

                final var latitude = geoPoint.getLatitude();
                final var longitude = geoPoint.getLongitude();
                final var time = breadcrumb.getTime();

                // Skip duplicate timestamps, e.g. from segment stitching
                if (time.equals(previousBreadcrumbTime)) continue;

                csv.writeNext(new String[]{
                        track.getMarkerUid(),
                        track.getCallsign(),
                        String.valueOf(time.toEpochMilli()),
                        String.valueOf(latitude),
                        String.valueOf(longitude),
                        geoPoint.isAltitudeValid() ? String.valueOf(geoPoint.getAltitude()) : "",
                        String.valueOf(getValueOrZero(geoPoint.getCE())),
                        String.valueOf(getValueOrZero(geoPoint.getLE())),
                        String.valueOf(getValueOrZero(breadcrumb.getBearing())),
                        String.valueOf(getValueOrZero(breadcrumb.getSpeedInMps())),
                        getSourceOrEmpty(geoPointMetadata.getGeopointSource()),
                        getSourceOrEmpty(geoPointMetadata.getAltitudeSource()),
                        "MakePoint(" + longitude + ", " + latitude + ", 4326)" // From ATAK; EPSG code for coordinates
                });

                previousBreadcrumbTime = time;
            }

            String output = (PreferenceEntry.BREADCRUMBS_CSV_EXPORT_HEADER.getBooleanValue() ? HEADER + NEW_LINE : "") + writer;
            os.write(output.getBytes(FileSystemUtils.UTF8_CHARSET));
        }

        return true;
    }

    /**
     * @param track The track to export
     * @param exportFile The file where data should be written
     * @return {@code true} if the file was successfully exported, {@code false} if it failed
     * @throws IOException thrown if there is an error writing to the provided file
     */
    public boolean exportTrackKml(@Nonnull ITrack track, @Nonnull File exportFile) throws IOException
    {
        final var kml = buildTrackKml(track);
        return kml.marshal(exportFile);
    }

    /**
     * @param track The track to export
     * @param exportFile The file where data should be written
     * @throws IOException thrown if there is an error writing to the provided file
     */
    public boolean exportTrackKmz(@Nonnull ITrack track, @Nonnull File exportFile) throws IOException
    {
        final var kml = buildTrackKml(track);

        // Add KML file to KMZ zip
        try (ZipOutputStream zos = FileSystemUtils.getZipOutputStream(exportFile))
        {
            zos.putNextEntry(new ZipEntry("doc.kml"));
            if (!kml.marshal(zos))
            {
                // The KML file couldn't be marshalled, so we need to delete the empty zip file we ended up creating.
                zos.close();
                exportFile.delete();
                return false;
            }
            zos.closeEntry();
        }

        return true;
    }

    /**
     * @param track The track to export
     * @return KML model of the track
     */
    @Nonnull
    private Kml buildTrackKml(@Nonnull ITrack track)
    {
        return PreferenceEntry.BREADCRUMBS_KML_EXPORT_TIMESTAMPS.getBooleanValue()
                ? buildTrackKmlWithTimestamps(track)
                : buildTrackKmlNoTimestamps(track);
    }

    /**
     * @param track The track to export
     * @return KML model of the track
     * @since 5.5
     */
    @Nonnull
    private Kml buildTrackKmlWithTimestamps(@Nonnull ITrack track)
    {
        final boolean clampToGround = PreferenceEntry.BREADCRUMBS_KML_EXPORT_CLAMP_TO_GROUND.getBooleanValue();

        var callsign = track.getCallsign();
        callsign = callsign == null || callsign.isEmpty() ? "(unknown callsign)" : callsign;
        final var trackName = track.getName();
        final var placemarkName = callsign + " " + trackName;
        final var startTimeString = getShortInstantString(track.getStartTime());

        final var kml = new DefaultNamespaceKml();
        final var document = kml.createAndSetDocument()
                .withName(trackName)
                .withDescription("Generated by TAKX " + PlatformVersion.CURRENT_VERSION_STRING + " on " + getShortInstantString(Instant.now()))
                .withOpen(true);

        final var style = addStyle(document, track, false);
        final var styleIdString = "#" + style.getId();

        final var schema = document.createAndAddSchema();
        schema.withId(ATAK_KML_TRACK_EXTENDED_SCHEMA);

        schema.createAndAddSimpleField().withDisplayName("Speed").withName("speed").withType("double");
        schema.createAndAddSimpleField().withDisplayName("Circular Error (m)").withName("ce").withType("double");
        schema.createAndAddSimpleField().withDisplayName("Linear Error (m)").withName("le").withType("double");

        // Tracks (folder) > Placemark > Track
        final var kmlTrack = document.createAndAddFolder().withName("Tracks").createAndAddPlacemark()
                .withId(track.getId() + track.getName()) // Matching ATAK: DB id + track name
                .withName(placemarkName)
                .withDescription("Track for " + callsign + " beginning at " + startTimeString)
                .withStyleUrl(styleIdString)
                .createAndSetTrack()
                .withAltitudeMode(clampToGround ? AltitudeMode.CLAMP_TO_GROUND : AltitudeMode.ABSOLUTE);

        // Track details: when, coord, angles + extended data > schema data: speed, CE, LE, point src, alt src
        final SimpleArrayData speedList = new SimpleArrayData().withName("speed");
        final SimpleArrayData ceList = new SimpleArrayData().withName("ce");
        final SimpleArrayData leList = new SimpleArrayData().withName("le");
        final SimpleArrayData geoPointSrcList = new SimpleArrayData().withName("geopointsrc");
        final SimpleArrayData altitudeSourceList = new SimpleArrayData().withName("altsrc");
        final var breadcrumbs = breadcrumbManager.getTrackBreadcrumbs(track);

        breadcrumbs.forEach(breadcrumb -> {
            final var geoPointMetaData = breadcrumb.getGeoPointMetaData();
            final var geoPoint = geoPointMetaData.get();

            kmlTrack.addToWhen(getShortInstantString(breadcrumb.getTime()));
            kmlTrack.addToCoord(getCoordinateString(geoPoint, clampToGround));
            kmlTrack.addToAngles(decimalFormat.format(getValueOrZero(breadcrumb.getBearing())));

            speedList.addToValue(decimalFormat.format(getValueOrZero(breadcrumb.getSpeedInMps())));
            ceList.addToValue(decimalFormat.format(getValueOrZero(geoPoint.getCE())));
            leList.addToValue(decimalFormat.format(getValueOrZero(geoPoint.getLE())));
            geoPointSrcList.addToValue(geoPointMetaData.getGeopointSource());
            altitudeSourceList.addToValue(geoPointMetaData.getAltitudeSource());
        });

        final var schemaData = kmlTrack.createAndSetExtendedData().createAndAddSchemaData()
                .withSchemaUrl("#" + ATAK_KML_TRACK_EXTENDED_SCHEMA);

        final var schemaDataExtensions = new ArrayList<>();
        schemaDataExtensions.add(speedList);
        schemaDataExtensions.add(ceList);
        schemaDataExtensions.add(leList);
        schemaDataExtensions.add(geoPointSrcList);
        schemaDataExtensions.add(altitudeSourceList);
        schemaData.setSchemaDataExtension(schemaDataExtensions);

        // Add the track start checkpoint
        final var startGeoPoint = breadcrumbs.get(0).getGeoPointMetaData().get();

        document.createAndAddFolder().withName("Checkpoints").createAndAddPlacemark()
                .withId(UUID.randomUUID().toString())
                .withName(placemarkName)
                .withDescription("Start of track for " + callsign + " beginning at " + startTimeString)
                .withStyleUrl(styleIdString)
                .withTimePrimitive(KmlFactory.createTimeStamp().withWhen(startTimeString))
                .createAndSetPoint()
                .withAltitudeMode(clampToGround ? AltitudeMode.CLAMP_TO_GROUND : AltitudeMode.ABSOLUTE)
                .withCoordinates(List.of(KmlFactory.createCoordinate(startGeoPoint.getLongitude(),
                        startGeoPoint.getLatitude(), clampToGround ? 0 : startGeoPoint.getAltitude())));

        return kml;
    }

    /**
     * Exports a {@link ITrack track} to KML using {@code LineString} without using {@code gx:timestamp} for broader
     * compatibility. ATAK converts the track to a route before exporting to KML, and some of that is reflected in this
     * method for consistency.
     *
     * @param track The track to export
     * @return KML model of the track
     * @since 5.5
     */
    @Nonnull
    private Kml buildTrackKmlNoTimestamps(@Nonnull ITrack track)
    {
        final var clampToGround = PreferenceEntry.BREADCRUMBS_KML_EXPORT_CLAMP_TO_GROUND.getBooleanValue();
        final var trackName = track.getName();
        final var visible = track.isVisible();

        final var kml = new DefaultNamespaceKml();
        final var folder = kml.createAndSetFolder().withName(trackName);

        final var style = addStyle(folder, track, true);
        final var styleIdString = "#" + style.getId();

        // Holds the path in a LineString
        final var pathPlacemark = folder.createAndAddPlacemark()
                .withId(track.getUid() + trackName + " Path")
                .withName(trackName + " Path")
                .withStyleUrl(styleIdString)
                .withVisibility(visible);

        // Parity with ATAK that converts the track to a route with these defaults before exporting
        final var extendedData = pathPlacemark.createAndSetExtendedData();
        extendedData.createAndAddData("Ascending Check Points").withName("RouteOrder");
        extendedData.createAndAddData("Driving").withName("RouteMethod");
        extendedData.createAndAddData("Infil").withName("RouteDirection");
        extendedData.createAndAddData("Primary").withName("RouteType");
        extendedData.createAndAddData("Infil").withName("PlanningMethod");

        final var breadcrumbs = breadcrumbManager.getTrackBreadcrumbs(track);
        final var breadcrumbCount = breadcrumbs.size();

        var index = 0;
        GeoPoint previousPoint = null;
        final var coordinates = new ArrayList<Coordinate>();

        // Convert to coordinates, skipping points that are less than 1m apart
        for (var breadcrumb : breadcrumbs)
        {
            final var point = breadcrumb.getGeoPointMetaData().get();

            // Include first and last points and any others that are at least 1m apart
            if (previousPoint == null || (index == breadcrumbCount - 1) || GeoCalculations.slantDistance(previousPoint, point) >= 1)
            {
                coordinates.add(getCoordinate(point, clampToGround));
                previousPoint = point;
                index++;
            }
        }

        if (logger.isDebugEnabled())
        {
            final var skipped = breadcrumbCount - coordinates.size();
            if (skipped > 0) logger.debug("Skipped {} breadcrumbs that were less than 1m apart", skipped);
        }

        pathPlacemark.createAndSetLineString()
                .withAltitudeMode(clampToGround ? AltitudeMode.CLAMP_TO_GROUND : AltitudeMode.ABSOLUTE)
                .withCoordinates(coordinates);

        // Holds the start and end checkpoints in Placemarks
        final var checkpointsFolder = folder.createAndAddFolder()
                .withName(trackName + " Checkpoints");

        // First point
        checkpointsFolder.createAndAddPlacemark()
                .withId(UUID.randomUUID().toString())
                .withName(trackName + " SP")
                .withStyleUrl(styleIdString)
                .withVisibility(visible)
                .createAndSetPoint()
                .withAltitudeMode(clampToGround ? AltitudeMode.CLAMP_TO_GROUND : AltitudeMode.ABSOLUTE)
                .withCoordinates(List.of(coordinates.get(0)));

        // Last point
        checkpointsFolder.createAndAddPlacemark()
                .withId(UUID.randomUUID().toString())
                .withName("VDO")
                .withStyleUrl(styleIdString)
                .withVisibility(visible)
                .createAndSetPoint()
                .withAltitudeMode(clampToGround ? AltitudeMode.CLAMP_TO_GROUND : AltitudeMode.ABSOLUTE)
                .withCoordinates(List.of(coordinates.get(coordinates.size() - 1)));

        return kml;
    }

    /**
     * @param container The document or folder to which the style should be added
     * @param track The track
     * @param includeIcon Include an icon HREF in the icon style
     * @return A style that was added to the container
     */
    @Nonnull
    private Style addStyle(@Nonnull Container container, @Nonnull ITrack track, boolean includeIcon)
    {
        final var colorHexString = Integer.toHexString(track.getColor()).toUpperCase();
        final var lineStyle = KmlFactory.createLineStyle().withColor(colorHexString).withWidth(4);
        final var iconStyle = KmlFactory.createIconStyle().withColor(colorHexString).withScale(1);

        // Matches ATAK's default marker icon
        if (includeIcon) iconStyle.createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/wht-pushpin.png");

        return container.createAndAddStyle()
                .withId(String.valueOf(1 + lineStyle.getColor().hashCode() + Double.hashCode(lineStyle.getWidth()) + iconStyle.hashCode()))
                .withLineStyle(lineStyle)
                .withIconStyle(iconStyle);
    }

    /**
     * @param value The value to check
     * @return 0 if the value is {@code null} or {@code Double.NaN}, otherwise the value
     */
    private double getValueOrZero(@Nullable Double value)
    {
        return value == null || Double.isNaN(value) ? 0d : value;
    }

    /**
     * @param value The {@link GeoPointMetaData} source value to check
     * @return An empty String if the value is {@code null} or {@link GeoPointMetaData#UNKNOWN}, otherwise the value
     */
    private String getSourceOrEmpty(@Nullable String value)
    {
        return GeoPointMetaData.UNKNOWN.equals(value) ? "" : value;
    }

    /**
     * Returns an Instant's string value without nanoseconds.
     *
     * @param instant The Instant to convert
     * @return The Instant's time string with no nanoseconds 
     */
    private String getShortInstantString(@Nonnull Instant instant)
    {
        return instant.with(ChronoField.NANO_OF_SECOND, 0).toString();
    }

    /**
     * @param geoPoint The point to convert to a coordinate string
     * @param excludeAltitude Boolean indicating if altitude should be excluded form the coordinate
     * @return The coordinate string with spaces instead of commas
     */
    private String getCoordinateString(@Nonnull GeoPoint geoPoint, boolean excludeAltitude)
    {
        return getCoordinate(geoPoint, excludeAltitude).toString().replace(',', ' ');
    }

    /**
     * @param geoPoint The point to convert to a coordinate string
     * @param excludeAltitude Boolean indicating if altitude should be excluded form the coordinate
     * @return The coordinate string with spaces instead of commas
     * @since 5.5
     */
    private Coordinate getCoordinate(@Nonnull GeoPoint geoPoint, boolean excludeAltitude)
    {
        return new Coordinate(geoPoint.getLongitude(), geoPoint.getLatitude(),
                !excludeAltitude && geoPoint.isAltitudeValid() ? geoPoint.getAltitude() : 0d);
    }
}
