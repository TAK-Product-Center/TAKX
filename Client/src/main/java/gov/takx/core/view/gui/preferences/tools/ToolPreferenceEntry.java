package gov.takx.core.view.gui.preferences.tools;

import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.api.preferences.IToolPreferenceManager;
import gov.takx.api.preferences.tool.IToolPreference;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.cdi.InjectableContainerSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Defines a tool {@link IPreferenceEntry}.
 * 
 * @since 5.0
 */
public class ToolPreferenceEntry implements IPreferenceEntry
{
    private final String toolUid;
    private final String key;
    private final IToolPreference toolPreference;
    private IToolPreferenceManager toolPreferencesManager;

    private CopyOnWriteArrayList<IPreferenceEntryChangeListener> changeListeners;

    /**
     * Make the key/path for a given tool's preference, in the form {@code [toolUid]/[preferenceId]}.
     * 
     * @param toolUid Tool's unique key
     * @param preferenceId ID of preference within that tool
     * @return Constructed key
     * @since 5.4
     */
    @Nonnull
    public static String keyFor(@Nonnull String toolUid, @Nonnull String preferenceId)
    {
        return KeyUtils.getUniqueKey(toolUid, preferenceId);
    }

    /**
     * @param toolUid The Tool's unique key
     * @param toolPreference Tool preference from which to build this entry
     * @param listener An optional preference change listener
     *
     * @see IToolPreference#getId()
     * @since 5.4 (signature change)
     */
    public ToolPreferenceEntry(@Nonnull String toolUid, @Nonnull IToolPreference toolPreference, @Nullable IPreferenceEntryChangeListener listener)
    {
        this.toolUid = toolUid;
        this.toolPreference = toolPreference;
        key = keyFor(toolUid, toolPreference.getId());

        if (listener != null) addPreferenceEntryChangeListener(listener);
    }

    /**
     * @return The UID of the Tool to which this preference belongs
     * @since 5.5.1
     */
    @Nonnull
    public String getToolUid()
    {
        return toolUid;
    }

    /**
     * @return The ID of this entry's preference
     * @since 5.5.1
     */
    @Nonnull
    public String getPreferenceId()
    {
        return toolPreference.getId();
    }

    /**
     * @return The defining {@link IToolPreference}
     * @since 5.4
     */
    @Nonnull
    public IToolPreference getToolPreference()
    {
        return toolPreference;
    }

    @Override
    public String getDefaultValue()
    {
        return toolPreference.getDefaultValue();
    }

    @Override
    @Nonnull
    public String getKey()
    {
        return key;
    }

    @Override
    @Nonnull
    public String name()
    {
        return getTitle();
    }

    @Override
    @Nonnull
    public String getTitle()
    {
        return toolPreference.getTitle();
    }

    @Override
    @Nonnull
    public String getDescription()
    {
        return toolPreference.getDescription();
    }

    @Override
    @Nonnull
    public List<IPreferenceEntryChangeListener> getChangeListeners()
    {
        return changeListeners != null
                ? changeListeners
                : (changeListeners = new CopyOnWriteArrayList<>());
    }

    @Override
    @Nonnull
    public IPreferenceManager getPreferenceManager()
    {
        return toolPreferencesManager != null
                ? toolPreferencesManager
                : (toolPreferencesManager = new InjectableContainerSupport().getBean(IToolPreferenceManager.class));
    }
}