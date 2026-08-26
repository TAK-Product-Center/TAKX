package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.AShapeExporter;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.IRectangle;
import gov.takx.dataservices.persist.MapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_POINT;
import static gov.tak.api.cot.detail.DetailConstants.LINK;

/**
 * Abstract base exporter handling rectangular Map Objects.
 *
 * @since 3.0
 */
public abstract class ARectangularExporter extends AShapeExporter
{
    // CoT requires this order: UL, LL, LR, UR (CCW from UL)
    // But getVertices returns: LL, LR, UR, UL
    private static final int[] VERTEX_ORDER = {3, 0, 1, 2};

    private final Class<? extends MapObject> mapObjectType;

    protected ARectangularExporter(Class<? extends MapObject> mapObjectType)
    {
        this.mapObjectType = mapObjectType;
    }

    @Override
    public Class<? extends MapObject> getType()
    {
        return mapObjectType;
    }

    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        final CotEvent event = createBasicEvent(mapObject, ONE_DAY_SECONDS);
        final CotDetail detail = event.getDetail();

        final IRectangle rectangle = (IRectangle) mapObject;

        // Add in all the corners as 'link' details, in required CoT order.
        final List<ILocation> vertices = rectangle.getVertices();

        for (int i : VERTEX_ORDER) detail.addChild(new CotDetail(LINK, ATTR_POINT, new GeoPoint(vertices.get(i)).toString()));

        handleBattlePositionHoldingArea(mapObject, event);

        return event;
    }
}
