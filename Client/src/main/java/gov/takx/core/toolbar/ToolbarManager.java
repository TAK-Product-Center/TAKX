package gov.takx.core.toolbar;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.plugin.IPlugin;
import gov.takx.api.plugin.app.IAppListener;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.api.toolbar.IToolbarButtonBuilder;
import gov.takx.api.toolbar.IToolbarComponent;
import gov.takx.api.toolbar.IToolbarManager;
import gov.takx.api.toolbar.IToolbarSeparator;
import gov.takx.api.toolbar.IToolbarTab;
import gov.takx.api.toolbar.TabType;
import gov.takx.core.view.gui.menus.AToolbarTabWrapper;
import gov.takx.core.view.gui.menus.AppsToolbarTabWrapper;
import gov.takx.core.view.gui.menus.AttachmentsToolbarTabWrapper;
import gov.takx.core.view.gui.menus.ConnectToolbarTabWrapper;
import gov.takx.core.view.gui.menus.DataToolbarTabWrapper;
import gov.takx.core.view.gui.menus.DevicesToolbarTabWrapper;
import gov.takx.core.view.gui.menus.DrawToolbarTabWrapper;
import gov.takx.core.view.gui.menus.HelpToolbarTabWrapper;
import gov.takx.core.view.gui.menus.HomeToolbarTabWrapper;
import gov.takx.core.view.gui.menus.MapsToolbarTabWrapper;
import gov.takx.core.view.gui.menus.RangeAndBearingToolbarTabWrapper;
import gov.takx.core.view.gui.menus.ViewToolbarTabWrapper;
import gov.takx.core.view.gui.toolbar.IToolbar;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @since 3.4 (renamed from RibbonManager)
 */
@ApplicationScoped
public class ToolbarManager implements IToolbarManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Map of tab type to the class that creates and populates the tab. This used to be embedded in the tab type enum
    // but had to be moved so the enum could be included in the API, which doesn't have access to these classes.
    private static final Map<TabType, Class<? extends AToolbarTabWrapper>> TAB_CLASS_MAP = new EnumMap<>(TabType.class);

    static
    {
        TAB_CLASS_MAP.put(TabType.HOME, HomeToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.CONNECT, ConnectToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.DEVICES, DevicesToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.MAPS, MapsToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.ATTACHMENTS, AttachmentsToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.DATA, DataToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.DRAW, DrawToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.RANGE_AND_BEARING, RangeAndBearingToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.VIEW, ViewToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.PLUGINS, AppsToolbarTabWrapper.class);
        TAB_CLASS_MAP.put(TabType.HELP, HelpToolbarTabWrapper.class);
    }

    // Map of the plugin unique key to the list of tabs it created.
    private final Map<String, List<ToolbarTab>> pluginTabMap = new HashMap<>();

    // Map of the plugin unique key to the list of buttons it created.
    private final Map<String, List<ToolbarButton>> pluginButtonMap = new HashMap<>();

    private final IToolbar toolbar;
    private final ToolbarConfigurationManager toolbarConfigurationManager;
    private final Map<TabType, AToolbarTabWrapper> tabWrappers = new EnumMap<>(TabType.class);

    @Inject
    public ToolbarManager(ICoreAppManager appManager, @Headless boolean headless, IToolbar toolbar,
                          ToolbarConfigurationManager toolbarConfigurationManager)
    {
        this.toolbarConfigurationManager = toolbarConfigurationManager;
        this.toolbar = toolbar;

        if (!headless) appManager.addListener(new AppCleanupListener());
    }

    /**
     * Create default toolbar configuration and all the standard tabs.
     * Each tab wrapper will update it with the necessary tabs, buttons, and separators.
     *
     * @since 2.37
     */
    public void createStandardTabs()
    {
        toolbarConfigurationManager.createDefaultConfiguration();

        TabType.getDefaultTabTypes().forEach(this::createTabWrapper);
    }

    /**
     * Create a toolbar tab wrapper.
     *
     * @param tabType Tab type to create
     * @since 2.37
     */
    private void createTabWrapper(@Nonnull TabType tabType)
    {
        final Class<? extends AToolbarTabWrapper> wrapperClass = TAB_CLASS_MAP.get(tabType);

        try
        {
            tabWrappers.put(tabType, wrapperClass.getConstructor().newInstance());
        } catch (ReflectiveOperationException e)
        {
            logger.error("Failed to create: {}", wrapperClass.getSimpleName(), e);
        }
    }

    @Override
    @Nonnull
    public IToolbarButtonBuilder createButtonBuilder(@Nonnull IPlugin plugin, @Nonnull String displayText)
    {
        return ToolbarButtonBuilder.createPluginButtonBuilder(plugin, displayText);
    }

    @Override
    @Nonnull
    public IToolbarButtonBuilder createDrawingToolButtonBuilder(@Nonnull IPlugin plugin, @Nonnull String displayText)
    {
        return ToolbarButtonBuilder.createPluginDrawToolButtonBuilder(plugin, displayText);
    }

    /**
     * Observes toolbar change events to be able to add any new buttons created by plugins to the plugin button map.
     *
     * @param event The change event
     * @since 3.4
     */
    private void handleChangeEvent(@Observes ToolbarChangeEvent event)
    {
        final ToolbarChangeEventType eventType = event.type();
        if (eventType == ToolbarChangeEventType.COMPONENT_ADDED)
        {
            final AToolbarComponent component = (AToolbarComponent) event.newValue();
            addComponentToPluginButtonMap(component);
        }
    }

    /**
     * Adds the provided component to the plugin button map if it is a plugin button that is not already in the map.
     *
     * @param component The component to check
     * @since 3.4
     */
    private void addComponentToPluginButtonMap(IToolbarComponent component)
    {
        final String pluginUniqueKey = component.getPluginUniqueKey();
        if (pluginUniqueKey != null && component instanceof ToolbarButton button)
        {
            final List<ToolbarButton> buttons = pluginButtonMap.computeIfAbsent(pluginUniqueKey, k -> new ArrayList<>());
            final boolean idNotPresent = buttons.stream().noneMatch(b -> b.getId().equals(button.getId()));
            if (idNotPresent) buttons.add(button);
        }
    }

    @Override
    public void addDrawingToolButton(@Nonnull IToolbarButton toolbarButton) throws IllegalArgumentException
    {
        if (!(toolbarButton instanceof ToolbarButton button))
        {
            throw new IllegalArgumentException("IToolbarButton instances must be created via IToolbarButtonBuilder");
        }

        if (button.getType() != ButtonType.PLUGIN_DRAW_TOOL)
        {
            throw new IllegalArgumentException("Only plugin drawing tool buttons can be added via this method");
        }

        getDrawTabWrapper().addListenerToPluginDrawTool(button);

        // Make the button available to be added to toolbar configurations. Since the DRAW tab includes these
        // button types as dynamic buttons, it will also be added to any DRAW tab instances automatically.
        toolbarConfigurationManager.addReferenceButton(button);

        pluginButtonMap.computeIfAbsent(button.getPluginUniqueKey(), k -> new ArrayList<>()).add(button);
    }

    @Nonnull
    private DrawToolbarTabWrapper getDrawTabWrapper()
    {
        return getTabWrapper(TabType.DRAW);
    }

    /**
     * Get the tab wrapper for the given tab type.
     *
     * @param tabType Type of tab wrapper to return
     * @param <T> Inferred type
     * @return Tab for the given type
     * @since 2.37
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    private <T extends AToolbarTabWrapper> T getTabWrapper(@Nonnull TabType tabType)
    {
        return (T) tabWrappers.get(tabType);
    }

    @Override
    @Nonnull
    public IToolbarSeparator createSeparator(@Nonnull IPlugin plugin, @Nonnull String uniqueName)
    {
        return new ToolbarSeparator(plugin, uniqueName);
    }

    @Override
    @Nonnull
    public IToolbarTab createTab(@Nonnull IPlugin plugin, @Nonnull String displayText)
    {
        return ToolbarTab.createPluginTab(plugin, displayText);
    }

    /**
     * Since some plugins will call this on the EDT, we need it to be on the EDT for all callers to avoid tabs being
     * added to the toolbar out of order.
     * <p/>
     * {@inheritDoc}
     */
    @InvokeOnEDT
    @Override
    public void addTab(@Nonnull IToolbarTab tab)
    {
        insertTab(tab, toolbarConfigurationManager.getReferenceTabs().size());
    }

    /**
     * Since some plugins will call this on the EDT, we need it to be on the EDT for all callers to avoid tabs being
     * added to the toolbar out of order.
     * <p/>
     * {@inheritDoc}
     */
    @InvokeOnEDT
    @Override
    public void insertTab(@Nonnull IToolbarTab tab, int index) throws IllegalArgumentException
    {
        if (!(tab instanceof final ToolbarTab toolbarTab))
        {
            throw new IllegalArgumentException("'tab' must be created using IToolbarManager");
        }

        final TabType tabType = toolbarTab.getType();
        if (tabType.isSingular())
        {
            throw new IllegalArgumentException("Tabs with a singular type cannot be added again: " + tabType);
        }

        toolbarConfigurationManager.insertReferenceTab(toolbarTab, index);
        pluginTabMap.computeIfAbsent(toolbarTab.getPluginUniqueKey(), k -> new ArrayList<>()).add(toolbarTab);
        tab.getComponents().forEach(this::addComponentToPluginButtonMap);
    }

    @Override
    public void removeTab(@Nonnull IToolbarTab tab) throws IllegalArgumentException
    {
        if (!(tab instanceof final ToolbarTab toolbarTab))
        {
            throw new IllegalArgumentException("'tab' must be created using IToolbarManager");
        }

        final TabType tabType = toolbarTab.getType();
        if (tabType.isSingular())
        {
            throw new IllegalArgumentException("Tabs with a singular type cannot be deleted: " + tabType);
        }

        toolbarConfigurationManager.removeReferenceTab(toolbarTab);
        final String pluginUniqueKey = toolbarTab.getPluginUniqueKey();
        if (pluginTabMap.containsKey(pluginUniqueKey)) pluginTabMap.get(pluginUniqueKey).remove(toolbarTab);
    }

    @Override
    public UUID getSelectedTabId()
    {
        return toolbar.getSelectedTabId();
    }

    @Override
    public boolean selectSingularTab(@Nonnull TabType tabType) throws IllegalArgumentException
    {
        if (!tabType.isSingular()) throw new IllegalArgumentException("Tab type must be singular but was not: " + tabType);

        return selectTab(tabType.requireSingularId());
    }

    @Override
    public boolean selectTab(@Nonnull UUID tabId) throws IllegalArgumentException
    {
        if (getTab(tabId) == null) throw new IllegalArgumentException("No tab found for provided id: " + tabId);

        return toolbar.selectTab(tabId);
    }

    @Override
    public void clickButton(@Nonnull UUID buttonId) throws IllegalArgumentException
    {
        final IToolbarButton button = getButton(buttonId);
        if (button == null) throw new IllegalArgumentException("No button found for provided id: " + buttonId);

        // TODO KMB: if button is not on the toolbar, it won't be clicked. (TAKX-1499)
        toolbar.doButtonClick(buttonId);
    }

    @Override
    public boolean getToggleButtonState(@Nonnull UUID buttonId) throws IllegalArgumentException
    {
        verifyToggleButtonId(buttonId);
        return toolbar.getToggleButtonState(buttonId);
    }

    @Override
    public void setToggleButtonState(@Nonnull UUID buttonId, boolean selected) throws IllegalArgumentException
    {
        verifyToggleButtonId(buttonId);
        toolbar.setToggleButtonState(buttonId, selected);
    }

    /**
     * Verifies the provided id belongs to a toggle button.
     *
     * @param buttonId The id to check
     * @throws IllegalArgumentException if no buttons exists with the provided id or if the button with the provided id
     * is not a toggle button.
     * @since 3.4
     */
    private void verifyToggleButtonId(@Nonnull UUID buttonId) throws IllegalArgumentException
    {
        final IToolbarButton button = getButton(buttonId);
        if (button == null) throw new IllegalArgumentException("No button found for provided id: " + buttonId);
        if (!button.isTogglable()) throw new IllegalArgumentException("Button with provided id is not togglable: " + button);
    }

    @Override
    @Nullable
    public IToolbarTab getSingularTab(@Nonnull TabType tabType) throws IllegalArgumentException
    {
        if (!tabType.isSingular()) throw new IllegalArgumentException("Tab type must be singular but was not: " + tabType);

        return toolbarConfigurationManager.getReferenceTab(tabType.requireSingularId());
    }

    @Override
    @Nullable
    public IToolbarTab getTab(@Nonnull UUID tabId)
    {
        return toolbarConfigurationManager.getReferenceTab(tabId);
    }

    @Override
    @Nonnull
    public List<? extends IToolbarTab> getTabs(@Nonnull TabType tabType)
    {
        return toolbarConfigurationManager.getReferenceTabs(tabType);
    }

    @Nonnull
    @Override
    public List<? extends IToolbarTab> getTabs()
    {
        return toolbarConfigurationManager.getReferenceTabs();
    }

    @Nonnull
    @Override
    public List<? extends IToolbarTab> getActiveTabs()
    {
        return toolbarConfigurationManager.getActiveTabs();
    }

    @Override
    @Nullable
    public IToolbarButton getSingularButton(@Nonnull ButtonType buttonType) throws IllegalArgumentException
    {
        if (!buttonType.isSingular()) throw new IllegalArgumentException("Button type must be singular but was not: " + buttonType);

        return toolbarConfigurationManager.getReferenceButton(buttonType.requireSingularId());
    }

    @Override
    public IToolbarButton getButton(@Nonnull UUID buttonId)
    {
        return toolbarConfigurationManager.getReferenceButton(buttonId);
    }

    @Override
    @Nonnull
    public List<? extends IToolbarButton> getButtons(@Nonnull ButtonType buttonType)
    {
        return toolbarConfigurationManager.getReferenceButtons(buttonType);
    }

    @Nonnull
    @Override
    public Set<? extends IToolbarButton> getButtons()
    {
        return toolbarConfigurationManager.getReferenceButtons();
    }

    /**
     * Resets tab map when the project is closed. This is to ensure that when another project is opened/created, the
     * references from plugins that were shut down are not kept around.
     */
    private void cleanup(@Observes ProjectClosed event)
    {
        pluginTabMap.values().stream().flatMap(Collection::stream).forEach(toolbarConfigurationManager::removeReferenceTab);
        pluginTabMap.clear();

        pluginButtonMap.values().stream().flatMap(Collection::stream)
                .forEach(button -> toolbarConfigurationManager.removeReferenceButton(button, true));
        pluginButtonMap.clear();
    }

    /**
     * Listener to cleanup after apps when they stop.
     * @since 2.37
     */
    private class AppCleanupListener implements IAppListener
    {
        /**
         * This method will remove any tabs and buttons created by the app plugin that was just stopped.
         * <p>
         * Note on the use of @InvokeAndWaitOnEDT: It is confirmed when using @InvokeOnEDT, we are not allowing enough
         * time for the App plugin to shut down which will result in a {@code ContextNotActiveException("No active
         * contexts for scope type jakarta.enterprise.context.ApplicationScoped")}. Switching to invokeAndWait allows
         * enough time for the clean up to finish. Per Larry, need to find alternative ways to clean up the toolbars
         * that are owned by the app plugin.
         *
         * @param app the application that was just stopped
         */
        @Override
        @InvokeAndWaitOnEDT
        public void onAppStopped(IImmutableAppInstance app)
        {
            final String appUniqueKey = KeyUtils.getUniqueKey(app.getFamily(), app.getType());

            final List<ToolbarTab> tabs = pluginTabMap.remove(appUniqueKey);
            if (tabs != null) tabs.forEach(toolbarConfigurationManager::removeReferenceTab);

            final List<ToolbarButton> buttons = pluginButtonMap.remove(appUniqueKey);
            if (buttons != null) buttons.forEach(button -> toolbarConfigurationManager.removeReferenceButton(button, true));
        }
    }
}
