package gov.tak.api.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_VALUE;
import static gov.tak.api.cot.detail.DetailConstants.BPHA;

/**
 * Abstract base for exporters handling shapes.
 *
 * @since 3.0
 */
@PseudoBean
public abstract class AShapeExporter extends ACotEventExporter
{
    /**
     * Add the Battle Position Holding Area detail if it is present on the shape.
     *
     * @param mapObject map object to process
     * @param event     CoT Event to update
     */
    protected void handleBattlePositionHoldingArea(IMapObject mapObject, CotEvent event)
    {
        if (mapObject.hasMetaValue(BPHA))
        {
            final CotDetail holdingArea = new CotDetail(BPHA, ATTR_VALUE, mapObject.getMetaString(BPHA, ""));
            event.getDetail().addChild(holdingArea);
        }
    }
}
