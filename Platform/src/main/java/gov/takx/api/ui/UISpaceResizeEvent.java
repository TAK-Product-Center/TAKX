package gov.takx.api.ui;

import gov.takx.platform.ui.ComponentDescriptor;
import jakarta.annotation.Nonnull;

import java.awt.Component;
import java.awt.Rectangle;

/**
 * The docking frame was resized. Either as an externalized frame or docked in the center area or as a flap panel.
 *
 */
public class UISpaceResizeEvent extends UISpaceEvent
{
    private final Rectangle size;

    /**
     * The docked panel viewable state has changed.
     *
     * @param componentDescriptor Descriptor of the component involved
     * @param component The affected component
     * @param size the size of the rectangle in its current state
     * @since 3.4 (updated args)
     */
    public UISpaceResizeEvent(@Nonnull ComponentDescriptor componentDescriptor, @Nonnull Component component, @Nonnull Rectangle size)
    {
        super(componentDescriptor, component);
        this.size = size;
    }

    /**
     * Current size of the panel.
     * <p>
     * Note: if the panel is currently docked then only the width and height of the
     * rectangle are relevant. In this case the x and y values will be -1.
     *
     * @return current size
     */
    public Rectangle getSize()
    {
        return new Rectangle(size); // Rectangles are mutable, so return a copy
    }
}
