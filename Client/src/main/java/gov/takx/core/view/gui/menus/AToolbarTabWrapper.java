package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.FinalizeToolbars;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarButtonBuilder;
import gov.takx.core.toolbar.ToolbarConfigurationManager;
import gov.takx.core.toolbar.ToolbarTab;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.toolbar.IToolbar;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 */
@PseudoBean
public abstract class AToolbarTabWrapper implements IRaptorLifecycleListener
{
    @Inject
    protected ToolbarConfigurationManager configurationManager;

    @Inject
    @UiEventBus
    protected EventBus uiEventBus;

    @Inject
    protected IMapObjectManager mapObjectManager;

    @Inject
    protected IExtendedGlobeManager globeManager;

    @Inject
    protected IToolbar toolbar;

    @Inject
    protected IExtendedUISpaceManager uiSpaceManager;

    protected ToolbarTab tab;

    private boolean firstRun = true;

    /**
     * Finalize our toolbar components.
     * @since 5.2
     */
    private void finalizeToolbars(@Observes FinalizeToolbars ignored)
    {
        if (firstRun)
        {
            addComponents();
            firstRun = false;
        }
    }

    /**
     * Construct and add toolbar components.
     * @since 5.2
     */
    protected abstract void addComponents();

    /**
     * Create an empty tab, make it available, and add it to the default configuration.
     *
     * @param tabType Tab type to create
     * @param visible Whether the tab should be visible by default
     * @return Newly created tab
     * @since 5.2
     */
    @Nonnull
    protected ToolbarTab createTab(@Nonnull TabType tabType, boolean visible)
    {
        // TODO TAKX-4099 Toggle buttons don't work when added after the tab is created
        return createTab(tabType, Collections.emptyList(), visible);
    }

    /**
     * Create a tab, add the given components, make it available, and add it to the default configuration.
     *
     * @param tabType Tab type to create
     * @param components Initial components to add to the tab (may be an empty list)
     * @param visible Whether the tab should be visible by default
     * @return Newly created tab
     * @since 2.37 (parameters modified in 3.4)
     */
    @Nonnull
    protected ToolbarTab createTab(@Nonnull TabType tabType, @Nonnull List<AToolbarComponent> components, boolean visible)
    {
        tab = ToolbarTab.createSingularTab(tabType);
        components.forEach(component -> tab.addComponent(component, false));
        tab.setVisible(visible, false);

        configurationManager.addReferenceTab(tab); // This adds the tab to all configurations, including the default
        return tab;
    }

    /**
     * Create a toolbar button.
     *
     * @param buttonType the button type
     * @param displayText the text to display for the button
     * @param image the image containing the icon for the button
     * @param actionListener the action listener to be invoked when the button is pressed
     * @param tooltipText the tooltip text for the button
     * @return the newly constructed button
     * @since 3.4 (moved from ToolbarButtonFactory)
     */
    @Nonnull
    protected ToolbarButton createButton(@Nonnull ButtonType buttonType, @Nonnull String displayText,
                                         @Nonnull BufferedImage image, @Nonnull ActionListener actionListener,
                                         @Nullable String tooltipText)
    {
        return createButton(buttonType, displayText, actionListener, tooltipText)
                .setImage(image);
    }

    /**
     * Create a toolbar button with the image and display text defined by the provided {@code buttonType}.
     *
     * @param buttonType the button type
     * @param actionListener the action listener to be invoked when the button is pressed
     * @param tooltipText the tooltip text for the button
     * @return the newly constructed button
     * @since 3.4 (moved from ToolbarButtonFactory)
     */
    @Nonnull
    protected ToolbarButton createButton(@Nonnull ButtonType buttonType, @Nonnull ActionListener actionListener,
                                         @Nullable String tooltipText)
    {
        return createButton(buttonType, buttonType.requireDisplayText(), actionListener, tooltipText);
    }

    /**
     * Create a toolbar button with the image defined by the provided {@code buttonType}.
     *
     * @param buttonType the button type
     * @param displayText the text to display for the button
     * @param actionListener the action listener to be invoked when the button is pressed
     * @param tooltipText the tooltip text for the button
     * @return the newly constructed button
     * @since 3.4 (moved from ToolbarButtonFactory)
     */
    @Nonnull
    protected ToolbarButton createButton(@Nonnull ButtonType buttonType, @Nonnull String displayText,
                                         @Nonnull ActionListener actionListener, @Nullable String tooltipText)
    {
        return ToolbarButtonBuilder.createCoreButtonBuilder(buttonType, displayText)
                .addActionListener(actionListener)
                .setTooltipText(tooltipText)
                .build();
    }

    /**
     * Create a toolbar button that has a drop-down menu.
     *
     * @param buttonType the button type
     * @param actionListener the action listener to be invoked when the button is pressed
     * @param popupMenuProvider provider that produces the drop-down menu
     * @param tooltipText the tooltip text for the button
     * @return the newly constructed button
     * @since 3.4 (moved from ToolbarButtonFactory)
     */
    @Nonnull
    protected ToolbarButton createButtonWithPopupMenu(@Nonnull ButtonType buttonType,
                                                      @Nonnull ActionListener actionListener,
                                                      @Nonnull IPopupMenuProvider popupMenuProvider,
                                                      @Nullable String tooltipText)
    {
        return ToolbarButtonBuilder.createCoreButtonBuilder(buttonType, buttonType.requireDisplayText())
                .addActionListener(actionListener)
                .setPopupMenuProvider(popupMenuProvider)
                .setTooltipText(tooltipText)
                .build();
    }

    /**
     * Create a toolbar button that has a drop-down menu but no action on the main button.
     *
     * @param buttonType the button type
     * @param popupMenuProvider provider that produces the drop-down menu
     * @param tooltipText the tooltip text for the button
     * @return the newly constructed button
     * @since 3.5
     */
    @Nonnull
    protected ToolbarButton createButtonWithPopupMenu(@Nonnull ButtonType buttonType,
                                                      @Nonnull IPopupMenuProvider popupMenuProvider,
                                                      @Nullable String tooltipText)
    {
        return ToolbarButtonBuilder.createCoreButtonBuilder(buttonType, buttonType.requireDisplayText())
                .setPopupMenuProvider(popupMenuProvider)
                .setTooltipText(tooltipText)
                .build();
    }

    /**
     * Create a toggle toolbar button.
     *
     * @param buttonType the button type
     * @param actionListener the action listener to be invoked when the button is pressed
     * @param tooltipText the tooltip text for the button
     * @return the newly constructed button
     * @since 3.4 (moved from ToolbarButtonFactory)
     */
    @Nonnull
    protected ToolbarButton createToggleButton(@Nonnull ButtonType buttonType, @Nonnull ActionListener actionListener,
                                               @Nullable String tooltipText)
    {
        return ToolbarButtonBuilder.createCoreButtonBuilder(buttonType, buttonType.requireDisplayText())
                .addActionListener(actionListener)
                .setTogglable(true)
                .setTooltipText(tooltipText)
                .build();
    }

    /**
     * Creates a toolbar button that is used to toggle the panel identified by the provided core component type.
     *
     * @param coreComponentType The core component type to be toggled
     * @param tooltipText the tooltip text for the button
     * @since 2.37 (parameters changed in 3.4)
     */
    @Nonnull
    protected ToolbarButton createTogglePanelButton(@Nonnull CoreComponentType coreComponentType, @Nullable String tooltipText)
    {
        final ButtonType buttonType = coreComponentType.getButtonType();
        return createToggleButton(buttonType, e -> uiSpaceManager.toggleComponent(coreComponentType), tooltipText);
    }

    /**
     * @return The location of the center of the current (last focused) globe view
     * @since 2.37
     */
    protected ILocation getCenterLocationOnGlobe()
    {
        return globeManager.getLastFocusedGlobe().getCenterLocation();
    }

    /**
     * @return The last focused globe
     * @since 3.4.1
     */
    protected IApplicationGlobe getLastFocusedGlobe()
    {
        return globeManager.getLastFocusedGlobe();
    }
}
