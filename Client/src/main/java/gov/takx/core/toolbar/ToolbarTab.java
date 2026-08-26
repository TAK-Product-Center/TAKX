package gov.takx.core.toolbar;

import com.google.common.collect.ImmutableList;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.IPlugin;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarComponent;
import gov.takx.api.toolbar.IToolbarTab;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.utils.UUIDUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object representing a toolbar tab. The toolbar is composed of tabs. Each tab contains one or more components,
 * which may be buttons or separators.
 *
 * @since 2.0 (Renamed in 3.4)
 */
@XmlAccessorType(XmlAccessType.NONE)
@PseudoBean
public final class ToolbarTab implements IToolbarTab
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private transient Event<ToolbarChangeEvent> event;

    @XmlElement
    private String displayText;

    @XmlElement
    private final TabType type;

    @XmlElement
    private final UUID id;

    // Indicates if this tab is complete and valid for adding to the toolbar.
    private boolean valid = true;

    // Indicates if the tab is visible in the configuration.
    @XmlElement
    private boolean visible = true;

    // Indicates if the user was responsible for setting the tab to not visible. This information needs to be retained
    // so a plugin does not override a user's visibility preferences.
    @XmlElement
    private boolean hiddenByUser = false;

    // The name of the plugin that created this component (or null if created by TAKX)
    @XmlElement
    private final String pluginName;

    // If this tab was created by a plugin, this contains the unique key identifying the plugin.
    @XmlElement
    private final String pluginUniqueKey;

    // The configuration to which this tab belongs. A tab can only belong to a single configuration at a time.
    private ToolbarConfiguration configuration;

    // The components (buttons and separators) contained within this tab.
    @XmlElementWrapper
    @XmlElements({
            @XmlElement(name = "button", type = ToolbarButton.class),
            @XmlElement(name = "separator", type = ToolbarSeparator.class),
    })
    private final List<AToolbarComponent> components = new ArrayList<>();

    // Ids of components that would normally be part of this tab but that have been intentionally excluded by the user.
    @XmlElementWrapper
    @XmlElement(name = "excludedId")
    private final Set<UUID> excludedIds = ConcurrentHashMap.newKeySet();

    /**
     * Creates a tab with the provided singular tab type. The id and display text are both determined from the type.
     *
     * @param tabType The tab type (must be singular)
     * @return The newly created tab.
     * @throws IllegalArgumentException If the tab type is not singular.
     * @since 3.4
     */
    @Nonnull
    public static ToolbarTab createSingularTab(@Nonnull TabType tabType) throws IllegalArgumentException
    {
        final UUID id = tabType.getSingularId();
        if (id == null) throw new IllegalArgumentException("Tab type must be singular; " + tabType + " is not.");

        return new ToolbarTab(tabType, id, tabType.getDisplayText());
    }

    /**
     * Creates a {@link TabType#PLUGIN_CREATED} tab with the provided display text. The id is created from the plugin
     * key and the provided display text so it will be the same across application restarts.
     *
     * @param plugin Plugin creating the tab
     * @param displayText The display text for the tab
     * @return The newly created tab.
     * @since 3.4
     */
    @Nonnull
    public static ToolbarTab createPluginTab(@Nonnull IPlugin plugin, @Nonnull String displayText)
    {
        // Use the plugin key and tab display text to generate the UUID so it will be the same when restarting TAKX.
        // Otherwise, the tabs in the toolbar config won't match and nothing will be displayed.
        UUID id = UUIDUtils.generateType3UUID(plugin.getUniqueKey() + ":" + displayText);
        return new ToolbarTab(TabType.PLUGIN_CREATED, id, displayText, plugin);
    }

    /**
     * Creates a {@link TabType#USER_CREATED} tab with the provided display text.
     *
     * @param displayText The display text for the tab
     * @return The newly created tab.
     * @since 3.4
     */
    @Nonnull
    public static ToolbarTab createUserTab(@Nonnull String displayText)
    {
        // A time-based UUID can be used for user-created tabs since they won't need to be correlated to programmatically
        // created tab ids later.
        return new ToolbarTab(TabType.USER_CREATED, UUIDUtils.generateType1UUID(), displayText);
    }

    /**
     * No-arg constructor for JAXB.
     */
    @SuppressWarnings("unused") // Used by JAXB
    private ToolbarTab()
    {
        // JAXB will use reflection to set these values correctly; setting to null here so they can be final
        type = null;
        id = null;
        pluginName = null;
        pluginUniqueKey = null;
    }

    /**
     * Constructor for a tab <b>not</b> created by a plugin.
     *
     * @param type Tab type to create
     * @param id The id for the tab
     * @param displayText The text to display for the tab
     * @see #createSingularTab(TabType)
     * @see #createUserTab(String)
     */
    private ToolbarTab(@Nonnull TabType type, @Nonnull UUID id, @Nonnull String displayText)
    {
        this.type = type;
        this.id = id;
        this.displayText = displayText;
        pluginName = null;
        pluginUniqueKey = null;
    }

    /**
     * Constructor for a tab created by a plugin.
     *
     * @param displayText The name to use for the tab (and the title text unless custom text is set later)
     * @param plugin Plugin creating the tab
     * @see #createPluginTab(IPlugin, String)
     * @since 3.4
     */
    private ToolbarTab(@Nonnull TabType type, @Nonnull UUID id, @Nonnull String displayText, @Nonnull IPlugin plugin)
    {
        this.type = type;
        this.id = id;
        this.displayText = displayText;
        pluginName = plugin.getName();
        pluginUniqueKey = plugin.getUniqueKey();
    }

    /**
     * Copy constructor.
     *
     * @param origTab The tab to be copied
     * @see #copy()
     */
    private ToolbarTab(@Nonnull ToolbarTab origTab)
    {
        configuration = null;
        id = origTab.id;
        displayText = origTab.displayText;
        type = origTab.type;
        valid = origTab.valid;
        visible = origTab.visible;
        hiddenByUser = origTab.hiddenByUser;
        pluginName = origTab.pluginName;
        pluginUniqueKey = origTab.pluginUniqueKey;
        origTab.getComponents().forEach(component -> addComponent(component, true));
    }

    /**
     * Creates a copy of itself.
     *
     * @return The new tab that is a copy of the instance on which the call was made.
     * @since 3.4
     */
    @Nonnull
    public ToolbarTab copy()
    {
        return new ToolbarTab(this);
    }

    /**
     * Since some plugins will call this on the EDT, we need it to be on the EDT for all callers to avoid components
     * being dded to the toolbar out of order; using @InvokeAndWaitOnEDT due to the return value.
     *
     * {@inheritDoc}
     */
    @InvokeAndWaitOnEDT
    @Override
    public boolean addComponent(@Nonnull IToolbarComponent component) throws IllegalArgumentException
    {
        return insertComponent(component, components.size());
    }

    /**
     * Since some plugins will call this on the EDT, we need it to be on the EDT for all callers to avoid components
     * being dded to the toolbar out of order; using @InvokeAndWaitOnEDT due to the return value.
     *
     * {@inheritDoc}
     */
    @InvokeAndWaitOnEDT
    @Override
    public boolean insertComponent(@Nonnull IToolbarComponent component, int index) throws IllegalArgumentException
    {
        if (!(component instanceof AToolbarComponent toolbarComponent))
        {
            throw new IllegalArgumentException("IToolbarComponent instances must be created via IToolbarManager");
        }

        // Create a copy if the component is already part of another tab
        return insertComponent(toolbarComponent, index, toolbarComponent.getTab() != null);
    }

    /**
     * Adds a component to the tab, as long as a component with the same id isn't already present.
     *
     * @param component The component to add
     * @param createCopy Indicates whether a copy of the component should be added or the component itself. A copy should be
     *                   created any time a component is added to another configuration or tab besides the default
     *                   configuration to ensure the new component can be changed independently of the original component.
     * @return {@code true} if the component was added; {@code false} if the component is already in the tab or has been
     * excluded by the user
     * @throws IllegalArgumentException if the component is already associated with a toolbar tab (to avoid this,
     * set{@code createCopy} to {@code true}).
     * @since 3.4
     */
    public boolean addComponent(@Nonnull AToolbarComponent component, boolean createCopy) throws IllegalArgumentException
    {
        return insertComponent(component, components.size(), createCopy);
    }

    /**
     * Adds a component to the tab in the specified index, as long as a component with the same id isn't already present.
     *
     * @param component The component to add
     * @param index The index at which to add the component
     * @param createCopy Indicates whether a copy of the component should be added or the component itself. A copy should be
     *                   created any time a component is added to another configuration or tab besides the default
     *                   configuration to ensure the new component can be changed independently of the original component.
     * @return {@code true} if the component was added; {@code false} if the component is already in the tab or has been
     * excluded by the user
     * @throws IllegalArgumentException if the component is already associated with a toolbar tab (to avoid this,
     * set{@code createCopy} to {@code true}).
     * @since 3.4
     */
    public boolean insertComponent(@Nonnull AToolbarComponent component, int index, boolean createCopy) throws IllegalArgumentException
    {
        return insertComponent(component, index, createCopy, false);
    }

    /**
     * Adds a component to the tab, as long as a component with the same id isn't already present.
     *
     * @param component The component to add
     * @param index The index at which to add the component
     * @param createCopy Indicates whether a copy of the component should be added or the component itself. A copy
     *                   should be created any time a component is added to another configuration besides the default
     *                   configuration to ensure the new component can be changed independently of the original component.
     * @param addedByUser Indicates if the component is being added by the user via the user interface
     * @return {@code true} if the component was added; {@code false} if the component is already in the tab or has been
     * excluded by the user
     * @throws IllegalArgumentException if the component is already associated with a toolbar tab (to avoid this,
     * set{@code createCopy} to {@code true}).
     * @since 3.4
     */
    public boolean insertComponent(@Nonnull AToolbarComponent component, int index, boolean createCopy, boolean addedByUser)
            throws IllegalArgumentException
    {
        // Only add the component if it is being added by the user or the user has not specifically excluded it
        final UUID componentId = component.getId();
        boolean excludedComponent = excludedIds.contains(componentId);
        if (!addedByUser && excludedComponent)
        {
            logger.debug("Component was excluded by user: {}; it will not be added to the tab: {}", component, this);
            return false;
        }

        if (containsComponentId(componentId)) return false;

        AToolbarComponent componentToAdd = createCopy ? component.copy() : component;
        componentToAdd.setTab(this);
        components.add(index, componentToAdd);

        // If the user is the one adding this component, it should no longer be listed as excluded
        if (excludedComponent) excludedIds.remove(componentId);

        fireEvent(ToolbarChangeEventType.COMPONENT_ADDED, null, componentToAdd);
        return true;
    }

    @Override
    public void removeComponent(@Nonnull IToolbarComponent component) throws IllegalArgumentException
    {
        if (!(component instanceof AToolbarComponent toolbarComponent))
        {
            throw new IllegalArgumentException("IToolbarComponent instances must be created via IToolbarManager");
        }

        removeComponent(toolbarComponent, false);
    }

    /**
     * Removes the provided component from the tab. If it is being removed by the user and it would normally be included
     * in the tab by default (i.e. removing a drawing tool from the Draw tab), it will be added to the excluded buttons
     * list to prevent TAKX from re-adding it programmatically.
     *
     * @param component The component to remove
     * @param removedByUser Indicates if the component is being removed by the user via the user interface
     * @since 3.4
     */
    public void removeComponent(@Nonnull AToolbarComponent component, boolean removedByUser)
    {
        final boolean removed = components.remove(component);
        if (removed)
        {
            component.setTab(null);

            // If a component was removed by the user, and it is a dynamic button type or was created by a plugin, it
            // needs to be added to the exclude list to prevent TAKX or a plugin from re-adding it.
            if (removedByUser && (isDynamicButton(component) || isPluginCreatedButton(component)))
            {
                excludedIds.add(component.getId());
            }

            fireEvent(ToolbarChangeEventType.COMPONENT_REMOVED, component, null);
        }
    }

    /**
     * Indicates if the provided component is one of the buttons that is dynamically added to the tab by TAKX. This is
     * true if:
     * <ol>
     * <li>the tab has a specific type of buttons added/removed by TAKX</li>
     * <li>the component type is the same as the dynamic type for the tab</li>
     * </ol>
     *
     * @param component The component to check
     * @return {@code true} if this component may be dynamically updated by TAKX.
     * @since 3.4 (formerly in ToolbarBand)
     */
    private boolean isDynamicButton(@Nonnull AToolbarComponent component)
    {
        if (type.isUpdatesDynamically() && component instanceof ToolbarButton button)
        {
            return type.getDynamicButtonTypes().contains(button.getType());
        }

        return false;
    }

    /**
     * @param component The component to check
     * @return {@code true} if the component is a button created by a plugin.
     * @since 3.4
     */
    private boolean isPluginCreatedButton(@Nonnull AToolbarComponent component)
    {
        if (!(component instanceof ToolbarButton button)) return false;
        final ButtonType type = button.getType();
        return type == ButtonType.PLUGIN_CREATED || type == ButtonType.PLUGIN_DRAW_TOOL;
    }

    /**
     * @return The components that belong to the tab.
     * @since 3.4
     */
    @Override
    @Nonnull
    public List<AToolbarComponent> getComponents()
    {
        return ImmutableList.copyOf(components);
    }

    /**
     * @param component The component for which to get the index
     * @return The index of the provided component, or -1 if it is not present.
     * @since 3.4
     */
    public int getComponentIndex(@Nonnull AToolbarComponent component)
    {
        return components.indexOf(component);
    }

    /**
     * @param componentId The component id for which to look
     * @return {@code true} if the tab contains a component with an id matching the provided id.
     * @since 3.4
     */
    public boolean containsComponentId(@Nonnull UUID componentId)
    {
        return components.stream().anyMatch(component -> component.getId().equals(componentId));
    }

    /**
     * @return The configuration to which the tab belongs or {@code null} if not yet added to a configuration.
     */
    @Nullable
    public ToolbarConfiguration getConfiguration()
    {
        return configuration;
    }

    /**
     * @return The configuration to which the tab belongs.
     * @throws NullPointerException If the configuration is {@code null}
     * @since 3.4
     */
    @Nonnull
    public ToolbarConfiguration requireConfiguration()
    {
        return Objects.requireNonNull(configuration, "configuration is null");
    }

    /**
     * Associates this tab with the provided configuration.
     *
     * @param configuration The configuration to which the tab belongs
     * @throws IllegalArgumentException when this tab is already associated with a toolbar configuration.
     */
    public void setConfiguration(@Nullable ToolbarConfiguration configuration) throws IllegalArgumentException
    {
        if (configuration == this.configuration) return; // No action if trying to set to the current config

        if (configuration != null && this.configuration != null)
        {
            throw new IllegalArgumentException("Toolbar tab cannot be added to more than one configuration");
        }

        this.configuration = configuration;
    }

    @Override
    @Nonnull
    public String getDisplayText()
    {
        return displayText;
    }

    @Override
    @Nonnull
    public ToolbarTab setDisplayText(@Nonnull String displayText)
    {
        String oldDisplayText = this.displayText;
        this.displayText = displayText;
        fireEvent(ToolbarChangeEventType.TAB_TEXT_CHANGED, oldDisplayText, displayText);
        return this;
    }

    @Override
    @Nonnull
    public TabType getType()
    {
        return type;
    }

    /**
     * @return The unique id for the tab. Note: there may be multiple tab instances in different toolbar configurations
     * with this id. That is intentional so they all function the same way with regards to things like dynamic buttons.
     * @since 3.4
     */
    @Override
    @Nonnull
    public UUID getId()
    {
        return id;
    }

    public boolean isValid()
    {
        return valid;
    }

    /**
     * Sets the valid state of the tab.
     *
     * @param valid Boolean indicating if the tab is valid
     * @return this tab instance (to allow for chaining calls)
     */
    @Nonnull
    public ToolbarTab setValid(boolean valid)
    {
        if (valid != this.valid)
        {
            this.valid = valid;

            fireEvent(ToolbarChangeEventType.TAB_VALIDITY_CHANGED, !this.valid, this.valid);
        }

        return this;
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public void setVisible(boolean visible)
    {
        setVisible(visible, false);
    }

    /**
     * Updates the tab visibility and fires the appropriate change event.
     *
     * @param visible The new visibility
     * @param setByUser Indication of whether the visibility change was initiated by the user
     * @return this tab instance (to allow for chaining calls)
     */
    @Nonnull
    public ToolbarTab setVisible(boolean visible, boolean setByUser)
    {
        // If the tab is currently invisible and the user was the one who set it to invisible, only the user should be
        // allowed to set it back to visible
        if (!this.visible && hiddenByUser && !setByUser)
        {
            logger.info("Only the user can update the visibility of this tab since the user hid it. Tab: {}", displayText);
            return this;
        }

        // If tab is being hidden, save an indication of whether the tab was hidden by the user or a plugin
        if (!visible) hiddenByUser = setByUser;

        if (visible != this.visible)
        {
            this.visible = visible;
            fireEvent(ToolbarChangeEventType.TAB_VISIBILITY_CHANGED, !this.visible, this.visible);
        }

        return this;
    }

    @Override
    @Nullable
    public String getPluginUniqueKey()
    {
        return pluginUniqueKey;
    }

    /**
     * @return True if tab is in the default configuration
     */
    public boolean isInDefaultConfiguration()
    {
        return configuration != null && configuration.isDefault();
    }

    /**
     * Fires a change event after ensuring this tab is part of a configuration.
     *
     * @param type The type of change event
     * @param oldValue The old value of the changed item
     * @param newValue The new value of the changed item
     * @since 2.16
     */
    private void fireEvent(@Nonnull ToolbarChangeEventType type, @Nullable Object oldValue, @Nullable Object newValue)
    {
        if (configuration != null && !Objects.equals(oldValue, newValue))
        {
            event.fire(new ToolbarChangeEvent(configuration, type, this, oldValue, newValue));
        }
    }

    /**
     * @return A display string for identifying the tab in the user interface for toolbar configuration which includes
     * an indication of the tab's origin:
     * <ul>
     * <li>Tabs created by a plugin are annotated with the plugin's name</li>
     * <li>Tabs created by the user are annotated as custom</li>
     * </ul>
     * @since 3.4
     */
    @Nonnull
    public String getIdentificationString()
    {
        if (pluginName != null)
        {
            return displayText + " (" + pluginName + ")";
        } else
        {
            return type == TabType.USER_CREATED ? displayText + " (Custom)" : displayText;
        }
    }

    /**
     * To allow the managing classes to distinguish between events fired from reference tabs and their copies (which
     * initially have the same values), instances are only equal if they are the same object.
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public boolean equals(Object o)
    {
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        // Instances are only equal if they are the same object, so include the hashcode in the toString output to
        // assist with debugging.
        return "ToolbarTab@" + Integer.toHexString(hashCode()) + "{" +
                "displayText='" + displayText + '\'' +
                ", type=" + type +
                ", id=" + id +
                ", valid=" + valid +
                ", visible=" + visible +
                ", hiddenByUser=" + hiddenByUser +
                ", pluginName='" + pluginName + '\'' +
                ", pluginUniqueKey='" + pluginUniqueKey + '\'' +
                ", configuration=" + configuration +
                ", components=" + components +
                ", excludedIds=" + excludedIds +
                '}';
    }
}
