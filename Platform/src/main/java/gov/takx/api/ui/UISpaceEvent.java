package gov.takx.api.ui;

import gov.takx.api.event.PlatformEvent;
import gov.takx.platform.ui.ComponentDescriptor;
import jakarta.annotation.Nonnull;

import java.awt.Component;

/**
 * Abstract base for all UI Space events, relating to dockable components.
 */
public abstract class UISpaceEvent implements PlatformEvent
{
    private final ComponentDescriptor componentDescriptor;
    private final Component component;

    /**
     * @param componentDescriptor Descriptor of the component involved
     * @param component The affected component
     * @since 3.4 (replaced discrete args with componentDescriptor)
     */
    protected UISpaceEvent(@Nonnull ComponentDescriptor componentDescriptor, @Nonnull Component component)
    {
        this.componentDescriptor = componentDescriptor;
        this.component = component;
    }

    /**
     * @return The descriptor providing the properties of the component
     * @since 3.4
     */
    @Nonnull
    public ComponentDescriptor getComponentDescriptor()
    {
        return componentDescriptor;
    }

    /**
     * @return The component involved in the change event.
     * @since 3.4
     */
    public Component getComponent()
    {
        return component;
    }

    /**
     * @return panel name/title
     */
    public String getTitle()
    {
        return componentDescriptor.getName();
    }

    /**
     * @return the id of the component
     */
    public String getId()
    {
        return componentDescriptor.getId();
    }
}
