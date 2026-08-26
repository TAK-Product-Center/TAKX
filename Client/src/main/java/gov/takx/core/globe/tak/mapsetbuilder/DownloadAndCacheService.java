package gov.takx.core.globe.tak.mapsetbuilder;

import android.os.SystemClock;
import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequest.CacheMode;
import com.atakmap.map.contentservices.CacheRequestListener;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.osm.OSMDroidTileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import com.atakmap.map.layer.raster.tilematrix.TileClientSpi;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import com.atakmap.map.layer.raster.tilematrix.TileContainerSpi;
import com.atakmap.map.layer.raster.tilematrix.TileScraper;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * Helper class to download offline imagery. This is copied from ATAK with some editing.
 *
 * @since 5.0
 */
@PseudoBean
public class DownloadAndCacheService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // legacy method used if 1
    private static final int NUM_DOWNLOAD_THREADS = 4;

    /**
     * The current status of the running job
     */
    public enum JobStatus
    {
        CONNECTING, DOWNLOADING, COMPLETE, CANCELLED, ERROR
    }

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    private CacheRequest currentRequest = null;
    protected int queuedDownloads = 0;

    protected int currentProgress = 0;
    protected int secondaryProgress = 0;
    protected int maxProgress = 0;

    /**
     * Cancel any running download on another thread.
     */
    public void cancelDownload()
    {
        if (currentRequest != null) currentRequest.canceled = true;
    }

    /**
     * Download and cache tile data.
     *
     * @param upperLeft the upper left location of the area to download
     * @param lowerRight the lower right location of the area to download
     * @param title the title of the data set
     * @param uri the uri to download imagery data from
     * @param minRes the minimum resolution
     * @param maxRes the maximum resolution
     * @param cacheUri the uri to save the cached tile data
     * @param progressMonitor a progress monitor callback
     * @return the final job status
     */
    public JobStatus startDownload(@Nonnull ILocation upperLeft, @Nonnull ILocation lowerRight, @Nonnull String title,
                                   @Nonnull String uri, double minRes, double maxRes, @Nullable String cacheUri, @Nonnull IImportExportProgressMonitor progressMonitor)
    {
        ILocation[] geometry = {
                new GeoPoint(upperLeft),
                new GeoPoint(upperLeft.getLatitude(), lowerRight.getLongitude()),
                new GeoPoint(lowerRight),
                new GeoPoint(lowerRight.getLatitude(), upperLeft.getLongitude()),
                new GeoPoint(upperLeft),
        };
        return startDownload(geometry, title, uri, minRes, maxRes, cacheUri, progressMonitor);
    }

    /**
     * Download and cache tile data.
     *
     * @param geometry the boundary of the area to download
     * @param title the title of the data set
     * @param uri the uri to download imagery data from
     * @param minRes the minimum resolution
     * @param maxRes the maximum resolution
     * @param cacheUri the uri to save the cached tile data
     * @param progressMonitor a progress monitor callback
     * @return the final job status
     */
    public JobStatus startDownload(@Nonnull ILocation[] geometry, @Nonnull String title,
                                   @Nonnull String uri, double minRes, double maxRes, @Nullable String cacheUri, @Nonnull IImportExportProgressMonitor progressMonitor)
    {
        queuedDownloads++;

        if (title.isEmpty())
        {
            logger.error("Couldn't start download and cache service because the cache title was empty");
            reportJobStatus(JobStatus.ERROR, progressMonitor);
            return JobStatus.ERROR;
        }

        if (uri.isEmpty())
        {
            logger.error("Couldn't start download and cache service because the layer URI was empty");
            reportJobStatus(JobStatus.ERROR, progressMonitor);
            return JobStatus.ERROR;
        }

        if (Double.isNaN(minRes) || Double.isNaN(maxRes))
        {
            logger.error("Couldn't start download and cache service because the levels were not defined");
            reportJobStatus(JobStatus.ERROR, progressMonitor);
            return JobStatus.ERROR;
        }

        if (geometry.length < 3)
        {
            logger.error("Couldn't start download and cache service because the geometry was not well defined");
            reportJobStatus(JobStatus.ERROR, progressMonitor);
            return JobStatus.ERROR;
        }

        return download(title, geometry, cacheUri, uri, minRes, maxRes, progressMonitor);
    }

    /**
     * Download and cache tile data.
     *
     * @param geometry the boundary of the area to download
     * @param title the title of the data set
     * @param client the tile client to download imagery data from
     * @param minRes the minimum resolution
     * @param maxRes the maximum resolution
     * @param cacheUri the uri to save the cached tile data
     * @param progressMonitor a progress monitor callback
     * @return the final job status
     */
    public JobStatus startDownload(@Nonnull ILocation[] geometry, @Nonnull String title,
                                   @Nonnull TileClient client, double minRes, double maxRes, @Nullable String cacheUri, @Nonnull IImportExportProgressMonitor progressMonitor)
    {
        queuedDownloads++;

        if (title.isEmpty())
        {
            logger.error("Couldn't start download and cache service because the cache title was empty");
            reportJobStatus(JobStatus.ERROR, progressMonitor);
            return JobStatus.ERROR;
        }

        if (Double.isNaN(minRes) || Double.isNaN(maxRes))
        {
            logger.error("Couldn't start download and cache service because the levels were not defined");
            reportJobStatus(JobStatus.ERROR, progressMonitor);
            return JobStatus.ERROR;
        }

        if (geometry.length < 3)
        {
            logger.error("Couldn't start download and cache service because the geometry was not well defined");
            reportJobStatus(JobStatus.ERROR, progressMonitor);
            return JobStatus.ERROR;
        }

        return download(title, geometry, cacheUri, client, minRes, maxRes, progressMonitor);
    }

    /**
     * Initiate a connectivity check for the TileClient serviced by the specified dataset descriptor.
     * In general this indicates that the network connectivity has changed and the TileClient should
     * reevaluate if it can connect to resources.
     *
     * @param datasetDescriptor the dataset descriptor
     */
    public static void checkConnectivity(DatasetDescriptor datasetDescriptor)
    {
        String layerURI = datasetDescriptor.getUri();
        TileClient client = TileClientFactory.create(layerURI, null, null);
        if (client == null) return;
        try
        {
            client.checkConnectivity();
        } finally
        {
            client.dispose();
        }
    }

    /**
     * Get an estimate of the number of tiles produced from the specified {@link TileClient}.
     *
     * @param client the tile client
     * @param minRes the minimum resolution to download
     * @param maxRes the maximum resolution to download
     * @param geometry the boundary of the area to download
     * @return the estimated tile count
     * @since 5.6
     */
    public static int estimateTileCount(TileClient client, double minRes, double maxRes, ILocation[] geometry)
    {
        TileContainerSpi preferredContainerSpi = getPreferredContainerSpi(client);
        CacheRequest request = createRequest(null, preferredContainerSpi.getName(), maxRes, minRes, geometry);
        request.countOnly = true;
        return TileScraper.estimateTileCount(client, request);
    }

    /**
     * kicks off a download of the selected layers at the selected levels in the selected rectangle
     */
    private JobStatus download(String title, ILocation[] geometry, @Nullable String cacheUri, final String layerURI, double minRes, double maxRes, @Nonnull IImportExportProgressMonitor progressMonitor)
    {
        TileClientSpi.Options opts = new TileClientSpi.Options();
        opts.dnsLookupTimeout = 5000L;

        TileClient client = null;
        try
        {
            client = TileClientFactory.create(layerURI, null, opts);
            if (client == null)
            {
                logger.error("Could not create tile client for  {}", layerURI);
                return JobStatus.ERROR;
            }
            return download(title, geometry, cacheUri, client, minRes, maxRes, progressMonitor);
        } finally
        {
            if (client != null) client.dispose();
        }
    }

    private JobStatus download(String title, ILocation[] geometry, @Nullable String cacheUri, final TileClient client, double minRes, double maxRes, @Nonnull IImportExportProgressMonitor progressMonitor)
    {
        // download the tiles
        reportJobStatus(JobStatus.CONNECTING, progressMonitor);

        final JobStatus[] terminalJobStatus = {JobStatus.ERROR};

        try
        {
            TileContainerSpi preferredContainerSpi = getPreferredContainerSpi(client);

            terminalJobStatus[0] = JobStatus.COMPLETE;
            if (cacheUri == null)
            {
                final File dir;
                if (title.endsWith(".ovr"))
                {
                    dir = projectVFSManager.getVirtualFileSystem("grg").getRootDir();
                } else
                {
                    dir = projectVFSManager.getVirtualFileSystem("imagery").getRootDir();
                }

                cacheUri = new File(dir, FileUtils.makeSafeName(title) + preferredContainerSpi.getDefaultExtension()).getAbsolutePath();
                logger.debug("starting download to: {}", cacheUri);
            }

            currentRequest = createRequest(cacheUri, preferredContainerSpi.getName(), maxRes, minRes, geometry);

            logger.debug("starting download of {} cache...", title);

            client.cache(currentRequest, new CacheRequestListener()
            {
                long downloadStart;
                int task = -1;

                @Override
                public void onRequestStarted()
                {
                    reportJobStatus(JobStatus.DOWNLOADING, progressMonitor);
                    downloadStart = SystemClock.elapsedRealtime();
                }

                @Override
                public void onRequestComplete()
                {
                    terminalJobStatus[0] = JobStatus.COMPLETE;
                }

                @Override
                public void onRequestProgress(int taskNum, int numTasks, int taskProgress, int maxTaskProgress, int totalProgress, int maxTotalProgress)
                {
                    if (taskNum != task)
                    {
                        secondaryProgress += maxTaskProgress;
                        task = taskNum;

                    }
                    currentProgress = totalProgress;
                    maxProgress = maxTotalProgress;

                    final double tilesPerMS = totalProgress / (double) (SystemClock.elapsedRealtime() - downloadStart);

                    long timeLeft = (long) Math.ceil((maxTotalProgress - totalProgress) / tilesPerMS / 1000d);
                    reportDownloadStatus(title, timeLeft, taskProgress + " / " + maxTaskProgress,
                            (taskNum + 1) + "/" + numTasks, progressMonitor);
                }

                @Override
                public boolean onRequestError(Throwable t, String message, boolean fatal)
                {
                    // for now just notify the user of the error
                    logger.debug("Lost network connection during map download.");
                    if (queuedDownloads <= 1)
                    {
                        stopSelf();
                    }

                    StringBuilder logMessage = new StringBuilder("Error while trying to download from ");
                    logMessage.append(client.getName());
                    if (message != null)
                    {
                        logMessage.append(", ");
                        logMessage.append(message);
                    }
                    logger.error(logMessage.toString(), t);
                    terminalJobStatus[0] = JobStatus.ERROR;
                    return false;
                }

                @Override
                public void onRequestCanceled()
                {
                    terminalJobStatus[0] = JobStatus.CANCELLED;

                    logger.debug("Download request has been cancelled.");
                    if (queuedDownloads <= 1)
                    {
                        stopSelf();
                        maxProgress = 0;
                        secondaryProgress = 0;
                        currentProgress = 0;
                    } else
                    {
                        secondaryProgress = currentProgress;
                    }
                }
            });
        } finally
        {
            queuedDownloads--;
            currentRequest = null;

            logger.debug("Finished downloading {} errors: {}", title, terminalJobStatus[0] == JobStatus.ERROR);
            logger.debug("tilesets left in queue: {}", queuedDownloads);

            // report job status on termination
            reportJobStatus(terminalJobStatus[0], progressMonitor);
        }

        if (queuedDownloads <= 0)
        {
            maxProgress = 0;
            secondaryProgress = 0;
            currentProgress = 0;
            stopSelf();
        }
        return terminalJobStatus[0];
    }

    protected void stopSelf()
    {
        //
    }

    /**
     * Reports on the time left to download all the tilesets in the queue, the
     * current tile that has been downloaded, and the current layer that is being downloaded.
     */
    protected void reportDownloadStatus(String title, Long time, String tile, String layer, IImportExportProgressMonitor progressMonitor)
    {
        progressMonitor.setProgressIndeterminate(false);
        progressMonitor.printMessage("Layer: " + layer + " | Tile: " + tile + " | " + title);
        progressMonitor.setMaxProgress(maxProgress);
        progressMonitor.setProgress(currentProgress);
    }

    protected void reportJobStatus(JobStatus statusMessage, IImportExportProgressMonitor progressMonitor)
    {
        switch (statusMessage)
        {
            case CONNECTING ->
            {
                progressMonitor.setProgressIndeterminate(true);
                progressMonitor.printMessage("Connecting...");
            }
            case DOWNLOADING ->
            {
                progressMonitor.setProgressIndeterminate(false);
                progressMonitor.printMessage("Downloading...");
            }
            case COMPLETE ->
            {
                progressMonitor.setComplete();
                progressMonitor.setProgressIndeterminate(false);
                progressMonitor.printMessage("Done");
            }
            case CANCELLED ->
            {
                progressMonitor.setComplete();
                progressMonitor.setProgressIndeterminate(false);
                progressMonitor.printMessage("Cancelled");
            }
            case ERROR ->
            {
                progressMonitor.setComplete();
                progressMonitor.setProgressIndeterminate(false);
                progressMonitor.printMessage("Done with errors");
            }
        }
    }

    private static CacheRequest createRequest(String offlineCachePath, String preferredContainerProvider, double maxRes, double minRes, ILocation[] geometry)
    {
        CacheRequest cacheRequest = new CacheRequest();
        cacheRequest.cacheFile = offlineCachePath != null ? new File(offlineCachePath) : null;
        cacheRequest.preferredContainerProvider = preferredContainerProvider;
        cacheRequest.expirationOffset = 7L * 24L * 60L * 60L * 1000L;
        cacheRequest.maxResolution = maxRes;
        cacheRequest.minResolution = minRes;

        cacheRequest.maxThreads = NUM_DOWNLOAD_THREADS;
        cacheRequest.mode = CacheMode.Append;

        LineString geomString = new LineString(2);
        for (ILocation gp : geometry)
        {
            geomString.addPoint(gp.getLongitude(), gp.getLatitude());
        }
        cacheRequest.region = new Polygon(geomString);

        if (cacheRequest.minResolution < cacheRequest.maxResolution)
        {
            cacheRequest.minResolution = 0d;
            cacheRequest.maxResolution = 0d;
        }
        return cacheRequest;
    }

    @Nonnull
    private static TileContainerSpi getPreferredContainerSpi(TileClient client)
    {
        final TileContainerSpi[] preferredContainerSpi = {OSMDroidTileContainer.SPI};
        TileContainerFactory.visitCompatibleSpis(spis -> {
            // XXX - preference should come in from tileset create UI or
            //       from preferences
            final String preferredProvider = "OSMDroid";
            TileContainerSpi candidate = null;
            TileContainerSpi preferred = null;
            for (TileContainerSpi spi : spis)
            {
                if (candidate == null) candidate = spi;
                if (spi.getName().equals(preferredProvider))
                {
                    preferred = spi;
                    break;
                }
            }

            // if the preferred provider is compatible, use it,
            // otherwise use the first compatible provider
            if (preferred != null)
            {
                preferredContainerSpi[0] = preferred;
            } else if (candidate != null)
            {
                preferredContainerSpi[0] = candidate;
            }
        }, client);
        return preferredContainerSpi[0];
    }
}
