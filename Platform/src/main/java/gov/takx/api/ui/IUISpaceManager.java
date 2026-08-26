package gov.takx.api.ui;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.api.ui.docking.DockState;
import gov.takx.api.ui.docking.DockingPortNotFoundException;
import gov.takx.platform.ui.ComponentDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * The UI Space Manager is responsible for allocating space among the primary areas of the UI application frame.
 * <p/>
 * For event management see {@link UISpaceFocusEvent}, {@link UISpaceResizeEvent}, {@link UISpaceStateChangeEvent},
 * and {@link UISpaceVisibilityEvent}. You can trap the events by using the {@link jakarta.enterprise.event.Observes}
 * annotation.
 * <p/>
 * Example:<br>
 * {@code public void visibilityChanged(@Observes UISpaceVisibilityEvent event){...}}
 *
 */
@InjectableService
@NoPluginImplementations
public interface IUISpaceManager
{
    /**
     * Ensure that the globe is visible and has focus.
     */
    void showGlobe();

    /**
     * Add a component to the application UI.
     * <p>
     * The component will be initially docked to the location specified in the ComponentDescriptor's ComponentPosition.
     * <p>
     * Note that if the component was previously added, then this call does nothing.
     *
     * @param componentDescriptor The descriptor to uniquely identify and specify initial properties of the component
     * @param component The component to be added
     * @return The button on the toolbar associated with toggling the component (or {@code null} if
     * {@link ComponentDescriptor#isToolbarButtonVisible()} returned false)
     * @apiNote this used to be DockingSystem:dockMinimizedComponentToGlobePanel
     * @since 2.15
     */
    @Nullable
    IToolbarButton addComponent(@Nonnull ComponentDescriptor componentDescriptor, @Nonnull Component component);

    /**
     * Remove the specified component from the UI.
     *
     * @param component The component to be removed
     * @since 2.15 (updated in 3.4 to take Component instead of ComponentDescriptor)
     */
    void removeComponent(@Nonnull Component component);

    /**
     * Create a docking port that supports the docking and undocking of components.
     * <p>
     * The returned {@code JComponent} can then be added to a user interface to create an additional area
     * where dockable {@code Components} can be managed.
     * <p>
     * See {@link #addComponentToDockingPort(String, ComponentDescriptor, Component)}
     *
     * @param portIdentifier Unique identifier to associate with a docking port
     * @return The docking port; add this to your UI where you want
     * @throws IllegalArgumentException if {@code portIdentifier} is blank or not unique
     * @since 2.15
     */
    @Nonnull
    @Experimental
    JComponent createDockingPort(@Nonnull String portIdentifier) throws IllegalArgumentException;

    /**
     * Create a docking port that supports the docking and undocking of components.
     * <p>
     * The returned {@code JComponent} can then be added to a user interface to create an additional area
     * where dockable {@code Components} can be managed.
     * <p>
     * This method allows optional {@code name} as a visual display attribute and {@code preferredSize}
     * to be used to define the preferred dimensions of the docking port.
     *
     * @param portIdentifier Unique identifier to associate with a docking port
     * @param name Optional name used for display purposes such as a title string
     * @param preferredSize The preferred size of the docking port
     * @return The docking port; add this to your UI where you want
     * @throws IllegalArgumentException if {@code portIdentifier} is blank or not unique
     * @since 2.15
     */
    @Nonnull
    @Experimental
    JComponent createDockingPort(@Nonnull String portIdentifier, String name, Dimension preferredSize)
            throws IllegalArgumentException;

    /**
     * Docks the specified {@code Component} to the docking port identified by {@code portIdentifier}.
     *
     * @param portIdentifier Identifier of the docking port
     * @param componentDescriptor The descriptor to uniquely identify and specify initial properties of the component
     * @param component The {@code Component} to be added
     * @throws IllegalArgumentException if the {@code portIdentifier} is blank
     * @throws DockingPortNotFoundException if no docking port with the {@code portIdentifier} exists
     * @since 2.15
     */
    @Experimental
    void addComponentToDockingPort(@Nonnull String portIdentifier, @Nonnull ComponentDescriptor componentDescriptor,
                                   @Nonnull Component component)
            throws IllegalArgumentException, DockingPortNotFoundException;

    /**
     * Docks the specified {@code Component} in a tab next to an existing {@code parent} component.
     * <p>
     * The parent must be currently docked; otherwise, this method will throw an {@code IllegalArgumentException}.
     * Note that this method is equivalent to {@link #addChildComponent(ComponentDescriptor, Component, Component, ComponentPosition)},
     * with componentPosition = CENTER.
     *
     * @param componentDescriptor The descriptor to uniquely identify and specify initial properties of the component
     * @param component The {@code Component} to be docked
     * @param parent The {@code Component} used as a reference point for docking
     * @throws IllegalArgumentException if {@code parent} is not already docked
     * @see #addChildComponent(ComponentDescriptor, Component, Component, ComponentPosition)
     * @since 2.15
     */
    @Experimental
    void addChildComponent(@Nonnull ComponentDescriptor componentDescriptor, @Nonnull Component component, @Nonnull Component parent)
            throws IllegalArgumentException;

    /**
     * Docks the specified {@code Component} relative to an existing {@code parent} component.
     * <p>
     * The parent must be currently docked; otherwise, this method will throw an {@code IllegalArgumentException}.
     * Note that this method is equivalent to
     * {@link #addChildComponent(ComponentDescriptor, Component, Component, ComponentPosition, float)},
     * with ratio = 0.5.
     *
     * @param componentDescriptor The descriptor to uniquely identify and specify initial properties of the component
     * @param component The {@code Component} to be docked
     * @param parent The {@code Component} used as a reference point for docking
     * @param position The position of the child {@code Component} relative to the parent
     * @throws IllegalArgumentException if {@code parent} is not already docked
     * @see #addChildComponent(ComponentDescriptor, Component, Component, ComponentPosition, float)
     * @since 2.15
     */
    @Experimental
    void addChildComponent(@Nonnull ComponentDescriptor componentDescriptor, @Nonnull Component component, @Nonnull Component parent,
                           @Nonnull ComponentPosition position)
            throws IllegalArgumentException;

    /**
     * Docks the specified {@code Component} relative to an existing {@code parent} component.
     * <p>
     * The parent must be currently docked; otherwise, this method will throw an {@code IllegalArgumentException}.
     * This overload of the method allows setting the proportional size of the child relative to the parent using
     * the {@code ratio} parameter.
     *
     * @param descriptor The descriptor to uniquely identify and specify initial properties of the component
     * @param component The {@code Component} to be docked
     * @param parent The {@code Component} used as a reference point for docking
     * @param position The position of the child {@code Component} relative to the parent
     * @param ratio The proportional size of the child relative to the parent in the range (0, 1)
     * @throws IllegalArgumentException if {@code parent} is not already docked
     * @since 2.15
     */
    @Experimental
    void addChildComponent(@Nonnull ComponentDescriptor descriptor, @Nonnull Component component, @Nonnull Component parent,
                           @Nonnull ComponentPosition position, float ratio)
            throws IllegalArgumentException;

    /**
     * Makes the specified component visible to the user. If it is docked, show it docked. If it is undocked, move it to
     * the front. If it is minimized or closed, then open it and show it.
     *
     * @param component The component to show
     * @since 2.15 (updated in 3.4 to take Component instead of ComponentDescriptor)
     */
    void showComponent(@Nonnull Component component);

    /**
     * Shows the dockable component in an "externalized" mode at the location specified in the component descriptor.
     * <p>
     * If docked, the component will be undocked. Normally ({@code forceLocation = false}), if the user has externalized
     * the component previously, then the system will use the previous location. If {@code forceLocation = true}, the
     * location specified by ComponentDescriptor.externalizedRect will be used.
     *
     * @param component The component to show
     * @param forceLocation True to override any location the user may have moved the component's externalized window to.
     * @throws IllegalStateException if an externalized rectangle was not set for the component
     * @since 2.15 (renamed and updated in 3.4 to take Component instead of ComponentDescriptor)
     */
    void showExternalizedComponent(@Nonnull Component component, boolean forceLocation) throws IllegalStateException;

    /**
     * Toggles a component's visibility. In the case of a CENTER-positioned component, brings it to the front.
     * <p>
     * Note that in many cases a component cannot be toggled. For example if the user has undocked it. In these cases,
     * this method has no effect.
     *
     * @param component The component to toggle
     * @since 2.15
     */
    void toggleComponent(@Nonnull Component component);

    /**
     * Hides the specified component, effectively re-docking if necessary and then minimizing it.
     *
     * @param component The component to hide
     * @since 2.15
     */
    void hideComponent(@Nonnull Component component);

    /**
     * Scan all dockables, and if any are externalized, then re-dock them.  Their externalized location may also be
     * updated if the current location is not within the bounds of any active screen device.
     *
     * @since 5.8
     */
    void reDockAllExternalizedComponents();

    /**
     * Sets the state of a dockable component's tray button while the component is minimized.
     *
     * @param component The component to update
     * @param dockState The new state of the dockable component's tray button while the component is minimized
     * @since 2.24
     */
    void setDockedComponentState(@Nonnull Component component, @Nonnull DockState dockState);

    /**
     * Resizes the dockable component wrapping this component. This should only be invoked if the component's
     * contents change significantly. This method should be used as sparingly as possible to avoid user confusion.
     *
     * @param component The component to update
     * @param dimension The new size of the dockable component wrapping the component
     * @since 2.25
     */
    void setComponentSize(@Nonnull Component component, @Nonnull Dimension dimension);

    /**
     * Sets the icon image for the component in the toolbar and its tab (if applicable). Note that calling this method
     * causes a full replacement of the icon images.
     *
     * @param component The component for which to set the icon image
     * @param iconImage The icon image to set
     * @since 3.2
     */
    void setComponentIconImage(@Nonnull Component component, @Nonnull BufferedImage iconImage);

    /**
     * Sets the icon image for the component in the toolbar and its tab (if applicable). Note that calling this method
     * causes a full replacement of the icon images.
     *
     * @param component The component for which to set the icon image
     * @param iconImage The icon image to set
     * @param tabIconImage Optional icon image to use in the dockable's tab, if applicable. If {@code null},
     *                     {@code iconImage} will be used.
     * @since 3.4.1
     */
    void setComponentIconImage(@Nonnull Component component, @Nonnull BufferedImage iconImage,
                               @Nullable BufferedImage tabIconImage);

    /**
     * Set the title text to the specified string, replacing the default title of the component name. The
     * component name is not changed.
     *
     * @param component The component whose title is to be changed.
     * @param title     The new title.
     * @since 3.4.0
     */
    void setComponentTitleText(@Nonnull Component component, @Nonnull String title);

    /**
     * Add the given component to the "persistent" area of the main app frame.  These components are always visible,
     * and the user has no mechanism to control that. It is up to the plugin to make any user accommodations.
     * <p>
     * Components are added in a left-to-right order as they are added by plugins.  There is no mechanism to control
     * the ordering beyond that.
     * <p>
     * Note: This UI location is intended for small components, not large panels.  So, take care with what you add to
     * this location, as it could drastically distort the user experience.
     *
     * @param component Component to add
     * @since 5.4
     */
    void addPersistentComponent(@Nonnull Component component);

    /**
     * Remove the given component to the "persistent" area of the main app frame.
     *
     * @param component Component to remove
     * @since 5.4
     */
    void removePersistentComponent(@Nonnull Component component);
}
