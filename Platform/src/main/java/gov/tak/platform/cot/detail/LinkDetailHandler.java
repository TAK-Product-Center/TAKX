package gov.tak.platform.cot.detail;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.utils.StringUtils;

/**
 * Handler for {@code <link/>} detail tag that is used in CoT SPIs/DPs.
 *
 * @since 5.1
 */
@PseudoBean
public class LinkDetailHandler extends CotDetailHandler
{
    public LinkDetailHandler()
    {
        super(DetailConstants.LINK);
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        return item instanceof PointMapItem;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final String linkUid = detail.getAttribute(DetailConstants.ATTR_UID);
        final String linkType = detail.getAttribute(DetailConstants.ATTR_TYPE);

        if (item.getCotType().startsWith(CotTypes.SENSOR_POINT_OF_INTEREST) && !StringUtils.isEmpty(linkUid))
        {
            item.setMetaString(StandardAttributeNames.PARENT_UID, linkUid);
            if (!StringUtils.isEmpty(linkType)) item.setMetaString(StandardAttributeNames.PARENT_TYPE, linkType);
        } else
        {
            if (linkUid != null) item.setMetaString("parent_uid", linkUid);
            if (linkType != null) item.setMetaString("parent_type", linkType);

            final String time = detail.getAttribute(DetailConstants.ATTR_PRODUCTION_TIME);
            if (!StringUtils.isEmpty(time)) item.setMetaString(StandardAttributeNames.PRODUCTION_TIME, time);

            final String parentCallsign = detail.getAttribute(DetailConstants.ATTR_PARENT_CALLSIGN);
            if (!StringUtils.isEmpty(parentCallsign)) item.setMetaString(StandardAttributeNames.PARENT_CALLSIGN, parentCallsign);
        }

        return ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail root)
    {
        String parentUid = item.getMetaString(StandardAttributeNames.PARENT_UID, "");
        String parentType = item.getMetaString(StandardAttributeNames.PARENT_TYPE, "");
        String productionTime = item.getMetaString(StandardAttributeNames.PRODUCTION_TIME, "");
        String parentCallsign = item.getMetaString(StandardAttributeNames.PARENT_CALLSIGN, "");

        if (!parentUid.isBlank() && !parentType.isBlank())
        {
            // <link relation='p-p' type='a-f-G-U-C-I' uid='852b2a2a-02be-5a0c-b2ae-66001ffc1c55' />

            final var linkDetail = new CotDetail(DetailConstants.LINK);
            root.addChild(linkDetail);

            linkDetail.setAttribute(DetailConstants.ATTR_RELATION, DetailConstants.LINK_RELATION_PARENT_PRODUCER);
            linkDetail.setAttribute(DetailConstants.ATTR_TYPE, parentType);
            linkDetail.setAttribute(DetailConstants.ATTR_UID, parentUid);

            if (!productionTime.isBlank()) linkDetail.setAttribute(DetailConstants.ATTR_PRODUCTION_TIME, productionTime);
            if (!parentCallsign.isBlank()) linkDetail.setAttribute(DetailConstants.ATTR_PARENT_CALLSIGN, parentCallsign);
        }

        return true;
    }
}
