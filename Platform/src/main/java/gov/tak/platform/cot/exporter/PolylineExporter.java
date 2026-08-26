package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.ShapePathMapObject;
import gov.takx.dataservices.persist.ShapePolygonMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Exporter handling {@link ShapePolygonMapObject}.
 *
 * @since 3.0
 */
public class PolylineExporter extends APolyshapeExporter
{
    @Override
    public Class<? extends MapObject> getType()
    {
        return ShapePathMapObject.class;
    }

    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        final IMapObject owner = mapObject.getOwner();

        // See if this marker is part of a Route or MultiPolyline compound MO, and redirect export if so
        if (owner instanceof IMultiPolylineMapObject)
        {
            return new MultiPolylineExporter().export(owner);
        } else if (owner instanceof IRouteMapObject)
        {
            return new RouteExporter().export(owner);
        } else
        {
            return super.export(mapObject); // Nope, just simple polyline
        }
    }

    /**
     * Create a CoT Event from the mapObject 'directly', meaning ignore its owner and export as a simple polyline.
     *
     * @param mapObject MapObject to process
     * @return Generated CotEvent, may be {@code null} if the exporter can't generate the event for some reason
     */
    @Nullable
    CotEvent exportDirect(@Nonnull IMapObject mapObject)
    {
        return super.export(mapObject);
    }
}
