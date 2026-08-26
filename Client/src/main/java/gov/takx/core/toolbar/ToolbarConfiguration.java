package gov.takx.core.toolbar;

import com.google.common.collect.ImmutableList;
import gov.takx.api.annotation.PseudoBean;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Object representing a toolbar configuration. The toolbar is composed of tabs. Each tab contains one or more components,
 * which may be buttons or separators.
 *
 * @since 2.0 (Renamed in 3.4)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@PseudoBean
public final class ToolbarConfiguration
{
    // The current version of the configuration model for storage in the XML file to assess compatibility when loading.
    // This should be updated any time a breaking change is made to the configuration model.
    public static final String CURRENT_CONFIGURATION_VERSION = "2.0";

    @Inject
    private transient Event<ToolbarChangeEvent> event;

    @XmlElement
    private final String version;

    // The name of the configuration (used in the preferences dialog)
    @XmlElement
    private String name = "<Unnamed>";

    // Indicates if the configuration is editable.
    @XmlElement
    private boolean editable = true;

    // Indicates if the toolbar should be minimized by default
    @XmlElement
    private boolean minimized = false;

    // Indicates if the toolbar can be deleted by a user from the Toolbar configuration dialog
    @XmlElement
    private boolean deletable = true;

    // The tabs included in the configuration. Note: some may not be visible.
    @XmlElementWrapper(name = "tabs")
    @XmlElement(name = "tab")
    private final List<ToolbarTab> tabs = new ArrayList<>();

    /**
     * Creates a configuration with the current version.
     *
     * @return The newly created configuration.
     * @since 3.4
     */
    @Nonnull
    public static ToolbarConfiguration createConfiguration()
    {
        return new ToolbarConfiguration(CURRENT_CONFIGURATION_VERSION);
    }

    /**
     * No-arg constructor for JAXB.
     */
    @SuppressWarnings("unused") // Used by JAXB
    private ToolbarConfiguration()
    {
        // JAXB will use reflection to set this value correctly if it was set in the XML file; setting to 1.0 here for
        // cases where it is not specified in the XML file, since the version field wasn't present in the first version
        // of the configuration model.
        this("1.0");
    }

    /**
     * Constructor that sets the version.
     *
     * @param version The version of the configuration model contained in this configuration
     * @since 3.4
     */
    private ToolbarConfiguration(@Nonnull String version)
    {
        this.version = version;
    }

    /**
     * Copy constructor.
     *
     * @param origConfiguration The configuration to be copied
     */
    public ToolbarConfiguration(@Nonnull ToolbarConfiguration origConfiguration)
    {
        // Copy name, unless this is a copy of the default configuration
        if (!origConfiguration.isDefault()) name = origConfiguration.getName();

        editable = origConfiguration.isEditable();
        minimized = origConfiguration.isMinimized();
        version = origConfiguration.version;
        deletable = origConfiguration.isDeletable();

        origConfiguration.getTabs().forEach(tab -> insertTabInternal(tab, tabs.size(), true));
    }

    /**
     * @return {@code true} if this configuration is the default configuration
     * @see ToolbarConfigurationManager#DEFAULT_CONFIGURATION_NAME
     * @since 2.37
     */
    public boolean isDefault()
    {
        return name.equals(ToolbarConfigurationManager.DEFAULT_CONFIGURATION_NAME);
    }

    /**
     * Adds a tab to the last position, as long as a tab with the same id isn't already present.
     *
     * @param tab The tab to add
     * @param createCopy Indicates whether a copy of the tab should be added or the tab itself. A copy should be
     *                   created any time a tab is added to another configuration besides the default configuration to
     *                   ensure the new tab can be changed independently of the original tab.
     * @return {@code true} if the tab was added to the configuration; {@code false} if a tab with the same id was
     * already present
     */
    public boolean addTab(@Nonnull ToolbarTab tab, boolean createCopy) throws IllegalArgumentException
    {
        return insertTab(tab, tabs.size(), createCopy);
    }

    /**
     * Adds a tab at the specified index, as long as a tab with the same id isn't already present, and fires a change
     * event for the newly added tab.
     *
     * @param tab The tab to add
     * @param index The index at which to add the tab
     * @param createCopy Indicates whether a copy of the tab should be added or the tab itself. A copy should be
     *                   created any time a tab is added to another configuration besides the default configuration to
     *                   ensure the new tab can be changed independently of the original tab.
     * @return {@code true} if the tab was added to the configuration; {@code false} if a tab with the same id was
     * already present
     */
    public boolean insertTab(@Nonnull ToolbarTab tab, int index, boolean createCopy)
    {
        if (containsTabId(tab.getId())) return false;

        ToolbarTab tabToAdd = insertTabInternal(tab, index, createCopy);
        fireEvent(ToolbarChangeEventType.TAB_ADDED, null, tabToAdd);

        return true;
    }

    /**
     * Adds a tab at the specified index without firing a change event.
     *
     * @param tab The tab to add
     * @param index The index at which to add the tab
     * @param createCopy Indicates whether a copy of the tab should be added or the tab itself
     * @return The tab that was added (which may be a copy of the provided tab)
     * @since 3.4
     */
    @Nonnull
    private ToolbarTab insertTabInternal(@Nonnull ToolbarTab tab, int index, boolean createCopy)
    {
        ToolbarTab tabToAdd = createCopy ? tab.copy() : tab;
        tabToAdd.setConfiguration(this);
        tabs.add(index, tabToAdd);
        return tabToAdd;
    }

    /**
     * Removes the provided tab from the configuration, firing a change event for the removal.
     *
     * @param tab The tab to remove
     */
    public void removeTab(@Nonnull ToolbarTab tab)
    {
        final boolean removed = tabs.remove(tab);
        if (removed)
        {
            tab.setConfiguration(null);
            fireEvent(ToolbarChangeEventType.TAB_REMOVED, tab, null);
        }
    }

    @Nonnull
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name for the configuration.
     *
     * @param name The name of the configuration
     * @return this configuration instance (to allow for chaining calls)
     */
    public ToolbarConfiguration setName(@Nonnull String name)
    {
        this.name = name;
        return this;
    }

    /**
     * @return Whether the toolbar configuration is editable.
     */
    public boolean isEditable()
    {
        return editable;
    }

    /**
     * Sets whether the toolbar configuration is editable.
     *
     * @param editable True if the toolbar configuration should be editable
     * @return this configuration instance (to allow for chaining calls)
     */
    public ToolbarConfiguration setEditable(boolean editable)
    {
        this.editable = editable;
        return this;
    }

    /**
     * @return Whether the toolbar is minimized by default.
     */
    public boolean isMinimized()
    {
        return minimized;
    }

    /**
     * Sets whether the toolbar should be minimized by default.
     *
     * @param minimized True if the toolbar should start in a minimized state
     * @return this configuration instance (to allow for chaining calls)
     */
    public ToolbarConfiguration setMinimized(boolean minimized)
    {
        this.minimized = minimized;
        return this;
    }

    /**
     * @return Whether the toolbar can be deleted by a user.
     * @since 5.5.1
     */
    public boolean isDeletable()
    {
        return deletable;
    }

    /**
     * Sets whether the toolbar configuration is deletable.
     *
     * @param deletable {@code true} if the toolbar configuration should be deletable
     * @return this configuration instance (to allow for chaining calls)
     * @since 5.5.1
     */
    public ToolbarConfiguration setDeletable(boolean deletable)
    {
        this.deletable = deletable;
        return this;
    }

    /**
     * @param tabToFind The tab for which to get the index
     * @return The index of the provided tab, or -1 if it is not present.
     */
    public int getTabIndex(@Nonnull ToolbarTab tabToFind)
    {
        return tabs.indexOf(tabToFind);
    }

    /**
     * @return all tabs in the configuration.
     */
    @Nonnull
    public List<ToolbarTab> getTabs()
    {
        return ImmutableList.copyOf(tabs);
    }

    /**
     * @param tabId The tab id for which to look
     * @return {@code true} if the configuration contains a tab with an id matching the provided id.
     * @since 3.4
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean containsTabId(@Nonnull UUID tabId)
    {
        return tabs.stream().anyMatch(tab -> tab.getId().equals(tabId));
    }

    /**
     * @return The version of the configuration model.
     * @since 3.4
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Notifies any observers that an event affecting the toolbar configuration has occurred.
     *
     * @param type The type of change
     * @param oldValue The old value of the changed item
     * @param newValue The new value of the changed item
     * @since 2.16
     */
    private void fireEvent(@Nonnull ToolbarChangeEventType type, @Nullable Object oldValue, @Nullable Object newValue)
    {
        if (!Objects.equals(oldValue, newValue)) event.fire(new ToolbarChangeEvent(this, type, this, oldValue, newValue));
    }
}
