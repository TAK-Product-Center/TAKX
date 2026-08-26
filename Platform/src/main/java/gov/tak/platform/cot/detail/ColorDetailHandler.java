package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;

/**
 * Map item color.
 * <p>
 * TODO: Unfortunately not consistent whether the color is stored within attribute "argb" or "value" - for now just include both
 *
 * @since 3.0
 */
public class ColorDetailHandler extends CotDetailHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    ColorDetailHandler()
    {
        super(DetailConstants.COLOR);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        final Color color = getColor(item);
        if (color != null)
        {
            final int rgb = color.getRGB();
            final CotDetail colorDetail = new CotDetail(DetailConstants.COLOR);
            colorDetail.setAttribute(DetailConstants.ATTR_VALUE, Integer.toString(rgb));
            colorDetail.setAttribute(DetailConstants.ATTR_ARGB, Integer.toString(rgb));
            detail.addChild(colorDetail);
            return true;
        }
        return false;
    }

    /**
     * @return The color to send for this map item, or {@code null} if none should be specified
     * @since 5.3
     */
    @Nullable
    private Color getColor(@Nonnull MapItem item)
    {
        if (item.hasMetaValue(StandardAttributeNames.COLOR))
        {
            return new Color(item.getMetaInteger(StandardAttributeNames.COLOR, Color.white.getRGB()), true);
        } else if (item instanceof IShapeMapObject<?> shapeMapObject)
        {
            return shapeMapObject.getLineColor();
        }
        return null;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final String attr = detail.hasAttribute(DetailConstants.ATTR_ARGB)
                ? DetailConstants.ATTR_ARGB
                : DetailConstants.ATTR_VALUE;

        final String colorArgb = detail.getAttribute(attr);

        if (StringUtils.isEmpty(colorArgb))
        {
            logger.warn("Import failure: color value was empty.");
            return ImportResult.FAILURE;
        }

        item.setMetaInteger(StandardAttributeNames.COLOR, parseInt(colorArgb, Color.white.getRGB()));

        return ImportResult.SUCCESS;
    }
}
