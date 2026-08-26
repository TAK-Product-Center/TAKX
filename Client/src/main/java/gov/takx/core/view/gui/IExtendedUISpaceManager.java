package gov.takx.core.view.gui;

import gov.takx.api.ui.IUISpaceManager;
import jakarta.annotation.Nonnull;

/**
 * Extends the UI space manager for use with {@link CoreComponentType} panels within the core.
 *
 */
public interface IExtendedUISpaceManager extends IUISpaceManager
{
    /**
     * Show the dockable component in its "most natural" state. If it is docked, show it docked. If it is
     * un-docked, move it to the front. If it is closed then dock it and show it.
     *
     * @param coreComponentType the core component type to be shown
     * @since 2.13 (parameter changed in 3.4)
     */
    void showComponent(@Nonnull CoreComponentType coreComponentType);

    /**
     * Hide the dockable component. Basically just re-dock it.
     *
     * @param coreComponentType the core component type to be hidden
     * @since 2.13 (parameter changed in 3.4)
     */
    void hideComponent(@Nonnull CoreComponentType coreComponentType);

    /**
     * Toggle a component's visibility. In the case of a CENTER-positioned panel, brings it to the front.
     * <p>
     * Note that in many cases a component cannot be toggled. For example if the user has un-docked it.
     *
     * @param coreComponentType the core component type to be toggled
     * @since 2.13 (parameter changed in 3.4)
     */
    void toggleComponent(@Nonnull CoreComponentType coreComponentType);
}
