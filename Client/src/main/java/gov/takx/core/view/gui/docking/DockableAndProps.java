package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.common.location.CExternalizedLocation;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.platform.ui.ComponentDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Objects;

/**
 * Wrapper class to associate a dockable with its properties.
 *
 * @since 2.12 (combined with DockableProps in 3.4)
 */
public class DockableAndProps
{
    private final ComponentDescriptor descriptor;
    private Component component;
    private ComponentDockable dockable;
    private CExternalizedLocation externalizedLocation = null;
    private Dimension minimizedSize;

    /**
     * @param component the component to be docked
     * @param descriptor descriptor for the component
     * @param dockable the dockable containing the component
     */
    public DockableAndProps(@Nonnull Component component, @Nonnull ComponentDescriptor descriptor,
                            @Nullable ComponentDockable dockable)
    {
        this.component = component;
        this.descriptor = descriptor;
        this.dockable = dockable;

        final Rectangle externalizedRect = descriptor.getExternalizedRect();
        if (externalizedRect != null) setExternalizedLocation(externalizedRect);

        minimizedSize = component.getPreferredSize();
    }

    /**
     * Updates the externalized location from the provided rectangle.
     *
     * @param rectangle The dimension and location of the component if it is externalized (i.e. popped out)
     * @since 3.4
     */
    public void setExternalizedLocation(@Nonnull Rectangle rectangle)
    {
        externalizedLocation = rectangle.isEmpty() ? null : new CExternalizedLocation(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    /**
     * Sets the location for the dockable for when it is externalized.
     *
     * @param externalizedLocation Externalized location for a dockable
     * @since 3.4 (moved from DockableProps)
     */
    public void setExternalizedLocation(@Nonnull CExternalizedLocation externalizedLocation)
    {
        this.externalizedLocation = externalizedLocation;
    }

    /**
     * @return an externalized location created from the externalized rectangle (may be {@code null} if the rectangle
     * was {@code null} or empty)
     * @since 3.4 (moved from DockableProps)
     */
    @Nullable
    public CExternalizedLocation getExternalizedLocation()
    {
        return externalizedLocation;
    }

    /**
     * @return The component being docked.
     * @since 3.4
     */
    @Nonnull
    public Component getComponent()
    {
        return component;
    }

    /**
     * Updates the component associated with the dockable.
     * 
     * @param component the component to be docked
     * @since 3.4
     */
    public void setComponent(@Nonnull Component component)
    {
        this.component = component;
    }

    /**
     * @return The descriptor providing the properties of this dockable component
     * @since 3.4 (moved from DockableProps)
     */
    @Nonnull
    public ComponentDescriptor getDescriptor()
    {
        return descriptor;
    }

    /**
     * @return the dockable component, which may be {@code null} if the dockable was closed.
     * @since 3.4
     */
    @Nullable
    public ComponentDockable getDockable()
    {
        return dockable;
    }

    /**
     * @return True if a non-null dockable is present.
     * @since 3.4
     */
    public boolean hasDockable()
    {
        return dockable != null;
    }

    /**
     * @return the dockable component.
     * @throws NullPointerException if the dockable is {@code null}
     * @since 3.4
     */
    @Nonnull
    public ComponentDockable requireDockable()
    {
        return Objects.requireNonNull(dockable, "dockable is null");
    }

    /**
     * Sets the dockable that contains the component in the UI.
     *
     * @param dockable the dockable component, which may be {@code null} if the dockable was closed
     * @since 3.4
     */
    public void setDockable(@Nullable ComponentDockable dockable)
    {
        this.dockable = dockable;
    }

    /**
     * @return The id of the component.
     * @since 3.4 (moved from DockableProps)
     */
    @Nonnull
    public String getId()
    {
        return descriptor.getId();
    }

    /**
     * @return The component's starting position on the main app frame.
     * @since 3.4
     */
    @Nonnull
    public ComponentPosition getComponentPosition()
    {
        return descriptor.getComponentPosition();
    }

    /**
     * @return The name of the component.
     * @since 3.4 (moved from DockableProps)
     */
    public String getName()
    {
        return descriptor.getName();
    }

    /**
     * @return the size of the unpinned component when it is expanded from the minimized dock.
     * @since 3.4
     */
    public Dimension getMinimizedSize()
    {
        return minimizedSize;
    }

    /**
     * Sets a new size for the unpinned component when it is expanded from the minimized dock.
     *
     * @param minimizedSize the new size
     * @since 3.4
     */
    public void setMinimizedSize(Dimension minimizedSize)
    {
        this.minimizedSize = minimizedSize;
    }

    @Override
    public String toString()
    {
        return "DockableAndProps{" +
                "descriptor=" + descriptor +
                ", component=" + component +
                ", dockable=" + dockable +
                ", externalizedLocation=" + externalizedLocation +
                ", minimizedSize=" + minimizedSize +
                '}';
    }
}
