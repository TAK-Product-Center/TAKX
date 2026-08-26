package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.takx.commons.utils.StringUtils;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_TITLE;
import static gov.tak.api.cot.detail.DetailConstants.MED_EVAC;

/**
 * Handler for {@code <_medevac_/>} detail tag for MED or CAS evac locations.  Set the marker title from the
 * title in this detail.
 *
 * @since 3.1
 */
public class MedicalDetailHandler extends CotDetailHandler
{
    MedicalDetailHandler()
    {
        super(MED_EVAC);
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final String title = detail.getAttribute(ATTR_TITLE);

        if (!StringUtils.isEmpty(title)) item.setTitle(title);

        return ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        return false; // TODO implement when we have a mechanism to create these
    }
}
