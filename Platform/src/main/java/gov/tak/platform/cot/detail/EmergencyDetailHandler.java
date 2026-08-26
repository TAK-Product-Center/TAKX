package gov.tak.platform.cot.detail;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.annotation.PseudoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.tak.api.util.StandardAttributeNames.EMERGENCY;
import static gov.tak.api.util.StandardAttributeNames.EMERGENCY_MESSAGE;

/**
 * Processes the {@code <emergency type="..."/>} detail tag.
 *
 * @since 5.7
 */
@PseudoBean
public class EmergencyDetailHandler extends CotDetailHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String EMERGENCY_DETAIL = EMERGENCY;

    EmergencyDetailHandler()
    {
        super(EMERGENCY_DETAIL);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        if (item.hasMetaValue(EMERGENCY))
        {
            final var emergency = new CotDetail(EMERGENCY_DETAIL);
            emergency.setAttribute(DetailConstants.ATTR_TYPE, item.getMetaString(EMERGENCY, "911"));
            detail.addChild(emergency);
            return true;
        }
        return false;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        if (!event.getType().startsWith(CotTypes.PREFIX_EMERGENCY)) return ImportResult.IGNORE;

        final String type = detail.getAttribute(DetailConstants.ATTR_TYPE);

        if (type == null || !(item instanceof PointMapItem pointMapItem))
        {
            logger.warn("Got an emergency message that can't be handled: {} with marker: {}", event, item);
            return ImportResult.FAILURE;
        }

        logger.debug("Add emergency attribute for {}, loc {}", item.getUid(), pointMapItem.getLocation());

        item.setMetaString(EMERGENCY, type);

        final String text = detail.getInnerText();
        if (text != null) item.setMetaString(EMERGENCY_MESSAGE, text);

        return ImportResult.SUCCESS;
    }
}
