package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.Marker;
import gov.takx.commons.utils.StringUtils;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_CALLSIGN;
import static gov.tak.api.cot.detail.DetailConstants.CONTACT;
import static gov.tak.api.util.StandardAttributeNames.CALLSIGN;

/**
 * Processes the {@code <contact callsign=""/>} detail tag.
 *
 * @since 3.0
 */
public class ContactDetailHandler extends CotDetailHandler
{
    ContactDetailHandler()
    {
        super(CONTACT);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        String title = item.getTitle();

        if (item instanceof Marker)
        {
            // For markers the "callsign" attributes takes priority
            final String callsign = item.getMetaString(CALLSIGN, "");

            if (!StringUtils.isEmpty(callsign)) title = callsign;
        }

        if (!StringUtils.isEmpty(title))
        {
            detail.addChild(new CotDetail(CONTACT, ATTR_CALLSIGN, title));
            return true;
        }
        return false;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final String callsign = detail.getAttribute(ATTR_CALLSIGN);

        if (StringUtils.isEmpty(callsign)) return ImportResult.IGNORE;

        item.setTitle(callsign);

        if (item instanceof Marker)
        {
            item.setMetaString(CALLSIGN, callsign);
            // PlacePointTool.updateCallsign((Marker) item); TODO
        }

        return ImportResult.SUCCESS;
    }
}
