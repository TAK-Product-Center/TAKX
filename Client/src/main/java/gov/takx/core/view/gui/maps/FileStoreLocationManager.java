package gov.takx.core.view.gui.maps;

import gov.nasa.worldwind.WorldWind;
import gov.takx.core.globe.GlobeLayerManager;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A singleton to manage location Strings added to WorldWind's data file store.
 * <p/>
 * WorldWind requires that image/elevation cache file stores point to the parent directory of actual caches.  For
 * example, if the directory ~/images/earth contains earthCache.xml, the file store must point to its parent directory,
 * which is ~/images. However, if the selected directory is ~/images, then it is not necessary to point to a parent
 * directory, because ~/images does not directly contain cache information (its child folder does).
 * <p/>
 * Due to this, it becomes necessary to add two directories when an image/elevation cache directory is selected by a
 * user - the actual location, and its parent's location.  The user may also choose to remove a cache location, which
 * should remove both the actual directory and the parent directory (if the parent directory has not been directly
 * previously added).
 * <p/>
 * This manager keeps track of parent locations added which should not be persisted in TAKX's list of file store
 * locations.
 * <p/>
 * For usage examples, see {@link GlobeLayerManager GlobeLayerManager}.
 *
 */
public class FileStoreLocationManager
{
    private static FileStoreLocationManager instance = null;
    private final Set<String> nonPersistedLocations = new HashSet<>();

    /**
     * private constructor for Singleton usage
     */
    private FileStoreLocationManager()
    {
    }

    public static synchronized FileStoreLocationManager getInstance()
    {
        if (instance == null)
        {
            instance = new FileStoreLocationManager();
        }
        return instance;
    }

    public synchronized void addLocationWithParent(String location)
    {
        addLocation(location, true);
        String parentLocation = new File(location).getParent();
        addLocation(parentLocation, false);
    }

    /**
     * Get a Set containing all known file store locations which should not be persisted in the user preferences file.
     *
     * @return Set of non persisted file store locations.
     */
    public synchronized Set<String> getNonPersistedLocations()
    {
        return Collections.unmodifiableSet(nonPersistedLocations);
    }

    /**
     * Add a file store location to WorldWind.  This should only be called when it is known that the parent directory
     * shouldn't be added to WorldWind.  Otherwise, use {@link #addLocationWithParent(String)}.
     *
     * @param location      file store location to add to WorldWind.
     * @param shouldPersist should store location in preferences
     */
    public synchronized void addLocation(String location, boolean shouldPersist)
    {
        if (shouldPersist)
        {
            nonPersistedLocations.remove(location);
        } else
        {
            nonPersistedLocations.add(location);
        }

        if (!WorldWind.getDataFileStore().getLocations().contains(new File(location)))
        {
            WorldWind.getDataFileStore().addLocation(location, true);
        }
    }

    /**
     * Decrement a reference count for a file store location.
     *
     * @param location file store location to reference decrement and potentially remove from WorldWind.
     */
    public synchronized void removeLocation(String location)
    {
        String parentLocation = new File(location).getParent();
        if (nonPersistedLocations.contains(parentLocation))
        {
            WorldWind.getDataFileStore().removeLocation(parentLocation);
            nonPersistedLocations.remove(parentLocation);
        }

        WorldWind.getDataFileStore().removeLocation(location);
    }
}
