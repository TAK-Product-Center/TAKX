package gov.takx.core.explorer.common;

import gov.takx.api.explorer.ExplorerUtils;
import gov.takx.api.explorer.IExplorable;
import gov.takx.commons.constants.State;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Utility methods for core explorer classes.
 *
 * @since 1.48
 */
public class CoreExplorerUtils extends ExplorerUtils
{
    private static final Icon INACTIVE_ICON;
    private static final Icon ACTIVE_ICON;
    private static final Icon WARNING_ICON;
    private static final Icon CRITICAL_ICON;
    private static final Map<State, Icon> STATE_TO_ICON_MAP;
    public static final int ICON_SIZE = PreferenceEntry.EXPLORER_ICON_SIZE.getIntegerValue();
    public static final int ICON_COLUMN_SIZE = (int) (ICON_SIZE * 1.5);

    private static final IdentityHashMap<BufferedImage, Icon> iconMap = new IdentityHashMap<>();

    static
    {
        INACTIVE_ICON = new ImageIcon(GraphicsUtils.createGlossyCircle(ICON_SIZE, Color.GRAY));
        ACTIVE_ICON = new ImageIcon(GraphicsUtils.createGlossyCircle(ICON_SIZE, Color.GREEN));
        WARNING_ICON = new ImageIcon(GraphicsUtils.createGlossyTriangle(ICON_SIZE, Color.YELLOW));
        CRITICAL_ICON = new ImageIcon(GraphicsUtils.createGlossyOctagon(ICON_SIZE, Color.RED));

        STATE_TO_ICON_MAP = Map.of(
                State.Inactive, INACTIVE_ICON,
                State.Active, ACTIVE_ICON,
                State.Warning, WARNING_ICON,
                State.Serious, WARNING_ICON,
                State.Critical, CRITICAL_ICON);
    }

    /**
     * Get the explorer icon associated with an alarm state.
     *
     * @param state alarm state of interest.
     * @return icon for the alarm state.
     */
    @Nullable
    public static Icon getIconForState(@Nullable State state)
    {
        return state == null || state == State.Unknown ? null : STATE_TO_ICON_MAP.get(state);
    }

    /**
     * Create a sized icon from an image.
     *
     * @param iconImage image to convert into an icon.
     * @param size the max image size
     * @return properly-sized icon from the image.
     * @since 5.6
     */
    public static Icon iconForImage(@Nullable BufferedImage iconImage, int size)
    {
        if (iconImage == null) return null;
        synchronized (iconMap)
        {
            return iconMap.computeIfAbsent(iconImage, icon -> new ImageIcon(ImageUtils.getScaledInstanceRespectingAspectRatioWithPadding(icon, size, size)));
        }
    }

    /**
     * Convenience method for creating an icon for an explorable.
     *
     * @param explorable explorable of interest.
     * @return icon for the explorable, or {@code null} if the explorable does not provide an icon image.
     */
    public static Icon iconForExplorable(@Nonnull IExplorable explorable)
    {
        return iconForExplorable(explorable, ICON_SIZE);
    }

    /**
     * Convenience method for creating an icon for an explorable.
     *
     * @param explorable explorable of interest
     * @param size the size of the returned image
     * @return icon for the explorable, or {@code null} if the explorable does not provide an icon image
     * @since 5.6
     */
    public static Icon iconForExplorable(@Nonnull IExplorable explorable, int size)
    {
        return iconForImage(explorable.getIconImage(), size);
    }
}
