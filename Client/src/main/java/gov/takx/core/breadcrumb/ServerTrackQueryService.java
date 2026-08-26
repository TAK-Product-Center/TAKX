package gov.takx.core.breadcrumb;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import gov.takx.core.persist.breadcrumb.Breadcrumb;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Queries TAK servers for {@link gov.takx.api.playback.IMarker} {@link gov.takx.api.breadcrumb.ITrack} data.
 *
 * @since 5.4
 */
@ApplicationScoped
public class ServerTrackQueryService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final XPathExpression PLACEMARK_PATH;
    private static final XPathExpression NAME_PATH; // relative to Placemark
    private static final XPathExpression TRACK_LIST_PATH; // relative to Placemark
    private static final XPathExpression TIME_PATH; // relative to Track
    private static final XPathExpression POSITION_PATH; // relative to Track

    static
    {
        final var xpath = XPathFactory.newInstance().newXPath();

        try
        {
            PLACEMARK_PATH = xpath.compile("/kml/Document/Placemark");
            NAME_PATH = xpath.compile("name");
            TRACK_LIST_PATH = xpath.compile("MultiTrack/Track");
            TIME_PATH = xpath.compile("when");
            POSITION_PATH = xpath.compile("coord");
        } catch (XPathExpressionException e)
        {
            throw new IllegalStateException("Failed to compile track XPath expressions", e);
        }
    }

    private final TakServerApiClient apiClient;
    private final BreadcrumbManager breadcrumbManager;
    private final ExecutorService executorService;
    private final INotificationService notificationService;
    private final IMapObjectManager mapObjectManager;
    private final String nodeUuid;

    @Inject
    ServerTrackQueryService(TakServerApiClient apiClient, BreadcrumbManager breadcrumbManager,
                            ExecutorService executorService, INotificationService notificationService,
                            IMapObjectManager mapObjectManager, @NodeUUID String nodeUuid)
    {
        this.apiClient = apiClient;
        this.breadcrumbManager = breadcrumbManager;
        this.executorService = executorService;
        this.notificationService = notificationService;
        this.mapObjectManager = mapObjectManager;
        this.nodeUuid = nodeUuid;
    }

    /**
     * Queries the provided TAK server for marker tracks and imports any missing tracks into the local data store.
     *
     * @param connection The connection to the server that should be searched
     * @param markerUid The marker's UID
     * @param start The start time boundary or {@code null} if unbounded
     * @param end The end time boundary or {@code null} if unbounded
     * @return A future that will complete when the import is complete
     */
    @Nonnull
    public CompletableFuture<Void> downloadTracks(@Nonnull ITakServerConnection connection, @Nonnull String markerUid,
                                                  @Nullable Instant start, @Nullable Instant end)
    {
        if (!connection.isConnected() || !connection.isEnabled())
        {
            showNotification("Server " + connection.getDisplayName() + " is not available", Notification.Type.Warning);
            return CompletableFuture.completedFuture(null);
        }

        // If querying the self marker, use the node UUID which is consistent across sessions and projects
        final var uid = mapObjectManager.getSelfMarker().getUid().equals(markerUid) ? nodeUuid : markerUid;

        return apiClient.trackEndpoints(connection).getTracks(uid)
                .withMultiTrackThreshold(PreferenceEntry.BREADCRUMBS_SERVER_TRACK_TIME_GAP.getIntegerValue())
                .withTimeBounds(start, end)
                .asyncGetKmlFileBytes(executorService)
                .thenAccept(kmlBytes -> {
                    try
                    {
                        if (logger.isDebugEnabled()) writeToFile(kmlBytes);

                        final var xmlDoc = SafeXmlDocumentUtils.getDocumentBuilder().parse(new ByteArrayInputStream(kmlBytes));
                        final var placemark = (Node) PLACEMARK_PATH.evaluate(xmlDoc, XPathConstants.NODE);

                        if (placemark == null)
                        {
                            showNotification("No tracks for marker found on server", Notification.Type.Info);
                            return;
                        }

                        final var name = ((Node) NAME_PATH.evaluate(placemark, XPathConstants.NODE)).getTextContent();
                        final var trackList = (NodeList) TRACK_LIST_PATH.evaluate(placemark, XPathConstants.NODESET);

                        if (trackList == null)
                        {
                            showNotification("No tracks for maker found on server", Notification.Type.Info);
                            return;
                        }

                        // Process each track
                        for (int trackIndex = 0; trackIndex < trackList.getLength(); trackIndex++)
                        {
                            final var track = (Node) trackList.item(trackIndex);
                            final var trackUid = UUID.randomUUID().toString();
                            final var whenList = (NodeList) TIME_PATH.evaluate(track, XPathConstants.NODESET);
                            final var coordList = (NodeList) POSITION_PATH.evaluate(track, XPathConstants.NODESET);

                            if (whenList.getLength() != coordList.getLength())
                            {
                                logger.error("Invalid data received from the server: \"when\" list length does not match \"coord\" list length");
                                continue;
                            }

                            final var breadcrumbList = new ArrayList<Breadcrumb>();
                            Breadcrumb previousBreadcrumb = null;

                            // Process each track breadcrumb
                            for (int breadcrumbIndex = 0; breadcrumbIndex < whenList.getLength(); breadcrumbIndex++)
                            {
                                final var when = whenList.item(breadcrumbIndex).getTextContent();
                                final var coord = coordList.item(breadcrumbIndex).getTextContent();
                                final var geoPoint = getGeoPoint(coord);
                                final var instant = safeParseInstant(when);

                                if (instant == null) continue;

                                final var breadcrumb = new Breadcrumb(geoPoint, instant, null, null, trackUid);
                                breadcrumbList.add(breadcrumb);

                                // Calculate and set the bearing on the previous track
                                if (previousBreadcrumb != null)
                                {
                                    final double bearing = GeoCalculations.bearing(previousBreadcrumb.getGeoPointMetaData().get(), geoPoint.get());
                                    previousBreadcrumb.setBearing(bearing);
                                }

                                previousBreadcrumb = breadcrumb;
                            }

                            breadcrumbManager.addTrackFromServer(trackUid, markerUid, name, breadcrumbList);
                        }
                    } catch (IOException | XPathExpressionException | SAXException | ParserConfigurationException e)
                    {
                        logger.error("Error processing server track data", e);
                        showNotification("Error processing server track data", Notification.Type.Warning);
                    }
                });
    }

    /**
     * @param instantString The instant String to parse
     * @return The Instant or {@code null} if there was an error parsing the String
     */
    @Nullable
    private Instant safeParseInstant(@Nonnull String instantString)
    {
        try
        {
            return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(instantString));
        } catch (DateTimeParseException e)
        {
            logger.error("Error parsing Instant String {}", instantString);
            return null;
        }
    }

    /**
     * @param message The notification message
     */
    private void showNotification(@Nonnull String message, @Nonnull Notification.Type type)
    {
        notificationService.show(Notification.builder(message).setType(type).build());
    }

    /**
     * Converts a {@code gx:coord} String to a {@link GeoPointMetaData} object.
     *
     * @param coord The coordinate String with the format "lon lat"
     * @return The point object
     */
    @Nonnull
    private GeoPointMetaData getGeoPoint(@Nonnull String coord)
    {
        final var coordinates = coord.split(" ");
        final var geoPoint = GeoPoint.parseGeoPoint(coordinates[1] + "," + coordinates[0]);

        return GeoPointMetaData.wrap(geoPoint);
    }

    /**
     * @param kmlBytes The bytes to write to a temp file for debugging
     */
    private void writeToFile(@Nonnull byte[] kmlBytes)
    {
        try
        {
            final var tempFile = File.createTempFile("server-track", ".kml");
            Files.write(tempFile.toPath(), kmlBytes);
        } catch (IOException e)
        {
            logger.debug("Error writing track data to file", e);
        }
    }
}
