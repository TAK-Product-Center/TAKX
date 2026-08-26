package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_BATTERY;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_READINESS;
import static gov.tak.api.cot.detail.DetailConstants.STATUS;
import static gov.tak.api.util.StandardAttributeNames.BATTERY;
import static gov.tak.api.util.StandardAttributeNames.READINESS;

/**
 * Handle device status.
 *
 * @since 3.0
 */
public class StatusDetailHandler extends CotDetailHandler
{
    StatusDetailHandler()
    {
        super(STATUS);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        return false; // Only exported on self SA message
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final String readiness = detail.getAttribute(ATTR_READINESS);
        if (readiness != null)
        {
            item.setMetaBoolean(READINESS, Boolean.parseBoolean(readiness));
        }

        final String battery = detail.getAttribute(ATTR_BATTERY);
        if (battery != null)
        {
            item.setMetaLong(BATTERY, parseInt(battery, 100));
        }

        return ImportResult.SUCCESS;
    }
}
