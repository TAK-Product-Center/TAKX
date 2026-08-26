package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;

/**
 * Handle "remarks" for a Map Item.
 *
 * @since 3.0
 */
public class RemarksDetailHandler extends CotDetailHandler
{
    RemarksDetailHandler()
    {
        super(DetailConstants.REMARKS);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        if (item.getRemarks() != null)
        {
            detail.addChild(new CotDetail(DetailConstants.REMARKS, item.getRemarks()));
            return true;
        }

        return false;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final String remarksText = detail.getInnerText();

        item.setRemarks(remarksText == null ? "" : remarksText);

        return ImportResult.SUCCESS;
    }
}
