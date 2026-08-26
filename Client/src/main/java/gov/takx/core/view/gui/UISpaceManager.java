package gov.takx.core.view.gui;

import bibliothek.gui.dock.common.location.CExternalizedLocation;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.docking.DockState;
import gov.takx.api.ui.docking.DockingPortNotFoundException;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.launcher.ApplicationLauncher;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarButtonBuilder;
import gov.takx.core.toolbar.ToolbarConfigurationManager;
import gov.takx.core.view.gui.docking.ComponentDockable;
import gov.takx.core.view.gui.docking.DockableAndProps;
import gov.takx.core.view.gui.docking.DockingManager;
import gov.takx.core.view.gui.docking.DockingStateNotifier;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static bibliothek.gui.dock.common.mode.ExtendedMode.MINIMIZED;
import static gov.takx.platform.ui.UIResources.STANDARD_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.TOOLBAR_BUTTON_IMAGE_SIZE;

/**
 * The UI Space Manager is responsible for allocating space among the primary tabbed areas of the TAKX
 * UI Application frame.
 */
@ApplicationScoped
public class UISpaceManager implements IExtendedUISpaceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // A simple consumer to bring the dockable to the front if it is showing.
    private static final Consumer<DockableAndProps> TO_FRONT_HANDLER = dockableAndProps -> {
        final ComponentDockable dockable = dockableAndProps.getDockable();
        if (dockable != null && !dockable.isShowing()) dockable.toFront();
    };

    // Set of components that we need to call clearState/shutdown on
    private final Set<IRaptorLifecycleListener> statefulObjects = new HashSet<>();

    private final boolean headless;
    private final DockingManager dockingManager;
    private final DockingStateNotifier dockingStateNotifier;
    private final ToolbarConfigurationManager configurationManager;
    private final IAppFrame appFrame;

    @Inject
    UISpaceManager(@Headless boolean headless, DockingManager dockingManager, DockingStateNotifier dockingStateNotifier,
                   ToolbarConfigurationManager configurationManager, ApplicationLauncher applicationLauncher)
    {
        this.headless = headless;
        this.dockingManager = dockingManager;
        this.dockingStateNotifier = dockingStateNotifier;
        this.configurationManager = configurationManager;
        appFrame = applicationLauncher.getAppFrame();
    }

    @Override
    public void toggleComponent(@Nonnull CoreComponentType coreComponentType)
    {
        toggleComponent(dockingManager.findDockableAndPropsById(coreComponentType.getComponentDescriptorId()));
    }

    @Override
    public void toggleComponent(@Nonnull Component component)
    {
        toggleComponent(dockingManager.findDockableAndProps(component));
    }

    private void toggleComponent(DockableAndProps dockableAndProps)
    {
        if (dockableAndProps != null)
        {
            final ComponentDockable dockable = dockableAndProps.getDockable();

            if (dockable == null || !dockable.isShowing())
            {
                showComponent(dockableAndProps, TO_FRONT_HANDLER);
            } else
            {
                if (!dockable.getExtendedMode().equals(ExtendedMode.EXTERNALIZED))
                {
                    dockable.setExtendedMode(MINIMIZED);
                }
            }
        }
    }

    @Nullable
    @Override
    public IToolbarButton addComponent(@Nonnull ComponentDescriptor descriptor, @Nonnull Component component)
    {
        IToolbarButton button = null;
        // Try to prevent exceptions when a plugin calls this method when we're headless
        if (!headless)
        {
            dockingManager.dockComponent(component, descriptor);
            if (descriptor.isToolbarButtonVisible())
            {
                button = addToolbarButton(descriptor, component);
            }

            // If it implements the IRaptorLifecycleListener interface, then we will fire load/clear/shutdown events to it
            if (component instanceof IRaptorLifecycleListener lifecycleListener) statefulObjects.add(lifecycleListener);
        }

        return button;
    }

    @Override
    public void addPersistentComponent(@Nonnull Component component)
    {
        appFrame.addPersistentComponent(component);
    }

    @Override
    public void removePersistentComponent(@Nonnull Component component)
    {
        appFrame.removePersistentComponent(component);
    }

    /**
     * Add a toolbar button to the Apps tab to toggle the component associated with the provided component descriptor.
     *
     * @param componentDescriptor The component descriptor for which to add a toolbar button
     * @param component The component to be toggled by the button
     * @return The button that was added (or the existing button if there was already a button for toggling the component)
     * @since 3.4 (moved from AppsToolbarTabWrapper)
     */
    private IToolbarButton addToolbarButton(@Nonnull ComponentDescriptor componentDescriptor, Component component)
    {
        // This can be called as part of recreating a dockable that was previously closed by the user, meaning a button
        // already exists, so we should first check if this component descriptor is associated with a button before
        // creating a new one.
        final String componentDescriptorId = componentDescriptor.getId();
        final UUID buttonId = configurationManager.getIdOfButtonWithComponentDescriptorId(componentDescriptorId);
        if (buttonId != null) return configurationManager.getReferenceButton(buttonId);

        final ToolbarButton button = ToolbarButtonBuilder.createCoreButtonBuilder(ButtonType.PLUGIN_PANEL, componentDescriptor.getName())
                .addActionListener(e -> toggleComponent(component))
                .setTooltipText(componentDescriptor.getTooltipText())
                .setTogglable(true)
                .setImage(UIResources.getSizedImage(componentDescriptor.getIconUrl(), TOOLBAR_BUTTON_IMAGE_SIZE))
                .setComponentDescriptorId(componentDescriptorId)
                .build();

        // Add the button to the set of available reference buttons, which will also auto-add it to any relevant tabs.
        configurationManager.addReferenceButton(button);

        return button;
    }

    @Override
    public void removeComponent(@Nonnull Component component)
    {
        final DockableAndProps dockableAndProps = dockingManager.findDockableAndProps(component);

        dockingManager.removeDockable(dockableAndProps);

        if (dockableAndProps != null)
        {
            final UUID buttonId = configurationManager.getIdOfButtonWithComponentDescriptorId(dockableAndProps.getId());
            if (buttonId != null) configurationManager.removeButton(buttonId);
        }
    }

    @Override
    public void showGlobe()
    {
        dockingManager.showPrimaryGlobe();
    }

    @Override
    public void showComponent(@Nonnull CoreComponentType coreComponentType)
    {
        showComponent(dockingManager.findDockableAndPropsById(coreComponentType.getComponentDescriptorId()), TO_FRONT_HANDLER);
    }

    @Override
    public void showComponent(@Nonnull Component component)
    {
        showComponent(dockingManager.findDockableAndProps(component), TO_FRONT_HANDLER);
    }

    @Override
    public void showExternalizedComponent(@Nonnull Component component, boolean forceLocation) throws IllegalStateException
    {
        showComponent(dockingManager.findDockableAndProps(component),
                dockableAndProps -> {
                    CExternalizedLocation externalizedLocation = dockableAndProps.getExternalizedLocation();
                    if (externalizedLocation == null || forceLocation)
                    {
                        // Reset the current externalized rectangle to the original value provided in the descriptor
                        final ComponentDescriptor descriptor = dockableAndProps.getDescriptor();
                        final Rectangle externalizedRect = descriptor.getExternalizedRect();
                        if (externalizedRect == null)
                        {
                            throw new IllegalStateException("No externalized rectangle set for component. " +
                                    "This must be set in the ComponentDescriptor. component = " + component +
                                    "; descriptor = " + descriptor);
                        }

                        dockableAndProps.setExternalizedLocation(externalizedRect);
                        externalizedLocation = dockableAndProps.getExternalizedLocation();
                    }

                    if (!dockableAndProps.hasDockable())
                    {
                        logger.warn("Dockable doesn't exist for component (may have been closed by user). {}", component);
                        return;
                    }

                    dockableAndProps.requireDockable().setLocation(externalizedLocation);
                });
    }

    /**
     * Show the given dockable, which must have been docked at one time. If it has never been docked, then this
     * method does nothing. If it has, but there is no current dockable (the user closed it), then recreate
     * the dockable. Pass the resulting dockable to the handler for final processing.
     *
     * @param dockableAndProps Dockable to process, if {@code null}, ignored
     * @param dockableHandler Invoked to handle the actual "show" operation
     */
    private void showComponent(@Nullable DockableAndProps dockableAndProps, @Nullable Consumer<DockableAndProps> dockableHandler)
    {
        if (dockableAndProps == null) return; // Never been docked, can't show it

        // If there is no associated dockable it must have been closed by the user.
        // We'll want to re-create it in order to show it again.
        if (!dockableAndProps.hasDockable())
        {
            addComponent(dockableAndProps.getDescriptor(), dockableAndProps.getComponent());
            dockableAndProps = dockingManager.findDockableAndPropsById(dockableAndProps.getDescriptor().getId());
        }

        // With the existing (or newly made) dockable make it visible and call the handler if specified.
        if (dockableAndProps != null)
        {
            ComponentDockable dockable = dockableAndProps.getDockable();
            if (dockable != null) dockable.setVisible(true);
            if (dockableHandler != null) dockableHandler.accept(dockableAndProps);
        }
    }

    @Override
    public void hideComponent(@Nonnull CoreComponentType coreComponentType)
    {
        hideComponent(dockingManager.findDockableAndPropsById(coreComponentType.getComponentDescriptorId()));
    }

    @Override
    public void hideComponent(@Nonnull Component component)
    {
        hideComponent(dockingManager.findDockableAndProps(component));
    }

    private void hideComponent(@Nullable DockableAndProps dockableAndProps)
    {
        if (dockableAndProps == null || !dockableAndProps.hasDockable()) return;

        final ComponentDockable dockable = dockableAndProps.requireDockable();
        final ExtendedMode extendedMode = dockable.getExtendedMode();

        // AFAICT, minimizing an already minimized dockable causes the global focus to change, which
        // interferes with the MapObject primary dialog obtaining focus when it is displayed.
        if (!ExtendedMode.EXTERNALIZED.equals(extendedMode) && !MINIMIZED.equals(extendedMode))
        {
            dockable.setExtendedMode(MINIMIZED);
        }
    }

    @Nonnull
    @Override
    public JComponent createDockingPort(@Nonnull String portIdentifier) throws IllegalArgumentException
    {
        if (portIdentifier.isEmpty()) throw new IllegalArgumentException("'portIdentifier' must not be empty.");

        return (JComponent) dockingManager.createContentArea(portIdentifier).getComponent();
    }

    @Nonnull
    @Override
    public JComponent createDockingPort(@Nonnull String portIdentifier, @Nullable String name, @Nullable Dimension preferredSize)
            throws IllegalArgumentException
    {
        if (portIdentifier.isEmpty()) throw new IllegalArgumentException("'portIdentifier' must not be empty.");

        final JComponent component = (JComponent) dockingManager.createContentArea(portIdentifier).getComponent();

        component.setName(name);
        component.setPreferredSize(preferredSize);
        return component;
    }

    @Override
    public void addComponentToDockingPort(@Nonnull String portIdentifier, @Nonnull ComponentDescriptor descriptor, @Nonnull Component component)
            throws IllegalArgumentException, DockingPortNotFoundException
    {
        dockingManager.addComponentToDockingPort(portIdentifier, descriptor, component);
    }

    @Override
    public void addChildComponent(@Nonnull ComponentDescriptor descriptor, @Nonnull Component component,
                                  @Nonnull Component parent)
            throws IllegalArgumentException
    {
        addChildComponent(descriptor, component, parent, ComponentPosition.CENTER);
    }

    @Override
    public void addChildComponent(@Nonnull ComponentDescriptor descriptor, @Nonnull Component component,
                                  @Nonnull Component parent, @Nonnull ComponentPosition position)
    {
        addChildComponent(descriptor, component, parent, position, .5f);
    }

    @Override
    public void addChildComponent(@Nonnull ComponentDescriptor descriptor, @Nonnull Component component,
                                  @Nonnull Component parent, @Nonnull ComponentPosition position, float ratio)
            throws IllegalArgumentException
    {
        if (ratio < 0.0 || ratio > 1.0) throw new IllegalArgumentException("Ratio must be from 0.0 - 1.0");

        dockingManager.dockChildComponent(descriptor, component, parent, position, ratio);
    }

    @Override
    public void setDockedComponentState(@Nonnull Component component, @Nonnull DockState dockState)
    {
        if (UserPreferenceEntry.DOCK_BUTTON_NOTIFICATIONS.getBooleanValue())
        {
            dockingStateNotifier.setDockableState(component, dockState);
        }
    }

    @Override
    public void setComponentSize(@Nonnull Component component, @Nonnull Dimension dimension)
    {
        dockingManager.setDockableSize(component, dimension);
    }

    @Override
    public void setComponentIconImage(@Nonnull Component component, @Nonnull BufferedImage iconImage)
    {
        setComponentIconImage(component, iconImage, null);
    }

    @Override
    public void setComponentIconImage(@Nonnull Component component, @Nonnull BufferedImage iconImage,
                                      @Nullable BufferedImage tabIconImage)
    {
        final DockableAndProps dockableAndProps = dockingManager.findDockableAndProps(component);
        if (dockableAndProps == null)
        {
            logger.warn("No dockable found for component {}; icon will not be updated", component);
            return;
        }

        final UUID buttonId = configurationManager.getIdOfButtonWithComponentDescriptorId(dockableAndProps.getId());
        if (buttonId != null)
        {
            final ToolbarButton button = configurationManager.getReferenceButton(buttonId);
            if (button != null) button.setImage(iconImage);
        }

        if (dockableAndProps.hasDockable())
        {
            dockableAndProps.requireDockable().setTitleIcon(
                    ImageUtils.getScaledIcon(tabIconImage != null ? tabIconImage : iconImage,
                            STANDARD_IMAGE_SIZE.width, STANDARD_IMAGE_SIZE.height));
        }

        // TODO KMB: Need to make sure the icon is retained if the dockable gets closed/reopened
    }

    @Override
    public void setComponentTitleText(@Nonnull Component component, @Nonnull String title)
    {
        final DockableAndProps dockableAndProps = dockingManager.findDockableAndProps(component);

        if (dockableAndProps == null)
        {
            logger.warn("No dockable and properties found for component {}; title will not be updated", component);
            return;
        }

        if (dockableAndProps.hasDockable())
        {
            dockableAndProps.requireDockable().setTitleText(title);
        } else
        {
            logger.warn("No dockable found for component {}; title will not be updated", component);
        }
    }

    /**
     * Observe project closing events and clear out our state.
     */
    private void handleProjectClosing(@Observes(notifyObserver = Reception.IF_EXISTS) ProjectClosing event)
    {
        final IProgressMonitor progressMonitor = event.getProgressMonitor();

        for (IRaptorLifecycleListener statefulObject : statefulObjects)
        {
            try
            {
                statefulObject.onClearState(progressMonitor);
            } catch (Exception e)
            {
                logger.error("Exception calling clearState on component: {}", statefulObject, e);
            }
        }

        statefulObjects.clear();
    }
}
