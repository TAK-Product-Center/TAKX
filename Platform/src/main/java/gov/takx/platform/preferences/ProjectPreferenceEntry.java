package gov.takx.platform.preferences;

import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IProjectPreferenceManager;
import gov.takx.platform.cdi.InjectableContainerSupport;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Definitions for all of our Project preference keys and default values.
 *
 * @since 2.35
 */
public enum ProjectPreferenceEntry implements IPreferenceEntry
{
    CREATION_TIME("CreationTime", ""),
    DATABASE_TYPE("DatabaseType", ""),
    DATABASE_VERSION("DatabaseVersion", ""),
    DESCRIPTION("Description", ""),
    /** @since 3.4 */
    DTED_STREAM_SERVER("DtedStreamServer", "tak.gov"),
    /** @since 3.4 */
    DTED_STREAM("DtedStream", "true"),
    END_DATE("End Date", ""),
    LAST_UPDATE("LastUpdateTime", ""),
    LAYER_MANAGER_FAVORITES("LayerManager/Favorites", ""),
    LAYER_MANAGER_OPEN_GROUPS("LayerManager/OpenGroups", ""),
    LOCATION("Location", ""),
    MAP_OBJECTS_NODE("MapObjects", null),
    NOTES("Notes", ""),
    PLAYBACK_NODE("Playback", null),
    PROJECT_LABELS_NODE("ProjectLabels", null),
    SELECTED_ELEVATION_MODELS("SelectedElevationModels", ""),
    SIZE("Size", ""),
    START_DATE("Start Date", ""),
    TITLE("Title", ""),
    UPGRADER_COUNT("upgraderCount", ""),
    UUID("UUID", ""),
    VERSION("version", ""),
    VFS("VFS", ""),
    VIEW_BOOKMARKS_NODE("ViewBookmarks", null),
    WMS_OFFLINE_MODE("WMS_Offline", "false"),

    NONE("NONE", "NONE"); // Never use this

    private final String key;
    private final String defaultValue;

    private IProjectPreferenceManager projectPreferenceManager;
    private List<IPreferenceEntryChangeListener> changeListeners;

    @Override
    public String getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public String getKey()
    {
        return key;
    }

    /**
     * Constructor for immutable preferences (can't change at runtime).
     *
     * @param key          Preference key
     * @param defaultValue Default value if not specified in preferences file
     */
    ProjectPreferenceEntry(String key, String defaultValue)
    {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public IProjectPreferenceManager getPreferenceManager()
    {
        return projectPreferenceManager != null
                ? projectPreferenceManager
                : (projectPreferenceManager = new InjectableContainerSupport().getBean(IProjectPreferenceManager.class));
    }

    @Override
    public List<IPreferenceEntryChangeListener> getChangeListeners()
    {
        return changeListeners != null
                ? changeListeners
                : (changeListeners = new CopyOnWriteArrayList<>());
    }
}
