package gov.takx.platform.ui;

import gov.takx.api.toolbar.TabType;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Component;
import java.awt.Rectangle;
import java.net.URL;

/**
 * Builder pattern for defining a new {@link ComponentDescriptor}.
 * <p/>
 * Note: As of TAKX version 3.2, all panels will be closeable by default. If a developer doesn't want users to have the
 * ability to close panels, be sure to call {@link #setCloseable(boolean) setCloseable(false)}.
 *
 * @since 2.15
 */
public class ComponentDescriptorBuilder
{
    private final String id;
    private final String name;
    private String tooltipText;
    private URL iconUrl = ImageUtils.DEFAULT_IMAGE_URL;
    private boolean buttonVisible = true;
    private ComponentPosition componentPosition = ComponentPosition.RIGHT;
    private boolean parentScrollingEnabled = false;
    private boolean closeable = true;
    private Boolean pinned = null;
    private boolean hidePin = false;
    private Rectangle externalizedRect = null;
    private boolean playbackAware = false;
    private boolean externalizable = true;

    /**
     * Constructs a builder that can produce a {@link ComponentDescriptor} using the canonical name of the class and the
     * provided title as the id.
     *
     * @param componentClass The class that will be used for the component
     * @param name The name and display title for the component
     * @since 3.4
     */
    public ComponentDescriptorBuilder(@Nonnull Class<? extends Component> componentClass, @Nonnull String name)
    {
        id = componentClass.getCanonicalName() + ":" + name;
        this.name = name;
    }

    /**
     * Constructs a builder that can produce a {@link ComponentDescriptor}.
     *
     * @param component The component that will be added to the user interface
     * @param name The name and display title for the component
     */
    public ComponentDescriptorBuilder(@Nonnull Component component, @Nonnull String name)
    {
        this(component.getClass(), name);
    }

    /**
     * Defines a resource accessible via the classpath for use as the toolbar button's icon. This button is used by the
     * user to toggle visibility of the component. If an icon is not provided, a default will be used.
     *
     * @param url URL pointing to a resource accessible via the classpath to use as the icon for the button in the toolbar
     * @return Builder
     */
    @Nonnull
    public ComponentDescriptorBuilder setIcon(@Nonnull URL url)
    {
        iconUrl = url;
        return this;
    }

    /**
     * Defines tooltip text for the button in the toolbar.
     *
     * @param tooltipText Tooltip text
     * @return Builder
     */
    @Nonnull
    public ComponentDescriptorBuilder setTooltipText(@Nullable String tooltipText)
    {
        this.tooltipText = tooltipText;
        return this;
    }

    /**
     * Defines the component position. The default is {@link ComponentPosition#RIGHT}.
     *
     * @param componentPosition component position within the application
     * @return Builder
     */
    @Nonnull
    public ComponentDescriptorBuilder setComponentPosition(@Nonnull ComponentPosition componentPosition)
    {
        this.componentPosition = componentPosition;
        return this;
    }

    /**
     * Defines whether a button will be added to the toolbar for toggling the component. If {@code buttonVisible} is
     * {@code true}, the button that is added will be returned by
     * {@link IUISpaceManager#addComponent(ComponentDescriptor, Component)}. The default is {@code true}, which will add
     * a toolbar button to the {@link TabType#PLUGINS} tab.
     * <p>
     * Use with caution, because if the button is <i>not</i> added and the component descriptor is closeable, the user
     * may close the component and have no way to reopen it.
     *
     * @param buttonVisible True if a toolbar button should be visible
     * @return Builder
     * @see #setCloseable(boolean)
     * @since 3.4 (renamed from setRibbonBarButtonVisible)
     */
    @Nonnull
    public ComponentDescriptorBuilder setToolbarButtonVisible(boolean buttonVisible)
    {
        this.buttonVisible = buttonVisible;
        return this;
    }

    /**
     * Defines if this component's parent dockable component will contain scrollbars. You may want to turn on scrolling
     * if your component has parts that are being cut off. The default is {@code false}.
     *
     * @param parentScrollingEnabled Whether scrolling will be enabled in the parent component
     * @return Builder
     * @since 1.43 (renamed in 3.4)
     */
    @Nonnull
    public ComponentDescriptorBuilder setParentScrollingEnabled(boolean parentScrollingEnabled)
    {
        this.parentScrollingEnabled = parentScrollingEnabled;
        return this;
    }

    /**
     * If this component should allow the user to close it. The default is {@code true}.
     *
     * @param closeable true if the user can close it
     * @return Builder
     * @since 2.13
     */
    @Nonnull
    public ComponentDescriptorBuilder setCloseable(boolean closeable)
    {
        this.closeable = closeable;
        return this;
    }

    /**
     * Set this value only if you want the initial location of the component to be externalized. The default is
     * {@code null}, meaning the component will not be externalized.
     *
     * @param externalizedRect the location to place the component initially
     * @return Builder
     * @throws IllegalArgumentException if the externalized rectangle is empty
     * @since 2.13
     */
    @Nonnull
    public ComponentDescriptorBuilder setExternalizedRect(@Nonnull Rectangle externalizedRect) throws IllegalArgumentException
    {
        if (externalizedRect.isEmpty())
        {
            throw new IllegalArgumentException("The externalizedRect must not be empty; received: " + externalizedRect);
        }

        this.externalizedRect = externalizedRect;
        return this;
    }

    /**
     * Defines if the component should be docked next to the globe and stay open when another dockable component gains
     * focus or if the component should float above all other dockable components and collapse when another dockable
     * gains focus. The default is {@code false}.
     *
     * @param pinned {@code true} if the component should be docked next to the globe and stay open when another
     *               dockable component gains focus (may be {@code null} to use the default application setting)
     * @return Builder
     * @since 2.23
     */
    @Nonnull
    public ComponentDescriptorBuilder setPinned(@Nullable Boolean pinned)
    {
        this.pinned = pinned;
        return this;
    }

    /**
     * Defines if the component should show a button to allow users to change the pinned state. The default is
     * {@code false}, meaning the pin button is shown to users.
     *
     * @param hidePin Specifies if the pin control should be hidden to prevent users from changing the pinned state of a
     *                component
     * @return Builder
     * @since 3.0
     */
    @Nonnull
    public ComponentDescriptorBuilder setHidePin(boolean hidePin)
    {
        this.hidePin = hidePin;
        return this;
    }

    /**
     * Set whether the component is "playback aware" meaning that it displays or is affected by the playback state,
     * i.e., "live" or not. When set {@code true}, the component will automatically have a notification border added
     * to it so that the user will be visually informed that this component is affected by playback state.
     *
     * @param playbackAware Pass {@code true} to have the system add a playback state visual indicator to the component
     * @return Builder
     * @since 2.25
     */
    @Nonnull
    public ComponentDescriptorBuilder setPlaybackAware(boolean playbackAware)
    {
        this.playbackAware = playbackAware;
        return this;
    }

    /**
     * Set whether the component should have the ability to undock from the docking framework. This controls if the icon
     * for undocking will show on the component. By default, this is set to true.
     *
     * @param externalizable Pass {@code false} to disable the externalizable behavior.
     * @return Builder
     * @since 3.3.0
     */
    @Nonnull
    public ComponentDescriptorBuilder setExternalizable(boolean externalizable)
    {
        this.externalizable = externalizable;
        return this;
    }

    /**
     * Validates the inputs and builds a {@code ComponentDescriptor}.
     *
     * @return ComponentDescriptor for use by the {@link IUISpaceManager}.
     */
    @Nonnull
    public ComponentDescriptor build()
    {
        return new ComponentDescriptor(id, componentPosition, name, tooltipText, iconUrl, buttonVisible,
                parentScrollingEnabled, pinned, closeable, externalizedRect, playbackAware, hidePin,
                externalizable);
    }
}
