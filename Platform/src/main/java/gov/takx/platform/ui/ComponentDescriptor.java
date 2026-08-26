package gov.takx.platform.ui;

import gov.takx.api.toolbar.TabType;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.IUISpaceManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Component;
import java.awt.Rectangle;
import java.net.URL;

/**
 * Collection of metadata used to define a new UI component.
 *
 * @see ComponentDescriptorBuilder
 * @since 2.15
 */
public class ComponentDescriptor
{
    private final String id;
    private final String name;
    private final String tooltipText;
    private final URL iconUrl;
    private final boolean buttonVisible;
    private final ComponentPosition componentPosition;
    private final boolean parentScrollingEnabled;
    private final boolean closeable;
    private final Boolean pinned;
    private final Boolean hidePin;
    private final Rectangle externalizedRect;
    private final boolean playbackAware;
    private final boolean externalizable;

    /**
     * @param id The id of the component
     * @param componentPosition The component's starting position on the main app frame
     * @param name The name and display title for the component
     * @param tooltipText The tooltip text to display for the component
     * @param iconUrl The URL for the component icon
     * @param buttonVisible Whether an associated toolbar button should be visible
     * @param parentScrollingEnabled Whether scrolling should be enabled for the parent of the component
     * @param pinned Whether the component should be pinned by default
     * @param closeable Whether the component should be closeable by the user via a close button
     * @param externalizedRect The starting dimensions and locations of the component if it is externalized (i.e. popped out)
     * @param playbackAware Whether the component should be highlighted with a green border during playback
     * @param hidePin Whether the pin button should be hidden from the user
     * @param externalizable Whether the externalizable button should be hidden from the user
     * @since 3.0
     */
    ComponentDescriptor(@Nonnull String id, @Nonnull ComponentPosition componentPosition, @Nonnull String name,
                        @Nullable String tooltipText, @Nonnull URL iconUrl, boolean buttonVisible,
                        boolean parentScrollingEnabled, @Nullable Boolean pinned, boolean closeable,
                        @Nullable Rectangle externalizedRect, boolean playbackAware, boolean hidePin,
                        boolean externalizable)
    {
        // Not enforcing nullability here since it is checked by the builder.
        this.id = id;
        this.name = name;
        this.tooltipText = tooltipText;
        this.iconUrl = iconUrl;
        this.componentPosition = componentPosition;
        this.buttonVisible = buttonVisible;
        this.parentScrollingEnabled = parentScrollingEnabled;
        this.pinned = pinned;
        this.closeable = closeable;
        this.externalizedRect = externalizedRect;
        this.playbackAware = playbackAware;
        this.hidePin = hidePin;
        this.externalizable = externalizable;
    }

    /**
     * @return The id of the component.
     */
    @Nonnull
    public String getId()
    {
        return id;
    }

    /**
     * @return The name and display title for the component.
     */
    @Nonnull
    public String getName()
    {
        return name;
    }

    /**
     * @return The tooltip text to display for the component.
     */
    @Nullable
    public String getTooltipText()
    {
        return tooltipText;
    }

    /**
     * @return The URL for the component icon.
     */
    @Nonnull
    public URL getIconUrl()
    {
        return iconUrl;
    }

    /**
     * @return The component's starting position on the main app frame.
     */
    @Nonnull
    public ComponentPosition getComponentPosition()
    {
        return componentPosition;
    }

    /**
     * @return {@code true} if a toolbar button should be added to the {@link TabType#PLUGINS} tab of the toolbar to
     * toggle the button. If {@code true}, the button that is added to the toolbar will be returned by
     * {@link IUISpaceManager#addComponent(ComponentDescriptor, Component)}.
     * @since 3.4 (renamed from isRibbonBarButtonVisible)
     */
    public boolean isToolbarButtonVisible()
    {
        return buttonVisible;
    }

    /**
     * @return {@code true} if the component should be closeable by the user via a close button.
     * @since 3.4 (renamed from isClosable)
     */
    public boolean isCloseable()
    {
        return closeable;
    }

    /**
     * @return {@code true} if the pin button is hidden from the user.
     * @since 3.0 (renamed in 3.4)
     */
    public boolean isHidePin()
    {
        return hidePin;
    }

    /**
     * @return {@code true} if the component will be docked next to the globe and stay open when another dockable
     * component gains focus, {@code false} if the component should not be docked next to the globe and stay open after
     * losing focus, or {@code null} if the default pin behavior should be used.
     * @since 2.23
     */
    @Nullable
    public Boolean isPinned()
    {
        return pinned;
    }

    /**
     * @return The starting dimensions and locations of the component if it is externalized (i.e. popped out).
     */
    @Nullable
    public Rectangle getExternalizedRect()
    {
        return externalizedRect;
    }

    /**
     * @return {@code true} if scrolling is enabled for the parent of the component.
     * @see ComponentDescriptorBuilder#setParentScrollingEnabled(boolean)
     * @since 1.43 (renamed in 3.4)
     */
    public boolean isParentScrollingEnabled()
    {
        return parentScrollingEnabled;
    }

    /**
     * Determines if the component is "playback aware" meaning that it displays or is affected by the playback state,
     * i.e., "live" or not. When {@code true}, the component will automatically have a notification border added
     * to it so that the user will be visually informed that this component is affected by playback state.
     *
     * @return {@code true} if the component is playback aware and will have a border added automatically during playback.
     * @see ComponentDescriptorBuilder#setPlaybackAware(boolean)
     * @since 2.25
     */
    public boolean isPlaybackAware()
    {
        return playbackAware;
    }

    /**
     * @return {@code true} if the component is externalizable, i.e. can be undocked from the docking framework. If
     * {@code false}, the externalizable button will be hidden from the user.
     * @since 3.3.0
     */
    public boolean isExternalizable()
    {
        return externalizable;
    }

    @Override
    public String toString()
    {
        return "ComponentDescriptor{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", tooltipText='" + tooltipText + '\'' +
                ", iconUrl=" + iconUrl +
                ", buttonVisible=" + buttonVisible +
                ", componentPosition=" + componentPosition +
                ", parentScrollingEnabled=" + parentScrollingEnabled +
                ", closeable=" + closeable +
                ", pinned=" + pinned +
                ", hidePin=" + hidePin +
                ", externalizedRect=" + externalizedRect +
                ", playbackAware=" + playbackAware +
                ", externalizable=" + externalizable +
                '}';
    }
}
