package gov.takx.core.toolbar;

import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.api.ui.IUISpaceManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Manages the toolbar configurations, including the currently active configuration and the set of all reference tabs
 * and buttons available to be included in configurations.
 *
 * @since 2.0 (Renamed in 3.4)
 */
@ApplicationScoped
public class ToolbarConfigurationManager
{
    public static final String DEFAULT_CONFIGURATION_NAME = "Default Configuration";

    @Inject
    private ToolbarFileManager fileManager;

    @Inject
    private ToolbarUpdater toolbarUpdater;

    // Map of configuration names to the associated configuration
    private final Map<String, ToolbarConfiguration> configurationMap = new ConcurrentHashMap<>();

    // Tabs which are available to be included in any toolbar configuration. These are the reference models on which all
    // other tabs in the configurations are based. This includes all tabs created by the core or programmatically by
    // plugins. Tabs created by the user are not managed as reference tabs since the user manages their contents directly.
    private final List<ToolbarTab> referenceTabs = new CopyOnWriteArrayList<>();

    // Buttons which are available to be included in any toolbar configuration. These are the reference models on which
    // all other buttons in the configurations are based. This includes all buttons created by the core or by plugins.
    private final Set<ToolbarButton> referenceButtons = ConcurrentHashMap.newKeySet();

    // The currently selected toolbar configuration.
    private ToolbarConfiguration activeConfiguration = null;

    /**
     * Loads the configurations from the file system, activating the specified configuration. If none is specified, the
     * default configuration is activated.
     */
    public void loadConfigurations()
    {
        List<ToolbarConfiguration> configurationList = fileManager.loadConfigurations();

        for (ToolbarConfiguration configuration : configurationList)
        {
            populateConfiguration(configuration);
            configurationMap.put(configuration.getName(), configuration);
        }

        // Set the active configuration based on the preferences
        String activeConfigurationName = fileManager.loadActiveConfigurationFromPreferences();
        if (configurationMap.containsKey(activeConfigurationName))
        {
            activateConfiguration(configurationMap.get(activeConfigurationName));
        } else if (activeConfiguration == null)
        {
            // If the specified configuration doesn't exist, activate the default configuration (if not already active)
            activateConfiguration(getDefaultConfiguration());
        }
    }

    /**
     * Reads a ToolbarConfiguration object from an XML file. Note: the imported configuration is not added to the list
     * of configurations by this call. If that is desired, the returned configuration should be added via
     * {@link #addConfiguration(ToolbarConfiguration)}.
     *
     * @param fileName The name of the file from which to read
     * @return The ToolbarConfiguration read from the file, or {@code null} if the read failed.
     * @throws IllegalArgumentException if the provided file contains a configuration version that is not supported.
     */
    @Nullable
    public ToolbarConfiguration importConfiguration(@Nonnull String fileName) throws IllegalArgumentException
    {
        ToolbarConfiguration configuration = fileManager.readConfigurationFromFile(fileName);

        if (configuration != null) populateConfiguration(configuration);

        return configuration;
    }

    /**
     * Populates the provided configuration with the reference tabs/buttons.
     *
     * @param configuration The configuration to update
     */
    private void populateConfiguration(@Nonnull ToolbarConfiguration configuration)
    {
        for (ToolbarTab tab : configuration.getTabs())
        {
            tab.setConfiguration(configuration);
            populateTabAndButtons(tab);
        }

        // We need to update the configuration after it is added in case it is missing any of the reference tabs. This
        // could happen if an old configuration was imported after a plugin added some tabs to the toolbar or if any of
        // the default tabs were manually deleted from the config file.
        final boolean createCopiesOfTabs = !configuration.isDefault();
        for (int i = 0; i < referenceTabs.size(); i++)
        {
            ToolbarTab referenceTab = referenceTabs.get(i);
            if (!configuration.containsTabId(referenceTab.getId()))
            {
                configuration.insertTab(referenceTab, i, createCopiesOfTabs);
            }
        }
    }

    /**
     * Populates a tab and its buttons in a new configuration.
     *
     * @param tab The tab to populate
     * @since 2.16 (renamed in 3.4)
     */
    private void populateTabAndButtons(@Nonnull ToolbarTab tab)
    {
        // If a tab wasn't created by the user and doesn't have a corresponding reference tab, it isn't valid. This can
        // happen if the tab is from a plugin that is not loaded.
        if (tab.getType() != TabType.USER_CREATED) tab.setValid(isIdInReferenceTabs(tab.getId()));

        for (AToolbarComponent component : tab.getComponents())
        {
            // Nothing to copy for separators
            if (component instanceof ToolbarButton button) populateButtonFields(button);
            component.setTab(tab);
        }

        // Add any necessary dynamic buttons that aren't already present in the tab.
        tab.getType().getDynamicButtonTypes().forEach(buttonType -> getReferenceButtonStream(buttonType)
                .forEach(c -> tab.addComponent(c, !tab.isInDefaultConfiguration())));
    }

    /**
     * @param tabId The tab id to look for
     * @return True if the provided id is present in the reference tabs
     * @since 3.4
     */
    private boolean isIdInReferenceTabs(@Nonnull UUID tabId)
    {
        return referenceTabs.stream().anyMatch(t -> t.getId().equals(tabId));
    }

    /**
     * Populates the missing fields of an imported button. For buttons, only the identifying or customizable fields are
     * saved. The remaining fields need to be populated from the reference buttons. It is possible for a configuration
     * button to not be present in the reference buttons (e.g. a button from a plugin that is not loaded). If that is
     * the case, the button will remain in the configuration but will not appear on the toolbar if the configuration is
     * activated. This can be determined by calling {@link AToolbarComponent#isValid()} on the button.
     *
     * @param button The button to populate
     */
    private void populateButtonFields(@Nonnull ToolbarButton button)
    {
        final UUID buttonId = button.getId();
        referenceButtons.stream()
                .filter(b -> b.getId().equals(buttonId))
                .findFirst()
                .ifPresentOrElse(button::copyTransientStateFromReference, () -> button.setValid(false));
    }

    /**
     * Exports the provided configuration to a file.
     *
     * @param fileName The name of the file to which to write
     * @return True if the file was written successfully
     */
    public boolean exportConfiguration(@Nonnull String fileName, @Nonnull ToolbarConfiguration configuration)
    {
        return fileManager.writeConfigurationToFile(fileName, configuration);
    }

    /**
     * Activates the provided configuration, updating the toolbar accordingly.
     *
     * @param configuration Configuration to activate
     */
    public void activateConfiguration(@Nonnull ToolbarConfiguration configuration)
    {
        toolbarUpdater.activateConfiguration(configuration);
        activeConfiguration = configuration;
        fileManager.storeActiveConfigurationToPreferences(configuration.getName());
    }

    /**
     * Adds a new configuration to the set of configurations. The new configuration is saved to a file unless it is the
     * default configuration, which is always created dynamically when the application starts.
     *
     * @param configuration The configuration to add
     */
    public void addConfiguration(@Nonnull ToolbarConfiguration configuration)
    {
        populateConfiguration(configuration);

        configurationMap.put(configuration.getName(), configuration);

        // Don't save the default configuration to a file, since it should be dynamically created as TAKX starts
        if (!configuration.isDefault()) fileManager.saveConfiguration(null, configuration);
    }

    @Nullable
    public ToolbarConfiguration getActiveConfiguration()
    {
        return activeConfiguration;
    }

    /**
     * @return The list of configurations.
     * @since 3.4
     */
    @VisibleForTesting
    List<ToolbarConfiguration> getConfigurations()
    {
        return configurationMap.values().stream().toList();
    }

    /**
     * @return The default configuration
     * @since 2.37
     */
    @Nonnull
    public ToolbarConfiguration getDefaultConfiguration()
    {
        return configurationMap.get(DEFAULT_CONFIGURATION_NAME);
    }

    /**
     * @return The list of configurations other than the default
     * @since 2.37
     */
    @Nonnull
    public List<ToolbarConfiguration> getNonDefaultConfigurations()
    {
        return configurationMap.values().stream().filter(c -> !c.isDefault()).toList();
    }

    /**
     * @return New default configuration.
     * @see #DEFAULT_CONFIGURATION_NAME
     * @since 2.37
     */
    @Nonnull
    public ToolbarConfiguration createDefaultConfiguration()
    {
        final ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration().setDeletable(false);
        configuration.setName(DEFAULT_CONFIGURATION_NAME);
        addConfiguration(configuration);
        return configuration;
    }

    @Nullable
    public ToolbarConfiguration getConfiguration(@Nonnull String name)
    {
        return configurationMap.get(name);
    }

    public void removeConfiguration(@Nonnull ToolbarConfiguration configuration)
    {
        configurationMap.remove(configuration.getName());

        // If configuration was active, activate the default configuration
        if (configuration.equals(activeConfiguration)) activateConfiguration(getDefaultConfiguration());

        fileManager.deleteConfiguration(configuration);
    }

    /**
     * Replaces an existing configuration with an updated version. If the old configuration was active, the new
     * configuration will be activated.
     *
     * @param oldConfigurationName The name of the previous configuration
     * @param newConfiguration The updated configuration
     */
    public void updateConfiguration(@Nonnull String oldConfigurationName, @Nonnull ToolbarConfiguration newConfiguration)
    {
        configurationMap.remove(oldConfigurationName);
        configurationMap.put(newConfiguration.getName(), newConfiguration);

        if (activeConfiguration != null && activeConfiguration.getName().equals(oldConfigurationName))
        {
            activateConfiguration(newConfiguration);
        }

        fileManager.saveConfiguration(oldConfigurationName, newConfiguration);
    }

    /**
     * Adds a tab to the collection of reference tabs which are available to add to the toolbar, including adding any
     * contained buttons to the collection of reference buttons. The tab will also be added to all configurations to
     * ensure the expected core tabs and any plugin-created tabs are available in all configurations.
     *
     * @param tab The tab to add
     */
    public void addReferenceTab(@Nonnull ToolbarTab tab)
    {
        insertReferenceTab(tab, referenceTabs.size());
    }

    /**
     * Adds the provided tab to the list of reference tabs at the specified index. All configurations will be updated
     * with the newly added tab.
     *
     * @param tab The tab to add
     * @param index The index at which to add the tab
     * @since 3.4
     */
    public void insertReferenceTab(@Nonnull ToolbarTab tab, int index)
    {
        // The tab will only be added if there is not already a reference tab with the same id.
        final UUID tabId = tab.getId();
        if (!isIdInReferenceTabs(tabId))
        {
            referenceTabs.add(index, tab);

            // Add each button to the reference list, so they will be available for inclusion in other configurations.
            tab.getComponents().stream()
                    .filter(c -> c instanceof ToolbarButton)
                    .map(ToolbarButton.class::cast)
                    .forEach(this::addReferenceButton);

            // Any invalid tabs that match this new tab should be updated.
            getConfigurationTabStream(tabId).filter(t -> !t.isValid()).forEach(t -> t.setValid(true));

            // And any configurations that don't have this tab need to have it added. This is necessary to ensure that
            // plugin-created tabs are present across all configurations.
            configurationMap.values().stream().filter(c -> !c.containsTabId(tabId)).forEach(c -> c.addTab(tab, !c.isDefault()));
        }
    }

    /**
     * Removes a tab from the collection of reference tabs and updates all configurations accordingly.
     *
     * @param tab The tab to remove
     */
    public void removeReferenceTab(@Nonnull ToolbarTab tab)
    {
        final UUID tabId = tab.getId();
        referenceTabs.removeIf(t -> t.getId().equals(tabId));

        getConfigurationTabStream(tabId).forEach(tabToDelete -> {
            // If the tab's parent configuration is the default (which holds the true tab references), be sure to
            // hard delete it.
            // Note: the configuration can't be null since this is a stream over the tabs in configurations
            final ToolbarConfiguration parentConfiguration = tabToDelete.requireConfiguration();

            if (parentConfiguration.isDefault())
            {
                parentConfiguration.removeTab(tabToDelete);
            } else
            {
                tabToDelete.setValid(false);
            }
        });
    }

    /**
     * Adds a button to the collection of reference buttons which are available to add to the toolbar. If any tabs exist
     * with a tab type that should update dynamically with buttons of this type, the button will be added to those tabs
     * (unless explicitly excluded by a user previously).
     *
     * @param button The button to add
     */
    public void addReferenceButton(@Nonnull ToolbarButton button)
    {
        // The button will only be added if there is not already a button in the collection with the same id.
        final UUID buttonId = button.getId();
        if (!isIdInReferenceButtons(buttonId))
        {
            referenceButtons.add(button);

            // Any invalid buttons that match this new button should be updated
            getConfigurationButtonStream(buttonId).filter(b -> !b.isValid()).forEach(b -> b.copyTransientStateFromReference(button));

            // The button should also be added to any tabs which are setup to dynamically update with buttons of that type
            final TabType tabType = TabType.getTabWithDynamicButtonType(button.getType());
            if (tabType != null) getAllTabStream(tabType).forEach(tab -> tab.addComponent(button, !tab.isInDefaultConfiguration()));
        }
    }

    /**
     * @param buttonId The button id to look for
     * @return True if the provided id is present in the reference buttons
     * @since 3.4
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isIdInReferenceButtons(@Nonnull UUID buttonId)
    {
        return referenceButtons.stream().anyMatch(t -> t.getId().equals(buttonId));
    }

    /**
     * Get the id of the button associated with the provided component descriptor id.
     *
     * @param componentDescriptorId The id of the associated component descriptor
     * @return The id of the first button found that is associated with the provided component descriptor id (or
     * {@code null} if none found).
     * @since 3.4
     */
    @Nullable
    public UUID getIdOfButtonWithComponentDescriptorId(@Nonnull String componentDescriptorId)
    {
        return referenceButtons.stream().filter(b -> componentDescriptorId.equals(b.getComponentDescriptorId()))
                .map(AToolbarComponent::getId)
                .findFirst().orElse(null);
    }

    /**
     * Removes a button from the collection of reference buttons
     *
     * @param button The button to remove
     * @param softDelete Indicates if the button should be merely set to invalid (soft delete) or removed completely (hard delete)
     */
    public void removeReferenceButton(@Nonnull ToolbarButton button, boolean softDelete)
    {
        final UUID buttonId = button.getId();
        referenceButtons.removeIf(b -> b.getId().equals(buttonId));

        // Remove from reference tabs too for completeness
        referenceTabs.forEach(tab -> tab.getComponents().stream().filter(c -> c.getId().equals(buttonId))
                .forEach(c -> tab.removeComponent(c, false)));

        getConfigurationButtonStream(buttonId)
                .forEach(buttonToDelete -> {
                    // For soft delete, just set the button to invalid, which will remove it from the toolbar
                    // but allow it to remain in the configuration in order to retain any customization. If the button's
                    // parent configuration is the default (which holds all the true button references), be sure to hard
                    // delete it so that references are cleaned up properly.
                    if (softDelete)
                    {
                        buttonToDelete.setValid(false);

                        ToolbarConfiguration parentConfiguration = buttonToDelete.requireParentConfiguration();
                        if (parentConfiguration.isDefault()) hardDeleteButton(buttonToDelete);
                    } else
                    {
                        hardDeleteButton(buttonToDelete);
                    }
                });
    }

    /**
     * Helper method that performs the delete of a button by attempting to delete the button from its tab, if it has one.
     *
     * @since 2.36
     */
    private void hardDeleteButton(@Nonnull ToolbarButton button)
    {
        // If the button belongs to a tab, remove it from the tab
        final ToolbarTab tab = button.getTab();
        if (tab != null) tab.removeComponent(button, false);
    }

    // TODO KMB: I suspect we no longer need to support "hard delete" and that always doing a "soft delete" will produce
    //  the correct behavior. However, this logic was specifically added to fix RAPTORX-1264, so I created a ticket to
    //  try this out so I can ensure it doesn't reintroduce the issue. See TAKX-1478.
    /**
     * Removes a button with the specified id from the collection of reference buttons if it exists.
     *
     * @param buttonId The id of the button to remove
     * @param softDelete Indicates if the button should be merely set to invalid (soft delete) or removed completely (hard delete)
     */
    public void removeReferenceButton(@Nonnull UUID buttonId, boolean softDelete)
    {
        for (ToolbarButton button : referenceButtons)
        {
            if (button.getId().equals(buttonId))
            {
                removeReferenceButton(button, softDelete);
                break;
            }
        }
    }

    /**
     * Associates all buttons that have the specified id with the provided component descriptor id.
     *
     * @param buttonId The id of the button to associate with the component descriptor
     * @param componentDescriptorId The id of the component descriptor
     * @since 3.4
     */
    public void associateButtonWithComponentDescriptor(@Nonnull UUID buttonId, @Nonnull String componentDescriptorId)
    {
        // Any buttons that match this id should be updated
        getAllButtonStream(buttonId).forEach(b -> b.setComponentDescriptorId(componentDescriptorId));
    }

    /**
     * @return Set of reference buttons available to add to the toolbar.
     */
    @Nonnull
    public Set<ToolbarButton> getReferenceButtons()
    {
        return Collections.unmodifiableSet(referenceButtons);
    }

    /**
     * @param buttonType The button type
     * @return List of the reference buttons with the provided button type.
     * @since 3.4
     */
    @Nonnull
    public List<ToolbarButton> getReferenceButtons(@Nonnull ButtonType buttonType)
    {
        return getReferenceButtonStream(buttonType).toList();
    }

    /**
     * @param buttonId The button id
     * @return The reference button with the provided id (or {@code null} if one doesn't exist)
     * @since 3.4
     */
    @Nullable
    public ToolbarButton getReferenceButton(@Nonnull UUID buttonId)
    {
        return referenceButtons.stream().filter(b -> b.getId().equals(buttonId)).findFirst().orElse(null);
    }

    /**
     * @return The set of reference tabs available for inclusion in a toolbar configuration.
     * @since 3.4
     */
    public List<ToolbarTab> getReferenceTabs()
    {
        return Collections.unmodifiableList(referenceTabs);
    }

    /**
     * @return The list of tabs that are part of the active configuration
     * @since 5.1
     */
    public List<ToolbarTab> getActiveTabs()
    {
        return activeConfiguration.getTabs();
    }

    /**
     * @param tabType The tab type
     * @return List of reference tabs with the provided tab type.
     * @since 3.4
     */
    @Nonnull
    public List<ToolbarTab> getReferenceTabs(@Nonnull TabType tabType)
    {
        return referenceTabs.stream().filter(b -> b.getType() == tabType).toList();
    }

    /**
     * @param tabId The tab id
     * @return The reference tab with the provided id (or {@code null} if one doesn't exist)
     * @since 3.4
     */
    @Nullable
    public ToolbarTab getReferenceTab(@Nonnull UUID tabId)
    {
        return referenceTabs.stream().filter(t -> t.getId().equals(tabId)).findFirst().orElse(null);
    }

    /**
     * @param buttonId The button id
     * @return Stream of all buttons with the provided id across all configurations and reference buttons.
     * @since 3.4
     */
    @Nonnull
    private Stream<ToolbarButton> getAllButtonStream(@Nonnull UUID buttonId)
    {
        return Stream.concat(getConfigurationButtonStream(buttonId),
                referenceButtons.stream().filter(c -> c.getId().equals(buttonId))).distinct();
    }

    /**
     * @param buttonId The button id
     * @return Stream of all buttons across all configurations with the provided id.
     * @since 3.4
     */
    @Nonnull
    private Stream<ToolbarButton> getConfigurationButtonStream(@Nonnull UUID buttonId)
    {
        return getConfigurationTabStream().flatMap(c -> c.getComponents().stream())
                .filter(c -> c.getId().equals(buttonId))
                .map(ToolbarButton.class::cast);
    }

    /**
     * @param buttonType The button type
     * @return Stream of reference buttons with the provided type.
     * @since 3.4
     */
    @Nonnull
    private Stream<ToolbarButton> getReferenceButtonStream(@Nonnull ButtonType buttonType)
    {
        return referenceButtons.stream().filter(b -> b.getType() == buttonType);
    }

    /**
     * @return Stream of all tabs across all configurations and reference tabs.
     * @since 3.4
     */
    @Nonnull
    private Stream<ToolbarTab> getAllTabStream()
    {
        return Stream.concat(getConfigurationTabStream(), referenceTabs.stream()).distinct();
    }

    /**
     * @param tabType The tab type
     * @return Stream of all tabs associated with the provided type across all configurations and reference tabs.
     * @since 3.4
     */
    @Nonnull
    private Stream<ToolbarTab> getAllTabStream(@Nonnull TabType tabType)
    {
        return getAllTabStream().filter(tab -> tab.getType() == tabType);
    }

    /**
     * @param tabId The tab id
     * @return Stream of all tabs with the provided id across all configurations and reference tabs.
     * @since 3.4
     */
    @Nonnull
    private Stream<ToolbarTab> getAllTabStream(UUID tabId)
    {
        return getAllTabStream().filter(t -> t.getId().equals(tabId));
    }

    /**
     * @return Stream of all tabs across all configurations.
     * @since 3.4
     */
    @Nonnull
    private Stream<ToolbarTab> getConfigurationTabStream()
    {
        return configurationMap.values().stream().flatMap(c -> c.getTabs().stream());
    }

    /**
     * @param tabId The tab id
     * @return Stream of all tabs with the provided id across all configurations.
     * @since 3.4
     */
    @Nonnull
    private Stream<ToolbarTab> getConfigurationTabStream(UUID tabId)
    {
        return getConfigurationTabStream().filter(t -> t.getId().equals(tabId));
    }

    // TODO KMB: Dynamically updating the toolbar doesn't do a good job of paying attention to order currently, since
    //  most tabs/buttons are simply added at the end. I can't imagine that is desirable, since it could lead to buttons
    //  appearing in a different order. We'll need to put some thought into how this should work and if maintaining an
    //  ordering is desired, what property would be used for ordering dynamically added buttons. The obvious choice is
    //  alphabetically by display text. (TAKX-1487)
    /**
     * Add a button to the tab at the specified index.
     *
     * @param tabType The tab type to which to add the button
     * @param button The button to add
     * @param index The index in the tab at which to add the button
     * @throws IllegalArgumentException if a component with the same id is already part of the tab or if the component
     * is already associated with a toolbar tab (to avoid this, set{@code createCopy} to {@code true}).
     * @since 3.4
     */
    public void addButtonToTab(@Nonnull TabType tabType, @Nonnull ToolbarButton button, int index) throws IllegalArgumentException
    {
        getAllTabStream(tabType).forEach(tab -> tab.insertComponent(button, index, !tab.isInDefaultConfiguration(), false));
    }

    /**
     * Remove the given component from all tabs with the given id using a <em>hard</em> delete. This is used
     * when a plugin removes a component that is represented by a button on the toolbar, such as a draw toggle button.
     *
     * @param component Component to remove
     * @param tabType The tab type from which to remove it
     * @since 3.4
     */
    public void removeComponentFromTab(@Nonnull AToolbarComponent component, @Nonnull TabType tabType)
    {
        getAllTabStream(tabType).forEach(tab -> tab.removeComponent(component, false));
        if (component instanceof ToolbarButton button) removeReferenceButton(button, false);
    }

    /**
     * Remove the button with the provided id from all tabs where it exists using a <em>soft</em> delete. This is
     * used to remove buttons created via the {@link IUISpaceManager}, typically when an app is stopped and we want to
     * retain any user customization of the placement of the button.
     *
     * @param buttonId The id of the button to remove
     * @since 3.4
     */
    public void removeButton(@Nonnull UUID buttonId)
    {
        removeReferenceButton(buttonId, true);
    }

    /**
     * Handles change events that affect any reference tabs or buttons, updating all configurations accordingly.
     *
     * @param event The change event
     * @since 3.4
     */
    private void handleChangeEvent(@Observes ToolbarChangeEvent event)
    {
        // Only need to process the event if it is for one of the reference buttons or tabs
        final Object eventSource = event.source();
        final ToolbarChangeEventType eventType = event.type();
        final Object newValue = event.newValue();
        final ToolbarComponentType eventSourceType = eventType.getSourceType();
        if (eventSourceType == ToolbarComponentType.TAB)
        {
            final ToolbarTab tab = (ToolbarTab) eventSource;
            final UUID tabId = tab.getId();
            if (referenceTabs.contains(tab))
            {
                Consumer<ToolbarTab> consumer = switch (eventType)
                {
                    case TAB_TEXT_CHANGED -> t -> t.setDisplayText((String) newValue);
                    case TAB_VISIBILITY_CHANGED -> t -> t.setVisible((Boolean) newValue, false);
                    case COMPONENT_ADDED ->
                    {
                        // If the component is a button and this is the first time it has been seen, add it as a reference button
                        final AToolbarComponent component = (AToolbarComponent) newValue;
                        if (component instanceof ToolbarButton button && !isIdInReferenceButtons(component.getId()))
                        {
                            addReferenceButton(button);
                        }

                        yield t -> t.addComponent(component, true);
                    }
                    case COMPONENT_REMOVED ->
                    {
                        final AToolbarComponent component = (AToolbarComponent) event.oldValue();
                        if (component instanceof ToolbarButton button)
                        {
                            // If the component is a plugin button and removing it from tabs with this id will remove
                            // the last instance of it, remove it from the reference button list.
                            final ButtonType buttonType = button.getType();
                            if (buttonType == ButtonType.PLUGIN_CREATED || buttonType == ButtonType.PLUGIN_DRAW_TOOL)
                            {
                                final UUID componentId = component.getId();
                                final boolean notInAnyOtherTabs = referenceTabs.stream()
                                        .filter(t -> !t.getId().equals(tabId))
                                        .noneMatch(t -> t.containsComponentId(componentId));
                                if (notInAnyOtherTabs)
                                {
                                    removeReferenceButton(button, true);

                                    // Removing the reference button also updates the configurations, so yield null for the action
                                    yield null;
                                }
                            }
                        }

                        yield t -> t.removeComponent(component, false);
                    }
                    default -> null;
                };

                if (consumer != null) getAllTabStream(tabId).forEach(consumer);
            }
        } else if (eventSourceType == ToolbarComponentType.BUTTON)
        {
            final ToolbarButton button = (ToolbarButton) eventSource;
            final UUID buttonId = button.getId();
            if (referenceButtons.contains(button))
            {
                Consumer<ToolbarButton> consumer = switch (eventType)
                {
                    case BUTTON_IMAGE_CHANGED -> b -> b.setImage((BufferedImage) newValue);
                    case BUTTON_STATE_CHANGED -> b -> b.setEnabled((Boolean) newValue);
                    case BUTTON_TEXT_CHANGED -> b -> b.setDisplayText((String) newValue);
                    case BUTTON_TOOLTIP_CHANGED -> b -> b.setTooltipText((String) newValue);
                    default -> null;
                };

                if (consumer != null) getAllButtonStream(buttonId).forEach(consumer);
            }
        }
    }
}
