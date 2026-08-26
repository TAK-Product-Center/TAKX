package gov.takx.core.kml;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.TimeStamp;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.kml.IKmlConstants;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.CancelableStatusDialog;
import gov.takx.commons.swing.ICancelableTask;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.zip.ZipFile;

/**
 * KML Importer for importing map objects only.  Converts kml track data into RDMs and pushes them
 * through the core.
 *
 * @since 1.25 (pretty much rewritten in 5.5.1)
 */
@Singleton
public class KmlMapObjectImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IRaptorDataPortal dataPortal;
    private final IMapObjectManager mapObjectManager;
    private final INotificationService notificationService;

    @Inject
    public KmlMapObjectImporter(IRaptorDataPortal dataPortal, IMapObjectManager mapObjectManager, INotificationService notificationService)
    {
        this.dataPortal = dataPortal;
        this.mapObjectManager = mapObjectManager;
        this.notificationService = notificationService;
    }

    /**
     * Import KMZ map object data.  Uses a {@link CancelableStatusDialog} to allow the user to cancel import midstream,
     * and wires the notification up with an {@link CancelableTask}.
     *
     * @param kmzFile kmz file to import
     */
    @InvokeOnEDT
    public void importKML(final File kmzFile)
    {
        if (kmzFile.exists())
        {
            final var cancelableTask = new CancelableTask();
            final var statusDialog = new CancelableStatusDialog(cancelableTask, false);
            statusDialog.setDialogTitle("KML/KMZ Import");
            statusDialog.setStatusText("Preparing KML/KMZ Import");
            statusDialog.setProgressBounds(0, 100);
            statusDialog.setVisible(true);

            final var worker = new ImportWorker(kmzFile, statusDialog);
            cancelableTask.setWorker(worker);
            worker.start();
        }
    }

    /**
     * Cancelable Task to allow the user to cancel the import through the CancelableStatusDialog
     */
    private static class CancelableTask implements ICancelableTask
    {
        private ImportWorker worker;

        @Override
        public void onCancel()
        {
            worker.cancelImport();
        }

        public void setWorker(ImportWorker worker)
        {
            this.worker = worker;
        }
    }

    /**
     * Import worker thread.  Create Map Objects and RDMs according to the KML content.
     */
    private class ImportWorker extends Thread
    {
        private final File kmzFile;
        private final CancelableStatusDialog statusDialog;

        private ZipFile sourceZipFile;
        private IRaptorDataStructure currentDataStructure;
        private long trackPoints;
        private int imported;
        private boolean cancelled;

        private ImportWorker(File kmzFile, CancelableStatusDialog statusDialog)
        {
            this.kmzFile = kmzFile;
            this.statusDialog = statusDialog;

            setName("KML ImportWorker: " + kmzFile.getName());
        }

        @Override
        public void run()
        {
            try (var zipFile = new ZipFile(kmzFile))
            {
                sourceZipFile = zipFile;
                final var kml = KmlModel.parseModel(kmzFile).kml();

                trackPoints = countTrackPoints(kml);
                logger.debug("Counted {} number of tracks", trackPoints);

                if (trackPoints > 0)
                {
                    statusDialog.setStatusText("Importing from KML/KMZ");
                    processKmlDocument(kml);
                }

                statusDialog.setVisible(false);

                if (trackPoints == 0)
                {
                    AlwaysOnTopJOptionPane.showErrorDialog("No Map Object tracks found in " + kmzFile.getAbsolutePath(), "Error");
                } else
                {
                    notificationService.show(Notification.builder("Imported " + imported + " records successfully", Notification.Type.Success).build());
                }

            } catch (Exception e)
            {
                logger.warn("Failed to import {}", kmzFile, e);
                statusDialog.setVisible(false);
                AlwaysOnTopJOptionPane.showErrorDialog("Error importing Map Object tracks from " + kmzFile.getAbsolutePath(), "Error");
            }
        }

        /**
         * Process the kml Document.  Only the "Map Objects" Folder within the document is processed.
         */
        private void processKmlDocument(Kml kml)
        {
            if (kml.getFeature() instanceof Document kmlDoc)
            {
                for (var feature : kmlDoc.getFeature())
                {
                    if (feature instanceof Folder folder && "Map Objects".equals(folder.getName()))
                    {
                        processMapObjects(folder);
                        break;
                    }
                }
            }
        }

        /**
         * Process all the Map Object sub-folders in the given folder.
         */
        private void processMapObjects(Folder folder)
        {
            for (Feature feature : folder.getFeature())
            {
                if (cancelled) return;

                if (feature instanceof Folder subfolder) processMapObject(subfolder);
            }
        }

        /**
         * Process a folder defining a single Map Object as a Placemark, and a sub-folder of tracks.
         */
        private void processMapObject(Folder folder)
        {
            for (Feature feature : folder.getFeature())
            {
                if (cancelled) return;

                if (feature instanceof Placemark placemark)
                {
                    final String mapObjectData = getExtendedDataValue(placemark);
                    if (mapObjectData != null) createMapObject(placemark, new KmlMapObjectMetadata(mapObjectData));
                } else if (feature instanceof Folder subfolder)
                {
                    processTracks(subfolder);
                }
            }
        }

        /**
         * Get the value of the extended data from the given feature.  It will be the value of the first Data object
         * in the {@code ExtendedData} element, if any.
         */
        private static String getExtendedDataValue(Feature feature)
        {
            final var extendedData = feature.getExtendedData();
            final var dataList = extendedData != null ? extendedData.getData() : null;
            return dataList != null && !dataList.isEmpty() ? dataList.get(0).getValue() : null;
        }

        /**
         * Create a Map Object from the data in {@code mapObjectMetadata}.  The location and icon are extracted
         * from the {@code placemark} geometry and style.
         */
        private void createMapObject(Placemark placemark, KmlMapObjectMetadata mapObjectMetadata)
        {
            final String family = mapObjectMetadata.getFamily();
            final String type = mapObjectMetadata.getType();
            final String unitId = mapObjectMetadata.getUnitID();
            final String name = mapObjectMetadata.getName();
            final ILocation location = getLocation(placemark);

            logger.debug("create MO '{}' key {}/{}/{} @ {}", name, family, type, unitId, location);

            final var builder = MapObjectSpec.builder(family, type, unitId).name(name).location(location);

            final var image = getIcon(placemark);
            if (image != null) builder.initializer(mo -> mo.setExternalIconImage(image));

            mapObjectManager.getOrCreateMapObject(builder.build());

            // Build the data structure to define the generated RDMs
            currentDataStructure = new DataStructureBuilder(family, type, IKmlConstants.KML_MESSAGE_VERSION, IKmlConstants.KML_MESSAGE_TYPE).build();
        }

        /**
         * @return the location from the placemark geometry, or {@code null} if not found.
         */
        private ILocation getLocation(Placemark placemark)
        {
            if (placemark.getGeometry() instanceof Point point)
            {
                final var coordinates = point.getCoordinates();
                if (coordinates.size() == 1)
                {
                    final var coordinate = coordinates.get(0);
                    return new GeoPoint(coordinate.getLatitude(), coordinate.getLongitude(), coordinate.getAltitude());
                }
            }
            return null;
        }

        /**
         * @return the icon from the placemark style, or {@code null} if not found.
         */
        private BufferedImage getIcon(Placemark placemark)
        {
            final var styleSelectors = placemark.getStyleSelector();
            if (!styleSelectors.isEmpty() && styleSelectors.get(0) instanceof Style style)
            {
                final String href = style.getIconStyle().getIcon().getHref();
                try (var is = sourceZipFile.getInputStream(sourceZipFile.getEntry(href)))
                {
                    return ImageIO.read(is);
                } catch (IOException ignored)
                {
                }
            }
            return null;
        }

        /**
         * Process all the track points defined in the given folder, each one is a {@link Placemark}.
         */
        private void processTracks(Folder folder)
        {
            for (var feature : folder.getFeature())
            {
                if (cancelled) return;

                if (feature instanceof Placemark placemark) processTrackPlacemark(placemark);
            }
        }

        /**
         * Process a KML placemark representing a track point and send the RDM. We get the data from the ExtendedData.
         *
         * @param placemark the KMLPlacemark
         */
        private void processTrackPlacemark(Placemark placemark)
        {
            final String trackPointData = getExtendedDataValue(placemark);

            if (trackPointData != null && placemark.getGeometry() instanceof Point point)
            {
                logger.debug("Extended Placemark data: {}", trackPointData);

                final var trackPointMetadata = new KmlTrackPointMetadata(trackPointData);

                final var positions = point.getCoordinates();
                if (positions.size() == 1)
                {
                    final var position = positions.get(0);
                    logger.debug("Position = {}", position);
                    trackPointMetadata.setLocation(position);

                    if (placemark.getTimePrimitive() instanceof TimeStamp timeStamp)
                    {
                        String when = timeStamp.getWhen();
                        logger.debug("Time: {}", when);
                        trackPointMetadata.setTime(when);
                    }

                    sendRaptorDataMessage(trackPointMetadata);
                    incrementTracksImported();
                }
            }
        }

        /**
         * Send an RDM corresponding to the given metadata.
         *
         * @param point the KmlTrackPointMetadata
         * @since 3.0 (delegate parameter removed)
         */
        private void sendRaptorDataMessage(KmlTrackPointMetadata point)
        {
            var msg = currentDataStructure.createRaptorDataMessage(point.getUnitID());
            msg.setLocation(point.getLocation());
            msg.setHasMotion(point.isMotion());
            msg.setSpeedInMps(point.getSpeed());
            msg.setHeading(point.getHeading());
            msg.setTime(point.getTime());
            dataPortal.sendToGateway(msg);
        }

        /**
         * Count the number of tracks starting from the KMLRoot and traversing the folder ExtendedData
         *
         * @param kml Parsed KML model
         * @return the number of tracks in the KMLFolders under the KMLRoot
         */
        private long countTrackPoints(Kml kml)
        {
            long trackPoints = 0;

            if (kml.getFeature() instanceof Document document)
            {
                trackPoints = document.getFeature().stream()
                        .filter(feature -> feature instanceof Folder)
                        .map(Folder.class::cast)
                        .mapToLong(this::countKmlFolder).sum();
            }

            return trackPoints;
        }

        /**
         * Count the number of track points in the specified Folder and all subfolders.
         *
         * @param folder Folder to process
         * @return the number of tracks referenced in the ExtendedData
         */
        private long countKmlFolder(Folder folder)
        {
            long trackPoints = 0;

            for (var feature : folder.getFeature())
            {
                // A Placemark represents either a Map Object, or its path.  The existence of the extended data
                // indicates it's a Map Object, and holds the track count for that Map Object.
                if (feature instanceof Placemark placemark)
                {
                    final String extendedDataValue = getExtendedDataValue(placemark);
                    if (extendedDataValue != null)
                    {
                        var mapObjectMetadata = new KmlMapObjectMetadata(extendedDataValue);
                        if (mapObjectMetadata.isValidMapObject())
                        {
                            final long count = mapObjectMetadata.getTrackPointCount();
                            trackPoints += count == 0 ? 1 : count;
                        }
                    }
                } else if (feature instanceof Folder subfolder)
                {
                    trackPoints += countKmlFolder(subfolder);
                }
            }

            return trackPoints;
        }

        private void incrementTracksImported()
        {
            imported++;
            if (trackPoints > 0)
            {
                statusDialog.setProgressValue(Math.min((int) ((imported / (double) trackPoints) * 100), 100));
                statusDialog.setProgressText("Processing " + imported + " of " + trackPoints + " records");
            }
        }

        private void cancelImport()
        {
            cancelled = true;
        }
    }
}
