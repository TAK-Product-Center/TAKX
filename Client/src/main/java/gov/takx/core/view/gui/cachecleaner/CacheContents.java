package gov.takx.core.view.gui.cachecleaner;

import gov.nasa.worldwind.cache.BasicDataFileStore;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.util.DataConfigurationFilter;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwindx.examples.util.FileStoreDataSet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class CacheContents
{
    private File cacheLocation;
    private final ArrayList<File> installLocations = new ArrayList<>();
    private final Map<File, List<FileStoreDataSet>> dataSetMap = new ConcurrentHashMap<>();
    private final ExecutorService cacheUpdateFixedThreadPool;
    private final CacheContentListener listener;

    /**
     * Constructor.
     *
     * @param listener the listener of update events created by this class.
     */
    public CacheContents(CacheContentListener listener)
    {
        this.listener = listener;

        // Create a thread pool for handling cache location updates
        cacheUpdateFixedThreadPool = Executors.newFixedThreadPool(4);
        FileStore store = new BasicDataFileStore();
        assembleLocations(store);
    }

    /**
     * Attempt to shutdown the cache location updates (if they are currently running.
     */
    public void shutdownUpdater()
    {
        cacheUpdateFixedThreadPool.shutdownNow();
    }

    /**
     * Finds the locations associated with a FileStore.  Generally, it is used to find the default
     * WorldWindData/WorldWindInstalled folders
     *
     * @param store a file store with information on cache locations.
     */
    protected void assembleLocations(FileStore store)
    {
        File writeLocation = store.getWriteLocation();
        // Set the cache write location if it exists.
        if (writeLocation.exists())
        {
            cacheLocation = store.getWriteLocation();
        }

        // Find all the install locations that exist.
        for (File location : store.getLocations())
        {
            if (location.exists() && store.isInstallLocation(location.getPath()))
            {
                installLocations.add(location);
            }
        }
    }

    /**
     * Get a list of current known cache locations.
     *
     * @return a list of cache locations.
     */
    public List<File> getFileStoreLocations()
    {
        List<File> locations = new ArrayList<>();
        if (cacheLocation != null)
        {
            locations.add(cacheLocation);
        }

        locations.addAll(installLocations);
        return locations;
    }

    /**
     * Add a new cache location to manage.
     *
     * @param location a location to add.
     */
    public void addFileStoreLocation(File location)
    {
        if (location.exists())
        {
            installLocations.add(location);
        }
    }

    /**
     * Look up the data set map to find last available location information.
     *
     * @param location a cache location to look up.
     * @return a list of file store data sets if one exists.
     */
    public List<FileStoreDataSet> getDataSetsForLocation(File location)
    {
        if (dataSetMap.containsKey(location))
        {
            return dataSetMap.get(location);
        }
        return null;
    }

    /**
     * Find a list of data set information for a given directory.
     *
     * @param dir a directory to search for caches
     * @return a list of cache data set information.
     */
    public List<FileStoreDataSet> findImportedDataSets(File dir)
    {
        // Find the data configuration files under the specified directory. Each data config file defines the location
        // of a dataset, and any associated metadata.
        String[] names = WWIO.listDescendantFilenames(dir, new DataConfigurationFilter(), false);
        if (names == null || names.length == 0)
        {
            return null;
        }

        ArrayList<FileStoreDataSet> datasets = new ArrayList<>();

        // Create a FileStoreDataSet for each data configuration file. Put the data config file itself in the exclusion
        // list so it won't be deleted.
        for (String pathname : names)
        {
            File dataConfigFile = new File(dir, pathname);
            File dataRoot = dataConfigFile.getParentFile();

            // Ensures that the data configuration file is not deleted by the FileStoreDataSet cleanup routine.
            ArrayList<File> exclusions = new ArrayList<>();
            exclusions.add(dataConfigFile);

            FileStoreDataSet ds = new FileStoreDataSet(dataRoot, dir.getPath());
            ds.setExclusions(exclusions);
            ds.setFileGranularity(true);

            datasets.add(ds);
        }

        return datasets;
    }

    /**
     * Update all known locations with cache data set information
     */
    public void updateAllDataSets()
    {
        updateDataSets(getFileStoreLocations());
    }

    /**
     * Update a list of of location with cache data set info.
     *
     * @param locations a list of cache locations.
     */
    public void updateDataSets(List<File> locations)
    {
        for (File location : locations)
        {
            updateDataSet(location);
        }
    }

    /**
     * Update a specific location with cache data set info.
     *
     * @param location a cache location.
     */
    public void updateDataSet(File location)
    {
        if (listener != null)
        {
            listener.cacheUpdateStarted(location);
        }
        dataSetMap.remove(location);
        cacheUpdateFixedThreadPool.execute(new CacheUpdaterRunnable(location));
    }

    /**
     * Thread pool tasks for updating cache location data set information.
     */
    protected class CacheUpdaterRunnable implements Runnable
    {
        private final File location;

        public CacheUpdaterRunnable(File location)
        {
            this.location = location;
        }

        @Override
        public void run()
        {
            List<FileStoreDataSet> dataSets;
            if (cacheLocation != null && cacheLocation == location)
            {
                dataSets = FileStoreDataSet.getDataSets(location);
            } else
            {
                dataSets = findImportedDataSets(location);
            }

            // WorldWind could not find any valid descriptor XMLs in this location
            if (dataSets != null)
            {
                dataSetMap.put(location, dataSets);
            }

            if (listener != null)
            {
                listener.cacheUpdateCompleted(location);
            }
        }
    }
}
