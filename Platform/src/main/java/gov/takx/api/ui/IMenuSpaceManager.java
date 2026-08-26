package gov.takx.api.ui;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.app.IAppPluginDelegate;
import gov.takx.api.plugin.device.IDevicePluginDelegate;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Action;

import java.awt.Component;
import java.util.List;
import java.util.Objects;

/**
 * The Menu Space Manager is responsible for allocating space among the menu bar and menu items on the TAKX frame.
 * <p>
 * Note: this class is not available for direct injection via CDI. It can be accessed via either an app delegate (using
 * {@link IAppPluginDelegate#getTerrainMenuSpaceManager()}) or a device delegate (using
 * {@link IDevicePluginDelegate#getMenuSpaceManager()}). Because {@link IMapObjectController}
 * extends {@link IDevicePluginDelegate}, the menu space manager can easily be accessed for any map object via its
 * controller.
 * <p>
 * When retrieved from an app delegate, only the subset of functionality defined in {@link ITerrainMenuSpaceManager} is
 * supported. {@link ITerrainMenuSpaceManager} can be directly injected to get access to this functionality.
 *
 */
@SuppressWarnings("UnusedDeclaration") // Used by plugins
@NoPluginImplementations
public interface IMenuSpaceManager extends ITerrainMenuSpaceManager
{
    // Enum of possible context menu choices (order is important, so don't sort this).
    enum ContextMenuEnum
    {
        // TODO KMB: These icons should be cleaned up too for consistency and understanding
        OPEN("Open", false, kernelAssetIcon("open_file.png")),
        CONFIGURE("Configure", false, kernelAssetIcon("settings.png")),
        HELP("Help", false, kernelAssetIcon("help.png")),
        DEVICE_LOG("Device Log", false, kernelAssetIcon("generic_doc.png")),
        UNDO("Undo Last Move", true, kernelAssetIcon("undo.png")),
        GOTO("Go To", true, kernelAssetIcon("go_to.png")),
        CENTER_ON("Center On", false, kernelAssetIcon("lock.png")),
        KEEP_IN_VIEW("Keep In View", false, kernelAssetIcon("3d_free_look.png")),
        SEND_AS_COT("Send as CoT", false, kernelAssetIcon("send.png")),
        INSPECT_COT("Inspect CoT", false, platformIcon("inspect_cot.png")),
        DEACTIVATE("Deactivate", false, kernelAssetIcon("deactivate.png")),
        DELETE("Delete", false, kernelAssetIcon("delete.png")),
        HIDE("Hide", false, kernelAssetIcon("hide.png")),
        SHOW("Show", false, kernelAssetIcon("show.png")),
        CLEAR_ALARMS("Clear Alarms", false, kernelAssetIcon("clear_alarm.png")),
        REMOVE_FROM("Remove From", false),
        ORIENTABLE("Orientable", false),
        EDITABLE("Editable", false),
        /** @since 5.5 **/
        TOGGLE_RANGE_RINGS("Range Rings", false, platformIcon("circle.png")),
        /** @since 5.5 **/
        CHANGE_DIRECTION("Change Direction", false, takAssetIcon("To_From.png")),
        /** @since 5.5 **/
        CHANGE_DEGREES("Change Degrees", false),

        /** @since 5.5 **/
        MILS_TRUE_NORTH("Mils True", false),
        /** @since 5.5 **/
        DEG_TRUE_NORTH("Deg True", false),
        /** @since 5.5 **/
        MILS_MAG_NORTH("Mils Mag", false),
        /** @since 5.5 **/
        DEG_MAG_NORTH("Deg Mag", false),
        /** @since 5.5 **/
        MILS_GRID_NORTH("Mils Grid", false),
        /** @since 5.5 **/
        DEG_GRID_NORTH("Deg Grid", false),

        /**
         * Play the associated video file.
         *
         * @since 5.3
         */
        VIDEO("Play video", false, platformIcon("video.png")),

        /**
         * Creates a copy of a dynamic measure.
         *
         * @since 5.0
         */
        PIN("Pin", false, platformIcon("marker.png")),

        /** @since 5.0 */
        LOCK_DISTANCE("Lock Distance", false, platformIcon("lock-distance.png")),

        /**
         * Draws an arrow from the map object to the point clicked.
         *
         * @since 5.1
         */
        MEASURE_TO("Measure To", false, takAssetIcon("Range-And-Bearing_32x32.png")),

        /** @since 5.1.0 */
        FINE_ADJUST("Fine Adjust", false, platformIcon("move.png")),

        /** @since 5.3.0 */
        GEOFENCE("GeoFence", false, takAssetIcon("Geo-Fences_32x32.png")),

        /** @since 5.3 */
        BREADCRUMBS("Breadcrumbs", false, takAssetIcon("Breadcrumbs_32x32.png")),

        /** @since 5.6 */
        DRIFTER(Objects.requireNonNull(ButtonType.DRIFTER.getDisplayText()), false, ButtonType.DRIFTER.getImageUrl().toString());

        /**
         * @return String form of a URL referring to the given named file in {@link ResourceDirectory#PLATFORM_ICONS}
         * @since 5.7
         */
        private static String platformIcon(@Nonnull String name)
        {
            return UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, name).toString();
        }

        /**
         * @return String form of a URL referring to the given named file takkernel assets
         * @since 5.7
         */
        private static String kernelAssetIcon(@Nonnull String name)
        {
            return "asset://icons/" + name;
        }

        /**
         * @return String form of a URL referring to the given named file in {@link ResourceDirectory#TAK_ASSETS_PNG}
         * @since 5.7
         */
        private static String takAssetIcon(@Nonnull String name)
        {
            return UIResources.getUrl(ResourceDirectory.TAK_ASSETS_PNG, name).toString();
        }

        private final String name;
        private final boolean separatorBefore;
        private final String iconName;

        ContextMenuEnum(@Nonnull String name, boolean separatorBefore)
        {
            this(name, separatorBefore, null);
        }

        ContextMenuEnum(@Nonnull String name, boolean separatorBefore, @Nullable String iconName)
        {
            this.iconName = iconName;
            this.name = name;
            this.separatorBefore = separatorBefore;
        }

        /**
         * @return {@code true} if a separator should be added to the context menu <em>before</em> this menu entry
         * @since 2.37
         */
        public boolean shouldAddSeparatorBefore()
        {
            return separatorBefore;
        }

        @Nonnull
        @Override
        public String toString()
        {
            return name;
        }

        @Nullable
        public String getIconName()
        {
            return iconName;
        }
    }

    /**
     * Redirects the popup menu event handler to a custom event handler.  Especially useful for providing
     * either a custom dialog or custom processing of the device log action.
     * <p/>
     * Passing in a {@code null} action restores the original device log processing.
     *
     * @param menuEntry enum that indicates which popup menu is being overridden.  Presently, not supported for
     *                  {@link ContextMenuEnum#CENTER_ON CENTER_ON} or {@link ContextMenuEnum#KEEP_IN_VIEW KEEP_IN_VIEW},
     * @param action    Action responsible for acting on the event through the popup menu
     * @throws IllegalStateException    when invoked from within the IAppPlugin context
     * @throws IllegalArgumentException If {@code menuEntry} is {@link ContextMenuEnum#CENTER_ON CENTER_ON} or
     *                                  {@link ContextMenuEnum#KEEP_IN_VIEW KEEP_IN_VIEW},
     * @since 2.0
     */
    void redirectPopupMenuAction(@Nonnull ContextMenuEnum menuEntry, @Nullable Action action);

    /**
     * Sets the visibility of a popup menu.
     *
     * @param menuEntry enum that indicates which popup menu is having the visibility set.  Available for all menu items.
     * @param visible   Popup menu visibility
     * @throws IllegalStateException when invoked from within the IAppPlugin context
     * @since 2.0
     */
    void setPopupMenuVisible(@Nonnull ContextMenuEnum menuEntry, boolean visible);

    /**
     * Sets the text for a specific popup menu.
     *
     * @param menuEntry enum that indicates which popup menu is being overridden.  Presently, not supported for
     *                  {@link ContextMenuEnum#CENTER_ON CENTER_ON} or {@link ContextMenuEnum#KEEP_IN_VIEW KEEP_IN_VIEW},
     * @param text      text to override the default with
     * @throws IllegalStateException    when invoked from within the IAppPlugin context
     * @throws IllegalArgumentException If {@code menuEntry} is {@link ContextMenuEnum#CENTER_ON CENTER_ON} or
     *                                  {@link ContextMenuEnum#KEEP_IN_VIEW KEEP_IN_VIEW},
     * @since 2.0
     */
    void setPopupMenuText(@Nonnull ContextMenuEnum menuEntry, @Nullable String text);

    /**
     * Completely customizes which menu items to use and in what order.
     *
     * @param menuEntries the list of menu items to use instead of the default.
     * @throws IllegalStateException when invoked from within the IAppPlugin context
     * @since 2.0
     */
    void setMenuOrder(@Nonnull List<ContextMenuEnum> menuEntries);

    /**
     * Adds a new popup menu item to the map object device popup menu.  Note, to add a separator, use the call
     * {@link #addDevicePopupMenuSeparator()} or {@link #addDevicePopupMenuSeparator(int)}, or just directly
     * pass in a JSeparator component instance.
     *
     * @param component Component to add
     * @throws IllegalStateException when invoked from within the IAppPlugin context
     */
    void addDevicePopupMenuComponent(@Nonnull Component component);

    /**
     * Adds a new popup menu item to the map object device popup menu.  Note, to add a separator, use the call
     * {@link #addDevicePopupMenuSeparator()} or {@link #addDevicePopupMenuSeparator(int)}, or just directly
     * pass in a JSeparator component instance.
     * This method allows the popup menu item to be inserted at any location, -1 specifies the end.
     * <p>
     * Note, if you want a specific order at the end of the menu, just specify ordered arbitrarily large index numbers.
     *
     * @param component Component to add
     * @param index     index in which to insert the menu
     * @throws IllegalStateException when invoked from within the IAppPlugin context
     * @since 2.0
     */
    void addDevicePopupMenuComponent(@Nonnull Component component, int index);

    /**
     * Add a separator to the device popup menu.
     *
     * @since 2.37
     */
    void addDevicePopupMenuSeparator();

    /**
     * Add a separator to the device popup menu at the given index.
     *
     * @param index index in which to insert the separator, -1 to add at the end
     * @since 2.37
     */
    void addDevicePopupMenuSeparator(int index);

    /**
     * Removes the given component from the map object device popup menu.
     *
     * @param component Menu to remove
     * @throws IllegalStateException when invoked from within the IAppPlugin context
     */
    void removeDevicePopupMenuComponent(@Nonnull Component component);
}
