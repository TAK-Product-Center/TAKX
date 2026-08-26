package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.icons.IconSetConstants;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.Marker;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.net.URI;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_ICONSET_PATH;
import static gov.tak.api.cot.detail.DetailConstants.USER_ICON;
import static gov.tak.api.util.StandardAttributeNames.ADAPT_MARKER_ICON;
import static gov.tak.api.util.StandardAttributeNames.ICONSET_PATH;

/**
 * Handle user icon detail.
 *
 * @since 3.1
 */
@PseudoBean
public class UserIconDetailHandler extends CotDetailHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Icon set path prefix indicating a base64 encoded icon (image bytes)
    private static final String BASE_64 = "base64://";

    @Inject
    private IIconSetManager iconSetManager;

    UserIconDetailHandler()
    {
        super(USER_ICON);
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        if (item instanceof Marker marker)
        {
            final String path = detail.getAttribute(ATTR_ICONSET_PATH);

            if (!StringUtils.isEmpty(path))
            {
                item.setMetaString(ICONSET_PATH, path);

                // Spot map and 2525C icon handling is done during marker icon adaptation, not here
                if (!path.startsWith(IconSetConstants.COT_MAPPING_SPOTMAP) && !path.startsWith(IconSetConstants.COT_MAPPING_2525C))
                {
                    if (path.startsWith(BASE_64))
                    {
                        // Just set the MO URI since we have a base64 URL protocol handler
                        try
                        {
                            marker.setIconImage(new URI(path));
                        } catch (Exception e)
                        {
                            logger.warn("Failed to load base64 encoded image for UID '{}'", marker.getUid(), e);
                            return ImportResult.FAILURE;
                        }
                    } else
                    {
                        // Defer to the Icon Set Manager
                        final var userIcon = iconSetManager.getIconFromPath(path);
                        if (userIcon != null)
                        {
                            marker.setIconImage(userIcon.toUri());

                            final Color color = getColorFromDetail(event.getDetail());
                            if (color != null && !marker.getIconImageURI().equals(ImageUtils.DEFAULT_IMAGE_URI))
                            {
                                marker.setExternalIconImage(GraphicsUtils.colorImage(marker.getIconImage(), color));
                            }

                            marker.setMetaBoolean(ADAPT_MARKER_ICON, false); // We configured the icon
                        } else
                        {
                            logger.warn("Invalid Icon set path for UID '{}': {}", item.getUid(), path);
                            return ImportResult.FAILURE;
                        }
                    }
                }
            }
        }

        return ImportResult.SUCCESS;
    }

    /**
     * @return The color specified in the "color" detail, or {@code null} if not present, not valid, or equals {@link Color#white}
     * @since 5.3
     */
    @Nullable
    private Color getColorFromDetail(@Nonnull CotDetail detail)
    {
        final var colorDetail = detail.getFirstChildByName(DetailConstants.COLOR);
        final String colorAttribute = colorDetail != null ? colorDetail.getAttribute(DetailConstants.ATTR_ARGB) : null;

        if (colorAttribute != null)
        {
            try
            {
                final Color color = new Color(Long.decode(colorAttribute).intValue(), true);
                return Color.white.equals(color) ? null : color;
            } catch (NumberFormatException ignored)
            {
            }
        }
        return null;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        final String path = item.getMetaString(ICONSET_PATH, "");

        if (!StringUtils.isEmpty(path))
        {
            detail.addChild(new CotDetail(USER_ICON, ATTR_ICONSET_PATH, path));
            return true;
        }
        return false;
    }
}
