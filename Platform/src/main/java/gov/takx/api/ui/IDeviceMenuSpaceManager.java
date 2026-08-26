package gov.takx.api.ui;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Action;

import java.awt.Component;
import java.util.List;

/**
 * Contract for the menu space manager handling a device Map Object.
 *
 * @since 3.0
 */
public interface IDeviceMenuSpaceManager extends IMenuSpaceManager
{
    /**
     * @return The ordered list of menu entries to include in the context menu
     */
    @Nonnull
    List<ContextMenuEnum> getMenuOrder();

    /**
     * Get the text replacement for a given menu item.
     *
     * @param menuEntry Menu item of interest
     * @return Text replacement, or {@code null} if not replaced
     * @since 3.0 (was in concrete class in 2.37)
     */
    @Nullable
    String getTextReplacement(@Nonnull ContextMenuEnum menuEntry);

    /**
     * Get the redirected action for a given menu item.
     *
     * @param menuEntry Menu item of interest
     * @return Redirected action, or {@code null} if not redirected
     * @since 3.0 (was in concrete class in 2.37)
     */
    @Nullable
    Action getRedirectedAction(@Nonnull ContextMenuEnum menuEntry);

    /**
     * @return The list of additional menu items added by a plugin, may be empty
     */
    @Nonnull
    List<IIndexedComponent> getAdditionalMenuItems();

    /**
     * Contract for a tuple of a Component and an associated index (position in the menu).
     */
    interface IIndexedComponent
    {
        /**
         * @return The component
         */
        @Nonnull
        Component getComponent();

        /**
         * @return The index for the component, may be -1 to mean the end
         */
        int getIndex();
    }
}
