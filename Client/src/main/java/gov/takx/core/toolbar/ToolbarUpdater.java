package gov.takx.core.toolbar;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.ui.UISpaceVisibilityEvent;
import gov.takx.core.view.gui.toolbar.IToolbar;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/**
 * Handles updating the toolbar for a newly activated configuration or changes to the currently active configuration.
 *
 * @since 2.0 (Renamed in 3.4)
 */
@ApplicationScoped
public class ToolbarUpdater
{
    // The currently selected toolbar configuration
    private ToolbarConfiguration activeConfiguration = null;

    private final IToolbar toolbar;
    private final ToolbarConfigurationManager configurationManager;

    @Inject
    public ToolbarUpdater(IToolbar toolbar, ToolbarConfigurationManager configurationManager)
    {
        this.toolbar = toolbar;
        this.configurationManager = configurationManager;
    }

    /**
     * Activates the provided configuration, updating the toolbar accordingly.
     *
     * @param configuration Configuration to activate
     */
    @InvokeOnEDT
    public void activateConfiguration(@Nonnull ToolbarConfiguration configuration)
    {
        // First, deactivate any existing configuration
        if (activeConfiguration != null) deactivateConfiguration();

        activeConfiguration = configuration;

        // Create tabs from new configuration
        int index = 0;
        for (ToolbarTab tab : configuration.getTabs())
        {
            if (toolbar.insertTab(tab, index)) index++;
        }

        // Minimize the toolbar if necessary
        toolbar.setMinimized(configuration.isMinimized());
    }

    /**
     * Updates toolbar button's selection state to always match when panels are displayed or collapsed.
     *
     * @param uiSpaceVisibilityEvent a notification that a dockable panel has been expanded or collapsed
     * @since 3.1
     */
    @InvokeOnEDT
    public void panelVisibilityChanged(@Observes UISpaceVisibilityEvent uiSpaceVisibilityEvent)
    {
        final UUID buttonId = configurationManager.getIdOfButtonWithComponentDescriptorId(uiSpaceVisibilityEvent.getId());
        if (buttonId != null) toolbar.setToggleButtonState(buttonId, uiSpaceVisibilityEvent.isVisible());
    }

    /**
     * Deactivates the active configuration, updating the toolbar accordingly.
     */
    private void deactivateConfiguration()
    {
        // Remove old tabs/buttons from previous configuration
        toolbar.clear();
        activeConfiguration = null;
    }

    /**
     * Handles change events and updates the toolbar accordingly if the change affects the active configuration.
     *
     * @param event The change event
     */
    @InvokeOnEDT
    private void handleChangeEvent(@Observes ToolbarChangeEvent event)
    {
        // Only need to process the event if it is for the active configuration
        if (event.configuration().equals(activeConfiguration))
        {
            // The best code reuse is seen by first splitting the events based on the source.
            switch (event.type().getSourceType())
            {
                case CONFIGURATION -> handleChangeEventFromConfiguration(event);
                case TAB -> handleChangeEventFromTab(event);
                case BUTTON -> handleChangeEventFromButton(event);
            }
        }
    }

    /**
     * Handles events fired by a {@link ToolbarConfiguration}.
     *
     * @param event The change event
     */
    private void handleChangeEventFromConfiguration(@Nonnull ToolbarChangeEvent event)
    {
        ToolbarConfiguration configuration = (ToolbarConfiguration) event.source();
        ToolbarTab tab;

        final ToolbarChangeEventType eventType = event.type();
        switch (eventType)
        {
            case TAB_ADDED ->
            {
                tab = (ToolbarTab) event.newValue();
                int index = getTabEffectiveIndex(configuration.getTabs(), tab);
                toolbar.insertTab(tab, index);
            }
            case TAB_REMOVED ->
            {
                tab = (ToolbarTab) event.oldValue();
                toolbar.removeTab(tab);
            }
            default -> throw new IllegalStateException("Unexpected event type: " + eventType);
        }
    }

    /**
     * Determines the effective index for a tab, taking into account invisible tabs before it.
     *
     * @param siblingTabs The list of tabs to which the tab belongs
     * @param tab         The tab for which to find the effective index
     * @return The effective index of the tab
     */
    private int getTabEffectiveIndex(@Nonnull List<ToolbarTab> siblingTabs, @Nonnull ToolbarTab tab)
    {
        int index = 0;
        for (ToolbarTab toolbarTab : siblingTabs)
        {
            if (toolbarTab.equals(tab))
            {
                break;
            } else if (toolbarTab.isVisible() && toolbarTab.isValid())
            {
                index++;
            }
        }

        return index;
    }

    /**
     * Handles events fired by a {@link ToolbarTab}.
     *
     * @param event The change event
     */
    private void handleChangeEventFromTab(@Nonnull ToolbarChangeEvent event)
    {
        ToolbarConfiguration configuration = event.configuration();
        ToolbarTab tab = (ToolbarTab) event.source();
        AToolbarComponent component;
        int index;

        final ToolbarChangeEventType eventType = event.type();
        switch (eventType)
        {
            case TAB_TEXT_CHANGED -> toolbar.updateTabText(tab);
            case TAB_VALIDITY_CHANGED ->
            {
                boolean valid = (boolean) event.newValue();
                if (valid)
                {
                    index = getTabEffectiveIndex(configuration.getTabs(), tab);
                    toolbar.insertTab(tab, index);
                } else if (tab.isVisible())
                {
                    toolbar.removeTab(tab);
                }
            }
            case TAB_VISIBILITY_CHANGED ->
            {
                boolean visible = (boolean) event.newValue();
                if (visible)
                {
                    index = getTabEffectiveIndex(configuration.getTabs(), tab);
                    toolbar.insertTab(tab, index);
                } else if (tab.isValid())
                {
                    toolbar.removeTab(tab);
                }
            }
            case COMPONENT_ADDED ->
            {
                component = (AToolbarComponent) event.newValue();
                index = getComponentEffectiveIndex(tab.getComponents(), component);
                toolbar.insertComponent(component, tab, index);
            }
            case COMPONENT_REMOVED ->
            {
                component = (AToolbarComponent) event.oldValue();
                toolbar.removeComponent(component, tab);
            }
            default -> throw new IllegalStateException("Unexpected event type: " + eventType);
        }
    }

    /**
     * Determines the effective index for a component, taking into account invalid components before it.
     *
     * @param siblingComponents The list of components to which the component belongs
     * @param component         The component for which to find the effective index
     * @return The effective index of the component
     */
    private int getComponentEffectiveIndex(@Nonnull List<AToolbarComponent> siblingComponents, @Nonnull AToolbarComponent component)
    {
        int index = 0;
        for (AToolbarComponent toolbarComponent : siblingComponents)
        {
            if (toolbarComponent.equals(component))
            {
                break;
            } else if (toolbarComponent.isValid())
            {
                index++;
            }
        }

        return index;
    }

    /**
     * Handles events fired by a {@link ToolbarButton}.
     *
     * @param event The change event
     */
    private void handleChangeEventFromButton(@Nonnull ToolbarChangeEvent event)
    {
        ToolbarButton button = (ToolbarButton) event.source();
        ToolbarTab tab = button.requireTab();

        // Unless this is a validity change event, if the button is invalid drop the event, since the button doesn't
        // exist on the toolbar
        final ToolbarChangeEventType eventType = event.type();
        if (eventType != ToolbarChangeEventType.BUTTON_VALIDITY_CHANGED && !button.isValid()) return;

        switch (eventType)
        {
            case BUTTON_IMAGE_CHANGED, BUTTON_LISTENER_ADDED, BUTTON_LISTENER_REMOVED, BUTTON_TEXT_CHANGED, BUTTON_STATE_CHANGED, BUTTON_TOOLTIP_CHANGED -> toolbar.updateButton(button);
            case BUTTON_VALIDITY_CHANGED ->
            {
                boolean valid = (boolean) event.newValue();
                if (valid)
                {
                    final int buttonIndex = getComponentEffectiveIndex(tab.getComponents(), button);
                    toolbar.insertComponent(button, tab, buttonIndex);
                } else
                {
                    toolbar.removeComponent(button, tab);
                }
            }
            default -> throw new IllegalStateException("Unexpected event type: " + eventType);
        }
    }
}
