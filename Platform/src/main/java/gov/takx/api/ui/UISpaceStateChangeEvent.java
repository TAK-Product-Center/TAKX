package gov.takx.api.ui;

import gov.takx.platform.ui.ComponentDescriptor;
import jakarta.annotation.Nonnull;

import java.awt.Component;

/**
 * The state of the docking frame has changed. Meaning that it has been popped into view or un-docked or re-docked etc.<br>
 * <br>
 * See {@link DockingState} For a description of each of the states.
 *
 */
public class UISpaceStateChangeEvent extends UISpaceEvent
{
    private final DockingState state;

    /**
     * The current state of the dockable frame<br>
     * <ul>
     * <li><u>FlapButton</u>: The panel exists as a button on the side or the bottom. If the button has
     * been clicked then the panel will slide out over the adjacent content. If the panel is visible, then
     * the visible event will also be sent.</li>
     * <li><u>Docked</u>: The panel has been docked into the center area of the main TAKX window either by
     * dragging in from the flap button state or by clicking on the maximize button.</li>
     * <li><u>MaximalDocked</u>: The panel has been maximized while being docked.</li>
     * <li><u>Externalized</u>: The panel has been un-docked and is now floating.</li>
     * <li><u>StackTab</u>: The panel has been docked as a tab in the center pane.</li>
     * <li><u>MaximalExternalized</u>: The panel has been maximized while being un-docked.</li>
     * <li><u>Closed</u>: The docking frame was closed.</li>
     * </ul>
     */
    public enum DockingState
    {
        FlapButton,
        Docked,
        MaximalDocked,
        Externalized,
        StackTab,
        MaximalExternalized,
        Closed
    }

    /**
     * The docked panel viewable state has changed.
     *
     * @param componentDescriptor Descriptor of the component involved
     * @param component The affected component
     * @param state the current state of the panel
     * @since 3.4 (updated args)
     */
    public UISpaceStateChangeEvent(@Nonnull ComponentDescriptor componentDescriptor, @Nonnull Component component, DockingState state)
    {
        super(componentDescriptor, component);
        this.state = state;
    }

    /**
     * @return current state of the panel
     */
    public DockingState getState()
    {
        return state;
    }

    /**
     * @since 3.4
     */
    @Override
    public String toString()
    {
        return "UISpaceStateChangeEvent{" +
                "state=" + state +
                ", id=" + getId() +
                ", title=" + getTitle() +
                '}';
    }
}
