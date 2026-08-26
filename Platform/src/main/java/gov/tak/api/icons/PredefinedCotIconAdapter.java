package gov.tak.api.icons;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static gov.tak.api.util.StandardAttributeNames.ICONSET_PATH;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Icon adapter to handle a set of predefined icons to use for specific CoT types.
 *
 * @since 3.0
 */
@ApplicationScoped
public class PredefinedCotIconAdapter implements IconAdapter
{
    // Comparator to order mappings by prefix
    private static final Comparator<Mapping> MAPPING_COMPARATOR = Comparator.comparing(m -> m.typePrefix);

    private static final String SPOT_MARKER_ICON = "reference_point.png";
    private static final BufferedImage LABEL_IMAGE = UIResources.getImage(ResourceDirectory.PLATFORM_ICONS, "spot_map_label.png");

    // All of our predefined mappings
    private static final List<Mapping> MAPPINGS = Arrays.asList(
            new Mapping("b", "b.png"),
            new Mapping("b-i", "document.png"),
            new Mapping(CotTypes.VIDEO, "video.png", ResourceDirectory.PLATFORM_ICONS),
            new Mapping("b-i-x-i", "camera.png"),

            new Mapping("b-a-g", "alarm-geophysical.png"),
            new Mapping("b-a-o-opn", "alarm-open.png"),
            new Mapping("b-a-o-pan", "alarm-panic.png"),
            new Mapping("b-a-o-tbl", "alarm-trouble.png"),

            new Mapping("b-m-p-s-p-i", "b-m-p-s-p-i.png"),
            new Mapping("b-m-p-s-p-loc", "sensor_location.png"),
            new Mapping("b-m-p-s-p-op", "binos.png"),

            new Mapping("b-m-p-a", "aimpoint.png"),
            new Mapping("b-m-p-c-z", "blacktriangle.png"),
            new Mapping("b-m-p-w", "gugpgpow------.png"),
            new Mapping("b-m-p-c-ip", "piicon_initial.png"),
            new Mapping("b-m-p-c-cp", "piicon_contact.png"),
            new Mapping("b-m-p-w-GOTO", "green_flag.png"),
            new Mapping("b-r-f-h-c", "damaged.png"),

            // JumpMaster plugin backup (in case the plugin is not installed)
            new Mapping("b-m-p-j-basic", "dip.png"),
            new Mapping("b-m-p-j-dip", "dip.png"),
            new Mapping("b-m-p-j-alt", "alt_dip.png"),
            new Mapping("b-m-p-j-alt-yellow", "alt_dip.png"),
            new Mapping("b-m-p-j-alt-green", "alt_dip_green.png"),
            new Mapping("b-m-p-j-alt-gray", "alt_dip_gray.png"),
            new Mapping("b-m-p-j-open", "open.png"),
            new Mapping("b-m-p-j-prp", SPOT_MARKER_ICON),
            new Mapping("b-m-p-j-virtual-dz", "virtual_dz.png"),
            new Mapping("b-m-p-j-last-out", "redlight.png"),
            new Mapping("b-m-p-j-height-marker", SPOT_MARKER_ICON),
            new Mapping("b-m-p-j-height-marker-yellow", "height_marker_yellow.png"),
            new Mapping("b-m-p-j-height-marker-red", "height_marker_red.png"),
            new Mapping("b-m-p-j-harp", "stoplight.png"),
            new Mapping("b-m-p-j-height-marker-gone", "no_icon.png"),
            new Mapping("b-m-p-j-harp-ring", "no_icon.png"),
            new Mapping("b-m-p-j-wind", "b.png"),
            new Mapping("b-m-p-j-lp", SPOT_MARKER_ICON),

            new Mapping("t-k-d", "ninelineicon.png"),
            new Mapping("t-s", "b.png"),

            new Mapping("inv_waypoint", "cornerpoint.png"),
            new Mapping("vsd-marker", "viewshed_eye.png"),

            new Mapping("u-X", SPOT_MARKER_ICON),
            new Mapping("u-d-p", SPOT_MARKER_ICON),
            new Mapping("u-d-c-c", SPOT_MARKER_ICON),   // Drawing circle center point
            new Mapping("u-d-z-c-c", SPOT_MARKER_ICON), // DZ circle center point
            new Mapping("u-r-b-c-c", SPOT_MARKER_ICON), // R+B circle center point
            new Mapping("u-r-b-bullseye", "no_icon.png"),    // R+B bullseye center point
            new Mapping("u-o-p", SPOT_MARKER_ICON),
            new Mapping("line_marker", SPOT_MARKER_ICON),
            new Mapping("crit_marker", SPOT_MARKER_ICON),
            new Mapping("end_u-o-r", SPOT_MARKER_ICON),

            new Mapping("line_corner", SPOT_MARKER_ICON),
            new Mapping("square_corner", SPOT_MARKER_ICON),
            new Mapping("square_marker", SPOT_MARKER_ICON),
            new Mapping("shape_marker", SPOT_MARKER_ICON),
            new Mapping("center_", SPOT_MARKER_ICON),
            new Mapping("corner_", "cornerpoint.png"),
            new Mapping("side_", "middlepoint.png"),

            new Mapping("a-h-G-E-S-t", "ttl-icon.png"),
            new Mapping("a-f-G-E-S-t", "ttl-icon-f.png"),
            new Mapping("a-u-G-E-S-t", "ttl-icon-u.png"),
            new Mapping("a-n-G-E-S-t", "ttl-icon-n.png"),
            new Mapping("a-f-G-I-U-exp", "green_flag.png"),

            new Mapping("stack", "cas_stack.png"));

    static
    {
        MAPPINGS.sort(MAPPING_COMPARATOR.reversed()); // Sort the mappings so that the longest prefixes are first
    }

    @Override
    public boolean adapt(@Nonnull Marker marker)
    {
        final String cotType = marker.getCotType();

        if (CotTypes.SPOT_MARKER.equals(cotType))
        {
            final String path = marker.getMetaString(ICONSET_PATH, "");
            if (path.endsWith(IconSetConstants.LABEL))
            {
                // LABEL markers won't show an icon on the globe, but they need to have the correct icon for usage
                // in other UI locations.
                marker.setExternalIconImage(LABEL_IMAGE);
            } else
            {
                // Spot Map Markers have a color, so we need to apply that to the base image
                final int colorInt = marker.getMetaInteger(StandardAttributeNames.COLOR, Color.white.getRGB());
                final var color = new Color(colorInt, true);
                marker.setExternalIconImage(RoleIconColorizer.colorize(SPOT_MARKER_ICON, color));
            }
            return true;
        } else
        {
            final URI uri = getUriForType(cotType);
            if (uri != null)
            {
                marker.setIconImage(uri);
                return true;
            }
        }
        return false;
    }

    /**
     * @return the URI of the icon to use if the given CoT type is one of our predefined types, {@code null} if not
     * @since 3.2
     */
    @Nullable
    public URI getUriForType(String cotType)
    {
        for (Mapping mapping : MAPPINGS)
        {
            if (cotType.startsWith(mapping.typePrefix))
            {
                return UIResources.getUri(mapping.resourceDirectory, mapping.iconName, false);
            }
        }

        return null;
    }

    @Override
    public void dispose()
    {
    }

    /**
     * Mapping from CoT type prefix to an icon name.
     *
     * @param typePrefix CoT type prefix to match
     * @param iconName Name of icon to apply if matched
     * @param resourceDirectory Location of named icon, defaults to {@link ResourceDirectory#TAK_ASSETS_ICONS}
     */
    private record Mapping(String typePrefix, String iconName, ResourceDirectory resourceDirectory)
    {
        Mapping(String typePrefix, String iconName)
        {
            this(typePrefix, iconName, TAK_ASSETS_ICONS);
        }
    }
}
