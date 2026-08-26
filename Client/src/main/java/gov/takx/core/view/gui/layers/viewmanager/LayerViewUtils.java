package gov.takx.core.view.gui.layers.viewmanager;

import gov.takx.api.layer.LayerGroup;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.platform.ui.UIResources;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Constants related to the display of layers in the layer view panel.
 */
public class LayerViewUtils
{
    public static final String FAVORITES = "Favorite Layers";
    public static final String SEARCH_ENABLED = "<html><font color=red>Layer Search Filter active</font></html>";
    public static final String SEARCH_DISABLED = "<html><font color=#646464>Layer Search Filter not active</font></html>";
    public static final String EXTRA_TOOLS = "Extra Tools";
    public static final String WIDGETS = "Map Widgets";
    public static final ImageIcon IN_VIEW_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "visibility.png");

    public static final List<LayerGroup> EXTRAS_TOOLS_GROUP = List.of(LayerGroup.TOOLS, LayerGroup.FEATURES);
    public static final Set<String> EXTRAS_TOOLS_GROUP_NAMES = EXTRAS_TOOLS_GROUP.stream().map(LayerGroup::getName).collect(Collectors.toSet());

    public static final List<String> GROUP_ORDER = List.of(
            FAVORITES,
            LayerGroup.IMAGERY.getName(),
            LayerGroup.SURFACE_IMAGERY.getName(),
            LayerGroup.MODELS.getName(),
            LayerGroup.ELEVATION.getName(),
            LayerGroup.SURFACE_STRUCTURES.getName(),
            LayerGroup.ANALYTIC_SURFACES.getName(),
            LayerGroup.SHAPE_FILES.getName(),
            LayerGroup.PLACEMARK_OVERLAY.getName(),
            LayerGroup.MAP_DATA.getName(),
            LayerGroup.TOOLS.getName(),
            LayerGroup.FEATURES.getName(),
            LayerGroup.EXTRAS.getName(),
            EXTRA_TOOLS,
            WIDGETS,
            LayerGroup.UNLISTED_OVERLAY.getName(),
            LayerGroup.WSS_SHAPE_GROUPS.getName(),
            LayerGroup.TOP.getName());

    public static final Set<String> IGNORED_GROUPS = Set.of(
            LayerGroup.MAP_DATA.getName(),
            LayerGroup.WSS_SHAPE_GROUPS.getName(),
            LayerGroup.TOP.getName(),
            LayerGroup.EXTRAS.getName(),
            LayerGroup.UNLISTED_OVERLAY.getName(),
            LayerGroup.INTERNAL_TOOLS.getName());

    public static final Set<String> COLLAPSED_GROUPS = Set.of(
            LayerGroup.FEATURES.getName(),
            LayerGroup.TOOLS.getName(),
            LayerGroup.EXTRAS.getName(),
            FAVORITES,
            EXTRA_TOOLS);

    public static final Set<String> UNDRAGGABLE_GROUPS = Set.of(
            LayerGroup.MODELS.getName(),
            LayerGroup.ELEVATION.getName(),
            LayerGroup.TOOLS.getName(),
            LayerGroup.EXTRAS.getName(),
            LayerGroup.FEATURES.getName(),
            FAVORITES,
            EXTRA_TOOLS,
            WIDGETS);

    public static final ImageIcon ACTIVE_FILTER_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "filter.png");
    public static final ImageIcon INACTIVE_FILTER_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "filter_off.png");
    public static final ImageIcon FAVORITE_ICON;
    public static final ImageIcon NOT_FAVORITE_ICON;
    /** @since 5.2 */
    public static final ImageIcon SHOWING_BOUNDING_BOX_ICON;
    /** @since 5.2 */
    public static final ImageIcon HIDING_BOUNDING_BOX_ICON;
    public static final int ELEVATION_RENDER_DELTA = 2000;

    public enum Column
    {
        VISIBLE(Boolean.class),
        TITLE(String.class),
        STATUS(Icon.class),
        BOUNDING_BOX(Icon.class),
        FAVORITE(Icon.class),
        OBJECT(Object.class); // Always hidden

        final Class<?> type;

        Column(Class<?> type)
        {
            this.type = type;
        }

        public static final Column[] VALUES = values();
    }

    static
    {
        final URL favoriteUrl = UIResources.getUrl(PLATFORM_ICONS, "favorite.png");
        final BufferedImage favoriteImage = UIResources.getSizedImage(favoriteUrl, UIResources.STANDARD_IMAGE_SIZE);
        final BufferedImage notFavoriteImage = GraphicsUtils.darkenImage(GraphicsUtils.grayscaleImage(favoriteImage), 0.7);

        FAVORITE_ICON = new ImageIcon(favoriteImage);
        NOT_FAVORITE_ICON = new ImageIcon(notFavoriteImage);

        final URL drawBoundsUrl = UIResources.getUrl(PLATFORM_ICONS, "bounding_box.svg");
        final BufferedImage showBoundingBoxImage = UIResources.getSvg(null, drawBoundsUrl, UIResources.STANDARD_IMAGE_SIZE.width, UIResources.STANDARD_IMAGE_SIZE.height, true);
        //noinspection DataFlowIssue - showBoundingBoxImage can't be null, the default icon will be returned on any error
        final BufferedImage hideBoundingBoxImage = GraphicsUtils.darkenImage(GraphicsUtils.grayscaleImage(showBoundingBoxImage), 0.7);

        SHOWING_BOUNDING_BOX_ICON = new ImageIcon(showBoundingBoxImage);
        HIDING_BOUNDING_BOX_ICON = new ImageIcon(hideBoundingBoxImage);
    }

    /**
     * Convert a String LayerGroup name to the corresponding LayerViewManager String group name.  Basically, this
     * method points all LayerGroups listed in EXTRA_TOOLS to that virtual group.  Other LayerGroup names will simply
     * pass through.
     */
    public static String convertLayerGroupName(String group)
    {
        return EXTRAS_TOOLS_GROUP_NAMES.contains(group) ? EXTRA_TOOLS : group;
    }

    /**
     * Convert the String name of a LayerGroup to the LayerGroup itself.
     */
    public static LayerGroup nameToGroup(String groupName)
    {
        return Arrays.stream(LayerGroup.values())
                .filter(group -> group.getName().equals(groupName))
                .findFirst().orElse(null);
    }
}
