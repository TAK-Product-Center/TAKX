package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ACotEventExporter;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.MultiPolylineMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_LINE;
import static gov.tak.api.cot.detail.DetailConstants.LINK;

/**
 * CoT Exporter handling {@link MultiPolylineMapObject} objects.
 *
 * @since 3.0
 */
public class MultiPolylineExporter extends ACotEventExporter
{
    @Override
    public Class<? extends MapObject> getType()
    {
        return MultiPolylineMapObject.class;
    }

    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        final MultiPolylineMapObject multiPolylineMO = (MultiPolylineMapObject) mapObject;

        final CotEvent event = createBasicEvent(multiPolylineMO, ONE_DAY_SECONDS);
        final CotDetail detail = event.getDetail();

        final PolylineExporter polylineExporter = new PolylineExporter();

        for (IShapePathMapObject polyline : multiPolylineMO.getPolylines())
        {
            // Force the polyline export to ignore its owner (the multi-polyline), and export as a simple polyline
            final CotEvent polylineEvent = polylineExporter.exportDirect(polyline);

            if (polylineEvent != null) detail.addChild(new CotDetail(LINK, ATTR_LINE, polylineEvent.toString()));
        }

        return event;
    }
}
