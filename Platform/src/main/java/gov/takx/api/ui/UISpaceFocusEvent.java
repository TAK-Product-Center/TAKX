package gov.takx.api.ui;

import gov.takx.platform.ui.ComponentDescriptor;
import jakarta.annotation.Nonnull;

import java.awt.Component;

/**
 * The docked frame has just gained or lost focus.
 *
 */
public class UISpaceFocusEvent extends UISpaceEvent
{
    private final boolean focus;

    /**
     * The docked panel viewable state has changed.
     *
     * @param componentDescriptor Descriptor of the component involved
     * @param component The affected component
     * @param focus true if the panel has focus
     * @since 3.4 (updated args)
     */
    public UISpaceFocusEvent(@Nonnull ComponentDescriptor componentDescriptor, @Nonnull Component component, boolean focus)
    {
        super(componentDescriptor, component);
        this.focus = focus;
    }

    /**
     * @return true if the panel has focus
     */
    public boolean hasFocus()
    {
        return focus;
    }
}
