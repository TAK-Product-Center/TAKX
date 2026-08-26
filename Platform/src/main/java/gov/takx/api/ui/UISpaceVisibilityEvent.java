package gov.takx.api.ui;

import gov.takx.platform.ui.ComponentDescriptor;
import jakarta.annotation.Nonnull;

import java.awt.Component;

/**
 * The docked frame has just become visible or not visible.
 *
 */
public class UISpaceVisibilityEvent extends UISpaceEvent
{
    private final boolean visible;

    /**
     * The docked panel visibility has changed.
     *
     * @param componentDescriptor Descriptor of the component involved
     * @param component The affected component
     * @param visible true if the panel is visible
     * @since 3.4 (updated args)
     */
    public UISpaceVisibilityEvent(@Nonnull ComponentDescriptor componentDescriptor, @Nonnull Component component, boolean visible)
    {
        super(componentDescriptor, component);
        this.visible = visible;
    }

    /**
     * @return true if the panel is visible to the user
     * @since 3.4 (renamed from isShown)
     */
    public boolean isVisible()
    {
        return visible;
    }
}
