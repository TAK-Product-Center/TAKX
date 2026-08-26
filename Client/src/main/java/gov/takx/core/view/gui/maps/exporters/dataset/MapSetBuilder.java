package gov.takx.core.view.gui.maps.exporters.dataset;

import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import gov.nasa.worldwind.geom.Sector;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.dataset.DataSetBuilderParameters;
import gov.takx.core.globe.tak.mapsetbuilder.DownloadAndCacheService;
import gov.takx.core.globe.tak.mapsetbuilder.DownloadAndCacheService.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages the collection of {@link DatasetDescriptor}s required to build a new map set.
 *
 * @since 1.43
 */
public class MapSetBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The name of the map set.
     */
    private final String dataSetName;

    /**
     * The location where the map data will be stored.
     */
    private final String targetDir;

    /**
     * The sources from which the data set will be built.
     */
    private final List<DataSetBuilderParameters> sources;

    /**
     * Progress monitor.
     */
    private final IImportExportProgressMonitor progressMonitor;

    /**
     * Tracks if this builder has been forcefully cancelled.
     */
    private boolean cancelled;

    private final DownloadAndCacheService downloadAndCacheService;

    /**
     * Constructor.
     *
     * @param dataSetName     The map data set name.
     * @param targetDir       The location where the map data set will be stored.
     * @param sources         The source data.
     * @param progressMonitor The progress monitor that will be updated with the build progress.
     */
    public MapSetBuilder(String dataSetName, String targetDir, List<DataSetBuilderParameters> sources,
                         IImportExportProgressMonitor progressMonitor)
    {
        this.dataSetName = dataSetName;
        this.targetDir = targetDir;
        this.sources = Collections.unmodifiableList(sources);
        this.progressMonitor = progressMonitor;
        downloadAndCacheService = new DownloadAndCacheService();
    }

    /**
     * Build the map data set.
     *
     * @param result The progress result object that will be updated with success or failure info.
     */
    public void build(ImportExportResult result)
    {
        boolean successfulImport = false;
        try
        {
            new File(targetDir).mkdirs();
            for (DataSetBuilderParameters parameters : sources)
            {
                Sector sector = parameters.boundary().getSector();
                GeoPoint upperLeft = new GeoPoint(sector.getMaxLatitude().degrees, sector.getMinLongitude().degrees);
                GeoPoint lowerRight = new GeoPoint(sector.getMinLatitude().degrees, sector.getMaxLongitude().degrees);
                double minRes = parameters.lowResolution() != null ? parameters.lowResolution() : Double.MAX_VALUE;
                double maxRes = parameters.hiResolution() != null ? parameters.hiResolution() : 0;
                progressMonitor.setSectorOfInterest(sector);

                if (parameters.datasetDescriptors() != null)
                {
                    for (DatasetDescriptor datasetDescriptor : parameters.datasetDescriptors())
                    {
                        String cacheUri = new File(targetDir, FileUtils.sanitizeForFilename(datasetDescriptor.getName() + ".sqlite")).getAbsolutePath();
                        JobStatus status = downloadAndCacheService.startDownload(upperLeft, lowerRight, datasetDescriptor.getName(), datasetDescriptor.getUri(), minRes, maxRes, cacheUri, progressMonitor);
                        if (status == JobStatus.COMPLETE) successfulImport = true;
                        if (cancelled) break;
                    }
                }
                if (parameters.elevationSource() != null)
                {
                    if (parameters.elevationSource() instanceof Controls controls)
                    {
                        TileClient tileClient = controls.getControl(TileClient.class);
                        if (tileClient != null)
                        {
                            ILocation[] geometry = {
                                    new GeoPoint(upperLeft),
                                    new GeoPoint(upperLeft.getLatitude(), lowerRight.getLongitude()),
                                    new GeoPoint(lowerRight),
                                    new GeoPoint(lowerRight.getLatitude(), upperLeft.getLongitude()),
                                    new GeoPoint(upperLeft),
                            };
                            String cacheUri = new File(targetDir, FileUtils.sanitizeForFilename(tileClient.getName() + ".sqlite")).getAbsolutePath();
                            JobStatus status = downloadAndCacheService.startDownload(geometry, tileClient.getName(), tileClient, minRes, maxRes, cacheUri, progressMonitor);
                            if (status == JobStatus.COMPLETE) successfulImport = true;
                            if (cancelled) break;
                        }
                    }
                }
                if (cancelled) break;
            }
        } catch (Exception e)
        {
            logger.error("Error creating data set in directory {}", targetDir);
        } finally
        {
            result.setSuccess(cancelled || successfulImport);
        }
    }

    /**
     * Retry on building the map data set with only the futures that are incomplete
     *
     * @param result The progress result object that will be updated with success or failure info.
     */
    public void retry(final ImportExportResult result)
    {
        stop();
        cancelled = false;

        long connectionWaitTime = 30; // seconds waiting till next reconnect
        long totalTimeOutMs = TimeUnit.MINUTES.toMillis(10);
        long totalTimeOutExpiredMs = System.currentTimeMillis() + totalTimeOutMs;

        while (System.currentTimeMillis() < totalTimeOutExpiredMs)
        {
            if (cancelled)
            {
                result.setMessage("User Cancelled");

                // A successful result tells the user that the process was completed and prevents a dialog from warning the
                // user that the data could not be retrieved
                result.setSuccess(true);
                return;
            }

            if (testTileURLConnection())
            {
                logger.info("Connection is valid.");

                // CORE-4231:  There's a weird period where the connection is valid, but the network still can't
                // pull tiles right away.  So we are waiting for 90 seconds before executing the build
                waitFor(90);
            } else
            {
                logger.warn("Connection is invalid.");
                waitFor(connectionWaitTime);
            }
        }

        build(result);
        if (result.getSuccess())
        {
            logger.info("Successfully built MapSet: {}", dataSetName);
        }
    }

    private void waitFor(long seconds)
    {
        // Wait for a short period of time before looping back to test the connection
        try
        {
            logger.info("Sleeping for {} seconds.", seconds);

            // Sleep in 1 second increments to prevent delays if a user cancels the task
            for (int i = 0; i < seconds; i++)
            {
                if (cancelled)
                {
                    break;
                }

                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException e)
        {
            logger.warn("Failed to Sleep for {} seconds", seconds);
        }
    }

    /**
     * Attempt to force all the builder threads to stop immediately.
     */
    public void stop()
    {
        cancelled = true;
        downloadAndCacheService.cancelDownload();
    }

    private boolean testTileURLConnection()
    {
        for (DataSetBuilderParameters parameters : sources)
        {
            for (DatasetDescriptor datasetDescriptor : parameters.datasetDescriptors())
            {
                DownloadAndCacheService.checkConnectivity(datasetDescriptor);
            }
        }
        return true;
    }
}
